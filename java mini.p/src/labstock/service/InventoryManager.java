package labstock.service;

import labstock.model.Activity;
import labstock.model.BorrowTransaction;
import labstock.model.Consumable;
import labstock.model.ElectronicComponent;
import labstock.model.Equipment;
import labstock.model.Item;
import labstock.model.Student;
import labstock.model.TransactionStatus;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The heart of the application. It keeps the inventory, the transactions
 * and the activity log in ArrayLists, and contains every rule of the lab:
 * borrowing, returning, adding, editing and deleting items.
 *
 * After every successful change it saves all data through the FileManager
 * and tells the screens to refresh themselves.
 */
public class InventoryManager {

    private static final int FIRST_TRANSACTION_NUMBER = 1001;
    private static final String TRANSACTION_PREFIX = "TXN-";

    // what a valid name / roll number / item ID may contain
    private static final String NAME_PATTERN = "[A-Za-z][A-Za-z .']*";
    private static final String ROLL_PATTERN = "[A-Za-z0-9/-]+";
    private static final String ID_PATTERN = "[A-Za-z0-9-]+";

    private static final String OHM = "\u03A9";     // used in the sample item names
    private static final String MICRO = "\u00B5";
    private static final String TIMES = "\u00D7";

    private final FileManager fileManager;
    private final List<Item> items = new ArrayList<>();
    private final List<BorrowTransaction> transactions = new ArrayList<>();
    private final List<Activity> activities = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private int nextTransactionNumber = FIRST_TRANSACTION_NUMBER;
    private LocalTime lastSaved;

    public InventoryManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    // ================================================================ loading

    /** Loads the saved files, or creates sample data on the very first run. */
    public void load() throws IOException {
        if (!fileManager.hasSavedData()) {
            createSampleData();
            writeAllFiles();
            return;
        }
        items.addAll(fileManager.loadItems());
        transactions.addAll(fileManager.loadTransactions());
        activities.addAll(fileManager.loadActivities());

        // continue numbering after the highest transaction ID already used
        for (BorrowTransaction t : transactions) {
            try {
                nextTransactionNumber = Math.max(nextTransactionNumber, t.getNumber() + 1);
            } catch (NumberFormatException e) {
                // an ID without a number cannot affect the sequence
            }
        }
    }

    // ================================================================ borrowing

    /**
     * Issues an item to a student. Every rule is checked first; only if all
     * of them pass is the stock reduced and the transaction recorded.
     *
     * @param expectedReturnDate ignored (may be null) for consumables, which are not returned
     * @return the new transaction (its ID is shown to the user)
     */
    public BorrowTransaction borrowItem(String studentName, String rollNumber, String itemId,
                                        int quantity, LocalDate expectedReturnDate) throws LabException {
        // 1. student details
        if (!studentName.matches(NAME_PATTERN)) {
            throw new LabException("Student name can only contain letters, spaces and dots.");
        }
        if (!rollNumber.matches(ROLL_PATTERN)) {
            throw new LabException("Roll number can only contain letters, digits, - and /.");
        }

        // 2. lab rule: nothing new for a student who has not returned an overdue item
        BorrowTransaction overdue = findOverdueLoan(rollNumber);
        if (overdue != null) {
            throw new LabException(rollNumber.toUpperCase() + " has an overdue loan: "
                    + overdue.getTransactionId() + " (" + overdue.getItemName() + ", due "
                    + DateUtil.format(overdue.getExpectedReturnDate()) + ").\n"
                    + "It must be returned before anything new is issued.");
        }

        // 3. the item must exist
        Item item = findItem(itemId);
        if (item == null) {
            throw new LabException("Item " + itemId + " was not found in the inventory.");
        }

        // 4. the quantity must be sensible and in stock
        if (quantity <= 0) {
            throw new LabException("Quantity must be at least 1.");
        }
        if (item.isOutOfStock()) {
            throw new LabException(item.getName() + " is out of stock.");
        }
        if (quantity > item.getAvailableQuantity()) {
            throw new LabException(onlyAvailableMessage(item.getAvailableQuantity()));
        }
        if (quantity > item.getMaxPerBorrow()) {
            throw new LabException("Equipment can be borrowed at most " + item.getMaxPerBorrow()
                    + " units at a time.");
        }

        // 5. a returnable item needs a return date that is not in the past
        LocalDate today = LocalDate.now();
        LocalDate dueDate = null;                    // stays null for consumables
        if (item.isReturnable()) {
            if (expectedReturnDate == null) {
                throw new LabException("Please enter the expected return date.");
            }
            if (expectedReturnDate.isBefore(today)) {
                throw new LabException("Expected return date cannot be in the past.");
            }
            dueDate = expectedReturnDate;
        }

        // all checks passed: reduce the stock and record the transaction.
        // issue() is polymorphic - a Consumable also removes the units from its total.
        item.issue(quantity);
        BorrowTransaction transaction = new BorrowTransaction(nextTransactionId(),
                new Student(studentName, rollNumber.toUpperCase()), item.getId(), item.getName(),
                quantity, today, dueDate, null);
        transactions.add(transaction);
        log(studentName + (item.isReturnable() ? " borrowed " : " was issued ")
                + describe(quantity, item.getName()));
        saveChanges();
        return transaction;
    }

