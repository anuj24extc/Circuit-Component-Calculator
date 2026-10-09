package labstock.ui;

import labstock.model.BorrowTransaction;
import labstock.model.Item;
import labstock.service.DateUtil;
import labstock.service.InputValidator;
import labstock.service.InventoryManager;
import labstock.service.LabException;
import labstock.ui.components.FlatButton;
import labstock.ui.components.FlatTextField;
import labstock.ui.components.RoundedPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;

/**
 * Issues components and equipment to students.
 *
 * Left: the form. Right: details of the chosen item and a receipt of the
 * last transaction. Two lines update while the user types:
 *  - under ROLL NUMBER: what that student already has (and any overdue loan)
 *  - under COMPONENT: how many units are available
 */
public class BorrowPanel extends BasePage {

    private static final int FIELD_WIDTH = 360;
    private static final int SIDE_WIDTH = 290;

    // ---- form
    private final FlatTextField nameField = new FlatTextField("e.g. Rahul Sharma", FIELD_WIDTH);
    private final FlatTextField rollField = new FlatTextField("e.g. EX-2104", FIELD_WIDTH);
    private final ItemPicker picker = new ItemPicker(FIELD_WIDTH);
    private final FlatTextField quantityField = new FlatTextField(null, 64);
    private final FlatTextField dateField = new FlatTextField("DD/MM/YYYY", 150);
    private final JLabel studentLabel = infoLabel();
    private final JLabel availabilityLabel = infoLabel();
    private final JLabel loanHint = Theme.label(" ", Theme.SMALL, Theme.TEXT_DIM);
    private final FlatButton borrowButton = new FlatButton("BORROW COMPONENT", FlatButton.Kind.PRIMARY);

    // ---- selected item card
    private final JLabel itemName = Theme.label(" ", Theme.HEADING, Theme.TEXT);
    private final JLabel itemMeta = Theme.label(" ", Theme.SMALL, Theme.TEXT_MUTED);
    private final JLabel itemStatus = Theme.label(" ", Theme.PILL, Theme.ACCENT);
    private final JLabel availableNumber = Theme.label("0", Theme.STAT_VALUE, Theme.TEXT);
    private final JLabel availableText = Theme.label(" ", Theme.SMALL, Theme.TEXT_MUTED);
    private final StockBar stockBar = new StockBar();
    private final JLabel typeValue = detailValue();
    private final JLabel loanValue = detailValue();
    private final JLabel limitValue = detailValue();
    private final JLabel minimumValue = detailValue();

    // ---- receipt card
    private final JLabel receiptId = Theme.label("No component issued yet", Theme.SMALL, Theme.TEXT_DIM);
    private final JLabel receiptStudent = Theme.label(" ", Theme.BODY, Theme.TEXT);
    private final JLabel receiptItem = Theme.label(" ", Theme.SMALL, Theme.TEXT_MUTED);
    private final JLabel receiptDue = Theme.label(" ", Theme.SMALL, Theme.TEXT_MUTED);

    public BorrowPanel(InventoryManager manager) {
        super(manager, "Borrow Component", "Issue laboratory components and equipment to students.");
        quantityField.setText("1");
        quantityField.setHorizontalAlignment(FlatTextField.CENTER);

        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(buildForm(), BorderLayout.NORTH);
        body.add(left, BorderLayout.CENTER);
        body.add(buildSideColumn(), BorderLayout.EAST);
        setBody(body);

        picker.addChangeListener(() -> {
            setDefaultReturnDate();
            updateItemDetails();
        });
        Theme.onTextChange(quantityField, this::updateAvailability);
        Theme.onTextChange(rollField, this::updateStudentInfo);
    }

    @Override
    public void refresh() {
        picker.setItems(manager.getItems());
        if (dateField.getText().trim().isEmpty()) {
            setDefaultReturnDate();
        }
        updateItemDetails();
        updateStudentInfo();
    }

