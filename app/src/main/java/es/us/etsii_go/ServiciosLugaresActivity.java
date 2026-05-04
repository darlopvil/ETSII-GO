package es.us.etsii_go;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ServiciosLugaresActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_resultados);

        TextView txtResultado = findViewById(R.id.txtResultado);
        Button btnVolver = findViewById(R.id.btnVolver);
        RecyclerView recyclerLugares = findViewById(R.id.recyclerLugares);

        String categoria = getIntent().getStringExtra("categoria");

        if (categoria != null) {
            txtResultado.setText(categoria);
        } else {
            txtResultado.setText("Resultados");
        }

        List<Lugar> listaLugares = new ArrayList<>();

        if ("Comer barato".equals(categoria)) {
            listaLugares.add(new Lugar(
                    "Ñan Ñam",
                    "Avenida de la Reina Mercedes, 31",
                    "Muy Barato",
                    "Ideal para bocadillos rápidos a precios económicos.",
                    "BARATO",
                    R.drawable.img_4
            ));

            listaLugares.add(new Lugar(
                    "100 montaditos",
                    "Avenida Reina Mercedes, 43",
                    "Barato",
                    "Muy popular entre estudiantes. Comida rápida y económica.",
                    "BARATO",
                    R.drawable.img_1
            ));

            listaLugares.add(new Lugar(
                    "Kalixto",
                    "Avenida Reina Mercedes, 19",
                    "Barato",
                    "Tapas muy baratas e ideal para comer rápido.",
                    "MUY BARATO",
                    R.drawable.img_2
            ));

            listaLugares.add(new Lugar(
                    "Bocatería Bocapizza",
                    "Calle Ensanche, 6",
                    "Muy barato",
                    "Bocadillos grandes y baratos, ideal para comer y llenarse bien.",
                    "MUY BARATO",
                    R.drawable.img_3
            ));

            listaLugares.add(new Lugar(
                    "TITI Burger",
                    "Avenida Reina Mercedes, 3",
                    "Muy barato",
                    "Ideal para los amantes de las hamburguesas.",
                    "BARATO",
                    R.drawable.img_5
            ));

        } else if ("Estudiar".equals(categoria)) {
            listaLugares.add(new Lugar(
                    "Biblioteca ETSII",
                    "ETSII",
                    "Gratis",
                    "Situada en la misma Universidad ETSII.",
                    "ESTUDIO",
                    R.drawable.img_6
            ));

            listaLugares.add(new Lugar(
                    "CRAI",
                    "Avenida Reina Mercedes, s/n, 41012 Sevilla",
                    "Gratis",
                    "Estudio individual o grupal en salas de estudio separados.",
                    "24H",
                    R.drawable.img_7
            ));

            listaLugares.add(new Lugar(
                    "Bunker (Facultad Matemáticas)",
                    "C. Tarfia, 41012 Sevilla",
                    "Gratis",
                    "Abierta hasta tarde para estudiantes.",
                    "24H",
                    R.drawable.img_8
            ));

        } else if ("Comprar básico".equals(categoria)) {
            listaLugares.add(new Lugar(
                    "Supermercado Jamón",
                    "Avenida de la Reina Mercedes, 45",
                    "Económico",
                    "Compra rápida de productos de primera necesidad.",
                    "BÁSICO",
                    R.drawable.img_9
            ));

            listaLugares.add(new Lugar(
                    "Supermercado Mas Go",
                    "Avenida de la Reina Mercedes, 39",
                    "Económico",
                    "Compra de productos, alimentos y postres.",
                    "BÁSICO",
                    R.drawable.img
            ));

            listaLugares.add(new Lugar(
                    "Farmacia Garcia-Mina C B",
                    "Avenida de la Reina Mercedes, 17",
                    "***",
                    "Compra de medicamentos y productos para la salud, bienestar e higiene.",
                    "BÁSICO",
                    R.drawable.img_10
            ));

            listaLugares.add(new Lugar(
                    "Farmacia Rosario Nuñez Valdes",
                    "Avenida de la Reina Mercedes, 33",
                    "***",
                    "Compra de medicamentos y productos básicos de farmacia.",
                    "BÁSICO",
                    R.drawable.img_10
            ));

            listaLugares.add(new Lugar(
                    "Copistería Papelería el Estudiante",
                    "Avenida de la Reina Mercedes, 31",
                    "Económico",
                    "Compra de productos escolares.",
                    "BÁSICO",
                    R.drawable.img_11
            ));

            listaLugares.add(new Lugar(
                    "Copistería Central",
                    "Avenida de la Reina Mercedes, 41",
                    "Económico",
                    "Copias baratas y productos escolares económicos.",
                    "BÁSICO",
                    R.drawable.img_11
            ));

        } else {
            listaLugares.add(new Lugar(
                    "Gimnasio Universitario (SADUS)",
                    "C. Pirotecnia, s/n, 41013 Sevilla",
                    "Cuota mensual",
                    "Descuento para estudiantes.",
                    "DEPORTE",
                    R.drawable.img_12
            ));
        }

        recyclerLugares.setLayoutManager(new LinearLayoutManager(this));
        recyclerLugares.setAdapter(new ServiciosLugaresAdapter(listaLugares));

        btnVolver.setOnClickListener(v -> finish());
    }
}