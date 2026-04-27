package es.us.etsii_go;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {

    @GET("servicios")
    Call<List<Informacion>> getServicios();

    @GET("charlas")
    Call<List<Informacion>> getCharlas();

    @GET("noticias")
    Call<List<Informacion>> getNoticias();

    @GET("informacion")
    Call<List<Informacion>> getInformacion();
}