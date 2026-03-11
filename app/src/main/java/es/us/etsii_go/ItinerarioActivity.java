package es.us.etsii_go;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ItinerarioActivity extends AppCompatActivity {

    // VARIABLES GLOBALES
    private EditText origen, destino;
    private Button botonCalcularRuta;
    private ScrollView scrollResultados;
    private LinearLayout layoutResultados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.itinerario);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.itinerario_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // INSTANCIAMOS LAS VARIABLES
     origen = findViewById(R.id.campo_origen);
     destino = findViewById(R.id.campo_destino);
     botonCalcularRuta = findViewById(R.id.boton_calcular_ruta);
     scrollResultados = findViewById(R.id.scroll_resultados);
     layoutResultados = findViewById(R.id.resultados_layout);

     // LISTENER DEL BOTÓN
        botonCalcularRuta.setOnClickListener(v -> {
            String origen_input = origen.getText().toString().trim();
            String destino_input = destino.getText().toString().trim();

            if (origen_input.isEmpty() || destino_input.isEmpty()) {
                Toast.makeText(this, "Por favor, indica un origen y un destino", Toast.LENGTH_SHORT).show();
                return;
            }
            // Tras presionar el botón, ocultamos el teclado
            InputMethodManager miTeclado = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            miTeclado.hideSoftInputFromWindow(v.getWindowToken(), 0);

            // Mostramos un mensaje de "Cargando..." para indicar que se está calculando la ruta
            Toast.makeText(this, "Buscando la mejor ruta...", Toast.LENGTH_SHORT).show();

            // Llamamos a la API en un hilo secundario
            calcularRuta(origen_input, destino_input);

        });

    }

    // MÉTODOS PERSONALES
    private void calcularRuta(String origen, String destino) {

        // Creamos un hilo con la petición a la API de Google Routes
        new Thread(() -> {
            try {
                // 1.- Establecemos el cuerpo de la solicitud
                URL url = new URL("https://routes.googleapis.com/directions/v2:computeRoutes");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("X-Goog-Api-Key", BuildConfig.API_KEY_GOOGLE);
                // Pedimos que nos devuelva los pasos exactos de la ruta
                connection.setRequestProperty("X-Goog-FieldMask","routes.duration,routes.distanceMeters,routes.legs.steps");
                connection.setDoOutput(true);

                // 2.- Construimos el JSON de la petición
                JSONObject body = new JSONObject();
                body.put("origin", new JSONObject().put("address", origen));
                body.put("destination", new JSONObject().put("address", destino));
                body.put("travelMode", "TRANSIT");  // Usar el transporte público si está disponible

                OutputStream os = connection.getOutputStream(); // "Abrimos la tubería" para enviar los datos a Google
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));    // Pasamos a bytes los datos del usuario y los mandamos con write()
                os.close(); // Cerramos la tubería. Google ya puede procesar la solicitud

                // 3.- Leemos la respuesta (si es exitosa o el error sino)
                int responseCode = connection.getResponseCode();
                BufferedReader br;

                if (responseCode >= 200 && responseCode <= 229) {
                    // Respuesta existosa, leemos el InputStream normalmente:
                    br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    // Hubo un error, leemos el ErrorStream:
                    br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }
                StringBuilder respuesta = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    respuesta.append(line);
                    line = br.readLine();
                }
                br.close();

                // ------------ DEBUG ZONE --------------
                String jsonCrudo = respuesta.toString();
                logLargo("ItinerarioApp_JSON", "Respuesta HTTP " + responseCode + ": \n" + jsonCrudo);

                // 4.- Parseamos la respuesta y actualizamos la UI SOLO si la respuesta fue exitosa:
                if (responseCode == 200) {
                    parsearYMostrarResultados(jsonCrudo);
                } else {
                    Log.e("ItinerarioApp", "Google devolvió un error: " + jsonCrudo);
                    runOnUiThread(() -> Toast.makeText(ItinerarioActivity.this, "Error en la petición: HTTP " + responseCode, Toast.LENGTH_SHORT).show());
                }


            } catch (Exception e) {
                Log.e("ItinerarioApp", "Error en la conexión API", e);
                runOnUiThread(() -> Toast.makeText(ItinerarioActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show());
            }
        }).start(); // Arrancamos el hilo

    }

    private void parsearYMostrarResultados(String jsonResponse) {
        // Volvemos al Hilo Principal (UI) para pintar el resultado de la respuesta API
        runOnUiThread(()-> {
            try {
                // 1.- Limpiamos resultados anteriores por si el usuario busca otra vez
                layoutResultados.removeAllViews();

                // 2.- Hacemos visible el ScrollView
                scrollResultados.setVisibility(ScrollView.VISIBLE);

                // 3.- Parseo del JSON
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray rutas = jsonObject.getJSONArray("routes");

                if (rutas.length() == 0) {
                    Toast.makeText(this, "No se ha encontrado una ruta disponible", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject ruta = rutas.getJSONObject(0);
                JSONArray legs = ruta.getJSONArray("legs");
                JSONObject leg = legs.getJSONObject(0);
                JSONArray steps = leg.getJSONArray("steps");

                // 4.- Recorrer los pasos y crear TextViews dinámicos
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step = steps.getJSONObject(i);

                    // Extraemos el modo de viaje (ej. WALK, TRANSIT)
                    // Usamos optString para que no falle si algún dato no existe
                    // Si no existe el dato, lo indicamos con "Desconocido"
                    String modoViaje = step.optString("travelMode", "Desconocido");
                    String textoFinal = "";

                    /* ------- CASO 1: CAMINANDO ------- */
                    if (modoViaje.equals("WALK")) {
                        // Extraemos la instrucción de navegación si existe
                        String instruccion = "Caminar";
                        if (step.has("navigationInstruction")) {
                            JSONObject navInstruction = step.getJSONObject("navigationInstruction");
                            instruccion = navInstruction.optString("instructions", "Caminar");
                        }

                        // ATENCIÓN: Si el SIGUIENTE paso es TRANSIT, quitamos lo de "El destino está..."
                        if (i < steps.length() - 1) { // Comprobamos que no sea el último paso
                            JSONObject siguientePaso = steps.getJSONObject(i + 1);
                            if (siguientePaso.optString("travelMode", "").equals("TRANSIT")) {
                                // Limpiamos el texto confuso. Ej: "Continúa por Av. Torneo El destino está a la izquierda."
                                // Nos quedamos solo con la primera parte antes del salto de línea
                                if (instruccion.contains("\nEl destino")) {
                                    instruccion = instruccion.substring(0, instruccion.indexOf("\nEl destino"));
                                    instruccion += "\n📍 Dirígete a la parada";
                                }
                            }
                        }

                        // Extraemos la distancia formateada si existe (ej.: "37 m", "6,4 km")
                        String distanciaStr = "";
                        if (step.has("localizedValues") && step.getJSONObject("localizedValues").has("distance")) {
                            distanciaStr = step.getJSONObject("localizedValues").getJSONObject("distance").optString("text", "N/A");
                        }
                        textoFinal = "🚶 " + instruccion;
                        if (!distanciaStr.isEmpty()) {
                            textoFinal += " (" + distanciaStr + ")";
                        }
                    }

                    // --- CASO 2: TRANSPORTE PÚBLICO (Bus, Metro, etc) ---
                    else if (modoViaje.equals("TRANSIT")) {
                        if (step.has("transitDetails")) {
                            JSONObject transit = step.getJSONObject("transitDetails");

                            // 1. Sacamos qué vehículo es y el número de línea (ej. Autobús 03)
                            String tipoVehiculo = "Transporte";
                            String numLinea = "";
                            if (transit.has("transitLine")) {
                                JSONObject lineaInfo = transit.getJSONObject("transitLine");
                                numLinea = lineaInfo.optString("nameShort", lineaInfo.optString("name", ""));
                                if (lineaInfo.has("vehicle") && lineaInfo.getJSONObject("vehicle").has("name")) {
                                    tipoVehiculo = lineaInfo.getJSONObject("vehicle").getJSONObject("name").optString("text", "Transporte");
                                }
                            }

                            textoFinal = "🚌 " + tipoVehiculo + " (Línea " + numLinea + ")\n";

                            // 2. Sacamos paradas y horarios
                            if (transit.has("stopDetails")) {
                                JSONObject stops = transit.getJSONObject("stopDetails");

                                String origenName = stops.has("departureStop") ? stops.getJSONObject("departureStop").optString("name", "Origen") : "Origen";
                                String destinoName = stops.has("arrivalStop") ? stops.getJSONObject("arrivalStop").optString("name", "Destino") : "Destino";

                                String horaSalida = "";
                                String horaLlegada = "";

                                if (transit.has("localizedValues")) {
                                    JSONObject locValues = transit.getJSONObject("localizedValues");
                                    if (locValues.has("departureTime") && locValues.getJSONObject("departureTime").has("time")) {
                                        horaSalida = locValues.getJSONObject("departureTime").getJSONObject("time").optString("text", "");
                                    }
                                    if (locValues.has("arrivalTime") && locValues.getJSONObject("arrivalTime").has("time")) {
                                        horaLlegada = locValues.getJSONObject("arrivalTime").getJSONObject("time").optString("text", "");
                                    }
                                }

                                textoFinal += "🟢 Sube en: " + origenName + " (" + horaSalida + ")\n";
                                textoFinal += "🔴 Baja en: " + destinoName + " (" + horaLlegada + ")";
                            }
                        } else {
                            textoFinal = "🚌 Coger transporte público";
                        }
                    }
                    // --- CASO 3: OTROS MODOS (Bici, coche, etc) ---
                    else {
                        textoFinal = "▶️ Paso " + (i + 1) + " [" + modoViaje + "]";
                    }

                    // II.- Creamos el TextView
                    TextView paso = new TextView(this);
                    paso.setText(textoFinal);
                    paso.setTextSize(16f);
                    paso.setPadding(0, 24, 0, 24);

                    // III.- Lo agregamos a nuestro LinearLayout en itinerario.xml
                    layoutResultados.addView(paso);

                    // IV. Agregamos una línea separadora
                    View separador = new View(this);
                    separador.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)); // 2 píxeles de alto
                     // color del separador
                    int miColor = ContextCompat.getColor(this, R.color.darker_gray);
                    separador.setBackgroundColor(miColor);
                    // V.- Agregamos el separador
                    layoutResultados.addView(separador);

                }
            } catch (Exception e) {
                Log.e("ItinerarioApp", "Error al parsear y mostrar los resultados", e);
                Toast.makeText(ItinerarioActivity.this, "Error al leer los datos de la ruta", Toast.LENGTH_SHORT).show();
            }



        });
    }
    // ---------- DEBUG ONLY ---------------------
    private void logLargo(String tag, String mensaje) {
        if (mensaje.length() > 3000) {
            Log.d(tag, mensaje.substring(0, 3000));
            logLargo(tag, mensaje.substring(3000));
        } else {
            Log.d(tag, mensaje);
        }
    }
}