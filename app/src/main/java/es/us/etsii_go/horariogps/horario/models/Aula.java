package es.us.etsii_go.horariogps.horario.models;

public class Aula {
        public String nombre;
        public String modulo;
        public int planta;
        public float x;
        public float y;

    @Override
    public String toString() {
        return nombre + " (Módulo " + modulo + ", Planta " + planta + ")";
    }
}
