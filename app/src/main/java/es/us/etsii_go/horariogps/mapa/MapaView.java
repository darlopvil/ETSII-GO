package es.us.etsii_go.horariogps.mapa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

public class MapaView extends SubsamplingScaleImageView {

    private PointF puntoAdibujar;
    private PointF aulacoords;      // Coordenada (source) del aula seleccionada
    private String aulaNombre;   // Nombre del aula
    private int aulaColor;       // Color del punto del aula

    private final Paint paint = new Paint();
    private final Paint textPaint = new Paint();

    //private float rotationAngle = 0f;

    public MapaView(Context context) {
        this(context, null);
    }

    public MapaView(Context context, AttributeSet attr) {
        super(context, attr);
        initialise();
    }

    private void initialise() {
        paint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(35);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    // Metodo para actualizar la posición del usuario (punto rojo)
    public void dibujar(PointF punto) {
        this.puntoAdibujar = punto;
        invalidate();
    }

    // Metodo para configurar el aula objetivo
    public void setAulaData(PointF sPin, String nombre, int color) {
        this.aulacoords = sPin;
        this.aulaNombre = nombre;
        this.aulaColor = color;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // No dibujar nada si la imagen no está lista
        if (!isReady()) return;

        // 1. Dibujar el Aula Seleccionada
        if (aulacoords != null) {
            PointF vPin = sourceToViewCoord(aulacoords);
            if (vPin != null) {
                paint.setColor(aulaColor);
                canvas.drawCircle(vPin.x, vPin.y, 35, paint);
                if (aulaNombre != null) {
                    canvas.drawText(aulaNombre, vPin.x, vPin.y + 60, textPaint);
                }
            }
        }

        // 2. Dibujar la Ubicación del Usuario (Punto Rojo)
        if (puntoAdibujar != null) {
            PointF vPin = sourceToViewCoord(puntoAdibujar);
            if (vPin != null) {
                paint.setColor(Color.RED);
                // Dibujamos un borde blanco sutil para que resalte
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                canvas.drawCircle(vPin.x, vPin.y, 42, paint);

                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(vPin.x, vPin.y, 40, paint);

                canvas.drawText("Aquí estás", vPin.x, vPin.y + 70, textPaint);
            }
        }
    }
}