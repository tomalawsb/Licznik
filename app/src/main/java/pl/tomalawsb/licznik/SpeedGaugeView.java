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
    private final int NAVY = Color.rgb(8,24,64);

    public SpeedGaugeView(Context c) { super(c); }
    public void setSpeed(float s) { speed = Math.max(0, s); invalidate(); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth();
        int h = getHeight();
        float stroke = dp(18);
        float pad = dp(22);
        RectF r = new RectF(pad, pad + dp(16), w - pad, h + dp(92));

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(stroke);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(Color.rgb(226,232,240));
        c.drawArc(r, 200, 140, false, p);

        float maxRange = 80f;
        float sweep = Math.min(140f, (speed / maxRange) * 140f);
        p.setColor(GREEN);
        c.drawArc(r, 200, sweep, false, p);

        p.setStyle(Paint.Style.FILL);
        p.setColor(NAVY);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setTextSize(dp(68));
        c.drawText(String.format(Locale.US, "%.1f", speed), w / 2f, h / 2f + dp(22), p);

        p.setFakeBoldText(true);
        p.setTextSize(dp(25));
        p.setColor(Color.rgb(100,116,139));
        c.drawText("km/h", w / 2f, h / 2f + dp(58), p);

        p.setTextSize(dp(24));
        p.setColor(GREEN);
        c.drawText("⌁", w / 2f, dp(70), p);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
