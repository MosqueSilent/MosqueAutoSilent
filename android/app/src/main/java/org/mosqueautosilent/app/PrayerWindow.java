package org.mosqueautosilent.app;

/** Pure timing rule: L is fixed at 30 seconds; D defaults to 10 minutes after Iqama. */
public final class PrayerWindow {
    public static final long LEAD_TIME_MS = 30_000L;
    public static final int DEFAULT_DURATION_MINUTES = 10;
    private PrayerWindow() {}

    public static long silenceStartsAt(long iqamaAtMs) { return iqamaAtMs - LEAD_TIME_MS; }
    public static long restoresAt(long iqamaAtMs, int durationMinutes) {
        return iqamaAtMs + Math.max(1, durationMinutes) * 60_000L;
    }
    public static boolean isActive(long nowMs, long iqamaAtMs, int durationMinutes) {
        return nowMs >= silenceStartsAt(iqamaAtMs) && nowMs < restoresAt(iqamaAtMs, durationMinutes);
    }
}