    // ================================================================ returning

    /** Marks a transaction as returned and puts the units back in stock. */
    public void returnItem(String transactionId) throws LabException {
        BorrowTransaction transaction = findTransaction(transactionId);
        if (transaction == null) {
            throw new LabException("Transaction " + transactionId + " was not found.");
        }
        if (!transaction.isLoan()) {
            throw new LabException(transaction.getTransactionId() + " was a consumable issue.\n"
                    + "Consumables are used up and are not returned.");
        }
        if (transaction.isReturned()) {
            throw new LabException(transaction.getTransactionId() + " was already returned on "
                    + DateUtil.format(transaction.getReturnDate()) + ".");
        }

        Item item = findItem(transaction.getItemId());
        if (item != null) {                          // the item may have been removed from the file by hand
            item.receive(transaction.getQuantity());
        }
        transaction.markReturned(LocalDate.now());
        log(transaction.getStudentName() + " returned "
                + describe(transaction.getQuantity(), transaction.getItemName()));
        saveChanges();
    }

    // ================================================================ managing items

    /** @param type Equipment.TYPE, ElectronicComponent.TYPE or Consumable.TYPE */
    public void addItem(String type, String id, String name, String category,
                        int totalQuantity, int minimumStock) throws LabException {
        id = id.toUpperCase();
        if (!id.matches(ID_PATTERN)) {
            throw new LabException("Item ID can only contain letters, digits and -.");
        }
        if (findItem(id) != null) {
            throw new LabException("An item with ID " + id + " already exists.");
        }
        checkQuantities(totalQuantity, minimumStock);

        Item item;
        if (Equipment.TYPE.equals(type)) {
            item = new Equipment(id, name, category, totalQuantity, totalQuantity, minimumStock);
        } else if (Consumable.TYPE.equals(type)) {
            item = new Consumable(id, name, category, totalQuantity, totalQuantity, minimumStock);
        } else {
            item = new ElectronicComponent(id, name, category, totalQuantity, totalQuantity, minimumStock);
        }
        items.add(item);
        log("Added " + id + " " + name + " (" + totalQuantity + " units)");
        saveChanges();
    }

    public void updateItem(String id, String name, String category,
                           int totalQuantity, int minimumStock) throws LabException {
        Item item = findItem(id);
        if (item == null) {
            throw new LabException("Item " + id + " was not found.");
        }
        checkQuantities(totalQuantity, minimumStock);
        if (totalQuantity < item.getBorrowedQuantity()) {
            throw new LabException(item.getBorrowedQuantity() + " units are currently borrowed, "
                    + "so the total cannot be less than " + item.getBorrowedQuantity() + ".");
        }

        item.setName(name);
        item.setCategory(category);
        item.setTotalQuantity(totalQuantity);
        item.setMinimumStock(minimumStock);
        log("Updated " + id + " " + name);
        saveChanges();
    }

