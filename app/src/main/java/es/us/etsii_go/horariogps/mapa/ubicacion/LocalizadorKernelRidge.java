package es.us.etsii_go.horariogps.mapa.ubicacion;

import android.net.wifi.ScanResult;
import android.util.Log;

import es.us.etsii_go.horariogps.mapa.models.ModeloDatosEscaneo;
import es.us.etsii_go.horariogps.mapa.models.ModeloDatosWifi;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LocalizadorKernelRidge {

    private final List<ModeloDatosWifi> baseDatos;
    private double[][] alphas;
    private final double gamma;
    private final List<String> bssidsMaestros;
    private List<double[]> vectoresEntrenamiento;
    private double[] medias;
    private double[] scales;

    public LocalizadorKernelRidge(List<ModeloDatosWifi> baseDatos, String jsonAlphas, double gamma, List<String> bssidsMaestros, double[] medias, double[] scales) {
        // Clase que calcula el algoritmo kernerRidge, a partir de un escaneo.
        // NOTA: el algoritmo no se ejecuta aqui, se hace previamente con los datos, y es el que
        // determina los alphas, gamma, los bssidsMaestros, y las medias y scales.
        // Lo hago en un scrip de python previamente.

        this.baseDatos = baseDatos;
        this.gamma = gamma;
        this.bssidsMaestros = bssidsMaestros;
        cargarAlphasDesdeStringJson(jsonAlphas);

        this.medias=medias;
        this.scales=scales;

        // Hacemos la preparacion de los vectores de entrenamiento al crear la clase, asi nos
        // aseguramos de que se ejecutan solo una vez (asi mejoramos el rendimiento) y solo
        // utilizamos la lista de la base de datos (vamos, la lista de datos wifi que vienen
        // del json) solo una vez.
        // A partir de ahora, solo trabajamos con los vectoresEntrenamiento, ya listos.
        List<double[]> vectoresEntren = new ArrayList<>();
        for (int i = 0; i < baseDatos.size(); i++) {
            List<ModeloDatosEscaneo> lecturasEntrenamiento = baseDatos.get(i).getLecturas();
            double[] xEntrenamiento = prepararVectorEntrenamiento(lecturasEntrenamiento);
            vectoresEntren.add((xEntrenamiento));
        }
        this.vectoresEntrenamiento = vectoresEntren;
    }

    //METODO PRINCIPAL
    public float[] calcularPosicionKRidge(List<ScanResult> escaneoActual) {
        // Funcion que a partir de un escaneo, calculamos la aproximacion de la posicion
        // a partir del algoritmo de KernelRidge

        // 1. Preparamos el vector del escaneo en tiempo real (ScanResult de Android)
        double[] xActual = prepararVectorEscaner(escaneoActual);

        // Mensajes auxiliares, seran borrados
        Log.d("xact", "Estas son las kvector:"+ Arrays.toString(xActual));
        Log.d("ventreno", "Estas son las kvector:"+ Arrays.toString(this.vectoresEntrenamiento.get(2)));

        // 2. Calcular Vector Kernel
        double[] kVector = new double[baseDatos.size()];
        for (int i = 0; i < baseDatos.size(); i++) {
            // Al escaneo actual lo comparamos con cada uno de los que tenemos en los vectores entrenamiento
            kVector[i] = calcularKernelRBF(xActual, this.vectoresEntrenamiento.get(i));
        }

        // Otro mensaje
        Log.d("kvector", "Estas son las kvector:"+ Arrays.toString(kVector));

        // 3. Proyección a 2D
        double posX = 0.0;
        double posY = 0.0;

        for (int i = 0; i < baseDatos.size(); i++) {
            // Calculo de la posicion a partir del algoritmo
            posX += kVector[i] * alphas[i][0];
            posY += kVector[i] * alphas[i][1];
            Log.d("Posiciones", "Estas son las pos"+posX+posY);
        }
        return new float[]{(float) posX, (float) posY};
    }

    //MÉTODOS DE PREPARACIÓN DE VECTORES
    private double[] prepararVectorEscaner(List<ScanResult> resultados) {
        double[] vector = new double[bssidsMaestros.size()];
        Arrays.fill(vector, -100.0);

        // Aqui voy a quedarme con los que he filtrado en el escaner, por ejemplo
        // con los que empiezan por eduroam o digi.
        if (resultados != null) {
            for (ScanResult sr : resultados) {
                if (sr.SSID != null && sr.SSID.toLowerCase().startsWith("digi")) {
                    int index = bssidsMaestros.indexOf(sr.BSSID);
                    if (index != -1) {
                        vector[index] = sr.level;
                    }
                }
            }
        }
        return normalizarVector(vector);
    }

    private double[] prepararVectorEntrenamiento(List<ModeloDatosEscaneo> resultados) {
        double[] vector = new double[bssidsMaestros.size()];
        Arrays.fill(vector, -100.0);

        // Aqui no hace falta pues ya lo hemos filtrado en el json, al obtener los datos
        if (resultados != null) {
            for (ModeloDatosEscaneo sr : resultados) {
                int index = bssidsMaestros.indexOf(sr.bssid);
                if (index != -1) {
                    vector[index] = sr.rssi;
                }
            }
        }
        return normalizarVector(vector);
    }

    private void cargarAlphasDesdeStringJson(String jsonString) {
        // Metodo para obtener el alpha a partir de un string de json, ya que los alphas vienen
        // de un string en formato json.
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            int numFilas = jsonArray.length();
            alphas = new double[numFilas][2];

            for (int i = 0; i < numFilas; i++) {
                JSONArray fila = jsonArray.getJSONArray(i);
                alphas[i][0] = fila.getDouble(0);
                alphas[i][1] = fila.getDouble(1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Metodos matematicos, el calculo del kernel entre los vectores x e y:
    private double calcularKernelRBF(double[] x, double[] y) {
        double sumaCuadrados = 0;
        for (int i = 0; i < x.length; i++) {
            double diff = x[i] - y[i];
            sumaCuadrados += diff * diff;
        }
        return Math.exp(-gamma * sumaCuadrados);
    }

    // Y la normalizacion del vector:
    public double[] normalizarVector(double[] escaneoActual) {
        int n = escaneoActual.length;
        double[] vectorNormalizado = new double[n];

        for (int i = 0; i < n; i++) {
            // Accedemos a las variables de instancia this.means y this.scales
            double mean = this.medias[i];
            double scale = this.scales[i];

            // Aplicamos (valor - media) / escala
            // Usamos un pequeño umbral (1e-9) para evitar dividir por cero
            if (Math.abs(scale) > 1e-9) {
                vectorNormalizado[i] = (escaneoActual[i] - mean) / scale;
            } else {
                // Si la escala es 0, el valor estandarizado es 0
                vectorNormalizado[i] = 0.0;
            }
        }

        return vectorNormalizado;
    }
}