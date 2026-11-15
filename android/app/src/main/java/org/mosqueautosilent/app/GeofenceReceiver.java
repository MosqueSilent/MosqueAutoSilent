package org.mosqueautosilent.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/** Persists ENTER/EXIT state so the foreground prototype can evaluate the AND rule. */
public class GeofenceReceiver extends BroadcastReceiver {
    public static final String PREFS = "mosque_auto_silent";
    public static final String KEY_INSIDE = "inside_geofence";

    @Override public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) return;
        int transition = event.getGeofenceTransition();
        boolean inside;
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) inside = true;
        else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) inside = false;
        else return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_INSIDE, inside).commit();
        // A geofence exit is a safety restore and must work even when the UI is
        // not running. Enter events only update presence; activation remains local
        // to the foreground prototype where SSID and timing are evaluated.
        if (!inside && prefs.getBoolean("mosque_active", false)) {
            boolean restored = new AudioProfileController(context).restore();
            if (restored) prefs.edit().putBoolean("mosque_active", false).commit();
        }
    }
}
