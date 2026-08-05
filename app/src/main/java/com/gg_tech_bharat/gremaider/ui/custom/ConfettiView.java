package com.gg_tech_bharat.gremaider.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfettiView extends View {

    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private boolean isAnimating = false;
    private long animationStartTime = 0;
    private static final long DURATION_MS = 3000; // 3 seconds

    private static final int[] COLORS = {
            Color.parseColor("#4A90E2"), // Premium Blue
            Color.parseColor("#E2849A"), // Premium Coral/Pink
            Color.parseColor("#F5A623"), // Golden Yellow
            Color.parseColor("#7ED321"), // Bright Green
            Color.parseColor("#9B59B6"), // Soft Violet
            Color.parseColor("#50E3C2")  // Teal
    };

    public ConfettiView(Context context) {
        super(context);
        init();
    }

    public ConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ConfettiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Set up paint
    }

    public void startConfetti() {
        particles.clear();
        int width = getWidth();
        int height = getHeight();
        
        if (width == 0 || height == 0) {
            // If the view is not laid out yet, we will defer
            postDelayed(this::startConfetti, 100);
            return;
        }

        // Spawn 80 particles
        for (int i = 0; i < 80; i++) {
            particles.add(new Particle(width, height));
        }

        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isAnimating) return;

        long elapsed = System.currentTimeMillis() - animationStartTime;
        if (elapsed > DURATION_MS) {
            isAnimating = false;
            particles.clear();
            return;
        }

        boolean activeParticlesLeft = false;

        for (Particle p : particles) {
            p.update();
            p.draw(canvas);
            if (p.y < getHeight()) {
                activeParticlesLeft = true;
            }
        }

        if (activeParticlesLeft) {
            postInvalidateOnAnimation();
        } else {
            isAnimating = false;
            particles.clear();
        }
    }

    private class Particle {
        float x, y;
        float vx, vy;
        float size;
        int color;
        float angle;
        float angularSpeed;
        Paint paint;

        Particle(int screenWidth, int screenHeight) {
            // Spawn at random x, slightly above the screen
            this.x = random.nextFloat() * screenWidth;
            this.y = -random.nextFloat() * 100 - 20;
            
            // Random horizontal speed (wind effect)
            this.vx = (random.nextFloat() - 0.5f) * 6;
            
            // Random falling speed
            this.vy = random.nextFloat() * 10 + 6;
            
            // Random size (10dp to 24dp equivalent)
            this.size = random.nextFloat() * 20 + 10;
            
            // Random color
            this.color = COLORS[random.nextInt(COLORS.length)];
            
            this.angle = random.nextFloat() * 360;
            this.angularSpeed = (random.nextFloat() - 0.5f) * 15;

            this.paint = new Paint();
            this.paint.setColor(color);
            this.paint.setStyle(Paint.Style.FILL);
            this.paint.setAntiAlias(true);
        }

        void update() {
            x += vx;
            y += vy;
            angle += angularSpeed;

            // Apply light gravity
            vy += 0.1f;
        }

        void draw(Canvas canvas) {
            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(angle);
            
            // Draw rectangle confetti particle
            canvas.drawRect(-size / 2, -size / 4, size / 2, size / 4, paint);
            
            canvas.restore();
        }
    }
}
