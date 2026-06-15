package pl.tomalawsb.licznik;
public class MainActivity extends android.app.Activity implements android.hardware.SensorEventListener {
    public static final String CURRENT_RELEASE_TAG = "v2.9-1406262155";
    public static final int CURRENT_VERSION_CODE = 20900;
    private static final String GITHUB_API_LATEST = "https://api.github.com/repos/tomalawsb/Licznik/releases/latest";
    private static final int REQ_PERMISSIONS = 1001;
    public static final String VERSION_NAME = "2.9 - 1406262155";
    private final int BG;
    private final int BLUE;
    private final int BORDER;
    private final int CARD_BG;
    private final int GREEN;
    private final int GREEN_DARK;
    private final int GREEN_LIGHT;
    private final int MUTED;
    private final int NAVY;
    private final int RED;
    private final int TEXT;
    private android.hardware.Sensor accelerometerSensor;
    private android.widget.TextView caloriesText;
    private final Runnable clockUiTicker;
    private float compassDisplayRotation;
    private android.widget.ImageView compassView;
    private android.widget.LinearLayout contentBox;
    private int currentTab;
    private android.widget.TextView distanceText;
    private long elapsedBaseMs;
    private long elapsedSyncRealtime;
    private android.widget.TextView elevationText;
    private boolean gravityReady;
    private final float[] gravityValues;
    private android.widget.TextView headerModeIcon;
    private long lastElapsedMs;
    private String lastPointsJson;
    private android.hardware.Sensor magneticFieldSensor;
    private boolean magneticReady;
    private final float[] magneticValues;
    private android.widget.TextView navHistory;
    private android.widget.TextView navProfile;
    private android.widget.TextView navProgress;
    private android.widget.TextView navRide;
    private android.widget.TextView paceText;
    private boolean paused;
    private android.widget.LinearLayout primaryActionButton;
    private android.widget.TextView primaryActionIcon;
    private android.widget.TextView primaryActionLabel;
    private android.widget.LinearLayout rideActionHost;
    private android.hardware.Sensor rotationVectorSensor;
    private pl.tomalawsb.licznik.RouteMapView routeView;
    private boolean running;
    private String selectedMode;
    private android.hardware.SensorManager sensorManager;
    private android.widget.TextView speedHeroWatermark;
    private android.widget.TextView speedSummaryText;
    private android.widget.TextView speedValueText;
    private android.widget.TextView statusText;
    private android.widget.TextView timeText;
    private final android.os.Handler uiHandler;
    private final android.content.BroadcastReceiver updateReceiver;
    private boolean waitingForStopResult;

    public MainActivity()
    {
        this.GREEN = android.graphics.Color.rgb(16, 163, 99);
        this.GREEN_LIGHT = android.graphics.Color.rgb(235, 247, 226);
        this.GREEN_DARK = android.graphics.Color.rgb(5, 132, 83);
        this.BLUE = android.graphics.Color.rgb(32, 112, 210);
        this.RED = android.graphics.Color.rgb(220, 55, 55);
        this.NAVY = android.graphics.Color.rgb(28, 31, 40);
        this.TEXT = android.graphics.Color.rgb(45, 47, 52);
        this.MUTED = android.graphics.Color.rgb(125, 125, 125);
        this.BORDER = android.graphics.Color.rgb(222, 218, 210);
        this.BG = android.graphics.Color.rgb(250, 248, 243);
        this.CARD_BG = android.graphics.Color.rgb(255, 254, 250);
        this.selectedMode = "Rower";
        this.running = 0;
        this.paused = 0;
        this.currentTab = 0;
        this.lastElapsedMs = 0;
        this.elapsedBaseMs = 0;
        this.elapsedSyncRealtime = 0;
        this.lastPointsJson = "[]";
        this.waitingForStopResult = 0;
        float[] v2_4 = new float[3];
        this.gravityValues = v2_4;
        android.os.Looper v1_12 = new float[3];
        this.magneticValues = v1_12;
        this.gravityReady = 0;
        this.magneticReady = 0;
        this.compassDisplayRotation = 0;
        this.uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        this.clockUiTicker = new pl.tomalawsb.licznik.MainActivity$1(this);
        this.updateReceiver = new pl.tomalawsb.licznik.MainActivity$2(this);
        return;
    }

    static synthetic long access$000(pl.tomalawsb.licznik.MainActivity p2)
    {
        return p2.lastElapsedMs;
    }

    static synthetic long access$002(pl.tomalawsb.licznik.MainActivity p0, long p1)
    {
        p0.lastElapsedMs = p1;
        return p1;
    }

    static synthetic boolean access$100(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.running;
    }

    static synthetic String access$1002(pl.tomalawsb.licznik.MainActivity p0, String p1)
    {
        p0.lastPointsJson = p1;
        return p1;
    }

    static synthetic boolean access$102(pl.tomalawsb.licznik.MainActivity p0, boolean p1)
    {
        p0.running = p1;
        return p1;
    }

    static synthetic boolean access$1100(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.waitingForStopResult;
    }

    static synthetic boolean access$1102(pl.tomalawsb.licznik.MainActivity p0, boolean p1)
    {
        p0.waitingForStopResult = p1;
        return p1;
    }

    static synthetic int access$1200(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.currentTab;
    }

    static synthetic void access$1300(pl.tomalawsb.licznik.MainActivity p0)
    {
        p0.renderRide();
        return;
    }

    static synthetic void access$1400(pl.tomalawsb.licznik.MainActivity p0)
    {
        p0.renderHistory();
        return;
    }

    static synthetic android.widget.TextView access$1500(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.speedValueText;
    }

    static synthetic android.widget.TextView access$1600(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.speedSummaryText;
    }

    static synthetic android.widget.TextView access$1700(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.distanceText;
    }

    static synthetic android.widget.TextView access$1800(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.caloriesText;
    }

    static synthetic String access$1900(pl.tomalawsb.licznik.MainActivity p0, double p1)
    {
        return p0.formatCalories(p1);
    }

    static synthetic boolean access$200(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.paused;
    }

    static synthetic android.widget.TextView access$2000(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.paceText;
    }

    static synthetic boolean access$202(pl.tomalawsb.licznik.MainActivity p0, boolean p1)
    {
        p0.paused = p1;
        return p1;
    }

    static synthetic String access$2100(pl.tomalawsb.licznik.MainActivity p0, double p1)
    {
        return p0.formatPace(p1);
    }

    static synthetic android.widget.TextView access$2200(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.elevationText;
    }

    static synthetic pl.tomalawsb.licznik.RouteMapView access$2300(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.routeView;
    }

    static synthetic void access$2400(pl.tomalawsb.licznik.MainActivity p0, double p1)
    {
        p0.updateStatus(p1);
        return;
    }

    static synthetic void access$2500(pl.tomalawsb.licznik.MainActivity p0)
    {
        p0.updatePrimaryButton();
        return;
    }

    static synthetic long access$300(pl.tomalawsb.licznik.MainActivity p2)
    {
        return p2.elapsedBaseMs;
    }

    static synthetic long access$302(pl.tomalawsb.licznik.MainActivity p0, long p1)
    {
        p0.elapsedBaseMs = p1;
        return p1;
    }

    static synthetic long access$400(pl.tomalawsb.licznik.MainActivity p2)
    {
        return p2.elapsedSyncRealtime;
    }

    static synthetic long access$402(pl.tomalawsb.licznik.MainActivity p0, long p1)
    {
        p0.elapsedSyncRealtime = p1;
        return p1;
    }

    static synthetic android.widget.TextView access$500(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.timeText;
    }

    static synthetic android.os.Handler access$600(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.uiHandler;
    }

    static synthetic String access$700(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.selectedMode;
    }

    static synthetic String access$702(pl.tomalawsb.licznik.MainActivity p0, String p1)
    {
        p0.selectedMode = p1;
        return p1;
    }

    static synthetic android.content.SharedPreferences access$800(pl.tomalawsb.licznik.MainActivity p0)
    {
        return p0.prefs();
    }

    static synthetic void access$900(pl.tomalawsb.licznik.MainActivity p0)
    {
        p0.updateModeVisuals();
        return;
    }

    private android.widget.LinearLayout actionBtn(String p7, String p8, int p9, android.view.View$OnClickListener p10)
    {
        android.widget.LinearLayout v0_1 = new android.widget.LinearLayout(this);
        v0_1.setOrientation(0);
        v0_1.setGravity(17);
        v0_1.setPadding(this.dp(10), 0, this.dp(16), 0);
        v0_1.setBackground(this.gradient(p9, android.graphics.Color.rgb(0, 126, 89), 14));
        v0_1.setElevation(((float) this.dp(3)));
        v0_1.setOnClickListener(p10);
        android.widget.TextView v7_1 = this.text(p7, 36, -1, 1);
        v7_1.setGravity(17);
        v0_1.addView(v7_1, new android.widget.LinearLayout$LayoutParams(this.dp(58), -1));
        android.widget.TextView v7_2 = this.text(p8, 17, -1, 1);
        v7_2.setGravity(16);
        v0_1.addView(v7_2, new android.widget.LinearLayout$LayoutParams(-2, -1));
        return v0_1;
    }

