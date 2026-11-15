package org.mosqueautosilent.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/** Restores an interrupted local session after the phone restarts. */
public final class BootRestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        SharedPreferences prefs = context.getSharedPreferences(GeofenceReceiver.PREFS, Context.MODE_PRIVATE);
        boolean restored = new AudioProfileController(context).restore();
        if (restored) prefs.edit().putBoolean("mosque_active", false).commit();
    }
}
