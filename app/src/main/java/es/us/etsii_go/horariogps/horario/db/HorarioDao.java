package es.us.etsii_go.horariogps.horario.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import es.us.etsii_go.horariogps.horario.models.CeldaHorario;

import java.util.List;

@Dao
public interface HorarioDao {

    @Query("SELECT * FROM tabla_horario")
    List<CeldaHorario> obtenerTodo();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(CeldaHorario celda);

    @Update
    void actualizar(CeldaHorario celda);

    @Delete
    void borrar(CeldaHorario celda);

    @Query("SELECT DISTINCT color FROM tabla_horario WHERE color != 0 AND color != -1 LIMIT 20")
    List<Integer> obtenerColoresRecientes();
}