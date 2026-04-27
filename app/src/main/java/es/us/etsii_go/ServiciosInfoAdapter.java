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

public class ServiciosInfoAdapter extends RecyclerView.Adapter<ServiciosInfoAdapter.InformacionViewHolder> {

    private List<Informacion> listaInformacion;

    public ServiciosInfoAdapter(List<Informacion> listaInformacion) {
        this.listaInformacion = listaInformacion;
    }

    public static class InformacionViewHolder extends RecyclerView.ViewHolder {
        ImageView imgInfo;
        TextView txtTitulo;
        TextView txtSubtitulo;
        TextView txtTipo;
        TextView txtDescripcion;

        public InformacionViewHolder(View itemView) {
            super(itemView);

            imgInfo = itemView.findViewById(R.id.imgLugar);
            txtTitulo = itemView.findViewById(R.id.txtNombreLugar);
            txtSubtitulo = itemView.findViewById(R.id.txtDireccionLugar);
            txtTipo = itemView.findViewById(R.id.txtEtiquetaLugar);
            txtDescripcion = itemView.findViewById(R.id.txtDescripcionLugar);
        }
    }

    @Override
    public InformacionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lugar, parent, false);

        return new InformacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(InformacionViewHolder holder, int position) {
        Informacion info = listaInformacion.get(position);

        holder.txtTitulo.setText(info.getTitulo());
        holder.txtSubtitulo.setText(info.getSubtitulo());
        holder.txtTipo.setText(info.getTipo());
        holder.txtDescripcion.setText(info.getDescripcion());
        holder.imgInfo.setImageResource(R.mipmap.ic_launcher);

        holder.itemView.setOnClickListener(v -> {
            if (info.getUrl() != null && !info.getUrl().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(info.getUrl()));
                holder.itemView.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaInformacion.size();
    }
}