    public void deleteItem(String id) throws LabException {
        Item item = findItem(id);
        if (item == null) {
            throw new LabException("Item " + id + " was not found.");
        }
        if (item.getBorrowedQuantity() > 0) {
            throw new LabException(item.getBorrowedQuantity() + " units of " + item.getName()
                    + " are still with students.\nIt can be deleted after they are returned.");
        }
        items.remove(item);
        log("Deleted " + id + " " + item.getName());
        saveChanges();
    }

    private void checkQuantities(int totalQuantity, int minimumStock) throws LabException {
        if (totalQuantity < 1) {
            throw new LabException("Total quantity must be at least 1.");
        }
        if (minimumStock < 0) {
            throw new LabException("Minimum stock cannot be negative.");
        }
    }

    // ================================================================ queries for the screens

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<BorrowTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /** Finds an item by ID (not case sensitive), or returns null. */
    public Item findItem(String id) {
        for (Item item : items) {
            if (item.getId().equalsIgnoreCase(id.trim())) {
                return item;
            }
        }
        return null;
    }

    public BorrowTransaction findTransaction(String transactionId) {
        for (BorrowTransaction t : transactions) {
            if (t.getTransactionId().equalsIgnoreCase(transactionId.trim())) {
                return t;
            }
        }
        return null;
    }

    /** Loans whose items have not come back yet (consumable issues are not loans). */
    public List<BorrowTransaction> getOpenTransactions() {
        List<BorrowTransaction> open = new ArrayList<>();
        for (BorrowTransaction t : transactions) {
            if (t.isLoan() && !t.isReturned()) {
                open.add(t);
            }
        }
        return open;
    }

    /** The open loans of one student. */
    public List<BorrowTransaction> getOpenLoansFor(String rollNumber) {
        List<BorrowTransaction> open = new ArrayList<>();
        for (BorrowTransaction t : getOpenTransactions()) {
            if (t.getRollNumber().equalsIgnoreCase(rollNumber.trim())) {
                open.add(t);
            }
        }
        return open;
    }

    /** The student's first overdue loan, or null if they have none. */
    public BorrowTransaction findOverdueLoan(String rollNumber) {
        for (BorrowTransaction t : getOpenLoansFor(rollNumber)) {
            if (t.getStatus() == TransactionStatus.OVERDUE) {
                return t;
            }
        }
        return null;
    }

    /** The name last used with this roll number, or null for a new student. */
    public String findStudentName(String rollNumber) {
        for (int i = transactions.size() - 1; i >= 0; i--) {
            if (transactions.get(i).getRollNumber().equalsIgnoreCase(rollNumber.trim())) {
                return transactions.get(i).getStudentName();
            }
        }
        return null;
    }

    /** Matches a transaction ID (e.g. "1042" or "TXN-1042") or a roll number. */
    public List<BorrowTransaction> searchTransactions(String query) {
        String text = query.trim().toUpperCase();
        List<BorrowTransaction> found = new ArrayList<>();
        for (BorrowTransaction t : transactions) {
            if (t.getTransactionId().toUpperCase().contains(text)
                    || t.getRollNumber().toUpperCase().contains(text)) {
                found.add(t);
            }
        }
        return found;
    }

    /** Items below their minimum stock (out-of-stock items included). */
    public List<Item> getLowStockItems() {
        List<Item> low = new ArrayList<>();
        for (Item item : items) {
            if (item.isLowStock() || item.isOutOfStock()) {
                low.add(item);
            }
        }
        return low;
    }

    /** The newest activities first. */
    public List<Activity> getRecentActivity(int count) {
        List<Activity> recent = new ArrayList<>();
        for (int i = activities.size() - 1; i >= 0 && recent.size() < count; i--) {
            recent.add(activities.get(i));
        }
        return recent;
    }

    public int getTotalUnits() {
        int total = 0;
        for (Item item : items) {
            total += item.getTotalQuantity();
        }
        return total;
    }

