package es.us.etsii_go.horariogps.mapa.ubicacion;

import android.net.wifi.ScanResult;
import android.util.Log;

import es.us.etsii_go.horariogps.mapa.models.ModeloDatosEscaneo;
import es.us.etsii_go.horariogps.mapa.models.ModeloDatosWifi;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiPositioningManager {
    // Clase para gestionar el posicionamiento wifi, al crearse le pasamos una lista
    // que actua como base de datos, que sera el json de todas las senales wifi.
    // A partir de un escaneo, otra lista, calcula un algoritmo ( que sera knn o reduccion
    // de dimensiones) , y devolvera el punto en coordenadas (x,y) en el que
    // piensa que estamos.
    private List<ModeloDatosWifi> database;
    private final int VALOR_MINIMO = -100; // Señal no encontrada

    private LocalizadorKernelRidge kldaHandler;

    public WifiPositioningManager(List<ModeloDatosWifi> database) {
        this.database = database;
    }

    public static WifiPositioningManager createWithKLDA(List<ModeloDatosWifi> db, String json, double g, List<String> macs,double[] medias, double[] scales) {
        WifiPositioningManager manager = new WifiPositioningManager(db);
        manager.setupKLDA(json, g, macs,medias,scales);
        return manager;
    }

    // ALGORITMO KNN:
    public float[] calculatePositionKNN(List<ScanResult> currentScan) {
        double minDistance = Double.MAX_VALUE;
        ModeloDatosWifi bestMatch = null;

        // Creamos un diccionario para ser mas eficiente en la busqueda.
        Map<String, Integer> currentSignals = new HashMap<>();
        for (ScanResult s : currentScan) {
            currentSignals.put(s.BSSID, s.level);
        }

        // Algoritmo: Distancia Euclídea
        // d = sqrt( sum( (señal_json - señal_aire)^2 ) )
        List<Map.Entry<ModeloDatosWifi, Double>> scores = new ArrayList<>();

        for (ModeloDatosWifi point : database) {
            double sum = 0;
            for (ModeloDatosEscaneo reading : point.lecturas) {
                int airRssi = currentSignals.containsKey(reading) ? currentSignals.get(reading.bssid) : VALOR_MINIMO;
                sum += Math.pow(reading.rssi - airRssi, 2);
            }
            double distance = Math.sqrt(sum);
            Log.d("ALGORITMO", "Distancia a " + point.punto_id + ": " + distance);
            scores.add(new AbstractMap.SimpleEntry<>(point, distance));
        }

        // Ordenamos por cercanía
        Collections.sort(scores, (a, b) -> a.getValue().compareTo(b.getValue()));

        // Elegimos el primero
        ModeloDatosWifi p1 = scores.get(0).getKey();
        ModeloDatosWifi p2 = scores.get(1).getKey();

        float finalX = (p1.coordenada_pdf.x + p2.coordenada_pdf.x) / 2;
        float x = p1.coordenada_pdf.x;
        float finalY = (p1.coordenada_pdf.y + p2.coordenada_pdf.y) / 2;
        float y = p1.coordenada_pdf.y;

        return new float[]{x,y};
    }


    // ALGORITMO REDUCCION DE DIMENSIONES: se basa en que al reducir las dimensiones a 2, o
    // a 3, depende de como salga, de las distancias medidas (que seria mejor normalizar),
    // entonces se pinta el mapa de manera muy exacta. Vamos a ver como sale:

    public void setupKLDA(String jsonAlphas, double gamma, List<String> bssidsMaestros,double[] medias, double[] scales) {
        this.kldaHandler = new LocalizadorKernelRidge(
                this.database,
                jsonAlphas,
                gamma,
                bssidsMaestros,
                medias,
                scales
        );
    }

    // --- ALGORITMO REDUCCIÓN DE DIMENSIONES (KLDA) ---
    public float[] calculatePositionKLDA(List<ScanResult> currentScan) {
        if (kldaHandler == null) {
            Log.e("WIFI_POS", "¡Error! Debes llamar a setupKLDA primero.");
            return new float[]{0, 0};
        }

        // Delegamos el trabajo a la clase especializada
        return kldaHandler.calcularPosicionKRidge(currentScan);
    }


}
