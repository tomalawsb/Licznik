from pathlib import Path
import base64
import re

VERSION = '2.7 - 1406262027'
TAG = 'v2.7-1406262027'
CODE = '20700'
COMPASS_B64 = Path('tools/compass_v27.b64').read_text(encoding='ascii').strip()

def replace_method(text, signature, new_method):
    start = text.find(signature)
    if start < 0:
        raise SystemExit(f"Nie znaleziono metody: {signature}")
    brace = text.find('{', start)
    if brace < 0:
        raise SystemExit(f"Brak klamry metody: {signature}")
    depth = 0
    i = brace
    in_string = False
    escaped = False
    while i < len(text):
        ch = text[i]
        if in_string:
            if escaped:
                escaped = False
            elif ch == '\\':
                escaped = True
            elif ch == '"':
                in_string = False
        else:
            if ch == '"':
                in_string = True
            elif ch == '{':
                depth += 1
            elif ch == '}':
                depth -= 1
                if depth == 0:
                    return text[:start] + new_method.rstrip() + text[i+1:]
        i += 1
    raise SystemExit(f"Nie domknięto metody: {signature}")

main_path = Path('app/src/main/java/pl/tomalawsb/licznik/MainActivity.java')
main = main_path.read_text(encoding='utf-8')
if 'import android.widget.ImageView;' not in main:
    main = main.replace('import android.widget.FrameLayout;\n', 'import android.widget.FrameLayout;\nimport android.widget.ImageView;\n')
main = re.sub(r'public static final String VERSION_NAME = ".*?";', f'public static final String VERSION_NAME = "{VERSION}";', main)
main = re.sub(r'public static final String CURRENT_RELEASE_TAG = ".*?";', f'public static final String CURRENT_RELEASE_TAG = "{TAG}";', main)
main = re.sub(r'public static final int CURRENT_VERSION_CODE = \d+;', f'public static final int CURRENT_VERSION_CODE = {CODE};', main)
if 'private ImageView compassView;' not in main:
    main = main.replace('    private RouteMapView routeView;\n', '    private RouteMapView routeView;\n    private ImageView compassView;\n')
main = main.replace('        buildActionRow();\n        buildRecentRides();', '        buildActionRow();')

main = replace_method(main, '    private void buildSpeedHero()', r'''    private void buildSpeedHero() {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(gradient(GREEN, Color.rgb(0, 126, 89), 20));
        hero.setPadding(dp(18), dp(9), dp(12), dp(9));
        hero.setElevation(dp(2));

        speedHeroWatermark = null;
        compassView = new ImageView(this);
        compassView.setImageResource(R.drawable.compass_north);
        compassView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        compassView.setContentDescription("Kompas wskazujący północ");
        FrameLayout.LayoutParams compassLp = new FrameLayout.LayoutParams(dp(92), dp(92), Gravity.TOP | Gravity.RIGHT);
        compassLp.topMargin = dp(8);
        compassLp.rightMargin = dp(4);
        hero.addView(compassView, compassLp);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.LEFT);
        column.setPadding(0, 0, dp(92), 0);
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

        speedSummaryText = text("Średnia: 0.000 km/h  •  Maks: 0.0 km/h", 15, Color.rgb(229, 250, 237), true);
        speedSummaryText.setSingleLine(true);
        speedSummaryText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        column.addView(speedSummaryText, new LinearLayout.LayoutParams(-1, dp(38)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(148));
        lp.setMargins(0, dp(4), 0, dp(10));
        contentBox.addView(hero, lp);
    }''')

main = replace_method(main, '    private void buildDistanceTimeRow()', r'''    private void buildDistanceTimeRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(100));
        rowLp.setMargins(0, 0, 0, dp(10));
        contentBox.addView(row, rowLp);

        distanceText = buildBigStatCard(row, "⌘", "0.00\nkm dystansu", GREEN_DARK);
        timeText = buildBigStatCard(row, "◷", "00:00:00\nczas jazdy", BLUE);
    }''')

main = replace_method(main, '    private void buildGpsCard()', r'''    private void buildGpsCard() {
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

        card.addView(mapFrame, new LinearLayout.LayoutParams(-1, dp(220)));

        LinearLayout extras = new LinearLayout(this);
        extras.setGravity(Gravity.CENTER);
        extras.setPadding(dp(6), dp(8), dp(6), dp(8));
        extras.setBackgroundColor(Color.WHITE);
        elevationText = buildMiniMetric(extras, "▲", "Wzniesienie", "--", Color.rgb(72, 142, 55));
        caloriesText = buildMiniMetric(extras, "♨", "Kalorie", "--", Color.rgb(58, 110, 50));
        paceText = buildMiniMetric(extras, "◴", "Tempo", "--", Color.rgb(58, 110, 50));
        card.addView(extras, new LinearLayout.LayoutParams(-1, dp(66)));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(286));
        lp.setMargins(0, 0, 0, dp(10));
        contentBox.addView(card, lp);
    }''')

