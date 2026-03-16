package es.us.etsii_go;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class InformacionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.informacion);

        TextView textoGithub = findViewById(R.id.info_texto_integrantes);   // Buscamos la variable que contiene el enlace
        TextView textoEscuela = findViewById(R.id.info_texto_asig_datos);
        TextView textoRecursos = findViewById(R.id.info_texto_recursos_datos);
        TextView textoPermisos = findViewById(R.id.info_texto_permisos_datos);

        // 1. Extraemos el texto y le decimos a Android que lo interprete como HTML (para que pinte el enlace de azul)
        textoGithub.setText(android.text.Html.fromHtml(getString(R.string.texto_integrantes), Html.FROM_HTML_MODE_COMPACT));
        textoEscuela.setText(android.text.Html.fromHtml(getString(R.string.texto_asig_datos), Html.FROM_HTML_MODE_COMPACT));
        textoRecursos.setText(android.text.Html.fromHtml(getString(R.string.texto_recursos_datos), Html.FROM_HTML_MODE_COMPACT));
        textoPermisos.setText(android.text.Html.fromHtml(getString(R.string.texto_permisos_datos), Html.FROM_HTML_MODE_COMPACT));   // En este caso no es por enlaces, sino para que interprete etiquetas HTML como los <br>

        // 2. Hacemos que responda al click abriendo el navegador
        textoGithub.setMovementMethod(LinkMovementMethod.getInstance());    // Hacemos el enlace clickable
        textoEscuela.setMovementMethod(LinkMovementMethod.getInstance());
        textoRecursos.setMovementMethod(LinkMovementMethod.getInstance());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.informacion_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }
}