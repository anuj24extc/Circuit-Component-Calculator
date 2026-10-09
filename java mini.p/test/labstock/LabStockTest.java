package labstock;

import labstock.model.BorrowTransaction;
import labstock.model.Consumable;
import labstock.model.Item;
import labstock.model.TransactionStatus;
import labstock.service.DateUtil;
import labstock.service.FileManager;
import labstock.service.InputValidator;
import labstock.service.InventoryManager;
import labstock.service.LabException;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;

/**
 * Automatic tests for every rule of LABSTOCK, written in plain Java (no
 * JUnit needed). Each check prints "ok" or "FAIL"; the program exits with
 * code 1 if anything failed.
 *
 * It works on a fresh temporary data folder, so the real data/ folder is
 * never touched. Run it with run-tests.bat, or from the IDE like any main().
 */
public class LabStockTest {

    private static int passed = 0;
    private static int failed = 0;

    /** Code that is expected to throw a LabException. */
    private interface Action {
        void run() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        File folder = Files.createTempDirectory("labstock-test").toFile();
        InventoryManager m = new InventoryManager(new FileManager(folder.getPath()));
        m.load();                                   // first run: creates the sample data
        LocalDate nextWeek = LocalDate.now().plusDays(7);

        section("Sample data");
        check("68 items", m.getItems().size() == 68);
        check("28 of them are consumables", countConsumables(m) == 28);
        check("12 sample transactions", m.getTransactions().size() == 12);
        check("2 loans overdue", m.getOverdueCount() == 2);
        check("3 low-stock items (E004, C014, C026)", m.getLowStockItems().size() == 3);
        check("E004 has 1 of 3 available", m.findItem("E004").getAvailableQuantity() == 1);
        check("issuing 10 consumable resistors lowered C001's total to 40",
                m.findItem("C001").getTotalQuantity() == 40);

        section("Input checks");
        expectError("empty name", () -> InputValidator.requireText("  ", "student name"));
        expectError("text quantity", () -> InputValidator.parseWholeNumber("abc", "quantity"));
        expectError("decimal quantity", () -> InputValidator.parseWholeNumber("2.5", "quantity"));
        expectError("| in a name", () -> InputValidator.requireText("a|b", "item name"));
        expectError("impossible date 31/02", () -> DateUtil.parse("31/02/2026", "expected return date"));
        expectError("wrong date format", () -> DateUtil.parse("2026-10-12", "expected return date"));

        section("Borrowing rules");
        expectError("zero quantity", () -> m.borrowItem("Test User", "EX-1", "E001", 0, nextWeek));
        expectError("negative quantity", () -> m.borrowItem("Test User", "EX-1", "E001", -3, nextWeek));
        expectError("more than available", () -> m.borrowItem("Test User", "EX-1", "C014", 9, nextWeek));
        expectError("equipment limit of 2", () -> m.borrowItem("Test User", "EX-1", "E002", 3, nextWeek));
        expectError("unknown item", () -> m.borrowItem("Test User", "EX-1", "X999", 1, nextWeek));
        expectError("return date in the past",
                () -> m.borrowItem("Test User", "EX-1", "E001", 1, LocalDate.now().minusDays(1)));
        expectError("returnable item without a date", () -> m.borrowItem("Test User", "EX-1", "E001", 1, null));
        expectError("digits in the name", () -> m.borrowItem("R2D2", "EX-1", "E001", 1, nextWeek));
        expectError("student with an overdue loan (EX-2131)",
                () -> m.borrowItem("Sneha Kulkarni", "EX-2131", "C036", 1, nextWeek));
        expectError("same rule, roll typed in lower case", () -> m.borrowItem("Sneha Kulkarni", "ex-2131", "C036", 1, nextWeek));

        section("Student lookup");
        check("EX-2104 is Rahul Sharma", "Rahul Sharma".equals(m.findStudentName("ex-2104")));
        check("EX-2104 has 1 open loan", m.getOpenLoansFor("EX-2104").size() == 1);
        check("an unknown roll number has no name", m.findStudentName("EX-9999") == null);

