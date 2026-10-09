package labstock.model;

/**
 * Parts that are used up in experiments and never come back: resistors,
 * capacitors, LEDs, diodes, jumper wires, batteries...
 *
 * Issuing a consumable removes the units from the lab for good, so it
 * lowers the total quantity as well as the available quantity. The
 * transaction is recorded as ISSUED and cannot be "returned".
 */
public class Consumable extends Item {

    public static final String TYPE = "CONSUMABLE";

    public Consumable(String id, String name, String category,
                      int totalQuantity, int availableQuantity, int minimumStock) {
        super(id, name, category, totalQuantity, availableQuantity, minimumStock);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int getLoanDays() {
        return 0;                       // not lent, so there is no loan period
    }

    @Override
    public int getMaxPerBorrow() {
        return Integer.MAX_VALUE;       // limited only by the stock
    }

    @Override
    public boolean isReturnable() {
        return false;
    }

    /** The units leave the shelf AND the lab's total, because they are used up. */
    @Override
    public void issue(int quantity) {
        super.issue(quantity);
        useUp(quantity);
    }
}
