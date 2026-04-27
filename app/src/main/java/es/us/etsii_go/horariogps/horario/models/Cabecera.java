package es.us.etsii_go.horariogps.horario.models;

public class Cabecera {
    private String id;
    private String texto;

    public Cabecera(String id, String texto) {
        this.id = id;
        this.texto = texto;
    }
    public String getTexto() { return texto; }
}