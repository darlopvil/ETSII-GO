package es.us.etsii_go.horariogps.mapa.models;

import java.util.List;

public class ModeloDatosWifi {
    public String punto_id;
    public Coordenada coordenada_pdf;
    public List<ModeloDatosEscaneo> lecturas;

    public List<ModeloDatosEscaneo> getLecturas() {
        return lecturas;
    }

    public static class Coordenada {
        public float x;
        public float y;
    }
}
