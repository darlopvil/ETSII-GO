package es.us.etsii_go;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_actividad_principal), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configuración de los 4 botones
        configurarBotones();

    }

    private void configurarBotones() {

        // 1.- Botón "GPS Facultad"
        findViewById(R.id.boton_gps_facultad).setOnClickListener(v -> {
            Intent intent_gps = new Intent(this, RecorridoActivity.class);
            startActivity(intent_gps);
        });

        // 2.- Botón "Servicios"
        findViewById(R.id.boton_servicios).setOnClickListener(v -> {
            Intent intent_servicios = new Intent(this, ServiciosActivity.class);
            startActivity(intent_servicios);
        });

        // 3.- Botón "Itinerario"
        findViewById(R.id.boton_itinerario).setOnClickListener(v -> {
            Intent intent_itinerario = new Intent(this, ItinerarioActivity.class);
            startActivity(intent_itinerario);
        });

        // 4.- Botón "Información"
        findViewById(R.id.boton_info).setOnClickListener(v -> {
            Intent intent_info = new Intent(this, InformacionActivity.class);
            startActivity(intent_info);
        });
    }
}