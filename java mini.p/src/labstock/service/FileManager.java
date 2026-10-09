package labstock.service;

import labstock.model.Activity;
import labstock.model.BorrowTransaction;
import labstock.model.Consumable;
import labstock.model.ElectronicComponent;
import labstock.model.Equipment;
import labstock.model.Item;
import labstock.model.Student;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and loads all data as plain text files in the "data" folder.
 * This is the only class that knows the file format.
 *
 * Each file has one record per line, with the columns separated by "|".
 * Lines that start with "#" are comments (the first line names the columns).
 *
 *   inventory.txt     EQUIPMENT|E001|Arduino UNO|Development Board|5|4|2
 *   transactions.txt  TXN-1005|Rahul Sharma|EX-2104|E001|Arduino UNO|1|04/10/2026|11/10/2026|-|BORROWED
 *   activity.txt      04/10/2026 10:15|Rahul Sharma borrowed Arduino UNO
 *
 * A "-" means "no date" (not returned yet, or no return date for consumables).
 * UTF-8 is set explicitly so symbols such as the ohm sign are saved correctly.
 * Files are saved through a temporary file, so a crash while saving can
 * never leave a half-written data file behind.
 */
public class FileManager {

    private static final String SEPARATOR = "|";
    private static final String SPLIT_PATTERN = "\\|";   // split() takes a regular expression
    private static final String NO_DATE = "-";

    private static final String INVENTORY_HEADER = "# TYPE|ID|NAME|CATEGORY|TOTAL|AVAILABLE|MIN_STOCK";
    private static final String TRANSACTION_HEADER =
            "# TXN_ID|STUDENT|ROLL_NO|ITEM_ID|ITEM_NAME|QTY|BORROWED|EXPECTED_RETURN|RETURNED|STATUS";
    private static final String ACTIVITY_HEADER = "# DATE_TIME|MESSAGE";

    private final File folder;
    private final File inventoryFile;
    private final File transactionFile;
    private final File activityFile;

    public FileManager(String folderName) {
        folder = new File(folderName);
        inventoryFile = new File(folder, "inventory.txt");
        transactionFile = new File(folder, "transactions.txt");
        activityFile = new File(folder, "activity.txt");
    }

    /** False on the very first run, before anything has been saved. */
    public boolean hasSavedData() {
        return inventoryFile.exists();
    }

    public String getFolderName() {
        return folder.getName();
    }

    // ------------------------------------------------------------ loading

    public List<Item> loadItems() throws IOException {
        List<Item> items = new ArrayList<>();
        for (String line : readDataLines(inventoryFile)) {
            try {
                items.add(parseItem(line));
            } catch (IllegalArgumentException e) {      // includes NumberFormatException
                warnSkipped(inventoryFile, line);
            }
        }
        return items;
    }

    public List<BorrowTransaction> loadTransactions() throws IOException {
        List<BorrowTransaction> transactions = new ArrayList<>();
        for (String line : readDataLines(transactionFile)) {
            try {
                transactions.add(parseTransaction(line));
            } catch (IllegalArgumentException | DateTimeParseException e) {
                warnSkipped(transactionFile, line);
            }
        }
        return transactions;
    }

    public List<Activity> loadActivities() throws IOException {
        List<Activity> activities = new ArrayList<>();
        for (String line : readDataLines(activityFile)) {
            String[] parts = line.split(SPLIT_PATTERN, 2);
            try {
                activities.add(new Activity(DateUtil.parseDateTime(parts[0]), parts[1]));
            } catch (DateTimeParseException | ArrayIndexOutOfBoundsException e) {
                warnSkipped(activityFile, line);
            }
        }
        return activities;
    }

    // ------------------------------------------------------------ saving

