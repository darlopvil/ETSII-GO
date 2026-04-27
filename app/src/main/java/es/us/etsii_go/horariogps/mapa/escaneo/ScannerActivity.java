package es.us.etsii_go.horariogps.mapa.escaneo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import es.us.etsii_go.R;
import es.us.etsii_go.horariogps.mapa.models.ModeloDatosEscaneo;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScannerActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    private TextView jsonOutput;

    private static final int PERMISO_UBICACION_ID = 100;

    private final String NOMBRE_ARCHIVO = "datos_entrenamiento_wifi_casa.json";

    private int escaneosRealizados = 0;
    private final int TOTAL_ESCANEOS = 5;
    private ProgressDialog progressDialog;

    List<List<ScanResult>> listaDeEscaneosParaMediana = new ArrayList<>();

    private void iniciarMuestreo() {
        escaneosRealizados = 0;
        listaDeEscaneosParaMediana.clear();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Realizando muestreo (0/5)...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ejecutarSiguienteEscaneo();
    }

    private void ejecutarSiguienteEscaneo() {
        if (escaneosRealizados < TOTAL_ESCANEOS) {
            // 1. Lanzar el escaneo de Android
            wifiManager.startScan();

            // 2. Obtener resultados actuales
            List<ScanResult> resultados = wifiManager.getScanResults();

            List<ScanResult> resultadosfiltrados = new ArrayList<>();

            for (ScanResult res : resultados) {
                if (res.SSID != null && res.SSID.toLowerCase().startsWith("digi")) {
                    resultadosfiltrados.add(res);
                }
            }
            listaDeEscaneosParaMediana.add(resultadosfiltrados);

            escaneosRealizados++;
            progressDialog.setMessage("Realizando muestreo (" + escaneosRealizados + "/5)...");

            // 3. Esperar 3 segundos antes del próximo (para evitar caché)
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ejecutarSiguienteEscaneo();
                }
            }, 3000);

        } else {
            progressDialog.dismiss();

            // Calculamos la mediana con el metodo correspondiente
            List<LecturaFiltrada> datosLimpios = calcularMedianaEscaneos(listaDeEscaneosParaMediana);

            // Llamamos al diálogo que pide ID, X, Y para guardar
            mostrarDialogoGuardado(datosLimpios);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_mapa_scanner_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        jsonOutput = findViewById(R.id.jsonOutput);

        Button btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(v -> iniciarMuestreo());

        pedirPermisosWifi();
    }

    private void startWifiScan() {
        // Registramos el receptor para esperar el resultado
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();

        Toast.makeText(this, "Escaneando...", Toast.LENGTH_SHORT).show();
    }

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> results = wifiManager.getScanResults();
            List<ModeloDatosEscaneo> recordList = new ArrayList<>();

            // Los resultados estan filtrados
            for (ScanResult res : results) {
                if (res.SSID != null && res.SSID.toLowerCase().startsWith("digi")) {
                    recordList.add(new ModeloDatosEscaneo(res.BSSID, res.SSID, res.level));
                }

            }

            // Convertimos la lista a un JSON bonito usando GSON
            String finalJson = new GsonBuilder().setPrettyPrinting().create().toJson(recordList);

            // Lo mostramos en pantalla para copiarlo
            jsonOutput.setText(finalJson);
            Log.d("WIFI_JSON", finalJson);

            // Importante: desregistrar para no gastar batería
            //unregisterReceiver(this);
        }
    };


    private void pedirPermisosWifi() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Si no tenemos el permiso, lanzamos el pop-up para pedirlo
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISO_UBICACION_ID);
        }
    }


    // Para obtener los datos
    private void mostrarDialogoGuardado(List<LecturaFiltrada> resultadosEscaneo) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Guardar Punto de Entrenamiento");

            // Crear un layout vertical para meter los 3 campos de texto
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 20);

            final EditText inputId = new EditText(this);
            inputId.setHint("ID del punto (ej. 1, 2, 3...)");
            layout.addView(inputId);

            final EditText inputX = new EditText(this);
            inputX.setHint("Coordenada X (metros)");
            inputX.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            layout.addView(inputX);

            final EditText inputY = new EditText(this);
            inputY.setHint("Coordenada Y (metros)");
            inputY.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            layout.addView(inputY);

            builder.setView(layout);

            // Botón Guardar
            builder.setPositiveButton("Guardar", (dialog, which) -> {
                try {
                    String id = inputId.getText().toString();
                    double x = Double.parseDouble(inputX.getText().toString());
                    double y = Double.parseDouble(inputY.getText().toString());

                    guardarDatosEnJson(id, x, y, resultadosEscaneo);

                } catch (NumberFormatException e) {
                    Toast.makeText(ScannerActivity.this, "Por favor, introduce números válidos", Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

            builder.show();
        }

        /**
         * Lee el JSON existente, añade el nuevo punto y lo vuelve a guardar
         */
        private void guardarDatosEnJson(String id, double x, double y, List<LecturaFiltrada> escaneos) {
            JSONArray baseDatosArray = leerJsonExistente();

            try {
                // 1. Crear el objeto para este punto físico
                JSONObject nuevoPunto = new JSONObject();
                nuevoPunto.put("id", id);
                nuevoPunto.put("coordenada_x", x);
                nuevoPunto.put("coordenada_y", y);

                // 2. Crear el array de lecturas Wi-Fi para este punto
                JSONArray lecturasArray = new JSONArray();
                for (LecturaFiltrada sr : escaneos) {
                    JSONObject lectura = new JSONObject();
                    lectura.put("bssid", sr.bssid); // Crucial para tu clase KLDA
                    lectura.put("ssid", sr.ssid);   // Solo para que tú lo leas más fácil
                    lectura.put("rssi", sr.rssiMediana);  // La potencia bruta
                    lecturasArray.put(lectura);
                }

                nuevoPunto.put("lecturas", lecturasArray);

                // 3. Añadir a la base de datos principal
                baseDatosArray.put(nuevoPunto);

                // 4. Escribir al archivo
                FileOutputStream fos = openFileOutput(NOMBRE_ARCHIVO, Context.MODE_PRIVATE);
                fos.write(baseDatosArray.toString(4).getBytes()); // El '4' lo formatea bonito con tabulaciones
                fos.close();

                Toast.makeText(this, "Guardado correctamente. Puntos totales: " + baseDatosArray.length(), Toast.LENGTH_SHORT).show();
                Log.d("WIFI_DB", "Guardado punto ID: " + id);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Recupera el archivo JSON si ya existe, o crea un array vacío si es la primera vez
         */
        private JSONArray leerJsonExistente() {
            try {
                File file = new File(getFilesDir(), NOMBRE_ARCHIVO);
                if (!file.exists()) {
                    return new JSONArray();
                }

                FileInputStream fis = openFileInput(NOMBRE_ARCHIVO);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader bufferedReader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                fis.close();

                return new JSONArray(sb.toString());

            } catch (Exception e) {
                e.printStackTrace();
                return new JSONArray(); // Si hay error o está corrupto, empezamos de cero (¡cuidado!)
            }
        }


        // Voy a coger 5 escaneos de cada punto, y tomar la mediana:


    // Clase sencilla para devolver los datos procesados
    class LecturaFiltrada {
        String bssid;
        String ssid;
        int rssiMediana;

        LecturaFiltrada(String bssid, String ssid, int rssiMediana) {
            this.bssid = bssid;
            this.ssid = ssid;
            this.rssiMediana = rssiMediana;
        }
    }


    private List<LecturaFiltrada> calcularMedianaEscaneos(List<List<ScanResult>> todosLosEscaneos) {
        Map<String, AcumuladorRssi> mapaAcumulador = new HashMap<>();

        // Agrupamos todos los valores por BSSID (Router)
        for (List<ScanResult> unEscaneo : todosLosEscaneos) {
            for (ScanResult sr : unEscaneo) {
                if (!mapaAcumulador.containsKey(sr.BSSID)) {
                    mapaAcumulador.put(sr.BSSID, new AcumuladorRssi(sr.SSID));
                }
                mapaAcumulador.get(sr.BSSID).acumular(sr.level);
            }
        }

        // Calculamos la mediana y creamos la lista final
        List<LecturaFiltrada> resultados = new ArrayList<>();
        for (Map.Entry<String, AcumuladorRssi> entry : mapaAcumulador.entrySet()) {
            String bssid = entry.getKey();
            AcumuladorRssi acumulador = entry.getValue();

            resultados.add(new LecturaFiltrada(
                    bssid,
                    acumulador.ssid,
                    acumulador.getMediana() // <-- Aquí ocurre la magia
            ));
        }
        return resultados;
    }

    // Clase auxiliar para guardar y calcular la Mediana
    class AcumuladorRssi {
        String ssid;
        List<Integer> historialRssi;

        AcumuladorRssi(String ssid) {
            this.ssid = ssid;
            this.historialRssi = new ArrayList<>();
        }

        void acumular(int rssi) {
            historialRssi.add(rssi);
        }

        int getMediana() {
            if (historialRssi.isEmpty()) return -100;

            // 1. Ordenamos la lista de menor a mayor
            Collections.sort(historialRssi);

            int size = historialRssi.size();

            // 2. Si es impar, cogemos el del medio
            if (size % 2 != 0) {
                return historialRssi.get(size / 2);
            }
            // 3. Si es par, cogemos la media de los dos del medio
            else {
                int valor1 = historialRssi.get((size / 2) - 1);
                int valor2 = historialRssi.get(size / 2);
                return (valor1 + valor2) / 2;
            }
        }
    }
}
