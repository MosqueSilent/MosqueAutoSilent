package org.mosqueautosilent.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Mosque Auto-Silent's refreshed native interface and local session controller. */
class MainActivityV3 extends AppCompatActivity {
    private static final int PAGE_HOME = 0;
    private static final int PAGE_SETUP = 1;
    private static final int PAGE_SCHEDULE = 2;
    private static final int PAGE_IMAM = 3;
    private static final int PAGE_MORE = 4;
    private static final String KEY_THEME_MODE = "theme_mode";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences store;
    private WifiScanner wifiScanner;
    private GeofenceManager geofenceManager;
    private AudioProfileController audioController;
    private UiPalette colors;

    private final List<String> ssids = new ArrayList<>();
    private final List<WifiScanner.Network> wifiNetworks = new ArrayList<>();
    private Spinner ssidSpinner;
    private TextView wifiScanInfo;
    private TextView locationInfo;
    private TextView homeWifiStatus;
    private TextView homeGeoStatus;
    private TextView homeModeStatus;
    private TextView homeCountdown;
    private TextView homeCountdownLabel;
    private TextView homeNextPrayer;
    private TextView homeProfile;
    private TextView sessionStatus;
    private TextView attendeeStatus;
    private TextView connectedCount;
    private TextView localSessionsCount;
    private TextView localReleaseCount;
    private LinearLayout scheduleRows;
    private FrameLayout pageContainer;
    private Button endPrayerButton;
    private SeekBar durationSeek;
    private TextView durationText;
    private SwitchCompat demoWifiSwitch;
    private SwitchCompat demoGeoSwitch;
    private boolean arabicLanguage;
    private Spinner mosqueSpinner;
    private TextView mosqueHeader;
    private final List<MosqueProfile> mosqueProfiles = new ArrayList<>();
    private MosqueProfile selectedMosque;

