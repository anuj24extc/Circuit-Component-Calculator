package labstock.model;

/**
 * Anything the lab keeps in stock and gives to students.
 *
 * Item is abstract: every real item is one of three kinds, each with its
 * own rules (provided by overriding getLoanDays(), getMaxPerBorrow(),
 * isReturnable() and issue()):
 *   Equipment           - boards and instruments: lent for 7 days, max 2 at a time
 *   ElectronicComponent - ICs, sensors, modules: lent for 14 days
 *   Consumable          - resistors, LEDs, wires: issued and used up, never returned
 */
public abstract class Item {

    // stock status shown in the tables
    public static final String STATUS_READY = "READY";
    public static final String STATUS_LOW = "LOW STOCK";
    public static final String STATUS_OUT = "OUT OF STOCK";

    /** Categories offered when an item is added. */
    public static final String[] CATEGORIES = {
            "Resistor", "Capacitor", "IC", "Transistor", "Diode", "LED", "Display",
            "Sensor", "Module", "Prototyping", "Power",
            "Development Board", "Test Equipment", "Tool", "Other"
    };

    private final String id;
    private String name;
    private String category;
    private int totalQuantity;
    private int availableQuantity;
    private int minimumStock;

    protected Item(String id, String name, String category,
                   int totalQuantity, int availableQuantity, int minimumStock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.minimumStock = minimumStock;
    }

    // ------------------------------------------------------------ rules that differ by type

    /** "EQUIPMENT", "COMPONENT" or "CONSUMABLE" - also written to the inventory file. */
    public abstract String getType();

    /** True if the item must come back after use (overridden by Consumable). */
    public boolean isReturnable() {
        return true;
    }

    /** "Equipment", "Component" or "Consumable" for display. */
    public String getTypeLabel() {
        String type = getType();
        return type.charAt(0) + type.substring(1).toLowerCase();
    }

    /** Default number of days a student may keep the item. */
    public abstract int getLoanDays();

    /** Largest quantity one student may borrow in a single transaction. */
    public abstract int getMaxPerBorrow();

    // ------------------------------------------------------------ getters and setters

    public String getId()             { return id; }
    public String getName()           { return name; }
    public String getCategory()       { return category; }
    public int getTotalQuantity()     { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public int getMinimumStock()      { return minimumStock; }

    /** Units that are currently with students. */
    public int getBorrowedQuantity() {
        return totalQuantity - availableQuantity;
    }

    public void setName(String name)          { this.name = name; }
    public void setCategory(String category)  { this.category = category; }
    public void setMinimumStock(int minimum)  { this.minimumStock = minimum; }

    /** Changes the total stock. Units already on loan stay on loan. */
    public void setTotalQuantity(int newTotal) {
        int borrowed = getBorrowedQuantity();
        totalQuantity = newTotal;
        availableQuantity = newTotal - borrowed;
    }

    // ------------------------------------------------------------ stock changes

    /** Takes units out of stock when a student borrows them. */
    public void issue(int quantity) {
        if (quantity <= 0 || quantity > availableQuantity) {
            throw new IllegalArgumentException("Cannot issue " + quantity + " of " + id);
        }
        availableQuantity -= quantity;
    }

    /** Puts units back into stock when they are returned. */
    public void receive(int quantity) {
        availableQuantity = Math.min(totalQuantity, availableQuantity + quantity);
    }

    /** Removes used-up units from the total (only consumables do this). */
    protected void useUp(int quantity) {
        totalQuantity -= quantity;
    }

    // ------------------------------------------------------------ status

    public boolean isOutOfStock() {
        return availableQuantity == 0;
    }

    /** Low stock means fewer units available than the minimum the lab wants to keep. */
    public boolean isLowStock() {
        return availableQuantity < minimumStock;
    }

    public String getStockStatus() {
        if (isOutOfStock()) {
            return STATUS_OUT;
        }
        if (isLowStock()) {
            return STATUS_LOW;
        }
        return STATUS_READY;
    }

    @Override
    public String toString() {
        return name;
    }
}
