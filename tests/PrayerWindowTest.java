import org.junit.Test;
import org.mosqueautosilent.app.PrayerWindow;
import static org.junit.Assert.*;

public final class PrayerWindowTest {
    private static final long IQAMA = 1_000_000L;

    @Test public void startsExactlyThirtySecondsBeforeIqama() {
        long start = PrayerWindow.silenceStartsAt(IQAMA);
        assertEquals(IQAMA - 30_000L, start);
        assertFalse(PrayerWindow.isActive(start - 1, IQAMA, 10));
        assertTrue(PrayerWindow.isActive(start, IQAMA, 10));
    }

    @Test public void restoresTenMinutesAfterIqamaByDefault() {
        long restore = PrayerWindow.restoresAt(IQAMA, PrayerWindow.DEFAULT_DURATION_MINUTES);
        assertEquals(IQAMA + 600_000L, restore);
        assertTrue(PrayerWindow.isActive(restore - 1, IQAMA, PrayerWindow.DEFAULT_DURATION_MINUTES));
        assertFalse(PrayerWindow.isActive(restore, IQAMA, PrayerWindow.DEFAULT_DURATION_MINUTES));
    }

    @Test public void adjustableDurationIsMeasuredAfterIqamaAndNeverBelowOneMinute() {
        assertEquals(IQAMA + 60_000L, PrayerWindow.restoresAt(IQAMA, 1));
        assertEquals(IQAMA + 1_800_000L, PrayerWindow.restoresAt(IQAMA, 30));
        assertEquals(IQAMA + 60_000L, PrayerWindow.restoresAt(IQAMA, 0));
    }
}
