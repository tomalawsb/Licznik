package pl.tomalawsb.licznik;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RideTrackingService extends Service {
    public static final String ACTION_START = "pl.tomalawsb.licznik.START";
    public static final String ACTION_STOP = "pl.tomalawsb.licznik.STOP";
    public static final String ACTION_PAUSE = "pl.tomalawsb.licznik.PAUSE";
    public static final String ACTION_RESUME = "pl.tomalawsb.licznik.RESUME";
    public static final String ACTION_SNAPSHOT = "pl.tomalawsb.licznik.SNAPSHOT";
    public static final String ACTION_RESET = "pl.tomalawsb.licznik.RESET";
    public static final String ACTION_UPDATE = "pl.tomalawsb.licznik.UPDATE";
    public static final String EXTRA_STOPPED = "stopped";
    public static final String EXTRA_HISTORY_SAVED = "historySaved";
    public static final String EXTRA_MESSAGE = "message";

    private static final String CHANNEL_ID = "ride_tracking";
    private static final int NOTIFICATION_ID = 77;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    private boolean running = false;
    private boolean paused = false;
    private String mode = "Rower";

    private long startElapsed = 0;
    private long elapsedBeforePause = 0;

    private double distanceMeters = 0;
    private double currentSpeedKmh = 0;
    private double targetSpeedKmh = 0;
    private double maxSpeedKmh = 0;
    private float lastAccuracy = -1;

    private Location lastRawLocation;
    private int movingConfidence = 0;
    private int stationaryConfidence = 0;
    private long lastNotificationUpdate = 0;
    private long lastAverageCalculation = 0;
    private double displayedAverageSpeedKmh = 0;
    private boolean pointsChanged = true;
    private String cachedPointsJson = "[]";

    private final Handler tickHandler = new Handler(Looper.getMainLooper());
    private final JSONArray points = new JSONArray();

    private final Runnable tickRunnable = new Runnable() {
        @Override public void run() {
            if (running && !paused) {
                double factor = targetSpeedKmh > currentSpeedKmh ? 0.22 : 0.16;
                currentSpeedKmh += (targetSpeedKmh - currentSpeedKmh) * factor;
                if (targetSpeedKmh < 0.25 && currentSpeedKmh < 0.35) currentSpeedKmh = 0;

                long now = SystemClock.elapsedRealtime();
                if (now - lastNotificationUpdate >= 1000) {
                    updateNotification();
                    lastNotificationUpdate = now;
                }
                sendUpdate();
                tickHandler.postDelayed(this, 250);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        createChannel();
        locationCallback = new LocationCallback() {
            @Override public void onLocationResult(LocationResult result) {
                if (result == null) return;
                for (Location loc : result.getLocations()) handleLocation(loc);
            }
        };
    }

    @Override public void onDestroy() {
        tickHandler.removeCallbacksAndMessages(null);
        removeLocationUpdates();
        super.onDestroy();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_SNAPSHOT : intent.getAction();
        if (ACTION_START.equals(action)) startRide(intent.getStringExtra("mode"));
        else if (ACTION_STOP.equals(action)) stopRide();
        else if (ACTION_PAUSE.equals(action)) pauseRide();
        else if (ACTION_RESUME.equals(action)) resumeRide();
        else if (ACTION_RESET.equals(action)) resetRide();
        else if (ACTION_SNAPSHOT.equals(action)) {
            pointsChanged = true;
            sendUpdate();
            if (!running) stopSelf();
        }
        return START_STICKY;
    }

    private void startRide(String requestedMode) {
        if (requestedMode != null) mode = requestedMode;
        running = true;
        paused = false;
        clearRideData();
        startAsForeground();
        requestLocation();
        tickHandler.removeCallbacks(tickRunnable);
        tickHandler.post(tickRunnable);
        sendUpdate();
    }

    private void pauseRide() {
        if (!running || paused) return;
        elapsedBeforePause += SystemClock.elapsedRealtime() - startElapsed;
        paused = true;
        targetSpeedKmh = 0;
        currentSpeedKmh = 0;
        movingConfidence = 0;
        stationaryConfidence = 0;
        tickHandler.removeCallbacks(tickRunnable);
        removeLocationUpdates();
        updateNotification();
        sendUpdate();
    }

    private void resumeRide() {
        if (!running || !paused) return;
        paused = false;
        startElapsed = SystemClock.elapsedRealtime();
        lastRawLocation = null;
        movingConfidence = 0;
        stationaryConfidence = 0;
        requestLocation();
        tickHandler.removeCallbacks(tickRunnable);
        tickHandler.post(tickRunnable);
        updateNotification();
        sendUpdate();
    }

    private void stopRide() {
        boolean shouldSave = running && (distanceMeters > 1.0 || getElapsedMs() > 5000 || points.length() > 1);
        boolean saved = false;
        if (shouldSave) saved = saveHistory();
        running = false;
        paused = false;
        tickHandler.removeCallbacks(tickRunnable);
        removeLocationUpdates();
        clearRideData();
        sendUpdate(true, saved, shouldSave ? (saved ? "Jazda zakończona i zapisana." : "Jazda zakończona, ale nie udało się zapisać historii.") : "Pomiar zakończony. Brak danych do zapisania.");
        stopForeground(true);
        stopSelf();
    }

    private void resetRide() {
        boolean wasRunning = running;
        boolean wasPaused = paused;
        clearRideData();
        running = wasRunning;
        paused = wasPaused;

        if (!running) {
            sendUpdate(false, false, "Licznik wyzerowany.");
            stopSelf();
            return;
        }

        startElapsed = SystemClock.elapsedRealtime();
        if (!paused) {
            requestLocation();
            tickHandler.removeCallbacks(tickRunnable);
            tickHandler.post(tickRunnable);
        }
        updateNotification();
        sendUpdate(false, false, "Pomiar wyzerowany.");
    }

    private void clearRideData() {
        startElapsed = SystemClock.elapsedRealtime();
        elapsedBeforePause = 0;
        distanceMeters = 0;
        currentSpeedKmh = 0;
        targetSpeedKmh = 0;
        maxSpeedKmh = 0;
        lastAccuracy = -1;
        lastRawLocation = null;
        movingConfidence = 0;
        stationaryConfidence = 0;
        lastNotificationUpdate = 0;
        lastAverageCalculation = 0;
        displayedAverageSpeedKmh = 0;
        while (points.length() > 0) points.remove(0);
        cachedPointsJson = "[]";
        pointsChanged = true;
    }

    private void startAsForeground() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        else startForeground(NOTIFICATION_ID, n);
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = paused ? "Pomiar jazdy wstrzymany" : "Trwa pomiar jazdy";
        String text = String.format(Locale.US, "%s • %.2f km • śr. %.3f km/h", mode, distanceMeters / 1000.0, getAverageSpeed());
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pi)
                .setOngoing(running && !paused)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setColor(0xFF22C55E)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Pomiar jazdy GPS", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Stałe powiadomienie wymagane do pomiaru GPS po zablokowaniu telefonu.");
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void requestLocation() {
        if (!running || paused) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMinUpdateIntervalMillis(500)
                    .setMaxUpdateDelayMillis(0)
                    .setMinUpdateDistanceMeters(0)
                    .setWaitForAccurateLocation(false)
                    .build();
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (Exception ignored) {}
    }

    private void removeLocationUpdates() {
        try {
            if (fusedClient != null && locationCallback != null) fusedClient.removeLocationUpdates(locationCallback);
        } catch (Exception ignored) {}
    }

    private void handleLocation(Location loc) {
        if (!running || paused || loc == null) return;
        if (loc.hasAccuracy()) lastAccuracy = loc.getAccuracy();

        double maxAllowed = "Samochód".equals(mode) ? 260.0 : 90.0;
        double startSpeed = "Samochód".equals(mode) ? 4.0 : 2.2;
        double settleSpeed = "Samochód".equals(mode) ? 2.0 : 1.2;
        double maxAccuracy = "Samochód".equals(mode) ? 85.0 : 65.0;
        double accuracy = loc.hasAccuracy() ? loc.getAccuracy() : 25.0;

        if (accuracy > maxAccuracy) {
            // Bardzo słaby fix: nie licz dystansu, ale nie kasuj punktu odniesienia.
            targetSpeedKmh = 0;
            movingConfidence = 0;
            sendUpdate();
            return;
        }

        if (lastRawLocation == null) {
            lastRawLocation = loc;
            addPoint(loc);
            targetSpeedKmh = 0;
            sendUpdate();
            return;
        }

        long dtMs = getDeltaMs(lastRawLocation, loc);
        if (dtMs < 450) {
            // Zbyt gęsty odczyt: nie doliczamy dystansu, ale przesuwamy punkt odniesienia,
            // żeby kolejny interwał nie zawyżał prędkości przez stary punkt bazowy.
            lastRawLocation = loc;
            sendUpdate();
            return;
        }

        float meters = lastRawLocation.distanceTo(loc);
        double seconds = dtMs / 1000.0;
        double computedSpeed = seconds > 0 ? (meters / seconds) * 3.6 : 0;

        double gpsSpeed = -1;
        double gpsSpeedAcc = -1;
        if (loc.hasSpeed()) gpsSpeed = loc.getSpeed() * 3.6;
        if (Build.VERSION.SDK_INT >= 26 && loc.hasSpeedAccuracy()) gpsSpeedAcc = loc.getSpeedAccuracyMetersPerSecond() * 3.6;

        boolean speedAccuracyAcceptable = gpsSpeed >= 0 && (gpsSpeedAcc < 0 || gpsSpeedAcc <= Math.max(7.0, gpsSpeed * 0.75));
        boolean gpsSpeedEvidence = speedAccuracyAcceptable && gpsSpeed >= startSpeed && gpsSpeed <= maxAllowed;

        double movementGateMeters = Math.max(3.0, accuracy * 0.55);
        boolean displacementEvidence = meters >= movementGateMeters && computedSpeed >= startSpeed && computedSpeed <= maxAllowed;
        boolean obviousSpike = meters > 220 || computedSpeed > maxAllowed * 1.35 || (gpsSpeed > maxAllowed * 1.35);

        if (obviousSpike) {
            // Nowy punkt odniesienia, bez doliczania fałszywego skoku.
            lastRawLocation = loc;
            targetSpeedKmh = 0;
            movingConfidence = 0;
            stationaryConfidence++;
            sendUpdate();
            return;
        }

        boolean stationaryByGpsSpeed = speedAccuracyAcceptable && gpsSpeed < settleSpeed && meters < Math.max(accuracy * 1.4, 8.0);
        if (!gpsSpeedEvidence && !displacementEvidence || stationaryByGpsSpeed) {
            stationaryConfidence++;
            movingConfidence = 0;
            targetSpeedKmh = 0;
            if (stationaryConfidence >= 2) currentSpeedKmh *= 0.55;
            lastRawLocation = loc;
            sendUpdate();
            return;
        }

        stationaryConfidence = 0;
        movingConfidence++;

        double acceptedSpeed;
        if (gpsSpeedEvidence) acceptedSpeed = gpsSpeed;
        else acceptedSpeed = computedSpeed;

        // Do pokazania prędkości wystarczy wiarygodny punkt. Do doliczania dystansu wymagamy minimum stabilności.
        if (movingConfidence >= 2) {
            distanceMeters += meters;
            addPoint(loc);
            if (acceptedSpeed > maxSpeedKmh) maxSpeedKmh = acceptedSpeed;
        }

        targetSpeedKmh = clamp(acceptedSpeed, 0, maxAllowed);
        lastRawLocation = loc;
        sendUpdate();
    }

    private long getDeltaMs(Location a, Location b) {
        if (Build.VERSION.SDK_INT >= 17) {
            long d = (b.getElapsedRealtimeNanos() - a.getElapsedRealtimeNanos()) / 1000000L;
            if (d > 0) return d;
        }
        long d = b.getTime() - a.getTime();
        return d > 0 ? d : 1000;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private void addPoint(Location loc) {
        try {
            JSONArray p = new JSONArray();
            p.put(loc.getLatitude());
            p.put(loc.getLongitude());
            points.put(p);
            pointsChanged = true;
        } catch (Exception ignored) {}
    }

    private long getElapsedMs() {
        if (!running) return elapsedBeforePause;
        return elapsedBeforePause + (paused ? 0 : (SystemClock.elapsedRealtime() - startElapsed));
    }

    private double calculateAverageSpeed() {
        long elapsed = getElapsedMs();
        if (elapsed <= 0) return 0;
        return (distanceMeters / 1000.0) / (elapsed / 3600000.0);
    }

    private double getAverageSpeed() {
        long now = SystemClock.elapsedRealtime();
        if (lastAverageCalculation == 0 || now - lastAverageCalculation >= 1000) {
            displayedAverageSpeedKmh = calculateAverageSpeed();
            lastAverageCalculation = now;
        }
        return displayedAverageSpeedKmh;
    }

    private void sendUpdate() {
        sendUpdate(false, false, null);
    }

    private void sendUpdate(boolean stopped, boolean historySaved, String message) {
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
        if (pointsChanged) {
            cachedPointsJson = points.toString();
            pointsChanged = false;
            i.putExtra("pointsJson", cachedPointsJson);
        }
        i.putExtra(EXTRA_STOPPED, stopped);
        i.putExtra(EXTRA_HISTORY_SAVED, historySaved);
        if (message != null) i.putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(i);
    }

    private boolean saveHistory() {
        try {
            SharedPreferences sp = getSharedPreferences("licznik", MODE_PRIVATE);
            JSONArray history = new JSONArray(sp.getString("history", "[]"));
            JSONObject o = new JSONObject();
            o.put("mode", mode);
            o.put("date", new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("pl", "PL")).format(new Date()));
            o.put("distanceKm", distanceMeters / 1000.0);
            o.put("elapsed", MainActivity.formatDuration(getElapsedMs()));
            o.put("elapsedMs", getElapsedMs());
            o.put("avg", calculateAverageSpeed());
            o.put("max", maxSpeedKmh);
            o.put("pointsJson", points.toString());
            history.put(o);
            while (history.length() > 100) history.remove(0);
            sp.edit().putString("history", history.toString()).apply();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
