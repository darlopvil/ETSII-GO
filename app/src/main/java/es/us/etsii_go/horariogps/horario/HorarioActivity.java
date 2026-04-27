package es.us.etsii_go.horariogps.horario;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import es.us.etsii_go.R;
import es.us.etsii_go.horariogps.horario.aulas.AulaAdapter;
import es.us.etsii_go.horariogps.horario.aulas.RepositorioAulas;
import es.us.etsii_go.horariogps.horario.db.AppDatabase;
import es.us.etsii_go.horariogps.horario.models.Aula;
import es.us.etsii_go.horariogps.horario.models.CeldaHorario;
import es.us.etsii_go.horariogps.mapa.MapaActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.otaliastudios.zoom.ZoomEngine;
import com.otaliastudios.zoom.ZoomLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import es.us.etsii_go.horariogps.mapa.escaneo.ScannerActivity;
import yuku.ambilwarna.AmbilWarnaDialog;

public class HorarioActivity extends AppCompatActivity implements HorarioAdapter.OnHorarioActionListener{

    private AppDatabase db;
    private HorarioAdapter adapter;
    private List<CeldaHorario> listaCeldaHorario = new ArrayList<>();

    RecyclerView recyclerView;
    List<CeldaHorario> lista;
    ZoomLayout zoomLayout;
    HorizontalScrollView containerDays;
    ScrollView containerHours;
    LinearLayout layoutDias, layoutHoras;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Esto lo pongo porque el mapa se me ponia oscuro cuando tenia el modo oscuro
        // de mi movil, con esto le obligo a que no funcione el modo noche.
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        EdgeToEdge.enable(this);

        db = AppDatabase.getInstancia(this);

        setContentView(R.layout.view_horario_main);

        recyclerView = findViewById(R.id.recyclerHorario);
        zoomLayout = findViewById(R.id.zoomLayout);
        containerDays = findViewById(R.id.containerDays);
        containerHours = findViewById(R.id.containerHours);

