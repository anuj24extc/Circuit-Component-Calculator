package labstock.service;

/**
 * Thrown when an action breaks one of the lab's rules, for example
 * borrowing more units than are available or returning an item twice.
 *
 * The message is written for the user and is shown directly in a dialog.
 * It is a checked exception, so the compiler makes the UI handle it.
 */
public class LabException extends Exception {

    public LabException(String message) {
        super(message);
    }
}
