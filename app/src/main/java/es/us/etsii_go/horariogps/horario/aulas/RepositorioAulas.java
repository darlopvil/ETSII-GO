package es.us.etsii_go.horariogps.horario.aulas;

import android.content.Context;

import es.us.etsii_go.horariogps.horario.models.Aula;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RepositorioAulas {
    // Para leer las aulas del json una sola vez, y mejorar el rendimiento.
    private static List<Aula> aulasGuardadas = null;

    public static List<Aula> getTodasLasAulas(Context context) {
        // Si esta vacia, es la primera vez que intentamos acceder,luego leemos el json.
        if (aulasGuardadas == null) {
            aulasGuardadas = cargarAulasDesdeJson(context);
        }
        return aulasGuardadas;
    }

    public static Aula getAulaPorNombre(Context context, String nombre) {
        for (Aula a : getTodasLasAulas(context)) {
            if (a.nombre.equals(nombre)) return a;
        }
        return null;
    }

    private static List<Aula> cargarAulasDesdeJson(Context context) {
        try {
            // 1. Abrir el archivo desde la carpeta assets
            InputStream is = context.getAssets().open("aulas.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            // 2. Convertir a String
            String json = new String(buffer, "UTF-8");

            // 3. Convertir de String JSON a Lista de Objetos usando GSON
            Type listType = new TypeToken<ArrayList<Aula>>(){}.getType();
            return new Gson().fromJson(json, listType);

        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