    public int getAvailableUnits() {
        int available = 0;
        for (Item item : items) {
            available += item.getAvailableQuantity();
        }
        return available;
    }

    public int getBorrowedUnits() {
        return getTotalUnits() - getAvailableUnits();
    }

    public int getOverdueCount() {
        int overdue = 0;
        for (BorrowTransaction t : transactions) {
            if (t.getStatus() == TransactionStatus.OVERDUE) {
                overdue++;
            }
        }
        return overdue;
    }

    /** Next free ID for a new item: E016 for equipment, C054 for components and consumables. */
    public String suggestItemId(String type) {
        String prefix = Equipment.TYPE.equals(type) ? "E" : "C";
        int highest = 0;
        for (Item item : items) {
            String id = item.getId();
            if (id.startsWith(prefix) && id.substring(1).matches("\\d+")) {
                highest = Math.max(highest, Integer.parseInt(id.substring(1)));
            }
        }
        return prefix + String.format("%03d", highest + 1);
    }

    public String getDataFolder() {
        return fileManager.getFolderName();
    }

    /** Time of the last successful save, or null if nothing was saved in this session. */
    public LocalTime getLastSaved() {
        return lastSaved;
    }

    /** "Only 3 units are currently available." */
    public static String onlyAvailableMessage(int available) {
        return available == 1 ? "Only 1 unit is currently available."
                              : "Only " + available + " units are currently available.";
    }

    /** Registers code to run after every change (used by the main window to refresh screens). */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    // ================================================================ private helpers

    /** Transaction IDs are sequential: TXN-1001, TXN-1002, ... */
    private String nextTransactionId() {
        return TRANSACTION_PREFIX + (nextTransactionNumber++);
    }

    /** "Arduino UNO" for one unit, "10 x 1k Resistor" for more. */
    private static String describe(int quantity, String itemName) {
        return quantity == 1 ? itemName : quantity + " " + TIMES + " " + itemName;
    }

    private void log(String message) {
        activities.add(new Activity(LocalDateTime.now().withSecond(0).withNano(0), message));
    }

    /** Saves every list to its file, then tells the screens to refresh. */
    private void saveChanges() throws LabException {
        try {
            writeAllFiles();
        } catch (IOException e) {
            throw new LabException("The change was made, but it could not be saved to the "
                    + fileManager.getFolderName() + " folder:\n" + e.getMessage());
        } finally {
            for (Runnable listener : listeners) {
                listener.run();
            }
        }
    }

    private void writeAllFiles() throws IOException {
        fileManager.saveItems(items);
        fileManager.saveTransactions(transactions);
        fileManager.saveActivities(activities);
        lastSaved = LocalTime.now();
    }

    // ================================================================ sample data (first run only)

