package labstock.model;

import java.time.LocalDateTime;

/**
 * One line of the activity log shown on the dashboard,
 * e.g. "Rahul Sharma borrowed Arduino UNO".
 */
public class Activity {

    private final LocalDateTime time;
    private final String message;

    public Activity(LocalDateTime time, String message) {
        this.time = time;
        this.message = message;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public String getMessage() {
        return message;
    }
}
