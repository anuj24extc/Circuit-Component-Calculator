package labstock.model;

/**
 * The states a transaction can be in.
 * OVERDUE is not stored in the file: it is worked out from the dates.
 * ISSUED is used for consumables, which are given away rather than lent.
 */
public enum TransactionStatus {
    BORROWED,
    OVERDUE,
    RETURNED,
    ISSUED
}