main = replace_method(main, '    private void buildActionRow()', r'''    private void buildActionRow() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(70));
        rowLp.setMargins(0, dp(2), 0, dp(6));
        contentBox.addView(row, rowLp);

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
    }''')

main = replace_method(main, '    private void showRouteMapDialog(String title, String pointsJson, String summary)', r'''    private void showRouteMapDialog(String title, String pointsJson, String summary) {
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

        TextView hint = text(summary == null ? "Przesuwaj mapę palcem, przybliżaj dwoma palcami." : summary + "\nPrzesuwaj mapę palcem, przybliżaj dwoma palcami.", 12, MUTED, false);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(hint, new LinearLayout.LayoutParams(-1, dp(48)));

        RouteMapView fullMap = new RouteMapView(this);
        fullMap.setInteractive(true);
        fullMap.setPointsFromJson(pointsJson);
        root.addView(fullMap, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView closeBottom = settingsButton("Zamknij", GREEN, v -> d.dismiss());
        LinearLayout.LayoutParams closeBottomLp = new LinearLayout.LayoutParams(-1, dp(48));
        closeBottomLp.setMargins(0, dp(10), 0, 0);
        root.addView(closeBottom, closeBottomLp);

        closeBtn.setOnClickListener(v -> d.dismiss());
        fitBtnTop.setOnClickListener(v -> {
  if (currentRoute) fullMap.centerOnLastPoint(17.0);
  else fullMap.fitRoute();
        });
        d.setOnShowListener(x -> {
  Window win = d.getWindow();
  if (win != null) {
      win.setLayout(-1, -1);
      win.setBackgroundDrawableResource(android.R.color.transparent);
  }
  if (currentRoute) fullMap.centerOnLastPoint(17.0);
  else fullMap.fitRoute();
        });
        d.show();
    }''')

main = replace_method(main, '    private LinearLayout actionBtn(String icon, String label, int color, View.OnClickListener listener)', r'''    private LinearLayout actionBtn(String icon, String label, int color, View.OnClickListener listener) {
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
    }''')

main = replace_method(main, '    private TextView squareAction(String label, int color, View.OnClickListener listener)', r'''    private TextView squareAction(String label, int color, View.OnClickListener listener) {
        TextView b = text(label, 24, color, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(round(Color.WHITE, 14, BORDER, 1));
        b.setElevation(dp(2));
        b.setOnClickListener(listener);
        return b;
    }''')

main_path.write_text(main, encoding='utf-8')

route_path = Path('app/src/main/java/pl/tomalawsb/licznik/RouteMapView.java')
route = route_path.read_text(encoding='utf-8')
route = replace_method(route, '    public boolean hasRoute()', r'''    public boolean hasRoute() {
        return !routePoints.isEmpty();
    }''')
route = replace_method(route, '    public void fitRoute()', r'''    public void fitRoute() {
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
    }''')
route = replace_method(route, '    private void redrawRoute()', r'''    private void redrawRoute() {
        mapView.getOverlays().clear();
        if (routePoints.isEmpty()) {
  placeholder.setVisibility(VISIBLE);
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
        fitRoute();
    }''')
route_path.write_text(route, encoding='utf-8')

drawable_dir = Path('app/src/main/res/drawable-nodpi')
drawable_dir.mkdir(parents=True, exist_ok=True)
(drawable_dir / 'compass_north.png').write_bytes(base64.b64decode(COMPASS_B64))

gradle_path = Path('app/build.gradle')
gradle = gradle_path.read_text(encoding='utf-8')
gradle = re.sub(r'versionCode \d+', f'versionCode {CODE}', gradle)
gradle = re.sub(r"versionName '.*?'", f"versionName '{VERSION}'", gradle)
gradle_path.write_text(gradle, encoding='utf-8')

readme_path = Path('README.md')
readme = readme_path.read_text(encoding='utf-8')
readme = re.sub(r'Wersja: \*\*.*?\*\*', f'Wersja: **{VERSION}**', readme, count=1)
marker = f'## Zmiany {VERSION}'
if marker not in readme:
    readme += f'''\n\n{marker}\n- Przebudowano kartę prędkości: prędkość jest wyżej, a średnia i maksymalna są większe.\n- Dodano kompas z rzeczywistą przezroczystością PNG, wskazujący północ.\n- Usunięto Ostatnie jazdy z ekranu głównego.\n- Powiększono mapę i obniżono panel sterowania.\n- Powiększono ikony przycisków jazdy, Stop i Reset.\n- Po otwarciu aktualnej mapy ostatnia pozycja użytkownika jest wyśrodkowana.\n'''
readme_path.write_text(readme, encoding='utf-8')