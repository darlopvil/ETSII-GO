package es.us.etsii_go.horariogps.horario;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import es.us.etsii_go.R;
import es.us.etsii_go.horariogps.horario.models.CeldaHorario;

import java.util.List;

public class HorarioAdapter extends RecyclerView.Adapter<HorarioAdapter.ViewHolder> {

    public interface OnHorarioActionListener {
        void onEditarCelda(int posicion, CeldaHorario celda);

    }
    private List<CeldaHorario> lista;
    private OnHorarioActionListener actionListener;

    public HorarioAdapter(List<CeldaHorario> lista,OnHorarioActionListener listener) {
        this.lista = lista;
        this.actionListener = listener;
    }

    public void setList(List<CeldaHorario> nuevaLista) {
        this.lista = nuevaLista;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView hora, contenido, aula;
        LinearLayout contenedor;

        public ViewHolder(View itemView) {
            super(itemView);
            hora = itemView.findViewById(R.id.textHora);
            contenido = itemView.findViewById(R.id.textAsignatura);
            aula = itemView.findViewById(R.id.textAula);
            contenedor = itemView.findViewById(R.id.contenedor_celda);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horario_celda, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // Cuando le pasas la lista de celdas, en cada posicion la pinta.
        CeldaHorario celda = lista.get(position);

        holder.contenedor.setBackgroundColor(celda.getColor());
        holder.hora.setText(celda.getHora());
        holder.contenido.setText(celda.getContenido());
        holder.aula.setText(celda.getAula());

        holder.itemView.setOnClickListener(v -> {

            // Para los errores de posicion
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION && actionListener != null) {

                CeldaHorario celdaActual = lista.get(currentPosition);
                actionListener.onEditarCelda(currentPosition, celdaActual);
            }
        });

    }
}