package es.us.etsii_go;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.Image;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ItinerarioActivity extends AppCompatActivity {

    // VARIABLES GLOBALES
    private EditText origen, destino;
    private ImageButton botonFavOrigen, botonFavDestino, botonListaOrigen, botonListaDestino, botonGpsOrigen, botonIntercambiar;
    // Variables para controlar el GPS
    private double latitudGPS = 0.0;
    private double longitudGPS = 0.0;
    private boolean origenEsGPS = false;    // centinela por si el usuario ha introducido una dirección como coordenada GPS
    private boolean destinoEsGPS = false;   // centinela por si el usuario ha introducido una dirección como coordenada GPS

    // Persistencia de datos con SharedPreferences:
    /*
        Es un archivo XML que se oculta al usuario de Android, pero que es
        muy rápido de consultar. Por lo que ofrece persistencia de datos y rapidez de lectura
        a la par que seguridad. Funciona como un map con pares de clave-valor. Ejemplo sencillo:
        <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
    <map>
        <set name="lugares">
            <string>Plaza de Armas</string>
            <string>Hospital Virgen del Rocío</string>
            <string>ETSII</string>
        </set>
    </map>
     */
    private SharedPreferences preferencias;
    private ArrayList<String> listaFavoritos = new ArrayList<>();
    private Button botonCalcularRuta;
    private ScrollView scrollResultados;
    private LinearLayout layoutResultados;
    private RadioGroup grupoModoViaje;
    // Mapa para asociar: Un nombre a una LISTA de paradas (ida y vuelta)
    // Necesario para:
    /*
        El 'step' justo antes de llegar a la parada calcula la distancia ambas paradas
        de ida y vuelta, y se queda con la más cercana (la parada correcta). Así
        evitamos coger la parada equivocada.
     */
    private HashMap<String, ArrayList<ParadaTussam>> mapaParadasTussam = new HashMap<>();
    // Clase ParadaTussam para coger todos los datos necesarios:
    private static class ParadaTussam {
        String nodo; // la línea
        double lat; // coordenada latitud de la parada
        double lon; // coordenada longitud de la parada
        ParadaTussam(String nodo, double lat, double lon) {
            this.nodo = nodo;
            this.lat = lat;
            this.lon = lon;
        }
    }


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
     botonFavOrigen = findViewById(R.id.btn_fav_origen);
     botonFavDestino = findViewById(R.id.btn_fav_destino);
     botonListaOrigen = findViewById(R.id.btn_lista_origen);
     botonListaDestino = findViewById(R.id.btn_lista_destino);
     botonCalcularRuta = findViewById(R.id.boton_calcular_ruta);
     scrollResultados = findViewById(R.id.scroll_resultados);
     layoutResultados = findViewById(R.id.resultados_layout);
     grupoModoViaje = findViewById(R.id.grupo_modo_viaje);
     botonGpsOrigen = findViewById(R.id.btn_gps_origen);
     botonIntercambiar = findViewById(R.id.btn_intercambiar);

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

        // LISTENER de Intercambiar origen y destino:
        botonIntercambiar.setOnClickListener(v -> {
            // 1.- Primero intercambiamos las banderas si se trata de coordendas GPS (Evitar Condición de Carrera)
            boolean tempGPS = origenEsGPS;
            origenEsGPS = destinoEsGPS;
            destinoEsGPS = tempGPS;

            // 2.- Intercambiamos los textos
            String tempTexto = origen.getText().toString();
            origen.setText(destino.getText().toString());
            destino.setText(tempTexto);

            // ATENCIÓN: Las estrellas se actualizarán solas gracias al "watcher"
        });

        // LISTENER del Botón GPS:
        botonGpsOrigen.setOnClickListener(v -> obtenerUbicacionGPS());

        // Llamamos a cargarParadasTussam()
        cargarParadasTussam();

        // Invocamos la lógica de los favoritos:
        gestionarFavoritos();

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
                // ORIGEN: Determinar si es texto o coordenadas GPS
                JSONObject origenJSON = new JSONObject();
                if (origenEsGPS && origen.contains("ubicación")) {    // Si es GPS
                    JSONObject latLng = new JSONObject();
                    latLng.put("latitude", latitudGPS);
                    latLng.put("longitude", longitudGPS);
                    origenJSON.put("location", new JSONObject().put("latLng", latLng));
                } else {    // Si es texto normal
                    origenJSON.put("address", origen);
                }
                body.put("origin", origenJSON);

                // DESTINO: Determinar si es texto o coordenadas GPS (Por si el usuario le dio al botón de intercambiar)
                JSONObject destinoJSON = new JSONObject();
                if (destinoEsGPS && destino.contains("ubicación")) {  // Si es GPS
                    JSONObject latLng = new JSONObject();
                    latLng.put("latitude", latitudGPS);
                    latLng.put("longitude", longitudGPS);
                    destinoJSON.put("location", new JSONObject().put("latLng", latLng));
                } else {    // Si es texto normal
                    destinoJSON.put("address", destino);
                }
                body.put("destination", destinoJSON);

                body.put("travelMode", modoViajeAPI);  // Le pasamos directamente el modo de viaje seleccionado en el RadioButtom
                body.put("computeAlternativeRoutes", true); // Pedir todas las rutas disponibles

                OutputStream os = connection.getOutputStream(); // "Abrimos la tubería" para enviar los datos a Google
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));    // Pasamos a bytes los datos del usuario y los mandamos con write()
                os.close(); // Cerramos la tubería. Google ya puede procesar la solicitud

                // 3.- Leemos la respuesta (si es exitosa o el error si no)
                int responseCode = connection.getResponseCode();
                BufferedReader br;

                if (responseCode >= 200 && responseCode <= 229) {
                    // Respuesta exitosa, leemos el InputStream normalmente:
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

                                            // A. Rescatamos las coordenadas del origen
                                            double oriLat = 0, oriLon = 0;
                                            if (stops.has("departureStop") && stops.getJSONObject("departureStop").has("location")) {
                                                JSONObject loc = stops.getJSONObject("departureStop").getJSONObject("location").getJSONObject("latLng");
                                                oriLat = loc.optDouble("latitude", 0);
                                                oriLon = loc.optDouble("longitude", 0);
                                            }

                                            // B. Rescatamos las coordenadas del destino
                                            double destLat = 0, destLon = 0;
                                            if (stops.has("arrivalStop") && stops.getJSONObject("arrivalStop").has("location")) {
                                                JSONObject loc = stops.getJSONObject("arrivalStop").getJSONObject("location").getJSONObject("latLng");
                                                destLat = loc.optDouble("latitude", 0);
                                                destLon = loc.optDouble("longitude", 0);
                                            }

                                            // C. Extraemos línea
                                            String lineaBus = "";
                                            if (step.has("transitDetails")) {
                                                JSONObject transitDetails = step.getJSONObject("transitDetails");
                                                if (transitDetails.has("transitLine")) {
                                                    lineaBus = transitDetails.getJSONObject("transitLine").optString("nameShort", "").toLowerCase();
                                                }
                                            }

                                            // D. Buscamos la parada correcta con el desempate geográfico
                                            String claveOrigen = origenName.toLowerCase() + "_" + lineaBus;
                                            String numeroOrigen = obtenerNodoMasCercano(lineaBus, oriLat, oriLon);
                                            if (numeroOrigen != null) {
                                                origenName = origenName + " (Parada Nº " + numeroOrigen + ")";
                                            }

                                          //  String claveDestino = destinoName.toLowerCase() + "_" + lineaBus;
                                            String numeroDestino = obtenerNodoMasCercano(lineaBus, destLat, destLon);
                                            if (numeroDestino != null) {
                                                destinoName = destinoName + " (Parada Nº " + numeroDestino + ")";
                                            }

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

    private void cargarParadasTussam() {
        try {
            // Abrimos el CSV guardado en res/raw
            InputStream is = getResources().openRawResource(R.raw.paradas_tussam);  // Convertimos el raw en un objeto InputStream leíble
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));  // Creamos un buffer de lectura sobre el objeto y con una codificación de lectura

            String line;
            boolean primeraLinea = true;

            while ((line = reader.readLine()) != null) {
                // Saltamos la cabecera del archivo (son los nombres de las columnas)
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                // Separamos los campos de cada fila por el delimitador (coma)
                String[] columnas = line.split(",");
                if (columnas.length >=7) {  // Medida de seguridad para evitar que la app rebiente si el CSV tiene alguna línea al final

                    try {


                        // Extraemos coordenadas X e Y de la parada "en formato TUSSAM"
                        double x = Double.parseDouble(columnas[0].trim());  // Coordenada X
                        double y = Double.parseDouble(columnas[1].trim());  // Coordenada Y

                        // Fórmula para convertir las coordenadas Web Mercator a coordenadas GPS:
                        double lon = (x / 20037508.3427892) * 180.0;
                        double lat = Math.toDegrees(Math.atan(Math.exp(y / 6378137.0)) * 2.0 - Math.PI / 2.0);

                        String labelLinea = columnas[3].trim().toLowerCase(); // (ej. 03)
                        String nodo = columnas[5].trim(); // Número de la parada (ej. 879)
                       // String nombre = columnas[6].trim().toLowerCase(); // Nombre de la parada

                        // La clave es solo el número de la línea (ej. "03")
                        String claveSoloLinea = labelLinea;

                        // Si la clave no existe, creamos una nueva lista vacía
                        if (!mapaParadasTussam.containsKey(claveSoloLinea)) {
                            mapaParadasTussam.put(claveSoloLinea, new ArrayList<>());
                        }

                        // Agregamos esta parada a su lista correspondiente
                        mapaParadasTussam.get(claveSoloLinea).add(new ParadaTussam(nodo, lat, lon));

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            reader.close();
            Log.d("ItinerarioApp", "Cargadas " + mapaParadasTussam.size() + " combinaciones de paradas de TUSSAM.");

        } catch (Exception e) {
            String errorDetallado = e.toString();

            Log.e("ItinerarioApp", "Error al cargar el CSV de las paradas" + errorDetallado);

            e.printStackTrace();

        }
    }

    // Método que busca qué marquesina de autobús está más cerca de las coordenadas de Google
    private String obtenerNodoMasCercano(String clave, double targetLat, double targetLon) {
        // Devolvemos la lista con el par de paradas
        java.util.ArrayList<ParadaTussam> paradas = mapaParadasTussam.get(clave);
        if (paradas == null || paradas.isEmpty()) return null;

        ParadaTussam masCercana = null;
        float minDistancia = Float.MAX_VALUE;
        float[] resultadosDistancia = new float[1];

        for (ParadaTussam p : paradas) {
            // Android nos calcula la distancia en metros entre las dos coordenadas GPS
            android.location.Location.distanceBetween(targetLat, targetLon, p.lat, p.lon, resultadosDistancia);
            if (resultadosDistancia[0] < minDistancia) {
                minDistancia = resultadosDistancia[0];
                masCercana = p;
            }
        }
        return masCercana.nodo;
    }


    // Gestión de lugares favoritos:
    private void gestionarFavoritos() {
        // 1.- Cargamos los favoritos guardados en el móvil
        preferencias = getSharedPreferences("MisFavoritos", Context.MODE_PRIVATE);  // MODE_PRIVATE: Ninguna otra app puede leer las preferencias
        Set<String> setGuardados = preferencias.getStringSet("lugares", new HashSet<>());   //SharedPreferencies NO usa listas, sino Set (genial para evitar lugares repetidos)

        listaFavoritos.clear(); // Útil para evitar que se pierdan favoritos al meterlos y girar la pantalla
        listaFavoritos.addAll(setGuardados);

        // 2.- Listeners para guardar/quitar favorito (la estrella)
       botonFavOrigen.setOnClickListener(v -> {
           toggleFavorito(origen, botonFavOrigen);
       });

       botonFavDestino.setOnClickListener(v -> {
           toggleFavorito(destino, botonFavDestino);
       });

        // 3.- Listeners para abrir la lista de favoritos
        botonListaOrigen.setOnClickListener(v -> {
            mostrarDialogoFavoritos(origen, botonFavOrigen);
        });

        botonListaDestino.setOnClickListener(v -> {
            mostrarDialogoFavoritos(destino, botonFavDestino);
        });

        // 4.- Configuramos el "vigilante" para que la estrella se encienda al escribir
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                actualizarIconoEstrella(origen, botonFavOrigen);
                actualizarIconoEstrella(destino, botonFavDestino);

                // GPS: Si el usuario altera el texto y ya no pone "Mi ubicación", desactivamos el GPS
                if (!origen.getText().toString().contains("ubicación")) origenEsGPS = false;
                if (!destino.getText().toString().contains("ubicación")) destinoEsGPS = false;
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // no es necesario
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // no es necesario
            }
        };
        origen.addTextChangedListener(watcher);
        destino.addTextChangedListener(watcher);

    }

    // Método que verdaderamente "mete en favoritos" un lugar, se le pasa un campo y el botón de la estrella.
    private void toggleFavorito(EditText campo, android.widget.ImageButton boton) {
        String texto = campo.getText().toString().trim();

        // Si el campo está vacío, avisamos y cortamos la ejecución. No podemos guardar "Nada" como favorito
        if (texto.isEmpty()) {
            Toast.makeText(this, "Escribe una dirección primero", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si el campo es una ubicación GPS
        if (texto.contains("ubicación")) {
            Toast.makeText(this, "No puedes guardar el GPS como favorito. ¡Usa el botón de la diana!", Toast.LENGTH_LONG).show();
            return;
        }

        // Si el texto ya está en la lista de favoritos, lo borramos
        if (listaFavoritos.contains(texto)) {
            listaFavoritos.remove(texto);
            Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
        }
        // Si no está en la lista, lo añadimos
        else {
            listaFavoritos.add(texto);
            Toast.makeText(this, "Guardado en favoritos ⭐", Toast.LENGTH_SHORT).show();
        }

        // Guardamos la nueva lista en la memoria del móvil (haciendo la conversión obligatoria a Set)
        java.util.HashSet<String> nuevoSetParaGuardar = new java.util.HashSet<>(listaFavoritos);
        preferencias.edit().putStringSet("lugares", nuevoSetParaGuardar).apply();

        // Actualizamos el dibujo de la estrella
        actualizarIconoEstrella(campo, boton);
    }

    // Actualiza el estado visual de la estrella:
    /*
        Si estrella está apagada:
                    el valor que el user está escribiendo en un campo NO existe en la lista de favoritos.
        Si estrella está encendida:
                    el valor que el user está escribiendo en un campo SÍ existe en la lista de favoritos.
     */
    private void actualizarIconoEstrella(EditText campo, android.widget.ImageButton boton) {
        String texto = campo.getText().toString().trim();

        // Si el campo está vacío, la estrella se apaga
        if (texto.isEmpty()) {
            boton.setImageResource(android.R.drawable.btn_star_big_off);
        }
        // Si lo que hay escrito coincide con un favorito, la estrella se enciende
        else if (listaFavoritos.contains(texto)) {
            boton.setImageResource(android.R.drawable.btn_star_big_on);
        }
        // En cualquier otro caso (texto nuevo no guardado), la estrella se apaga
        else {
            boton.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    private void mostrarDialogoFavoritos(EditText campoDestino, ImageButton botonEstrella) {
        if (listaFavoritos.isEmpty()) {
            Toast.makeText(this, "Aún no tienes favoritos guardados", Toast.LENGTH_SHORT).show();
            return; // Corta la ejecución de este método, no sigue leyendo las líneas de abajo. Es como un break; pero para los if-else
        }

        // Creamos una lista de Android de toda la vida (ListView)
        ListView listView = new ListView(this);
        // ArrayAdapter hace de traductor entre el listView visual y los elementos de la lista escrita en Java. listView no sabe de Java.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaFavoritos);
        listView.setAdapter(adapter);

        // Creamos la ventana emergente
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Mis Favoritos")
                .setMessage("Toca para elegir.\nMantén pulsado para borrar.")
                .setView(listView)
                .setNegativeButton("Cerrar", null)
                .create();

        // ACCIÓN 1: Click corto -> Elegir un favorito de la lista y ponerlo en el EditText
        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            String seleccionado = listaFavoritos.get(i);
            campoDestino.setText(seleccionado); // Rellena el texto
            dialogo.dismiss(); // Cierra el menú
        });

        // ACCIÓN 2: Click largo -> Borrar el favorito seleccionado
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Borramos el elemento seleccionado de la lista
                listaFavoritos.remove(i);

                // Guardamos la nueva lista en la "memoria"
                HashSet<String> nuevoSet = new HashSet<>(listaFavoritos);
                preferencias.edit().putStringSet("lugares", nuevoSet).apply();

                // Avisamos a la ventana emergente para que se refresque:
                adapter.notifyDataSetChanged();

                // Actualizamos las estrellas por si teníamos ese lugar escrito
                actualizarIconoEstrella(origen, botonFavOrigen);
                actualizarIconoEstrella(destino, botonFavDestino);

                Toast.makeText(ItinerarioActivity.this, "Favorito eliminado \uD83D\uDDD1\uFE0F", Toast.LENGTH_SHORT).show();

                // Cerramos la ventana si borramos el último favorito que existe en la listview
                if (listaFavoritos.isEmpty()) {
                    dialogo.dismiss();
                }

                return true;    // Medida de seguridad: Le decimos a Android que nosotros gestionamos lo que sucede con el click largo, él no hace nada más.
            }
        });

        dialogo.show();

    }

    private void obtenerUbicacionGPS() {
        // 1.- Comprobamos si el usuario nos ha dado permiso de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si no lo tiene, se lo pedimos:
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
            return;
        }

        // 2.- Si hay permiso, encendemos el Localizador de Android
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            // Pedimos la última ubicación conocida
            android.location.Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) { // Si falla, probamos a detectar la localización por antenas WiFi/datos móviles
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) { // Pero si tenemos coordenadas, proseguimos:
                latitudGPS = location.getLatitude();
                longitudGPS = location.getLongitude();
                origenEsGPS = true; // Activamos la bandera
                origen.setText("📍 Mi ubicación actual");
                Toast.makeText(this, "Ubicación encontrada", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación. Activa el GPS.", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
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