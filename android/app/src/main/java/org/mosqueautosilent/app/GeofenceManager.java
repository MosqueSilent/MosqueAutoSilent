package org.mosqueautosilent.app;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import java.util.Locale;

/** Registers one circular geofence around the device's current location for a coffee-shop test. */
public final class GeofenceManager {
    public interface Callback { void onResult(boolean ok, String message); }
    private final Context context;
    private final GeofencingClient client;
    private PendingIntent pendingIntent;

    public GeofenceManager(Context context) {
        this.context = context.getApplicationContext();
        this.client = LocationServices.getGeofencingClient(context);
    }

    /** Reads a fresh foreground location without moving the configured geofence. */
    public void refreshCurrentLocation(Callback callback) {
        boolean fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine) { callback.onResult(false, "Grant precise location to refresh the current position."); return; }
        try {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        callback.onResult(false, "Current location is unavailable. Turn on Android Location and refresh again.");
                        return;
                    }
                    callback.onResult(true, String.format(Locale.getDefault(),
                        "Current location · %.5f, %.5f · accuracy ±%.0f m",
                        location.getLatitude(), location.getLongitude(), location.getAccuracy()));
                })
                .addOnFailureListener(error -> callback.onResult(false, "Could not refresh current location: " + error.getMessage()));
        } catch (SecurityException ex) {
            callback.onResult(false, "Location permission is missing.");
        }
    }

    public void armAtCurrentLocation(float radiusMetres, Callback callback) {
        boolean fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean background = Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine) { callback.onResult(false, "Grant precise location first."); return; }
        if (!background) { callback.onResult(false, "Grant Allow all the time location access for background geofence events."); return; }
        try {
            LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location == null) { callback.onResult(false, "Could not get current location. Turn on Location and try again."); return; }
                    armAtCoordinates(location.getLatitude(), location.getLongitude(), radiusMetres, "current test location", callback);
                })
                .addOnFailureListener(error -> callback.onResult(false, "Location lookup failed: " + error.getMessage()));
        } catch (SecurityException ex) { callback.onResult(false, "Location permission is missing: " + ex.getMessage()); }
    }

    public void armAtCoordinates(double latitude, double longitude, float radiusMetres, String label, Callback callback) {
        boolean fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean background = Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine) { callback.onResult(false, "Grant precise location first."); return; }
        if (!background) { callback.onResult(false, "Grant Allow all the time location access for background geofence events."); return; }
        try {
            Geofence geofence = new Geofence.Builder()
                .setRequestId("MOSQUE_TEST_ZONE")
                .setCircularRegion(latitude, longitude, radiusMetres)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(0)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();
            GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build();
            try {
                client.addGeofences(request, getPendingIntent())
                    .addOnSuccessListener(unused -> callback.onResult(true,
                        "Geofence armed: " + label + " · " + latitude + ", " + longitude + " · " + radiusMetres + " m radius."))
                    .addOnFailureListener(error -> callback.onResult(false, "Geofence registration failed: " + error.getMessage()));
            } catch (SecurityException ex) { callback.onResult(false, "Location permission is missing: " + ex.getMessage()); }
        } catch (IllegalArgumentException ex) { callback.onResult(false, "Invalid geofence settings: " + ex.getMessage()); }
    }

    private PendingIntent getPendingIntent() {
        if (pendingIntent != null) return pendingIntent;
        Intent intent = new Intent(context, GeofenceReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
        pendingIntent = PendingIntent.getBroadcast(context, 3011, intent, flags);
        return pendingIntent;
    }
}
