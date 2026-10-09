package labstock.service;

/**
 * Checks the text typed into the forms before it is used:
 * empty boxes, numbers that are not numbers, and the "|" character
 * (which separates the columns in the data files).
 */
public final class InputValidator {

    private InputValidator() { }

    /** Returns the trimmed text, or throws if the box is empty. */
    public static String requireText(String text, String fieldName) throws LabException {
        String value = (text == null) ? "" : text.trim();
        if (value.isEmpty()) {
            throw new LabException("Please enter the " + fieldName + ".");
        }
        if (value.contains("|")) {
            throw new LabException(capitalize(fieldName) + " cannot contain the | character.");
        }
        return value;
    }

    /** Converts the text to a whole number, e.g. "5". */
    public static int parseWholeNumber(String text, String fieldName) throws LabException {
        String value = requireText(text, fieldName);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new LabException(capitalize(fieldName) + " must be a whole number, for example 5.");
        }
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
