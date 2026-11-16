package org.mosqueautosilent.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.core.content.ContextCompat;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Foreground/on-demand SSID scan helper. Android can throttle or return cached results. */
public final class WifiScanner {
    public interface Callback { void onResults(List<Network> networks, boolean fresh); }

    public static final class Network {
        public final String ssid;
        public final int rssiDbm;
        public final int signalPercent;

        Network(String ssid, int rssiDbm) {
            this.ssid = ssid;
            this.rssiDbm = rssiDbm;
            this.signalPercent = Math.max(0, Math.min(100, Math.round(2f * (rssiDbm + 100))));
        }
    }
    private final Context context;
    private final WifiManager wifi;
    private BroadcastReceiver receiver;

    public WifiScanner(Context context) {
        this.context = context;
        this.wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public boolean requestScan(Callback callback) {
        if (wifi == null || !wifi.isWifiEnabled()) { callback.onResults(Collections.emptyList(), false); return false; }
        unregister();
        receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent intent) {
                boolean fresh = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                callback.onResults(readResults(), fresh);
                unregister();
            }
        };
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        boolean requested;
        try { requested = wifi.startScan(); }
        catch (SecurityException ex) { unregister(); callback.onResults(Collections.emptyList(), false); return false; }
        if (!requested) {
            // Return the last available results, clearly marked as potentially cached.
            callback.onResults(readResults(), false);
            unregister();
        }
        return requested;
    }

    private List<Network> readResults() {
        Map<String, Network> strongest = new HashMap<>();
        try {
            for (ScanResult item : wifi.getScanResults()) {
                String ssid = item.SSID == null ? "" : item.SSID.trim();
                if (ssid.isEmpty() || ssid.equals("<unknown ssid>")) continue;
                Network prior = strongest.get(ssid);
                if (prior == null || item.level > prior.rssiDbm) strongest.put(ssid, new Network(ssid, item.level));
            }
        } catch (SecurityException ignored) { }
        List<Network> result = new ArrayList<>(strongest.values());
        Collections.sort(result, (left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.ssid, right.ssid));
        return result;
    }

    public void unregister() {
        if (receiver != null) {
            try { context.unregisterReceiver(receiver); } catch (IllegalArgumentException ignored) { }
            receiver = null;
        }
    }
}
