package pl.tomalawsb.licznik;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RideTrackingService extends Service implements LocationListener {
    public static final String ACTION_START = "pl.tomalawsb.licznik.START";
    public static final String ACTION_STOP = "pl.tomalawsb.licznik.STOP";
    public static final String ACTION_PAUSE = "pl.tomalawsb.licznik.PAUSE";
    public static final String ACTION_RESUME = "pl.tomalawsb.licznik.RESUME";
    public static final String ACTION_SNAPSHOT = "pl.tomalawsb.licznik.SNAPSHOT";
    public static final String ACTION_UPDATE = "pl.tomalawsb.licznik.UPDATE";

    private static final String CHANNEL_ID = "ride_tracking";
    private static final int NOTIFICATION_ID = 77;

    private LocationManager locationManager;
    private boolean running = false;
    private boolean paused = false;
    private String mode = "Rower";
    private long startElapsed = 0;
    private long elapsedBeforePause = 0;
    private double distanceMeters = 0;
    private double currentSpeedKmh = 0;
    private double maxSpeedKmh = 0;
    private float lastAccuracy = -1;
    private Location lastLocation;
    private final JSONArray points = new JSONArray();

    @Override public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SNAPSHOT : intent.getAction();
        if (ACTION_START.equals(action)) startRide(intent.getStringExtra("mode"));
        else if (ACTION_STOP.equals(action)) stopRide();
        else if (ACTION_PAUSE.equals(action)) pauseRide();
        else if (ACTION_RESUME.equals(action)) resumeRide();
        else if (ACTION_SNAPSHOT.equals(action)) sendUpdate();
        return START_STICKY;
    }

    private void startRide(String requestedMode) {
        if (requestedMode != null) mode = requestedMode;
        running = true;
        paused = false;
        startElapsed = SystemClock.elapsedRealtime();
        elapsedBeforePause = 0;
        distanceMeters = 0;
        currentSpeedKmh = 0;
        maxSpeedKmh = 0;
        lastAccuracy = -1;
        lastLocation = null;
        while (points.length() > 0) points.remove(0);
        startAsForeground();
        requestLocation();
        sendUpdate();
    }

    private void pauseRide() {
        if (!running || paused) return;
        elapsedBeforePause += SystemClock.elapsedRealtime() - startElapsed;
        paused = true;
        currentSpeedKmh = 0;
        removeLocationUpdates();
        updateNotification();
        sendUpdate();
    }

    private void resumeRide() {
        if (!running || !paused) return;
        paused = false;
        startElapsed = SystemClock.elapsedRealtime();
        lastLocation = null;
        requestLocation();
        updateNotification();
        sendUpdate();
    }

    private void stopRide() {
        if (running) saveHistory();
        running = false;
        paused = false;
        removeLocationUpdates();
        sendUpdate();
        stopForeground(true);
        stopSelf();
    }

    private void startAsForeground() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        else startForeground(NOTIFICATION_ID, n);
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = paused ? "Pomiar jazdy wstrzymany" : "Trwa pomiar jazdy";
        String text = String.format(Locale.US, "%s • %.2f km • śr. %.3f km/h", mode, distanceMeters/1000.0, getAverageSpeed());
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        b.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pi)
                .setOngoing(running && !paused)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);
        if (Build.VERSION.SDK_INT >= 21) b.setColor(0xFF22C55E);
        return b.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Pomiar jazdy GPS", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Stałe powiadomienie wymagane do pomiaru GPS po zablokowaniu telefonu.");
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private void requestLocation() {
        if (!running || paused) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        } catch (Exception ignored) {}
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 2, this);
        } catch (Exception ignored) {}
    }

    private void removeLocationUpdates() {
        try { locationManager.removeUpdates(this); } catch (Exception ignored) {}
    }

    @Override public void onLocationChanged(Location loc) {
        if (!running || paused || loc == null) return;
        if (loc.hasAccuracy()) lastAccuracy = loc.getAccuracy();
        if (loc.hasAccuracy() && loc.getAccuracy() > 80) { sendUpdate(); return; }

        double speedKmh = loc.hasSpeed() ? loc.getSpeed() * 3.6 : 0;
        if (lastLocation != null) {
            float meters = lastLocation.distanceTo(loc);
            long dt = Math.max(1, loc.getTime() - lastLocation.getTime());
            double computedSpeed = (meters / (dt / 1000.0)) * 3.6;
            if (speedKmh <= 0) speedKmh = computedSpeed;
            if (meters >= 1.5 && computedSpeed < 180) distanceMeters += meters;
        }
        currentSpeedKmh = Math.max(0, speedKmh);
        maxSpeedKmh = Math.max(maxSpeedKmh, currentSpeedKmh);
        lastLocation = loc;
        addPoint(loc);
        updateNotification();
        sendUpdate();
    }

    private void addPoint(Location loc) {
        try {
            JSONArray p = new JSONArray();
            p.put(loc.getLatitude()); p.put(loc.getLongitude());
            points.put(p);
            while (points.length() > 800) points.remove(0);
        } catch (Exception ignored) {}
    }

    private long getElapsedMs() {
        if (!running) return elapsedBeforePause;
        return elapsedBeforePause + (paused ? 0 : (SystemClock.elapsedRealtime() - startElapsed));
    }

    private double getAverageSpeed() {
        long elapsed = getElapsedMs();
        if (elapsed <= 0) return 0;
        return (distanceMeters / 1000.0) / (elapsed / 3600000.0);
    }

    private void sendUpdate() {
        Intent i = new Intent(ACTION_UPDATE);
        i.setPackage(getPackageName());
        i.putExtra("running", running);
        i.putExtra("paused", paused);
        i.putExtra("mode", mode);
        i.putExtra("speed", currentSpeedKmh);
        i.putExtra("avg", getAverageSpeed());
        i.putExtra("distanceKm", distanceMeters / 1000.0);
        i.putExtra("max", maxSpeedKmh);
        i.putExtra("elapsed", getElapsedMs());
        i.putExtra("points", points.length());
        i.putExtra("accuracy", (double) lastAccuracy);
        i.putExtra("pointsJson", points.toString());
        sendBroadcast(i);
    }

    private void saveHistory() {
        try {
            SharedPreferences sp = getSharedPreferences("licznik", MODE_PRIVATE);
            JSONArray history = new JSONArray(sp.getString("history", "[]"));
            JSONObject o = new JSONObject();
            o.put("mode", mode);
            o.put("date", new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("pl", "PL")).format(new Date()));
            o.put("distanceKm", distanceMeters / 1000.0);
            o.put("elapsed", MainActivity.formatDuration(getElapsedMs()));
            o.put("elapsedMs", getElapsedMs());
            o.put("avg", getAverageSpeed());
            o.put("max", maxSpeedKmh);
            o.put("pointsJson", points.toString());
            history.put(o);
            while (history.length() > 100) history.remove(0);
            sp.edit().putString("history", history.toString()).apply();
        } catch (Exception ignored) {}
    }

    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { sendUpdate(); }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public IBinder onBind(Intent intent) { return null; }
}