        section("Borrow and return");
        int before = m.findItem("C036").getAvailableQuantity();
        BorrowTransaction loan = m.borrowItem("Test User", "ex-77", "C036", 2, nextWeek);
        check("new ID continues the sequence (TXN-1013)", loan.getTransactionId().equals("TXN-1013"));
        check("stock went down by 2", m.findItem("C036").getAvailableQuantity() == before - 2);
        check("roll number saved in capitals", loan.getRollNumber().equals("EX-77"));
        check("status is BORROWED", loan.getStatus() == TransactionStatus.BORROWED);
        m.returnItem("TXN-1013");
        check("return put the stock back", m.findItem("C036").getAvailableQuantity() == before);
        expectError("second return is refused", () -> m.returnItem("TXN-1013"));
        expectError("unknown transaction", () -> m.returnItem("TXN-9999"));
        expectError("an old returned loan (TXN-1002)", () -> m.returnItem("TXN-1002"));

        section("Consumables");
        Item resistor = m.findItem("C003");
        int total = resistor.getTotalQuantity();
        BorrowTransaction issue = m.borrowItem("Test User", "EX-77", "C003", 5, null);
        check("issued without a return date", !issue.isLoan());
        check("status is ISSUED", issue.getStatus() == TransactionStatus.ISSUED);
        check("total went down by 5 (used up)", resistor.getTotalQuantity() == total - 5);
        check("nothing counted as borrowed", resistor.getBorrowedQuantity() == 0);
        check("not listed as an open loan", !m.getOpenTransactions().contains(issue));
        expectError("a consumable cannot be returned", () -> m.returnItem(issue.getTransactionId()));

        section("Managing items");
        expectError("duplicate ID", () -> m.addItem(Consumable.TYPE, "c001", "Dup", "Resistor", 5, 1));
        expectError("total of zero", () -> m.addItem(Consumable.TYPE, "C099", "X", "Resistor", 0, 1));
        expectError("negative minimum", () -> m.addItem(Consumable.TYPE, "C099", "X", "Resistor", 5, -1));
        expectError("deleting an item that is on loan", () -> m.deleteItem("E004"));
        expectError("total below the borrowed units", () -> m.updateItem("E004", "Function Generator",
                "Test Equipment", 1, 2));
        m.borrowItem("A B", "EX-2", "E005", 2, nextWeek);
        expectError("out of stock", () -> m.borrowItem("A B", "EX-3", "E005", 1, nextWeek));
        check("E005 shows OUT OF STOCK", m.findItem("E005").getStockStatus().equals(Item.STATUS_OUT));

        section("Saving and loading");
        File[] leftovers = folder.listFiles((dir, name) -> name.endsWith(".tmp"));
        check("safe save left no .tmp files", leftovers != null && leftovers.length == 0);
        InventoryManager reloaded = new InventoryManager(new FileManager(folder.getPath()));
        reloaded.load();
        check("same number of items", reloaded.getItems().size() == m.getItems().size());
        check("all 15 transactions", reloaded.getTransactions().size() == 15);
        check("C003 is still a Consumable", reloaded.findItem("C003") instanceof Consumable);
        check("ohm sign survived", reloaded.findItem("C001").getName().equals("1k\u03A9 Resistor"));
        check("issue is still ISSUED", reloaded.findTransaction(issue.getTransactionId()).getStatus()
                == TransactionStatus.ISSUED);
        check("returned loan is still RETURNED", reloaded.findTransaction("TXN-1013").isReturned());
        BorrowTransaction next = reloaded.borrowItem("A B", "EX-3", "C036", 1, nextWeek);
        check("numbering continues after a restart (TXN-1016)", next.getTransactionId().equals("TXN-1016"));

        deleteFolder(folder);
        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------ helpers

    private static void section(String title) {
        System.out.println();
        System.out.println("-- " + title);
    }

    private static void check(String label, boolean ok) {
        System.out.println((ok ? "  ok    " : "  FAIL  ") + label);
        if (ok) {
            passed++;
        } else {
            failed++;
        }
    }

    private static void expectError(String label, Action action) {
        try {
            action.run();
            System.out.println("  FAIL  " + label + " (no error was raised)");
            failed++;
        } catch (LabException e) {
            System.out.println("  ok    " + label + "  ->  " + e.getMessage().replace("\n", " "));
            passed++;
        } catch (Exception e) {
            System.out.println("  FAIL  " + label + " (" + e + ")");
            failed++;
        }
    }

    private static int countConsumables(InventoryManager m) {
        int count = 0;
        for (Item item : m.getItems()) {
            if (!item.isReturnable()) {
                count++;
            }
        }
        return count;
    }

    private static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        folder.delete();
    }
}
