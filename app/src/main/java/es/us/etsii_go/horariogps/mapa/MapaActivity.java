package es.us.etsii_go.horariogps.mapa;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import es.us.etsii_go.R;
import es.us.etsii_go.horariogps.horario.aulas.RepositorioAulas;
import es.us.etsii_go.horariogps.horario.models.Aula;
import es.us.etsii_go.horariogps.mapa.escaneo.ScannerActivity;
import es.us.etsii_go.horariogps.mapa.models.ModeloDatosWifi;
import es.us.etsii_go.horariogps.mapa.ubicacion.WifiPositioningManager;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class MapaActivity extends AppCompatActivity {
    private MapaView mapaView;
    private WifiManager wifiManager;
    private WifiPositioningManager positioningManager;
    private Handler handler = new Handler();
    private Runnable wifiScannerRunnable;
    private final int FRECUENCIA_MS = 3000;

    // Coordenadas actuales
    private PointF posUsuario = new PointF(-1f, -1f);
    private Aula aulaSeleccionada;
    private int colorPuntoAula;
    private ValueAnimator dotAnimator;

    private ExtendedFloatingActionButton btnEscaneo;
    private ExtendedFloatingActionButton btnUbicacion;
    private boolean bucleEnMarcha = false;


    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                List<ScanResult> results = wifiManager.getScanResults();


                //float[] coords = positioningManager.calculatePositionKNN(results);
                float[] coords = positioningManager.calculatePositionKLDA(results);
                updateDotPosition(coords[0], coords[1]);
            }
        }
    };

    // Atributo para los permisos
    private ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    // Tenemos permiso preciso
                    startLocationLogic();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    // Solo tenemos permiso aproximado
                    startLocationLogic();
                } else {
                    // El usuario dijo que no.
                    showPermissionDeniedMessage();
                }
            });



    // 1. On create:
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.view_mapa_main);

        checkAndRequestPermissions();

        mapaView = findViewById(R.id.mapaView);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 1. Cargar Datos WiFi del json, e instanciamos el objeto WifiPositioningManager que
        // se encargara de aplicar los algoritmos correspondientes.
        String json = loadJSONFromAsset("datos_entrenamiento_wifi_casa.json");
        List<ModeloDatosWifi> db = new Gson().fromJson(json, new TypeToken<List<ModeloDatosWifi>>() {
        }.getType());

        // DATOS GENERADOS Por mi modelo (la verdad es qeu no se donde ponerlo)

        List<String> bssidsmaestros = Arrays.asList(
                "08:w5:33:6c:a8:65",
                "05:f5:71:4c:a8:69"
        );
        double gamma = 0.0037;

        String jsonAlphas = "[[0.2423171577151562, -0.1352972435705378], [-0.5406121958864899, 0.8741582766628895], [0.7761107080255402, 0.7690381881791973], [0.6333926155635143, -0.3150353638147772], [-0.26508669258260664, -0.36423912132142827]]";

        double[] medias = {-79.0, -68.8, -67.8, -94.4, -90.2, -82.4, -79.8, -70.6, -80.2, -90.4, -97.2, -92.8, -78.2, -70.8, -87.6, -74.0, -79.6, -97.2, -91.6, -95.2, -85.4, -54.4, -56.8, -75.6, -97.2, -88.8, -71.4, -78.0, -83.2, -97.2, -75.4, -88.8, -79.4, -84.4, -91.6, -94.4, -97.2, -95.2, -77.8, -83.6, -87.4, -87.6, -83.6, -84.8, -85.6, -97.6, -89.2, -81.0, -87.6, -90.0, -81.4, -84.0, -97.2, -61.6, -94.4, -68.6, -92.0, -94.4, -94.4, -89.0, -93.2, -89.6, -91.6, -94.4, -88.8, -94.4, -97.6, -91.4, -97.2, -97.2, -97.2, -94.4, -78.0, -95.2, -89.2, -97.0, -94.0, -91.0, -95.2, -96.0, -94.4, -97.2, -97.6, -94.4, -97.2, -97.6, -97.4, -97.2, -95.0, -97.6, -97.2, -97.2, -97.2, -97.0, -97.6, -97.6, -97.6, -97.2, -97.6, -95.2};
        double[] scales = {5.865151319446072, 15.66397139936102, 9.927738916792686, 6.858571279792899, 19.6, 2.9393876913398134, 11.973303637676612, 11.146299834474219, 8.908422980528034, 8.138795979750322, 5.6, 5.878775382679627, 7.782030583337487, 10.870142593360953, 11.056219968868202, 18.121810064118872, 12.7216351150314, 5.6, 6.8585712797928995, 5.878775382679627, 10.650821564555478, 5.0039984012787215, 6.554387843269575, 14.051334456200237, 5.6, 5.6000000000000005, 4.586937976471886, 7.5099933422074345, 15.065191668213185, 5.6, 2.8, 5.6000000000000005, 7.2, 4.586937976471886, 6.8585712797928995, 6.858571279792899, 5.6, 9.6, 7.652450587883597, 2.9393876913398134, 6.740919818541086, 6.6211781428987395, 15.60256389187367, 10.244998779892558, 8.138795979750322, 4.800000000000001, 9.086253353280437, 5.059644256269407, 6.6211781428987395, 5.059644256269407, 15.856859714331838, 4.33589667773576, 5.6, 10.68831137270991, 6.858571279792899, 15.43502510525979, 6.6932802122726045, 6.858571279792899, 6.858571279792899, 10.469001862641921, 8.541662601625049, 9.00222194794152, 6.974238309665077, 6.8585712797928995, 9.907572861200668, 6.8585712797928995, 4.800000000000001, 7.08801805866774, 5.6, 5.6, 5.6, 6.974238309665077, 18.033302526159762, 5.878775382679627, 9.239047569960876, 6.0, 7.58946638440411, 7.536577472566709, 5.878775382679627, 8.0, 6.8585712797928995, 5.6000000000000005, 4.800000000000001, 6.8585712797928995, 5.6000000000000005, 4.8, 5.2, 5.6000000000000005, 10.0, 4.8, 5.6000000000000005, 5.6000000000000005, 5.6000000000000005, 6.0, 4.8, 4.8, 4.8, 5.6000000000000005, 4.8, 9.6};


        positioningManager = WifiPositioningManager.createWithKLDA(db,jsonAlphas,gamma,bssidsmaestros,medias,scales);


        // 2. Obtener datos del Intent
        String nombreAula = getIntent().getStringExtra("nombre_aula");
        colorPuntoAula = getIntent().getIntExtra("color_celda", Color.GRAY);
        aulaSeleccionada = RepositorioAulas.getAulaPorNombre(this, nombreAula);

        // 3. Cargar el mapa
        loadMapFromAssets("mapa.pdf");

        // 4. Configurar toques
        setupTapListener();

        // 5. Configuración del botón Scanner
        FloatingActionButton btnScanner = findViewById(R.id.btnGoToScanner);
        btnScanner.setOnClickListener(v -> {
            Intent intent = new Intent(MapaActivity.this, ScannerActivity.class);
            startActivity(intent);
        });

        btnEscaneo = findViewById(R.id.btn_escaneo_il);
        btnUbicacion = findViewById(R.id.btn_ubicacion);

        // Acción para Escaneo Ilimitado
        btnEscaneo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Aquí llamas a tu lógica de escaneo de MACs
                if (!bucleEnMarcha) {
                    bucleEnMarcha = true;
                    empezarBucleDeEscaneo();
                }
            }
        });

        // Acción para Buscar Ubicación
        btnUbicacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Aquí llamas a la lógica de GPS/Red
                wifiManager.startScan();
            }
        });
    }

    // 2. Carga del mapa y de datos del json
    private void loadMapFromAssets(String assetFileName) {
        try {
            File file = new File(getCacheDir(), assetFileName);

            if (!file.exists()) {
                InputStream is = getAssets().open(assetFileName);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                is.close();
                fos.close();
            }

            ParcelFileDescriptor fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            PdfRenderer renderer = new PdfRenderer(fd);
            PdfRenderer.Page page = renderer.openPage(0);

            // --- EL CAMBIO ESTÁ AQUÍ ---
            // Definimos un factor de escala. 3.0f o 4.0f suele ser suficiente para que se vea nítido.
            float escala = 4.0f;
            int newWidth = (int) (page.getWidth() * escala);
            int newHeight = (int) (page.getHeight() * escala);

            // Creamos el bitmap con la nueva resolución aumentada
            Bitmap bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

            // Renderizamos la página usando la matriz de escalado
            // Esto le dice al PDF que se dibuje más grande de lo que es originalmente
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            // Cargar en el mapaView (SubsamplingScaleImageView)
            mapaView.setImage(ImageSource.bitmap(bitmap));

            // --- CONFIGURACIÓN DE ZOOM ---

            // 1. Zoom máximo: qué tanto se puede acercar el usuario.
            // Con escala 4.0f en el bitmap, puedes poner un valor alto como 10f o 15f.
            mapaView.setMaxScale(10.0f);

            // 2. Zoom mínimo: qué tanto se puede alejar (1.0f es el tamaño original).
            mapaView.setMinScale(1.0f);

            // 3. Zoom por doble toque: cuánto se acerca al hacer doble clic.
            mapaView.setDoubleTapZoomScale(2.0f);

            // 4. Estilo de zoom: el centro del zoom será donde el usuario ponga los dedos.
            mapaView.setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_FIXED);

            // 5. Duración de la animación (en milisegundos)
            mapaView.setDoubleTapZoomDuration(500);

            // Pintamos el punto
            if (aulaSeleccionada != null) {
                float puntoaulaX = aulaSeleccionada.x * mapaView.getSWidth();
                float puntoaulaY = aulaSeleccionada.y * mapaView.getSHeight();
                mapaView.setAulaData(new PointF(puntoaulaX,puntoaulaY), aulaSeleccionada.nombre, colorPuntoAula);
            }

            page.close();
            renderer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String loadJSONFromAsset(String filename) {
        String json;
        try {
            // Abrimos el stream desde la carpeta assets
            InputStream is = getAssets().open(filename);

            // Medimos el tamaño del archivo para crear el buffer
            int size = is.available();
            byte[] buffer = new byte[size];

            // Leemos el contenido
            is.read(buffer);
            is.close();

            // Convertimos el buffer a String con codificación UTF-8
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    // 3. Para el wifiReceiver
    private void updateDotPosition(float xPercent, float yPercent) {
        if (dotAnimator != null) dotAnimator.cancel();

        // Convertimos porcentajes a coordenadas de la imagen (Source coordinates)
        float targetX = xPercent * mapaView.getSWidth();
        float targetY = yPercent * mapaView.getSHeight();

        float startX = posUsuario.x == -1f ? targetX : posUsuario.x;
        float startY = posUsuario.y == -1f ? targetY : posUsuario.y;

        dotAnimator = ValueAnimator.ofFloat(0f, 1f);
        dotAnimator.setDuration(1500);
        dotAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        dotAnimator.addUpdateListener(animation -> {
            float f = animation.getAnimatedFraction();
            posUsuario.x = startX + (targetX - startX) * f;
            posUsuario.y = startY + (targetY - startY) * f;

            // El PinView se encarga de redibujar
            mapaView.dibujar(new PointF(posUsuario.x, posUsuario.y));
        });
        dotAnimator.start();
    }

    private void empezarBucleDeEscaneo() {
        wifiScannerRunnable = new Runnable() {
            @Override
            public void run() {

                wifiManager.startScan();
                Log.d("WiFi", "Escaneo automático cada 3 segundos...");

                // Volver a llamar a este código en 3000ms
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(wifiScannerRunnable); // Primera ejecución inmediata
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        // 2. Registramos el receptor (Empezamos a escuchar)
        registerReceiver(wifiReceiver, intentFilter);

    }
    @Override
    protected void onPause() {
        super.onPause();
        // IMPORTANTE: Desregistrar para no colgar el sistema
        unregisterReceiver(wifiReceiver);

        // Parar el bucle
        handler.removeCallbacks(wifiScannerRunnable);
    }

    // 4. Para clicar y obtener la posicion
    @SuppressLint("ClickableViewAccessibility")
    private void setupTapListener() {
        // GestureDetector nos ayuda a diferenciar entre un toque simple y un scroll/zoom
        final android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (mapaView.isReady()) {
                    // 1. Convertimos el toque en pantalla a coordenadas de la "fuente" (la imagen original)
                    PointF sCoord = mapaView.viewToSourceCoord(e.getX(), e.getY());

                    if (sCoord != null) {
                        // 2. Calculamos el porcentaje (0.0 a 1.0) dividiendo por el ancho/alto total de la imagen
                        float xRelativo = sCoord.x / mapaView.getSWidth();
                        float yRelativo = sCoord.y / mapaView.getSHeight();

                        // 3. Lanzamos el diálogo para guardar el punto
                        mostrarDialogoCapturaPunto(xRelativo, yRelativo);
                    }
                }
                return true;
            }
        });

        // Pasamos los eventos de toque de la vista al detector de gestos
        mapaView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        mapaView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Primero procesamos nuestro detector de toques (para el JSON)
                gestureDetector.onTouchEvent(event);

                // DEVOLVEMOS FALSE para que la librería pueda seguir procesando
                // el zoom, el arrastre y la ROTACIÓN.
                return false;
            }
        });
    }
    private void mostrarDialogoCapturaPunto(float x, float y) {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final android.widget.EditText inputNombre = new android.widget.EditText(this);
        inputNombre.setHint("Nombre del aula");
        layout.addView(inputNombre);

        final android.widget.EditText inputModulo = new android.widget.EditText(this);
        inputModulo.setHint("Módulo (ej: A)");
        layout.addView(inputModulo);

        new AlertDialog.Builder(this)
                .setTitle("Registrar Coordenadas")
                .setMessage(String.format(java.util.Locale.US, "X: %.4f, Y: %.4f", x, y))
                .setView(layout)
                .setPositiveButton("Generar JSON", (dialog, which) -> {
                    String nombre = inputNombre.getText().toString();
                    String modulo = inputModulo.getText().toString();

                    String bloqueJson = String.format(java.util.Locale.US,
                            "{\n  \"nombre\": \"%s\",\n  \"modulo\": \"%s\",\n  \"x\": %.8f,\n  \"y\": %.8f\n},",
                            nombre, modulo, x, y);

                    Log.d("JSON_MAPA", bloqueJson);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // 5. PERMISOS PARA EL MAPA:
    public void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };
        if (hasPermissions(permissions)) {
            // Ya los tenemos, procedemos
            startLocationLogic();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // El usuario ya lo rechazó una vez. Lo profesional es mostrar un diálogo
            // PROPIO (un AlertDialog) antes de volver a pedir el del sistema.
            showCustomExplanationDialog();
        } else {
            // Primera vez que los pedimos o el usuario marcó "no volver a preguntar"
            requestPermissionLauncher.launch(permissions);
        }
    }
    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private void showPermissionDeniedMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Permiso necesario")
                .setMessage("Has desactivado los permisos de ubicación de forma permanente. Para ver tu ubicación el mapa, actívalos en los ajustes de la aplicación.")
                .setPositiveButton("Ir a Ajustes", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void showCustomExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Ubicación necesaria")
                .setMessage("Para mostrarte dónde estás en el mapa y guiarte a tu aula, necesitamos acceder a tu ubicación.")
                .setPositiveButton("Entendido", (dialog, which) -> {
                    // Volvemos a pedir el permiso tras la explicación
                    requestPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                })
                .setNegativeButton("Ahora no", (dialog, which) -> {
                    // Opcional: mostrar el mapa pero sin el punto azul de usuario
                    Toast.makeText(this, "Funcionalidad limitada sin GPS", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // FUNCIONES PARA VER SI ESTAN ACTIVOS WIFI Y GPS
    private void startLocationLogic() {
        // Si llegamos aquí, ya tenemos permiso de Android.
        // Ahora comprobamos si el interruptor del GPS está encendido.
        checkLocationSettings();
        // Tambien comprobamos si esta puesto el wifi
        explicarYActivarWifi();
    }

    // Para la localizacion, un mensaje de Google.
    private void checkLocationSettings() {
        // 1. Definimos la configuración de ubicación que necesita nuestra app
        // Usamos Alta Precisión para que el GPS se active sí o sí
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(5000)
                .build();

        // 2. Creamos la solicitud de ajustes de ubicación
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                // Esto obliga a mostrar el diálogo aunque el usuario haya dicho que no antes
                .setAlwaysShow(true);

        // 3. Obtenemos el cliente de ajustes de Google Play Services
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        // 4. Caso de éxito: El GPS ya está encendido o la configuración es correcta
        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // Aquí ya puedes iniciar tu lógica de posicionamiento (mostrar el punto azul, etc.)
            Toast.makeText(MapaActivity.this, "GPS listo para usarse", Toast.LENGTH_SHORT).show();
        });

        // 5. Caso de fallo: El GPS está apagado o en modo "Ahorro de batería"
        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // El error es "resolvible", lo que significa que podemos mostrar el diálogo de Google
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    // Esto lanza el cuadro de diálogo de un solo clic que activa el GPS automáticamente
                    // El número 100 es el requestCode para identificar la respuesta después
                    resolvable.startResolutionForResult(MapaActivity.this, 100);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Error al intentar abrir el diálogo
                }
            } else {
                // El error no se puede resolver (el móvil no tiene GPS, por ejemplo)
                Toast.makeText(this, "Tu dispositivo no soporta los ajustes de ubicación", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Para el wifi, un dialogo y un panel desde abajo.
    private void explicarYActivarWifi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && !wifiManager.isWifiEnabled()){
            new AlertDialog.Builder(this)
                .setTitle("Wi-Fi Necesario")
                .setMessage("Para poder calcular tu posición exacta dentro del edificio, necesitamos escanear las redes Wi-Fi cercanas. ¿Quieres activarlo ahora?")
                .setPositiveButton("Activar", (dialog, which) -> {
                    // Ahora sí, lanzamos el panel del sistema
                    showWifiPanel();
                })
                .setNegativeButton("Ahora no", null)
                .show();
        }
    }
    private void showWifiPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) o superior: Usamos el panel flotante
            Intent intent = new Intent(Settings.Panel.ACTION_WIFI);
            startActivityForResult(intent, 999); // 999 es un código ID para identificar esta acción
        } else {
            // Android 9 o inferior: Podemos encenderlo directamente (si tienes el permiso en el Manifest)
            wifiManager.setWifiEnabled(true);
            Toast.makeText(this, "Activando Wi-Fi...", Toast.LENGTH_SHORT).show();
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 999) {
            // El usuario ha interactuado con el panel de Wi-Fi y ha vuelto a la app
            if (isWifiEnabled()) {
                // ¡Perfecto! Ahora que tenemos permisos y el Wi-Fi encendido, a trabajar.

            } else {
                // El usuario cerró el panel pero NO activó el interruptor
                Toast.makeText(this, "El Wi-Fi sigue desactivado", Toast.LENGTH_LONG).show();
            }
        }
    }
    // Función auxiliar simple
    private boolean isWifiEnabled() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wm != null && wm.isWifiEnabled();
    }


}
