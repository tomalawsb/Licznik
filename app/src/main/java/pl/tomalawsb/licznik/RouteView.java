package pl.tomalawsb.licznik;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class RouteView extends View {
    private static class Pt { double lat, lon; Pt(double a, double b){lat=a;lon=b;} }
    private final List<Pt> points = new ArrayList<>();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RouteView(Context c) { super(c); setBackgroundColor(Color.rgb(240,253,244)); }

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
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(240,253,244)); c.drawRect(0,0,w,h,p);
        p.setColor(Color.rgb(219,234,254));
        for (int i=0;i<5;i++) c.drawLine(0, (i+1)*h/6f, w, (i+1)*h/6f, p);
        p.setColor(Color.rgb(220,252,231));
        c.drawCircle(w*0.18f, h*0.25f, dp(42), p);
        c.drawCircle(w*0.78f, h*0.68f, dp(50), p);

        if (points.size() < 2) {
            p.setColor(Color.rgb(100,116,139)); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(dp(15)); p.setFakeBoldText(true);
            c.drawText("Trasa pojawi się po odebraniu punktów GPS", w/2f, h/2f, p);
            return;
        }
        double minLat=points.get(0).lat,maxLat=points.get(0).lat,minLon=points.get(0).lon,maxLon=points.get(0).lon;
        for(Pt pt: points){minLat=Math.min(minLat,pt.lat);maxLat=Math.max(maxLat,pt.lat);minLon=Math.min(minLon,pt.lon);maxLon=Math.max(maxLon,pt.lon);}        
        double latSpan=Math.max(0.00001,maxLat-minLat), lonSpan=Math.max(0.00001,maxLon-minLon);
        float pad=dp(24);
        Path path = new Path();
        for(int i=0;i<points.size();i++){
            Pt pt=points.get(i);
            float x=(float)(pad+(pt.lon-minLon)/lonSpan*(w-2*pad));
            float y=(float)(pad+(maxLat-pt.lat)/latSpan*(h-2*pad));
            if(i==0) path.moveTo(x,y); else path.lineTo(x,y);
        }
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); p.setStrokeWidth(dp(6)); p.setColor(Color.rgb(37,99,235));
        c.drawPath(path,p);
        drawMarker(c, points.get(0), minLat,maxLat,minLon,maxLon,true);
        drawMarker(c, points.get(points.size()-1), minLat,maxLat,minLon,maxLon,false);
    }

    private void drawMarker(Canvas c, Pt pt, double minLat,double maxLat,double minLon,double maxLon, boolean start){
        double latSpan=Math.max(0.00001,maxLat-minLat), lonSpan=Math.max(0.00001,maxLon-minLon);
        float pad=dp(24), w=getWidth(), h=getHeight();
        float x=(float)(pad+(pt.lon-minLon)/lonSpan*(w-2*pad));
        float y=(float)(pad+(maxLat-pt.lat)/latSpan*(h-2*pad));
        p.setStyle(Paint.Style.FILL); p.setColor(start?Color.rgb(34,197,94):Color.BLACK); c.drawCircle(x,y,dp(12),p);
        p.setColor(Color.WHITE); c.drawCircle(x,y,dp(5),p);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
