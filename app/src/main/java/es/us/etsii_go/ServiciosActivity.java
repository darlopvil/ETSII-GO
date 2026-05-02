package es.us.etsii_go;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

public class ServiciosActivity extends AppCompatActivity {

    private Spinner spinnerCategorias;
    private Button btnBuscar;
    private View btnInformacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.servicios);

        spinnerCategorias = findViewById(R.id.spinnerCategorias);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnInformacion = findViewById(R.id.btnInformacion);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.categorias_array,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorias.setAdapter(adapter);

        btnBuscar.setOnClickListener(v -> {
            String categoriaSeleccionada = spinnerCategorias.getSelectedItem().toString();

            Intent intent = new Intent(ServiciosActivity.this, ServiciosLugaresActivity.class);
            intent.putExtra("categoria", categoriaSeleccionada);
            startActivity(intent);
        });

        btnInformacion.setOnClickListener(v -> {
            Intent intent = new Intent(ServiciosActivity.this, ServiciosInfoActivity.class);
            startActivity(intent);
        });
    }
}