    // ------------------------------------------------------------ actions

    private void borrow() {
        try {
            String name = InputValidator.requireText(nameField.getText(), "student name");
            String roll = InputValidator.requireText(rollField.getText(), "roll number");
            Item item = picker.getSelectedItem();
            if (item == null) {
                throw new LabException("Choose a component: type part of its name and pick it from the list.");
            }
            int quantity = InputValidator.parseWholeNumber(quantityField.getText(), "quantity");
            // consumables are not returned, so they have no return date
            LocalDate returnDate = item.isReturnable()
                    ? DateUtil.parse(dateField.getText(), "expected return date")
                    : null;

            // the manager checks every rule, then records the transaction
            BorrowTransaction transaction = manager.borrowItem(name, roll, item.getId(), quantity, returnDate);

            showReceipt(transaction);
            String last = transaction.isLoan()
                    ? "Return by " + DateUtil.format(transaction.getExpectedReturnDate()) + "."
                    : "Consumable: no return needed.";
            MessageDialog.showSuccess(this, "Component issued", "Component issued successfully.\n"
                    + "Transaction ID: " + transaction.getTransactionId() + "\n" + last);
            clearForm();
        } catch (LabException e) {
            MessageDialog.showError(this, "Cannot issue component", e.getMessage());
        }
    }

    private void clearForm() {
        nameField.setText("");
        rollField.setText("");
        quantityField.setText("1");
        setDefaultReturnDate();
        nameField.requestFocusInWindow();
    }

    /** The - and + buttons next to the quantity box. */
    private void changeQuantity(int step) {
        int quantity;
        try {
            quantity = Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            quantity = 1;
        }
        quantityField.setText(String.valueOf(Math.max(1, quantity + step)));
    }

    // ------------------------------------------------------------ updating the screen

    /**
     * Suggests a return date from the item type (7 days for equipment,
     * 14 for components). Consumables have no return date at all.
     */
    private void setDefaultReturnDate() {
        Item item = picker.getSelectedItem();
        if (item != null && !item.isReturnable()) {
            dateField.setText("Not returned");
            dateField.setEditable(false);
            dateField.setForeground(Theme.TEXT_DIM);
            loanHint.setText("Consumable: issued for good, no return needed");
            return;
        }
        int days = (item == null) ? 7 : item.getLoanDays();
        dateField.setEditable(true);
        dateField.setForeground(Theme.TEXT);
        dateField.setText(DateUtil.format(LocalDate.now().plusDays(days)));
        loanHint.setText("Default loan period: " + days + " days");
    }

    /** Line under the roll number: fills in a known student's name and shows what they have. */
    private void updateStudentInfo() {
        String roll = rollField.getText().trim();
        String knownName = roll.isEmpty() ? null : manager.findStudentName(roll);
        if (knownName == null) {
            setInfo(studentLabel, " ", Theme.TEXT_DIM);
            return;
        }
        if (nameField.getText().trim().isEmpty()) {
            nameField.setText(knownName);           // a returning student: fill in the name
        }

        BorrowTransaction overdue = manager.findOverdueLoan(roll);
        List<BorrowTransaction> open = manager.getOpenLoansFor(roll);
        if (overdue != null) {
            setInfo(studentLabel, "Overdue: " + overdue.getTransactionId() + " " + overdue.getItemName()
                    + ", due " + DateUtil.format(overdue.getExpectedReturnDate()) + ". Return it first.", Theme.ERROR);
        } else if (!open.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (BorrowTransaction t : open) {
                names.append(names.length() == 0 ? "" : ", ").append(t.getItemName());
            }
            setInfo(studentLabel, "Has " + open.size() + (open.size() == 1 ? " item" : " items")
                    + " out: " + names, Theme.TEXT_MUTED);
        } else {
            setInfo(studentLabel, "Known student: nothing borrowed at the moment.", Theme.TEXT_MUTED);
        }
    }

