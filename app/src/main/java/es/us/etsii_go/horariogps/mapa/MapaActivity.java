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

        setContentView(R.layout.view_mapa_main);

        checkAndRequestPermissions();

        mapaView = findViewById(R.id.mapaView);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 1. Cargar Datos WiFi del json, e instanciamos el objeto WifiPositioningManager que
        // se encargara de aplicar los algoritmos correspondientes.
        String json = loadJSONFromAsset("datos_entrenamiento_wifi_version1.json");
        List<ModeloDatosWifi> db = new Gson().fromJson(json, new TypeToken<List<ModeloDatosWifi>>() {
        }.getType());

        // DATOS GENERADOS Por mi modelo (la verdad es qeu no se donde ponerlo)

        List<String> bssidsmaestros = Arrays.asList(
                "38:91:b7:10:f5:8f",
                "38:91:b7:11:1a:cf",
                "38:91:b7:10:a5:40",
                "38:91:b7:11:1a:c0",
                "38:91:b7:10:a5:4f",
                "38:91:b7:10:ef:ef",
                "38:91:b7:10:f5:80",
                "38:91:b7:11:6e:ef",
                "38:91:b7:11:6e:e0",
                "38:91:b7:11:59:80",
                "38:91:b7:11:59:8f",
                "38:91:b7:11:61:6f",
                "38:91:b7:11:61:60",
                "38:91:b7:11:11:00",
                "38:91:b7:11:5c:cf",
                "14:84:73:df:f5:4f",
                "38:91:b7:11:5d:60",
                "38:91:b7:11:b1:c0",
                "38:91:b7:11:5c:c0",
                "38:91:b7:11:51:2f",
                "38:91:b7:11:67:2f",
                "38:91:b7:11:cb:8f",
                "38:91:b7:11:49:c0",
                "14:84:73:df:f5:40",
                "38:91:b7:11:3e:2f",
                "38:91:b7:10:9f:a0",
                "38:91:b7:11:3e:20",
                "38:91:b7:11:cc:e0",
                "38:91:b7:11:7e:c0",
                "38:91:b7:11:64:e0",
                "38:91:b7:11:7e:cf",
                "38:91:b7:11:3b:4f",
                "38:91:b7:11:3e:cf",
                "38:91:b7:11:64:ef",
                "38:91:b7:11:3f:8f",
                "38:91:b7:11:d1:cf",
                "14:84:73:df:f5:e0",
                "38:91:b7:10:ef:e0",
                "38:91:b7:11:5b:0f",
                "38:91:b7:11:3f:80",
                "14:84:73:e3:47:ef",
                "14:84:73:e3:47:e0",
                "14:84:73:df:f5:ef",
                "38:91:b7:11:cb:a0"
        );
        double gamma = 0.02;

        String jsonAlphas = "[[0.09895973329461774, 0.4972831553163167], [-0.05887626072494593, -0.022400618147182947], [-0.06047072342995497, 0.38377231492288105], [0.22719799024036277, 0.10207798334142366], [0.35054578110423995, 0.6553568014762591], [0.3041927141999372, 0.31292663440246726], [0.12212245017778554, 0.34634692073118617], [-0.0628329936474241, -0.14615528171101377], [0.22428763438320998, -0.145893157953956], [0.47568376392381456, 0.1406931320183186], [0.21099281903340275, 0.24096979332858204], [0.0017245838908504744, 0.21338730894894953]]";

        double[] medias = {-82.0, -98.25, -90.91666666666667, -97.83333333333333, -81.75, -80.58333333333333, -84.33333333333333, -87.25, -87.83333333333333, -89.16666666666667, -93.08333333333333, -88.66666666666667, -88.33333333333333, -97.83333333333333, -96.16666666666667, -85.08333333333333, -95.33333333333333, -97.83333333333333, -98.0, -98.08333333333333, -98.08333333333333, -98.25, -97.83333333333333, -95.83333333333333, -89.5, -98.08333333333333, -98.08333333333333, -97.83333333333333, -97.33333333333333, -95.25, -98.0, -98.25, -98.0, -94.58333333333333, -97.75, -98.08333333333333, -98.83333333333333, -93.58333333333333, -98.0, -97.83333333333333, -97.33333333333333, -97.41666666666667, -98.08333333333333, -96.91666666666667};

        double[] scales = {19.087517736293876, 5.80409338312195, 15.8506484269747, 7.1860203791033666, 22.69774217846348, 17.14865560004308, 19.690663326110226, 19.604952605570528, 18.26122181624828, 15.608936186969592, 12.023992219262656, 13.822284744410222, 17.084756038319334, 7.1860203791033666, 8.581310441237335, 12.932248665856823, 10.561986345169906, 7.1860203791033666, 6.6332495807108, 6.356864181514517, 6.356864181514517, 5.80409338312195, 7.1860203791033666, 9.325889889025188, 16.152915113584505, 6.356864181514516, 6.356864181514516, 7.1860203791033666, 8.844332774281066, 10.670246795021503, 6.6332495807108, 5.80409338312195, 6.6332495807108, 12.12750546851614, 7.46240577829965, 6.356864181514516, 3.8693955887479663, 14.974747261825677, 6.6332495807108, 7.1860203791033666, 8.844332774281066, 8.567947375084783, 6.356864181514516, 10.226259770262482};


        positioningManager = WifiPositioningManager.createWithKLDA(db,jsonAlphas,gamma,bssidsmaestros,medias,scales);


        // 2. Obtener datos del Intent
        String nombreAula = getIntent().getStringExtra("nombre_aula");
        colorPuntoAula = getIntent().getIntExtra("color_celda", Color.parseColor("#808080"));

        if (nombreAula != null) {
            aulaSeleccionada = RepositorioAulas.getAulaPorNombre(this, nombreAula);
        } else {
            aulaSeleccionada = new Aula("","",0,0,0);
        }

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
                .setTitle(R.string.permiso_necesario)
                .setMessage(R.string.permisos_ubicacion)
                .setPositiveButton(R.string.ir_a_ajustes, (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
    private void showCustomExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ubicacion_necesaria)
                .setMessage(R.string.mensaje_ubicacion)
                .setPositiveButton(R.string.entendido, (dialog, which) -> {
                    // Volvemos a pedir el permiso tras la explicación
                    requestPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                })
                .setNegativeButton(R.string.ahora_no, (dialog, which) -> {
                    // Opcional: mostrar el mapa pero sin el punto azul de usuario
                    Toast.makeText(this, R.string.funcionalidad_limitada_sin_gps, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(MapaActivity.this, R.string.gps_listo_para_usarse, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.tu_dispositivo_no_soporta_los_ajustes_de_ubicaci_n, Toast.LENGTH_LONG).show();
            }
        });
    }

    // Para el wifi, un dialogo y un panel desde abajo.
    private void explicarYActivarWifi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && !wifiManager.isWifiEnabled()){
            new AlertDialog.Builder(this)
                .setTitle(R.string.wi_fi_necesario)
                .setMessage(R.string.mensaje_wifi)
                .setPositiveButton(R.string.activar, (dialog, which) -> {
                    // Ahora sí, lanzamos el panel del sistema
                    showWifiPanel();
                })
                .setNegativeButton(R.string.ahora_no, null)
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
            Toast.makeText(this, R.string.activando_wi_fi, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.el_wi_fi_sigue_desactivado, Toast.LENGTH_LONG).show();
            }
        }
    }
    // Función auxiliar simple
    private boolean isWifiEnabled() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wm != null && wm.isWifiEnabled();
    }


}
