package es.us.etsii_go;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class ItinerarioActivity extends AppCompatActivity {

    // VARIABLES GLOBALES
    private EditText origen, destino;
    private ImageButton botonFavOrigen, botonFavDestino, botonListaOrigen, botonListaDestino, botonGpsOrigen, botonGpsDestino, botonIntercambiar;
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
    private ArrayList<Favorito> listaFavoritos = new ArrayList<>();

    // Clase Favorito para almacenar toda la información sobre los lugares favoritos:
    private static class Favorito {
        String alias;   // El nombre que le pone el usuario a un lugar (ej. "Casa")
        String direccion;   // La dirección real a texto o "Mi ubicación actual" si es con coordenadas GPS
        boolean esGps;
        double lat;
        double lon;

        Favorito(String alias, String direccion, boolean esGps, double lat, double lon) {
            this.alias = alias;
            this.direccion = direccion;
            this.esGps = esGps;
            this.lat = lat;
            this.lon = lon;
        }
    }
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

    // Freno para el TextWatcher: Evitamos bucle infinito al comprobar el texto y comparar con los favoritos
    private boolean modificandoTextoAutomatico = false;

    /* * AUTOCOMPLETADO: LANZADORES DE GOOGLE PLACES
     * En Android moderno, usamos ActivityResultLauncher para abrir una pantalla
     * y quedarnos esperando su resultado.
     * Estos "vigilantes" se despiertan cuando la pantalla de búsqueda de Google se cierra.
     * Comprueban si todo ha ido bien (RESULT_OK) y, si es así, extraen la dirección
     * seleccionada por el usuario y la pegan en la caja de texto (origen o destino).
     */
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> launcherAutocompleteOrigen =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    com.google.android.libraries.places.api.model.Place place = com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(result.getData());
                    origen.setText(place.getAddress()); // Escribimos la calle en tu campo origen
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> launcherAutocompleteDestino =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    com.google.android.libraries.places.api.model.Place place = com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(result.getData());
                    destino.setText(place.getAddress()); // Escribimos la calle en tu campo destino
                }
            });

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
     botonGpsDestino = findViewById(R.id.btn_gps_destino);
     botonIntercambiar = findViewById(R.id.btn_intercambiar);

        /*
         * AUTOCOMPLETADO: INICIALIZACIÓN DE LA API DE PLACES
         * Antes de usar el buscador de calles, tenemos que "despertar" a la librería
         * y autenticarnos con nuestra API Key. Si no hacemos esto, la app crasheará
         * al intentar abrir la ventana de autocompletado.
         */
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            com.google.android.libraries.places.api.Places.initialize(getApplicationContext(), BuildConfig.API_KEY_GOOGLE);
        }

        /*
         * AUTOCOMPLETADO: CONFIGURACIÓN DE LAS CAJAS DE TEXTO (ORIGEN Y DESTINO)
         * Desactivamos el teclado nativo de Android (setFocusable(false)) para que
         * el usuario no pueda escribir a mano. En su lugar, al tocar la caja,
         * abrimos la ventana superpuesta (OVERLAY) del buscador oficial de Google.
         */
        origen.setFocusable(false); // Evita que se abra el teclado nativo
        origen.setOnClickListener(v -> abrirBuscadorGoogle(launcherAutocompleteOrigen));

        destino.setFocusable(false);
        destino.setOnClickListener(v -> abrirBuscadorGoogle(launcherAutocompleteDestino));


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

        // LISTENERS botones Diana del GPS:
        botonGpsOrigen.setOnClickListener(v -> obtenerUbicacionGPS(true));
        botonGpsDestino.setOnClickListener(v -> obtenerUbicacionGPS(false));

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
                // ---- ORIGEN ----
                JSONObject origenJSON = new JSONObject();
                Favorito favOrigen = buscarFavoritoPorAlias(origen);

                    // Caso 1: GPS normal (El usuario acaba de darle a la diana, aparece el texto ""📍 Mi ubicación actual")
                if (origenEsGPS && origen.contains("ubicación")) {    // Si es GPS
                    JSONObject latLng = new JSONObject();
                    latLng.put("latitude", latitudGPS);
                    latLng.put("longitude", longitudGPS);
                    origenJSON.put("location", new JSONObject().put("latLng", latLng));

                    // Caso 2: GPS favorito (El usuario ha seleccionado "Casa" de la lista de favoritos
                } else if (favOrigen != null && favOrigen.esGps){
                    JSONObject latLng = new JSONObject();
                    latLng.put("latitude", favOrigen.lat);
                    latLng.put("longitude", favOrigen.lon);
                    origenJSON.put("location", new JSONObject().put("latLng", latLng));

                    // Caso 3: Texto normal o Favorito de texto renombrado (ej. "Uni" -> "Av. Reina Mercedes")
                    // Si es favorito mandamos su dirección real oculta, si no, mandamos lo que haya escrito.
                } else {
                    String textoAEnviar;
                   if (favOrigen != null) {
                       textoAEnviar = favOrigen.direccion;
                   } else {
                       textoAEnviar = origen;
                   }
                   origenJSON.put("address", textoAEnviar);
                }
                body.put("origin", origenJSON);

                // ---- DESTINO ----
                JSONObject destinoJSON = new JSONObject();
                Favorito favDestino = buscarFavoritoPorAlias(destino);

                if (destinoEsGPS && destino.contains("ubicación")) {  // Si es GPS
                    JSONObject latLng = new JSONObject();
                    latLng.put("latitude", latitudGPS);
                    latLng.put("longitude", longitudGPS);
                    destinoJSON.put("location", new JSONObject().put("latLng", latLng));
                } else if (favDestino != null && favDestino.esGps) {
                    JSONObject latLng = new JSONObject();
                    latLng.put("latitude", favDestino.lat);
                    latLng.put("longitude", favDestino.lon);
                    destinoJSON.put("location", new JSONObject().put("latLng", latLng));
                } else {
                    String textoAEnviar;
                    if (favDestino != null) {
                        textoAEnviar = favDestino.direccion;
                    } else {
                        textoAEnviar = destino;
                    }
                    destinoJSON.put("address", textoAEnviar);
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
        cargarFavoritosDesdeMemoria();


        // 2.- Listeners para guardar/quitar favorito (la estrella)
       botonFavOrigen.setOnClickListener(v -> {
           toggleFavorito(origen, botonFavOrigen, true);
       });

       botonFavDestino.setOnClickListener(v -> {
           toggleFavorito(destino, botonFavDestino, false);
       });

        // 3.- Listeners para abrir la lista de favoritos
        botonListaOrigen.setOnClickListener(v -> {
            mostrarDialogoFavoritos(origen, botonFavOrigen, true);
        });

        botonListaDestino.setOnClickListener(v -> {
            mostrarDialogoFavoritos(destino, botonFavDestino, false);
        });

        // 4.- Configuramos el "vigilante" para que la estrella se encienda al escribir
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                // Si la app está cambiando el texto "automáticamente", NOS PARAMOS. ¡Salimos de este método!
                if (modificandoTextoAutomatico) {
                    return;
                }
                modificandoTextoAutomatico = true;  // Si no, entonces entramos y cerramos el paso detrás nuestro. ¡Podemos hacer comprobaciones!

                // ---- COMPROBAMOS ORIGEN ----
                Favorito favOrigen = actualizarIconoEstrella(origen, botonFavOrigen, true);

                // Si encontramos un favorito y el campo NO tiene un Alias, lo cambiamos.
                if (favOrigen != null && !origen.getText().toString().trim().equals(favOrigen.alias)) {
                    origen.setText(favOrigen.alias);
                    origen.setSelection(favOrigen.alias.length());  // Ponemos el cursor parpadeando al final de la palabra
                    Toast.makeText(ItinerarioActivity.this, "📍 Detectado: Ubicación guardada como '"+ favOrigen.alias + "'", Toast.LENGTH_SHORT ).show();
                }

                // ---- COMPROBAMOS DESTINO ----
                Favorito favDestino = actualizarIconoEstrella(destino, botonFavDestino, false);

                // Si encontramos un favorito y el campo NO tiene un Alias, lo cambiamos.
                if (favDestino != null && !destino.getText().toString().trim().equals(favDestino.alias)) {
                    destino.setText(favDestino.alias);
                    destino.setSelection(favDestino.alias.length());  // Ponemos el cursor parpadeando al final de la palabra
                    Toast.makeText(ItinerarioActivity.this, "📍 Detectado: Ubicación guardada como '"+ favDestino.alias + "'", Toast.LENGTH_SHORT ).show();
                }


                // GPS: Si el usuario altera el texto y ya no aparece el texto por defecto con la palabra "ubicación", desactivamos el GPS
                if (!origen.getText().toString().contains("ubicación")) origenEsGPS = false;
                if (!destino.getText().toString().contains("ubicación")) destinoEsGPS = false;

                modificandoTextoAutomatico = false; // Ya hemos terminado de hacer las comprobaciones, abrimos el paso.
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
    private void toggleFavorito(EditText campo, android.widget.ImageButton boton, boolean esCampoOrigen) {
        String texto = campo.getText().toString().trim();

        // Si el campo está vacío, avisamos y cortamos la ejecución. No podemos guardar "Nada" como favorito
        if (texto.isEmpty()) {
            Toast.makeText(this, "Escribe o detecta una dirección primero", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1.- Buscamos si ya existe en la lista para borrarlo
        for (int i = 0; i < listaFavoritos.size(); i ++) {
            if (listaFavoritos.get(i).alias.equals(texto) || listaFavoritos.get(i).direccion.equals(texto)) {
                listaFavoritos.remove(i);
                guardarFavoritosEnMemoria();
                actualizarIconoEstrella(campo, boton, esCampoOrigen);
                Toast.makeText(this, "Favorito eliminado 🗑️", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 2.- Si NO existe, lo añadimos
        boolean esGps;
        double lat, lon;

        // Preguntamos primero si las coordenadas introducidas están en el campo origen o destino:
        if (esCampoOrigen) {    // SI están en el CAMPO ORIGEN, activamos bandera de origen y guardamos coordenadas
            esGps = origenEsGPS;
            lat = latitudGPS;
            lon = longitudGPS;
        } else {    // SI están en el CAMPO DESTINO, activamos la bandera de destino y las guardamos coordenadas
            esGps = destinoEsGPS;
            lat = latitudGPS;
            lon = longitudGPS;
        }

        if (esGps) {     // Si se trata de una coordenada
            // Obligamos al usuario a ponerle un nombre (ej. "Gimnasio")
            // De esta manera, evitamos que en otro sitio distinto al de las coordenadas guardadas, se piense que son sus coordenadas reales (de su nuevo sitio).
            EditText input = new EditText(this);
            input.setHint("Ej. Casa, Trabajo, Gimnasio...");
            new AlertDialog.Builder(this)
                    .setTitle("Ponle un nombre a esta ubicación")
                    .setMessage("Vas a guardar tus coordenadas actuales. ¿Qué nombre le ponemos?")
                    .setView(input)
                    .setPositiveButton("Guardar", (dialog, which) -> {
                        String alias = input.getText().toString().trim();
                        if (!alias.isEmpty()) {
                            listaFavoritos.add(new Favorito(alias, texto, true, lat, lon));
                            guardarFavoritosEnMemoria();
                            campo.setText(alias);   // Ponemos el nombre corto en el campo
                            actualizarIconoEstrella(campo, boton, esCampoOrigen);
                            Toast.makeText(this, "Coordenadas guardadas ⭐", Toast.LENGTH_SHORT).show();
                        }
                    }).setNegativeButton("Cancelar", null).show();
        } else {
            // Si es texto normal, lo guardamos directamente, usando la calle como Alias por defecto
            listaFavoritos.add(new Favorito(texto, texto, false, 0,0));
            guardarFavoritosEnMemoria();
            actualizarIconoEstrella(campo, boton, esCampoOrigen);
            Toast.makeText(this, "Guardado en favoritos ⭐", Toast.LENGTH_SHORT).show();
        }

    }

    // Actualiza el estado visual de la estrella:
    /*
        Si estrella está apagada:
                    el valor que el user está escribiendo en un campo NO existe en la lista de favoritos.
        Si estrella está encendida:
                    el valor que el user está escribiendo en un campo SÍ existe en la lista de favoritos.
     */

    private Favorito buscarFavoritoPorAlias(String alias) {
        for (Favorito f : listaFavoritos) {
            if (f.alias.equals(alias)) {
                return f; // Hemos encontrado el favorito con el alias con que lo guardó el usuario
            }
        }

        return null;    // No está en favoritos, es texto normal
    }
    private Favorito actualizarIconoEstrella(EditText campo, android.widget.ImageButton boton, boolean esCampoOrigen) {
        String texto = campo.getText().toString().trim();
        Favorito encontrado = null;

        // Comprobamos si el texto escrito coincide con algún Alias o Dirección de nuestros favoritos
        for (Favorito f: listaFavoritos) {
            // 1.- Coincidencia por Alias (ej. el usuario escribió "Casa" a mano)
            if (f.alias.equals(texto)){
                encontrado = f;
                break;
            }

            // 2. Coincidencia por Dirección o GPS
            if (f.direccion.equals(texto)) {
                if (f.esGps) {
                    // Si se ha presionado la Diana, comprobamos si físicamente estamos a menos de 100 metros de ese favorito.
                    // EVITAMOS QUE: Si tienes guardado "Casa" y pulsas el GPS en la Universidad, no se confundirán.
                    float[] distancia = new float[1];
                    Location.distanceBetween(latitudGPS, longitudGPS, f.lat, f.lon, distancia);

                    if (distancia[0] <= 100.0) {    // Si la ubicación GPS introducida al pinchar en la diana está a 100 metros del favorito
                        encontrado = f; // Hemos encontrado el favorito guardado
                        break;

                    }

                } else { // Si no se ha presionado la Diana, es texto manual (ej. "Avenida Reina Mercedes)
                    encontrado = f;
                    break;

                }
            }
        }

        if (encontrado != null) {   // Si es una favorito, encendemos la estrella
            boton.setImageResource(android.R.drawable.btn_star_big_on);
        } else {    // En caso contrario, la mantenemos apagada
            boton.setImageResource(android.R.drawable.btn_star_big_off);
        }

        return encontrado; // Devolvemos el Favorito encontrado para que el TextWatcher pueda leer su nombre y pintarlo.
    }

    private void mostrarDialogoFavoritos(EditText campo, ImageButton botonEstrella, boolean esCampoOrigen) {
        if (listaFavoritos.isEmpty()) {
            Toast.makeText(this, "Aún no tienes favoritos guardados", Toast.LENGTH_SHORT).show();
            return; // Corta la ejecución de este método, no sigue leyendo las líneas de abajo. Es como un break; pero para los if-else
        }

        // Creamos una lista de Android de toda la vida (ListView)
        ListView listView = new ListView(this);

        // Creamos la ventana emergente
        AlertDialog dialogo = new AlertDialog.Builder(this)
                .setTitle("Mis Favoritos")
                .setMessage("Toca para elegir.\nMantén pulsado para borrar.")
                .setView(listView)
                .setNegativeButton("Cerrar", null)
                .create();

        // Creamos un adaptador personalizado para separar los items de la listview del botón de editar
        ArrayAdapter<Favorito> adapter = new ArrayAdapter<Favorito>(this, R.layout.item_favorito, listaFavoritos) {
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_favorito, parent, false);
                }

                Favorito fav = getItem(position);
                TextView textoAlias = convertView.findViewById(R.id.texto_alias_favorito);
                ImageButton botonEditar = convertView.findViewById(R.id.btn_editar_favorito);

                textoAlias.setText(fav.alias);

                // ACCIÓN DEL LÁPIZ: Editar el nombre del favorito
                botonEditar.setOnClickListener(v -> {
                    EditText inputEditar = new EditText(getContext());
                    inputEditar.setText(fav.alias);
                    new AlertDialog.Builder(getContext())
                            .setTitle("Renombrar Favorito")
                            .setView(inputEditar)
                            .setPositiveButton("Guardar", (d, w) -> {
                                fav.alias = inputEditar.getText().toString().trim();
                                guardarFavoritosEnMemoria();
                                notifyDataSetChanged(); // Refresca la lista de los favoritos
                                actualizarIconoEstrella(origen, botonFavOrigen, esCampoOrigen);
                                actualizarIconoEstrella(destino, botonFavDestino, esCampoOrigen);
                            }).setNegativeButton("Cancelar", null).show();
                });

                // ACCIÓN 1: Click corto -> Elegir un favorito de la lista y ponerlo en el EditText
                convertView.setOnClickListener(view -> {
                    campo.setText(fav.alias);

                    // Si el favorito era una coordenada GPS, recargamos sus coordenadas en las variables globales
                    if (esCampoOrigen) {    // Comprobamos si el usuario ha seleccionado el fav para el campo origen
                        origenEsGPS = fav.esGps;
                        if (origenEsGPS) {  // Preguntamos si el fav es una coordenada y nos guardamos sus valores
                            latitudGPS = fav.lat;
                            longitudGPS = fav.lon;
                        }
                    } else {    // Comprobamos si el usuario ha seleccionado el fav para el campo destino
                        destinoEsGPS = fav.esGps;
                        if (destinoEsGPS) { // Preguntamos si el fav es una coordenada y nos guardamos sus valores
                            latitudGPS = fav.lat;
                            longitudGPS = fav.lon;
                        }
                    }

                    dialogo.dismiss();  // Cerramos la ventana
                });

                // ACCIÓN 2: Click largo -> Borrar el favorito seleccionado
                convertView.setOnLongClickListener(view -> {
                    listaFavoritos.remove(position);
                    guardarFavoritosEnMemoria();
                    notifyDataSetChanged(); // Refrescamos la lsita de favoritos
                    actualizarIconoEstrella(origen, botonFavOrigen, esCampoOrigen);
                    actualizarIconoEstrella(destino, botonFavDestino, esCampoOrigen);

                    Toast.makeText(ItinerarioActivity.this, "Favorito eliminado 🗑️", Toast.LENGTH_SHORT).show();

                    // Si borramos el único favorito que existe en la listview, cerramos automáticamente la ventana AlertDialog
                    if (listaFavoritos.isEmpty()) {
                        dialogo.dismiss();
                    }
                    return true;     // Medida de seguridad: Le decimos a Android que nosotros gestionamos lo que sucede con el click largo, él no hace nada más.
                });


                return convertView; // Devolvemos la fila montada
            }
        };

        listView.setAdapter(adapter);

        dialogo.show();

    }

    // Método para obtener la ubicación GPS al pulsar el icono de la diana:
    // Le pasamos TRUE por parámetro al método si se pulsó la diana de ORIGEN, FALSE si se pulsó la de DESTINO
    private void obtenerUbicacionGPS(boolean esParaOrigen) {

        // 0.- Comprobamos si el usuario nos ha dado permiso de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Si no lo tiene, se lo pedimos:
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 100);
            return;
        }

        Toast.makeText(this, "Buscando su ubicación. Espere, por favor...", Toast.LENGTH_SHORT).show();

        // 1.- Instanciamos el cliente de Google:
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // 2. Pedimos la ubicación actual (getCurrentLocation ignora el historial y fuerza una lectura nueva)
        // Usamos PRIORITY_HIGH_ACCURACY para decirle a Google que queremos la máxima precisión (GPS + WiFi + Bluetooth)
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    // Cogemos las coordenadas y activamos el centinela
                    if (location != null) {
                        latitudGPS = location.getLatitude();
                        longitudGPS = location.getLongitude();

                        // Decidimos a qué campo se lo asignamos
                        if (esParaOrigen) {
                            origenEsGPS = true;
                            origen.setText("📍 Mi ubicación actual");     // Actualizamos la caja de texto
                        } else {
                            destinoEsGPS = true;
                            destino.setText("📍 Mi ubicación actual");    // Actualizamos la caja de texto
                        }

                        Toast.makeText(ItinerarioActivity.this, "¡Ubicación exacta encontrada!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ItinerarioActivity.this, "Asegúrate de tener el GPS encendido en los ajustes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- MÉTODOS DE CONVERSIÓN A JSON (FAVORITOS) ---
    private void guardarFavoritosEnMemoria() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Favorito f: listaFavoritos) {
                JSONObject obj = new JSONObject();
                obj.put("alias", f.alias);
                obj.put("direccion", f.direccion);
                obj.put("esGps", f.esGps);
                obj.put("lat", f.lat);
                obj.put("lon", f.lon);
                jsonArray.put(obj);

            }

            preferencias.edit().putString("favoritos_json", jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarFavoritosDesdeMemoria() {
        listaFavoritos.clear();
        String jsonString = preferencias.getString("favoritos_json", "[]");
        try {
            JSONArray jsonArray = new JSONArray(jsonString);    // ATENCIÓN: Si no se le pasa el jsonString, la lista de favoritos se destruye cada vez que ingresamos en la actividad
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                listaFavoritos.add(new Favorito(
                        obj.getString("alias"),
                        obj.getString("direccion"),
                        obj.getBoolean("esGps"),
                        obj.getDouble("lat"),
                        obj.getDouble("lon")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Método encargado de configurar y abrir la pantalla predictiva de Google.
     * Google se encarga internamente de gestionar los "Tokens de Sesión" para
     * no cobrarnos por cada letra tecleada, sino solo por la búsqueda final.
     */
    private void abrirBuscadorGoogle(androidx.activity.result.ActivityResultLauncher<android.content.Intent> launcher) {
        // 1. Elegimos qué datos queremos que nos devuelva Google.
        // Solo pedimos ID, Nombre y Dirección para ahorrar costes (no pedimos fotos ni horarios).
        java.util.List<com.google.android.libraries.places.api.model.Place.Field> fields = java.util.Arrays.asList(
                com.google.android.libraries.places.api.model.Place.Field.ID,
                com.google.android.libraries.places.api.model.Place.Field.NAME,
                com.google.android.libraries.places.api.model.Place.Field.ADDRESS
        );

        // 2. Construimos la ventana (Intent) en modo OVERLAY (semitransparente por encima de la app)
        // y limitamos las búsquedas exclusivamente a España ("ES") para mayor precisión.
        android.content.Intent intent = new com.google.android.libraries.places.widget.Autocomplete.IntentBuilder(
                com.google.android.libraries.places.widget.model.AutocompleteActivityMode.OVERLAY, fields)
                .setCountries(Collections.singletonList("ES"))  // Restringir los resultados a dentro de España. setCountries() pide una lista, creamos una de un solo elemento
                .build(this);

        // 3. Lanzamos la pantalla
        launcher.launch(intent);
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