    /** Fills the "selected item" card on the right. */
    private void updateItemDetails() {
        Item item = picker.getSelectedItem();
        if (item == null) {
            itemName.setText("No item chosen");
            itemMeta.setText("Search " + manager.getItems().size() + " items in the Component box.");
            itemStatus.setText(" ");
            itemStatus.setIcon(null);
            availableNumber.setText("-");
            availableText.setText(" ");
            stockBar.setStock(0, 1, Theme.ACCENT);
            for (JLabel label : new JLabel[] {typeValue, loanValue, limitValue, minimumValue}) {
                label.setText("-");
            }
            borrowButton.setText("BORROW COMPONENT");
            updateAvailability();
            return;
        }

        Color statusColour = Theme.statusColor(item.getStockStatus());
        itemName.setText(item.getName());
        itemMeta.setText(item.getId() + "  \u00B7  " + item.getCategory());
        itemStatus.setText(item.getStockStatus());
        itemStatus.setForeground(statusColour);
        itemStatus.setIcon(Theme.dot(statusColour));
        availableNumber.setText(String.valueOf(item.getAvailableQuantity()));
        availableText.setText("of " + item.getTotalQuantity() + " units available");
        stockBar.setStock(item.getAvailableQuantity(), item.getTotalQuantity(),
                item.isLowStock() || item.isOutOfStock() ? Theme.WARNING : Theme.ACCENT);

        typeValue.setText(item.getTypeLabel());
        loanValue.setText(item.isReturnable() ? item.getLoanDays() + " days" : "Not returned");
        limitValue.setText(item.getMaxPerBorrow() == Integer.MAX_VALUE ? "No limit" : item.getMaxPerBorrow() + " units");
        minimumValue.setText(item.getMinimumStock() + " units");
        borrowButton.setText(item.isReturnable() ? "BORROW COMPONENT" : "ISSUE COMPONENT");
        updateAvailability();
    }

    /** Line under the component box: "Available: 3 units", or a warning while typing. */
    private void updateAvailability() {
        Item item = picker.getSelectedItem();
        if (item == null) {
            setInfo(availabilityLabel, "Type part of a name, ID or category, then pick from the list.", Theme.TEXT_DIM);
            return;
        }
        int available = item.getAvailableQuantity();
        Integer quantity = typedQuantity();

        if (available == 0) {
            setInfo(availabilityLabel, item.getName() + " is out of stock.", Theme.ERROR);
        } else if (quantity != null && quantity > available) {
            setInfo(availabilityLabel, InventoryManager.onlyAvailableMessage(available), Theme.WARNING);
        } else if (quantity != null && quantity > item.getMaxPerBorrow()) {
            setInfo(availabilityLabel, "Equipment: at most " + item.getMaxPerBorrow() + " units per borrow.", Theme.WARNING);
        } else {
            setInfo(availabilityLabel, "Available: " + available + (available == 1 ? " unit" : " units"), Theme.TEXT_MUTED);
        }
    }

    /** Sets one of the small status lines, with a coloured dot in front. */
    private static void setInfo(JLabel label, String text, Color colour) {
        label.setText(text);
        label.setForeground(colour);
        boolean blank = text.trim().isEmpty() || colour == Theme.TEXT_DIM;
        label.setIcon(blank ? null : Theme.dot(colour == Theme.TEXT_MUTED ? Theme.ACCENT : colour));
    }