    public void saveItems(List<Item> items) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Item item : items) {
            lines.add(join(item.getType(), item.getId(), item.getName(), item.getCategory(),
                    String.valueOf(item.getTotalQuantity()),
                    String.valueOf(item.getAvailableQuantity()),
                    String.valueOf(item.getMinimumStock())));
        }
        writeFile(inventoryFile, INVENTORY_HEADER, lines);
    }

    public void saveTransactions(List<BorrowTransaction> transactions) throws IOException {
        List<String> lines = new ArrayList<>();
        for (BorrowTransaction t : transactions) {
            String expected = t.isLoan() ? DateUtil.format(t.getExpectedReturnDate()) : NO_DATE;
            String returned = t.isReturned() ? DateUtil.format(t.getReturnDate()) : NO_DATE;
            // OVERDUE is not written: it is worked out from the dates when loading
            String status = !t.isLoan() ? "ISSUED" : t.isReturned() ? "RETURNED" : "BORROWED";
            lines.add(join(t.getTransactionId(), t.getStudentName(), t.getRollNumber(),
                    t.getItemId(), t.getItemName(), String.valueOf(t.getQuantity()),
                    DateUtil.format(t.getBorrowDate()), expected, returned, status));
        }
        writeFile(transactionFile, TRANSACTION_HEADER, lines);
    }

    public void saveActivities(List<Activity> activities) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Activity activity : activities) {
            lines.add(join(DateUtil.formatDateTime(activity.getTime()), activity.getMessage()));
        }
        writeFile(activityFile, ACTIVITY_HEADER, lines);
    }

    // ------------------------------------------------------------ converting lines

    private Item parseItem(String line) {
        String[] p = line.split(SPLIT_PATTERN, -1);
        if (p.length != 7) {
            throw new IllegalArgumentException("Expected 7 columns");
        }
        int total = Integer.parseInt(p[4].trim());
        int available = Integer.parseInt(p[5].trim());
        int minimum = Integer.parseInt(p[6].trim());
        if (available < 0 || available > total) {
            throw new IllegalArgumentException("Available must be between 0 and total");
        }

        // the TYPE column decides which subclass is created
        if (Equipment.TYPE.equals(p[0])) {
            return new Equipment(p[1], p[2], p[3], total, available, minimum);
        }
        if (ElectronicComponent.TYPE.equals(p[0])) {
            return new ElectronicComponent(p[1], p[2], p[3], total, available, minimum);
        }
        if (Consumable.TYPE.equals(p[0])) {
            return new Consumable(p[1], p[2], p[3], total, available, minimum);
        }
        throw new IllegalArgumentException("Unknown type " + p[0]);
    }

    private BorrowTransaction parseTransaction(String line) {
        String[] p = line.split(SPLIT_PATTERN, -1);
        if (p.length != 10) {
            throw new IllegalArgumentException("Expected 10 columns");
        }
        LocalDate expected = NO_DATE.equals(p[7]) ? null : DateUtil.parseStored(p[7]);
        LocalDate returned = NO_DATE.equals(p[8]) ? null : DateUtil.parseStored(p[8]);
        return new BorrowTransaction(p[0], new Student(p[1], p[2]), p[3], p[4],
                Integer.parseInt(p[5].trim()), DateUtil.parseStored(p[6]), expected, returned);
    }

    private static String join(String... values) {
        return String.join(SEPARATOR, values);
    }

    // ------------------------------------------------------------ reading and writing files

    /** Returns every non-empty line that is not a "#" comment. */
    private List<String> readDataLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!file.exists()) {
            return lines;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("\uFEFF")) {          // byte-order mark added by some editors
                    line = line.substring(1);
                }
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    /**
     * Rewrites a whole file: the header line, then one line per record.
     *
     * Safe saving: everything is first written to "name.txt.tmp". Only when
     * that has finished is the temporary file renamed over the real one, so
     * if the program or the computer stops half-way, the old file is intact.
     */
    private void writeFile(File file, String header, List<String> lines) throws IOException {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IOException("Could not create the folder " + folder.getAbsolutePath());
        }

        // step 1: write the new contents to a temporary file
        File temp = new File(folder, file.getName() + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8))) {
            writer.write(header);
            writer.newLine();
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }

        // step 2: replace the real file with it in one operation
        try {
            Files.move(temp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void warnSkipped(File file, String line) {
        System.err.println("Skipped damaged line in " + file.getName() + ": " + line);
    }
}
