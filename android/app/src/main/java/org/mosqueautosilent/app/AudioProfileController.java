package org.mosqueautosilent.app;

import android.content.Context;
import android.media.AudioManager;
import android.app.NotificationManager;
import android.provider.Settings;
import android.content.Intent;

/** Saves the user's original ringer mode once and restores that exact mode. */
public final class AudioProfileController {
    private static final String PREFS = "mosque_auto_silent";
    private static final String KEY_SAVED_MODE = "saved_ringer_mode";
    private static final String KEY_HAS_SAVED_MODE = "has_saved_ringer_mode";
    private final Context context;
    private final AudioManager audio;
    private final NotificationManager notifications;

    public AudioProfileController(Context context) {
        this.context = context.getApplicationContext();
        this.audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.notifications = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public boolean hasPolicyAccess() {
        return notifications != null && notifications.isNotificationPolicyAccessGranted();
    }

    public Intent policyAccessSettingsIntent() {
        return new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
    }

    public int currentMode() { return audio.getRingerMode(); }

    public boolean silenceAndSave() {
        if (!hasPolicyAccess()) return false;
        var prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HAS_SAVED_MODE, false)) {
            // Persist before changing system audio so process death cannot lose the
            // profile that must be restored.
            boolean saved = prefs.edit().putInt(KEY_SAVED_MODE, audio.getRingerMode())
                .putBoolean(KEY_HAS_SAVED_MODE, true).commit();
            if (!saved) return false;
        }
        try {
            audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            return audio.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
        } catch (SecurityException ex) {
            return false;
        }
    }

    public boolean hasPendingRestore() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_SAVED_MODE, false);
    }

    public boolean restore() {
        var prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_HAS_SAVED_MODE, false)) return true;
        if (!hasPolicyAccess()) return false;
        int previous = prefs.getInt(KEY_SAVED_MODE, AudioManager.RINGER_MODE_NORMAL);
        try {
            audio.setRingerMode(previous);
            boolean restored = audio.getRingerMode() == previous;
            if (restored) return prefs.edit().remove(KEY_SAVED_MODE)
                .putBoolean(KEY_HAS_SAVED_MODE, false).commit();
            return false;
        } catch (SecurityException ex) {
            return false;
        }
    }

    public static String modeName(int mode) {
        if (mode == AudioManager.RINGER_MODE_SILENT) return "Silent";
        if (mode == AudioManager.RINGER_MODE_VIBRATE) return "Vibrate";
        return "Normal";
    }
}
