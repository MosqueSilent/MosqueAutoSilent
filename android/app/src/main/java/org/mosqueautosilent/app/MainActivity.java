package org.mosqueautosilent.app;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int LEAD_SECONDS = 30;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences store;
    private WifiScanner wifiScanner;
    private GeofenceManager geofenceManager;
    private AudioProfileController audioController;
    private Spinner ssidSpinner;
    private TextView statusText, wifiText, geoText, prayerText, profileText, timerText, durationText, scanText;
    private SwitchCompat demoWifiSwitch, demoGeoSwitch;
    private SeekBar durationSeek;
    private Button endPrayerButton;
    private List<String> ssids = new ArrayList<>();
    private String selectedSsid = "";
    private boolean wifiInRange = false;
    private boolean manualWifiOverride = false;
    private boolean demoInside = false;
    private boolean active = false;
    private long iqamaAt = 0L;
    private long lastFreshScan = 0L;
    private int durationMinutes = PrayerWindow.DEFAULT_DURATION_MINUTES;
    private final Runnable ticker = new Runnable() {
        @Override public void run() { evaluateState(); handler.postDelayed(this, 1000L); }
    };
    private final Runnable scanAtIqama = new Runnable() {
        @Override public void run() { if (hasWifiPermissions()) scanWifi(true); }
    };
    private int geofenceRadiusMetres = 100;
    private TextView geofenceRadiusText;
    private ActivityResultLauncher<String[]> permissionRequest;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = getSharedPreferences(GeofenceReceiver.PREFS, MODE_PRIVATE);
        wifiScanner = new WifiScanner(this);
        geofenceManager = new GeofenceManager(this);
        audioController = new AudioProfileController(this);
        selectedSsid = store.getString("selected_ssid", "");
        demoInside = store.getBoolean(GeofenceReceiver.KEY_INSIDE, false);
        durationMinutes = store.getInt("duration_minutes", PrayerWindow.DEFAULT_DURATION_MINUTES);
        iqamaAt = store.getLong("test_iqama_at", 0L);
        active = store.getBoolean("mosque_active", false) || audioController.hasPendingRestore();
        geofenceRadiusMetres = store.getInt("geofence_radius", 100);
        permissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (hasWifiPermissions()) scanWifi(true);
        });
        buildUi();
        schedulePreIqamaScan();
        evaluateState();
    }

    private void buildUi() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(Color.rgb(244,247,250));
        shell.setPadding(dp(14), dp(12), dp(14), dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("🕌  Mosque Auto-Silent", 22, Color.rgb(16,43,70), true);
        header.addView(title);
        header.addView(text("Tarfaya Mosque · prototype pilot", 12, Color.rgb(98,117,135), false));
        shell.addView(header);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setPadding(0, dp(10), 0, dp(10));
        String[] labels = {"Home", "Setup", "Schedule", "Admin"};
        FrameLayout content = new FrameLayout(this);
        LinearLayout.LayoutParams contentLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        shell.addView(nav);
        shell.addView(content, contentLp);
        String[] pageNames = {"Home", "Setup", "Schedule", "Admin"};
        ScrollView[] pages = new ScrollView[4];
        for (int i = 0; i < pages.length; i++) {
            LinearLayout pageBody = new LinearLayout(this);
            pageBody.setOrientation(LinearLayout.VERTICAL);
            pageBody.setPadding(dp(3), dp(4), dp(3), dp(22));
            pages[i] = new ScrollView(this);
            pages[i].setFillViewport(false);
            pages[i].addView(pageBody);
            content.addView(pages[i], new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            pages[i].setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        }
        Button[] navButtons = new Button[4];
        for (int i = 0; i < 4; i++) {
            final int pageIndex = i;
            navButtons[i] = button(labels[i], i == 0);
            navButtons[i].setTextSize(11);
            LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, dp(42), 1f);
            nlp.setMargins(dp(2), 0, dp(2), 0);
            nav.addView(navButtons[i], nlp);
            navButtons[i].setOnClickListener(v -> {
                for (int j = 0; j < pages.length; j++) {
                    pages[j].setVisibility(j == pageIndex ? View.VISIBLE : View.GONE);
                    navButtons[j].setBackgroundTintList(android.content.res.ColorStateList.valueOf(j == pageIndex ? Color.rgb(16,43,70) : Color.rgb(229,237,244)));
                    navButtons[j].setTextColor(j == pageIndex ? Color.WHITE : Color.rgb(16,43,70));
                }
            });
        }
        LinearLayout home = (LinearLayout) pages[0].getChildAt(0);
        LinearLayout setup = (LinearLayout) pages[1].getChildAt(0);
        LinearLayout schedule = (LinearLayout) pages[2].getChildAt(0);
        LinearLayout admin = (LinearLayout) pages[3].getChildAt(0);

        statusText = text("WAITING FOR CONDITIONS", 17, Color.rgb(16,43,70), true); addCard(home, statusText);
        profileText = text("Sound profile: " + AudioProfileController.modeName(audioController.currentMode()), 14, Color.rgb(25,62,95), true); home.addView(profileText);
        addSection(home, "ACTIVATION CHECK");
        wifiText = text("Wi-Fi in range: NO", 14, Color.DKGRAY, true); addCard(home, wifiText);
        geoText = text("Geofence: OUTSIDE", 14, Color.DKGRAY, true); addCard(home, geoText);
        prayerText = text("No test Iqama scheduled", 12, Color.GRAY, false); home.addView(prayerText);
        timerText = text("--:--", 30, Color.rgb(16,43,70), true); addCard(home, timerText);
        endPrayerButton = button("IMAM: END PRAYER NOW · RESTORE SOUND", true); home.addView(endPrayerButton);
        endPrayerButton.setOnClickListener(v -> restoreSound("Imam ended prayer manually"));
        home.addView(text("Automatic activation requires Wi-Fi in range + inside geofence + prayer window. This build is a local device prototype; mosque-wide release needs a deployed server.", 12, Color.rgb(98,117,135), false));

        addSection(setup, "1 · SELECT A VISIBLE WI-FI NETWORK");
        Button scan = button("Scan nearby Wi-Fi", true); setup.addView(scan); scan.setOnClickListener(v -> ensureWifiPermissionsAndScan());
        ssidSpinner = new Spinner(this); setup.addView(ssidSpinner);
        scanText = text("No scan yet", 12, Color.GRAY, false); setup.addView(scanText);
        demoWifiSwitch = new SwitchCompat(this); demoWifiSwitch.setText("Demo: simulate selected Wi-Fi in range"); setup.addView(demoWifiSwitch);
        demoWifiSwitch.setOnCheckedChangeListener((button, checked) -> { manualWifiOverride = checked; evaluateState(); });

        addSection(setup, "2 · MOSQUE GEOFENCE");
        setup.addView(text("Tarfaya Mosque · زنقة طرفاية حمرية 50000 مكناس Morocco", 13, Color.rgb(25,62,95), true));
        setup.addView(text("33.90066, -5.54761 · approximate mapped centre", 12, Color.GRAY, false));
        geofenceRadiusText = text("Geofence radius: " + geofenceRadiusMetres + " metres", 13, Color.rgb(25,62,95), true); setup.addView(geofenceRadiusText);
        SeekBar radiusSeek = new SeekBar(this); radiusSeek.setMax(350); radiusSeek.setProgress(geofenceRadiusMetres - 50); setup.addView(radiusSeek);
        radiusSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { geofenceRadiusMetres = progress + 50; geofenceRadiusText.setText("Geofence radius: " + geofenceRadiusMetres + " metres"); store.edit().putInt("geofence_radius", geofenceRadiusMetres).apply(); }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        Button armTarfaya = button("Set Tarfaya Mosque geofence", true); setup.addView(armTarfaya);
        armTarfaya.setOnClickListener(v -> ensureLocationForTarfayaGeofence());
        Button armCurrent = button("Use my current location (coffee-shop test)", false); setup.addView(armCurrent);
        armCurrent.setOnClickListener(v -> ensureLocationForGeofence());
        demoGeoSwitch = new SwitchCompat(this); demoGeoSwitch.setText("Demo: simulate inside test zone"); setup.addView(demoGeoSwitch);
        demoGeoSwitch.setChecked(demoInside);
        demoGeoSwitch.setOnCheckedChangeListener((button, checked) -> { demoInside = checked; store.edit().putBoolean(GeofenceReceiver.KEY_INSIDE, checked).apply(); evaluateState(); });

        addSection(schedule, "PRAYER TIMING");
        schedule.addView(text("L = 30 seconds before Iqama (fixed)", 14, Color.rgb(25,62,95), true));
        durationText = text("D = " + durationMinutes + " minutes after Iqama", 14, Color.rgb(25,62,95), true); schedule.addView(durationText);
        durationSeek = new SeekBar(this); durationSeek.setMax(29); durationSeek.setProgress(durationMinutes-1); schedule.addView(durationSeek);
        durationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { durationMinutes=progress+1; durationText.setText("D = " + durationMinutes + " minutes after Iqama"); store.edit().putInt("duration_minutes", durationMinutes).apply(); evaluateState(); }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        Button iqama30 = button("Set test Iqama in 30 seconds", true); schedule.addView(iqama30);
        iqama30.setOnClickListener(v -> setTestIqama(30_000L));
        Button iqama90 = button("Set test Iqama in 90 seconds", false); schedule.addView(iqama90);
        iqama90.setOnClickListener(v -> setTestIqama(90_000L));
        Button scanNow = button("Schedule Wi-Fi scan 1 minute before Iqama", false); schedule.addView(scanNow);
        scanNow.setOnClickListener(v -> { schedulePreIqamaScan(); Toast.makeText(this, "Pre-Iqama scan scheduled", Toast.LENGTH_SHORT).show(); });
        schedule.addView(text("Wi-Fi scan strategy: do not continuously scan. Schedule a scan at Iqama − 60 seconds, then evaluate the 30-second silence lead. Android may throttle scans; a fresh result is not guaranteed.", 12, Color.rgb(98,117,135), false));
        schedule.addView(text("The supplied 2026 MAWAQIT PDF contains prayer-time columns, not Iqama. Use a manual test Iqama until Tarfaya confirms its congregation schedule.", 12, Color.rgb(134,93,25), false));

        addSection(admin, "ADMINISTRATION");
        admin.addView(text("Mosque: مسجد طرفاية - مكناس", 15, Color.rgb(16,43,70), true));
        admin.addView(text("MAWAQIT mosque ID: 5655", 12, Color.GRAY, false));
        admin.addView(text("Default lead time L: 30 seconds", 13, Color.rgb(25,62,95), true));
        admin.addView(text("Default duration D: 10 minutes (adjustable on Schedule)", 13, Color.rgb(25,62,95), true));
        Button access = button("Grant sound / Do Not Disturb access", false); admin.addView(access);
        access.setOnClickListener(v -> startActivity(audioController.policyAccessSettingsIntent()));
        Button permissions = button("Review app permissions", false); admin.addView(permissions);
        permissions.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:"+getPackageName()))));
        admin.addView(text("Android displays sensitive permissions as separate runtime prompts and settings screens; they cannot all be silently granted during installation. The app requests each permission when its feature is configured.", 12, Color.rgb(98,117,135), false));
        admin.addView(text("Imam release is local to this phone in this prototype. A mosque-wide Imam command requires a secure backend and attendee registration.", 12, Color.rgb(98,117,135), false));

        setContentView(shell);
    }

    private void setTestIqama(long delayMs) {
        iqamaAt = System.currentTimeMillis() + delayMs;
        store.edit().putLong("test_iqama_at", iqamaAt).remove("released_iqama_at").apply();
        schedulePreIqamaScan();
        evaluateState();
    }

    private void schedulePreIqamaScan() {
        handler.removeCallbacks(scanAtIqama);
        if (iqamaAt <= 0) return;
        long scanAt = iqamaAt - 60_000L;
        long delay = Math.max(0L, scanAt - System.currentTimeMillis());
        handler.postDelayed(scanAtIqama, delay);
    }

    private void ensureWifiPermissionsAndScan() {
        List<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        if (!missing.isEmpty()) permissionRequest.launch(missing.toArray(new String[0])); else scanWifi(true);
    }

    private boolean hasWifiPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void scanWifi(boolean showToast) {
        boolean locationOn = false;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm != null) {
            if (Build.VERSION.SDK_INT >= 28) locationOn = lm.isLocationEnabled();
            else locationOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        if (!locationOn) { scanText.setText("Turn on Android Location to scan visible SSIDs."); return; }
        scanText.setText("Scanning… Android may throttle scans; stale results are not treated as fresh.");
        wifiScanner.requestScan((found, fresh) -> runOnUiThread(() -> {
            ssids = found;
            List<String> choices = new ArrayList<>();
            choices.add(ssids.isEmpty() ? "No SSIDs found" : "Select a Wi-Fi network");
            choices.addAll(ssids);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); ssidSpinner.setAdapter(adapter);
            int index = ssids.indexOf(selectedSsid); ssidSpinner.setSelection(index >= 0 ? index + 1 : 0);
            if (fresh) {
                lastFreshScan = System.currentTimeMillis();
                wifiInRange = !selectedSsid.isEmpty() && ssids.contains(selectedSsid);
            }
            scanText.setText((fresh?"Fresh":"Cached / throttled") + " scan · " + ssids.size() + " SSID(s) · " + new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
            evaluateState();
            if (showToast) Toast.makeText(this, "Found " + ssids.size() + " network(s)", Toast.LENGTH_SHORT).show();
        }));
    }

    private void ensureLocationForGeofence() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}); return;
        }
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Allow background location")
                .setMessage("Android geofencing needs ‘Allow all the time’ location access so enter/exit events can arrive when the app is not open. Enable it in App permissions, then tap this button again.")
                .setPositiveButton("Open app settings", (d,w) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:"+getPackageName()))))
                .setNegativeButton("Cancel", null).show(); return;
        }
        geofenceManager.armAtCurrentLocation(geofenceRadiusMetres, (ok, message) -> runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show()));
    }

    private void ensureLocationForTarfayaGeofence() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}); return;
        }
        if (Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Allow background location")
                .setMessage("Choose Allow all the time in Android app location settings so geofence events can arrive while the app is closed.")
                .setPositiveButton("Open app settings", (d,w) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:"+getPackageName()))))
                .setNegativeButton("Cancel", null).show(); return;
        }
        geofenceManager.armAtCoordinates(33.90066, -5.54761, geofenceRadiusMetres, "Tarfaya Mosque, Meknes", (ok, message) -> runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show()));
    }

    private boolean effectiveWifiPresent() {
        if (manualWifiOverride) return true;
        if (selectedSsid.isEmpty()) return false;
        // A selected SSID counts only when confirmed by a recent scan (within 2 minutes).
        // Latch the last fresh scan result through the short prayer session. Do not
        // treat scan throttling or a cached result as proof that the AP disappeared.
        return lastFreshScan > 0 && ssids.contains(selectedSsid) &&
            (active || System.currentTimeMillis() - lastFreshScan <= 120_000L);
    }

    private boolean effectiveInside() { return demoInside || store.getBoolean(GeofenceReceiver.KEY_INSIDE, false); }

    private void evaluateState() {
        if (ssidSpinner != null && ssidSpinner.getSelectedItem() != null &&
            !"No SSIDs found".equals(ssidSpinner.getSelectedItem().toString()) &&
            !"Select a Wi-Fi network".equals(ssidSpinner.getSelectedItem().toString())) {
            String candidate = ssidSpinner.getSelectedItem().toString();
            if (!candidate.equals(selectedSsid)) { selectedSsid=candidate; store.edit().putString("selected_ssid", selectedSsid).apply(); }
        } else if (ssidSpinner != null && ssidSpinner.getSelectedItem() != null && selectedSsid.isEmpty()) {
            store.edit().putString("selected_ssid", "").apply();
        }
        boolean wifi = effectiveWifiPresent();
        boolean inside = effectiveInside();
        long now = System.currentTimeMillis();
        long start = iqamaAt > 0 ? PrayerWindow.silenceStartsAt(iqamaAt) : Long.MAX_VALUE;
        long end = iqamaAt > 0 ? PrayerWindow.restoresAt(iqamaAt, durationMinutes) : 0L;
        boolean prayerWindow = iqamaAt > 0 && now >= start && now < end;
        long releasedFor = store.getLong("released_iqama_at", 0L);
        if (!active && wifi && inside && prayerWindow && releasedFor != iqamaAt) activateSound();
        if (active && (!wifi || !inside)) restoreSound(!wifi ? "Wi-Fi signal lost" : "Geofence exited");
        if (active && now >= end) restoreSound("Automatic duration D elapsed");
        if (wifiText != null) wifiText.setText("Wi-Fi in range: " + (wifi?"YES":"NO") + (selectedSsid.isEmpty()?"":" · "+selectedSsid));
        if (geoText != null) geoText.setText("Geofence: " + (inside?"INSIDE":"OUTSIDE"));
        if (prayerText != null) prayerText.setText(iqamaAt>0?"Test Iqama: "+new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(iqamaAt))+" · silence starts "+new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(start)):"No test Iqama scheduled");
        if (statusText != null) statusText.setText(active?"MOSQUE MODE ACTIVE · SILENT":(wifi&&inside?"PRESENCE CONFIRMED · WAITING FOR PRAYER":"WAITING FOR CONDITIONS"));
        if (profileText != null) profileText.setText("Sound profile: "+AudioProfileController.modeName(audioController.currentMode()));
        if (timerText != null) {
            long remaining = active?end-now:(iqamaAt>0?Math.max(0,start-now):0);
            timerText.setText(active?"Restore in "+formatMs(remaining):iqamaAt>0?"Silent in "+formatMs(remaining):"--:--");
        }
        if (endPrayerButton != null) endPrayerButton.setEnabled(active);
    }

    private void activateSound() {
        if (!audioController.hasPolicyAccess()) {
            Toast.makeText(this, "Grant Notification Policy Access first; no sound setting was changed.", Toast.LENGTH_LONG).show();
            startActivity(audioController.policyAccessSettingsIntent()); return;
        }
        boolean ok = audioController.silenceAndSave();
        if (ok) { active = true; store.edit().putBoolean("mosque_active", true).apply(); Toast.makeText(this, "Mosque Mode active · saved previous profile", Toast.LENGTH_LONG).show(); }
        else Toast.makeText(this, "Could not activate silent mode on this device.", Toast.LENGTH_LONG).show();
    }

    private void restoreSound(String reason) {
        if (!active) return;
        if (reason.toLowerCase(Locale.ROOT).contains("imam")) store.edit().putLong("released_iqama_at", iqamaAt).apply();
        boolean restored = audioController.restore();
        if (restored) { active=false; store.edit().putBoolean("mosque_active", false).apply(); Toast.makeText(this, "Restored previous profile · "+reason, Toast.LENGTH_LONG).show(); }
        else Toast.makeText(this, "Restore pending: Notification Policy Access is required.", Toast.LENGTH_LONG).show();
    }

    private String formatMs(long ms) {
        long seconds=Math.max(0,ms/1000); long hours=seconds/3600; long minutes=(seconds%3600)/60; long secs=seconds%60;
        return hours>0?String.format(Locale.getDefault(),"%02d:%02d:%02d",hours,minutes,secs):String.format(Locale.getDefault(),"%02d:%02d",minutes,secs);
    }
    private TextView text(String value, int size, int color, boolean bold) { TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setPadding(0,dp(5),0,dp(5));return v; }
    private Button button(String label, boolean primary) { Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(primary?Color.WHITE:Color.rgb(16,43,70));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?Color.rgb(16,43,70):Color.rgb(229,237,244)));return b; }
    private void addSection(LinearLayout root,String label) { TextView t=text(label,12,Color.rgb(98,117,135),true);t.setPadding(0,dp(18),0,dp(7));root.addView(t); }
    private void addCard(LinearLayout root,View view) { LinearLayout card=new LinearLayout(this);card.setPadding(dp(13),dp(12),dp(13),dp(12));card.setBackgroundColor(Color.WHITE);card.addView(view);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(10);root.addView(card,lp); }
    private void addSpace(LinearLayout root,int height){View v=new View(this);root.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}

    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(ticker); handler.post(ticker); if (hasWifiPermissions() && iqamaAt > 0 && Math.abs(iqamaAt - System.currentTimeMillis()) < 90_000L) scanWifi(false); schedulePreIqamaScan(); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(ticker); wifiScanner.unregister(); }
    @Override protected void onDestroy() { super.onDestroy(); handler.removeCallbacksAndMessages(null); wifiScanner.unregister(); }
}
