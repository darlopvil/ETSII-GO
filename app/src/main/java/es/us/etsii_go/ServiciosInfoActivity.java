package es.us.etsii_go;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServiciosInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_information);

        TextView txtTitulo = findViewById(R.id.txtTituloInformacion);
        Button btnVolver = findViewById(R.id.btnVolverInformacion);
        RecyclerView recyclerInfo = findViewById(R.id.recyclerInformacion);

        txtTitulo.setText("Información");
        recyclerInfo.setLayoutManager(new LinearLayoutManager(this));

        RetrofitClient.getApiService().getInformacion().enqueue(new Callback<List<Informacion>>() {
            @Override
            public void onResponse(Call<List<Informacion>> call, Response<List<Informacion>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Informacion> lista = response.body();
                    recyclerInfo.setAdapter(new ServiciosInfoAdapter(lista));
                } else {
                    Toast.makeText(
                            ServiciosInfoActivity.this,
                            "Error al cargar información",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }

            @Override
            public void onFailure(Call<List<Informacion>> call, Throwable t) {
                Toast.makeText(
                        ServiciosInfoActivity.this,
                        "No se pudo conectar: " + t.getLocalizedMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });

        btnVolver.setOnClickListener(v -> finish());
    }
}