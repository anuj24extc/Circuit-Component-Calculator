package labstock.model;

/**
 * Boards and instruments such as Arduino boards, multimeters and
 * oscilloscopes. They are expensive and shared by many batches, so a
 * student may borrow at most 2 units at a time, for 7 days.
 */
public class Equipment extends Item {

    public static final String TYPE = "EQUIPMENT";
    private static final int LOAN_DAYS = 7;
    private static final int MAX_PER_BORROW = 2;

    public Equipment(String id, String name, String category,
                     int totalQuantity, int availableQuantity, int minimumStock) {
        super(id, name, category, totalQuantity, availableQuantity, minimumStock);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int getLoanDays() {
        return LOAN_DAYS;
    }

    @Override
    public int getMaxPerBorrow() {
        return MAX_PER_BORROW;
    }
}
