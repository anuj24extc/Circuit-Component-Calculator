package labstock.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/**
 * Converts dates to and from text. Dates are written as DD/MM/YYYY
 * everywhere: on screen and in the data files.
 *
 * STRICT resolving makes impossible dates such as 31/02/2026 fail
 * instead of being quietly changed to 28/02/2026.
 */
public final class DateUtil {

    private DateUtil() { }

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_INPUT =      // also accepts 1/9/2026
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    /** 12/10/2026, or "-" for no date. */
    public static String format(LocalDate date) {
        return (date == null) ? "-" : date.format(DATE);
    }

    /** Reads a date typed by the user. */
    public static LocalDate parse(String text, String fieldName) throws LabException {
        String value = (text == null) ? "" : text.trim();
        if (value.isEmpty()) {
            throw new LabException("Please enter the " + fieldName + ".");
        }
        try {
            return LocalDate.parse(value, DATE_INPUT);
        } catch (DateTimeParseException e) {
            throw new LabException("Enter the " + fieldName + " as DD/MM/YYYY, for example "
                    + format(LocalDate.now().plusDays(7)) + ".");
        }
    }

    /** Reads a date from a data file (throws DateTimeParseException if damaged). */
    public static LocalDate parseStored(String text) {
        return LocalDate.parse(text, DATE);
    }

    public static String formatDateTime(LocalDateTime time) {
        return time.format(DATE_TIME);
    }

    public static LocalDateTime parseDateTime(String text) {
        return LocalDateTime.parse(text, DATE_TIME);
    }

    /** "Today 21:42", "Yesterday 10:15" or "27 Sep 10:15" for the activity list. */
    public static String friendly(LocalDateTime time) {
        LocalDate day = time.toLocalDate();
        LocalDate today = LocalDate.now();
        if (day.equals(today)) {
            return "Today " + time.format(TIME);
        }
        if (day.equals(today.minusDays(1))) {
            return "Yesterday " + time.format(TIME);
        }
        return time.format(DAY_MONTH) + " " + time.format(TIME);
    }
}
