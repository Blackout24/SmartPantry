package com.example.smartpantry.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

/**
 * Small grouped bar chart drawn with Canvas (no chart library needed).
 * One group per label (month), one bar per series (e.g. saved / wasted).
 */
public class BarChartView extends View {

    private String[] labels = new String[0];
    private String[] names = new String[0];
    private int[] colors = new int[0];
    private double[][] series = new double[0][0]; // [seriesIndex][labelIndex]

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        float scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        textPaint.setColor(0xFF888888);
        textPaint.setTextSize(11 * scaledDensity);
        textPaint.setTextAlign(Paint.Align.CENTER);
        linePaint.setColor(0x55888888);
        linePaint.setStrokeWidth(1 * density);
    }

    public void setData(String[] labels, String[] names, int[] colors, double[][] series) {
        this.labels = labels;
        this.names = names;
        this.colors = colors;
        this.series = series;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = resolveSize((int) (230 * density), heightMeasureSpec);
        setMeasuredDimension(resolveSize((int) (300 * density), widthMeasureSpec), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float pad = 8 * density;

        drawLegend(canvas, pad);

        double max = 0;
        for (double[] s : series) for (double v : s) max = Math.max(max, v);

        float top = 40 * density;
        float bottom = h - 24 * density;
        if (labels.length == 0 || max <= 0) {
            canvas.drawText("No data yet", w / 2, (top + bottom) / 2, textPaint);
            drawMonthLabels(canvas, pad, w - pad, h);
            return;
        }

        float left = pad;
        float right = w - pad;
        float groupWidth = (right - left) / labels.length;
        float barWidth = groupWidth * 0.7f / Math.max(1, series.length);
        float groupInner = barWidth * series.length;
        float valueRoom = 16 * density; // space above the tallest bar for its label
        float usable = bottom - top - valueRoom;

        canvas.drawLine(left, bottom, right, bottom, linePaint);

        for (int g = 0; g < labels.length; g++) {
            float groupLeft = left + g * groupWidth + (groupWidth - groupInner) / 2;
            for (int s = 0; s < series.length; s++) {
                double v = series[s][g];
                float barLeft = groupLeft + s * barWidth;
                if (v > 0) {
                    float barHeight = Math.max(2 * density, (float) (v / max) * usable);
                    barPaint.setColor(colors[s]);
                    canvas.drawRoundRect(new RectF(barLeft + 1 * density, bottom - barHeight,
                            barLeft + barWidth - 1 * density, bottom), 3 * density, 3 * density, barPaint);
                    canvas.drawText(format(v), barLeft + barWidth / 2, bottom - barHeight - 4 * density, textPaint);
                }
            }
        }
        drawMonthLabels(canvas, left, right, h);
    }

    private void drawMonthLabels(Canvas canvas, float left, float right, float h) {
        if (labels.length == 0) return;
        float groupWidth = (right - left) / labels.length;
        for (int g = 0; g < labels.length; g++) {
            canvas.drawText(labels[g], left + g * groupWidth + groupWidth / 2, h - 6 * density, textPaint);
        }
    }

    private void drawLegend(Canvas canvas, float pad) {
        float x = pad;
        float y = 16 * density;
        Paint left = new Paint(textPaint);
        left.setTextAlign(Paint.Align.LEFT);
        for (int s = 0; s < names.length; s++) {
            barPaint.setColor(colors[s]);
            canvas.drawRoundRect(new RectF(x, y - 9 * density, x + 10 * density, y + 1 * density),
                    2 * density, 2 * density, barPaint);
            x += 14 * density;
            canvas.drawText(names[s], x, y, left);
            x += left.measureText(names[s]) + 16 * density;
        }
    }

    private static String format(double v) {
        if (v >= 10000) return String.format(Locale.US, "%.0fk", v / 1000);
        if (v >= 1000) return String.format(Locale.US, "%.1fk", v / 1000);
        if (v >= 10 || v == Math.rint(v)) return String.format(Locale.US, "%.0f", v);
        return String.format(Locale.US, "%.1f", v);
    }
}
