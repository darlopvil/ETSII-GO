package es.us.etsii_go;

public class Informacion {

    private String titulo;
    private String subtitulo;
    private String descripcion;
    private String url;
    private String tipo;

    public Informacion(String titulo, String subtitulo, String descripcion, String tipo) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.url = null;
    }

    public Informacion(String titulo, String subtitulo, String descripcion, String url, String tipo) {
        this.titulo = titulo;
        this.subtitulo = subtitulo;
        this.descripcion = descripcion;
        this.url = url;
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSubtitulo() {
        return subtitulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUrl() {
        return url;
    }

    public String getTipo() {
        return tipo;
    }
}