    private String selectedSsid = "";
    private boolean active;
    private boolean manualWifiOverride;
    private boolean demoInside;
    private int durationMinutes = PrayerWindow.DEFAULT_DURATION_MINUTES;
    private int geofenceRadiusMetres = 100;
    private long lastFreshScan;
    private long testIqamaAt;
    private long activeIqamaAt;
    private long activeRestoreAt;
    private LocalDate displayedDate;
    private int selectedPage = PAGE_HOME;
    private ActivityResultLauncher<String[]> permissionRequest;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            evaluateState();
            handler.postDelayed(this, 1000L);
        }
    };
    private final Runnable preIqamaScan = new Runnable() {
        @Override public void run() {
            if (hasWifiPermissions()) scanWifi(false);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences(GeofenceReceiver.PREFS, MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(preferences.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        super.onCreate(savedInstanceState);

        store = preferences;
        arabicLanguage = store.getBoolean("arabic_language", false);
        loadMosqueProfiles();
        colors = new UiPalette(isNightMode());
        wifiScanner = new WifiScanner(this);
        geofenceManager = new GeofenceManager(this);
        audioController = new AudioProfileController(this);
        selectedSsid = store.getString("selected_ssid", "");
        demoInside = store.getBoolean(GeofenceReceiver.KEY_INSIDE, false);
        durationMinutes = clamp(store.getInt("duration_minutes", PrayerWindow.DEFAULT_DURATION_MINUTES), 1, 30);
        geofenceRadiusMetres = clamp(store.getInt("geofence_radius", 100), 50, 400);
        testIqamaAt = store.getLong("test_iqama_at", 0L);
        activeIqamaAt = store.getLong("active_iqama_at", 0L);
        activeRestoreAt = store.getLong("active_restore_at", 0L);
        active = store.getBoolean("mosque_active", false) || audioController.hasPendingRestore();
        if (active && activeRestoreAt <= 0L) {
            boolean restored = audioController.restore();
            active = !restored;
            if (restored) {
                activeIqamaAt = 0L;
                store.edit().putBoolean("mosque_active", false)
                    .remove("active_iqama_at").remove("active_restore_at").commit();
            }
        }
        permissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (hasWifiPermissions()) scanWifi(false);
        });

        buildUi();
        refreshScheduleRows();
        schedulePreIqamaScan();
        evaluateState();
    }

    private void buildUi() {
        LinearLayout shell = column();
        shell.setBackgroundResource(R.drawable.bg_zellij);
        shell.setLayoutDirection(arabicLanguage ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        shell.setPadding(dp(18), dp(12), dp(18), dp(8));

        LinearLayout header = row();
        ImageView logo = new ImageView(this);
        logo.setImageResource(org.mosqueautosilent.app.R.drawable.ic_brand_mosque);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        logoParams.setMarginEnd(dp(12));
        header.addView(logo, logoParams);
        LinearLayout brand = column();
        brand.addView(text("Mosque Auto-Silent", 19, colors.text, true));
        mosqueHeader = text(selectedMosque.name + (selectedMosque.id.equals("tarfaya") ? " · Meknes" : ""), 12, colors.muted, false);
        brand.addView(mosqueHeader);
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView version = text("0.5", 11, colors.green, true);
        version.setGravity(Gravity.CENTER);
        version.setBackground(roundRect(colors.goodSoft, dp(14), colors.goodSoft));
        header.addView(version, new LinearLayout.LayoutParams(dp(44), dp(30)));
        shell.addView(header);
        addSpace(shell, 10);

        LinearLayout navigation = row();
        navigation.setPadding(dp(4), dp(4), dp(4), dp(4));
        navigation.setBackground(roundRect(colors.surface, dp(18), colors.border));
        String[] labels = {"Home", "Setup", "Schedule", "Imam", "More"};
        TextView[] tabs = new TextView[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int page = i;
            TextView tab = text(labels[i], 11, i == PAGE_HOME ? colors.onPrimary : colors.muted, true);
            tab.setGravity(Gravity.CENTER);
            tab.setBackground(roundRect(i == PAGE_HOME ? colors.primaryAction : colors.surface, dp(14),
                i == PAGE_HOME ? colors.primaryAction : colors.surface));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1f);
            params.setMargins(dp(2), 0, dp(2), 0);
            navigation.addView(tab, params);
            tabs[i] = tab;
            tab.setOnClickListener(v -> {
                selectedPage = page;
                for (int j = 0; j < tabs.length; j++) {
                    tabs[j].setTextColor(j == page ? colors.onPrimary : colors.muted);
                    tabs[j].setBackground(roundRect(j == page ? colors.primaryAction : colors.surface, dp(14),
                        j == page ? colors.primaryAction : colors.surface));
                }
                showPage(page);
            });
        }
        shell.addView(navigation);
        addSpace(shell, 8);

        FrameLayout content = new FrameLayout(this);
        pageContainer = content;
        shell.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout[] pageBodies = new LinearLayout[5];
        for (int i = 0; i < pageBodies.length; i++) {
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(true);
            LinearLayout body = column();
            body.setPadding(dp(7), dp(4), dp(7), dp(36));
            scroll.setClipToPadding(false);
            scroll.addView(body);
            content.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
            scroll.setVisibility(i == PAGE_HOME ? View.VISIBLE : View.GONE);
            pageBodies[i] = body;
        }
        buildHomePage(pageBodies[PAGE_HOME]);
        buildSetupPage(pageBodies[PAGE_SETUP]);
        buildSchedulePage(pageBodies[PAGE_SCHEDULE]);
        buildImamPage(pageBodies[PAGE_IMAM]);
        buildMorePage(pageBodies[PAGE_MORE]);
        setContentView(shell);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        androidx.core.view.WindowInsetsControllerCompat bars = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), shell);
        bars.setAppearanceLightStatusBars(!isNightMode());
        bars.setAppearanceLightNavigationBars(!isNightMode());
        ViewCompat.setOnApplyWindowInsetsListener(shell, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(dp(18) + insets.left, dp(12) + insets.top,
                dp(18) + insets.right, dp(8) + insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(shell);
    }

    private void buildHomePage(LinearLayout page) {
        page.addView(text("PRAYER STATUS", 11, colors.muted, true));
        addSpace(page, 8);
        LinearLayout hero = column();
        hero.setPadding(dp(20), dp(19), dp(20), dp(19));
        hero.setBackground(roundRect(colors.hero, dp(24), colors.hero));
        homeCountdownLabel = text("NEXT IQAMA", 11, colors.heroMuted, true);
        hero.addView(homeCountdownLabel);
        homeCountdown = text("--:--:--", 36, Color.WHITE, true);
        homeCountdown.setPadding(0, dp(5), 0, dp(3));
        hero.addView(homeCountdown);
        homeNextPrayer = text("Prayer times are unavailable", 14, Color.WHITE, true);
        hero.addView(homeNextPrayer);
        hero.addView(text("Silence begins 30 seconds before Iqama", 12, colors.heroMuted, false));
        page.addView(hero);

        addSpace(page, 13);
        LinearLayout statusRow = row();
        FrameLayout wifiCard = signalCard("WI-FI", "Not detected", "Select a network in Setup", colors.waiting, colors.waitingSoft, true);
        FrameLayout geoCard = signalCard("GEOFENCE", "Outside", "Set a location in Setup", colors.waiting, colors.waitingSoft, false);
        statusRow.addView(wifiCard, new LinearLayout.LayoutParams(0, dp(124), 1f));
        View gap = new View(this);
        statusRow.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        statusRow.addView(geoCard, new LinearLayout.LayoutParams(0, dp(124), 1f));
        page.addView(statusRow);

        addSpace(page, 10);
        homeModeStatus = statusPill("Waiting for conditions", colors.waiting, colors.waitingSoft);
        page.addView(homeModeStatus);
        homeProfile = text("Phone profile: " + AudioProfileController.modeName(audioController.currentMode()), 13, colors.muted, false);
        homeProfile.setPadding(dp(3), dp(8), dp(3), dp(2));
        page.addView(homeProfile);
        addSpace(page, 12);
        page.addView(text("STATUS KEY", 10, colors.muted, true));
        addLegend(page);
    }

    private void buildSetupPage(LinearLayout page) {
        addPageTitle(page, "Setup", "Choose the Wi-Fi and location conditions for this phone.");
        addSection(page, "MOSQUE");
        LinearLayout mosqueCard = card();
        mosqueCard.addView(text("Selected mosque", 14, colors.text, true));
        mosqueSpinner = new Spinner(this);
        refreshMosqueChoices();
        mosqueCard.addView(mosqueSpinner);
        Button addMosque = button("Add a mosque", false);
        mosqueCard.addView(addMosque, matchWrap());
        addMosque.setOnClickListener(v -> showAddMosqueDialog());
        page.addView(mosqueCard);

        addSection(page, "WI-FI PRESENCE");
        LinearLayout wifiCard = card();
        wifiCard.addView(text("Selected mosque network", 14, colors.text, true));
        ssidSpinner = new Spinner(this);
        ArrayAdapter<String> ssidAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
            new ArrayList<>(java.util.Collections.singletonList(L("Refresh networks to choose an SSID"))));
        ssidAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ssidSpinner.setAdapter(ssidAdapter);
        wifiCard.addView(ssidSpinner);
        wifiScanInfo = text("No scan yet", 12, colors.muted, false);
        wifiCard.addView(wifiScanInfo);
        Button scan = button("Refresh Wi-Fi networks", true);
        wifiCard.addView(scan, matchWrap());
        scan.setOnClickListener(v -> ensureWifiPermissionsAndScan());
        page.addView(wifiCard);

        addSection(page, "LOCATION PRESENCE");
        LinearLayout locationCard = card();
        locationInfo = text("Location has not been refreshed", 12, colors.muted, false);
        locationCard.addView(locationInfo);
        TextView radiusLabel = text("Geofence radius · " + geofenceRadiusMetres + " m", 13, colors.text, true);
        locationCard.addView(radiusLabel);
        SeekBar radius = new SeekBar(this);
        radius.setMax(350);
        radius.setProgress(geofenceRadiusMetres - 50);
        locationCard.addView(radius);
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                geofenceRadiusMetres = progress + 50;
                radiusLabel.setText(L("Geofence radius · " + geofenceRadiusMetres + " m"));
                store.edit().putInt("geofence_radius", geofenceRadiusMetres).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        Button refreshLocation = button("Refresh current location", true);
        locationCard.addView(refreshLocation, matchWrap());
        refreshLocation.setOnClickListener(v -> refreshLocationOnly());
        Button currentCenter = button("Use current location as test-zone center", false);
        locationCard.addView(currentCenter, matchWrap());
        currentCenter.setOnClickListener(v -> ensureLocationForGeofence());
        Button selectedMosqueZone = button("Set geofence for selected mosque", false);
        locationCard.addView(selectedMosqueZone, matchWrap());
        selectedMosqueZone.setOnClickListener(v -> ensureSelectedMosqueGeofence());
        page.addView(locationCard);

        addSection(page, "ACTIVATION RULE");
        page.addView(text("Selected Wi-Fi + inside the geofence + within the prayer window", 14, colors.text, true));
        page.addView(text("Both presence signals are required. This device scans Wi-Fi and refreshes its geofence from here.", 12, colors.muted, false));
    }

    private void buildSchedulePage(LinearLayout page) {
        addPageTitle(page, "Iqama schedule", "Times are calculated from the selected mosque's available prayer calendar.");
        addSection(page, "TODAY · ADHAN → IQAMA");
        scheduleRows = column();
        page.addView(scheduleRows);

        addSection(page, "SILENT WINDOW");
        LinearLayout timing = card();
        timing.addView(text("Start · 30 seconds before Iqama", 14, colors.text, true));
        durationText = text("Restore · 10 minutes after Iqama", 13, colors.muted, false);
        timing.addView(durationText);
        durationSeek = new SeekBar(this);
        durationSeek.setMax(29);
        durationSeek.setProgress(durationMinutes - 1);
        timing.addView(durationSeek);
        durationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                durationMinutes = progress + 1;
                durationText.setText(L("Restore · " + durationMinutes + " minutes after Iqama"));
                store.edit().putInt("duration_minutes", durationMinutes).apply();
                schedulePreIqamaScan();
                evaluateState();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        timing.addView(text("Restore duration can be set from 1 to 30 minutes. Default: 10 minutes.", 12, colors.muted, false));
        page.addView(timing);

        addSection(page, "DEVICE TEST");
        LinearLayout testButtons = row();
        Button test30 = button("Test in 30 sec", false);
        Button test90 = button("Test in 90 sec", false);
        testButtons.addView(test30, new LinearLayout.LayoutParams(0, dp(48), 1f));
        testButtons.addView(test90, new LinearLayout.LayoutParams(0, dp(48), 1f));
        page.addView(testButtons);
        test30.setOnClickListener(v -> setTestIqama(30_000L));
        test90.setOnClickListener(v -> setTestIqama(90_000L));
    }

    private void buildImamPage(LinearLayout page) {
        addPageTitle(page, "Imam dashboard", "Local controls and this phone's activity.");
        sessionStatus = statusPill("No active local session", colors.waiting, colors.waitingSoft);
        page.addView(sessionStatus);
        addSpace(page, 10);

        LinearLayout actionCard = card();
        actionCard.addView(text("END SALAT", 11, colors.muted, true));
        actionCard.addView(text("Restore this phone's previous sound profile.", 14, colors.text, true));
        endPrayerButton = button("End Salat · Restore Sound", true);
        actionCard.addView(endPrayerButton, matchWrap());
        endPrayerButton.setEnabled(false);
        endPrayerButton.setOnClickListener(v -> restoreSound("Imam ended Salat locally"));
        actionCard.addView(text("This action affects this phone only.", 12, colors.muted, false));
        page.addView(actionCard);

        addSection(page, "LOCAL COUNTERS · THIS PHONE");
        LinearLayout stats = row();
        localSessionsCount = metricCard("0", "Silence sessions");
        localReleaseCount = metricCard("0", "Imam releases");
        stats.addView(localSessionsCount, new LinearLayout.LayoutParams(0, dp(92), 1f));
        View spacer = new View(this);
        stats.addView(spacer, new LinearLayout.LayoutParams(dp(9), 1));
        stats.addView(localReleaseCount, new LinearLayout.LayoutParams(0, dp(92), 1f));
        page.addView(stats);

        addSection(page, "ATTENDEES");
        LinearLayout attendees = card();
        LinearLayout attendeeHeader = row();
        attendeeHeader.addView(text("Connected attendees", 14, colors.text, true),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        connectedCount = text("—", 18, colors.muted, true);
        attendeeHeader.addView(connectedCount);
        attendees.addView(attendeeHeader);
        attendeeStatus = text("Live attendee data is not connected. The mosque-wide roster and release require the authenticated backend and attendee clients.",
            12, colors.muted, false);
        attendees.addView(attendeeStatus);
        page.addView(attendees);
    }

    private void buildMorePage(LinearLayout page) {
        addPageTitle(page, "More", "Appearance, permissions and test controls.");
        addSection(page, "LANGUAGE");
        LinearLayout language = card();
        language.addView(text("App language", 14, colors.text, true));
        Spinner languageSpinner = new Spinner(this);
        String[] languageOptions = {L("🇬🇧 English"), L("🇲🇦 العربية")};
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languageOptions);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        languageSpinner.setSelection(arabicLanguage ? 1 : 0);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean useArabic = position == 1;
                if (useArabic != arabicLanguage) {
                    store.edit().putBoolean("arabic_language", useArabic).apply();
                    recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        language.addView(languageSpinner);
        page.addView(language);

        addSection(page, "APPEARANCE");
        LinearLayout appearance = card();
        appearance.addView(text("Theme", 14, colors.text, true));
        Spinner themeSpinner = new Spinner(this);
        String[] themes = {L("Follow device"), L("Light"), L("Dark")};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, themes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themeSpinner.setAdapter(adapter);
        int mode = store.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        themeSpinner.setSelection(mode == AppCompatDelegate.MODE_NIGHT_NO ? 1 : mode == AppCompatDelegate.MODE_NIGHT_YES ? 2 : 0);
        themeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedMode = position == 1 ? AppCompatDelegate.MODE_NIGHT_NO :
                    position == 2 ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                if (selectedMode != store.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)) {
                    store.edit().putInt(KEY_THEME_MODE, selectedMode).apply();
                    AppCompatDelegate.setDefaultNightMode(selectedMode);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        appearance.addView(themeSpinner);
        page.addView(appearance);

        addSection(page, "TEST OVERRIDES");
        LinearLayout overrides = card();
        demoWifiSwitch = new SwitchCompat(this);
        demoWifiSwitch.setText(L("Simulate Wi-Fi presence"));
        demoWifiSwitch.setTextColor(colors.text);
        demoWifiSwitch.setChecked(false);
        demoWifiSwitch.setOnCheckedChangeListener((button, checked) -> {
            manualWifiOverride = checked;
            evaluateState();
        });
        overrides.addView(demoWifiSwitch);
        demoGeoSwitch = new SwitchCompat(this);
        demoGeoSwitch.setText(L("Simulate inside geofence"));
        demoGeoSwitch.setTextColor(colors.text);
        demoGeoSwitch.setChecked(demoInside);
        demoGeoSwitch.setOnCheckedChangeListener((button, checked) -> {
            demoInside = checked;
            store.edit().putBoolean(GeofenceReceiver.KEY_INSIDE, checked).apply();
            evaluateState();
        });
        overrides.addView(demoGeoSwitch);
        overrides.addView(text("Overrides are for local testing and do not represent mosque-wide attendance.", 12, colors.muted, false));
        page.addView(overrides);

        addSection(page, "DEVICE ACCESS");
        LinearLayout access = card();
        Button policy = button("Sound & Do Not Disturb access", false);
        access.addView(policy, matchWrap());
        policy.setOnClickListener(v -> startActivity(audioController.policyAccessSettingsIntent()));
        Button permissions = button("Review app permissions", false);
        access.addView(permissions, matchWrap());
        permissions.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:" + getPackageName()))));
        page.addView(access);
    }

    private void loadMosqueProfiles() {
        mosqueProfiles.clear();
        mosqueProfiles.add(new MosqueProfile("tarfaya", "Tarfaya Mosque", 33.90066, -5.54761, true));
        try {
            JSONArray saved = new JSONArray(store.getString("custom_mosques", "[]"));
            for (int i = 0; i < saved.length(); i++) {
                JSONObject item = saved.getJSONObject(i);
                mosqueProfiles.add(new MosqueProfile(item.getString("id"), item.getString("name"),
                    item.getDouble("latitude"), item.getDouble("longitude"), false));
            }
        } catch (Exception ignored) {
            // A malformed local directory falls back to the known Tarfaya profile.
        }
        String selectedId = store.getString("selected_mosque_id", "tarfaya");
        selectedMosque = mosqueProfiles.get(0);
        for (MosqueProfile profile : mosqueProfiles) if (profile.id.equals(selectedId)) selectedMosque = profile;
    }

    private void refreshMosqueChoices() {
        if (mosqueSpinner == null) return;
        List<String> names = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < mosqueProfiles.size(); i++) {
            MosqueProfile profile = mosqueProfiles.get(i);
            names.add(L(profile.name));
            if (profile.id.equals(selectedMosque.id)) selectedIndex = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mosqueSpinner.setAdapter(adapter);
        mosqueSpinner.setSelection(selectedIndex);
        mosqueSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                MosqueProfile chosen = mosqueProfiles.get(position);
                if (selectedMosque == null || !selectedMosque.id.equals(chosen.id)) selectMosque(chosen);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void selectMosque(MosqueProfile profile) {
        if (selectedMosque != null && !selectedMosque.id.equals(profile.id)) {
            if (active) restoreSound("Mosque selection changed");
            demoInside = false;
            store.edit().putBoolean(GeofenceReceiver.KEY_INSIDE, false).apply();
            if (demoGeoSwitch != null) demoGeoSwitch.setChecked(false);
        }
        selectedMosque = profile;
        store.edit().putString("selected_mosque_id", profile.id).apply();
        if (mosqueHeader != null) mosqueHeader.setText(L(profile.name + (profile.id.equals("tarfaya") ? " · Meknes" : "")));
        if (locationInfo != null) locationInfo.setText(L(profile.hasPrayerCalendar
            ? "Selected mosque location: Tarfaya, Meknes." : "Selected mosque saved. Its prayer calendar is not configured yet."));
        refreshScheduleRows();
        schedulePreIqamaScan();
        evaluateState();
    }

    private void showAddMosqueDialog() {
        LinearLayout fields = column();
        fields.setPadding(dp(20), dp(8), dp(20), 0);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint(L("Mosque name"));
        fields.addView(name);
        EditText latitude = new EditText(this);
        latitude.setSingleLine(true);
        latitude.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        latitude.setHint(L("Latitude (for example 33.90)"));
        fields.addView(latitude);
        EditText longitude = new EditText(this);
        longitude.setSingleLine(true);
        longitude.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        longitude.setHint(L("Longitude (for example -5.54)"));
        fields.addView(longitude);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(L("Add a mosque"))
            .setView(fields).setPositiveButton(L("Save mosque"), null).setNegativeButton(L("Cancel"), null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String mosqueName = name.getText().toString().trim();
            try {
                double lat = Double.parseDouble(latitude.getText().toString().trim());
                double lon = Double.parseDouble(longitude.getText().toString().trim());
                if (mosqueName.isEmpty() || lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new IllegalArgumentException();
                MosqueProfile profile = new MosqueProfile("custom_" + System.currentTimeMillis(), mosqueName, lat, lon, false);
                mosqueProfiles.add(profile);
                saveCustomMosques();
                selectMosque(profile);
                refreshMosqueChoices();
                dialog.dismiss();
            } catch (RuntimeException ex) {
                Toast.makeText(this, L("Enter a mosque name and valid latitude and longitude."), Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private void saveCustomMosques() {
        JSONArray saved = new JSONArray();
        for (MosqueProfile profile : mosqueProfiles) {
            if (profile.id.equals("tarfaya")) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("id", profile.id);
                item.put("name", profile.name);
                item.put("latitude", profile.latitude);
                item.put("longitude", profile.longitude);
                saved.put(item);
            } catch (org.json.JSONException ignored) {}
        }
        store.edit().putString("custom_mosques", saved.toString()).apply();
    }

    private static final class MosqueProfile {
        final String id;
        final String name;
        final double latitude;
        final double longitude;
        final boolean hasPrayerCalendar;

        MosqueProfile(String id, String name, double latitude, double longitude, boolean hasPrayerCalendar) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.hasPrayerCalendar = hasPrayerCalendar;
        }
    }

    private void refreshScheduleRows() {
        if (scheduleRows == null) return;
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Casablanca"));
        displayedDate = today;
        scheduleRows.removeAllViews();
        if (selectedMosque == null || !selectedMosque.hasPrayerCalendar) {
            scheduleRows.addView(text("No Adhan calendar is configured for this mosque yet. Add its local prayer data before using automatic timing.",
                13, colors.waiting, false));
            return;
        }
        List<PrayerSchedule.Event> events = PrayerSchedule.forDate(this, today);
        if (events.isEmpty()) {
            scheduleRows.addView(text("Iqama times are not available for this date.", 13, colors.muted, false));
            return;
        }
        for (PrayerSchedule.Event event : events) {
            LinearLayout scheduleCard = row();
            scheduleCard.setGravity(Gravity.CENTER_VERTICAL);
            scheduleCard.setPadding(dp(14), dp(11), dp(14), dp(11));
            scheduleCard.setBackground(roundRect(colors.surface, dp(17), colors.border));
            LinearLayout.LayoutParams outer = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(70));
            outer.bottomMargin = dp(7);
            LinearLayout left = column();
            left.addView(text(event.prayer, 14, colors.text, true));
            left.addView(text("Adhan " + event.adhanText, 11, colors.muted, false));
            scheduleCard.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            LinearLayout right = column();
            right.setGravity(Gravity.RIGHT);
            right.addView(text(timeOf(event.iqamaAt), 15, colors.green, true));
            right.addView(text("Iqama " + event.offsetLabel(), 10, colors.muted, false));
            scheduleCard.addView(right);
            scheduleRows.addView(scheduleCard, outer);
        }
    }

    private void evaluateState() {
        if (scheduleRows != null && !LocalDate.now(ZoneId.of("Africa/Casablanca")).equals(displayedDate)) refreshScheduleRows();
        long now = System.currentTimeMillis();
        if (testIqamaAt > 0 && now >= PrayerWindow.restoresAt(testIqamaAt, durationMinutes)) {
            testIqamaAt = 0L;
            store.edit().remove("test_iqama_at").apply();
        }

        String candidatePrayer = "";
        long candidateIqamaAt = 0L;
        if (testIqamaAt > 0) {
            candidatePrayer = "Test Iqama";
            candidateIqamaAt = testIqamaAt;
        } else {
            PrayerSchedule.Event event = PrayerSchedule.currentOrNext(this, now, durationMinutes);
            if (event != null) {
                candidatePrayer = event.prayer;
                candidateIqamaAt = event.iqamaAt;
            }
        }
        boolean wifi = effectiveWifiPresent(now);
        boolean inside = demoInside || store.getBoolean(GeofenceReceiver.KEY_INSIDE, false);
        long targetStart = candidateIqamaAt > 0 ? PrayerWindow.silenceStartsAt(candidateIqamaAt) : Long.MAX_VALUE;
        long targetEnd = candidateIqamaAt > 0 ? PrayerWindow.restoresAt(candidateIqamaAt, durationMinutes) : 0L;
        boolean inWindow = candidateIqamaAt > 0 && now >= targetStart && now < targetEnd;
        long releasedFor = store.getLong("released_iqama_at", 0L);

        if (!active && wifi && inside && inWindow && releasedFor != candidateIqamaAt) activateSound(candidateIqamaAt, targetEnd);
        if (active && (!wifi || !inside)) restoreSound(!wifi ? "Selected Wi-Fi left range" : "Geofence exited");
        if (active && activeRestoreAt > 0 && now >= activeRestoreAt) restoreSound("Restore duration elapsed");

        updateHome(wifi, inside, candidatePrayer, candidateIqamaAt, now);
        updateImamPage();
    }

    private void updateHome(boolean wifi, boolean inside, String prayer, long targetAt, long now) {
        if (homeWifiStatus == null) return;
        boolean wifiConfigured = manualWifiOverride || !selectedSsid.isEmpty();
        WifiScanner.Network selectedNetwork = null;
        for (WifiScanner.Network network : wifiNetworks) if (network.ssid.equals(selectedSsid)) selectedNetwork = network;
        String strength = selectedNetwork == null ? "" : "\n" + L("Signal") + " " + selectedNetwork.signalPercent +
            "% · " + selectedNetwork.rssiDbm + " dBm";
        homeWifiStatus.setText(L("WI-FI") + "\n" + L(wifi ? "PRESENT" : wifiConfigured ? "NOT DETECTED" : "NOT SET") +
            (selectedSsid.isEmpty() ? "" : "\n" + selectedSsid) + strength);
        homeWifiStatus.setTextColor(wifi ? colors.good : wifiConfigured ? colors.bad : colors.waiting);
        homeWifiStatus.setBackground(roundRect(wifi ? colors.goodSoft : wifiConfigured ? colors.badSoft : colors.waitingSoft,
            dp(19), wifi ? colors.goodSoft : wifiConfigured ? colors.badSoft : colors.waitingSoft));
        homeGeoStatus.setText(L("GEOFENCE") + "\n" + L(inside ? "INSIDE" : "OUTSIDE") + "\n" +
            L(geofenceRadiusMetres + " m radius"));
        homeGeoStatus.setTextColor(inside ? colors.good : colors.bad);
        homeGeoStatus.setBackground(roundRect(inside ? colors.goodSoft : colors.badSoft, dp(19), inside ? colors.goodSoft : colors.badSoft));

        long nextAt = nextFutureIqama(now);
        if (testIqamaAt > now) {
            nextAt = testIqamaAt;
            homeNextPrayer.setText(L("Test Iqama · " + timeOf(testIqamaAt)));
        } else if (nextAt > 0) {
            PrayerSchedule.Event next = nextFutureEvent(now);
            homeNextPrayer.setText(L(next == null ? "Next Iqama · " + timeOf(nextAt) :
                "Next Iqama · " + next.prayer + " · " + timeOf(next.iqamaAt)));
        } else {
            homeNextPrayer.setText(L("Iqama schedule unavailable"));
        }
        homeCountdownLabel.setText(L(active ? "NEXT IQAMA AFTER THIS SALAT" : "COUNTDOWN TO IQAMA"));
        homeCountdown.setText(nextAt > 0 ? formatMs(nextAt - now) : "--:--:--");
        homeModeStatus.setText(L(active ? "●  SALAT ACTIVE · SILENT" : wifi && inside ? "●  PRESENCE READY · WAITING FOR IQAMA" : "●  WAITING FOR CONDITIONS"));
        homeModeStatus.setTextColor(active ? colors.good : wifi && inside ? colors.good : colors.waiting);
        homeModeStatus.setBackground(roundRect(active ? colors.goodSoft : wifi && inside ? colors.goodSoft : colors.waitingSoft,
            dp(15), active ? colors.goodSoft : wifi && inside ? colors.goodSoft : colors.waitingSoft));
        homeProfile.setText(L("Phone profile · " + AudioProfileController.modeName(audioController.currentMode()) +
            (active ? "   ·   restores " + timeOf(activeRestoreAt) : "")));
        homeProfile.setTextColor(colors.muted);
    }

    private void updateImamPage() {
        if (sessionStatus == null) return;
        sessionStatus.setText(L(active ? "●  SALAT ACTIVE ON THIS PHONE" : "●  NO ACTIVE LOCAL SESSION"));
        sessionStatus.setTextColor(active ? colors.good : colors.muted);
        sessionStatus.setBackground(roundRect(active ? colors.goodSoft : colors.surface, dp(15), active ? colors.goodSoft : colors.border));
        endPrayerButton.setEnabled(active);
        endPrayerButton.setAlpha(active ? 1f : 0.55f);
        localSessionsCount.setText(store.getInt("silence_sessions", 0) + "\n" + L("Silence sessions"));
        localReleaseCount.setText(store.getInt("imam_release_count", 0) + "\n" + L("Imam releases"));
        connectedCount.setText("—");
        attendeeStatus.setText(L("Live attendee data is not connected. The mosque-wide roster and release require the authenticated backend and attendee clients."));
    }

    private boolean effectiveWifiPresent(long now) {
        if (manualWifiOverride) return true;
        if (selectedSsid.isEmpty()) return false;
        return lastFreshScan > 0 && ssids.contains(selectedSsid) &&
            (active || now - lastFreshScan <= 120_000L);
    }

    private void activateSound(long iqamaAt, long restoreAt) {
        if (!audioController.hasPolicyAccess()) {
            Toast.makeText(this, L("Open Sound access to enable automatic silence."), Toast.LENGTH_LONG).show();
            startActivity(audioController.policyAccessSettingsIntent());
            return;
        }
        if (!audioController.silenceAndSave()) {
            Toast.makeText(this, L("Silent mode could not be applied on this phone."), Toast.LENGTH_LONG).show();
            return;
        }
        active = true;
        activeIqamaAt = iqamaAt;
        activeRestoreAt = restoreAt;
        store.edit().putBoolean("mosque_active", true)
            .putLong("active_iqama_at", activeIqamaAt)
            .putLong("active_restore_at", activeRestoreAt)
            .putInt("silence_sessions", store.getInt("silence_sessions", 0) + 1)
            .commit();
        Toast.makeText(this, L("Silent mode on · previous profile saved"), Toast.LENGTH_SHORT).show();
    }

    private void restoreSound(String reason) {
        if (!active) return;
        boolean imamRelease = reason.toLowerCase(Locale.ROOT).contains("imam");
        boolean restored = audioController.restore();
        if (!restored) {
            Toast.makeText(this, L("Restore pending · Sound access is required."), Toast.LENGTH_LONG).show();
            return;
        }
        long releasedIqama = activeIqamaAt;
        active = false;
        activeIqamaAt = 0L;
        activeRestoreAt = 0L;
        SharedPreferences.Editor edit = store.edit().putBoolean("mosque_active", false)
            .remove("active_iqama_at").remove("active_restore_at");
        if (imamRelease) {
            edit.putLong("released_iqama_at", releasedIqama)
                .putInt("imam_release_count", store.getInt("imam_release_count", 0) + 1);
        }
        edit.commit();
        Toast.makeText(this, L("Restored " + AudioProfileController.modeName(audioController.currentMode()) + " · " + reason),
            Toast.LENGTH_SHORT).show();
    }

    private void ensureWifiPermissionsAndScan() {
        List<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)
            missing.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        if (!missing.isEmpty()) permissionRequest.launch(missing.toArray(new String[0]));
        else scanWifi(false);
    }

    private boolean hasWifiPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void scanWifi(boolean showToast) {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean locationOn = manager != null && (Build.VERSION.SDK_INT >= 28 ? manager.isLocationEnabled() :
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
        if (!locationOn) {
            if (wifiScanInfo != null) wifiScanInfo.setText(L("Turn on Android Location, then refresh again."));
            return;
        }
        if (wifiScanInfo != null) wifiScanInfo.setText(L("Scanning visible networks…"));
        wifiScanner.requestScan((found, fresh) -> runOnUiThread(() -> {
            wifiNetworks.clear();
            wifiNetworks.addAll(found);
            ssids.clear();
            for (WifiScanner.Network network : found) ssids.add(network.ssid);
            if (fresh) lastFreshScan = System.currentTimeMillis();
            List<String> choices = new ArrayList<>();
            choices.add(L(ssids.isEmpty() ? "No networks found" : "Select a Wi-Fi network"));
            for (WifiScanner.Network network : found) choices.add(String.format(Locale.getDefault(),
                "%s · %d%% · %d dBm", network.ssid, network.signalPercent, network.rssiDbm));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ssidSpinner.setAdapter(adapter);
            int selected = ssids.indexOf(selectedSsid);
            ssidSpinner.setSelection(selected < 0 ? 0 : selected + 1);
            ssidSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position <= 0) return;
                    if (position - 1 >= ssids.size()) return;
                    selectedSsid = ssids.get(position - 1);
                    store.edit().putString("selected_ssid", selectedSsid).apply();
                    evaluateState();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
            wifiScanInfo.setText(L((fresh ? "Fresh scan" : "Cached / throttled scan") + " · " + found.size() + " networks · " +
                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date())));
            evaluateState();
            if (showToast) Toast.makeText(this, L("Wi-Fi list refreshed"), Toast.LENGTH_SHORT).show();
        }));
    }

    private void ensureLocationForGeofence() {
        if (!ensureLocationPermissions()) return;
        geofenceManager.armAtCurrentLocation(geofenceRadiusMetres, (ok, message) -> runOnUiThread(() -> {
            if (ok) store.edit().putString("geofence_mode", "current").apply();
            locationInfo.setText(L(message));
            Toast.makeText(this, L(ok ? "Location and geofence refreshed" : message), Toast.LENGTH_LONG).show();
        }));
    }

    private void refreshLocationOnly() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            return;
        }
        locationInfo.setText(L("Refreshing current location…"));
        geofenceManager.refreshCurrentLocation((ok, message) -> runOnUiThread(() -> {
            locationInfo.setText(L(message));
            if (!ok) Toast.makeText(this, L(message), Toast.LENGTH_LONG).show();
        }));
    }

    private void ensureSelectedMosqueGeofence() {
        if (!ensureLocationPermissions()) return;
        geofenceManager.armAtCoordinates(selectedMosque.latitude, selectedMosque.longitude, geofenceRadiusMetres,
            selectedMosque.name, (ok, message) -> runOnUiThread(() -> {
                if (ok) store.edit().putString("geofence_mode", "mosque").putString("geofence_mosque_id", selectedMosque.id).apply();
                locationInfo.setText(L(message));
                Toast.makeText(this, L(message), Toast.LENGTH_LONG).show();
            }));
    }

    private boolean ensureLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            return false;
        }
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setTitle(L("Allow background location"))
                .setMessage(L("Choose Allow all the time in app location settings so geofence enter and exit events can arrive while the app is closed."))
                .setPositiveButton(L("Open app settings"), (dialog, which) -> startActivity(new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + getPackageName()))))
                .setNegativeButton(L("Cancel"), null).show();
            return false;
        }
        return true;
    }

    private void setTestIqama(long delayMs) {
        testIqamaAt = System.currentTimeMillis() + delayMs;
        store.edit().putLong("test_iqama_at", testIqamaAt).remove("released_iqama_at").apply();
        schedulePreIqamaScan();
        evaluateState();
    }

    private void schedulePreIqamaScan() {
        handler.removeCallbacks(preIqamaScan);
        long now = System.currentTimeMillis();
        long iqama = testIqamaAt > now ? testIqamaAt : nextFutureIqama(now);
        if (iqama <= 0) return;
        handler.postDelayed(preIqamaScan, Math.max(0L, iqama - 60_000L - now));
    }

    private long nextFutureIqama(long now) {
        if (testIqamaAt > now) return testIqamaAt;
        PrayerSchedule.Event event = nextFutureEvent(now);
        return event == null ? 0L : event.iqamaAt;
    }

    private PrayerSchedule.Event nextFutureEvent(long now) {
        if (selectedMosque == null || !selectedMosque.hasPrayerCalendar) return null;
        for (PrayerSchedule.Event event : PrayerSchedule.nextTwoDays(this, LocalDate.now(ZoneId.of("Africa/Casablanca"))))
            if (event.iqamaAt > now) return event;
        return null;
    }

    private void showPage(int index) {
        if (pageContainer == null) return;
        for (int i = 0; i < pageContainer.getChildCount(); i++)
            pageContainer.getChildAt(i).setVisibility(i == index ? View.VISIBLE : View.GONE);
    }

    private void addPageTitle(LinearLayout page, String title, String subtitle) {
        page.addView(text(title, 23, colors.text, true));
        page.addView(text(subtitle, 12, colors.muted, false));
        addSpace(page, 10);
    }

    private FrameLayout signalCard(String title, String status, String detail, int tint, int background, boolean wifi) {
        FrameLayout card = new FrameLayout(this);
        TextView label = text(title + "\n" + status + "\n" + detail, 11, tint, true);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setPaddingRelative(dp(14), dp(11), dp(44), dp(11));
        label.setBackground(roundRect(background, dp(19), background));
        if (wifi) homeWifiStatus = label; else homeGeoStatus = label;
        card.addView(label, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageButton refresh = new ImageButton(this);
        refresh.setImageResource(R.drawable.ic_refresh);
        refresh.setColorFilter(colors.primaryAction);
        refresh.setContentDescription(L(wifi ? "Refresh Wi-Fi networks" : "Refresh geofence"));
        refresh.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        refresh.setPadding(dp(7), dp(7), dp(7), dp(7));
        refresh.setBackground(roundRect(background, dp(11), background));
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.END | Gravity.TOP);
        iconParams.topMargin = dp(7);
        iconParams.setMarginEnd(dp(7));
        card.addView(refresh, iconParams);
        refresh.setOnClickListener(v -> {
            if (wifi) ensureWifiPermissionsAndScan();
            else refreshGeofenceFromHome();
        });
        return card;
    }

    private void refreshGeofenceFromHome() {
        if ("current".equals(store.getString("geofence_mode", "mosque"))) ensureLocationForGeofence();
        else ensureSelectedMosqueGeofence();
    }

    private TextView statusPill(String value, int foreground, int background) {
        TextView pill = text(value, 12, foreground, true);
        pill.setPadding(dp(13), dp(9), dp(13), dp(9));
        pill.setBackground(roundRect(background, dp(15), background));
        return pill;
    }

    private void addLegend(LinearLayout page) {
        LinearLayout legend = row();
        legend.addView(legendItem("Ready", colors.good));
        legend.addView(legendItem("Waiting", colors.waiting));
        legend.addView(legendItem("Not detected", colors.bad));
        page.addView(legend);
    }

    private TextView legendItem(String label, int color) {
        TextView item = text("●  " + label, 10, color, true);
        item.setPadding(0, 0, dp(10), 0);
        return item;
    }

    private TextView metricCard(String count, String label) {
        TextView view = text(count + "\n" + label, 14, colors.text, true);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(13), dp(12), dp(10), dp(12));
        view.setBackground(roundRect(colors.surface, dp(17), colors.border));
        return view;
    }

    private LinearLayout card() {
        LinearLayout card = column();
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackground(roundRect(colors.surface, dp(20), colors.border));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(2);
        card.setLayoutParams(params);
        return card;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(L(label));
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setIncludeFontPadding(true);
        button.setLineSpacing(dp(2), 1.0f);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(primary ? colors.onPrimary : colors.primaryAction);
        button.setBackground(roundRect(primary ? colors.primaryAction : colors.buttonSoft, dp(15),
            primary ? colors.primaryAction : colors.buttonSoft));
        button.setPaddingRelative(dp(12), dp(5), dp(12), dp(5));
        button.setMinHeight(dp(48));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(L(value));
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(true);
        view.setLineSpacing(dp(2), 1.0f);
        view.setPadding(dp(3), dp(6), dp(3), dp(6));
        return view;
    }

    private void addSection(LinearLayout page, String title) {
        TextView section = text(title, 10, colors.muted, true);
        section.setPadding(dp(3), dp(14), 0, dp(6));
        page.addView(section);
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        return params;
    }

    private GradientDrawable roundRect(int fill, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        if (fill != stroke) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private void addSpace(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    /** Small built-in English/Arabic phrasebook for this offline prototype. */
    private String L(String value) {
        if (!arabicLanguage || value == null) return value;
        if (value.endsWith(" m radius")) return value.substring(0, value.length() - " m radius".length()) + " م نصف قطر";
        if (value.startsWith("Restored ")) return value.replace("Restored ", "تمت استعادة وضع ")
            .replace("Normal", "عادي").replace("Vibrate", "اهتزاز").replace("Silent", "صامت")
            .replace(" · Imam ended Salat locally", " · أنهى الإمام الصلاة على هذا الهاتف")
            .replace(" · Selected Wi-Fi left range", " · خرجت شبكة واي فاي المختارة من النطاق")
            .replace(" · Geofence exited", " · خرج الهاتف من النطاق الجغرافي")
            .replace(" · Restore duration elapsed", " · انتهت مدة الاستعادة");
        if (value.startsWith("Geofence armed: ")) return value.replace("Geofence armed: ", "تم تفعيل النطاق الجغرافي: ")
            .replace("current test location", "موقع الاختبار الحالي").replace("Tarfaya Mosque", "مسجد طرفاية").replace(" m radius.", " م نصف قطر.");
        if (value.startsWith("Current location · ")) return value.replace("Current location", "الموقع الحالي")
            .replace(" · accuracy ±", " · الدقة ±").replace(" m", " م");
        if (value.startsWith("Could not refresh current location: ")) return value.replace("Could not refresh current location:", "تعذّر تحديث الموقع الحالي:");
        if (value.startsWith("Location lookup failed: ")) return value.replace("Location lookup failed:", "فشل تحديد الموقع:");
        if (value.startsWith("Location permission is missing: ")) return value.replace("Location permission is missing:", "إذن الموقع غير متاح:");
        if (value.startsWith("Geofence radius · "))
            return "نصف قطر النطاق · " + value.substring("Geofence radius · ".length()).replace(" m", " م");
        if (value.startsWith("Restore · ") && value.endsWith(" minutes after Iqama")) {
            String number = value.substring("Restore · ".length(), value.length() - " minutes after Iqama".length());
            return "استعادة الصوت بعد الإقامة بـ" + number + " دقيقة";
        }
        if (value.startsWith("Next Iqama · ")) {
            String details = value.substring("Next Iqama · ".length());
            int separator = details.indexOf(" · ");
            return separator < 0 ? "الإقامة القادمة · " + details : "الإقامة القادمة · " +
                L(details.substring(0, separator)) + " · " + details.substring(separator + 3);
        }
        if (value.startsWith("Test Iqama · ")) return "إقامة تجريبية · " + value.substring("Test Iqama · ".length());
        if (value.startsWith("Adhan ")) return "الأذان " + value.substring("Adhan ".length());
        if (value.startsWith("Iqama +")) return "الإقامة بعد " + value.substring("Iqama +".length()).replace(" min", " دقيقة");
        if (value.startsWith("Phone profile · ")) return value.replace("Phone profile · ", "ملف صوت الهاتف · ")
            .replace("Normal", "عادي").replace("Vibrate", "اهتزاز").replace("Silent", "صامت")
            .replace("restores", "يستعيد الصوت");
        if (value.startsWith("Fresh scan · ") || value.startsWith("Cached / throttled scan · ")) {
            boolean fresh = value.startsWith("Fresh scan · ");
            String details = value.substring(value.indexOf(" · ") + 3).replace(" networks · ", " شبكة · ");
            return (fresh ? "بحث حديث · " : "بحث محفوظ أو مقيّد · ") + details;
        }
        switch (value) {
            case "🇲🇦 العربية": return "🇲🇦 العربية";
            case "🇬🇧 English": return "🇬🇧 English";
            case "العربية": return "العربية";
            case "English": return "English";
            case "Home": return "الرئيسية";
            case "Setup": return "الإعدادات";
            case "Schedule": return "الجدول";
            case "Imam": return "الإمام";
            case "More": return "المزيد";
            case "Tarfaya Mosque · Meknes": return "مسجد طرفاية · مكناس";
            case "MOSQUE": return "المسجد";
            case "Selected mosque": return "المسجد المختار";
            case "Add a mosque": return "إضافة مسجد";
            case "Set geofence for selected mosque": return "تحديد نطاق المسجد المختار";
            case "Times are calculated from the selected mosque's available prayer calendar.": return "تُحسب الأوقات من تقويم الصلاة المتاح للمسجد المختار.";
            case "No Adhan calendar is configured for this mosque yet. Add its local prayer data before using automatic timing.": return "لا يوجد تقويم أذان مُعدّ لهذا المسجد بعد. أضف بياناته المحلية قبل استخدام التوقيت التلقائي.";
            case "Selected mosque location: Tarfaya, Meknes.": return "موقع المسجد المختار: طرفاية، مكناس.";
            case "Selected mosque saved. Its prayer calendar is not configured yet.": return "تم حفظ المسجد. لم يُعدّ تقويم الصلاة الخاص به بعد.";
            case "Mosque name": return "اسم المسجد";
            case "Latitude (for example 33.90)": return "خط العرض (مثال 33.90)";
            case "Longitude (for example -5.54)": return "خط الطول (مثال -5.54)";
            case "Save mosque": return "حفظ المسجد";
            case "Enter a mosque name and valid latitude and longitude.": return "أدخل اسم المسجد وخط عرض وخط طول صحيحين.";
            case "Fajer": return "الفجر";
            case "Doher": return "الظهر";
            case "Aser": return "العصر";
            case "Maghrib": return "المغرب";
            case "Isha": return "العشاء";
            case "PRAYER STATUS": return "حالة الصلاة";
            case "NEXT IQAMA": return "الإقامة القادمة";
            case "Prayer times are unavailable": return "مواقيت الصلاة غير متاحة";
            case "Silence begins 30 seconds before Iqama": return "يبدأ الصامت قبل الإقامة بـ30 ثانية";
            case "STATUS KEY": return "دليل الحالات";
            case "Ready": return "جاهز";
            case "Waiting": return "انتظار";
            case "Not detected": return "غير مكتشف";
            case "WI-FI": return "واي فاي";
            case "GEOFENCE": return "النطاق الجغرافي";
            case "PRESENT": return "متصل";
            case "NOT DETECTED": return "غير مكتشف";
            case "NOT SET": return "غير محدد";
            case "INSIDE": return "داخل النطاق";
            case "OUTSIDE": return "خارج النطاق";
            case " m radius": return " م نصف قطر";
            case "COUNTDOWN TO IQAMA": return "العد التنازلي للإقامة";
            case "NEXT IQAMA AFTER THIS SALAT": return "الإقامة التالية بعد هذه الصلاة";
            case "●  SALAT ACTIVE · SILENT": return "●  الصلاة جارية · الهاتف صامت";
            case "●  PRESENCE READY · WAITING FOR IQAMA": return "●  الموقع جاهز · بانتظار الإقامة";
            case "●  WAITING FOR CONDITIONS": return "●  بانتظار تحقق الشروط";
            case "Iqama schedule unavailable": return "جدول الإقامة غير متاح";
            case "Selected mosque network": return "شبكة المسجد المختارة";
            case "Choose the Wi-Fi and location conditions for this phone.": return "اختر شبكة واي فاي وموقع هذا الهاتف.";
            case "WI-FI PRESENCE": return "التواجد عبر واي فاي";
            case "Refresh networks to choose an SSID": return "حدّث الشبكات لاختيار شبكة واي فاي";
            case "No scan yet": return "لم يتم البحث بعد";
            case "Refresh Wi-Fi networks": return "تحديث شبكات واي فاي";
            case "Refresh geofence": return "تحديث النطاق الجغرافي";
            case "Signal": return "الإشارة";
            case "LOCATION PRESENCE": return "التواجد بالموقع";
            case "Location has not been refreshed": return "لم يتم تحديث الموقع بعد";
            case "Refresh current location": return "تحديث الموقع الحالي";
            case "Use current location as test-zone center": return "استخدام الموقع الحالي مركزاً للنطاق التجريبي";
            case "Use Tarfaya Mosque geofence": return "استخدام نطاق مسجد طرفاية";
            case "ACTIVATION RULE": return "قاعدة التفعيل";
            case "Selected Wi-Fi + inside the geofence + within the prayer window": return "شبكة واي فاي المختارة + داخل النطاق + ضمن وقت الصلاة";
            case "Both presence signals are required. This device scans Wi-Fi and refreshes its geofence from here.": return "يلزم تحقق علامتي التواجد معاً. يبحث هذا الهاتف عن الشبكة ويحدّث موقع النطاق من هنا.";
            case "Iqama schedule": return "جدول الإقامة";
            case "Times are calculated from the Tarfaya Adhan calendar and your offsets.": return "تُحسب الأوقات من تقويم أذان طرفاية والفواصل المحددة.";
            case "TODAY · ADHAN → IQAMA": return "اليوم · الأذان ← الإقامة";
            case "SILENT WINDOW": return "فترة الصمت";
            case "Start · 30 seconds before Iqama": return "البدء · قبل الإقامة بـ30 ثانية";
            case "Restore · 10 minutes after Iqama": return "استعادة الصوت بعد الإقامة بـ10 دقائق";
            case "Restore duration can be set from 1 to 30 minutes. Default: 10 minutes.": return "يمكن ضبط مدة الاستعادة من دقيقة إلى 30 دقيقة. الافتراضي: 10 دقائق.";
            case "DEVICE TEST": return "اختبار الهاتف";
            case "Test in 30 sec": return "اختبار بعد 30 ثانية";
            case "Test in 90 sec": return "اختبار بعد 90 ثانية";
            case "Imam dashboard": return "لوحة الإمام";
            case "Local controls and this phone's activity.": return "عناصر التحكم المحلية ونشاط هذا الهاتف.";
            case "No active local session": return "لا توجد جلسة محلية نشطة";
            case "●  SALAT ACTIVE ON THIS PHONE": return "●  الصلاة جارية على هذا الهاتف";
            case "●  NO ACTIVE LOCAL SESSION": return "●  لا توجد جلسة محلية نشطة";
            case "END SALAT": return "إنهاء الصلاة";
            case "Restore this phone's previous sound profile.": return "استعادة وضع الصوت السابق لهذا الهاتف.";
            case "End Salat · Restore Sound": return "إنهاء الصلاة · استعادة الصوت";
            case "This action affects this phone only.": return "يؤثر هذا الإجراء على هذا الهاتف فقط.";
            case "LOCAL COUNTERS · THIS PHONE": return "عدادات محلية · هذا الهاتف";
            case "Silence sessions": return "جلسات الصمت";
            case "Imam releases": return "مرات إنهاء الإمام";
            case "ATTENDEES": return "المصلون";
            case "Connected attendees": return "المصلون المتصلون";
            case "Live attendee data is not connected. The mosque-wide roster and release require the authenticated backend and attendee clients.": return "بيانات المصلين غير متصلة. تتطلب القائمة والتحكم على مستوى المسجد خادماً موثّقاً وتطبيقات متصلة.";
            case "Appearance, permissions and test controls.": return "المظهر والأذونات وعناصر الاختبار.";
            case "LANGUAGE": return "اللغة";
            case "App language": return "لغة التطبيق";
            case "APPEARANCE": return "المظهر";
            case "Theme": return "السمة";
            case "Follow device": return "اتباع إعداد الجهاز";
            case "Light": return "فاتح";
            case "Dark": return "داكن";
            case "TEST OVERRIDES": return "إعدادات اختبار مؤقتة";
            case "Simulate Wi-Fi presence": return "محاكاة وجود شبكة واي فاي";
            case "Simulate inside geofence": return "محاكاة الوجود داخل النطاق";
            case "Overrides are for local testing and do not represent mosque-wide attendance.": return "هذه الإعدادات للاختبار المحلي ولا تمثل حضور المسجد كله.";
            case "DEVICE ACCESS": return "إذن الهاتف";
            case "Sound & Do Not Disturb access": return "إذن الصوت وعدم الإزعاج";
            case "Review app permissions": return "مراجعة أذونات التطبيق";
            case "Iqama times are not available for this date.": return "مواقيت الإقامة غير متاحة لهذا التاريخ.";
            case "Turn on Android Location, then refresh again.": return "فعّل الموقع على Android ثم حدّث مرة أخرى.";
            case "Scanning visible networks…": return "جارٍ البحث عن الشبكات الظاهرة…";
            case "No networks found": return "لم يتم العثور على شبكات";
            case "Select a Wi-Fi network": return "اختر شبكة واي فاي";
            case "Wi-Fi list refreshed": return "تم تحديث قائمة واي فاي";
            case "Refreshing current location…": return "جارٍ تحديث الموقع الحالي…";
            case "Location and geofence refreshed": return "تم تحديث الموقع والنطاق الجغرافي";
            case "Grant precise location to refresh the current position.": return "اسمح بالموقع الدقيق لتحديث موقعك الحالي.";
            case "Current location is unavailable. Turn on Android Location and refresh again.": return "الموقع الحالي غير متاح. فعّل الموقع على Android ثم حدّث مرة أخرى.";
            case "Location permission is missing.": return "إذن الموقع غير متاح.";
            case "Grant precise location first.": return "اسمح بالموقع الدقيق أولاً.";
            case "Allow background location": return "السماح بالموقع في الخلفية";
            case "Open app settings": return "فتح إعدادات التطبيق";
            case "Cancel": return "إلغاء";
            case "Open Sound access to enable automatic silence.": return "افتح إذن الصوت لتفعيل الصمت التلقائي.";
            case "Silent mode could not be applied on this phone.": return "تعذّر تفعيل الوضع الصامت على هذا الهاتف.";
            case "Silent mode on · previous profile saved": return "تم تفعيل الصامت · حُفظ وضع الصوت السابق";
            case "Restore pending · Sound access is required.": return "الاستعادة معلّقة · يلزم إذن الصوت.";
            case "Imam ended Salat locally": return "أنهى الإمام الصلاة على هذا الهاتف";
            case "Selected Wi-Fi left range": return "خرجت شبكة واي فاي المختارة من النطاق";
            case "Geofence exited": return "خرج الهاتف من النطاق الجغرافي";
            case "Restore duration elapsed": return "انتهت مدة الاستعادة";
            case "Choose Allow all the time in app location settings so geofence enter and exit events can arrive while the app is closed.": return "اختر السماح طوال الوقت من إعدادات الموقع حتى تصل أحداث دخول النطاق والخروج منه والتطبيق مغلق.";
            default: return value.replace("Tarfaya Mosque", "مسجد طرفاية").replace("Meknes", "مكناس").replace("Fajer", "الفجر")
                .replace("Doher", "الظهر").replace("Aser", "العصر").replace("Maghrib", "المغرب")
                .replace("Isha", "العشاء").replace("Iqama", "الإقامة").replace("Adhan", "الأذان");
        }
    }

    private int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private boolean isNightMode() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private String timeOf(long timestamp) {
        return timestamp <= 0 ? "--:--" : new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private String formatMs(long millis) {
        long total = Math.max(0L, millis / 1000L);
        long hours = total / 3600L;
        long minutes = (total % 3600L) / 60L;
        long seconds = total % 60L;
        return hours > 0 ? String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds) :
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
        if (hasWifiPermissions()) scanWifi(false);
        refreshScheduleRows();
        schedulePreIqamaScan();
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
        wifiScanner.unregister();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        wifiScanner.unregister();
    }

    private static final class UiPalette {
        final int background, surface, text, muted, border;
        final int primaryAction, onPrimary, hero, heroMuted, green;
        final int good, goodSoft, waiting, waitingSoft, bad, badSoft, buttonSoft;

        UiPalette(boolean dark) {
            background = dark ? Color.rgb(12, 23, 28) : Color.rgb(243, 247, 246);
            surface = dark ? Color.rgb(20, 36, 43) : Color.WHITE;
            text = dark ? Color.rgb(229, 240, 239) : Color.rgb(23, 48, 58);
            muted = dark ? Color.rgb(154, 173, 177) : Color.rgb(99, 118, 123);
            border = dark ? Color.rgb(39, 58, 65) : Color.rgb(223, 233, 230);
            primaryAction = dark ? Color.rgb(25, 101, 84) : Color.rgb(16, 56, 70);
            onPrimary = Color.WHITE;
            hero = Color.rgb(16, 56, 70);
            heroMuted = Color.rgb(192, 222, 215);
            green = dark ? Color.rgb(87, 210, 171) : Color.rgb(18, 145, 108);
            good = dark ? Color.rgb(87, 210, 171) : Color.rgb(7, 125, 90);
            goodSoft = dark ? Color.rgb(24, 61, 53) : Color.rgb(226, 245, 237);
            waiting = dark ? Color.rgb(242, 190, 89) : Color.rgb(155, 103, 12);
            waitingSoft = dark ? Color.rgb(59, 48, 29) : Color.rgb(251, 243, 222);
            bad = dark ? Color.rgb(255, 133, 121) : Color.rgb(173, 62, 55);
            badSoft = dark ? Color.rgb(62, 38, 39) : Color.rgb(251, 233, 230);
            buttonSoft = dark ? Color.rgb(33, 53, 61) : Color.rgb(231, 240, 237);
        }
    }
}
