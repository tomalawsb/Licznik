package pl.tomalawsb.licznik;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends android.app.Activity {
    public static final String VERSION_NAME = "1.7 - 0906260920";
    public static final String CURRENT_RELEASE_TAG = "v1.7-0906260920";
    public static final int CURRENT_VERSION_CODE = 7;
    private static final String GITHUB_RELEASES = "https://github.com/tomalawsb/Licznik/releases/latest";
    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/tomalawsb/Licznik/releases/latest";
    private static final int REQ_PERMISSIONS = 1001;

    private final int GREEN = Color.rgb(34, 197, 94);
    private final int GREEN_DARK = Color.rgb(22, 163, 74);
    private final int BLUE = Color.rgb(37, 99, 235);
    private final int ORANGE = Color.rgb(249, 115, 22);
    private final int PURPLE = Color.rgb(124, 58, 237);
    private final int RED = Color.rgb(239, 68, 68);
    private final int NAVY = Color.rgb(8, 24, 64);
    private final int MUTED = Color.rgb(100, 116, 139);
    private final int BORDER = Color.rgb(226, 232, 240);
    private final int BG = Color.rgb(247, 250, 255);

    private LinearLayout contentBox;
    private LinearLayout controlsRow;
    private LinearLayout primaryActionButton;
    private TextView primaryActionIcon, primaryActionLabel;
    private TextView clockPill, statusText, modeRower, modeSamochod;
    private SpeedGaugeView gaugeView;
    private RouteView routeView;
    private TextView avgText, distanceText, timeText, maxText, accuracyText;
    private TextView navRide, navHistory, navStats;
    private String selectedMode = "Rower";
    private boolean running = false;
    private boolean paused = false;
    private int currentTab = 0;
    private long lastElapsedMs = 0;
    private long elapsedBaseMs = 0;
    private long elapsedSyncRealtime = 0;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable clockUiTicker = new Runnable() {
        @Override public void run() {
            updateHeaderClock();
            long displayElapsed = lastElapsedMs;
            if (running && !paused) {
                displayElapsed = elapsedBaseMs + Math.max(0, SystemClock.elapsedRealtime() - elapsedSyncRealtime);
            }
            if (timeText != null) timeText.setText(formatDuration(displayElapsed) + "\njazdy");
            uiHandler.postDelayed(this, 250);
        }
    };

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!RideTrackingService.ACTION_UPDATE.equals(intent.getAction())) return;
            running = intent.getBooleanExtra("running", false);
            paused = intent.getBooleanExtra("paused", false);
            String incomingMode = intent.getStringExtra("mode");
            if (incomingMode != null) selectedMode = incomingMode;
            double speed = intent.getDoubleExtra("speed", 0);
            double avg = intent.getDoubleExtra("avg", 0);
            double distanceKm = intent.getDoubleExtra("distanceKm", 0);
            double max = intent.getDoubleExtra("max", 0);
            long elapsed = intent.getLongExtra("elapsed", 0);
            lastElapsedMs = elapsed;
            elapsedBaseMs = elapsed;
            elapsedSyncRealtime = SystemClock.elapsedRealtime();
            int points = intent.getIntExtra("points", 0);
            double accuracy = intent.getDoubleExtra("accuracy", -1);
            String pointsJson = intent.getStringExtra("pointsJson");

            if (gaugeView != null) gaugeView.setSpeed((float) speed);
            if (avgText != null) avgText.setText(String.format(Locale.US, "%.3f km/h", avg));
            if (distanceText != null) distanceText.setText(String.format(Locale.US, "%.2f\nkm", distanceKm));
            if (timeText != null) timeText.setText(formatDuration(elapsed) + "\njazdy");
            if (maxText != null) maxText.setText(String.format(Locale.US, "%.1f\nkm/h", max));
            if (accuracyText != null) accuracyText.setText(accuracy > 0 ? "Dokładność: " + Math.round(accuracy) + " m" : "Dokładność: --");
            if (pointsJson != null && routeView != null) routeView.setPointsFromJson(pointsJson);
            updateModeButtons();
            updateStatus();
            updateControlStates();
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        selectedMode = prefs().getString("last_mode", "Rower");
        buildUi();
        enterImmersiveMode();
        registerUpdates();
        requestPermissionsIfNeeded(false);
        renderRide();
        uiHandler.post(clockUiTicker);
        if (prefs().getBoolean("auto_update_check", false)) checkForUpdates(false);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) enterImmersiveMode();
    }

    private void enterImmersiveMode() {
        Window w = getWindow();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = w.getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            w.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @Override protected void onDestroy() {
        try { unregisterReceiver(updateReceiver); } catch (Exception ignored) {}
        uiHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private SharedPreferences prefs() { return getSharedPreferences("licznik", MODE_PRIVATE); }

    private void registerUpdates() {
        IntentFilter f = new IntentFilter(RideTrackingService.ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(updateReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(updateReceiver, f);
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(10), dp(18), dp(6));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(78)));

        TextView bikeIcon = circleText("🚲", 46, Color.WHITE, GREEN_DARK, 21);
        bikeIcon.setElevation(dp(4));
        header.addView(bikeIcon);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -1, 1));

        TextView title = text("Licznik jazdy", 21, NAVY, true);
        title.setSingleLine(true);
        titleBox.addView(title, new LinearLayout.LayoutParams(-1, 0, 1));
        statusText = text("GPS gotowy", 13, MUTED, false);
        statusText.setSingleLine(true);
        titleBox.addView(statusText, new LinearLayout.LayoutParams(-1, 0, 1));

        clockPill = pill(currentClockText(), BLUE, Color.WHITE, 17, true);
        clockPill.setElevation(dp(4));
        header.addView(clockPill, new LinearLayout.LayoutParams(dp(82), dp(50)));

        TextView settings = circleText("⚙", 50, Color.WHITE, NAVY, 23);
        settings.setElevation(dp(4));
        settings.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(dp(54), dp(54));
        setLp.leftMargin = dp(10);
        header.addView(settings, setLp);

        contentBox = new LinearLayout(this);
        contentBox.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(contentBox);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        controlsRow = new LinearLayout(this);
        controlsRow.setGravity(Gravity.CENTER);
        controlsRow.setPadding(dp(14), dp(8), dp(14), dp(8));
        controlsRow.setBackgroundColor(Color.WHITE);
        root.addView(controlsRow, new LinearLayout.LayoutParams(-1, dp(82)));
        buildControls();

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(4), dp(10), dp(6));
        nav.setBackgroundColor(Color.WHITE);
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(66)));
        navRide = navItem("🚲\nJazda", true);
        navHistory = navItem("☰\nHistoria", false);
        navStats = navItem("▥\nStatystyki", false);
        nav.addView(navRide, new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navHistory, new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navStats, new LinearLayout.LayoutParams(0, -1, 1));
        navRide.setOnClickListener(v -> renderRide());
        navHistory.setOnClickListener(v -> renderHistory());
        navStats.setOnClickListener(v -> renderStats());
    }

    private void buildControls() {
        controlsRow.removeAllViews();
        primaryActionButton = actionBtn("▶", "Start", GREEN, v -> primaryRideAction());
        primaryActionIcon = (TextView) primaryActionButton.getChildAt(0);
        primaryActionLabel = (TextView) primaryActionButton.getChildAt(1);
        controlsRow.addView(primaryActionButton, controlLp(1.55f));
        controlsRow.addView(actionBtn("■", "Stop", RED, v -> stopRide()), controlLp(1.0f));
        controlsRow.addView(actionBtn("↻", "Reset", Color.rgb(100, 116, 139), v -> resetRide()), controlLp(1.0f));
        updateControlStates();
    }

    private LinearLayout.LayoutParams controlLp(float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, weight);
        lp.setMargins(dp(5), 0, dp(5), 0);
        return lp;
    }

    private LinearLayout actionBtn(String icon, String label, int color, View.OnClickListener listener) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(4), dp(6), dp(4), dp(5));
        b.setBackground(gradient(color, darker(color), 20));
        b.setElevation(dp(6));
        b.setOnClickListener(listener);
        TextView i = text(icon, 20, Color.WHITE, true);
        i.setGravity(Gravity.CENTER);
        i.setIncludeFontPadding(false);
        b.addView(i, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView l = text(label, 13, Color.WHITE, true);
        l.setGravity(Gravity.CENTER);
        l.setIncludeFontPadding(false);
        b.addView(l, new LinearLayout.LayoutParams(-1, 0, 1));
        return b;
    }
    private void renderRide() {
        currentTab = 0;
        updateNav();
        controlsRow.setVisibility(View.VISIBLE);
        contentBox.removeAllViews();
        contentBox.setPadding(dp(16), 0, dp(16), dp(12));

        LinearLayout modeCard = card();
        modeCard.setPadding(dp(16), dp(10), dp(16), dp(12));
        TextView label = text("Tryb jazdy", 16, MUTED, true);
        modeCard.addView(label, new LinearLayout.LayoutParams(-1, dp(24)));
        LinearLayout seg = new LinearLayout(this);
        seg.setGravity(Gravity.CENTER);
        seg.setPadding(dp(5), dp(5), dp(5), dp(5));
        seg.setBackground(round(Color.rgb(248,250,252), 22, Color.rgb(218,226,236), 1));
        LinearLayout.LayoutParams segLp = new LinearLayout.LayoutParams(-1, dp(58));
        segLp.topMargin = dp(6);
        modeCard.addView(seg, segLp);
        modeRower = modeButton("🚲  Rower");
        modeSamochod = modeButton("🚗  Samochód");
        seg.addView(modeRower, new LinearLayout.LayoutParams(0, -1, 1));
        seg.addView(modeSamochod, new LinearLayout.LayoutParams(0, -1, 1));
        modeRower.setOnClickListener(v -> setMode("Rower"));
        modeSamochod.setOnClickListener(v -> setMode("Samochód"));
        contentBox.addView(modeCard, new LinearLayout.LayoutParams(-1, dp(108)));

        LinearLayout speedCard = card();
        speedCard.setPadding(dp(12), dp(4), dp(12), dp(12));
        gaugeView = new SpeedGaugeView(this);
        speedCard.addView(gaugeView, new LinearLayout.LayoutParams(-1, dp(196)));

        LinearLayout avg = new LinearLayout(this);
        avg.setOrientation(LinearLayout.HORIZONTAL);
        avg.setGravity(Gravity.CENTER_VERTICAL);
        avg.setPadding(dp(14), 0, dp(14), 0);
        avg.setBackground(round(Color.rgb(240,253,244), 19, Color.rgb(187,247,208), 1));
        TextView avgIcon = circleText("↗", 52, GREEN, Color.WHITE, 25);
        avg.addView(avgIcon);
        LinearLayout avgTexts = new LinearLayout(this);
        avgTexts.setOrientation(LinearLayout.VERTICAL);
        avgTexts.setPadding(dp(14),0,0,0);
        avgTexts.addView(text("Średnia prędkość", 16, NAVY, true), new LinearLayout.LayoutParams(-1, 0, 1));
        avgText = text("0.000 km/h", 30, GREEN_DARK, true);
        avgTexts.addView(avgText, new LinearLayout.LayoutParams(-1, 0, 1));
        avg.addView(avgTexts, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams avgLp = new LinearLayout.LayoutParams(-1, dp(78));
        speedCard.addView(avg, avgLp);
        LinearLayout.LayoutParams speedLp = new LinearLayout.LayoutParams(-1, dp(304));
        speedLp.topMargin = dp(12);
        contentBox.addView(speedCard, speedLp);

        LinearLayout stats = new LinearLayout(this);
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(-1, dp(88));
        statsLp.topMargin = dp(12);
        contentBox.addView(stats, statsLp);
        distanceText = addStat(stats, "⌁", "Dystans", "0.00\nkm", BLUE);
        timeText = addStat(stats, "◴", "Czas", "00:00:00\njazdy", ORANGE);
        maxText = addStat(stats, "▲", "Maks.", "0.0\nkm/h", PURPLE);


        LinearLayout mapCard = card();
        mapCard.setPadding(dp(14), dp(9), dp(14), dp(14));
        LinearLayout mapHeader = new LinearLayout(this);
        mapHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView mapTitle = text("Trasa GPS", 16, MUTED, true);
        mapHeader.addView(mapTitle, new LinearLayout.LayoutParams(0, -1, 1));
        accuracyText = text("Dokładność: --", 14, MUTED, false);
        mapHeader.addView(accuracyText);
        mapCard.addView(mapHeader, new LinearLayout.LayoutParams(-1, dp(32)));
        routeView = new RouteView(this);
        mapCard.addView(routeView, new LinearLayout.LayoutParams(-1, dp(132)));
        LinearLayout.LayoutParams mapLp = new LinearLayout.LayoutParams(-1, dp(190));
        mapLp.topMargin = dp(12);
        contentBox.addView(mapCard, mapLp);
        updateModeButtons();
        updateStatus();
        requestSnapshot();
    }

    private TextView addStat(LinearLayout parent, String icon, String label, String value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(4), dp(5), dp(4), dp(5));
        box.setBackground(round(Color.WHITE, 21, BORDER, 1));
        box.setElevation(dp(3));
        TextView i = text(icon, 15, color, true); i.setGravity(Gravity.CENTER); box.addView(i, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView l = text(label, 11, Color.rgb(51,65,85), true); l.setGravity(Gravity.CENTER); box.addView(l, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView v = text(value, 14, color, true); v.setGravity(Gravity.CENTER); box.addView(v, new LinearLayout.LayoutParams(-1, 0, 2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1); lp.setMargins(dp(4),0,dp(4),0);
        parent.addView(box, lp);
        return v;
    }

    private TextView modeButton(String label) {
        TextView t = text(label, 17, NAVY, true);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void setMode(String mode) {
        if (running) { Toast.makeText(this, "Tryb można zmienić przed startem jazdy.", Toast.LENGTH_SHORT).show(); return; }
        selectedMode = mode;
        prefs().edit().putString("last_mode", mode).apply();
        updateModeButtons();
    }

    private void updateModeButtons() {
        if (modeRower == null || modeSamochod == null) return;
        boolean bike = "Rower".equals(selectedMode);
        modeRower.setBackground(round(bike ? GREEN : Color.TRANSPARENT, 18, Color.TRANSPARENT, 0));
        modeSamochod.setBackground(round(!bike ? BLUE : Color.TRANSPARENT, 18, Color.TRANSPARENT, 0));
        modeRower.setTextColor(bike ? Color.WHITE : Color.rgb(51,65,85));
        modeSamochod.setTextColor(!bike ? Color.WHITE : Color.rgb(51,65,85));
    }

    private void startRide() {
        if (!requestPermissionsIfNeeded(true)) return;
        Intent i = new Intent(this, RideTrackingService.class);
        i.setAction(RideTrackingService.ACTION_START);
        i.putExtra("mode", selectedMode);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Pomiar uruchomiony. Powiadomienie utrzyma GPS po blokadzie ekranu.", Toast.LENGTH_LONG).show();
    }

    private void primaryRideAction() {
        if (!running) startRide();
        else togglePause();
    }

    private void togglePause() {
        Intent i = new Intent(this, RideTrackingService.class);
        i.setAction(paused ? RideTrackingService.ACTION_RESUME : RideTrackingService.ACTION_PAUSE);
        startService(i);
    }

    private void stopRide() {
        Intent i = new Intent(this, RideTrackingService.class);
        i.setAction(RideTrackingService.ACTION_STOP);
        startService(i);
        Toast.makeText(this, "Jazda zakończona i zapisana w historii.", Toast.LENGTH_SHORT).show();
    }

    private void resetRide() {
        Intent i = new Intent(this, RideTrackingService.class);
        i.setAction(RideTrackingService.ACTION_RESET);
        try { startService(i); } catch (Exception ignored) {}
        resetLocalViewOnly();
        Toast.makeText(this, running ? "Pomiar wyzerowany, jazda trwa dalej." : "Licznik wyzerowany.", Toast.LENGTH_SHORT).show();
    }

    private void resetLocalViewOnly() {
        if (gaugeView != null) gaugeView.setSpeed(0);
        if (avgText != null) avgText.setText("0.000 km/h");
        if (distanceText != null) distanceText.setText("0.00\nkm");
        if (timeText != null) timeText.setText("00:00:00\njazdy");
        if (maxText != null) maxText.setText("0.0\nkm/h");
        if (routeView != null) routeView.clear();
        lastElapsedMs = 0;
        elapsedBaseMs = 0;
        elapsedSyncRealtime = SystemClock.elapsedRealtime();
    }

    private boolean requestPermissionsIfNeeded(boolean showInfo) {
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!missing.isEmpty()) {
            if (showInfo) Toast.makeText(this, "Aplikacja potrzebuje lokalizacji i powiadomienia, żeby działać po zablokowaniu ekranu.", Toast.LENGTH_LONG).show();
            requestPermissions(missing.toArray(new String[0]), REQ_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void requestSnapshot() {
        Intent i = new Intent(this, RideTrackingService.class);
        i.setAction(RideTrackingService.ACTION_SNAPSHOT);
        try { startService(i); } catch (Exception ignored) {}
    }

    private void updateStatus() {
        if (statusText == null) return;
        if (running && !paused) statusText.setText("GPS aktywny");
        else if (running) statusText.setText("Pomiar wstrzymany");
        else statusText.setText("GPS gotowy");
    }

    private void renderHistory() {
        currentTab = 1; updateNav(); controlsRow.setVisibility(View.GONE);
        contentBox.removeAllViews(); contentBox.setPadding(dp(16), 0, dp(16), dp(18));
        contentBox.addView(text("Historia jazdy", 24, NAVY, true), new LinearLayout.LayoutParams(-1, dp(52)));
        try {
            JSONArray arr = new JSONArray(prefs().getString("history", "[]"));
            if (arr.length() == 0) {
                TextView empty = text("Brak zapisanych jazd. Uruchom pomiar i kliknij Stop.", 17, MUTED, false);
                empty.setGravity(Gravity.CENTER);
                contentBox.addView(empty, new LinearLayout.LayoutParams(-1, dp(160)));
                return;
            }
            for (int i = arr.length() - 1; i >= 0; i--) addHistoryCard(arr.getJSONObject(i), i == arr.length() - 1);
        } catch (Exception e) {
            contentBox.addView(text("Nie udało się odczytać historii.", 16, RED, true));
        }
    }

    private void addHistoryCard(JSONObject o, boolean expanded) throws Exception {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        String mode = o.optString("mode", "Rower");
        int accent = mode.equals("Samochód") ? BLUE : GREEN;
        TextView icon = circleText(mode.equals("Samochód") ? "🚗" : "🚲", 48, accent, Color.WHITE, 22);
        row.addView(icon);
        LinearLayout names = new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setPadding(dp(12),0,0,0);
        names.addView(text(mode, 17, accent, true), new LinearLayout.LayoutParams(-1, 0, 1));
        names.addView(text(o.optString("date", ""), 13, MUTED, false), new LinearLayout.LayoutParams(-1, 0, 1));
        row.addView(names, new LinearLayout.LayoutParams(0, -1, 1));
        row.addView(text(String.format(Locale.US, "%.2f km", o.optDouble("distanceKm", 0)), 18, BLUE, true));
        card.addView(row, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView line = text(String.format(Locale.US, "⏱ %s     śr. %.3f km/h     maks. %.1f km/h", o.optString("elapsed", "00:00:00"), o.optDouble("avg", 0), o.optDouble("max", 0)), 14, Color.rgb(51,65,85), true);
        card.addView(line, new LinearLayout.LayoutParams(-1, dp(32)));
        if (expanded) {
            RouteView rv = new RouteView(this); rv.setPointsFromJson(o.optString("pointsJson", "[]"));
            LinearLayout.LayoutParams rvLp = new LinearLayout.LayoutParams(-1, dp(145)); rvLp.topMargin = dp(8); card.addView(rv, rvLp);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, expanded ? dp(262) : dp(112)); lp.topMargin = dp(10); contentBox.addView(card, lp);
    }

    private void renderStats() {
        currentTab = 2; updateNav(); controlsRow.setVisibility(View.GONE);
        contentBox.removeAllViews(); contentBox.setPadding(dp(16), 0, dp(16), dp(18));
        contentBox.addView(text("Statystyki", 24, NAVY, true), new LinearLayout.LayoutParams(-1, dp(54)));
        try {
            JSONArray arr = new JSONArray(prefs().getString("history", "[]"));
            double total = 0, bestAvg = 0, bestMax = 0; long time = 0;
            int bikeCount = 0, carCount = 0;
            for (int i=0;i<arr.length();i++) {
                JSONObject o=arr.getJSONObject(i);
                total += o.optDouble("distanceKm",0);
                bestAvg=Math.max(bestAvg,o.optDouble("avg",0));
                bestMax=Math.max(bestMax,o.optDouble("max",0));
                time += o.optLong("elapsedMs",0);
                if ("Samochód".equals(o.optString("mode"))) carCount++; else bikeCount++;
            }
            LinearLayout row1 = new LinearLayout(this); row1.setGravity(Gravity.CENTER); contentBox.addView(row1, new LinearLayout.LayoutParams(-1, dp(112)));
            addStat(row1, "Σ", "Dystans", String.format(Locale.US, "%.2f\nkm", total), BLUE);
            addStat(row1, "◴", "Czas", formatDuration(time) + "\nrazem", GREEN);
            LinearLayout row2 = new LinearLayout(this); row2.setGravity(Gravity.CENTER); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(112)); lp.topMargin=dp(10); contentBox.addView(row2, lp);
            addStat(row2, "↗", "Najl. średnia", String.format(Locale.US, "%.3f\nkm/h", bestAvg), GREEN);
            addStat(row2, "▲", "Rekord", String.format(Locale.US, "%.1f\nkm/h", bestMax), PURPLE);
            LinearLayout row3 = new LinearLayout(this); row3.setGravity(Gravity.CENTER); LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(-1, dp(112)); lp3.topMargin=dp(10); contentBox.addView(row3, lp3);
            addStat(row3, "🚲", "Rower", bikeCount + "\njazd", GREEN);
            addStat(row3, "🚗", "Samochód", carCount + "\njazd", BLUE);
        } catch (Exception ignored) {}
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(6));

        TextView version = text("Wersja: " + VERSION_NAME + "\nPełna wersja Android — build release", 16, NAVY, true);
        box.addView(version, new LinearLayout.LayoutParams(-1, dp(64)));

        CheckBox autoUpdate = new CheckBox(this);
        autoUpdate.setText("Sprawdzaj aktualizacje przy starcie");
        autoUpdate.setTextSize(15);
        autoUpdate.setTextColor(Color.rgb(51,65,85));
        autoUpdate.setChecked(prefs().getBoolean("auto_update_check", false));
        autoUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> prefs().edit().putBoolean("auto_update_check", isChecked).apply());
        box.addView(autoUpdate, new LinearLayout.LayoutParams(-1, dp(46)));

        box.addView(settingsButton("Sprawdź aktualizację", BLUE, v -> checkForUpdates(true)));
        box.addView(settingsButton("Zezwolenia i ustawienia aplikacji", GREEN, v -> openAppSettings()));
        box.addView(settingsButton("Ustawienia baterii Android", ORANGE, v -> openBatterySettings()));
        box.addView(settingsButton("Wyczyść historię jazdy", RED, v -> confirmClearHistory()));

        TextView note = text("Działanie po zablokowaniu ekranu wymaga stałego powiadomienia GPS. Jeżeli telefon ubija aplikację, wyłącz oszczędzanie baterii dla Licznika jazdy.", 13, MUTED, false);
        note.setPadding(0, dp(8), 0, 0);
        box.addView(note, new LinearLayout.LayoutParams(-1, dp(84)));

        new AlertDialog.Builder(this)
                .setTitle("Opcje")
                .setView(box)
                .setNegativeButton("Zamknij", null)
                .show();
    }

    private TextView settingsButton(String label, int color, View.OnClickListener listener) {
        TextView b = text(label, 15, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(color, 15, color, 0));
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.setMargins(0, dp(6), 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this)
                .setTitle("Wyczyścić historię?")
                .setMessage("Usunie to zapisane jazdy z telefonu.")
                .setPositiveButton("Usuń", (d,w) -> {
                    prefs().edit().putString("history", "[]").apply();
                    Toast.makeText(this, "Historia wyczyszczona.", Toast.LENGTH_SHORT).show();
                    if (currentTab == 1) renderHistory();
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openBatterySettings() {
        try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
        catch (Exception e) { openAppSettings(); }
    }

    private void checkForUpdates(boolean showNoUpdate) {
        Toast.makeText(this, "Sprawdzam aktualizację...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String apkUrl = null;
            String tag = null;
            String error = null;
            int remoteCode = -1;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_API_LATEST).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                int code = conn.getResponseCode();
                if (code == 404) throw new Exception("Nie ma jeszcze opublikowanej wersji Release.");
                if (code < 200 || code > 299) throw new Exception("Serwer zwrócił kod: " + code);
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                JSONObject root = new JSONObject(sb.toString());
                tag = root.optString("tag_name", "");
                remoteCode = versionCodeFromTag(tag);

                if (tag.equals(CURRENT_RELEASE_TAG) || (remoteCode > 0 && remoteCode <= CURRENT_VERSION_CODE)) {
                    apkUrl = null;
                    error = "Masz aktualną wersję programu.";
                } else {
                    JSONArray assets = root.optJSONArray("assets");
                    if (assets != null) {
                        for (int i=0;i<assets.length();i++) {
                            JSONObject a = assets.getJSONObject(i);
                            String name = a.optString("name", "").toLowerCase(Locale.ROOT);
                            if (name.endsWith(".apk")) { apkUrl = a.optString("browser_download_url", null); break; }
                        }
                    }
                    if (apkUrl == null) throw new Exception("Znaleziono nowszy wpis, ale bez pliku APK.");
                }
            } catch (Exception e) { if (error == null) error = e.getMessage(); }

            String finalApkUrl = apkUrl;
            String finalTag = tag;
            String finalError = error;
            runOnUiThread(() -> {
                if (finalApkUrl != null) {
                    new AlertDialog.Builder(this)
                            .setTitle("Dostępna aktualizacja")
                            .setMessage("Znaleziono wersję: " + finalTag + "\n\nPobiorę plik APK. Android poprosi Cię jeszcze o potwierdzenie instalacji aktualizacji.")
                            .setPositiveButton("Pobierz", (d,w) -> openUrl(finalApkUrl))
                            .setNegativeButton("Później", null)
                            .show();
                } else if (showNoUpdate) {
                    new AlertDialog.Builder(this)
                            .setTitle("Aktualizacja")
                            .setMessage(finalError == null ? "Masz aktualną wersję programu." : finalError)
                            .setPositiveButton("Zamknij", null)
                            .show();
                }
            });
        }).start();
    }

    private int versionCodeFromTag(String tag) {
        if (tag == null) return -1;
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("v(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*").matcher(tag);
            if (!m.matches()) return -1;
            int major = Integer.parseInt(m.group(1));
            int minor = Integer.parseInt(m.group(2));
            int patch = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
            // W tym projekcie każda kolejna wersja 1.x ma versionCode = x.
            if (major == 1) return minor;
            return major * 1000000 + minor * 1000 + patch;
        } catch (Exception ignored) { return -1; }
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(this, "Nie można otworzyć linku.", Toast.LENGTH_SHORT).show(); }
    }

    private void updateHeaderClock() {
        if (clockPill != null) clockPill.setText(currentClockText());
    }

    private String currentClockText() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    private void updateControlStates() {
        if (primaryActionButton == null || primaryActionIcon == null || primaryActionLabel == null) return;
        if (!running) {
            primaryActionIcon.setText("▶");
            primaryActionLabel.setText("Start");
            primaryActionButton.setBackground(gradient(GREEN, darker(GREEN), 20));
        } else if (paused) {
            primaryActionIcon.setText("▶");
            primaryActionLabel.setText("Wznów");
            primaryActionButton.setBackground(gradient(BLUE, darker(BLUE), 20));
        } else {
            primaryActionIcon.setText("Ⅱ");
            primaryActionLabel.setText("Pauza");
            primaryActionButton.setBackground(gradient(BLUE, darker(BLUE), 20));
        }
    }

    private void updateNav() {
        if (navRide == null) return;
        navRide.setBackground(round(currentTab == 0 ? Color.rgb(239,246,255) : Color.TRANSPARENT, 20, Color.TRANSPARENT, 0));
        navHistory.setBackground(round(currentTab == 1 ? Color.rgb(239,246,255) : Color.TRANSPARENT, 20, Color.TRANSPARENT, 0));
        navStats.setBackground(round(currentTab == 2 ? Color.rgb(239,246,255) : Color.TRANSPARENT, 20, Color.TRANSPARENT, 0));
        navRide.setTextColor(currentTab == 0 ? GREEN : Color.rgb(51,65,85));
        navHistory.setTextColor(currentTab == 1 ? BLUE : Color.rgb(51,65,85));
        navStats.setTextColor(currentTab == 2 ? BLUE : Color.rgb(51,65,85));
    }

    private TextView navItem(String label, boolean active) {
        TextView t = text(label, 13, active ? GREEN : Color.rgb(51,65,85), true);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(Color.WHITE, 26, BORDER, 1));
        c.setElevation(dp(4));
        return c;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView pill(String s, int fg, int bg, int sp, boolean bold) {
        TextView t = text(s, sp, fg, bold);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(bg, 22, BORDER, 0));
        return t;
    }

    private TextView circleText(String s, int sizeDp, int bg, int fg, int sp) {
        TextView t = text(s, sp, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(bg, sizeDp / 2, Color.TRANSPARENT, 0));
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return t;
    }

    private android.graphics.drawable.GradientDrawable gradient(int startColor, int endColor, int radius) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor});
        g.setCornerRadius(dp(radius));
        return g;
    }

    private int darker(int color) {
        int r = Math.max(0, (int)(Color.red(color) * 0.86));
        int g = Math.max(0, (int)(Color.green(color) * 0.86));
        int b = Math.max(0, (int)(Color.blue(color) * 0.86));
        return Color.rgb(r, g, b);
    }

    private android.graphics.drawable.GradientDrawable round(int color, int radius, int strokeColor, int stroke) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        if (stroke > 0) g.setStroke(dp(stroke), strokeColor);
        return g;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    static String formatDuration(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600; long m = (s % 3600) / 60; long sec = s % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec);
    }
}
