package es.us.etsii_go;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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
import java.util.Locale;

public class ItinerarioActivity extends AppCompatActivity {

    // VARIABLES GLOBALES
    private EditText origen, destino;
    private Button botonCalcularRuta;
    private ScrollView scrollResultados;
    private LinearLayout layoutResultados;
    private RadioGroup grupoModoViaje;
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
     grupoModoViaje = findViewById(R.id.grupo_modo_viaje);

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

            // Averiguamos el botón que presionó el usuario del RadioButtom
            String modoViajeAPI = "TRANSIT"; // Opción por defecto
            int idSeleccionado = grupoModoViaje.getCheckedRadioButtonId();

            if (idSeleccionado == R.id.radio_coche) {
                modoViajeAPI = "DRIVE";
            } else if (idSeleccionado == R.id.radio_andar_solo) {
                modoViajeAPI = "WALK";
                
            }

            // Llamamos a la API en un hilo secundario
            calcularRuta(origen_input, destino_input, modoViajeAPI);

        });

    }

    // MÉTODOS PERSONALES
    private void calcularRuta(String origen, String destino, String modoViajeAPI) {

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
                body.put("travelMode", modoViajeAPI);  // Le pasamos directamente el modo de viaje seleccionado en el RadioButtom
                body.put("computeAlternativeRoutes", true); // Pedir todas las rutas disponibles

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
                String errorDetallado = e.toString();

                Log.e("ItinerarioApp", "Error en la conexión API" + errorDetallado);

                e.printStackTrace();

                runOnUiThread(() -> Toast.makeText(ItinerarioActivity.this, "Error de conexión" + e.getMessage(), Toast.LENGTH_SHORT).show());
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

                // 3.- Parseo del JSON principal
                JSONObject jsonObject = new JSONObject(jsonResponse);
                JSONArray rutas = jsonObject.optJSONArray("routes");

                if (rutas == null || rutas.length() == 0) {
                    Toast.makeText(this, "No se ha encontrado una ruta disponible", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 4.- Iteramos sobre TODAS las rutas disponibles
                for (int r = 0; r < rutas.length(); r++) {
                    JSONObject ruta = rutas.getJSONObject(r);

                    // --- A. Extraer datos para el botón cabecera (Duración y Distancia) ---
                    // La duración la devuelven en segundos (ej. "1800s"), le quitamos esa 's' y pasamos el tiempo a minutos
                    String duracionStr = ruta.optString("duration", "0s").replace("s","");
                    int durationMinutos = (int) (Float.parseFloat(duracionStr) / 60);
                    // La distancia viene en metros, para el trayecto completo hay que usar kilómetros.
                    int distanciaMetros = ruta.optInt("distanceMeters", 0);
                    String distanciaKm = String.format(Locale.getDefault(), "%.1f", distanciaMetros / 1000.0);

                    String tituloRuta = "📍 Opción " + (r + 1) + " (" + durationMinutos + " min, " + distanciaKm + " km)";

                    // --- B. Crear el Botón Desplegable ---
                    Button botonCabecera = new Button(this);
                    botonCabecera.setText(tituloRuta);
                    botonCabecera.setAllCaps(false);    // Evitamos que el texto del botón aparezca en mayúsculas
                    botonCabecera.setTextSize(16f);

                    // Añadimos margen superior entre los botones
                    LinearLayout.LayoutParams paramsBoton = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    paramsBoton.setMargins(0, (r == 0) ? 0: 32, 0,0 );
                    botonCabecera.setLayoutParams(paramsBoton);

                    // --- C. Crear el contenedor para los pasos de ESTA ruta ---
                    LinearLayout contenedorPasos = new LinearLayout(this);
                    contenedorPasos.setOrientation(LinearLayout.VERTICAL);
                    contenedorPasos.setVisibility(View.GONE);
                    contenedorPasos.setPadding(32, 16, 32, 16);

                    // --- D. Lógica de Mostrar/Ocultar ---
                    botonCabecera.setOnClickListener(v -> {
                        if (contenedorPasos.getVisibility() == View.GONE) {
                            contenedorPasos.setVisibility(View.VISIBLE);
                        } else {
                            contenedorPasos.setVisibility(View.GONE);
                        }
                    });
                    // Añadimos el botón al layout principal ANTES de crear el contenedor de pasos
                    layoutResultados.addView(botonCabecera);

                    // --- E. Extraer y pintar los pasos dentro del contenedor oculto ---
                    JSONArray legs = ruta.optJSONArray("legs");
                    if (legs != null && legs.length() > 0) {
                        JSONObject leg = legs.getJSONObject(0);
                        JSONArray steps = leg.optJSONArray("steps");

                        if (steps != null) {
                            // 4.- Recorrer los pasos y crear TextViews dinámicos
                            for (int i = 0; i < steps.length(); i++) {
                                JSONObject step = steps.getJSONObject(i);

                                // Extraemos el modo de viaje (ej. WALK, TRANSIT)
                                // Usamos optString para que no falle si algún dato no existe
                                // Si no existe el dato, lo indicamos con "Desconocido"
                                String modoViaje = step.optString("travelMode", "Desconocido");
                                String textoFinal = "";

                                /* ------- CASO 1: CAMINANDO O COCHE ------- */
                                if (modoViaje.equals("WALK") || modoViaje.equals("DRIVE")) {
                                    // Extraemos la instrucción de navegación si existe
                                    String instruccion = modoViaje.equals("WALK") ? "Caminar": "Conducir";
                                    if (step.has("navigationInstruction")) {
                                        JSONObject navInstruction = step.getJSONObject("navigationInstruction");
                                        instruccion = navInstruction.optString("instructions", instruccion);
                                    }

                                    // Limpiamos el texto de destino ".\nVía de uso restringido\n". Esto es por que
                                    // cuando pasas por calles peatonales, zonas de bajas emisiones o carreteras de peaje.
                                    instruccion = instruccion.replace(".\nVía de uso restringido\n", ".").replace("\nVía de uso restringido","").replace("Vía de uso restringido","");

                                    // Limpieza de texto si el siguiente paso es TRANSIT (solo útil si se ha seleccionado "Andar")
                                    if (modoViaje.equals("WALK") && i < steps.length() - 1 ) { // Comprobamos además de que no sea el último paso
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

                                    // Ponemos un emoji distinto según el modo
                                    String emoji =  modoViaje.equals("WALK") ? "🚶 ": "🚗";
                                    textoFinal = emoji + " " + instruccion;
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

                                // II.- Creamos el TextView para este paso
                                TextView paso = new TextView(this);
                                paso.setText(textoFinal);
                                paso.setTextSize(15f);
                                paso.setPadding(0, 16, 0, 16);

                                // III.- ATENCIÓN: Ahora lo agregamos a "contenedorPasos", NO a layoutResultados
                                contenedorPasos.addView(paso);

                                // IV. Agregamos una línea separadora
                                View separador = new View(this);
                                separador.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2)); // 2 píxeles de alto
                                // color del separador
                                int miColor = ContextCompat.getColor(this, R.color.darker_gray);
                                separador.setBackgroundColor(miColor);



                            }
                    }


                }
                    // --- F. Finalmente, añadimos el contenedor de pasos (oculto) al layout principal ---
                    layoutResultados.addView(contenedorPasos);

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