        // Para evitar el movimiento de las cabeceras, asi no se descuadra el horario
        containerHours.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        containerDays.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });


        layoutDias = findViewById(R.id.headerDays);
        layoutHoras = findViewById(R.id.headerHours);

        adapter = new HorarioAdapter(listaCeldaHorario,this);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        recyclerView.setAdapter(adapter);

        new Thread(() -> {
            // Aqui llamo a la funcion que carga los datos de la BD, y si no existen, crea una celda vacia.
            cargarDatos();
        }).start();

        crearCabeceras();

        setupZoomEngine();

        ExtendedFloatingActionButton btnMapaDirecto = findViewById(R.id.btnIrMapaDirecto);
        btnMapaDirecto.setOnClickListener(v -> {
            Intent intent = new Intent(this,MapaActivity.class);
            startActivity(intent);
        });
    }

    private void cargarDatos() {
        // Funcion que carga los datos para el recycler view, creados manualmente
        // y los que ya tenemos de la base de datos

        // 1. Obtenemos los datos de la BD
        List<CeldaHorario> datosBD = db.horarioDao().obtenerTodo();

        // 2. Creamos el diccionario para una búsqueda rápida
        // La clave será un String tipo "1-09:00", con el primero el id correspondiente a dias
        // Esto lo hago por las traducciones, para no complicarme con los dias
        Map<String, CeldaHorario> diccionarioBD = new HashMap<>();
        for (CeldaHorario celda : datosBD) {
            String clave = celda.getId() + "-" + celda.getHora();
            diccionarioBD.put(clave, celda);
        }

        // Aqui creo las 6x5= 30 celdas que se componen en el horario.
        lista = new ArrayList<>();
        String[] string_dias = {getString(R.string.dia_lunes), getString(R.string.dia_martes), getString(R.string.dia_miercoles), getString(R.string.dia_jueves), getString(R.string.dia_viernes)};
        String[] horas = {"8:30 - 10:20","10:40 - 12:30","12:40 - 14:30","15:30 - 17:20","17:40 - 19:30","19:40 - 21:30"};
        for (String hora : horas) {
            for (int id = 0; id < string_dias.length; id++) {
                lista.add(new CeldaHorario(id,string_dias[id], hora , "", Color.TRANSPARENT));
            }
        }

        // Buscamos coincidencias en la base de datos para sustituir las celdas creadas
        // por las que tenemos en la base de datos.

        for (CeldaHorario celda : lista) {
            String claveBusqueda = celda.getId() + "-" + celda.getHora();

            if (diccionarioBD.containsKey(claveBusqueda)) {
                // Sustituimos los datos
                CeldaHorario coincidencia = diccionarioBD.get(claveBusqueda);
                celda.setContenido(coincidencia.getContenido());
                celda.setAula(coincidencia.getAula());
                celda.setColor(coincidencia.getColor());
            }
        }
        runOnUiThread(() -> {
            adapter.setList(lista); // Pasar los datos nuevos al adapter
            adapter.notifyDataSetChanged();
        });
    }

    private void crearCabeceras() {
        // Funcion que crea el scroll view y horizontal scroll view de las cabeceras, con inflater.

        String[] dias = {getString(R.string.dia_lunes), getString(R.string.dia_martes), getString(R.string.dia_miercoles), getString(R.string.dia_jueves), getString(R.string.dia_viernes)};
        String[] horas = {"8:30 - 10:20","10:40 - 12:30","12:40 - 14:30","15:30 - 17:20","17:40 - 19:30","19:40 - 21:30"};
        LayoutInflater inflater = LayoutInflater.from(this);

        // Limpiar por si acaso
        layoutDias.removeAllViews();
        layoutHoras.removeAllViews();

        // Inflar Días
        for (String dia : dias) {
            View v = inflater.inflate(R.layout.item_horario_cabecera_dia, layoutDias, false);
            TextView tv = v.findViewById(R.id.textTabDias);
            tv.setText(dia);
            layoutDias.addView(v);
        }

        // Inflar Horas
        for (String hora : horas) {
            View v = inflater.inflate(R.layout.item_horario_cabecera_horas, layoutHoras, false);
            TextView tv = v.findViewById(R.id.textTabHoras);
            tv.setText(hora);
            layoutHoras.addView(v);
        }
    }

    private void setupZoomEngine() {
        //Funcion que controla los valores que hacemos zoom y deslizamientos para
        // conectarlos a las cabeceras y al texto de estas, para que el horario no
        // se descuadre.
        zoomLayout.getEngine().addListener(new ZoomEngine.Listener() {
            @Override
            public void onUpdate(@NonNull ZoomEngine engine, @NonNull Matrix matrix) {
                // 1. Extraemos los valores matemáticos de la vista
                float[] values = new float[9];
                matrix.getValues(values);

                // 2. Obtenemos cuánto se ha ampliado en X (horizontal) y en Y (vertical)
                float scaleX = values[Matrix.MSCALE_X];
                float scaleY = values[Matrix.MSCALE_Y];

                // 3. Extraemos Traslación (Movimiento/Desplazamiento)
                float transX = values[Matrix.MTRANS_X];
                float transY = values[Matrix.MTRANS_Y];

                // 4. Llamamos al metodo para actualizar la interfaz
                actualizarTodo(scaleX, scaleY, transX, transY);
            }

            @Override
            public void onIdle(@NonNull ZoomEngine engine) {
            }
        });
    }

    private void actualizarTodo(float scaleX, float scaleY, float transX, float transY) {

        //CABECERA DÍAS
        containerDays.setPivotX(0f);
        containerDays.setPivotY(0f);
        containerDays.setScaleX(scaleX);
        // Para sincronizar el desplazamiento horizontal
        containerDays.setTranslationX(transX);

        //CABECERA HORAS
        containerHours.setPivotX(0f);
        containerHours.setPivotY(0f);
        containerHours.setScaleY(scaleY);
        // Para sincronizar el desplazamiento vertical
        containerHours.setTranslationY(transY);

        //ESCALA INVERSA PARA EL TEXTO
        aplicarEscalaCabeceras(scaleX, scaleY);
    }

    private void aplicarEscalaCabeceras(float scaleX, float scaleY) {
        // Funcion que aplica la escala de texto a las cabeceras, para que no se muevan
        // al hacer zoom.

        // Calculamos la escala inversa para el texto (si el padre es 2x, el texto es 0.5x)
        float escalaInversaDias = 1.0f / scaleX;
        // Igual para las horas
        float escalaInversaHoras = 1.0f / scaleY;

        // CABECERA DIAS
        for (int i = 0; i < layoutDias.getChildCount(); i++) {
            View celda = layoutDias.getChildAt(i);
                TextView textoDia = celda.findViewById(R.id.textTabDias);
            if (textoDia != null) {
                textoDia.setScaleX(escalaInversaDias);
            }
        }
        // CABECERA HORAS
        for (int i = 0; i < layoutHoras.getChildCount(); i++) {
            View celda = layoutHoras.getChildAt(i);
            TextView textoHora = celda.findViewById(R.id.textTabHoras);
            if (textoHora != null) {
                textoHora.setScaleY(escalaInversaHoras);
            }
        }
    }


    private void onIrAlMapa(CeldaHorario celdaActual, int colorSeleccionado) {
        // Funcion que se ejecuta cuando se le da al boton de ir al mapa.

        String nombreAula = celdaActual.getAula();

        if (nombreAula == null || nombreAula.isEmpty() || nombreAula.equals(getString(R.string.aula_no_asignada))) {
            // Si no hay aula, mostramos un mensaje para evitar errores en el MapaActivity.
            Toast.makeText(this, R.string.selecciona_un_aula_primero, Toast.LENGTH_SHORT).show();
            return;
        }

        // Ir a MapaActivity
        Intent intent = new Intent(this, MapaActivity.class);
        intent.putExtra("nombre_aula", nombreAula);
        intent.putExtra("color_celda", colorSeleccionado);
        this.startActivity(intent);
        Toast.makeText(this, getString(R.string.navegando_a_detalles_de) + nombreAula, Toast.LENGTH_SHORT).show();

    }
    private void abrirSelectorColor(int colorInicial, View colorPreview, int[] colorSeleccionado) {
        // Selector de AmbilWarnaDialog
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, colorInicial, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
            }
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                // 1. Actualizamos el valor del color
                colorSeleccionado[0] = color;
                // 2. Cambiamos visualmente el cuadrito de muestra en el diálogo
                colorPreview.setBackgroundColor(color);
            }
        });
        colorPicker.show();
    }

    @Override
    public void onEditarCelda(int currentPosition, CeldaHorario celdaActual) {

        View dialogoView = LayoutInflater.from(this).inflate(R.layout.dialogo_horario_editar, null);

        // Vincular las vistas del XML del diálogo
        EditText editContenido = dialogoView.findViewById(R.id.edit_contenido);
        TextView textAula = dialogoView.findViewById(R.id.text_aula);
        Button seleccionAula = dialogoView.findViewById(R.id.seleccion_aula);
        Button btnIrAula = dialogoView.findViewById(R.id.btn_ir_mapa);
        View colorPreview = dialogoView.findViewById(R.id.view_color_preview);
        Button btnCambiarColor = dialogoView.findViewById(R.id.btn_cambiar_color);
        Button btnColoresRecientes = dialogoView.findViewById(R.id.btn_colores_recientes);

        // Para poder editar
        editContenido.setText(celdaActual.getContenido());
        textAula.setText(celdaActual.getAula());

        final int[] colorSeleccionado = {celdaActual.getColor()};
        GradientDrawable gd = (GradientDrawable) colorPreview.getBackground().mutate();
        gd.setColor(colorSeleccionado[0]);

        // BOTON CAMBIAR COLOR
        btnCambiarColor.setOnClickListener(v -> {
            // Llamamos a la función pasándole los datos necesarios
            abrirSelectorColor(colorSeleccionado[0], colorPreview, colorSeleccionado);

            //Cambio el color de la celda
            celdaActual.setColor(colorSeleccionado[0]);
        });

        // BOTON COLORES RECIENTES
        btnColoresRecientes.setOnClickListener(v4 -> {
            // 1. Inflar el diseño
            View viewGrid = LayoutInflater.from(this).inflate(R.layout.dialogo_horario_editar_coloresrecientes, null);
            GridLayout grid = viewGrid.findViewById(R.id.gridColores);

            // 2. Crear el sub-diálogo
            AlertDialog dialogRecientes = new AlertDialog.Builder(this)
                    .setTitle(R.string.colores_usados)
                    .setView(viewGrid)
                    .setNegativeButton(R.string.cerrar, null)
                    .create();

            // 3. Cargar los colores en el grid
            new Thread(() -> {
                List<Integer> colores = db.horarioDao().obtenerColoresRecientes();

                ((Activity) this).runOnUiThread(() -> {
                    for (Integer col : colores) {
                        // Crear el cuadrito de color
                        View card = new View(this);
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        params.width = 120;
                        params.height = 120;
                        params.setMargins(15, 15, 15, 15);
                        card.setLayoutParams(params);
                        card.setBackgroundColor(col);

                        // Al tocar un color del historial
                        card.setOnClickListener(v1 -> {
                            colorSeleccionado[0] = col; // Actualizamos el color elegido
                            colorPreview.setBackgroundColor(col); // Actualizamos la vista previa
                            dialogRecientes.dismiss(); // Cerramos el subdialogo
                        });
                        grid.addView(card);
                    }
                });
            }).start();
            dialogRecientes.show();
        });

        // BOTON SELECCIONAR AULA
        seleccionAula.setOnClickListener(v5 -> {
            // Usamos el context de la propia vista del botón
            mostrarDialogoBuscador(v5.getContext(),celdaActual, textAula);
        });

        // BOTON IR AL MAPA
        btnIrAula.setOnClickListener(v2 -> {
            onIrAlMapa(celdaActual,colorSeleccionado[0]);
        });

        // BOTONES DE BORRAR, CANCELAR Y GUARDAR
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.editar)
                .setView(dialogoView)

                .setPositiveButton(R.string.guardar, (dialog, which) -> {
                    celdaActual.setContenido(editContenido.getText().toString());
                    celdaActual.setAula(textAula.getText().toString());
                    celdaActual.setColor(colorSeleccionado[0]);
                    adapter.notifyItemChanged(currentPosition);

                    // Actualizar la base de datos
                    new Thread(() -> {
                        // Si ya existe en la BD (mismo ID), lo reemplazará
                        db.horarioDao().insertar(celdaActual);

                        // 4. Volver al hilo principal para refrescar solo esa celda
                        ((Activity) this).runOnUiThread(() -> {
                            adapter.notifyItemChanged(currentPosition);
                        });
                    }).start();
                })
                .setNegativeButton(R.string.cancelar, null)
                .setNeutralButton(R.string.borrar, (dialog, which) -> {
                    // Mensaje para confirmar el borrado
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.confirmar_borrado)
                            .setPositiveButton(R.string.borrar, (dialogConfirm, whichConfirm) -> {
                                // Ejecutamos el borrado en la base de datos
                                new Thread(() -> {
                                    db.horarioDao().borrar(celdaActual);

                                    // Limpiamos los datos del objeto en la lista
                                    celdaActual.setContenido("");
                                    celdaActual.setAula("");
                                    celdaActual.setColor(Color.WHITE);

                                    // Refrescamos la interfaz
                                    ((Activity) this).runOnUiThread(() -> {
                                        adapter.notifyItemChanged(currentPosition);
                                        Toast.makeText(this, R.string.contenido_eliminado, Toast.LENGTH_SHORT).show();
                                    });
                                }).start();
                            })

                            .setNegativeButton(R.string.cancelar, (dialogConfirm, whichConfirm) -> {
                                // Si dice que no, el diálogo se cierra y no pasa nada
                                dialogConfirm.dismiss();
                            })
                            .show();
                })
                .show();

    }

    private void mostrarDialogoBuscador(Context context, CeldaHorario celdaActual, TextView targetTextView) {
        // Funcion que muestra la pantalla de buscar aula.

        // 1. Cargar datos iniciales
        List<Aula> todasLasAulas = RepositorioAulas.getTodasLasAulas(context);

        List<Aula> listaFiltrada = new ArrayList<>(todasLasAulas);

        // 2. Inflar la vista del diálogo
        View view = LayoutInflater.from(context).inflate(R.layout.dialogo_horario_editar_buscaraula, null);

        EditText etBuscador = view.findViewById(R.id.etBuscador);
        Spinner spModulo = view.findViewById(R.id.spinnerModulo);
        Spinner spPlanta = view.findViewById(R.id.spinnerPlanta);
        RecyclerView rvAulas = view.findViewById(R.id.recyclerViewAulas);

        // 3. Crear el Diálogo
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.seleccionar_aula)
                .setView(view)
                .setNegativeButton(R.string.cancelar, null)
                .create();

        // 4. Configurar el RecyclerView de las aulas
        rvAulas.setLayoutManager(new LinearLayoutManager(context));
        AulaAdapter aulaAdapter = new AulaAdapter(listaFiltrada, aula -> {

            // Cuando seleccionamos el aula, se guarda en la celda actual
            Toast.makeText(context, getString(R.string.aula_seleccionada) + aula.nombre, Toast.LENGTH_SHORT).show();

            targetTextView.setText(aula.nombre);
            targetTextView.setTypeface(null, Typeface.BOLD);
            targetTextView.setTextColor(Color.WHITE);
            celdaActual.setAula(aula.nombre);
            dialog.dismiss();
        });
        rvAulas.setAdapter(aulaAdapter);

        // 5. Configurar los Spinners (filtros)
        configurarSpinners(context, todasLasAulas, spModulo, spPlanta);

        // 6. Lógica de Filtrado dinámico
        Runnable filtrar = () -> {
            String texto = etBuscador.getText().toString().toLowerCase().trim();
            String modSel = spModulo.getSelectedItem().toString();
            String plaSel = spPlanta.getSelectedItem().toString();

            List<Aula> resultados = new ArrayList<>();
            for (Aula a : todasLasAulas) {
                // Filtro de texto (nombre)
                boolean coincideTexto = texto.isEmpty() || a.nombre.toLowerCase().contains(texto);

                // Filtro de módulo
                boolean coincideModulo = modSel.equals(getString(R.string.modulo_todos)) || (a.modulo != null && a.modulo.equals(modSel));

                // Filtro de planta
                boolean coincidePlanta = plaSel.equals(getString(R.string.planta_todas)) || String.valueOf(a.planta).equals(plaSel);

                if (coincideTexto && coincideModulo && coincidePlanta) {
                    resultados.add(a);
                }
            }
            aulaAdapter.updateList(resultados);
        };

        // 7. Asignar los Listeners para que el filtro sea en tiempo real
        etBuscador.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { filtrar.run(); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) { filtrar.run(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };

        spModulo.setOnItemSelectedListener(spinnerListener);
        spPlanta.setOnItemSelectedListener(spinnerListener);

        // 8. Mostrar el diálogo
        dialog.show();
    }

    private void configurarSpinners(Context context, List<Aula> todasLasAulas, Spinner spModulo, Spinner spPlanta) {
        // TreeSet garantiza que los elementos sean únicos y estén ordenados alfabéticamente
        Set<String> modulosSet = new TreeSet<>();
        Set<String> plantasSet = new TreeSet<>();

        // Opciones por defecto
        modulosSet.add(getString(R.string.modulo_todos));
        plantasSet.add(getString(R.string.planta_todas));

        // Extraer datos del JSON
        for (Aula a : todasLasAulas) {
            if (a.modulo != null && !a.modulo.isEmpty()) {
                modulosSet.add(a.modulo);
            }
            plantasSet.add(String.valueOf(a.planta));
        }

        // Adaptador para Módulos
        ArrayAdapter<String> adapterModulo = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(modulosSet));
        spModulo.setAdapter(adapterModulo);

        // Adaptador para Plantas
        ArrayAdapter<String> adapterPlanta = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(plantasSet));
        spPlanta.setAdapter(adapterPlanta);
    }



}