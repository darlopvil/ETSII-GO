package es.us.etsii_go.horariogps.mapa.models;

public class ModeloDatosEscaneo {
    public String bssid;
    public String ssid;
    public int rssi;

    public ModeloDatosEscaneo(String bssid, String ssid, int rssi) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.rssi = rssi;
    }
}
