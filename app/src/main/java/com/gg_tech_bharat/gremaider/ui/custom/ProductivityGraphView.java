package com.gg_tech_bharat.gremaider.ui.custom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ProductivityGraphView extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private Paint textPaint;
    private Paint labelPaint;

    private int progress = 0;
    private float animatedProgress = 0f;
    private RectF rectF;
    private final float strokeWidth = 24f;

    // Linear gradient colors for progress arc
    private int startColor = Color.parseColor("#4A90E2"); // Samsung/OneUI Premium Blue
    private int endColor = Color.parseColor("#7B68EE");   // Medium Slate Blue

    public ProductivityGraphView(Context context) {
        super(context);
        init();
    }

    public ProductivityGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProductivityGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int colorTitle = androidx.core.content.ContextCompat.getColor(getContext(), com.gg_tech_bharat.gremaider.R.color.text_title_light);
        int colorSubtitle = androidx.core.content.ContextCompat.getColor(getContext(), com.gg_tech_bharat.gremaider.R.color.text_subtitle_light);
        int colorDivider = androidx.core.content.ContextCompat.getColor(getContext(), com.gg_tech_bharat.gremaider.R.color.grey_light);

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(colorDivider);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);
        backgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(colorTitle);
        textPaint.setTextSize(54f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(colorSubtitle);
        labelPaint.setTextSize(26f);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();
    }

    public void setProgress(int targetProgress) {
        if (targetProgress < 0) targetProgress = 0;
        if (targetProgress > 100) targetProgress = 100;
        
        int oldProgress = this.progress;
        this.progress = targetProgress;

        ValueAnimator animator = ValueAnimator.ofFloat(oldProgress, targetProgress);
        animator.setDuration(1000);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animatedProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float padding = strokeWidth + 10f;
        rectF.set(padding, padding, w - padding, h - padding);

        // Apply dynamic gradient
        Shader shader = new LinearGradient(
                rectF.left, rectF.top,
                rectF.right, rectF.bottom,
                startColor, endColor,
                Shader.TileMode.CLAMP
        );
        progressPaint.setShader(shader);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw track
        canvas.drawOval(rectF, backgroundPaint);

        // Draw sweeping progress arc (start from -90 degrees, which is 12 o'clock)
        float sweepAngle = (animatedProgress / 100f) * 360f;
        canvas.drawArc(rectF, -90, sweepAngle, false, progressPaint);

        // Draw score text in the center
        String scoreText = Math.round(animatedProgress) + "%";
        float textY = (getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(scoreText, getWidth() / 2f, textY - 10f, textPaint);

        // Draw label text
        canvas.drawText("Score Today", getWidth() / 2f, textY + 30f, labelPaint);
    }

    // Set colors dynamically
    public void setColors(int startColor, int endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
        // Recreate shader next draw
        requestLayout();
    }
}
