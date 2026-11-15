# Tarfaya calendar source note

Input: `Tarfaya calendar.pdf` supplied in this conversation.

The PDF has 12 pages and is titled “Annual prayer calendar 2026”. Its tabular columns are Date, Hijri date, Day, Fajr, Shurûq, Dhuhr, Asr, Maghrib and Isha. It is branded MAWAQIT and identifies مسجد طرفاية (Tarfaya Mosque). There are 365 unique dates from 2026-01-01 through 2026-12-31 in the extracted JSON.

**It does not contain Iqama/Ikama columns.** The JSON file `data/tarfaya_prayer_times_2026.json` preserves this limitation in its top-level note and every row's `time_type`. These values are included for reference only and must not be treated as congregation start times.
