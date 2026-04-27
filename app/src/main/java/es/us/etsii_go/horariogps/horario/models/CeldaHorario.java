package es.us.etsii_go.horariogps.horario.models;

import androidx.room.Entity;
import androidx.room.Ignore;

import org.jetbrains.annotations.NotNull;

@Entity(tableName = "tabla_horario", primaryKeys = {"id", "hora"})
public class CeldaHorario {
    private int id;
    @NotNull
    private String dia;
    @NotNull
    private String hora;
    private String contenido;
    private String aula;
    private int color;


    public CeldaHorario(int id,String dia, String hora, String contenido, String aula, int color) {
        this.id = id;
        this.dia = dia;
        this.hora = hora;
        this.contenido = contenido;
        this.aula = aula;
        this.color=color;
    }

    @Ignore
    public CeldaHorario(int id,String dia, String hora, String contenido, int color) {
        this.id = id;
        this.dia = dia;
        this.hora = hora;
        this.contenido = contenido;
        this.color=color;
    }

    public String getDia() { return dia; }
    public void setDia(String dia) {
        this.dia = dia;
    }

    public String getHora() { return hora; }
    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getAula() {return aula;}
    public void setAula(String aula) {
        this.aula = aula;
    }

    public int getColor() {
        return color;
    }
    public void setColor(int color) {
        this.color = color;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
}


