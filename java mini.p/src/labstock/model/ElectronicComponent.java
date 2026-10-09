package labstock.model;

/**
 * Reusable parts such as ICs, sensors, modules and breadboards.
 * Students may borrow as many as are in stock and keep them for 14 days.
 * (Parts that are used up, like resistors, are Consumables instead.)
 *
 * (Named ElectronicComponent rather than Component so it does not clash
 * with java.awt.Component, which every Swing class uses.)
 */
public class ElectronicComponent extends Item {

    public static final String TYPE = "COMPONENT";
    private static final int LOAN_DAYS = 14;

    public ElectronicComponent(String id, String name, String category,
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
        return Integer.MAX_VALUE;   // no limit apart from the stock itself
    }
}
