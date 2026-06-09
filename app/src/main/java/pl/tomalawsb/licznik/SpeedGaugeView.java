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
    private final int TRACK = Color.rgb(226,232,240);
    private final int TICK = Color.rgb(148,163,184);

    public SpeedGaugeView(Context c) { super(c); }

    public void setSpeed(float s) {
        targetSpeed = Math.max(0, s);
        if (Math.abs(targetSpeed - speed) < 0.03f) return;
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(speed, targetSpeed);
        animator.setDuration(480);
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
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float stroke = dp(9);
        float sidePad = dp(32);
        float top = dp(22);
        float bottom = h + dp(98);
        RectF arc = new RectF(sidePad, top, w - sidePad, bottom);
        float arcCx = arc.centerX();
        float arcCy = arc.centerY();
        float startAngle = 205f;
        float totalSweep = 130f;
        float maxRange = speed <= 90f ? 90f : 180f;
        float sweep = Math.min(totalSweep, Math.max(0f, (speed / maxRange) * totalSweep));

        // 1. Tło łuku: cieniej i spokojniej, bliżej mockupu.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(stroke);
        p.setColor(TRACK);
        c.drawArc(arc, startAngle, totalSweep, false, p);

        // 2. Zielony postęp łuku.
        if (sweep > 0.35f) {
            p.setColor(GREEN);
            c.drawArc(arc, startAngle, sweep, false, p);
        }

        // 3. Mały punkt na końcu aktywnego łuku — tylko kiedy jest realny postęp.
        if (sweep > 2f) {
            float endDeg = startAngle + sweep;
            float endRad = (float) Math.toRadians(endDeg);
            float rx = arc.width() / 2f;
            float ry = arc.height() / 2f;
            float x = arcCx + (float)Math.cos(endRad) * rx;
            float y = arcCy + (float)Math.sin(endRad) * ry;
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            c.drawCircle(x, y, dp(7), p);
            p.setColor(GREEN);
            c.drawCircle(x, y, dp(4), p);
        }

        // 4. Kreski licznika — wyżej, bliżej łuku, bez wchodzenia w cyfry.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i <= 12; i++) {
            float a = startAngle + (totalSweep / 12f) * i;
            float rad = (float)Math.toRadians(a);
            boolean major = i % 3 == 0;
            p.setStrokeWidth(major ? dp(2) : dp(1));
            p.setColor(major ? Color.rgb(71,85,105) : TICK);
            float rxOuter = arc.width() / 2f - stroke * 1.75f;
            float ryOuter = arc.height() / 2f - stroke * 1.75f;
            float len = major ? dp(13) : dp(8);
            float rxInner = rxOuter - len;
            float ryInner = ryOuter - len;
            float x1 = arcCx + (float)Math.cos(rad) * rxOuter;
            float y1 = arcCy + (float)Math.sin(rad) * ryOuter;
            float x2 = arcCx + (float)Math.cos(rad) * rxInner;
            float y2 = arcCy + (float)Math.sin(rad) * ryInner;
            c.drawLine(x1, y1, x2, y2, p);
        }

        // 5. Subtelna mini-igła nad cyframi, mniejsza niż wcześniej.
        float needleCy = h * 0.30f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(240,253,244));
        c.drawCircle(cx, needleCy, dp(13), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(dp(2));
        p.setColor(GREEN_DARK);
        c.drawCircle(cx, needleCy, dp(10), p);
        p.setStrokeWidth(dp(3));
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(cx, needleCy, cx + dp(7), needleCy - dp(7), p);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, needleCy, dp(2), p);

        // 6. Cyfry prędkości przesunięte wyżej, jak w mockupie.
        String speedText = String.format(Locale.US, "%.1f", speed);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setColor(NAVY);
        p.setTextSize(dp(speedText.length() > 4 ? 50 : 60));
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(0);
        c.drawText(speedText, cx, h * 0.51f, p);

        // 7. Jednostka bliżej cyfr, bez sztucznie dużej przerwy.
        p.setFakeBoldText(false);
        p.setTextSize(dp(23));
        p.setColor(MUTED);
        c.drawText("km/h", cx, h * 0.68f, p);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
