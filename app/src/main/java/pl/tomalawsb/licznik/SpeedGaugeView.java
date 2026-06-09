package pl.tomalawsb.licznik;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.Locale;

public class SpeedGaugeView extends View {
    private float speed = 0f;
    private float targetSpeed = 0f;
    private ValueAnimator animator;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int GREEN = Color.rgb(34,197,94);
    private final int GREEN_DARK = Color.rgb(22,163,74);
    private final int NAVY = Color.rgb(8,24,64);
    private final int MUTED = Color.rgb(100,116,139);

    public SpeedGaugeView(Context c) { super(c); }

    public void setSpeed(float s) {
        targetSpeed = Math.max(0, s);
        if (Math.abs(targetSpeed - speed) < 0.04f) return;
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(speed, targetSpeed);
        animator.setDuration(320);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            speed = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h * 0.82f;
        float stroke = dp(15);
        float pad = dp(25);
        RectF arc = new RectF(pad, dp(14), w - pad, h + dp(150));

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(stroke);
        p.setColor(Color.rgb(226,232,240));
        c.drawArc(arc, 205, 130, false, p);

        float maxRange = speed <= 90 ? 90f : 180f;
        float sweep = Math.min(130f, (speed / maxRange) * 130f);
        p.setColor(GREEN);
        c.drawArc(arc, 205, sweep, false, p);

        p.setStrokeWidth(dp(2));
        for (int i = 0; i <= 10; i++) {
            double angle = Math.toRadians(205 + (130.0 / 10.0) * i);
            float r1 = Math.min(w, h) * 0.39f;
            float r2 = r1 - dp(i % 5 == 0 ? 13 : 8);
            float x1 = cx + (float)Math.cos(angle) * r1;
            float y1 = cy + (float)Math.sin(angle) * r1;
            float x2 = cx + (float)Math.cos(angle) * r2;
            float y2 = cy + (float)Math.sin(angle) * r2;
            p.setColor(i % 5 == 0 ? Color.rgb(71,85,105) : Color.rgb(203,213,225));
            c.drawLine(x1, y1, x2, y2, p);
        }

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setColor(GREEN_DARK);
        p.setTextSize(dp(21));
        c.drawText("⌁", cx, dp(60), p);

        p.setColor(NAVY);
        p.setTextSize(dp(60));
        c.drawText(String.format(Locale.US, "%.1f", speed), cx, h / 2f + dp(20), p);

        p.setTextSize(dp(24));
        p.setColor(MUTED);
        c.drawText("km/h", cx, h / 2f + dp(56), p);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