    private void addHistoryCard(org.json.JSONObject p21)
    {
        android.widget.LinearLayout v1_15;
        android.widget.LinearLayout v8_1 = new android.widget.LinearLayout(this);
        v8_1.setOrientation(1);
        v8_1.setPadding(this.dp(14), this.dp(12), this.dp(14), this.dp(12));
        v8_1.setBackground(this.round(this.CARD_BG, 18, this.BORDER, 1));
        v8_1.setElevation(((float) this.dp(2)));
        int v11_4 = new android.widget.LinearLayout(this);
        v11_4.setGravity(16);
        String v12 = p21.optString("mode", "Rower");
        if (!v12.equals("Samoch\u00f3d")) {
            v1_15 = "\u1f6b2";
        } else {
            v1_15 = "\u1f697";
        }
        android.widget.LinearLayout$LayoutParams v0_21;
        if (!v12.equals("Samoch\u00f3d")) {
            v0_21 = this.GREEN;
        } else {
            v0_21 = this.BLUE;
        }
        v11_4.addView(this.circleText(v1_15, 44, v0_21, -1, 20));
        android.widget.LinearLayout$LayoutParams v0_25 = new android.widget.LinearLayout(this);
        v0_25.setOrientation(1);
        v0_25.setPadding(this.dp(12), 0, 0, 0);
        v0_25.addView(this.text(v12, 15, this.TEXT, 1), new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        v0_25.addView(this.text(p21.optString("date", ""), 12, pl.tomalawsb.licznik.MainActivity v6.MUTED, 0), new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        v11_4.addView(v0_25, new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216));
        android.widget.LinearLayout$LayoutParams v0_29 = this.text(String.format(java.util.Locale.US, "%.2f km  \u203a", new Object[] {Double.valueOf(p21.optDouble("distanceKm", 0))})), 17, v6.NAVY, 1);
        v0_29.setGravity(21);
        v11_4.addView(v0_29, new android.widget.LinearLayout$LayoutParams(this.dp(112), -1));
        v8_1.addView(v11_4, new android.widget.LinearLayout$LayoutParams(-1, this.dp(48)));
        android.widget.LinearLayout$LayoutParams v0_5 = this.text(String.format(java.util.Locale.US, "\u23f1 %s   \u2022   \u015br. %.3f km/h   \u2022   maks. %.1f km/h", new Object[] {p21.optString("elapsed", "00:00:00"), Double.valueOf(p21.optDouble("avg", 0)), Double.valueOf(p21.optDouble("max", 0))})), 10, v6.TEXT, 1);
        v0_5.setSingleLine(1);
        v8_1.addView(v0_5, new android.widget.LinearLayout$LayoutParams(-1, this.dp(28)));
        android.widget.LinearLayout$LayoutParams v0_7 = p21.optString("pointsJson", "[]");
        android.widget.LinearLayout v1_2 = String.format(java.util.Locale.US, "%s  \u2022  %.2f km  \u2022  %s  \u2022  \u015br. %.3f km/h  \u2022  maks. %.1f km/h", new Object[] {v12, Double.valueOf(p21.optDouble("distanceKm", 0)), p21.optString("elapsed", "00:00:00"), Double.valueOf(p21.optDouble("avg", 0)), Double.valueOf(p21.optDouble("max", 0))}));
        int v2_9 = new pl.tomalawsb.licznik.RouteMapView(this);
        v2_9.setPointsFromJson(v0_7);
        v2_9.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda23(this, v0_7, v1_2));
        android.widget.LinearLayout$LayoutParams v0_10 = new android.widget.LinearLayout$LayoutParams(-1, this.dp(104));
        v0_10.topMargin = this.dp(8);
        v8_1.addView(v2_9, v0_10);
        android.widget.LinearLayout$LayoutParams v0_12 = new android.widget.LinearLayout$LayoutParams(-1, this.dp(212));
        v0_12.setMargins(0, 0, 0, this.dp(12));
        this.contentBox.addView(v8_1, v0_12);
        return;
    }

    private void buildActionRow()
    {
        android.widget.LinearLayout v0_0 = this.rideActionHost;
        if (v0_0 != null) {
            v0_0.removeAllViews();
            android.widget.LinearLayout v0_2 = new android.widget.LinearLayout(this);
            v0_2.setGravity(17);
            v0_2.setGravity(17);
            this.rideActionHost.addView(v0_2, new android.widget.LinearLayout$LayoutParams(-1, -1));
            android.widget.TextView v1_1 = this.actionBtn("\u25b6", "Rozpocznij jazd\u0119", this.GREEN, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda26(this));
            this.primaryActionButton = v1_1;
            this.primaryActionIcon = ((android.widget.TextView) v1_1.getChildAt(0));
            this.primaryActionLabel = ((android.widget.TextView) this.primaryActionButton.getChildAt(1));
            android.widget.TextView v1_8 = new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216);
            v1_8.setMargins(0, 0, this.dp(8), 0);
            v0_2.addView(this.primaryActionButton, v1_8);
            android.widget.TextView v1_10 = this.squareAction("\u25a0", this.RED, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda27(this));
            android.widget.LinearLayout$LayoutParams v5_6 = new android.widget.LinearLayout$LayoutParams(this.dp(72), -1);
            v5_6.setMargins(0, 0, this.dp(8), 0);
            v0_2.addView(v1_10, v5_6);
            v0_2.addView(this.squareAction("\u21bb", this.TEXT, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda28(this)), new android.widget.LinearLayout$LayoutParams(this.dp(72), -1));
            return;
        } else {
            return;
        }
    }

    private android.widget.TextView buildBigStatCard(android.widget.LinearLayout p8, String p9, String p10, int p11)
    {
        android.widget.LinearLayout v0_1 = new android.widget.LinearLayout(this);
        v0_1.setOrientation(1);
        v0_1.setGravity(17);
        v0_1.setPadding(this.dp(8), this.dp(8), this.dp(8), this.dp(8));
        v0_1.setBackground(this.round(this.CARD_BG, 16, this.BORDER, 1));
        v0_1.setElevation(((float) this.dp(2)));
        android.widget.TextView v9_1 = this.text(p9, 20, p11, 1);
        v9_1.setGravity(17);
        v0_1.addView(v9_1, new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        android.widget.TextView v9_3 = this.text(p10, 17, this.TEXT, 1);
        v9_3.setGravity(17);
        v0_1.addView(v9_3, new android.widget.LinearLayout$LayoutParams(-1, 0, 1073741824));
        android.widget.LinearLayout$LayoutParams v10_4 = new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216);
        v10_4.setMargins(this.dp(5), 0, this.dp(5), 0);
        p8.addView(v0_1, v10_4);
        return v9_3;
    }

    private void buildDistanceTimeRow()
    {
        android.widget.TextView v0_1 = new android.widget.LinearLayout(this);
        v0_1.setGravity(17);
        String v1_5 = new android.widget.LinearLayout$LayoutParams(-1, this.dp(100));
        v1_5.setMargins(0, 0, 0, this.dp(10));
        this.contentBox.addView(v0_1, v1_5);
        this.distanceText = this.buildBigStatCard(v0_1, "\u2318", "0.00\nkm dystansu", this.GREEN_DARK);
        this.timeText = this.buildBigStatCard(v0_1, "\u25f7", "00:00:00\nczas jazdy", this.BLUE);
        return;
    }

    private void buildGpsCard()
    {
        android.widget.LinearLayout v0_1 = new android.widget.LinearLayout(this);
        v0_1.setOrientation(1);
        v0_1.setBackground(this.round(this.CARD_BG, 18, this.BORDER, 1));
        v0_1.setElevation(((float) this.dp(2)));
        android.widget.LinearLayout$LayoutParams v1_6 = new android.widget.FrameLayout(this);
        android.widget.LinearLayout v2_8 = new pl.tomalawsb.licznik.RouteMapView(this);
        this.routeView = v2_8;
        v2_8.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda9(this));
        v1_6.addView(this.routeView, new android.widget.FrameLayout$LayoutParams(-1, -1));
        this.statusText = this.pill("\u25cf  GPS gotowy", -1, this.GREEN, 12, 1);
        android.widget.LinearLayout v2_13 = new android.widget.FrameLayout$LayoutParams(this.dp(120), this.dp(30));
        v2_13.leftMargin = this.dp(12);
        v2_13.topMargin = this.dp(10);
        v1_6.addView(this.statusText, v2_13);
        v0_1.addView(v1_6, new android.widget.LinearLayout$LayoutParams(-1, this.dp(300)));
        android.widget.LinearLayout$LayoutParams v1_9 = new android.widget.LinearLayout(this);
        v1_9.setGravity(17);
        v1_9.setPadding(this.dp(6), this.dp(8), this.dp(6), this.dp(8));
        v1_9.setBackgroundColor(-1);
        android.widget.LinearLayout v7_2 = v1_9;
        this.elevationText = this.buildMiniMetric(v7_2, "\u25b2", "Wzniesienie", "--", android.graphics.Color.rgb(72, 142, 55));
        this.caloriesText = this.buildMiniMetric(v7_2, "\u2668", "Kalorie", "--", android.graphics.Color.rgb(58, 110, 50));
        this.paceText = this.buildMiniMetric(v7_2, "\u25f4", "Tempo", "--", android.graphics.Color.rgb(58, 110, 50));
        v0_1.addView(v1_9, new android.widget.LinearLayout$LayoutParams(-1, this.dp(66)));
        android.widget.LinearLayout$LayoutParams v1_3 = new android.widget.LinearLayout$LayoutParams(-1, this.dp(366));
        v1_3.setMargins(0, 0, 0, this.dp(10));
        this.contentBox.addView(v0_1, v1_3);
        return;
    }

    private android.widget.TextView buildMiniMetric(android.widget.LinearLayout p8, String p9, String p10, String p11, int p12)
    {
        android.widget.LinearLayout v0_1 = new android.widget.LinearLayout(this);
        v0_1.setOrientation(1);
        v0_1.setGravity(17);
        android.widget.TextView v9_3 = this.text(p9, 14, p12, 1);
        v9_3.setGravity(17);
        v0_1.addView(v9_3, new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        android.widget.TextView v9_2 = this.text(p11, 14, this.TEXT, 1);
        v9_2.setGravity(17);
        v0_1.addView(v9_2, new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        android.widget.LinearLayout$LayoutParams v10_1 = this.text(p10, 11, this.MUTED, 0);
        v10_1.setGravity(17);
        v0_1.addView(v10_1, new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        p8.addView(v0_1, new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216));
        return v9_2;
    }

    private void buildRecentRides()
    {
        int v0_1 = new android.widget.LinearLayout(this);
        v0_1.setGravity(16);
        int v5 = 1;
        int v2_16 = this.text("OSTATNIE JAZDY", 12, this.MUTED, 1);
        v2_16.setLetterSpacing(1034147594);
        v0_1.addView(v2_16, new android.widget.LinearLayout$LayoutParams(-2, this.dp(32)));
        int v2_4 = this.text("", 1, this.BORDER, 0);
        v2_4.setBackgroundColor(this.BORDER);
        int v3_10 = new android.widget.LinearLayout$LayoutParams(0, this.dp(1), 1065353216);
        v3_10.leftMargin = this.dp(12);
        v0_1.addView(v2_4, v3_10);
        this.contentBox.addView(v0_1, new android.widget.LinearLayout$LayoutParams(-1, this.dp(34)));
        try {
            int v0_5 = new org.json.JSONArray(this.prefs().getString("history", "[]"));
        } catch (Exception) {
            return;
        }
        if (v0_5.length() != 0) {
            android.widget.LinearLayout v1_3 = new android.widget.LinearLayout(this);
            v1_3.setOrientation(1);
            v1_3.setBackground(this.round(this.CARD_BG, 18, this.BORDER, 1));
            v1_3.setElevation(((float) this.dp(2)));
            int v3_1 = (v0_5.length() - 1);
            int v4_0 = 0;
            while ((v3_1 >= 0) && (v4_0 < 2)) {
                v1_3.addView(this.recentRideRow(v0_5.getJSONObject(v3_1)), new android.widget.LinearLayout$LayoutParams(-1, this.dp(74)));
                if ((v4_0 == 0) && (v0_5.length() > 1)) {
                    android.widget.TextView v8_2 = new android.widget.TextView(this);
                    v8_2.setBackgroundColor(android.graphics.Color.rgb(235, 231, 223));
                    v1_3.addView(v8_2, new android.widget.LinearLayout$LayoutParams(-1, this.dp(1)));
                }
                v3_1--;
                v4_0++;
            }
            int v3_19 = this.contentBox;
            int v2_13 = Math.min(2, v0_5.length());
            if (v0_5.length() <= 1) {
                v5 = 0;
            } else {
            }
            v3_19.addView(v1_3, new android.widget.LinearLayout$LayoutParams(-1, this.dp(((v2_13 * 74) + v5))));
            return;
        } else {
            int v0_3 = this.text("Brak zapisanych jazd. Po zako\u0144czeniu trasy pojawi\u0105 si\u0119 tutaj ostatnie wyniki.", 13, this.MUTED, 0);
            v0_3.setGravity(16);
            this.contentBox.addView(v0_3, new android.widget.LinearLayout$LayoutParams(-1, this.dp(58)));
            return;
        }
    }

    private void buildSpeedHero()
    {
        android.widget.FrameLayout v0_1 = new android.widget.FrameLayout(this);
        v0_1.setBackground(this.gradient(this.GREEN, android.graphics.Color.rgb(0, 126, 89), 20));
        v0_1.setPadding(this.dp(18), this.dp(9), this.dp(12), this.dp(9));
        v0_1.setElevation(((float) this.dp(2)));
        this.speedHeroWatermark = 0;
        android.widget.LinearLayout$LayoutParams v1_11 = new android.widget.ImageView(this);
        this.compassView = v1_11;
        v1_11.setImageResource(pl.tomalawsb.licznik.R$drawable.compass_north);
        this.compassView.setScaleType(android.widget.ImageView$ScaleType.FIT_CENTER);
        this.compassView.setContentDescription("Kompas wskazuj\u0105cy p\u00f3\u0142noc");
        this.compassView.setRotation(this.compassDisplayRotation);
        android.widget.LinearLayout$LayoutParams v1_16 = new android.widget.FrameLayout$LayoutParams(this.dp(92), this.dp(92), 53);
        v1_16.topMargin = this.dp(8);
        v1_16.rightMargin = this.dp(4);
        v0_1.addView(this.compassView, v1_16);
        android.widget.LinearLayout$LayoutParams v1_18 = new android.widget.LinearLayout(this);
        v1_18.setOrientation(1);
        v1_18.setGravity(3);
        v1_18.setPadding(0, 0, this.dp(92), 0);
        v0_1.addView(v1_18, new android.widget.FrameLayout$LayoutParams(-1, -1));
        v1_18.addView(this.text("\u25f4  AKTUALNA PR\u0118DKO\u015a\u0106", 13, android.graphics.Color.rgb(215, 250, 229), 1), new android.widget.LinearLayout$LayoutParams(-1, this.dp(24)));
        android.widget.LinearLayout v2_25 = new android.widget.LinearLayout(this);
        v2_25.setGravity(19);
        android.widget.LinearLayout$LayoutParams v10_10 = this.text("0.0", 54, -1, 1);
        this.speedValueText = v10_10;
        v10_10.setGravity(19);
        v2_25.addView(this.speedValueText, new android.widget.LinearLayout$LayoutParams(-2, this.dp(66)));
        android.widget.LinearLayout$LayoutParams v10_3 = this.text(" km/h", 19, android.graphics.Color.rgb(210, 242, 224), 1);
        v10_3.setGravity(19);
        v2_25.addView(v10_3, new android.widget.LinearLayout$LayoutParams(-2, this.dp(66)));
        v1_18.addView(v2_25, new android.widget.LinearLayout$LayoutParams(-1, this.dp(66)));
        android.widget.LinearLayout v2_2 = this.text("\u015arednia: 0.000 km/h  \u2022  Maks: 0.0 km/h", 15, android.graphics.Color.rgb(229, 250, 237), 1);
        this.speedSummaryText = v2_2;
        v2_2.setSingleLine(1);
        this.speedSummaryText.setGravity(19);
        v1_18.addView(this.speedSummaryText, new android.widget.LinearLayout$LayoutParams(-1, this.dp(38)));
        android.widget.LinearLayout$LayoutParams v1_4 = new android.widget.LinearLayout$LayoutParams(-1, this.dp(148));
        v1_4.setMargins(0, this.dp(4), 0, this.dp(10));
        this.contentBox.addView(v0_1, v1_4);
        return;
    }

    private void buildUi()
    {
        pl.tomalawsb.licznik.MainActivity v6 = this;
        android.widget.LinearLayout v7_1 = new android.widget.LinearLayout(this);
        v7_1.setOrientation(1);
        v7_1.setBackgroundColor(this.BG);
        this.setContentView(v7_1);
        int v9_2 = new android.widget.LinearLayout(this);
        v9_2.setOrientation(0);
        v9_2.setGravity(16);
        v9_2.setPadding(this.dp(18), this.dp(10), this.dp(18), this.dp(5));
        v7_1.addView(v9_2, new android.widget.LinearLayout$LayoutParams(-1, this.dp(84)));
        android.widget.TextView v0_24 = this.circleText(this.modeEmoji(), 46, android.graphics.Color.rgb(232, 247, 235), this.GREEN_DARK, 21);
        this.headerModeIcon = v0_24;
        v0_24.setElevation(((float) this.dp(2)));
        this.headerModeIcon.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda13(this));
        v9_2.addView(this.headerModeIcon);
        android.widget.TextView v0_29 = new android.widget.LinearLayout(this);
        v0_29.setOrientation(1);
        v0_29.setPadding(this.dp(14), 0, 0, 0);
        v9_2.addView(v0_29, new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216));
        v0_29.addView(this.text("Dzie\u0144 dobry!", 13, this.MUTED, 0), new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda19 v1_59 = this.text("Licznik jazdy", 22, v6.NAVY, 1);
        v1_59.setSingleLine(1);
        v0_29.addView(v1_59, new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        android.widget.TextView v0_31 = this.circleText("\u1f514", 42, -1, this.NAVY, 18);
        v0_31.setElevation(((float) this.dp(2)));
        v0_31.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda14(this));
        v9_2.addView(v0_31, new android.widget.LinearLayout$LayoutParams(this.dp(42), this.dp(42)));
        android.widget.TextView v0_1 = this.circleText("\u2699", 42, -1, this.NAVY, 21);
        v0_1.setElevation(((float) this.dp(2)));
        v0_1.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda15(this));
        pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda19 v1_8 = new android.widget.LinearLayout$LayoutParams(this.dp(42), this.dp(42));
        v1_8.leftMargin = this.dp(10);
        v9_2.addView(v0_1, v1_8);
        android.widget.TextView v0_3 = new android.widget.LinearLayout(this);
        this.contentBox = v0_3;
        v0_3.setOrientation(1);
        android.widget.TextView v0_6 = new android.widget.ScrollView(this);
        v0_6.setFillViewport(0);
        v0_6.addView(this.contentBox);
        v7_1.addView(v0_6, new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        android.widget.TextView v0_8 = new android.widget.LinearLayout(this);
        this.rideActionHost = v0_8;
        v0_8.setOrientation(1);
        this.rideActionHost.setGravity(17);
        this.rideActionHost.setPadding(this.dp(16), this.dp(4), this.dp(16), this.dp(8));
        this.rideActionHost.setBackgroundColor(this.BG);
        v7_1.addView(this.rideActionHost, new android.widget.LinearLayout$LayoutParams(-1, this.dp(86)));
        android.widget.TextView v0_14 = new android.widget.LinearLayout(this);
        v0_14.setGravity(17);
        v0_14.setPadding(this.dp(10), this.dp(4), this.dp(10), this.dp(8));
        v0_14.setBackground(this.round(-1, 28, 0, 0));
        v0_14.setElevation(((float) this.dp(4)));
        v7_1.addView(v0_14, new android.widget.LinearLayout$LayoutParams(-1, this.dp(70)));
        v6.navRide = v6.navItem(new StringBuilder().append(this.modeEmoji()).append("\nJazda").toString(), 1);
        this.navHistory = this.navItem("\u21ba\nHistoria", 0);
        this.navProgress = this.navItem("\u2301\nPost\u0119py", 0);
        this.navProfile = this.navItem("\u2659\nProfil", 0);
        v0_14.addView(this.navRide, new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216));
        v0_14.addView(this.navHistory, new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216));
        v0_14.addView(this.navProgress, new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216));
        v0_14.addView(this.navProfile, new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216));
        this.navRide.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda16(this));
        this.navHistory.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda17(this));
        this.navProgress.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda18(this));
        this.navProfile.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda19(this));
        return;
    }

    private android.widget.LinearLayout card()
    {
        android.widget.LinearLayout v0_1 = new android.widget.LinearLayout(this);
        v0_1.setOrientation(1);
        v0_1.setBackground(this.round(this.CARD_BG, 18, this.BORDER, 1));
        v0_1.setElevation(((float) this.dp(2)));
        return v0_1;
    }

    private void checkForUpdates(boolean p3)
    {
        android.widget.Toast.makeText(this, "Sprawdzam aktualizacj\u0119...", 0).show();
        new Thread(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda10(this, p3)).start();
        return;
    }

    private void chooseModeDialog()
    {
        if (!this.running) {
            android.app.AlertDialog$Builder v0_6 = new String[2];
            v0_6[0] = "Rower";
            v0_6[1] = "Samoch\u00f3d";
            new android.app.AlertDialog$Builder(this).setTitle("Tryb jazdy").setSingleChoiceItems(v0_6, "Samoch\u00f3d".equals(this.selectedMode), new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda21(this, v0_6)).setNegativeButton("Anuluj", 0).show();
            return;
        } else {
            android.widget.Toast.makeText(this, "Tryb mo\u017cna zmieni\u0107 przed startem jazdy.", 0).show();
            return;
        }
    }

    private android.widget.TextView circleText(String p2, int p3, int p4, int p5, int p6)
    {
        android.widget.TextView v2_1 = this.text(p2, p6, p5, 1);
        v2_1.setGravity(17);
        v2_1.setBackground(this.round(p4, (p3 / 2), 0, 0));
        v2_1.setLayoutParams(new android.widget.LinearLayout$LayoutParams(this.dp(p3), this.dp(p3)));
        return v2_1;
    }

    private String compactFirstLine(android.widget.TextView p4)
    {
        if ((p4 != null) && (p4.getText() != null)) {
            String v4_4 = p4.getText().toString();
            String v0_0 = v4_4.indexOf(10);
            if (v0_0 >= null) {
                v4_4 = new StringBuilder().append(v4_4.substring(0, v0_0)).append(" km").toString();
            }
            return v4_4;
        } else {
            return "--";
        }
    }

    private void confirmClearHistory()
    {
        new android.app.AlertDialog$Builder(this).setTitle("Wyczy\u015bci\u0107 histori\u0119?").setMessage("Usunie to zapisane jazdy z telefonu.").setPositiveButton("Usu\u0144", new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda0(this)).setNegativeButton("Anuluj", 0).show();
        return;
    }

    private int dp(int p2)
    {
        return ((int) ((((float) p2) * this.getResources().getDisplayMetrics().density) + 1056964608));
    }

    private void enterImmersiveMode()
    {
        android.view.View v0_0 = this.getWindow();
        if (android.os.Build$VERSION.SDK_INT < 30) {
            v0_0.getDecorView().setSystemUiVisibility(5894);
        } else {
            android.view.View v0_1 = v0_0.getInsetsController();
            if (v0_1 != null) {
                v0_1.hide((android.view.WindowInsets$Type.statusBars() | android.view.WindowInsets$Type.navigationBars()));
                v0_1.setSystemBarsBehavior(2);
            }
        }
        return;
    }

    private String formatCalories(double p4)
    {
        if (p4 > 4576918229304087675) {
            if (!"Samoch\u00f3d".equals(this.selectedMode)) {
                return new StringBuilder().append(Math.round((p4 * 4628574517030027264))).append(" kcal").toString();
            } else {
                return "--";
            }
        } else {
            return "--";
        }
    }

    static String formatDuration(long p6)
    {
        String v6_6 = Math.max(0, (p6 / 1000));
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", new Object[] {Long.valueOf((v6_6 / 3600)), Long.valueOf(((v6_6 % 3600) / 60)), Long.valueOf((v6_6 % 60))}));
    }

    private String formatPace(double p6)
    {
        if (p6 > 4596373779694328218) {
            String v6_8 = (4633641066610819072 / p6);
            int v2_0 = ((int) Math.floor(v6_8));
            String v6_3 = ((int) Math.round(((v6_8 - ((double) v2_0)) * 4633641066610819072)));
            if (v6_3 >= 60) {
                v2_0++;
                v6_3 -= 60;
            }
            return String.format(java.util.Locale.US, "%d:%02d", new Object[] {Integer.valueOf(v2_0), Integer.valueOf(v6_3)}));
        } else {
            return "--";
        }
    }

    private android.graphics.drawable.GradientDrawable gradient(int p3, int p4, int p5)
    {
        android.graphics.drawable.GradientDrawable v0_1 = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable$Orientation.LEFT_RIGHT, new int[] {p3, p4}));
        v0_1.setCornerRadius(((float) this.dp(p5)));
        return v0_1;
    }

    static synthetic void lambda$showRouteMapDialog$17(android.app.Dialog p0, android.view.View p1)
    {
        p0.dismiss();
        return;
    }

    static synthetic void lambda$showRouteMapDialog$18(android.app.Dialog p0, android.view.View p1)
    {
        p0.dismiss();
        return;
    }

    static synthetic void lambda$showRouteMapDialog$19(boolean p2, pl.tomalawsb.licznik.RouteMapView p3, android.view.View p4)
    {
        if (!p2) {
            p3.fitRoute();
        } else {
            p3.centerOnLastPoint(4625478292286210048);
        }
        return;
    }

    static synthetic void lambda$showRouteMapDialog$20(android.app.Dialog p0, boolean p1, pl.tomalawsb.licznik.RouteMapView p2, android.content.DialogInterface p3)
    {
        long v0_1 = p0.getWindow();
        if (v0_1 != 0) {
            v0_1.setLayout(-1, -1);
            v0_1.setBackgroundDrawableResource(17170445);
        }
        if (!p1) {
            p2.fitRoute();
        } else {
            p2.centerOnLastPoint(4625478292286210048);
        }
        return;
    }

    private void lowPassSensor(float[] p5, float[] p6)
    {
        int v0 = 0;
        while ((v0 < 3) && (v0 < p5.length)) {
            float v1_3 = p6[v0];
            p6[v0] = (v1_3 + ((p5[v0] - v1_3) * 1043878380));
            v0++;
        }
        return;
    }

    private String modeEmoji()
    {
        String v0_2;
        if (!"Samoch\u00f3d".equals(this.selectedMode)) {
            v0_2 = "\u1f6b2";
        } else {
            v0_2 = "\u1f697";
        }
        return v0_2;
    }

    private android.widget.TextView navItem(String p3, boolean p4)
    {
        int v4_1;
        if (p4 == 0) {
            v4_1 = this.MUTED;
        } else {
            v4_1 = this.GREEN_DARK;
        }
        android.widget.TextView v3_1 = this.text(p3, 12, v4_1, 1);
        v3_1.setGravity(17);
        return v3_1;
    }

    private void openAppSettings()
    {
        android.content.Intent v0_1 = new android.content.Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
        v0_1.setData(android.net.Uri.parse(new StringBuilder("package:").append(this.getPackageName()).toString()));
        this.startActivity(v0_1);
        return;
    }

    private void openBatterySettings()
    {
        try {
            this.startActivity(new android.content.Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"));
        } catch (Exception) {
            this.openAppSettings();
        }
        return;
    }

    private void openUrl(String p3)
    {
        try {
            this.startActivity(new android.content.Intent("android.intent.action.VIEW", android.net.Uri.parse(p3)));
        } catch (Exception) {
            android.widget.Toast.makeText(this, "Nie mo\u017cna otworzy\u0107 linku.", 0).show();
        }
        return;
    }

    private android.widget.TextView pill(String p1, int p2, int p3, int p4, boolean p5)
    {
        android.widget.TextView v1_1 = this.text(p1, p4, p2, p5);
        v1_1.setGravity(17);
        v1_1.setBackground(this.round(p3, 20, 0, 0));
        return v1_1;
    }

    private android.content.SharedPreferences prefs()
    {
        return this.getSharedPreferences("licznik", 0);
    }

    private void primaryRideAction()
    {
        if (this.running) {
            this.togglePause();
        } else {
            this.startRide();
        }
        return;
    }

    private android.view.View recentRideRow(org.json.JSONObject p12)
    {
        android.widget.LinearLayout v4_1;
        android.widget.LinearLayout v0_1 = new android.widget.LinearLayout(this);
        v0_1.setGravity(16);
        v0_1.setPadding(this.dp(12), this.dp(6), this.dp(12), this.dp(6));
        int v2_1 = p12.optString("mode", "Rower");
        if (!v2_1.equals("Samoch\u00f3d")) {
            v4_1 = "\u1f6b2";
        } else {
            v4_1 = "\u1f697";
        }
        android.widget.LinearLayout$LayoutParams v1_4;
        v0_1.addView(this.circleText(v4_1, 36, android.graphics.Color.rgb(235, 247, 226), this.GREEN_DARK, 17));
        android.widget.LinearLayout v4_5 = new android.widget.LinearLayout(this);
        v4_5.setOrientation(1);
        v4_5.setPadding(this.dp(12), 0, 0, 0);
        if (!v2_1.equals("Samoch\u00f3d")) {
            v1_4 = "Przejazd";
        } else {
            v1_4 = "Przejazd samochodem";
        }
        v4_5.addView(this.text(v1_4, 13, this.TEXT, 1), new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        v4_5.addView(this.text(p12.optString("date", ""), 11, this.MUTED, 0), new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
        v0_1.addView(v4_5, new android.widget.LinearLayout$LayoutParams(0, -1, 1065353216));
        android.widget.TextView v12_4 = this.text(String.format(java.util.Locale.US, "%.2f km  \u203a", new Object[] {Double.valueOf(p12.optDouble("distanceKm", 0))})), 13, this.GREEN_DARK, 1);
        v12_4.setGravity(21);
        v0_1.addView(v12_4, new android.widget.LinearLayout$LayoutParams(this.dp(96), -1));
        return v0_1;
    }

    private void registerCompassSensors()
    {
        android.hardware.Sensor v0_0 = this.sensorManager;
        if (v0_0 != null) {
            v0_0.unregisterListener(this);
            android.hardware.Sensor v0_2 = this.rotationVectorSensor;
            if (v0_2 == null) {
                android.hardware.Sensor v0_3 = this.accelerometerSensor;
                if (v0_3 != null) {
                    this.sensorManager.registerListener(this, v0_3, 1);
                }
                android.hardware.Sensor v0_1 = this.magneticFieldSensor;
                if (v0_1 != null) {
                    this.sensorManager.registerListener(this, v0_1, 1);
                }
            } else {
                this.sensorManager.registerListener(this, v0_2, 1);
            }
            return;
        } else {
            return;
        }
    }

    private void registerUpdates()
    {
        android.content.IntentFilter v0_1 = new android.content.IntentFilter("pl.tomalawsb.licznik.UPDATE");
        if (android.os.Build$VERSION.SDK_INT < 33) {
            this.registerReceiver(this.updateReceiver, v0_1);
        } else {
            this.registerReceiver(this.updateReceiver, v0_1, 4);
        }
        return;
    }

    private void renderHistory()
    {
        android.widget.TextView v0_0 = this.rideActionHost;
        if (v0_0 != null) {
            v0_0.setVisibility(8);
        }
        this.currentTab = 1;
        this.updateNav();
        this.contentBox.removeAllViews();
        this.contentBox.setPadding(this.dp(16), 0, this.dp(16), this.dp(18));
        this.contentBox.addView(this.text("Historia", 23, this.NAVY, 1), new android.widget.LinearLayout$LayoutParams(-1, this.dp(48)));
        try {
            android.widget.TextView v1_3 = new org.json.JSONArray(this.prefs().getString("history", "[]"));
        } catch (Exception) {
            this.contentBox.addView(this.text("Nie uda\u0142o si\u0119 odczyta\u0107 historii.", 16, this.RED, 1));
            return;
        }
        if (v1_3.length() != 0) {
            int v3_7 = (v1_3.length() - 1);
            while (v3_7 >= 0) {
                this.addHistoryCard(v1_3.getJSONObject(v3_7));
                v3_7--;
            }
            return;
        } else {
            android.widget.TextView v1_5 = this.text("Brak zapisanych jazd.", 16, this.MUTED, 0);
            v1_5.setGravity(17);
            this.contentBox.addView(v1_5, new android.widget.LinearLayout$LayoutParams(-1, this.dp(140)));
            return;
        }
    }

    private void renderProfile()
    {
        android.widget.LinearLayout v0_0 = this.rideActionHost;
        if (v0_0 != null) {
            v0_0.setVisibility(8);
        }
        this.currentTab = 3;
        this.updateNav();
        this.contentBox.removeAllViews();
        this.contentBox.setPadding(this.dp(16), 0, this.dp(16), this.dp(18));
        this.contentBox.addView(this.text("Profil i ustawienia", 23, this.NAVY, 1), new android.widget.LinearLayout$LayoutParams(-1, this.dp(50)));
        android.widget.LinearLayout v0_3 = new android.widget.LinearLayout(this);
        v0_3.setOrientation(1);
        v0_3.setPadding(this.dp(16), this.dp(14), this.dp(16), this.dp(14));
        v0_3.setBackground(this.round(this.CARD_BG, 18, this.BORDER, 1));
        v0_3.setElevation(((float) this.dp(2)));
        v0_3.addView(this.text("Wersja: 2.9 - 1406262155", 15, this.TEXT, 1), new android.widget.LinearLayout$LayoutParams(-1, this.dp(34)));
        v0_3.addView(this.settingsButton("Sprawd\u017a aktualizacj\u0119", this.GREEN, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda11(this)));
        v0_3.addView(this.settingsButton("Zezwolenia i ustawienia aplikacji", this.BLUE, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda22(this)));
        v0_3.addView(this.settingsButton("Ustawienia baterii Android", android.graphics.Color.rgb(168, 112, 35), new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda24(this)));
        v0_3.addView(this.settingsButton("Wyczy\u015b\u0107 histori\u0119 jazdy", this.RED, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda25(this)));
        this.contentBox.addView(v0_3, new android.widget.LinearLayout$LayoutParams(-1, -2));
        return;
    }

    private void renderProgress()
    {
        android.widget.LinearLayout v1_0 = this.rideActionHost;
        if (v1_0 != null) {
            v1_0.setVisibility(8);
        }
        this.currentTab = 2;
        void v19_1 = this.updateNav();
        this.contentBox.removeAllViews();
        Object[] v5_4 = 0;
        this.contentBox.setPadding(this.dp(16), 0, this.dp(16), this.dp(18));
        this.contentBox.addView(this.text("Post\u0119py", 23, this.NAVY, 1), new android.widget.LinearLayout$LayoutParams(-1, this.dp(50)));
        try {
            android.widget.LinearLayout v1_4 = new org.json.JSONArray(v19_1.prefs().getString("history", "[]"));
            Object[] v7_1 = 0;
            double v9 = 0;
            double v11 = 0;
            double v13 = 0;
            long v15 = 0;
        } catch (Exception) {
            return;
        }
        while (v5_4 < v1_4.length()) {
            int v4_4 = v1_4.getJSONObject(v5_4);
            v9 += v4_4.optDouble("distanceKm", v7_1);
            v11 = Math.max(v11, v4_4.optDouble("avg", v7_1));
            v13 = Math.max(v13, v4_4.optDouble("max", v7_1));
            v15 += v4_4.optLong("elapsedMs", 0);
            v5_4++;
            v7_1 = 0;
        }
        android.widget.LinearLayout v1_7 = new android.widget.LinearLayout(this);
        v1_7.setGravity(17);
        this.contentBox.addView(v1_7, new android.widget.LinearLayout$LayoutParams(-1, this.dp(110)));
        this.buildBigStatCard(v1_7, "\u03a3", String.format(java.util.Locale.US, "%.2f\nkm razem", new Object[] {Double.valueOf(v9)})), pl.tomalawsb.licznik.MainActivity v0.GREEN_DARK);
        this.buildBigStatCard(v1_7, "\u25f7", new StringBuilder().append(pl.tomalawsb.licznik.MainActivity.formatDuration(v15)).append("\nczas").toString(), v0.BLUE);
        android.widget.LinearLayout v1_10 = new android.widget.LinearLayout(this);
        v1_10.setGravity(17);
        String v2_16 = new android.widget.LinearLayout$LayoutParams(-1, this.dp(110));
        v2_16.topMargin = this.dp(12);
        this.contentBox.addView(v1_10, v2_16);
        this.buildBigStatCard(v1_10, "\u2197", String.format(java.util.Locale.US, "%.3f\nnajl. \u015br.", new Object[] {Double.valueOf(v11)})), v0.GREEN_DARK);
        this.buildBigStatCard(v1_10, "\u25b2", String.format(java.util.Locale.US, "%.1f\nrekord", new Object[] {Double.valueOf(v13)})), v0.NAVY);
        return;
    }

    private void renderRide()
    {
        this.currentTab = 0;
        if (!this.running) {
            this.selectedMode = this.prefs().getString("last_mode", this.selectedMode);
        }
        this.updateModeVisuals();
        this.updateNav();
        this.contentBox.removeAllViews();
        this.contentBox.setPadding(this.dp(16), 0, this.dp(16), this.dp(10));
        android.widget.LinearLayout v1_3 = this.rideActionHost;
        if (v1_3 != null) {
            v1_3.setVisibility(0);
        }
        this.buildSpeedHero();
        this.buildDistanceTimeRow();
        this.buildGpsCard();
        this.buildActionRow();
        this.updateStatus(-4616189618054758400);
        this.updatePrimaryButton();
        this.requestSnapshot();
        return;
    }

    private boolean requestPermissionsIfNeeded(boolean p4)
    {
        String[] v0_1 = new java.util.ArrayList();
        if (this.checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != 0) {
            v0_1.add("android.permission.ACCESS_FINE_LOCATION");
        }
        if ((android.os.Build$VERSION.SDK_INT >= 33) && (this.checkSelfPermission("android.permission.POST_NOTIFICATIONS") != 0)) {
            v0_1.add("android.permission.POST_NOTIFICATIONS");
        }
        if (v0_1.isEmpty()) {
            return 1;
        } else {
            if (p4 != 0) {
                android.widget.Toast.makeText(this, "Aplikacja potrzebuje lokalizacji i powiadomienia.", 1).show();
            }
            int v1_3 = new String[0];
            this.requestPermissions(((String[]) v0_1.toArray(v1_3)), 1001);
            return 0;
        }
    }

    private void requestSnapshot()
    {
        android.content.Intent v0_1 = new android.content.Intent(this, pl.tomalawsb.licznik.RideTrackingService);
        v0_1.setAction("pl.tomalawsb.licznik.SNAPSHOT");
        try {
            this.startService(v0_1);
        } catch (Exception) {
        }
        return;
    }

    private void resetLocalViewOnly()
    {
        long v0_0 = this.speedValueText;
        if (v0_0 != 0) {
            v0_0.setText("0.0");
        }
        long v0_9 = this.speedSummaryText;
        if (v0_9 != 0) {
            v0_9.setText("\u015arednia: 0.000 km/h  \u2022  Maks: 0.0 km/h");
        }
        long v0_10 = this.distanceText;
        if (v0_10 != 0) {
            v0_10.setText("0.00\nkm dystansu");
        }
        long v0_1 = this.timeText;
        if (v0_1 != 0) {
            v0_1.setText("00:00:00\nczas jazdy");
        }
        long v0_2 = this.elevationText;
        if (v0_2 != 0) {
            v0_2.setText("--");
        }
        long v0_3 = this.caloriesText;
        if (v0_3 != 0) {
            v0_3.setText("--");
        }
        long v0_4 = this.paceText;
        if (v0_4 != 0) {
            v0_4.setText("--");
        }
        long v0_5 = this.routeView;
        if (v0_5 != 0) {
            v0_5.clear();
        }
        this.lastPointsJson = "[]";
        this.lastElapsedMs = 0;
        this.elapsedBaseMs = 0;
        this.elapsedSyncRealtime = android.os.SystemClock.elapsedRealtime();
        return;
    }

    private void resetRide()
    {
        if (this.running) {
            android.widget.Toast v0_4 = new android.content.Intent(this, pl.tomalawsb.licznik.RideTrackingService);
            v0_4.setAction("pl.tomalawsb.licznik.RESET");
            this.startService(v0_4);
        }
        android.widget.Toast v0_1;
        this.resetLocalViewOnly();
        if (!this.running) {
            v0_1 = "Licznik wyzerowany.";
        } else {
            v0_1 = "Pomiar wyzerowany.";
        }
        android.widget.Toast.makeText(this, v0_1, 0).show();
        return;
    }

    private void rotateCompassTo(float p3)
    {
        float v0_0 = this.compassDisplayRotation;
        float v0_1 = (v0_0 + (((((p3 - v0_0) + 1141309440) % 1135869952) - 1127481344) * 1050924810));
        this.compassDisplayRotation = v0_1;
        android.widget.ImageView v3_2 = this.compassView;
        if (v3_2 != null) {
            v3_2.setRotation(v0_1);
        }
        return;
    }

    private android.graphics.drawable.GradientDrawable round(int p2, int p3, int p4, int p5)
    {
        android.graphics.drawable.GradientDrawable v0_1 = new android.graphics.drawable.GradientDrawable();
        v0_1.setColor(p2);
        v0_1.setCornerRadius(((float) this.dp(p3)));
        if (p5 > 0) {
            v0_1.setStroke(this.dp(p5), p4);
        }
        return v0_1;
    }

    private android.widget.TextView settingsButton(String p4, int p5, android.view.View$OnClickListener p6)
    {
        android.widget.TextView v4_1 = this.text(p4, 15, -1, 1);
        v4_1.setGravity(17);
        v4_1.setBackground(this.round(p5, 14, p5, 0));
        v4_1.setOnClickListener(p6);
        android.widget.LinearLayout$LayoutParams v5_3 = new android.widget.LinearLayout$LayoutParams(-1, this.dp(46));
        v5_3.setMargins(0, this.dp(7), 0, 0);
        v4_1.setLayoutParams(v5_3);
        return v4_1;
    }

    private void showRouteMapDialog(String p20, String p21, String p22)
    {
        boolean v9 = "Aktualna trasa".equals(p20);
        try {
            android.widget.TextView v2_0;
            if (p21 != null) {
                v2_0 = p21;
            } else {
                v2_0 = "[]";
            }
        } catch (Exception) {
            android.widget.Toast.makeText(this, "Nie uda\u0142o si\u0119 otworzy\u0107 trasy.", 0).show();
            return;
        }
        android.widget.TextView v2_14;
        pl.tomalawsb.licznik.RouteMapView v1_6 = new org.json.JSONArray(v2_0);
        if (!v9) {
            v2_14 = 2;
        } else {
            v2_14 = 1;
        }
        if (v1_6.length() >= v2_14) {
            pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda8 v0_10;
            android.app.Dialog v12_1 = new android.app.Dialog(this);
            android.widget.LinearLayout v13_1 = new android.widget.LinearLayout(this);
            v13_1.setOrientation(1);
            v13_1.setBackgroundColor(this.BG);
            v13_1.setPadding(this.dp(14), this.dp(12), this.dp(14), this.dp(12));
            v12_1.setContentView(v13_1);
            android.widget.LinearLayout v15_1 = new android.widget.LinearLayout(this);
            v15_1.setGravity(16);
            v15_1.addView(this.text(p20, 19, this.NAVY, 1), new android.widget.LinearLayout$LayoutParams(0, this.dp(42), 1065353216));
            if (!v9) {
                v0_10 = "Dopasuj";
            } else {
                v0_10 = "Moja pozycja";
            }
            pl.tomalawsb.licznik.RouteMapView v1_21;
            android.widget.LinearLayout$LayoutParams v5_8 = this.pill(v0_10, -1, this.GREEN, 13, 1);
            if (!v9) {
                v1_21 = 86;
            } else {
                v1_21 = 112;
            }
            pl.tomalawsb.licznik.RouteMapView v1_8;
            v15_1.addView(v5_8, new android.widget.LinearLayout$LayoutParams(this.dp(v1_21), this.dp(36)));
            android.widget.TextView v18 = v5_8;
            pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda8 v0_15 = this.circleText("\u00d7", 36, -1, this.NAVY, 22);
            pl.tomalawsb.licznik.RouteMapView v1_0 = new android.widget.LinearLayout$LayoutParams(this.dp(38), this.dp(38));
            v1_0.leftMargin = this.dp(8);
            v15_1.addView(v0_15, v1_0);
            v13_1.addView(v15_1, new android.widget.LinearLayout$LayoutParams(-1, this.dp(46)));
            if (p22 != null) {
                v1_8 = new StringBuilder().append(p22).append("\nPrzesuwaj map\u0119 palcem, przybli\u017caj dwoma palcami.").toString();
            } else {
                v1_8 = "Przesuwaj map\u0119 palcem, przybli\u017caj dwoma palcami.";
            }
            pl.tomalawsb.licznik.RouteMapView v1_9 = this.text(v1_8, 12, this.MUTED, 0);
            v1_9.setGravity(16);
            v13_1.addView(v1_9, new android.widget.LinearLayout$LayoutParams(-1, this.dp(48)));
            pl.tomalawsb.licznik.RouteMapView v1_11 = new pl.tomalawsb.licznik.RouteMapView(this);
            v1_11.setInteractive(1);
            v1_11.setPointsFromJson(p21);
            v13_1.addView(v1_11, new android.widget.LinearLayout$LayoutParams(-1, 0, 1065353216));
            android.widget.TextView v2_13 = this.settingsButton("Zamknij", pl.tomalawsb.licznik.MainActivity v6.GREEN, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda5(v12_1));
            android.widget.LinearLayout$LayoutParams v5_5 = new android.widget.LinearLayout$LayoutParams(-1, this.dp(48));
            v5_5.setMargins(0, this.dp(10), 0, 0);
            v13_1.addView(v2_13, v5_5);
            v0_15.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda6(v12_1));
            v18.setOnClickListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda7(v9, v1_11));
            v12_1.setOnShowListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda8(v12_1, v9, v1_11));
            v12_1.show();
            return;
        } else {
            android.widget.Toast.makeText(this, "Brak lokalizacji lub trasy do pokazania na mapie.", 0).show();
            return;
        }
    }

    private void showSettings()
    {
        android.app.AlertDialog$Builder v0_1 = new android.widget.LinearLayout(this);
        v0_1.setOrientation(1);
        v0_1.setPadding(this.dp(18), this.dp(8), this.dp(18), this.dp(6));
        v0_1.addView(this.text(new StringBuilder("Wersja: 2.9 - 1406262155\nTryb jazdy: ").append(this.selectedMode).toString(), 16, this.NAVY, 1), new android.widget.LinearLayout$LayoutParams(-1, this.dp(64)));
        String v1_5 = new android.widget.CheckBox(this);
        v1_5.setText("Sprawdzaj aktualizacje przy starcie");
        v1_5.setTextSize(1097859072);
        v1_5.setTextColor(this.TEXT);
        v1_5.setChecked(this.prefs().getBoolean("auto_update_check", 0));
        v1_5.setOnCheckedChangeListener(new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda29(this));
        v0_1.addView(v1_5, new android.widget.LinearLayout$LayoutParams(-1, this.dp(46)));
        v0_1.addView(this.settingsButton("Zmie\u0144 tryb jazdy", this.GREEN, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda30(this)));
        v0_1.addView(this.settingsButton("Sprawd\u017a aktualizacj\u0119", this.GREEN, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda1(this)));
        v0_1.addView(this.settingsButton("Zezwolenia i ustawienia aplikacji", this.BLUE, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda2(this)));
        v0_1.addView(this.settingsButton("Ustawienia baterii Android", android.graphics.Color.rgb(168, 112, 35), new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda3(this)));
        v0_1.addView(this.settingsButton("Wyczy\u015b\u0107 histori\u0119 jazdy", this.RED, new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda4(this)));
        new android.app.AlertDialog$Builder(this).setTitle("Opcje").setView(v0_1).setNegativeButton("Zamknij", 0).show();
        return;
    }

    private android.widget.TextView squareAction(String p4, int p5, android.view.View$OnClickListener p6)
    {
        android.widget.TextView v4_1 = this.text(p4, 36, p5, 1);
        v4_1.setGravity(17);
        v4_1.setBackground(this.round(-1, 14, this.BORDER, 1));
        v4_1.setElevation(((float) this.dp(2)));
        v4_1.setOnClickListener(p6);
        return v4_1;
    }

    private void startRide()
    {
        if (this.requestPermissionsIfNeeded(1)) {
            android.widget.Toast v0_5 = new android.content.Intent(this, pl.tomalawsb.licznik.RideTrackingService);
            v0_5.setAction("pl.tomalawsb.licznik.START");
            v0_5.putExtra("mode", this.selectedMode);
            this.startForegroundService(v0_5);
            android.widget.Toast.makeText(this, "Pomiar uruchomiony.", 0).show();
            return;
        } else {
            return;
        }
    }

    private void stopRide()
    {
        if (this.running) {
            this.waitingForStopResult = 1;
            android.content.Intent v0_5 = new android.content.Intent(this, pl.tomalawsb.licznik.RideTrackingService);
            v0_5.setAction("pl.tomalawsb.licznik.STOP");
            this.startService(v0_5);
            return;
        } else {
            android.widget.Toast.makeText(this, "Pomiar nie jest uruchomiony.", 0).show();
            return;
        }
    }

    private android.widget.TextView text(String p2, int p3, int p4, boolean p5)
    {
        android.widget.TextView v0_1 = new android.widget.TextView(this);
        v0_1.setText(p2);
        v0_1.setTextSize(((float) p3));
        v0_1.setTextColor(p4);
        v0_1.setGravity(16);
        v0_1.setIncludeFontPadding(0);
        if (p5) {
            v0_1.setTypeface(android.graphics.Typeface.DEFAULT, 1);
        }
        return v0_1;
    }

    private void togglePause()
    {
        String v1_2;
        android.content.Intent v0_1 = new android.content.Intent(this, pl.tomalawsb.licznik.RideTrackingService);
        if (!this.paused) {
            v1_2 = "pl.tomalawsb.licznik.PAUSE";
        } else {
            v1_2 = "pl.tomalawsb.licznik.RESUME";
        }
        v0_1.setAction(v1_2);
        this.startService(v0_1);
        return;
    }

    private void updateModeVisuals()
    {
        String v0_0 = this.modeEmoji();
        android.widget.TextView v1_0 = this.headerModeIcon;
        if (v1_0 != null) {
            v1_0.setText(v0_0);
        }
        android.widget.TextView v1_1 = this.speedHeroWatermark;
        if (v1_1 != null) {
            v1_1.setText(v0_0);
        }
        android.widget.TextView v1_2 = this.navRide;
        if (v1_2 != null) {
            v1_2.setText(new StringBuilder().append(v0_0).append("\nJazda").toString());
        }
        return;
    }

    private void updateNav()
    {
        android.widget.TextView v0_0 = this.navRide;
        if (v0_0 != null) {
            int v1_6;
            if (this.currentTab != 0) {
                v1_6 = this.MUTED;
            } else {
                v1_6 = this.GREEN_DARK;
            }
            int v1_0;
            v0_0.setTextColor(v1_6);
            if (this.currentTab != 1) {
                v1_0 = this.MUTED;
            } else {
                v1_0 = this.GREEN_DARK;
            }
            int v1_2;
            this.navHistory.setTextColor(v1_0);
            if (this.currentTab != 2) {
                v1_2 = this.MUTED;
            } else {
                v1_2 = this.GREEN_DARK;
            }
            int v1_5;
            this.navProgress.setTextColor(v1_2);
            if (this.currentTab != 3) {
                v1_5 = this.MUTED;
            } else {
                v1_5 = this.GREEN_DARK;
            }
            this.navProfile.setTextColor(v1_5);
            return;
        } else {
            return;
        }
    }

    private void updatePrimaryButton()
    {
        if (this.primaryActionButton != null) {
            android.widget.LinearLayout v0_2 = this.primaryActionIcon;
            if ((v0_2 != null) && (this.primaryActionLabel != null)) {
                if (this.running) {
                    if (!this.paused) {
                        v0_2.setText("\u2161");
                        this.primaryActionLabel.setText("Pauza");
                        this.primaryActionButton.setBackground(this.gradient(this.GREEN, android.graphics.Color.rgb(0, 126, 89), 14));
                    } else {
                        v0_2.setText("\u25b6");
                        this.primaryActionLabel.setText("Wzn\u00f3w jazd\u0119");
                        this.primaryActionButton.setBackground(this.gradient(this.GREEN, android.graphics.Color.rgb(0, 126, 89), 14));
                    }
                } else {
                    v0_2.setText("\u25b6");
                    this.primaryActionLabel.setText("Rozpocznij jazd\u0119");
                    this.primaryActionButton.setBackground(this.gradient(this.GREEN, android.graphics.Color.rgb(0, 126, 89), 14));
                }
            }
        }
        return;
    }

    private void updateStatus(double p9)
    {
        android.widget.TextView v0 = this.statusText;
        if (v0 != null) {
            double v1_0 = this.running;
            if ((v1_0 == 0) || (this.paused)) {
                if (v1_0 == 0) {
                    v0.setText("\u25cf  GPS gotowy");
                    this.statusText.setTextColor(this.GREEN_DARK);
                } else {
                    v0.setText("\u25cf  Pauza");
                    this.statusText.setTextColor(android.graphics.Color.rgb(168, 112, 35));
                }
            } else {
                if ((p9 <= 0) || (p9 > 4622945017495814144)) {
                    if ((p9 <= 4622945017495814144) || (p9 > 4630122629401935872)) {
                        if (p9 <= 4630122629401935872) {
                            v0.setText("\u25cf  GPS aktywny");
                            this.statusText.setTextColor(this.GREEN_DARK);
                        } else {
                            v0.setText("\u25cf  GPS s\u0142aby");
                            this.statusText.setTextColor(this.RED);
                        }
                    } else {
                        v0.setText("\u25cf  GPS \u015bredni");
                        this.statusText.setTextColor(android.graphics.Color.rgb(168, 112, 35));
                    }
                } else {
                    v0.setText("\u25cf  GPS aktywny");
                    this.statusText.setTextColor(this.GREEN_DARK);
                }
            }
            return;
        } else {
            return;
        }
    }

    private int versionCodeFromTag(String p6)
    {
        if (p6 != 0) {
            try {
                int v6_3 = java.util.regex.Pattern.compile("v(\\d+)\\.(\\d+)(?:\\.(\\d+))?.*").matcher(p6);
            } catch (Exception) {
                return -1;
            }
            if (v6_3.matches()) {
                int v6_2;
                int v1_2 = Integer.parseInt(v6_3.group(1));
                int v2_2 = Integer.parseInt(v6_3.group(2));
                if (v6_3.group(3) != null) {
                    v6_2 = Integer.parseInt(v6_3.group(3));
                } else {
                    v6_2 = 0;
                }
                return (((v1_2 * 10000) + (v2_2 * 100)) + v6_2);
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    synthetic void lambda$addHistoryCard$11$pl-tomalawsb-licznik-MainActivity(String p1, String p2, android.view.View p3)
    {
        this.showRouteMapDialog("Szczeg\u00f3\u0142y trasy", p1, p2);
        return;
    }

    synthetic void lambda$buildActionRow$10$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.resetRide();
        return;
    }

    synthetic void lambda$buildActionRow$8$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.primaryRideAction();
        return;
    }

    synthetic void lambda$buildActionRow$9$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.stopRide();
        return;
    }

    synthetic void lambda$buildGpsCard$7$pl-tomalawsb-licznik-MainActivity(android.view.View p5)
    {
        this.showRouteMapDialog("Aktualna trasa", this.lastPointsJson, String.format(java.util.Locale.US, "Dystans %s  \u2022  czas %s", new Object[] {this.compactFirstLine(this.distanceText), pl.tomalawsb.licznik.MainActivity.formatDuration(this.lastElapsedMs)})));
        return;
    }

    synthetic void lambda$buildUi$0$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.chooseModeDialog();
        return;
    }

    synthetic void lambda$buildUi$1$pl-tomalawsb-licznik-MainActivity(android.view.View p2)
    {
        android.widget.Toast.makeText(this, "Powiadomienie GPS pojawia si\u0119 po starcie jazdy.", 0).show();
        return;
    }

    synthetic void lambda$buildUi$2$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.showSettings();
        return;
    }

    synthetic void lambda$buildUi$3$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.renderRide();
        return;
    }

    synthetic void lambda$buildUi$4$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.renderHistory();
        return;
    }

    synthetic void lambda$buildUi$5$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.renderProgress();
        return;
    }

    synthetic void lambda$buildUi$6$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.renderProfile();
        return;
    }

    synthetic void lambda$checkForUpdates$28$pl-tomalawsb-licznik-MainActivity(String p1, android.content.DialogInterface p2, int p3)
    {
        this.openUrl(p1);
        return;
    }

    synthetic void lambda$checkForUpdates$29$pl-tomalawsb-licznik-MainActivity(String p3, String p4, boolean p5, String p6)
    {
        if (p3 == null) {
            if (p5 != null) {
                android.app.AlertDialog$Builder vtmp8 = new android.app.AlertDialog$Builder(this).setTitle("Aktualizacja");
                if (p6 == null) {
                    p6 = "Masz aktualn\u0105 wersj\u0119 programu.";
                }
                vtmp8.setMessage(p6).setPositiveButton("Zamknij", 0).show();
            }
        } else {
            new android.app.AlertDialog$Builder(this).setTitle("Dost\u0119pna aktualizacja").setMessage(new StringBuilder("Znaleziono wersj\u0119: ").append(p4).toString()).setPositiveButton("Pobierz", new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda12(this, p3)).setNegativeButton("P\u00f3\u017aniej", 0).show();
        }
        return;
    }

    synthetic void lambda$checkForUpdates$30$pl-tomalawsb-licznik-MainActivity(boolean p12)
    {
        String v2_0 = 0;
        try {
            String v3_11 = ((java.net.HttpURLConnection) new java.net.URL("https://api.github.com/repos/tomalawsb/Licznik/releases/latest").openConnection());
            v3_11.setConnectTimeout(8000);
            v3_11.setReadTimeout(8000);
            v3_11.setRequestProperty("Accept", "application/vnd.github+json");
            int v4_2 = v3_11.getResponseCode();
        } catch (Exception v0_1) {
            String v3_0 = 0;
            Exception v0_2 = v0_1.getMessage();
            Exception v0_3 = new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda20;
            v0_3(this, v2_0, v3_0, p12, v0_2);
            this.runOnUiThread(v0_3);
            return;
        }
        if (v4_2 == 404) {
            throw new Exception("Nie ma jeszcze opublikowanej wersji Release.");
        } else {
            if ((v4_2 < 200) || (v4_2 > 299)) {
                throw new Exception(new StringBuilder("Serwer zwr\u00f3ci\u0142 kod: ").append(v4_2).toString());
            } else {
                Exception v1_5 = new java.io.BufferedReader(new java.io.InputStreamReader(v3_11.getInputStream()));
                String v3_6 = new StringBuilder();
                while(true) {
                    int v4_6 = v1_5.readLine();
                    if (v4_6 == 0) {
                        break;
                    }
                    v3_6.append(v4_6);
                }
                Exception v1_7 = new org.json.JSONObject(v3_6.toString());
                v3_0 = v1_7.optString("tag_name", "");
                try {
                    int v4_7 = this.versionCodeFromTag(v3_0);
                } catch (Exception v0_1) {
                }
                if (!v3_0.equals("v2.9-1406262155")) {
                    if ((v4_7 <= 0) || (v4_7 > 20900)) {
                        Exception v0_8;
                        Exception v1_8 = v1_7.optJSONArray("assets");
                        if (v1_8 == null) {
                            v0_8 = 0;
                        } else {
                            int v4_9 = 0;
                            while (v4_9 < v1_8.length()) {
                                org.json.JSONObject v5_9 = v1_8.getJSONObject(v4_9);
                                if (!v5_9.optString("name", "").toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) {
                                    v4_9++;
                                } else {
                                    v0_8 = v5_9.optString("browser_download_url", 0);
                                }
                            }
                        }
                        if (v0_8 == null) {
                            try {
                                throw new Exception("Znaleziono nowszy wpis, ale bez pliku APK.");
                            } catch (Exception v1_11) {
                                v2_0 = v0_8;
                                v0_1 = v1_11;
                            }
                        } else {
                            v2_0 = v0_8;
                            v0_2 = 0;
                            v0_3 = new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda20;
                            v0_3(this, v2_0, v3_0, p12, v0_2);
                            this.runOnUiThread(v0_3);
                            return;
                        }
                    } else {
                    }
                }
                v0_2 = "Masz aktualn\u0105 wersj\u0119 programu.";
                v0_3 = new pl.tomalawsb.licznik.MainActivity$$ExternalSyntheticLambda20;
                v0_3(this, v2_0, v3_0, p12, v0_2);
                this.runOnUiThread(v0_3);
                return;
            }
        }
    }

    synthetic void lambda$chooseModeDialog$16$pl-tomalawsb-licznik-MainActivity(String[] p2, android.content.DialogInterface p3, int p4)
    {
        this.selectedMode = p2[p4];
        this.prefs().edit().putString("last_mode", this.selectedMode).apply();
        this.updateModeVisuals();
        android.widget.Toast.makeText(this, new StringBuilder("Wybrano: ").append(this.selectedMode).toString(), 0).show();
        p3.dismiss();
        return;
    }

    synthetic void lambda$confirmClearHistory$27$pl-tomalawsb-licznik-MainActivity(android.content.DialogInterface p2, int p3)
    {
        this.prefs().edit().putString("history", "[]").apply();
        android.widget.Toast.makeText(this, "Historia wyczyszczona.", 0).show();
        if (this.currentTab == 1) {
            this.renderHistory();
        }
        if (this.currentTab == 0) {
            this.renderRide();
        }
        return;
    }

    synthetic void lambda$renderProfile$12$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.checkForUpdates(1);
        return;
    }

    synthetic void lambda$renderProfile$13$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.openAppSettings();
        return;
    }

    synthetic void lambda$renderProfile$14$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.openBatterySettings();
        return;
    }

    synthetic void lambda$renderProfile$15$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.confirmClearHistory();
        return;
    }

    synthetic void lambda$showSettings$21$pl-tomalawsb-licznik-MainActivity(android.widget.CompoundButton p2, boolean p3)
    {
        this.prefs().edit().putBoolean("auto_update_check", p3).apply();
        return;
    }

    synthetic void lambda$showSettings$22$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.chooseModeDialog();
        return;
    }

    synthetic void lambda$showSettings$23$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.checkForUpdates(1);
        return;
    }

    synthetic void lambda$showSettings$24$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.openAppSettings();
        return;
    }

    synthetic void lambda$showSettings$25$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.openBatterySettings();
        return;
    }

    synthetic void lambda$showSettings$26$pl-tomalawsb-licznik-MainActivity(android.view.View p1)
    {
        this.confirmClearHistory();
        return;
    }

    public void onAccuracyChanged(android.hardware.Sensor p1, int p2)
    {
        return;
    }

    protected void onCreate(android.os.Bundle p3)
    {
        super.onCreate(p3);
        int v3_5 = this.getWindow();
        v3_5.setStatusBarColor(this.BG);
        v3_5.setNavigationBarColor(this.BG);
        this.selectedMode = this.prefs().getString("last_mode", "Rower");
        int v3_4 = ((android.hardware.SensorManager) this.getSystemService("sensor"));
        this.sensorManager = v3_4;
        if (v3_4 != 0) {
            this.rotationVectorSensor = v3_4.getDefaultSensor(11);
            this.accelerometerSensor = this.sensorManager.getDefaultSensor(1);
            this.magneticFieldSensor = this.sensorManager.getDefaultSensor(2);
        }
        this.buildUi();
        this.enterImmersiveMode();
        this.registerUpdates();
        this.requestPermissionsIfNeeded(0);
        this.renderRide();
        this.uiHandler.post(this.clockUiTicker);
        if (this.prefs().getBoolean("auto_update_check", 0)) {
            this.checkForUpdates(0);
        }
        return;
    }

    protected void onDestroy()
    {
        try {
            this.unregisterReceiver(this.updateReceiver);
        } catch (Exception) {
        }
        this.uiHandler.removeCallbacksAndMessages(0);
        super.onDestroy();
        return;
    }

    protected void onPause()
    {
        android.hardware.SensorManager v0 = this.sensorManager;
        if (v0 != null) {
            v0.unregisterListener(this);
        }
        super.onPause();
        return;
    }

    protected void onResume()
    {
        super.onResume();
        this.registerCompassSensors();
        return;
    }

    public void onSensorChanged(android.hardware.SensorEvent p6)
    {
        if ((p6 != 0) && (p6.sensor != null)) {
            int v0_2 = new float[9];
            boolean v4 = 1;
            if (p6.sensor.getType() != 11) {
                if (p6.sensor.getType() != 1) {
                    if (p6.sensor.getType() == 2) {
                        this.lowPassSensor(p6.values, this.magneticValues);
                        this.magneticReady = 1;
                    }
                } else {
                    this.lowPassSensor(p6.values, this.gravityValues);
                    this.gravityReady = 1;
                }
                v4 = 0;
            } else {
                android.hardware.SensorManager.getRotationMatrixFromVector(v0_2, p6.values);
            }
            if ((!v4) && ((this.gravityReady) && (this.magneticReady))) {
                v4 = android.hardware.SensorManager.getRotationMatrix(v0_2, 0, this.gravityValues, this.magneticValues);
            }
            if (v4) {
                float v6_8 = new float[3];
                android.hardware.SensorManager.getOrientation(v0_2, v6_8);
                float v6_10 = ((float) Math.toDegrees(((double) v6_8[0])));
                if (v6_10 < 0) {
                    v6_10 += 1135869952;
                }
                this.rotateCompassTo((- v6_10));
            } else {
                return;
            }
        }
        return;
    }

    public void onWindowFocusChanged(boolean p1)
    {
        super.onWindowFocusChanged(p1);
        if (p1) {
            this.enterImmersiveMode();
        }
        return;
    }
}
