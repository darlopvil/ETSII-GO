package es.us.etsii_go.horariogps.horario.aulas;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import es.us.etsii_go.R;
import es.us.etsii_go.horariogps.horario.models.Aula;

import java.util.List;

public class AulaAdapter extends RecyclerView.Adapter<AulaAdapter.AulaViewHolder> {

    // Adapter para poner las aulas en la vista de selección de aulas.
    private List<Aula> listaAulas;
    private OnAulaClickListener listener;

    public interface OnAulaClickListener {
        void onAulaClick(Aula aula);
    }

    public AulaAdapter(List<Aula> listaAulas, OnAulaClickListener listener) {
        this.listaAulas = listaAulas;
        this.listener = listener;
    }

    public void updateList(List<Aula> nuevaLista) {
        this.listaAulas = nuevaLista;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AulaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_horario_aula, parent, false);
        return new AulaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AulaViewHolder holder, int position) {

        Context context = holder.itemView.getContext();

        Aula aula = listaAulas.get(position);
        holder.tvNombre.setText(aula.nombre);

        String textoFormateado = context.getString(R.string.detalles_aula,aula.modulo,aula.planta);
        holder.tvDetalles.setText(textoFormateado);

        holder.itemView.setOnClickListener(v -> listener.onAulaClick(aula));
    }

    @Override
    public int getItemCount() { return listaAulas.size(); }

    static class AulaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvDetalles;
        AulaViewHolder(View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvDetalles = v.findViewById(R.id.tvDetalles);
        }
    }
}
