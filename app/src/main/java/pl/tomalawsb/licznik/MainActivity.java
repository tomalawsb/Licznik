package pl.tomalawsb.licznik;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends android.app.Activity {
    public static final String VERSION_NAME = "3.4 - 2906261015";
    public static final String CURRENT_RELEASE_TAG = "v3.4-2906261015";
    public static final int CURRENT_VERSION_CODE = 30400;

    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/tomalawsb/Licznik/releases/latest";
    private static final int REQ_PERMISSIONS = 1001;

    private final int GREEN = Color.rgb(16, 163, 99);
    private final int GREEN_LIGHT = Color.rgb(235, 247, 226);
    private final int GREEN_DARK = Color.rgb(5, 132, 83);
    private final int BLUE = Color.rgb(32, 112, 210);
    private final int RED = Color.rgb(220, 55, 55);
    private final int NAVY = Color.rgb(28, 31, 40);
    private final int TEXT = Color.rgb(45, 47, 52);
    private final int MUTED = Color.rgb(125, 125, 125);
    private final int BORDER = Color.rgb(222, 218, 210);
    private final int BG = Color.rgb(250, 248, 243);
    private final int CARD_BG = Color.rgb(255, 254, 250);

    private LinearLayout contentBox;
    private TextView statusText;
    private TextView navRide, navHistory, navProgress, navProfile;
    private TextView speedValueText, speedSummaryText, distanceText, timeText, elevationText, caloriesText, paceText;
    private TextView primaryActionIcon, primaryActionLabel;
    private TextView headerModeIcon, speedHeroWatermark;
    private LinearLayout primaryActionButton;
    private LinearLayout actionHost;
    private RouteMapView routeView;
    private ImageView compassDialView, compassNeedleView, targetCompassView;
    private TextView targetInfoText;
    private TextView poiInfoText;
    private final ArrayList<PoiPoint> poiPoints = new ArrayList<>();
    private int poiIndex = 0;
    private long lastPoiFetchRealtime = 0;
    private boolean poiFetchRunning = false;
    private RouteMapView activeFullMap;
    private Dialog activeRouteDialog;
    private boolean activeFullMapCurrentRoute = false;
    private boolean hasCurrentLocation = false;
    private double currentLat = 0;
    private double currentLon = 0;
    private boolean hasCourseBearing = false;
    private double courseBearing = 0;
    private boolean hasTargetPoint = false;
    private double targetLat = 0;
    private double targetLon = 0;

    private String selectedMode = "Rower";
    private boolean running = false;
    private boolean paused = false;
    private int currentTab = 0;
    private long lastElapsedMs = 0;
    private long elapsedBaseMs = 0;
    private long elapsedSyncRealtime = 0;
    private String lastPointsJson = "[]";
    private boolean waitingForStopResult = false;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final Runnable clockUiTicker = new Runnable() {
        @Override public void run() {
            long displayElapsed = lastElapsedMs;
            if (running && !paused) {
                displayElapsed = elapsedBaseMs + Math.max(0, SystemClock.elapsedRealtime() - elapsedSyncRealtime);
            }
            if (timeText != null) timeText.setText(formatDuration(displayElapsed) + "\nczas jazdy");
            uiHandler.postDelayed(this, 250);
        }
    };

    private final Runnable poiRotator = new Runnable() {
        @Override public void run() {
            cyclePoi(false);
            uiHandler.postDelayed(this, 180000);
        }
    };

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!RideTrackingService.ACTION_UPDATE.equals(intent.getAction())) return;

            running = intent.getBooleanExtra("running", false);
            paused = intent.getBooleanExtra("paused", false);
            String incomingMode = intent.getStringExtra("mode");
            if (incomingMode != null && !incomingMode.equals(selectedMode)) {
                selectedMode = incomingMode;
                updateModeVisuals();
            }

            double speed = intent.getDoubleExtra("speed", 0);
            double avg = intent.getDoubleExtra("avg", 0);
            double distanceKm = intent.getDoubleExtra("distanceKm", 0);
            double max = intent.getDoubleExtra("max", 0);
            long elapsed = intent.getLongExtra("elapsed", 0);
            double accuracy = intent.getDoubleExtra("accuracy", -1);
            String pointsJson = intent.getStringExtra("pointsJson");
            if (pointsJson != null) {
                lastPointsJson = pointsJson;
                updateCurrentLocationFromPoints(pointsJson);
            }
            boolean stopped = intent.getBooleanExtra(RideTrackingService.EXTRA_STOPPED, false);
            String serviceMessage = intent.getStringExtra(RideTrackingService.EXTRA_MESSAGE);
            if (stopped && waitingForStopResult) {
                waitingForStopResult = false;
                Toast.makeText(MainActivity.this, serviceMessage == null ? "Pomiar zakończony." : serviceMessage, Toast.LENGTH_SHORT).show();
                if (currentTab == 0) renderRide();
                else if (currentTab == 1) renderHistory();
            }

            lastElapsedMs = elapsed;
            elapsedBaseMs = elapsed;
            elapsedSyncRealtime = SystemClock.elapsedRealtime();

            if (speedValueText != null) speedValueText.setText(String.format(Locale.US, "%.1f", speed));
            if (speedSummaryText != null) speedSummaryText.setText(String.format(Locale.US, "Śr: %.3f  •  Maks: %.1f km/h", avg, max));
            if (distanceText != null) distanceText.setText(String.format(Locale.US, "%.2f\nkm dystansu", distanceKm));
            if (timeText != null) timeText.setText(formatDuration(elapsed) + "\nczas jazdy");
            if (caloriesText != null) caloriesText.setText(formatCalories(distanceKm));
            if (paceText != null) paceText.setText(formatPace(avg));
            if (elevationText != null) elevationText.setText("--");
            if (routeView != null && pointsJson != null) {
                routeView.setPointsFromJson(pointsJson);
                if (hasTargetPoint) routeView.setTargetPoint(targetLat, targetLon);
            }
            if (activeFullMap != null && activeFullMapCurrentRoute && pointsJson != null) {
                activeFullMap.setPointsFromJson(pointsJson);
                if (hasTargetPoint) activeFullMap.setTargetPoint(targetLat, targetLon);
                activeFullMap.centerOnLastPoint(17.0);
            }
            updateCompassAndTargetUi();
            updateStatus(accuracy);
            updatePrimaryButton();
        }
    };


    private static class PoiPoint {
        final String name;
        final String type;
        final String emoji;
        final double lat;
        final double lon;
        double distance;

        PoiPoint(String name, String type, String emoji, double lat, double lon, double distance) {
            this.name = name == null || name.trim().isEmpty() ? type : name.trim();
            this.type = type;
            this.emoji = emoji;
            this.lat = lat;
            this.lon = lon;
            this.distance = distance;
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(BG);
        selectedMode = prefs().getString("last_mode", "Rower");
        loadSavedTargetPoint();
        buildUi();
        enterImmersiveMode();
        registerUpdates();
        requestPermissionsIfNeeded(false);
        renderRide();
        uiHandler.post(clockUiTicker);
        uiHandler.postDelayed(poiRotator, 180000);
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
        header.setPadding(dp(18), dp(10), dp(18), dp(5));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(84)));

        headerModeIcon = circleText(modeEmoji(), 46, Color.rgb(232, 247, 235), GREEN_DARK, 21);
        headerModeIcon.setElevation(dp(2));
        headerModeIcon.setOnClickListener(v -> chooseModeDialog());
        header.addView(headerModeIcon);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(14), 0, 0, 0);
        header.addView(titleBox, new LinearLayout.LayoutParams(0, -1, 1));

        TextView greeting = text("Dzień dobry!", 13, MUTED, false);
        titleBox.addView(greeting, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView title = text("Licznik jazdy", 22, NAVY, true);
        title.setSingleLine(true);
        titleBox.addView(title, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView bell = circleText("🔔", 42, Color.WHITE, NAVY, 18);
        bell.setElevation(dp(2));
        bell.setOnClickListener(v -> Toast.makeText(this, "Powiadomienie GPS pojawia się po starcie jazdy.", Toast.LENGTH_SHORT).show());
        header.addView(bell, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView settings = circleText("⚙", 42, Color.WHITE, NAVY, 21);
        settings.setElevation(dp(2));
        settings.setOnClickListener(v -> showSettings());
        LinearLayout.LayoutParams setLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        setLp.leftMargin = dp(10);
        header.addView(settings, setLp);

        contentBox = new LinearLayout(this);
        contentBox.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(contentBox);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        actionHost = new LinearLayout(this);
        actionHost.setOrientation(LinearLayout.VERTICAL);
        actionHost.setGravity(Gravity.CENTER);
        actionHost.setPadding(dp(16), dp(4), dp(16), dp(4));
        actionHost.setVisibility(View.GONE);
        root.addView(actionHost, new LinearLayout.LayoutParams(-1, dp(74)));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10), dp(4), dp(10), dp(8));
        nav.setBackground(round(Color.WHITE, 28, Color.TRANSPARENT, 0));
        nav.setElevation(dp(4));
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(70)));

        navRide = navItem(modeEmoji() + "\nJazda", true);
        navHistory = navItem("↺\nHistoria", false);
        navProgress = navItem("⌁\nPostępy", false);
        navProfile = navItem("♙\nProfil", false);
        nav.addView(navRide, new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navHistory, new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navProgress, new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navProfile, new LinearLayout.LayoutParams(0, -1, 1));
        navRide.setOnClickListener(v -> renderRide());
        navHistory.setOnClickListener(v -> renderHistory());
        navProgress.setOnClickListener(v -> renderProgress());
        navProfile.setOnClickListener(v -> renderProfile());
    }

    private void renderRide() {
        currentTab = 0;
        updateNav();
        if (actionHost != null) actionHost.setVisibility(View.GONE);
        contentBox.removeAllViews();
        contentBox.setPadding(dp(16), 0, dp(16), dp(8));
        if (actionHost != null) actionHost.setVisibility(View.VISIBLE);

        buildSpeedHero();
        buildDistanceTimeRow();
        buildGpsCard();
        buildActionRow();

        updateStatus(-1);
        updatePrimaryButton();
        requestSnapshot();
    }

    private void buildSpeedHero() {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(gradient(GREEN, Color.rgb(0, 126, 89), 20));
        hero.setPadding(dp(18), dp(9), dp(18), dp(9));
        hero.setElevation(dp(2));

        speedHeroWatermark = null;

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.LEFT);
        hero.addView(column, new FrameLayout.LayoutParams(-1, -1));

        TextView label = text("◴  AKTUALNA PRĘDKOŚĆ", 13, Color.rgb(215, 250, 229), true);
        column.addView(label, new LinearLayout.LayoutParams(-1, dp(24)));

        LinearLayout speedLine = new LinearLayout(this);
        speedLine.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        speedValueText = text("0.0", 54, Color.WHITE, true);
        speedValueText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        speedLine.addView(speedValueText, new LinearLayout.LayoutParams(-2, dp(66)));
        TextView unit = text(" km/h", 19, Color.rgb(210, 242, 224), true);
        unit.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        speedLine.addView(unit, new LinearLayout.LayoutParams(-2, dp(66)));
        column.addView(speedLine, new LinearLayout.LayoutParams(-1, dp(66)));

        speedSummaryText = text("Śr: 0.000  •  Maks: 0.0 km/h", 16, Color.rgb(229, 250, 237), true);
        speedSummaryText.setSingleLine(true);
        speedSummaryText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        column.addView(speedSummaryText, new LinearLayout.LayoutParams(-1, dp(38)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(148));
        lp.setMargins(0, dp(4), 0, dp(10));
        contentBox.addView(hero, lp);
    }

    private void buildDistanceTimeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(100));
        rowLp.setMargins(0, 0, 0, dp(10));
        contentBox.addView(row, rowLp);

        distanceText = buildBigStatCard(row, "⌘", "0.00\nkm dystansu", GREEN_DARK);
        timeText = buildBigStatCard(row, "◷", "00:00:00\nczas jazdy", BLUE);
    }

    private TextView buildBigStatCard(LinearLayout parent, String icon, String value, int accent) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(dp(8), dp(8), dp(8), dp(8));
        c.setBackground(round(CARD_BG, 16, BORDER, 1));
        c.setElevation(dp(2));

        TextView i = text(icon, 20, accent, true);
        i.setGravity(Gravity.CENTER);
        c.addView(i, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView v = text(value, 17, TEXT, true);
        v.setGravity(Gravity.CENTER);
        c.addView(v, new LinearLayout.LayoutParams(-1, 0, 2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        lp.setMargins(dp(5), 0, dp(5), 0);
        parent.addView(c, lp);
        return v;
    }

    private void buildGpsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(CARD_BG, 18, BORDER, 1));
        card.setElevation(dp(2));

        FrameLayout mapFrame = new FrameLayout(this);
        routeView = new RouteMapView(this);
        routeView.setOnClickListener(v -> showRouteMapDialog("Aktualna trasa", lastPointsJson,
      String.format(Locale.US, "Dystans %s  •  czas %s", compactFirstLine(distanceText), formatDuration(lastElapsedMs))));
        mapFrame.addView(routeView, new FrameLayout.LayoutParams(-1, -1));

        statusText = pill("●  GPS gotowy", Color.WHITE, GREEN, 12, true);
        FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(dp(120), dp(30));
        statusLp.leftMargin = dp(12);
        statusLp.topMargin = dp(10);
        mapFrame.addView(statusText, statusLp);

        View compassOverlay = buildCompassOverlay();
        FrameLayout.LayoutParams compassLp = new FrameLayout.LayoutParams(dp(142), dp(174), Gravity.TOP | Gravity.RIGHT);
        compassLp.topMargin = dp(8);
        compassLp.rightMargin = dp(8);
        mapFrame.addView(compassOverlay, compassLp);

        card.addView(mapFrame, new LinearLayout.LayoutParams(-1, dp(300)));

        LinearLayout extras = new LinearLayout(this);
        extras.setGravity(Gravity.CENTER);
        extras.setPadding(dp(6), dp(8), dp(6), dp(8));
        extras.setBackgroundColor(Color.WHITE);
        elevationText = buildMiniMetric(extras, "▲", "Wzniesienie", "--", Color.rgb(72, 142, 55));
        caloriesText = buildMiniMetric(extras, "♨", "Kalorie", "--", Color.rgb(58, 110, 50));
        paceText = buildMiniMetric(extras, "◴", "Tempo", "--", Color.rgb(58, 110, 50));
        card.addView(extras, new LinearLayout.LayoutParams(-1, dp(66)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(366));
        lp.setMargins(0, 0, 0, dp(10));
        contentBox.addView(card, lp);

        updateCompassAndTargetUi();
    }

    private View buildCompassOverlay() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(0, 0, 0, 0);
        box.setBackgroundColor(Color.TRANSPARENT);
        box.setClickable(true);
        box.setOnClickListener(v -> {
            if (hasTargetPoint) {
                Toast.makeText(this, "Cel: " + formatDistanceMeters(distanceMeters(currentLat, currentLon, targetLat, targetLon)), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Dotknij mapy i przytrzymaj palec, żeby ustawić cel.", Toast.LENGTH_SHORT).show();
            }
        });

        FrameLayout stack = new FrameLayout(this);
        compassDialView = new ImageView(this);
        compassDialView.setImageResource(R.drawable.kompas_tarcza);
        compassDialView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        stack.addView(compassDialView, new FrameLayout.LayoutParams(-1, -1));

        compassNeedleView = new ImageView(this);
        compassNeedleView.setImageResource(R.drawable.kompas_igla_glowna);
        compassNeedleView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        stack.addView(compassNeedleView, new FrameLayout.LayoutParams(-1, -1));

        targetCompassView = new ImageView(this);
        targetCompassView.setImageResource(R.drawable.kompas_wskaznik_celu);
        targetCompassView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        targetCompassView.setVisibility(hasTargetPoint ? View.VISIBLE : View.INVISIBLE);
        stack.addView(targetCompassView, new FrameLayout.LayoutParams(-1, -1));

        box.addView(stack, new LinearLayout.LayoutParams(dp(104), dp(104)));

        targetInfoText = text("Przytrzymaj mapę\naby ustawić cel", 10, Color.WHITE, true);
        targetInfoText.setGravity(Gravity.CENTER);
        targetInfoText.setShadowLayer(4f, 0f, 1f, Color.BLACK);
        targetInfoText.setSingleLine(false);
        box.addView(targetInfoText, new LinearLayout.LayoutParams(-1, dp(34)));

        poiInfoText = text("POI: szukam...", 10, Color.WHITE, true);
        poiInfoText.setGravity(Gravity.CENTER);
        poiInfoText.setSingleLine(true);
        poiInfoText.setShadowLayer(4f, 0f, 1f, Color.BLACK);
        poiInfoText.setBackground(round(Color.argb(95, 0, 0, 0), 10, Color.argb(90, 255, 255, 255), 1));
        poiInfoText.setPadding(dp(4), 0, dp(4), 0);
        poiInfoText.setOnClickListener(v -> cyclePoi(true));
        poiInfoText.setOnLongClickListener(v -> {
            PoiPoint p = currentPoiPoint();
            if (p != null) {
                setTargetPoint(p.lat, p.lon);
                Toast.makeText(this, "POI ustawiony jako cel: " + p.name, Toast.LENGTH_LONG).show();
                return true;
            }
            return false;
        });
        box.addView(poiInfoText, new LinearLayout.LayoutParams(-1, dp(28)));
        return box;
    }

    private TextView buildMiniMetric(LinearLayout parent, String icon, String label, String value, int accent) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        TextView i = text(icon, 14, accent, true);
        i.setGravity(Gravity.CENTER);
        col.addView(i, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView v = text(value, 14, TEXT, true);
        v.setGravity(Gravity.CENTER);
        col.addView(v, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView l = text(label, 11, MUTED, false);
        l.setGravity(Gravity.CENTER);
        col.addView(l, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        parent.addView(col, lp);
        return v;
    }

    private void buildActionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, 0, 0, 0);

        primaryActionButton = actionBtn("▷", "Rozpocznij jazdę", GREEN, v -> primaryRideAction());
        primaryActionIcon = (TextView) primaryActionButton.getChildAt(0);
        primaryActionLabel = (TextView) primaryActionButton.getChildAt(1);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, -1, 1);
        pLp.setMargins(0, 0, dp(8), 0);
        row.addView(primaryActionButton, pLp);

        TextView stop = squareAction("■", RED, v -> stopRide());
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(dp(64), -1);
        sLp.setMargins(0, 0, dp(8), 0);
        row.addView(stop, sLp);

        TextView reset = squareAction("↻", TEXT, v -> resetRide());
        row.addView(reset, new LinearLayout.LayoutParams(dp(64), -1));

        if (actionHost != null) {
            actionHost.removeAllViews();
            actionHost.addView(row, new LinearLayout.LayoutParams(-1, -1));
        } else {
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(70));
            rowLp.setMargins(0, dp(2), 0, dp(6));
            contentBox.addView(row, rowLp);
        }
    }

    private void buildRecentRides() {
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("OSTATNIE JAZDY", 12, MUTED, true);
        title.setLetterSpacing(0.08f);
        titleRow.addView(title, new LinearLayout.LayoutParams(-2, dp(32)));
        TextView line = text("", 1, BORDER, false);
        line.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(0, dp(1), 1);
        lineLp.leftMargin = dp(12);
        titleRow.addView(line, lineLp);
        contentBox.addView(titleRow, new LinearLayout.LayoutParams(-1, dp(34)));

        try {
            JSONArray arr = new JSONArray(prefs().getString("history", "[]"));
            if (arr.length() == 0) {
                TextView empty = text("Brak zapisanych jazd. Po zakończeniu trasy pojawią się tutaj ostatnie wyniki.", 13, MUTED, false);
                empty.setGravity(Gravity.CENTER_VERTICAL);
                contentBox.addView(empty, new LinearLayout.LayoutParams(-1, dp(58)));
                return;
            }
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setBackground(round(CARD_BG, 18, BORDER, 1));
            list.setElevation(dp(2));
            int shown = 0;
            for (int i = arr.length() - 1; i >= 0 && shown < 2; i--, shown++) {
                JSONObject o = arr.getJSONObject(i);
                list.addView(recentRideRow(o), new LinearLayout.LayoutParams(-1, dp(74)));
                if (shown == 0 && arr.length() > 1) {
                    TextView divider = new TextView(this);
                    divider.setBackgroundColor(Color.rgb(235, 231, 223));
                    list.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
                }
            }
            contentBox.addView(list, new LinearLayout.LayoutParams(-1, dp(Math.min(2, arr.length()) * 74 + (arr.length() > 1 ? 1 : 0))));
        } catch (Exception ignored) {}
    }

    private View recentRideRow(JSONObject o) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(6), dp(12), dp(6));
        String mode = o.optString("mode", "Rower");
        TextView icon = circleText(mode.equals("Samochód") ? "🚗" : "🚲", 36, Color.rgb(235, 247, 226), GREEN_DARK, 17);
        row.addView(icon);
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(12), 0, 0, 0);
        labels.addView(text(mode.equals("Samochód") ? "Przejazd samochodem" : "Przejazd", 13, TEXT, true), new LinearLayout.LayoutParams(-1, 0, 1));
        labels.addView(text(o.optString("date", ""), 11, MUTED, false), new LinearLayout.LayoutParams(-1, 0, 1));
        row.addView(labels, new LinearLayout.LayoutParams(0, -1, 1));
        TextView dist = text(String.format(Locale.US, "%.2f km  ›", o.optDouble("distanceKm", 0)), 13, GREEN_DARK, true);
        dist.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(dist, new LinearLayout.LayoutParams(dp(96), -1));
        return row;
    }

    private void renderHistory() {
        currentTab = 1;
        updateNav();
        if (actionHost != null) actionHost.setVisibility(View.GONE);
        contentBox.removeAllViews();
        contentBox.setPadding(dp(16), 0, dp(16), dp(18));
        contentBox.addView(text("Historia", 23, NAVY, true), new LinearLayout.LayoutParams(-1, dp(48)));
        try {
            JSONArray arr = new JSONArray(prefs().getString("history", "[]"));
            if (arr.length() == 0) {
                TextView empty = text("Brak zapisanych jazd.", 16, MUTED, false);
                empty.setGravity(Gravity.CENTER);
                contentBox.addView(empty, new LinearLayout.LayoutParams(-1, dp(140)));
                return;
            }
            for (int i = arr.length() - 1; i >= 0; i--) addHistoryCard(arr.getJSONObject(i), i);
        } catch (Exception e) {
            contentBox.addView(text("Nie udało się odczytać historii.", 16, RED, true));
        }
    }

    private void addHistoryCard(JSONObject o, int historyIndex) throws Exception {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(CARD_BG, 18, BORDER, 1));
        card.setElevation(dp(2));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        String mode = o.optString("mode", "Rower");
        TextView icon = circleText(mode.equals("Samochód") ? "🚗" : "🚲", 44, mode.equals("Samochód") ? BLUE : GREEN, Color.WHITE, 20);
        top.addView(icon);
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);
        info.addView(text(mode, 15, TEXT, true), new LinearLayout.LayoutParams(-1, 0, 1));
        info.addView(text(o.optString("date", ""), 12, MUTED, false), new LinearLayout.LayoutParams(-1, 0, 1));
        top.addView(info, new LinearLayout.LayoutParams(0, -1, 1));
        TextView dist = text(String.format(Locale.US, "%.2f km", o.optDouble("distanceKm", 0)), 15, NAVY, true);
        dist.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        top.addView(dist, new LinearLayout.LayoutParams(dp(82), -1));

        TextView del = pill("Usuń", Color.WHITE, RED, 12, true);
        del.setGravity(Gravity.CENTER);
        del.setOnClickListener(v -> confirmDeleteHistoryEntry(historyIndex));
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(58), dp(34));
        delLp.leftMargin = dp(8);
        top.addView(del, delLp);

        card.addView(top, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView metrics = text(String.format(Locale.US, "⏱ %s   •   śr. %.3f km/h   •   maks. %.1f km/h",
                o.optString("elapsed", "00:00:00"), o.optDouble("avg", 0), o.optDouble("max", 0)), 10, TEXT, true);
        metrics.setSingleLine(true);
        card.addView(metrics, new LinearLayout.LayoutParams(-1, dp(28)));

        String pointsForMap = o.optString("pointsJson", "[]");
        String mapSummary = String.format(Locale.US, "%s  •  %.2f km  •  %s  •  śr. %.3f km/h  •  maks. %.1f km/h",
                mode, o.optDouble("distanceKm", 0), o.optString("elapsed", "00:00:00"), o.optDouble("avg", 0), o.optDouble("max", 0));
        RouteMapView rv = new RouteMapView(this);
        rv.setPointsFromJson(pointsForMap);
        rv.setOnClickListener(v -> showRouteMapDialog("Szczegóły trasy", pointsForMap, mapSummary));
        LinearLayout.LayoutParams rvLp = new LinearLayout.LayoutParams(-1, dp(104));
        rvLp.topMargin = dp(8);
        card.addView(rv, rvLp);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(212));
        lp.setMargins(0, 0, 0, dp(12));
        contentBox.addView(card, lp);
    }


    private void confirmDeleteHistoryEntry(int index) {
        new AlertDialog.Builder(this)
                .setTitle("Usunąć tę trasę?")
                .setMessage("Ta jedna trasa zostanie usunięta z historii.")
                .setPositiveButton("Usuń", (dialog, which) -> deleteHistoryEntry(index))
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void deleteHistoryEntry(int index) {
        try {
            JSONArray arr = new JSONArray(prefs().getString("history", "[]"));
            JSONArray out = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                if (i != index) out.put(arr.get(i));
            }
            prefs().edit().putString("history", out.toString()).apply();
            Toast.makeText(this, "Trasa usunięta.", Toast.LENGTH_SHORT).show();
            renderHistory();
        } catch (Exception e) {
            Toast.makeText(this, "Nie udało się usunąć trasy.", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderProgress() {
        currentTab = 2;
        updateNav();
        if (actionHost != null) actionHost.setVisibility(View.GONE);
        contentBox.removeAllViews();
        contentBox.setPadding(dp(16), 0, dp(16), dp(18));
        contentBox.addView(text("Postępy", 23, NAVY, true), new LinearLayout.LayoutParams(-1, dp(50)));
        try {
            JSONArray arr = new JSONArray(prefs().getString("history", "[]"));
            double total = 0, bestAvg = 0, bestMax = 0;
            long time = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                total += o.optDouble("distanceKm", 0);
                bestAvg = Math.max(bestAvg, o.optDouble("avg", 0));
                bestMax = Math.max(bestMax, o.optDouble("max", 0));
                time += o.optLong("elapsedMs", 0);
            }
            LinearLayout r1 = new LinearLayout(this);
            r1.setGravity(Gravity.CENTER);
            contentBox.addView(r1, new LinearLayout.LayoutParams(-1, dp(110)));
            buildBigStatCard(r1, "Σ", String.format(Locale.US, "%.2f\nkm razem", total), GREEN_DARK);
            buildBigStatCard(r1, "◷", formatDuration(time) + "\nczas", BLUE);
            LinearLayout r2 = new LinearLayout(this);
            r2.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(110));
            lp.topMargin = dp(12);
            contentBox.addView(r2, lp);
            buildBigStatCard(r2, "↗", String.format(Locale.US, "%.3f\nnajl. śr.", bestAvg), GREEN_DARK);
            buildBigStatCard(r2, "▲", String.format(Locale.US, "%.1f\nrekord", bestMax), NAVY);
        } catch (Exception ignored) {}
    }

    private void renderProfile() {
        currentTab = 3;
        updateNav();
        if (actionHost != null) actionHost.setVisibility(View.GONE);
        contentBox.removeAllViews();
        contentBox.setPadding(dp(16), 0, dp(16), dp(18));
        contentBox.addView(text("Profil i ustawienia", 23, NAVY, true), new LinearLayout.LayoutParams(-1, dp(50)));
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(round(CARD_BG, 18, BORDER, 1));
        card.setElevation(dp(2));
        card.addView(text("Wersja: " + VERSION_NAME, 15, TEXT, true), new LinearLayout.LayoutParams(-1, dp(34)));
        card.addView(settingsButton("Sprawdź aktualizację", GREEN, v -> checkForUpdates(true)));
        card.addView(settingsButton("Zezwolenia i ustawienia aplikacji", BLUE, v -> openAppSettings()));
        card.addView(settingsButton("Ustawienia baterii Android", Color.rgb(168, 112, 35), v -> openBatterySettings()));
        card.addView(settingsButton("Wyczyść historię jazdy", RED, v -> confirmClearHistory()));
        contentBox.addView(card, new LinearLayout.LayoutParams(-1, -2));
    }

    private void chooseModeDialog() {
        if (running) {
            Toast.makeText(this, "Tryb można zmienić przed startem jazdy.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] modes = {"Rower", "Samochód"};
        int checked = "Samochód".equals(selectedMode) ? 1 : 0;
        new AlertDialog.Builder(this)
                .setTitle("Tryb jazdy")
                .setSingleChoiceItems(modes, checked, (dialog, which) -> {
                    selectedMode = modes[which];
                    prefs().edit().putString("last_mode", selectedMode).apply();
                    updateModeVisuals();
                    Toast.makeText(this, "Wybrano: " + selectedMode, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Anuluj", null)
                .show();
    }

    private void primaryRideAction() {
        if (!running) startRide();
        else togglePause();
    }

    private void startRide() {
        if (!requestPermissionsIfNeeded(true)) return;
        Intent i = new Intent(this, RideTrackingService.class);
        i.setAction(RideTrackingService.ACTION_START);
        i.putExtra("mode", selectedMode);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        Toast.makeText(this, "Pomiar uruchomiony.", Toast.LENGTH_SHORT).show();
    }

    private void togglePause() {
        Intent i = new Intent(this, RideTrackingService.class);
        i.setAction(paused ? RideTrackingService.ACTION_RESUME : RideTrackingService.ACTION_PAUSE);
        startService(i);
    }

    private void stopRide() {
        if (!running) {
            Toast.makeText(this, "Pomiar nie jest uruchomiony.", Toast.LENGTH_SHORT).show();
            return;
        }
        waitingForStopResult = true;
        Intent i = new Intent(this, RideTrackingService.class);
        i.setAction(RideTrackingService.ACTION_STOP);
        startService(i);
    }

    private void resetRide() {
        if (running) {
            Intent i = new Intent(this, RideTrackingService.class);
            i.setAction(RideTrackingService.ACTION_RESET);
            try { startService(i); } catch (Exception ignored) {}
        }
        resetLocalViewOnly();
        Toast.makeText(this, running ? "Pomiar wyzerowany." : "Licznik wyzerowany.", Toast.LENGTH_SHORT).show();
    }

    private void resetLocalViewOnly() {
        if (speedValueText != null) speedValueText.setText("0.0");
        if (speedSummaryText != null) speedSummaryText.setText("Średnia: 0.000 km/h  •  Maks: 0.0 km/h");
        if (distanceText != null) distanceText.setText("0.00\nkm dystansu");
        if (timeText != null) timeText.setText("00:00:00\nczas jazdy");
        if (elevationText != null) elevationText.setText("--");
        if (caloriesText != null) caloriesText.setText("--");
        if (paceText != null) paceText.setText("--");
        if (routeView != null) routeView.clear();
        lastPointsJson = "[]";
        lastElapsedMs = 0;
        elapsedBaseMs = 0;
        elapsedSyncRealtime = SystemClock.elapsedRealtime();
    }

    private boolean requestPermissionsIfNeeded(boolean showInfo) {
        java.util.ArrayList<String> missing = new java.util.ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!missing.isEmpty()) {
            if (showInfo) Toast.makeText(this, "Aplikacja potrzebuje lokalizacji i powiadomienia.", Toast.LENGTH_LONG).show();
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

    private void updateStatus(double accuracy) {
        if (statusText == null) return;
        if (running && !paused) {
            if (accuracy > 0 && accuracy <= 12) {
                statusText.setText("●  GPS aktywny");
                statusText.setTextColor(GREEN_DARK);
            } else if (accuracy > 12 && accuracy <= 35) {
                statusText.setText("●  GPS średni");
                statusText.setTextColor(Color.rgb(168, 112, 35));
            } else if (accuracy > 35) {
                statusText.setText("●  GPS słaby");
                statusText.setTextColor(RED);
            } else {
                statusText.setText("●  GPS aktywny");
                statusText.setTextColor(GREEN_DARK);
            }
        } else if (running) {
            statusText.setText("●  Pauza");
            statusText.setTextColor(Color.rgb(168, 112, 35));
        } else {
            statusText.setText("●  GPS gotowy");
            statusText.setTextColor(GREEN_DARK);
        }
    }

    private void updatePrimaryButton() {
        if (primaryActionButton == null || primaryActionIcon == null || primaryActionLabel == null) return;
        if (!running) {
            primaryActionIcon.setText("▷");
            primaryActionLabel.setText("Rozpocznij jazdę");
            primaryActionButton.setBackground(gradient(GREEN, Color.rgb(0, 126, 89), 14));
        } else if (paused) {
            primaryActionIcon.setText("▷");
            primaryActionLabel.setText("Wznów jazdę");
            primaryActionButton.setBackground(gradient(GREEN, Color.rgb(0, 126, 89), 14));
        } else {
            primaryActionIcon.setText("Ⅱ");
            primaryActionLabel.setText("Pauza");
            primaryActionButton.setBackground(gradient(GREEN, Color.rgb(0, 126, 89), 14));
        }
    }

    private void showRouteMapDialog(String title, String pointsJson, String summary) {
        boolean currentRoute = "Aktualna trasa".equals(title);
        try {
            JSONArray arr = new JSONArray(pointsJson == null ? "[]" : pointsJson);
            int requiredPoints = currentRoute ? 1 : 2;
            if (arr.length() < requiredPoints) {
                Toast.makeText(this, "Brak lokalizacji lub trasy do pokazania na mapie.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Nie udało się otworzyć trasy.", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog d = new Dialog(this);
        d.setCanceledOnTouchOutside(false);
        d.setCancelable(true);
        activeRouteDialog = d;
        activeFullMapCurrentRoute = currentRoute;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(14), dp(12), dp(14), dp(12));
        d.setContentView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView titleView = text(title, 19, NAVY, true);
        top.addView(titleView, new LinearLayout.LayoutParams(0, dp(42), 1));
        TextView fitBtnTop = pill(currentRoute ? "Moja pozycja" : "Dopasuj", Color.WHITE, GREEN, 13, true);
        top.addView(fitBtnTop, new LinearLayout.LayoutParams(currentRoute ? dp(112) : dp(86), dp(36)));
        TextView closeBtn = circleText("×", 36, Color.WHITE, NAVY, 22);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        closeLp.leftMargin = dp(8);
        top.addView(closeBtn, closeLp);
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView hint = text((summary == null ? "" : summary + "\n") + "Przytrzymaj palec na mapie, żeby ustawić cel kompasu.", 12, MUTED, false);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(hint, new LinearLayout.LayoutParams(-1, dp(54)));

        RouteMapView fullMap = new RouteMapView(this);
        activeFullMap = fullMap;
        fullMap.setInteractive(true);
        fullMap.setPointsFromJson(pointsJson);
        if (hasTargetPoint) fullMap.setTargetPoint(targetLat, targetLon);
        fullMap.setOnTargetSelectedListener((lat, lon) -> setTargetPoint(lat, lon));
        root.addView(fullMap, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView closeBottom = settingsButton("Zamknij", GREEN, v -> d.dismiss());
        LinearLayout.LayoutParams closeBottomLp = new LinearLayout.LayoutParams(-1, dp(48));
        closeBottomLp.setMargins(0, dp(10), 0, 0);
        root.addView(closeBottom, closeBottomLp);

        View.OnClickListener closeNow = v -> {
            try { d.dismiss(); } catch (Exception ignored) {}
        };
        closeBtn.setOnClickListener(closeNow);
        closeBottom.setOnClickListener(closeNow);
        closeBtn.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_UP) d.dismiss();
            return true;
        });
        closeBottom.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_UP) d.dismiss();
            return true;
        });

        fitBtnTop.setOnClickListener(v -> {
            if (currentRoute) fullMap.centerOnLastPoint(17.0);
            else fullMap.fitRoute();
        });
        d.setOnDismissListener(x -> {
            if (activeRouteDialog == d) activeRouteDialog = null;
            if (activeFullMap == fullMap) activeFullMap = null;
            activeFullMapCurrentRoute = false;
        });
        d.setOnShowListener(x -> {
            Window win = d.getWindow();
            if (win != null) {
                win.setLayout(-1, -1);
                win.setBackgroundDrawableResource(android.R.color.transparent);
            }
            closeBottom.bringToFront();
            closeBtn.bringToFront();
            if (currentRoute) fullMap.centerOnLastPoint(17.0);
            else fullMap.fitRoute();
        });
        d.show();
    }



    private void cyclePoi(boolean manual) {
        if (poiPoints.size() > 0) {
            poiIndex = (poiIndex + 1) % poiPoints.size();
            updatePoiInfoUi();
            if (manual) {
                PoiPoint p = currentPoiPoint();
                if (p != null) Toast.makeText(this, p.name + " • przytrzymaj, aby ustawić jako cel", Toast.LENGTH_SHORT).show();
            }
        } else if (manual) {
            fetchPoiIfNeeded(true);
            Toast.makeText(this, hasCurrentLocation ? "Szukam najbliższych punktów POI..." : "Najpierw potrzebny jest sygnał GPS.", Toast.LENGTH_SHORT).show();
        }
    }

    private PoiPoint currentPoiPoint() {
        if (poiPoints.isEmpty()) return null;
        if (poiIndex < 0 || poiIndex >= poiPoints.size()) poiIndex = 0;
        return poiPoints.get(poiIndex);
    }

    private void updatePoiInfoUi() {
        if (poiInfoText == null) return;
        if (!hasCurrentLocation) {
            poiInfoText.setText("POI: czekam na GPS");
            return;
        }
        if (poiPoints.isEmpty()) {
            poiInfoText.setText("POI: szukam...");
            return;
        }
        for (PoiPoint p : poiPoints) {
            p.distance = distanceMeters(currentLat, currentLon, p.lat, p.lon);
        }
        Collections.sort(poiPoints, (a, b) -> Double.compare(a.distance, b.distance));
        if (poiIndex < 0 || poiIndex >= poiPoints.size()) poiIndex = 0;
        PoiPoint p = poiPoints.get(poiIndex);
        double b = bearingDegrees(currentLat, currentLon, p.lat, p.lon);
        poiInfoText.setText(p.emoji + " " + p.name + "  " + formatDistanceMeters(p.distance) + "  " + arrowForBearing(b));
    }

    private String arrowForBearing(double bearing) {
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        int idx = (int) Math.round(((bearing % 360.0) / 45.0)) % 8;
        return arrows[idx];
    }

    private void fetchPoiIfNeeded(boolean force) {
        if (!hasCurrentLocation || poiFetchRunning) return;
        long now = SystemClock.elapsedRealtime();
        if (!force && !poiPoints.isEmpty() && now - lastPoiFetchRealtime < 600000) return;
        poiFetchRunning = true;
        lastPoiFetchRealtime = now;
        final double lat = currentLat;
        final double lon = currentLon;

        new Thread(() -> {
            ArrayList<PoiPoint> result = new ArrayList<>();
            try {
                String query = buildPoiOverpassQuery(lat, lon);
                URL url = new URL("https://overpass-api.de/api/interpreter");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setConnectTimeout(12000);
                c.setReadTimeout(18000);
                c.setRequestMethod("POST");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                byte[] body = ("data=" + URLEncoder.encode(query, "UTF-8")).getBytes("UTF-8");
                c.setFixedLengthStreamingMode(body.length);
                OutputStream os = c.getOutputStream();
                os.write(body);
                os.close();

                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject root = new JSONObject(sb.toString());
                JSONArray elements = root.optJSONArray("elements");
                if (elements != null) {
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject e = elements.getJSONObject(i);
                        JSONObject tags = e.optJSONObject("tags");
                        if (tags == null) continue;

                        double pLat;
                        double pLon;
                        if (e.has("lat") && e.has("lon")) {
                            pLat = e.getDouble("lat");
                            pLon = e.getDouble("lon");
                        } else {
                            JSONObject center = e.optJSONObject("center");
                            if (center == null) continue;
                            pLat = center.getDouble("lat");
                            pLon = center.getDouble("lon");
                        }

                        String type = poiType(tags);
                        String emoji = poiEmoji(tags);
                        String name = tags.optString("name", "");
                        if (name.trim().isEmpty()) name = type;

                        double dist = distanceMeters(lat, lon, pLat, pLon);
                        if (dist <= 9000) result.add(new PoiPoint(name, type, emoji, pLat, pLon, dist));
                    }
                }

                Collections.sort(result, (a, b) -> Double.compare(a.distance, b.distance));
                ArrayList<PoiPoint> limited = new ArrayList<>();
                for (PoiPoint p : result) {
                    boolean duplicate = false;
                    for (PoiPoint existing : limited) {
                        if (distanceMeters(existing.lat, existing.lon, p.lat, p.lon) < 35 || existing.name.equalsIgnoreCase(p.name)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) limited.add(p);
                    if (limited.size() >= 20) break;
                }

                uiHandler.post(() -> {
                    poiFetchRunning = false;
                    poiPoints.clear();
                    poiPoints.addAll(limited);
                    poiIndex = 0;
                    updatePoiInfoUi();
                    if (poiPoints.isEmpty()) {
                        if (poiInfoText != null) poiInfoText.setText("POI: brak w pobliżu");
                    }
                });
            } catch (Exception ex) {
                uiHandler.post(() -> {
                    poiFetchRunning = false;
                    if (poiInfoText != null) poiInfoText.setText("POI: brak internetu");
                });
            }
        }).start();
    }

    private String buildPoiOverpassQuery(double lat, double lon) {
        String pos = String.format(Locale.US, "%.6f,%.6f", lat, lon);
        return "[out:json][timeout:18];(" +
                "node[amenity=fuel](around:7000," + pos + ");" +
                "way[amenity=fuel](around:7000," + pos + ");" +
                "node[shop~\"supermarket|convenience|bakery|general\"](around:5000," + pos + ");" +
                "way[shop~\"supermarket|convenience|bakery|general\"](around:5000," + pos + ");" +
                "node[tourism~\"alpine_hut|wilderness_hut|camp_site|information|viewpoint\"](around:9000," + pos + ");" +
                "way[tourism~\"alpine_hut|wilderness_hut|camp_site|information|viewpoint\"](around:9000," + pos + ");" +
                "node[amenity~\"pharmacy|parking|restaurant|cafe|hospital|police\"](around:5000," + pos + ");" +
                "way[amenity~\"pharmacy|parking|restaurant|cafe|hospital|police\"](around:5000," + pos + ");" +
                ");out center tags 40;";
    }

    private String poiType(JSONObject tags) {
        String amenity = tags.optString("amenity", "");
        String shop = tags.optString("shop", "");
        String tourism = tags.optString("tourism", "");

        if ("fuel".equals(amenity)) return "Stacja";
        if ("pharmacy".equals(amenity)) return "Apteka";
        if ("parking".equals(amenity)) return "Parking";
        if ("restaurant".equals(amenity)) return "Restauracja";
        if ("cafe".equals(amenity)) return "Kawiarnia";
        if ("hospital".equals(amenity)) return "Szpital";
        if ("police".equals(amenity)) return "Policja";
        if (!shop.isEmpty()) return "Sklep";
        if ("alpine_hut".equals(tourism) || "wilderness_hut".equals(tourism)) return "Schronisko";
        if ("camp_site".equals(tourism)) return "Kemping";
        if ("viewpoint".equals(tourism)) return "Punkt widokowy";
        if ("information".equals(tourism)) return "Informacja";
        return "POI";
    }

    private String poiEmoji(JSONObject tags) {
        String type = poiType(tags);
        if ("Stacja".equals(type)) return "⛽";
        if ("Sklep".equals(type)) return "🛒";
        if ("Schronisko".equals(type)) return "🏠";
        if ("Kemping".equals(type)) return "⛺";
        if ("Parking".equals(type)) return "🅿";
        if ("Apteka".equals(type)) return "✚";
        if ("Restauracja".equals(type)) return "🍽";
        if ("Kawiarnia".equals(type)) return "☕";
        if ("Szpital".equals(type)) return "🏥";
        if ("Policja".equals(type)) return "🚓";
        if ("Punkt widokowy".equals(type)) return "⛰";
        return "◆";
    }

    private void updateCurrentLocationFromPoints(String pointsJson) {
        try {
            JSONArray arr = new JSONArray(pointsJson == null ? "[]" : pointsJson);
            if (arr.length() == 0) return;
            JSONArray p = arr.getJSONArray(arr.length() - 1);
            currentLat = p.getDouble(0);
            currentLon = p.getDouble(1);
            hasCurrentLocation = true;

            if (arr.length() >= 2) {
                JSONArray prev = arr.getJSONArray(arr.length() - 2);
                double prevLat = prev.getDouble(0);
                double prevLon = prev.getDouble(1);
                double moved = distanceMeters(prevLat, prevLon, currentLat, currentLon);
                if (moved >= 2.0) {
                    courseBearing = bearingDegrees(prevLat, prevLon, currentLat, currentLon);
                    hasCourseBearing = true;
                }
            }
            fetchPoiIfNeeded(false);
            updatePoiInfoUi();
        } catch (Exception ignored) {}
    }

    private void setTargetPoint(double lat, double lon) {
        targetLat = lat;
        targetLon = lon;
        hasTargetPoint = true;
        prefs().edit()
                .putBoolean("has_target_point", true)
                .putFloat("target_lat", (float) lat)
                .putFloat("target_lon", (float) lon)
                .apply();
        if (routeView != null) routeView.setTargetPoint(lat, lon);
        if (activeFullMap != null) activeFullMap.setTargetPoint(lat, lon);
        View hapticView = activeFullMap != null ? activeFullMap : routeView;
        if (hapticView != null) hapticView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        updateCompassAndTargetUi();
        Toast.makeText(this, "Cel ustawiony. Turkusowa igła prowadzi do punktu.", Toast.LENGTH_LONG).show();
    }

    private void loadSavedTargetPoint() {
        if (!prefs().getBoolean("has_target_point", false)) return;
        targetLat = prefs().getFloat("target_lat", 0);
        targetLon = prefs().getFloat("target_lon", 0);
        hasTargetPoint = true;
    }

    private void updateCompassAndTargetUi() {
        if (routeView != null && hasTargetPoint) routeView.setTargetPoint(targetLat, targetLon);
        double targetBearing = 0;
        if (hasTargetPoint && hasCurrentLocation) {
            targetBearing = bearingDegrees(currentLat, currentLon, targetLat, targetLon);
        }
        if (targetCompassView != null) {
            targetCompassView.setVisibility(hasTargetPoint ? View.VISIBLE : View.INVISIBLE);
            targetCompassView.setAlpha(hasTargetPoint ? 1f : 0f);
            targetCompassView.setRotation((float) targetBearing);
            targetCompassView.bringToFront();
        }
        if (compassNeedleView != null) {
            compassNeedleView.setRotation(hasCourseBearing ? (float) courseBearing : 0f);
        }
        if (targetInfoText != null) {
            if (hasTargetPoint && hasCurrentLocation) {
                double meters = distanceMeters(currentLat, currentLon, targetLat, targetLon);
                targetInfoText.setText("Cel: " + formatDistanceMeters(meters) + "\nw linii prostej");
            } else if (hasTargetPoint) {
                targetInfoText.setText("Cel ustawiony\nczekam na GPS");
            } else {
                targetInfoText.setText("Przytrzymaj mapę\naby ustawić cel");
            }
        }
        updatePoiInfoUi();
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000.0;
        double f1 = Math.toRadians(lat1);
        double f2 = Math.toRadians(lat2);
        double df = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(df / 2) * Math.sin(df / 2)
                + Math.cos(f1) * Math.cos(f2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double f1 = Math.toRadians(lat1);
        double f2 = Math.toRadians(lat2);
        double dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(f2);
        double x = Math.cos(f1) * Math.sin(f2) - Math.sin(f1) * Math.cos(f2) * Math.cos(dl);
        double brng = Math.toDegrees(Math.atan2(y, x));
        return (brng + 360.0) % 360.0;
    }

    private String formatDistanceMeters(double meters) {
        if (meters < 1000) return String.format(Locale.US, "%.0f m", meters);
        return String.format(Locale.US, "%.2f km", meters / 1000.0);
    }

    private String compactFirstLine(TextView t) {
        if (t == null || t.getText() == null) return "--";
        String s = t.getText().toString();
        int idx = s.indexOf('\n');
        return idx >= 0 ? s.substring(0, idx) + " km" : s;
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(6));

        TextView version = text("Wersja: " + VERSION_NAME + "\nTryb jazdy: " + selectedMode, 16, NAVY, true);
        box.addView(version, new LinearLayout.LayoutParams(-1, dp(64)));

        CheckBox autoUpdate = new CheckBox(this);
        autoUpdate.setText("Sprawdzaj aktualizacje przy starcie");
        autoUpdate.setTextSize(15);
        autoUpdate.setTextColor(TEXT);
        autoUpdate.setChecked(prefs().getBoolean("auto_update_check", false));
        autoUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> prefs().edit().putBoolean("auto_update_check", isChecked).apply());
        box.addView(autoUpdate, new LinearLayout.LayoutParams(-1, dp(46)));

        box.addView(settingsButton("Zmień tryb jazdy", GREEN, v -> chooseModeDialog()));
        box.addView(settingsButton("Sprawdź aktualizację", GREEN, v -> checkForUpdates(true)));
        box.addView(settingsButton("Zezwolenia i ustawienia aplikacji", BLUE, v -> openAppSettings()));
        box.addView(settingsButton("Ustawienia baterii Android", Color.rgb(168, 112, 35), v -> openBatterySettings()));
        box.addView(settingsButton("Wyczyść historię jazdy", RED, v -> confirmClearHistory()));

        new AlertDialog.Builder(this)
                .setTitle("Opcje")
                .setView(box)
                .setNegativeButton("Zamknij", null)
                .show();
    }

    private TextView settingsButton(String label, int color, View.OnClickListener listener) {
        TextView b = text(label, 15, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(color, 14, color, 0));
        b.setOnClickListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(46));
        lp.setMargins(0, dp(7), 0, 0);
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
                    if (currentTab == 0) renderRide();
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
                            .setMessage("Znaleziono wersję: " + finalTag)
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
            return major * 10000 + minor * 100 + patch;
        } catch (Exception ignored) { return -1; }
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(this, "Nie można otworzyć linku.", Toast.LENGTH_SHORT).show(); }
    }

    private LinearLayout actionBtn(String icon, String label, int color, View.OnClickListener listener) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.HORIZONTAL);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(gradient(color, Color.rgb(0, 126, 89), 14));
        b.setElevation(dp(3));
        b.setOnClickListener(listener);
        TextView i = text(icon, 26, Color.WHITE, true);
        i.setGravity(Gravity.CENTER);
        b.addView(i, new LinearLayout.LayoutParams(dp(46), -1));
        TextView l = text(label, 16, Color.WHITE, true);
        l.setGravity(Gravity.CENTER_VERTICAL);
        b.addView(l, new LinearLayout.LayoutParams(-2, -1));
        return b;
    }

    private TextView squareAction(String label, int color, View.OnClickListener listener) {
        TextView b = text(label, 24, color, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(Color.WHITE, 14, BORDER, 1));
        b.setElevation(dp(2));
        b.setOnClickListener(listener);
        return b;
    }

    private TextView navItem(String label, boolean active) {
        TextView t = text(label, 12, active ? GREEN_DARK : MUTED, true);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private String modeEmoji() {
        return "Samochód".equals(selectedMode) ? "🚗" : "🚲";
    }

    private void updateModeVisuals() {
        String icon = modeEmoji();
        if (headerModeIcon != null) headerModeIcon.setText(icon);
        if (speedHeroWatermark != null) speedHeroWatermark.setText(icon);
        if (navRide != null) navRide.setText(icon + "\nJazda");
    }

    private void updateNav() {
        if (navRide == null) return;
        navRide.setTextColor(currentTab == 0 ? GREEN_DARK : MUTED);
        navHistory.setTextColor(currentTab == 1 ? GREEN_DARK : MUTED);
        navProgress.setTextColor(currentTab == 2 ? GREEN_DARK : MUTED);
        navProfile.setTextColor(currentTab == 3 ? GREEN_DARK : MUTED);
    }

    private String formatCalories(double distanceKm) {
        if (distanceKm <= 0.01) return "--";
        if ("Samochód".equals(selectedMode)) return "--";
        double kcalPerKm = 28.0;
        return Math.round(distanceKm * kcalPerKm) + " kcal";
    }

    private String formatPace(double avgKmh) {
        if (avgKmh <= 0.2) return "--";
        double minPerKm = 60.0 / avgKmh;
        int min = (int) Math.floor(minPerKm);
        int sec = (int) Math.round((minPerKm - min) * 60.0);
        if (sec >= 60) { min++; sec -= 60; }
        return String.format(Locale.US, "%d:%02d", min, sec);
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(round(CARD_BG, 18, BORDER, 1));
        c.setElevation(dp(2));
        return c;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private TextView pill(String s, int fg, int bg, int sp, boolean bold) {
        TextView t = text(s, sp, fg, bold);
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(bg, 20, Color.TRANSPARENT, 0));
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
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{startColor, endColor});
        g.setCornerRadius(dp(radius));
        return g;
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
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec);
    }
}
