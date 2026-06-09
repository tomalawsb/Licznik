package pl.tomalawsb.licznik;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class RouteView extends View {
    private static class Pt { double lat, lon; Pt(double a, double b){lat=a;lon=b;} }
    private final List<Pt> points = new ArrayList<>();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RouteView(Context c) { super(c); }

    public void clear(){ points.clear(); invalidate(); }

    public void setPointsFromJson(String json) {
        try {
            points.clear();
            JSONArray arr = new JSONArray(json == null ? "[]" : json);
            for (int i=0;i<arr.length();i++) {
                JSONArray x = arr.getJSONArray(i);
                points.add(new Pt(x.getDouble(0), x.getDouble(1)));
            }
            invalidate();
        } catch (Exception ignored) {}
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth(), h = getHeight();
        float radius = dp(18);

        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(250, 253, 255));
        c.drawRoundRect(new RectF(0,0,w,h), radius, radius, p);

        // spokojne tło mapy: mniej kolorów, mniej szumu
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(232, 249, 238));
        c.drawCircle(w*0.16f, h*0.24f, dp(42), p);
        c.drawCircle(w*0.76f, h*0.66f, dp(50), p);
        p.setColor(Color.rgb(241, 247, 255));
        c.drawCircle(w*0.48f, h*0.18f, dp(35), p);

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(dp(2));
        p.setColor(Color.rgb(226, 235, 246));
        for (int i=1;i<5;i++) c.drawLine(0, i*h/5f, w, i*h/5f, p);
        p.setColor(Color.rgb(231, 237, 245));
        for (int i=1;i<5;i++) c.drawLine(i*w/5f, 0, i*w/5f, h, p);

        p.setStrokeWidth(dp(3));
        p.setColor(Color.rgb(199, 222, 250));
        Path river = new Path();
        river.moveTo(0, h*0.66f);
        river.cubicTo(w*0.20f,h*0.50f,w*0.42f,h*0.75f,w*0.60f,h*0.56f);
        river.cubicTo(w*0.77f,h*0.38f,w*0.88f,h*0.54f,w,h*0.36f);
        c.drawPath(river, p);

        if (points.size() < 2) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(100,116,139));
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(dp(12));
            p.setFakeBoldText(false);
            c.drawText("Trasa pojawi się po odbiorze GPS", w/2f, h/2f, p);
            return;
        }

        double minLat=points.get(0).lat,maxLat=points.get(0).lat,minLon=points.get(0).lon,maxLon=points.get(0).lon;
        for(Pt pt: points){
            minLat=Math.min(minLat,pt.lat); maxLat=Math.max(maxLat,pt.lat);
            minLon=Math.min(minLon,pt.lon); maxLon=Math.max(maxLon,pt.lon);
        }
        double latSpan=Math.max(0.00001,maxLat-minLat), lonSpan=Math.max(0.00001,maxLon-minLon);
        float pad=dp(20);
        Path path = new Path();
        for(int i=0;i<points.size();i++){
            Pt pt=points.get(i);
            float x=(float)(pad+(pt.lon-minLon)/lonSpan*(w-2*pad));
            float y=(float)(pad+(maxLat-pt.lat)/latSpan*(h-2*pad));
            if(i==0) path.moveTo(x,y); else path.lineTo(x,y);
        }

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(dp(6));
        p.setColor(Color.WHITE);
        c.drawPath(path,p);
        p.setStrokeWidth(dp(4));
        p.setColor(Color.rgb(37,99,235));
        c.drawPath(path,p);

        drawMarker(c, points.get(0), minLat,maxLat,minLon,maxLon,true);
        drawMarker(c, points.get(points.size()-1), minLat,maxLat,minLon,maxLon,false);
    }

    private void drawMarker(Canvas c, Pt pt, double minLat,double maxLat,double minLon,double maxLon, boolean start){
        double latSpan=Math.max(0.00001,maxLat-minLat), lonSpan=Math.max(0.00001,maxLon-minLon);
        float pad=dp(20), w=getWidth(), h=getHeight();
        float x=(float)(pad+(pt.lon-minLon)/lonSpan*(w-2*pad));
        float y=(float)(pad+(maxLat-pt.lat)/latSpan*(h-2*pad));
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.WHITE); c.drawCircle(x,y,dp(12),p);
        p.setColor(start?Color.rgb(15,23,42):Color.rgb(34,197,94)); c.drawCircle(x,y,dp(8),p);
        p.setColor(Color.WHITE); c.drawCircle(x,y,dp(3),p);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
