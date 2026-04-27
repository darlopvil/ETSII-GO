package es.us.etsii_go.horariogps.horario.models;

public class Aula {
        public String nombre;
        public String modulo;
        public int planta;
        public float x;
        public float y;

    public Aula(String nombre, String modulo, int p, float x, float y) {
        this.nombre=nombre;
        this.modulo=modulo;
        this.planta=p;
        this.x=x;
        this.y=y;
    }

    @Override
    public String toString() {
        return nombre + " (Módulo " + modulo + ", Planta " + planta + ")";
    }
}
