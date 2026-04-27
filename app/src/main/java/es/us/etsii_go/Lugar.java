package es.us.etsii_go;

public class Lugar {

    private String nombre;
    private String direccion;
    private String precio;
    private String descripcion;
    private String etiqueta;
    private String enlace;

    public Lugar(String nombre, String direccion, String precio, String descripcion, String etiqueta) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.precio = precio;
        this.descripcion = descripcion;
        this.etiqueta = etiqueta;
        this.enlace = null;
    }

    public Lugar(String nombre, String direccion, String precio, String descripcion, String etiqueta, String enlace) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.precio = precio;
        this.descripcion = descripcion;
        this.etiqueta = etiqueta;
        this.enlace = enlace;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getPrecio() {
        return precio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getEnlace() {
        return enlace;
    }
}