    /** The quantity typed so far, or null if it is not a number yet. */
    private Integer typedQuantity() {
        try {
            return Integer.parseInt(quantityField.getText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void showReceipt(BorrowTransaction t) {
        receiptId.setText(t.getTransactionId());
        receiptId.setFont(Theme.MONO_LARGE);
        receiptId.setForeground(Theme.TEXT);
        receiptId.setIcon(Theme.check(Theme.ACCENT));
        receiptStudent.setText(t.getStudentName() + "  \u00B7  " + t.getRollNumber());
        receiptItem.setText(t.getQuantity() + " \u00D7 " + t.getItemName());
        receiptDue.setText(t.isLoan() ? "Return by " + DateUtil.format(t.getExpectedReturnDate())
                                      : "Consumable  \u00B7  no return needed");
    }

    // ------------------------------------------------------------ layout

    private JComponent buildForm() {
        RoundedPanel card = new RoundedPanel(new GridBagLayout(), 20, 22);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        c.gridy = 0;
        card.add(Theme.captioned("STUDENT NAME", nameField), c);
        c.gridy = 1;
        c.insets = new Insets(14, 0, 0, 0);
        card.add(Theme.captioned("ROLL NUMBER", rollField), c);
        c.gridy = 2;
        c.insets = new Insets(7, 0, 0, 0);
        card.add(studentLabel, c);
        c.gridy = 3;
        c.insets = new Insets(12, 0, 0, 0);
        card.add(Theme.captioned("COMPONENT", picker), c);
        c.gridy = 4;
        c.insets = new Insets(7, 0, 0, 0);
        card.add(availabilityLabel, c);

        // quantity stepper and return date side by side
        FlatButton minus = new FlatButton("\u2212", FlatButton.Kind.SECONDARY);
        FlatButton plus = new FlatButton("+", FlatButton.Kind.SECONDARY);
        minus.setBorder(new EmptyBorder(0, 12, 0, 12));
        plus.setBorder(new EmptyBorder(0, 12, 0, 12));
        minus.addActionListener(e -> changeQuantity(-1));
        plus.addActionListener(e -> changeQuantity(1));
        JPanel stepper = new JPanel();
        stepper.setLayout(new BoxLayout(stepper, BoxLayout.X_AXIS));
        stepper.setOpaque(false);
        stepper.add(minus);
        stepper.add(Box.createHorizontalStrut(6));
        stepper.add(quantityField);
        stepper.add(Box.createHorizontalStrut(6));
        stepper.add(plus);

        JPanel pair = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pair.setOpaque(false);
        pair.add(Theme.captioned("QUANTITY", stepper));
        pair.add(Box.createHorizontalStrut(28));
        pair.add(Theme.captioned("EXPECTED RETURN", dateField));
        c.gridy = 5;
        c.insets = new Insets(16, 0, 0, 0);
        card.add(pair, c);
        c.gridy = 6;
        c.insets = new Insets(6, 0, 0, 0);
        card.add(loanHint, c);

        FlatButton clearButton = new FlatButton("CLEAR", FlatButton.Kind.SECONDARY);
        borrowButton.addActionListener(e -> borrow());
        clearButton.addActionListener(e -> clearForm());
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setOpaque(false);
        buttons.add(borrowButton);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(clearButton);
        buttons.add(Box.createHorizontalGlue());
        c.gridy = 7;
        c.insets = new Insets(20, 0, 0, 0);
        card.add(buttons, c);
        return card;
    }

    private JComponent buildSideColumn() {
        // selected item
        RoundedPanel itemCard = new RoundedPanel(new GridBagLayout(), 18, 20);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.caption("SELECTED ITEM"), BorderLayout.WEST);
        itemStatus.setIconTextGap(6);
        top.add(itemStatus, BorderLayout.EAST);
        c.gridy = 0;
        itemCard.add(top, c);
        c.gridy = 1;
        c.insets = new Insets(12, 0, 0, 0);
        itemName.setMinimumSize(new Dimension(1, itemName.getPreferredSize().height));
        itemCard.add(itemName, c);
        c.gridy = 2;
        c.insets = new Insets(2, 0, 0, 0);
        itemMeta.setMinimumSize(new Dimension(1, itemMeta.getPreferredSize().height));
        itemCard.add(itemMeta, c);

        FlowLayout baseline = new FlowLayout(FlowLayout.LEFT, 0, 0);
        baseline.setAlignOnBaseline(true);
        JPanel countRow = new JPanel(baseline);
        countRow.setOpaque(false);
        availableText.setBorder(new EmptyBorder(0, 8, 0, 0));
        countRow.add(availableNumber);
        countRow.add(availableText);
        c.gridy = 3;
        c.insets = new Insets(14, 0, 0, 0);
        itemCard.add(countRow, c);
        c.gridy = 4;
        c.insets = new Insets(8, 0, 0, 0);
        itemCard.add(stockBar, c);
        c.gridy = 5;
        c.insets = new Insets(16, 0, 6, 0);
        itemCard.add(Theme.divider(), c);

        String[] names = {"Type", "Loan period", "Limit per borrow", "Minimum stock"};
        JLabel[] values = {typeValue, loanValue, limitValue, minimumValue};
        c.gridwidth = 1;
        for (int i = 0; i < names.length; i++) {
            c.gridy = 6 + i;
            c.insets = new Insets(6, 0, 0, 0);
            c.gridx = 0;
            itemCard.add(Theme.label(names[i], Theme.SMALL, Theme.TEXT_DIM), c);
            c.gridx = 1;
            itemCard.add(values[i], c);
        }

        // receipt of the last transaction
        RoundedPanel receiptCard = new RoundedPanel(new GridBagLayout(), 18, 20);
        GridBagConstraints r = new GridBagConstraints();
        r.gridx = 0;
        r.weightx = 1;
        r.fill = GridBagConstraints.HORIZONTAL;
        r.anchor = GridBagConstraints.WEST;
        r.gridy = 0;
        receiptCard.add(Theme.caption("LAST ISSUED"), r);
        r.gridy = 1;
        r.insets = new Insets(10, 0, 0, 0);
        receiptId.setIconTextGap(8);
        receiptCard.add(receiptId, r);
        r.gridy = 2;
        r.insets = new Insets(8, 0, 0, 0);
        receiptCard.add(receiptStudent, r);
        r.gridy = 3;
        r.insets = new Insets(2, 0, 0, 0);
        receiptCard.add(receiptItem, r);
        r.gridy = 4;
        receiptCard.add(receiptDue, r);

        JPanel column = new JPanel(new BorderLayout(0, 16)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(SIDE_WIDTH, super.getPreferredSize().height);
            }
        };
        column.setOpaque(false);
        column.add(itemCard, BorderLayout.NORTH);
        JPanel receiptHolder = new JPanel(new BorderLayout());
        receiptHolder.setOpaque(false);
        receiptHolder.add(receiptCard, BorderLayout.NORTH);
        column.add(receiptHolder, BorderLayout.CENTER);
        return column;
    }

    private static JLabel detailValue() {
        JLabel label = Theme.label(" ", Theme.SMALL, Theme.TEXT);
        label.setHorizontalAlignment(JLabel.RIGHT);
        return label;
    }

    /** A small status line; long text is cut with "..." instead of widening the form. */
    private static JLabel infoLabel() {
        JLabel label = Theme.label(" ", Theme.SMALL, Theme.TEXT_MUTED);
        label.setIconTextGap(8);
        label.setMinimumSize(new Dimension(1, 18));
        label.setPreferredSize(new Dimension(1, 18));
        return label;
    }

    /** Thin bar showing how much of the total stock is available. */
    private static class StockBar extends JComponent {

        private int available;
        private int total = 1;
        private Color colour = Theme.ACCENT;

        StockBar() {
            setPreferredSize(new Dimension(10, 4));
        }

        void setStock(int available, int total, Color colour) {
            this.available = available;
            this.total = Math.max(1, total);
            this.colour = colour;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQualityHints(g2);
            g2.setColor(Theme.BORDER_SOFT);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            int filled = (int) Math.round(getWidth() * (available / (double) total));
            g2.setColor(colour);
            g2.fillRoundRect(0, 0, filled, getHeight(), 4, 4);
            g2.dispose();
        }
    }
}
