package es.us.etsii_go.horariogps.horario.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import es.us.etsii_go.horariogps.horario.models.CeldaHorario;


@Database(entities = {CeldaHorario.class}, version = 1,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract HorarioDao horarioDao();

    private static AppDatabase instancia;

    public static AppDatabase getInstancia(Context context) {
        if (instancia == null) {
            instancia = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "base-datos-horario")
                    .build();
        }
        return instancia;
    }
}
