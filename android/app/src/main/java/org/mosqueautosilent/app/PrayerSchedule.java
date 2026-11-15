package org.mosqueautosilent.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Derives local Iqama times from the supplied Adhan calendar and Imam-provided offsets. */
public final class PrayerSchedule {
    private static final ZoneId MOSQUE_ZONE = ZoneId.of("Africa/Casablanca");
    private static final String[] COLUMNS = {"Fajer", "Doher", "Aser", "Maghrib", "Isha"};
    private static final String[] CSV_COLUMNS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    private static final int[] IQAMA_OFFSETS_MINUTES = {23, 15, 15, 10, 15};

    private PrayerSchedule() {}

    public static final class Event {
        public final String prayer;
        public final String adhanText;
        public final long adhanAt;
        public final long iqamaAt;
        public final int offsetMinutes;

        Event(String prayer, String adhanText, long adhanAt, int offsetMinutes) {
            this.prayer = prayer;
            this.adhanText = adhanText;
            this.adhanAt = adhanAt;
            this.offsetMinutes = offsetMinutes;
            this.iqamaAt = adhanAt + offsetMinutes * 60_000L;
        }

        public long silenceStartsAt() { return PrayerWindow.silenceStartsAt(iqamaAt); }
        public long restoresAt(int durationMinutes) { return PrayerWindow.restoresAt(iqamaAt, durationMinutes); }
        public String offsetLabel() { return "+" + offsetMinutes + " min"; }
    }

    public static List<Event> forDate(Context context, LocalDate date) {
        List<Event> events = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open("tarfaya_prayer_times_2026.csv"), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return events;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] row = line.split(",", -1);
                if (row.length < 9 || !row[0].equals(date.toString())) continue;
                for (int i = 0; i < CSV_COLUMNS.length; i++) {
                    int column = columnIndex(header, CSV_COLUMNS[i]);
                    if (column < 0 || column >= row.length || row[column].isEmpty()) continue;
                    LocalTime adhan = LocalTime.parse(row[column]);
                    long at = ZonedDateTime.of(date, adhan, MOSQUE_ZONE).toInstant().toEpochMilli();
                    events.add(new Event(COLUMNS[i], row[column], at, IQAMA_OFFSETS_MINUTES[i]));
                }
                break;
            }
        } catch (IOException | RuntimeException ignored) {
            // An unavailable or malformed calendar yields an empty schedule, never a guessed time.
        }
        return events;
    }

    public static List<Event> nextTwoDays(Context context, LocalDate today) {
        List<Event> events = new ArrayList<>(forDate(context, today));
        events.addAll(forDate(context, today.plusDays(1)));
        return events;
    }

    public static Event currentOrNext(Context context, long now, int durationMinutes) {
        LocalDate today = LocalDate.now(MOSQUE_ZONE);
        for (Event event : nextTwoDays(context, today)) {
            if (now >= event.silenceStartsAt() && now < event.restoresAt(durationMinutes)) return event;
            if (now < event.silenceStartsAt()) return event;
        }
        return null;
    }

    private static int columnIndex(String header, String name) {
        String[] columns = header.toLowerCase(Locale.ROOT).split(",", -1);
        for (int i = 0; i < columns.length; i++) if (columns[i].equals(name)) return i;
        return -1;
    }
}