    private void createSampleData() {
        // ---- equipment: boards, instruments and tools (ID, name, category, total, minimum stock)
        addEquipment("E001", "Arduino UNO", "Development Board", 5, 2);
        addEquipment("E002", "Arduino Nano", "Development Board", 4, 2);
        addEquipment("E003", "Digital Multimeter", "Test Equipment", 4, 2);
        addEquipment("E004", "Function Generator", "Test Equipment", 3, 2);
        addEquipment("E005", "Digital Oscilloscope", "Test Equipment", 2, 1);
        addEquipment("E006", "DC Power Supply", "Test Equipment", 3, 1);
        addEquipment("E007", "ESP32 Dev Board", "Development Board", 4, 2);
        addEquipment("E008", "NodeMCU ESP8266", "Development Board", 5, 2);
        addEquipment("E009", "Raspberry Pi 4 Model B", "Development Board", 2, 1);
        addEquipment("E010", "8051 Microcontroller Kit", "Development Board", 3, 1);
        addEquipment("E011", "Digital IC Trainer Kit", "Test Equipment", 4, 2);
        addEquipment("E012", "LCR Meter", "Test Equipment", 1, 1);
        addEquipment("E013", "Logic Analyzer (8 ch)", "Test Equipment", 2, 1);
        addEquipment("E014", "Soldering Station", "Tool", 4, 2);
        addEquipment("E015", "Wire Stripper", "Tool", 5, 2);

        // ---- components (lent, returnable) and consumables (issued, used up)
        addConsumable("C001", "1k" + OHM + " Resistor", "Resistor", 50, 15);
        addConsumable("C002", "10k" + OHM + " Resistor", "Resistor", 40, 15);
        addConsumable("C003", "220" + OHM + " Resistor", "Resistor", 50, 15);
        addConsumable("C004", "330" + OHM + " Resistor", "Resistor", 40, 10);
        addConsumable("C005", "4.7k" + OHM + " Resistor", "Resistor", 40, 10);
        addConsumable("C006", "100k" + OHM + " Resistor", "Resistor", 30, 10);
        addComponent("C007", "10k" + OHM + " Potentiometer", "Resistor", 12, 4);

        addConsumable("C008", "100nF Ceramic Capacitor", "Capacitor", 30, 10);
        addConsumable("C009", "22pF Ceramic Capacitor", "Capacitor", 20, 6);
        addConsumable("C010", "10" + MICRO + "F Electrolytic Capacitor", "Capacitor", 20, 5);
        addConsumable("C011", "100" + MICRO + "F Electrolytic Capacitor", "Capacitor", 15, 5);
        addConsumable("C012", "1000" + MICRO + "F Electrolytic Capacitor", "Capacitor", 8, 3);

        addComponent("C013", "IC 555 Timer", "IC", 15, 5);
        addComponent("C014", "LM358 Op-Amp", "IC", 4, 5);
        addComponent("C015", "LM741 Op-Amp", "IC", 10, 4);
        addComponent("C016", "7805 Voltage Regulator", "IC", 12, 4);
        addComponent("C017", "7408 Quad AND Gate", "IC", 10, 4);
        addComponent("C018", "7400 Quad NAND Gate", "IC", 10, 4);
        addComponent("C019", "7474 Dual D Flip-Flop", "IC", 8, 3);
        addComponent("C020", "74HC595 Shift Register", "IC", 6, 3);
        addComponent("C021", "CD4017 Decade Counter", "IC", 6, 3);
        addComponent("C022", "L293D Motor Driver", "IC", 5, 2);

        addConsumable("C023", "BC547 NPN Transistor", "Transistor", 30, 10);
        addConsumable("C024", "BC557 PNP Transistor", "Transistor", 25, 10);
        addConsumable("C025", "2N2222 NPN Transistor", "Transistor", 20, 8);
        addConsumable("C026", "IRF540N MOSFET", "Transistor", 3, 4);

        addConsumable("C027", "1N4007 Rectifier Diode", "Diode", 25, 10);
        addConsumable("C028", "1N4148 Signal Diode", "Diode", 30, 10);
        addConsumable("C029", "5.1V Zener Diode", "Diode", 15, 5);
        addConsumable("C030", "Bridge Rectifier W10M", "Diode", 6, 2);

        addConsumable("C031", "LED 5 mm Red", "LED", 100, 20);
        addConsumable("C032", "LED 5 mm Green", "LED", 80, 20);
        addConsumable("C033", "RGB LED (Common Cathode)", "LED", 15, 5);
        addComponent("C034", "7-Segment Display", "Display", 10, 4);
        addComponent("C035", "16x2 LCD Display", "Display", 6, 2);

        addComponent("C036", "IR Sensor Module", "Sensor", 6, 3);
        addComponent("C037", "LM35 Temperature Sensor", "Sensor", 8, 3);
        addComponent("C038", "HC-SR04 Ultrasonic Sensor", "Sensor", 5, 2);
        addComponent("C039", "DHT11 Humidity Sensor", "Sensor", 5, 2);
        addComponent("C040", "LDR Light Sensor", "Sensor", 20, 5);
        addComponent("C041", "PIR Motion Sensor", "Sensor", 4, 2);
        addComponent("C042", "MQ-2 Gas Sensor", "Sensor", 3, 2);

        addComponent("C043", "5V Relay Module", "Module", 6, 2);
        addComponent("C044", "HC-05 Bluetooth Module", "Module", 4, 2);
        addComponent("C045", "SG90 Servo Motor", "Module", 6, 2);
        addComponent("C046", "Piezo Buzzer", "Module", 15, 5);
        addConsumable("C047", "Tactile Push Button", "Module", 40, 10);

        addComponent("C048", "Breadboard (830 points)", "Prototyping", 10, 3);
        addConsumable("C049", "Jumper Wires Male-Male", "Prototyping", 12, 4);
        addConsumable("C050", "Jumper Wires Male-Female", "Prototyping", 10, 4);
        addConsumable("C051", "Zero PCB (General Purpose)", "Prototyping", 15, 5);

        addConsumable("C052", "9V Battery", "Power", 10, 4);
        addConsumable("C053", "9V Battery Clip", "Power", 15, 5);

        // past loans so the dashboard and history are not empty, oldest first
        // (name, roll, item, qty, borrowed N days ago, loan days, returned after N days or -1, hour)
        sampleLoan("Sneha Kulkarni", "EX-2131", "E004", 2, 12, 7, -1, 11);   // overdue
        sampleLoan("Priya Patil", "EX-2117", "E003", 1, 11, 7, 6, 10);       // returned
        sampleLoan("Vikram Rao", "EX-2112", "C038", 1, 10, 14, 5, 13);       // returned
        sampleLoan("Aditi Nair", "EX-2101", "E007", 1, 9, 7, -1, 12);        // overdue
        sampleLoan("Amit Shah", "EX-2109", "C001", 10, 8, 14, -1, 14);
        sampleLoan("Karan Mehta", "EX-2150", "C035", 1, 6, 14, 4, 11);       // returned
        sampleLoan("Rohan Deshmukh", "EX-2122", "C013", 4, 4, 14, 3, 12);    // returned
        sampleLoan("Ishaan Kapoor", "EX-2128", "C014", 2, 3, 14, -1, 15);
        sampleLoan("Rahul Sharma", "EX-2104", "E001", 1, 2, 7, -1, 10);
        sampleLoan("Neha Joshi", "EX-2140", "C048", 2, 1, 14, -1, 15);
        sampleLoan("Pooja Iyer", "EX-2135", "C043", 2, 1, 14, -1, 16);
        sampleLoan("Siddharth Pawar", "EX-2119", "C031", 6, 1, 0, -1, 17);  // consumable issue

        activities.sort(Comparator.comparing(Activity::getTime));
    }

