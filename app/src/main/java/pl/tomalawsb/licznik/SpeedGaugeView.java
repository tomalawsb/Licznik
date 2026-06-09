package pl.tomalawsb.licznik;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Locale;

public class SpeedGaugeView extends View {
    private float speed = 0f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int GREEN = Color.rgb(34,197,94);
    private final int GREEN_DARK = Color.rgb(22,163,74);
    private final int NAVY = Color.rgb(8,24,64);
    private final int MUTED = Color.rgb(100,116,139);

    public SpeedGaugeView(Context c) { super(c); }

    public void setSpeed(float s) {
        speed = Math.max(0, s);
        invalidate();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float stroke = dp(16);
        float pad = dp(22);
        RectF arc = new RectF(pad, dp(28), w - pad, h + dp(120));

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(stroke);
        p.setColor(Color.rgb(226,232,240));
        c.drawArc(arc, 200, 140, false, p);

        float maxRange = speed <= 90 ? 90f : 180f;
        float sweep = Math.min(140f, (speed / maxRange) * 140f);
        p.setColor(GREEN);
        c.drawArc(arc, 200, sweep, false, p);

        // drobne kreski skali
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(dp(2));
        for (int i = 0; i <= 10; i++) {
            double angle = Math.toRadians(200 + (140.0 / 10.0) * i);
            float r1 = Math.min(w, h) * 0.42f;
            float r2 = r1 - dp(i % 5 == 0 ? 13 : 8);
            float x1 = cx + (float)Math.cos(angle) * r1;
            float y1 = h * 0.74f + (float)Math.sin(angle) * r1;
            float x2 = cx + (float)Math.cos(angle) * r2;
            float y2 = h * 0.74f + (float)Math.sin(angle) * r2;
            p.setColor(i % 5 == 0 ? Color.rgb(71,85,105) : Color.rgb(203,213,225));
            c.drawLine(x1, y1, x2, y2, p);
        }

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setColor(GREEN_DARK);
        p.setTextSize(dp(24));
        c.drawText("⌁", cx, dp(68), p);

        p.setColor(NAVY);
        p.setTextSize(dp(66));
        c.drawText(String.format(Locale.US, "%.1f", speed), cx, h / 2f + dp(26), p);

        p.setFakeBoldText(true);
        p.setTextSize(dp(25));
        p.setColor(MUTED);
        c.drawText("km/h", cx, h / 2f + dp(62), p);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
