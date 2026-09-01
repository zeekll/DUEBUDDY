package com.example.duebuddy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight donut-style pie chart used on the Home dashboard to show
 * the breakdown of bills by status (Paid, Unpaid, Due Soon, Overdue).
 * Implemented as a plain custom View (Canvas arcs) so the app doesn't
 * need to pull in a third-party charting library for one chart.
 */
public class PieChartView extends View {

    public static class Slice {
        public final String label;
        public final float value;
        public final int color;

        public Slice(String label, float value, int color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private final List<Slice> slices = new ArrayList<>();
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();

    private final int emptyColor = Color.rgb(230, 230, 230);
    private final float strokeWidthFraction = 0.34f;

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
    }

    /**
     * Replaces the chart's data and redraws. Slices with a value of
     * zero are skipped. If every value is zero, an empty gray ring
     * is shown instead of a blank view.
     */
    public void setSlices(List<Slice> newSlices) {
        slices.clear();

        if (newSlices != null) {
            slices.addAll(newSlices);
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int size = Math.min(getWidth(), getHeight());

        if (size <= 0) {
            return;
        }

        float strokeWidth = size * strokeWidthFraction;
        arcPaint.setStrokeWidth(strokeWidth);

        float inset = strokeWidth / 2f + 4f;
        float left = (getWidth() - size) / 2f + inset;
        float top = (getHeight() - size) / 2f + inset;
        float right = left + size - inset * 2;
        float bottom = top + size - inset * 2;
        arcBounds.set(left, top, right, bottom);

        float total = 0f;
        for (Slice slice : slices) {
            total += Math.max(0f, slice.value);
        }

        if (total <= 0f) {
            arcPaint.setColor(emptyColor);
            canvas.drawArc(arcBounds, 0f, 360f, false, arcPaint);
            return;
        }

        float startAngle = -90f;

        for (Slice slice : slices) {
            float value = Math.max(0f, slice.value);

            if (value <= 0f) {
                continue;
            }

            float sweep = (value / total) * 360f;

            arcPaint.setColor(slice.color);
            canvas.drawArc(arcBounds, startAngle, sweep, false, arcPaint);

            startAngle += sweep;
        }
    }
}