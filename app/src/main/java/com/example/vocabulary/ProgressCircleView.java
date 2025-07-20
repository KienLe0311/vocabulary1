package com.example.vocabulary;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

public class ProgressCircleView extends View {

    private float progress = 0f; // 0.0 - 1.0

    private Paint backgroundPaint, progressPaint, textPaint;

    public ProgressCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.LTGRAY);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(25);
        backgroundPaint.setAntiAlias(true);

        progressPaint = new Paint();
        progressPaint.setColor(Color.BLUE);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(25);
        progressPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(48);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
    }

    public void setProgress(float value) {
        this.progress = Math.max(0, Math.min(1, value));
        invalidate(); // gọi vẽ lại
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 30;

        // Nền xám
        canvas.drawCircle(cx, cy, radius, backgroundPaint);

        // Tiến độ màu xanh
        float sweepAngle = progress * 360;
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                -90, sweepAngle, false, progressPaint);

        // Phần trăm
        String percentText = (int) (progress * 100) + "%";
        canvas.drawText(percentText, cx, cy + 15, textPaint);
    }
}
