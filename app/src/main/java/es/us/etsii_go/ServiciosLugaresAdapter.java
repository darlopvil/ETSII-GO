package es.us.etsii_go;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ServiciosLugaresAdapter extends RecyclerView.Adapter<ServiciosLugaresAdapter.LugarViewHolder> {

    private List<Lugar> listaLugares;

    public ServiciosLugaresAdapter(List<Lugar> listaLugares) {
        this.listaLugares = listaLugares;
    }

    // ViewHolder
    public static class LugarViewHolder extends RecyclerView.ViewHolder {
        ImageView imgLugar;
        TextView txtNombreLugar, txtDireccionLugar, txtEtiquetaLugar, txtDescripcionLugar;

        public LugarViewHolder(View itemView) {
            super(itemView);
            imgLugar = itemView.findViewById(R.id.imgLugar);
            txtNombreLugar = itemView.findViewById(R.id.txtNombreLugar);
            txtDireccionLugar = itemView.findViewById(R.id.txtDireccionLugar);
            txtEtiquetaLugar = itemView.findViewById(R.id.txtEtiquetaLugar);
            txtDescripcionLugar = itemView.findViewById(R.id.txtDescripcionLugar);
        }
    }

    @Override
    public LugarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lugar, parent, false);
        return new LugarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LugarViewHolder holder, int position) {
        Lugar lugar = listaLugares.get(position);

        holder.txtNombreLugar.setText(lugar.getNombre());
        holder.txtDireccionLugar.setText("📍 " + lugar.getDireccion());
        holder.txtEtiquetaLugar.setText(lugar.getEtiqueta());
        holder.txtDescripcionLugar.setText(lugar.getDescripcion());
        holder.imgLugar.setImageResource(R.mipmap.ic_launcher);

        holder.itemView.setOnClickListener(v -> {
            String consulta = lugar.getNombre() + ", " + lugar.getDireccion();
            Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(consulta));
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return listaLugares.size();
    }
}