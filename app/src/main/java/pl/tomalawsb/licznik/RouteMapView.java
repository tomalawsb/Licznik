package pl.tomalawsb.licznik;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class RouteMapView extends FrameLayout {
    private final MapView mapView;
    private final TextView placeholder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<GeoPoint> routePoints = new ArrayList<>();

    public RouteMapView(Context context) {
        super(context);
        setClipToOutline(true);
        setBackgroundColor(Color.rgb(245, 248, 252));

        Configuration.getInstance().setUserAgentValue(context.getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(context.getCacheDir());
        Configuration.getInstance().setOsmdroidTileCache(context.getCacheDir());

        mapView = new MapView(context);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setTilesScaledToDpi(true);
        mapView.setMultiTouchControls(false);
        mapView.setBuiltInZoomControls(false);
        mapView.setFlingEnabled(false);
        mapView.setMinZoomLevel(3.0);
        mapView.setMaxZoomLevel(20.0);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(new GeoPoint(52.0, 19.0));
        mapView.setOnTouchListener((v, event) -> event.getAction() == MotionEvent.ACTION_MOVE);
        addView(mapView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        placeholder = new TextView(context);
        placeholder.setText("Trasa pojawi się po odbiorze GPS");
        placeholder.setTextColor(Color.rgb(100, 116, 139));
        placeholder.setTextSize(12);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackgroundColor(Color.argb(135, 255, 255, 255));
        addView(placeholder, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void clear() {
        routePoints.clear();
        mapView.getOverlays().clear();
        placeholder.setVisibility(VISIBLE);
        mapView.invalidate();
    }

    public void setPointsFromJson(String json) {
        try {
            routePoints.clear();
            JSONArray arr = new JSONArray(json == null ? "[]" : json);
            for (int i = 0; i < arr.length(); i++) {
                JSONArray p = arr.getJSONArray(i);
                routePoints.add(new GeoPoint(p.getDouble(0), p.getDouble(1)));
            }
            redrawRoute();
        } catch (Exception ignored) {
            clear();
        }
    }

    private void redrawRoute() {
        mapView.getOverlays().clear();
        if (routePoints.size() < 2) {
            placeholder.setVisibility(VISIBLE);
            mapView.invalidate();
            return;
        }
        placeholder.setVisibility(GONE);

        Polyline shadow = new Polyline(mapView);
        shadow.setPoints(routePoints);
        shadow.getOutlinePaint().setColor(Color.WHITE);
        shadow.getOutlinePaint().setStrokeWidth(dp(8));
        shadow.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        shadow.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
        mapView.getOverlays().add(shadow);

        Polyline route = new Polyline(mapView);
        route.setPoints(routePoints);
        route.getOutlinePaint().setColor(Color.rgb(37, 99, 235));
        route.getOutlinePaint().setStrokeWidth(dp(5));
        route.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
        route.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
        mapView.getOverlays().add(route);

        mapView.getOverlays().add(marker(routePoints.get(0), Color.rgb(15, 23, 42)));
        mapView.getOverlays().add(marker(routePoints.get(routePoints.size() - 1), Color.rgb(34, 197, 94)));

        mapView.post(() -> {
            try {
                BoundingBox box = BoundingBox.fromGeoPoints(routePoints).increaseByScale(1.35f);
                mapView.zoomToBoundingBox(box, false, dp(18));
            } catch (Exception e) {
                mapView.getController().setCenter(routePoints.get(routePoints.size() - 1));
                mapView.getController().setZoom(15.0);
            }
            mapView.invalidate();
        });
    }

    private Marker marker(GeoPoint point, int color) {
        Marker m = new Marker(mapView);
        m.setPosition(point);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        m.setIcon(circleMarker(color));
        m.setInfoWindow(null);
        return m;
    }

    private Drawable circleMarker(int color) {
        int size = dp(26);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        float r = size / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        c.drawCircle(r, r, r - dp(1), paint);
        paint.setColor(color);
        c.drawCircle(r, r, r - dp(5), paint);
        paint.setColor(Color.WHITE);
        c.drawCircle(r, r, dp(4), paint);
        return new BitmapDrawable(getResources(), bmp);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mapView.onResume();
    }

    @Override protected void onDetachedFromWindow() {
        mapView.onPause();
        super.onDetachedFromWindow();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
