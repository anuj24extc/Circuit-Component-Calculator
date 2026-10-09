package labstock.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * One transaction: who took what, how many, when, and when it must come back.
 *
 * For consumables (issued, not lent) the expected return date is null.
 * The item name is stored as well as its ID so the history still reads
 * correctly even if the item is later renamed or deleted.
 */
public class BorrowTransaction {

    private final String transactionId;       // e.g. TXN-1042
    private final Student student;
    private final String itemId;
    private final String itemName;
    private final int quantity;
    private final LocalDate borrowDate;
    private final LocalDate expectedReturnDate;  // null for a consumable issue
    private LocalDate returnDate;              // null until the item is returned

    public BorrowTransaction(String transactionId, Student student, String itemId, String itemName,
                             int quantity, LocalDate borrowDate, LocalDate expectedReturnDate,
                             LocalDate returnDate) {
        this.transactionId = transactionId;
        this.student = student;
        this.itemId = itemId;
        this.itemName = itemName;
        this.quantity = quantity;
        this.borrowDate = borrowDate;
        this.expectedReturnDate = expectedReturnDate;
        this.returnDate = returnDate;
    }

    public String getTransactionId()        { return transactionId; }
    public Student getStudent()             { return student; }
    public String getStudentName()          { return student.getName(); }
    public String getRollNumber()           { return student.getRollNumber(); }
    public String getItemId()               { return itemId; }
    public String getItemName()             { return itemName; }
    public int getQuantity()                { return quantity; }
    public LocalDate getBorrowDate()        { return borrowDate; }
    public LocalDate getExpectedReturnDate(){ return expectedReturnDate; }
    public LocalDate getReturnDate()        { return returnDate; }

    public boolean isReturned() {
        return returnDate != null;
    }

    /** True for a loan that must come back; false for a consumable that was issued. */
    public boolean isLoan() {
        return expectedReturnDate != null;
    }

    public void markReturned(LocalDate date) {
        this.returnDate = date;
    }

    /** ISSUED for consumables; otherwise RETURNED, OVERDUE (date passed) or BORROWED. */
    public TransactionStatus getStatus() {
        if (!isLoan()) {
            return TransactionStatus.ISSUED;
        }
        if (isReturned()) {
            return TransactionStatus.RETURNED;
        }
        if (LocalDate.now().isAfter(expectedReturnDate)) {
            return TransactionStatus.OVERDUE;
        }
        return TransactionStatus.BORROWED;
    }

    /** How many days late an unreturned item is (0 if it is not overdue). */
    public long getDaysOverdue() {
        if (getStatus() != TransactionStatus.OVERDUE) {
            return 0;
        }
        return ChronoUnit.DAYS.between(expectedReturnDate, LocalDate.now());
    }

    /** The number part of the ID: TXN-1042 gives 1042. */
    public int getNumber() {
        return Integer.parseInt(transactionId.substring(transactionId.indexOf('-') + 1));
    }
}
