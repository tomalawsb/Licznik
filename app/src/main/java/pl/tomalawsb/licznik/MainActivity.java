package pl.tomalawsb.licznik;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends android.app.Activity {
    public static final String VERSION_NAME = "1.3 - 0906260810";

    private static final int REQ_PERMISSIONS = 1001;
    private final int GREEN = Color.rgb(34, 197, 94);
    private final int BLUE = Color.rgb(37, 99, 235);
    private final int NAVY = Color.rgb(8, 24, 64);
    private final int MUTED = Color.rgb(100, 116, 139);
    private final int BG = Color.rgb(247, 250, 255);

    private LinearLayout contentBox;
    private LinearLayout controlsRow;
    private TextView clockPill, statusText, modeRower, modeSamochod;
    private SpeedGaugeView gaugeView;
    private RouteView routeView;
    private TextView avgText, distanceText, timeText, maxText, pointsText, accuracyText;
    private TextView navRide, navHistory, navStats;
    private String selectedMode = "Rower";
    private boolean running = false;
    private boolean paused = false;
    private int currentTab = 0;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!RideTrackingService.ACTION_UPDATE.equals(intent.getAction())) return;
            running = intent.getBooleanExtra("running", false);
            paused = intent.getBooleanExtra("paused", false);
            selectedMode = intent.getStringExtra("mode") == null ? selectedMode : intent.getStringExtra("mode");
            double speed = intent.getDoubleExtra("speed", 0);
            double avg = intent.getDoubleExtra("avg", 0);
            double distanceKm = intent.getDoubleExtra("distanceKm", 0);
            double max = intent.getDoubleExtra("max", 0);
            long elapsed = intent.getLongExtra("elapsed", 0);
            int points = intent.getIntExtra("points", 0);
            double accuracy = intent.getDoubleExtra("accuracy", -1);
            String pointsJson = intent.getStringExtra("pointsJson");

            gaugeView.setSpeed((float) speed);
            avgText.setText(String.format(Locale.US, "%.3f km/h", avg));
            distanceText.setText(String.format(Locale.US, "%.2f\nkm", distanceKm));
            timeText.setText(formatDuration(elapsed) + "\njazdy");
            maxText.setText(String.format(Locale.US, "%.1f\nkm/h", max));
            pointsText.setText(points + "\nGPS");
            clockPill.setText(formatDuration(elapsed));
            accuracyText.setText(accuracy > 0 ? "Dokładność: " + Math.round(accuracy) + " m" : "Dokładność: --");
            if (pointsJson != null) routeView.setPointsFromJson(pointsJson);
            updateModeButtons();
            updateStatus();
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        selectedMode = prefs().getString("last_mode", "Rower");
        buildUi();
        registerUpdates();
        requestPermissionsIfNeeded(false);
        renderRide();
    }

    @Override protected void onDestroy() {
        try { unregisterReceiver(updateReceiver); } catch (Exception ignored) {}
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
        header.setPadding(dp(18), dp(14), dp(18), dp(8));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(96)));

        TextView bikeIcon = circleText("🚲", 48, Color.WHITE, GREEN, 22);
        header.addView(bikeIcon);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -1, 1));

        TextView title = text("Licznik jazdy", 26, NAVY, true);
        titleBox.addView(title);
        statusText = text("PWA: nieaktywne • Android: gotowy", 15, MUTED, false);
        titleBox.addView(statusText);

        clockPill = pill("00:00:00", BLUE, Color.WHITE, 18, true);
        header.addView(clockPill, new LinearLayout.LayoutParams(dp(112), dp(54)));

        TextView settings = circleText("⚙", 54, Color.WHITE, NAVY, 24);
        settings.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(dp(60), dp(60));
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
        controlsRow.setPadding(dp(14), dp(7), dp(14), dp(7));
        controlsRow.setBackgroundColor(Color.WHITE);
        root.addView(controlsRow, new LinearLayout.LayoutParams(-1, dp(82)));
        buildControls();

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(4), dp(6), dp(6));
        nav.setBackgroundColor(Color.WHITE);
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(74)));
        navRide = navItem("🚲\nJazda", true); navHistory = navItem("☰\nHistoria", false); navStats = navItem("▥\nStatystyki", false);
        nav.addView(navRide, new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navHistory, new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navStats, new LinearLayout.LayoutParams(0, -1, 1));
        navRide.setOnClickListener(v -> renderRide());
        navHistory.setOnClickListener(v -> renderHistory());
        navStats.setOnClickListener(v -> renderStats());
    }

    private void buildControls() {
        controlsRow.removeAllViews();
        Button start = controlBtn("▶\nStart", GREEN);
        Button pause = controlBtn("Ⅱ\nPauza", BLUE);
        Button stop = controlBtn("■\nStop", Color.rgb(239, 68, 68));
        Button reset = controlBtn("↻\nReset", Color.rgb(100, 116, 139));
        start.setOnClickListener(v -> startRide());
        pause.setOnClickListener(v -> togglePause());
        stop.setOnClickListener(v -> stopRide());
        reset.setOnClickListener(v -> resetLocalView());
        controlsRow.addView(start, controlLp());
        controlsRow.addView(pause, controlLp());
        controlsRow.addView(stop, controlLp());
        controlsRow.addView(reset, controlLp());
    }

    private LinearLayout.LayoutParams controlLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        lp.setMargins(dp(5), 0, dp(5), 0);
        return lp;
    }

    private Button controlBtn(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(color, 18, color, 0));
        return b;
    }

    private void renderRide() {
        currentTab = 0; updateNav(); controlsRow.setVisibility(View.VISIBLE);
        contentBox.removeAllViews();
        contentBox.setPadding(dp(16), 0, dp(16), dp(8));

        LinearLayout modeCard = card();
        modeCard.setPadding(dp(16), dp(14), dp(16), dp(16));
        TextView label = text("Tryb jazdy", 17, MUTED, true);
        modeCard.addView(label);
        LinearLayout seg = new LinearLayout(this);
        seg.setGravity(Gravity.CENTER);
        seg.setPadding(dp(6), dp(6), dp(6), dp(6));
        seg.setBackground(round(Color.rgb(248,250,252), 22, Color.rgb(218,226,236), 1));
        LinearLayout.LayoutParams segLp = new LinearLayout.LayoutParams(-1, dp(70)); segLp.topMargin = dp(10);
        modeCard.addView(seg, segLp);
        modeRower = modeButton("🚲  Rower"); modeSamochod = modeButton("🚗  Samochód");
        seg.addView(modeRower, new LinearLayout.LayoutParams(0, -1, 1));
        seg.addView(modeSamochod, new LinearLayout.LayoutParams(0, -1, 1));
        modeRower.setOnClickListener(v -> setMode("Rower"));
        modeSamochod.setOnClickListener(v -> setMode("Samochód"));
        contentBox.addView(modeCard, new LinearLayout.LayoutParams(-1, dp(150)));

        LinearLayout speedCard = card();
        speedCard.setPadding(dp(12), dp(8), dp(12), dp(12));
        gaugeView = new SpeedGaugeView(this);
        speedCard.addView(gaugeView, new LinearLayout.LayoutParams(-1, dp(245)));

        LinearLayout avg = new LinearLayout(this);
        avg.setOrientation(LinearLayout.HORIZONTAL);
        avg.setGravity(Gravity.CENTER_VERTICAL);
        avg.setPadding(dp(14), 0, dp(14), 0);
        avg.setBackground(round(Color.rgb(240,253,244), 20, Color.rgb(187,247,208), 1));
        TextView avgIcon = circleText("↗", 56, GREEN, Color.WHITE, 26);
        avg.addView(avgIcon);
        LinearLayout avgTexts = new LinearLayout(this); avgTexts.setOrientation(LinearLayout.VERTICAL); avgTexts.setPadding(dp(14),0,0,0);
        avgTexts.addView(text("Średnia prędkość", 16, NAVY, true));
        avgText = text("0.000 km/h", 31, GREEN, true);
        avgTexts.addView(avgText);
        avg.addView(avgTexts, new LinearLayout.LayoutParams(0, -1, 1));
        LinearLayout.LayoutParams avgLp = new LinearLayout.LayoutParams(-1, dp(86));
        speedCard.addView(avg, avgLp);
        LinearLayout.LayoutParams speedLp = new LinearLayout.LayoutParams(-1, dp(355)); speedLp.topMargin = dp(14);
        contentBox.addView(speedCard, speedLp);

        LinearLayout stats = new LinearLayout(this); stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(-1, dp(112)); statsLp.topMargin = dp(12);
        contentBox.addView(stats, statsLp);
        distanceText = addStat(stats, "⌁", "Dystans", "0.00\nkm", BLUE);
        timeText = addStat(stats, "◴", "Czas", "00:00:00\njazdy", BLUE);
        maxText = addStat(stats, "▲", "Maks.", "0.0\nkm/h", Color.rgb(124,58,237));
        pointsText = addStat(stats, "⊙", "Punkty", "0\nGPS", BLUE);

        LinearLayout mapCard = card();
        mapCard.setPadding(dp(14), dp(10), dp(14), dp(14));
        LinearLayout mapHeader = new LinearLayout(this); mapHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView mapTitle = text("Trasa GPS", 17, MUTED, true); mapHeader.addView(mapTitle, new LinearLayout.LayoutParams(0, -1, 1));
        accuracyText = text("Dokładność: --", 15, MUTED, false); mapHeader.addView(accuracyText);
        mapCard.addView(mapHeader, new LinearLayout.LayoutParams(-1, dp(36)));
        routeView = new RouteView(this);
        mapCard.addView(routeView, new LinearLayout.LayoutParams(-1, dp(150)));
        LinearLayout.LayoutParams mapLp = new LinearLayout.LayoutParams(-1, dp(220)); mapLp.topMargin = dp(12);
        contentBox.addView(mapCard, mapLp);
        updateModeButtons();
        updateStatus();
        requestSnapshot();
    }

    private TextView addStat(LinearLayout parent, String icon, String label, String value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(5), dp(6), dp(5), dp(6));
        box.setBackground(round(Color.WHITE, 22, Color.rgb(226,232,240), 1));
        TextView i = text(icon, 18, color, true); i.setGravity(Gravity.CENTER); box.addView(i);
        TextView l = text(label, 13, Color.rgb(51,65,85), true); l.setGravity(Gravity.CENTER); box.addView(l);
        TextView v = text(value, 20, color, true); v.setGravity(Gravity.CENTER); box.addView(v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1); lp.setMargins(dp(4),0,dp(4),0);
        parent.addView(box, lp);
        return v;
    }

    private TextView modeButton(String label) {
        TextView t = text(label, 18, NAVY, true);
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
        Toast.makeText(this, "Pomiar uruchomiony. Możesz zablokować ekran.", Toast.LENGTH_LONG).show();
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

    private void resetLocalView() {
        if (running) { Toast.makeText(this, "Najpierw zatrzymaj jazdę.", Toast.LENGTH_SHORT).show(); return; }
        if (gaugeView != null) gaugeView.setSpeed(0);
        if (avgText != null) avgText.setText("0.000 km/h");
        if (distanceText != null) distanceText.setText("0.00\nkm");
        if (timeText != null) timeText.setText("00:00:00\njazdy");
        if (maxText != null) maxText.setText("0.0\nkm/h");
        if (pointsText != null) pointsText.setText("0\nGPS");
        if (routeView != null) routeView.clear();
        clockPill.setText("00:00:00");
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
        startService(i);
    }

    private void updateStatus() {
        if (running && !paused) statusText.setText("Android • GPS działa w usłudze tła");
        else if (running) statusText.setText("Android • pomiar wstrzymany");
        else statusText.setText("Android • GPS gotowy");
    }

    private void renderHistory() {
        currentTab = 1; updateNav(); controlsRow.setVisibility(View.GONE);
        contentBox.removeAllViews(); contentBox.setPadding(dp(16), 0, dp(16), dp(18));
        TextView title = text("Historia jazdy", 24, NAVY, true);
        contentBox.addView(title, new LinearLayout.LayoutParams(-1, dp(48)));
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
            contentBox.addView(text("Nie udało się odczytać historii.", 16, Color.RED, true));
        }
    }

    private void addHistoryCard(JSONObject o, boolean expanded) throws Exception {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        String mode = o.optString("mode", "Rower");
        TextView icon = circleText(mode.equals("Samochód") ? "🚗" : "🚲", 48, mode.equals("Samochód") ? BLUE : GREEN, Color.WHITE, 22);
        row.addView(icon);
        LinearLayout names = new LinearLayout(this); names.setOrientation(LinearLayout.VERTICAL); names.setPadding(dp(12),0,0,0);
        names.addView(text(mode, 17, mode.equals("Samochód") ? BLUE : GREEN, true));
        names.addView(text(o.optString("date", ""), 13, MUTED, false));
        row.addView(names, new LinearLayout.LayoutParams(0, -1, 1));
        row.addView(text(String.format(Locale.US, "%.2f km", o.optDouble("distanceKm", 0)), 18, BLUE, true));
        card.addView(row, new LinearLayout.LayoutParams(-1, dp(56)));
        TextView line = text(String.format(Locale.US, "⏱ %s     śr. %.3f km/h     maks. %.1f km/h", o.optString("elapsed", "00:00:00"), o.optDouble("avg", 0), o.optDouble("max", 0)), 14, Color.rgb(51,65,85), true);
        card.addView(line, new LinearLayout.LayoutParams(-1, dp(32)));
        if (expanded) {
            RouteView rv = new RouteView(this); rv.setPointsFromJson(o.optString("pointsJson", "[]"));
            LinearLayout.LayoutParams rvLp = new LinearLayout.LayoutParams(-1, dp(145)); rvLp.topMargin = dp(8); card.addView(rv, rvLp);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, expanded ? dp(260) : dp(110)); lp.topMargin = dp(10); contentBox.addView(card, lp);
    }

    private void renderStats() {
        currentTab = 2; updateNav(); controlsRow.setVisibility(View.GONE);
        contentBox.removeAllViews(); contentBox.setPadding(dp(16), 0, dp(16), dp(18));
        contentBox.addView(text("Statystyki", 24, NAVY, true), new LinearLayout.LayoutParams(-1, dp(54)));
        try {
            JSONArray arr = new JSONArray(prefs().getString("history", "[]"));
            double total = 0, bestAvg = 0, bestMax = 0; long time = 0;
            for (int i=0;i<arr.length();i++) { JSONObject o=arr.getJSONObject(i); total += o.optDouble("distanceKm",0); bestAvg=Math.max(bestAvg,o.optDouble("avg",0)); bestMax=Math.max(bestMax,o.optDouble("max",0)); time += o.optLong("elapsedMs",0); }
            LinearLayout row1 = new LinearLayout(this); row1.setGravity(Gravity.CENTER); contentBox.addView(row1, new LinearLayout.LayoutParams(-1, dp(126)));
            addStat(row1, "Σ", "Dystans", String.format(Locale.US, "%.2f\nkm", total), BLUE);
            addStat(row1, "◴", "Czas", formatDuration(time) + "\nrazem", GREEN);
            LinearLayout row2 = new LinearLayout(this); row2.setGravity(Gravity.CENTER); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(126)); lp.topMargin=dp(10); contentBox.addView(row2, lp);
            addStat(row2, "↗", "Najl. średnia", String.format(Locale.US, "%.3f\nkm/h", bestAvg), GREEN);
            addStat(row2, "▲", "Rekord", String.format(Locale.US, "%.1f\nkm/h", bestMax), Color.rgb(124,58,237));
        } catch (Exception ignored) {}
    }

    private void showSettings() {
        String msg = "Wersja: " + VERSION_NAME + "\n\n" +
                "Ta wersja jest natywną aplikacją Android. Pomiar GPS jest uruchamiany jako Foreground Service z widocznym powiadomieniem, więc może działać po zablokowaniu telefonu.\n\n" +
                "Dla najlepszego działania wyłącz oszczędzanie baterii dla tej aplikacji.";
        new AlertDialog.Builder(this)
                .setTitle("Opcje")
                .setMessage(msg)
                .setPositiveButton("Ustawienia aplikacji", (d,w) -> openAppSettings())
                .setNegativeButton("Zamknij", null)
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void updateNav() {
        if (navRide == null) return;
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
        c.setBackground(round(Color.WHITE, 26, Color.rgb(226,232,240), 1));
        return c;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return t;
    }

    private TextView pill(String s, int fg, int bg, int sp, boolean bold) {
        TextView t = text(s, sp, fg, bold);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(bg, 22, Color.rgb(226,232,240), 0));
        return t;
    }

    private TextView circleText(String s, int sizeDp, int bg, int fg, int sp) {
        TextView t = text(s, sp, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(bg, sizeDp / 2, Color.TRANSPARENT, 0));
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return t;
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