    private void addEquipment(String id, String name, String category, int total, int minimum) {
        items.add(new Equipment(id, name, category, total, total, minimum));
    }

    private void addComponent(String id, String name, String category, int total, int minimum) {
        items.add(new ElectronicComponent(id, name, category, total, total, minimum));
    }

    private void addConsumable(String id, String name, String category, int total, int minimum) {
        items.add(new Consumable(id, name, category, total, total, minimum));
    }

    private void sampleLoan(String name, String roll, String itemId, int quantity,
                            int daysAgo, int loanDays, int returnedAfterDays, int hour) {
        Item item = findItem(itemId);
        LocalDate borrowed = LocalDate.now().minusDays(daysAgo);
        boolean returnable = item.isReturnable();
        LocalDate due = returnable ? borrowed.plusDays(loanDays) : null;
        LocalDate returned = (returnable && returnedAfterDays >= 0) ? borrowed.plusDays(returnedAfterDays) : null;

        item.issue(quantity);
        transactions.add(new BorrowTransaction(nextTransactionId(), new Student(name, roll), itemId,
                item.getName(), quantity, borrowed, due, returned));
        activities.add(new Activity(borrowed.atTime(hour, 15),
                name + (returnable ? " borrowed " : " was issued ") + describe(quantity, item.getName())));

        if (returned != null) {
            item.receive(quantity);
            activities.add(new Activity(returned.atTime(hour + 4, 40),
                    name + " returned " + describe(quantity, item.getName())));
        }
    }
}
