package pl.tomalawsb.licznik;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class RouteMapView extends FrameLayout {
    public interface OnTargetSelectedListener {
        void onTargetSelected(double lat, double lon);
    }

    private static class PoiMarker {
        final GeoPoint point;
        final String name;
        final String type;
        final String emoji;

        PoiMarker(GeoPoint point, String name, String type, String emoji) {
            this.point = point;
            this.name = name;
            this.type = type;
            this.emoji = emoji;
        }
    }

    private final MapView mapView;
    private final TextView placeholder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<GeoPoint> routePoints = new ArrayList<>();
    private final List<PoiMarker> poiMarkers = new ArrayList<>();
    private String lastPointsJson = "[]";
    private boolean interactive = false;
    private boolean autoFitOnRedraw = true;
    private OnClickListener externalClickListener;
    private GeoPoint targetPoint = null;
    private OnTargetSelectedListener targetSelectedListener = null;
    private MapEventsOverlay targetEventsOverlay = null;
    private GestureDetector targetGestureDetector = null;

    public RouteMapView(Context context) {
        super(context);
        setClipToOutline(true);
        setBackgroundColor(Color.rgb(245, 248, 252));
        setClickable(true);
        setFocusable(false);

        Configuration.getInstance().setUserAgentValue(context.getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(context.getCacheDir());
        Configuration.getInstance().setOsmdroidTileCache(context.getCacheDir());

        mapView = new MapView(context);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setTilesScaledToDpi(true);
        mapView.setMinZoomLevel(3.0);
        mapView.setMaxZoomLevel(20.0);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(new GeoPoint(52.0, 19.0));
        mapView.setOnTouchListener((v, event) -> {
            if (interactive) return false;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                performClick();
            }
            return true;
        });
        addView(mapView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        placeholder = new TextView(context);
        placeholder.setText("Trasa pojawi się po starcie");
        placeholder.setTextColor(Color.rgb(100, 116, 139));
        placeholder.setTextSize(12);
        placeholder.setGravity(Gravity.CENTER);
        placeholder.setBackgroundColor(Color.argb(135, 255, 255, 255));
        placeholder.setClickable(true);
        placeholder.setOnClickListener(v -> performClick());
        addView(placeholder, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setInteractive(false);
    }

    @Override public void setOnClickListener(OnClickListener l) {
        externalClickListener = l;
        super.setOnClickListener(l);
        if (placeholder != null) placeholder.setOnClickListener(v -> {
            if (externalClickListener != null) externalClickListener.onClick(this);
        });
    }

    @Override public boolean performClick() {
        super.performClick();
        if (externalClickListener != null) externalClickListener.onClick(this);
        return true;
    }

    public void setInteractive(boolean enabled) {
        interactive = enabled;
        mapView.setMultiTouchControls(enabled);
        mapView.setBuiltInZoomControls(enabled);
        mapView.setFlingEnabled(enabled);
        mapView.setClickable(enabled);
        if (enabled) {
            ensureTargetGestureDetector();
            ensureTargetEventsOverlay();
            mapView.setOnTouchListener((v, event) -> {
                if (targetGestureDetector != null) targetGestureDetector.onTouchEvent(event);
                return false;
            });
        } else {
            mapView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP) performClick();
                return true;
            });
        }
    }


    public void setOnTargetSelectedListener(OnTargetSelectedListener listener) {
        targetSelectedListener = listener;
        ensureTargetGestureDetector();
        ensureTargetEventsOverlay();
    }

    public void setTargetPoint(double lat, double lon) {
        targetPoint = new GeoPoint(lat, lon);
        redrawRoute();
    }

    public void clearTargetPoint() {
        targetPoint = null;
        redrawRoute();
    }


    private void ensureTargetGestureDetector() {
        if (targetGestureDetector != null) return;
        targetGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override public void onLongPress(MotionEvent e) {
                if (!interactive || targetSelectedListener == null || e == null) return;
                try {
                    android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());
                    GeoPoint p = (GeoPoint) mapView.getProjection().fromPixels(screenPoint.x, screenPoint.y);
                    if (p != null) {
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        targetSelectedListener.onTargetSelected(p.getLatitude(), p.getLongitude());
                    }
                } catch (Exception ignored) {}
            }

            @Override public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

    private void ensureTargetEventsOverlay() {
        if (!interactive || targetSelectedListener == null) return;
        if (targetEventsOverlay == null) {
            targetEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
                @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                    return false;
                }

                @Override public boolean longPressHelper(GeoPoint p) {
                    if (targetSelectedListener != null && p != null) {
                        targetSelectedListener.onTargetSelected(p.getLatitude(), p.getLongitude());
                        return true;
                    }
                    return false;
                }
            });
        }
        if (!mapView.getOverlays().contains(targetEventsOverlay)) {
            mapView.getOverlays().add(targetEventsOverlay);
        }
    }


    public void setPoiMarkersFromJson(String json) {
        poiMarkers.clear();
        try {
            JSONArray arr = new JSONArray(json == null ? "[]" : json);
            for (int i = 0; i < arr.length(); i++) {
                JSONArray p = arr.getJSONArray(i);
                double lat = p.getDouble(0);
                double lon = p.getDouble(1);
                String name = p.length() > 2 ? p.optString(2, "POI") : "POI";
                String type = p.length() > 3 ? p.optString(3, "POI") : "POI";
                String emoji = p.length() > 4 ? p.optString(4, "◆") : "◆";
                poiMarkers.add(new PoiMarker(new GeoPoint(lat, lon), name, type, emoji));
            }
        } catch (Exception ignored) {}
        redrawRoute();
    }

    public void centerOnPoint(double lat, double lon, double zoom) {
        GeoPoint p = new GeoPoint(lat, lon);
        mapView.post(() -> {
            mapView.getController().setCenter(p);
            mapView.getController().animateTo(p);
            mapView.getController().setZoom(zoom);
            mapView.invalidate();
        });
    }

    public void setAutoFitOnRedraw(boolean enabled) {
        autoFitOnRedraw = enabled;
    }



    public void clear() {
        lastPointsJson = "[]";
        routePoints.clear();
        mapView.getOverlays().clear();
        addPoiMarkerOverlays();
        if (targetPoint != null) mapView.getOverlays().add(marker(targetPoint, Color.rgb(6, 182, 212)));
        ensureTargetEventsOverlay();
        placeholder.setVisibility(VISIBLE);
        mapView.invalidate();
    }

    public String getPointsJson() {
        return lastPointsJson == null ? "[]" : lastPointsJson;
    }

    public boolean hasRoute() {
        return !routePoints.isEmpty();
    }

    public void fitRoute() {
        if (routePoints.isEmpty()) return;
        if (routePoints.size() == 1) {
  centerOnLastPoint(interactive ? 17.0 : 15.0);
  return;
        }
        mapView.post(() -> {
  try {
      BoundingBox box = BoundingBox.fromGeoPoints(routePoints).increaseByScale(1.35f);
      mapView.zoomToBoundingBox(box, true, dp(interactive ? 54 : 18));
  } catch (Exception e) {
      centerOnLastPoint(interactive ? 17.0 : 15.0);
  }
  mapView.invalidate();
        });
    }

    public void centerOnLastPoint(double zoom) {
        if (routePoints.isEmpty()) return;
        GeoPoint last = routePoints.get(routePoints.size() - 1);
        mapView.post(() -> {
  mapView.getController().setCenter(last);
  mapView.getController().animateTo(last);
  mapView.getController().setZoom(zoom);
  mapView.invalidate();
        });
    }

    public void setPointsFromJson(String json) {
        lastPointsJson = json == null ? "[]" : json;
        try {
            routePoints.clear();
            JSONArray arr = new JSONArray(lastPointsJson);
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
        if (routePoints.isEmpty()) {
  addPoiMarkerOverlays();
  if (targetPoint != null) mapView.getOverlays().add(marker(targetPoint, Color.rgb(6, 182, 212)));
  ensureTargetEventsOverlay();
  placeholder.setVisibility(poiMarkers.isEmpty() ? VISIBLE : GONE);
  mapView.invalidate();
  return;
        }
        placeholder.setVisibility(GONE);

        if (routePoints.size() >= 2) {
  Polyline shadow = new Polyline(mapView);
  shadow.setPoints(routePoints);
  shadow.getOutlinePaint().setColor(Color.WHITE);
  shadow.getOutlinePaint().setStrokeWidth(dp(interactive ? 10 : 8));
  shadow.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
  shadow.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
  mapView.getOverlays().add(shadow);

  Polyline route = new Polyline(mapView);
  route.setPoints(routePoints);
  route.getOutlinePaint().setColor(Color.rgb(37, 99, 235));
  route.getOutlinePaint().setStrokeWidth(dp(interactive ? 6 : 5));
  route.getOutlinePaint().setStrokeCap(Paint.Cap.ROUND);
  route.getOutlinePaint().setStrokeJoin(Paint.Join.ROUND);
  mapView.getOverlays().add(route);

  mapView.getOverlays().add(marker(routePoints.get(0), Color.rgb(15, 23, 42)));
        }
        mapView.getOverlays().add(marker(routePoints.get(routePoints.size() - 1), Color.rgb(34, 197, 94)));
        addPoiMarkerOverlays();
        if (targetPoint != null) mapView.getOverlays().add(marker(targetPoint, Color.rgb(6, 182, 212)));
        ensureTargetEventsOverlay();
        if (autoFitOnRedraw) fitRoute();
        else mapView.invalidate();
    }


    private void addPoiMarkerOverlays() {
        for (PoiMarker p : poiMarkers) {
            Marker m = marker(p.point, Color.rgb(245, 158, 11));
            m.setIcon(poiMarkerIcon(p.emoji, p.type));
            m.setTitle((p.emoji == null ? "◆" : p.emoji) + " " + p.name);
            m.setSnippet(p.type);
            mapView.getOverlays().add(m);
        }
    }

    private Drawable poiMarkerIcon(String emoji, String type) {
        int size = dp(interactive ? 42 : 34);
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        float r = size / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        c.drawCircle(r, r, r - dp(1), paint);
        paint.setColor(colorForPoiType(type));
        c.drawCircle(r, r, r - dp(4), paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(interactive ? 20 : 16));
        Paint.FontMetrics fm = paint.getFontMetrics();
        String e = (emoji == null || emoji.length() == 0) ? "◆" : emoji;
        c.drawText(e, r, r - (fm.ascent + fm.descent) / 2f, paint);
        return new BitmapDrawable(getResources(), bmp);
    }

    private int colorForPoiType(String type) {
        if (type == null) return Color.rgb(245, 158, 11);
        if (type.contains("Stacja")) return Color.rgb(234, 88, 12);
        if (type.contains("Schron") || type.contains("Szczyt") || type.contains("widok")) return Color.rgb(22, 163, 74);
        if (type.contains("rower") || type.contains("Rower")) return Color.rgb(37, 99, 235);
        if (type.contains("Woda")) return Color.rgb(14, 165, 233);
        if (type.contains("Apteka") || type.contains("Szpital")) return Color.rgb(220, 38, 38);
        return Color.rgb(245, 158, 11);
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
        int size = dp(interactive ? 30 : 26);
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
