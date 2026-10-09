package labstock.ui;

import labstock.model.BorrowTransaction;
import labstock.model.Item;
import labstock.model.TransactionStatus;
import labstock.service.DateUtil;
import labstock.service.InventoryManager;
import labstock.service.LabException;
import labstock.ui.components.FlatButton;
import labstock.ui.components.FlatTextField;
import labstock.ui.components.RoundedPanel;
import labstock.ui.components.StyledTable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes items back. The lab assistant searches by transaction ID or roll
 * number, picks the transaction, checks the details and presses RETURN.
 * With an empty search box the table lists every open loan.
 */
public class ReturnPanel extends BasePage {

    private static final int DETAILS_WIDTH = 290;

    private final FlatTextField searchField = new FlatTextField("Transaction ID or roll number", 300);
    private final JLabel listCaption = Theme.caption("OPEN LOANS");
    private final StyledTable table = new StyledTable("TXN ID", "STUDENT", "ROLL NO", "ITEM", "STATUS");
    private final List<BorrowTransaction> shown = new ArrayList<>();
    private BorrowTransaction selected;

    // details card
    private final JLabel detailId = Theme.label("No transaction selected", Theme.SMALL, Theme.TEXT_DIM);
    private final JLabel detailStatus = Theme.label(" ", Theme.PILL, Theme.TEXT_DIM);
    private final JLabel studentValue = value();
    private final JLabel rollValue = value();
    private final JLabel itemValue = value();
    private final JLabel quantityValue = value();
    private final JLabel borrowedValue = value();
    private final JLabel expectedValue = value();
    private final JLabel noteLabel = Theme.label(" ", Theme.SMALL, Theme.TEXT_DIM);
    private final FlatButton returnButton = new FlatButton("RETURN COMPONENT", FlatButton.Kind.PRIMARY);

    public ReturnPanel(InventoryManager manager) {
        super(manager, "Return Component", "Find a loan by its transaction ID or the student's roll number.");

        table.setColumnWidths(94, 130, 86, 140, 110);
        table.lockColumnWidths(0, 2, 4);
        table.setColumnStyle(Theme.MONO_FONT, Theme.TEXT_MUTED, 0, 2);
        table.setStatusColumn(4);
        table.setEmptyText("No matching transactions.");

        JPanel searchRow = new JPanel(new BorderLayout());
        searchRow.setOpaque(false);
        searchRow.add(searchField, BorderLayout.WEST);

        JPanel list = new JPanel(new BorderLayout(0, 10));
        list.setOpaque(false);
        list.add(listCaption, BorderLayout.NORTH);
        list.add(table.inScrollPane(true), BorderLayout.CENTER);

        JPanel left = new JPanel(new BorderLayout(0, 16));
        left.setOpaque(false);
        left.add(searchRow, BorderLayout.NORTH);
        left.add(list, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        body.add(left, BorderLayout.CENTER);
        body.add(buildDetails(), BorderLayout.EAST);
        setBody(body);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0 && row < shown.size()) {
                selected = shown.get(row);
                showDetails();
            }
        });
        returnButton.addActionListener(e -> returnSelected());
    }

    @Override
    public void refresh() {
        String query = searchField.getText().trim();
        List<BorrowTransaction> results = query.isEmpty()
                ? manager.getOpenTransactions()
                : manager.searchTransactions(query);
        listCaption.setText(query.isEmpty()
                ? "OPEN LOANS  \u00B7  " + results.size()
                : "SEARCH RESULTS  \u00B7  " + results.size());

        table.clearRows();
        shown.clear();
        for (int i = results.size() - 1; i >= 0; i--) {          // newest first
            BorrowTransaction t = results.get(i);
            shown.add(t);
            table.addRow(t.getTransactionId(), t.getStudentName(), t.getRollNumber(),
                    t.getItemName(), t.getStatus().name());
        }

        // a single search result is selected automatically
        if (!query.isEmpty() && shown.size() == 1) {
            selected = shown.get(0);
        }
        int row = shown.indexOf(selected);
        if (row >= 0) {
            table.setRowSelectionInterval(row, row);
        }
        showDetails();
    }

    private void returnSelected() {
        if (selected == null) {
            return;
        }
        String what = selected.getQuantity() + " \u00D7 " + selected.getItemName();
        boolean confirmed = MessageDialog.confirm(this, "Return component",
                "Return " + what + " from " + selected.getStudentName() + "?\nTransaction "
                        + selected.getTransactionId(), "RETURN");
        if (!confirmed) {
            return;
        }
        try {
            manager.returnItem(selected.getTransactionId());
            Item item = manager.findItem(selected.getItemId());
            String stock = (item == null) ? ""
                    : "\n" + item.getName() + " now has " + item.getAvailableQuantity() + " units available.";
            MessageDialog.showSuccess(this, "Component returned",
                    selected.getTransactionId() + " has been marked as returned." + stock);
        } catch (LabException e) {
            MessageDialog.showError(this, "Cannot return", e.getMessage());
        }
    }

    /** Fills the card on the right with the selected transaction. */
    private void showDetails() {
        BorrowTransaction t = selected;
        if (t == null) {
            detailId.setText("No transaction selected");
            detailId.setFont(Theme.SMALL);
            detailId.setForeground(Theme.TEXT_DIM);
            detailStatus.setText(" ");
            detailStatus.setIcon(null);
            for (JLabel label : new JLabel[] {studentValue, rollValue, itemValue, quantityValue, borrowedValue, expectedValue}) {
                label.setText("-");
            }
            noteLabel.setText("Pick a row from the list.");
            noteLabel.setForeground(Theme.TEXT_DIM);
            returnButton.setEnabled(false);
            return;
        }

        TransactionStatus status = t.getStatus();
        Color colour = Theme.statusColor(status.name());
        detailId.setText(t.getTransactionId());
        detailId.setFont(Theme.MONO_LARGE);
        detailId.setForeground(Theme.TEXT);
        detailStatus.setText(status.name());
        detailStatus.setForeground(colour);
        detailStatus.setIcon(Theme.dot(colour));
        studentValue.setText(t.getStudentName());
        rollValue.setText(t.getRollNumber());
        itemValue.setText(t.getItemName());
        quantityValue.setText(String.valueOf(t.getQuantity()));
        borrowedValue.setText(DateUtil.format(t.getBorrowDate()));
        expectedValue.setText(DateUtil.format(t.getExpectedReturnDate()));

        if (status == TransactionStatus.ISSUED) {
            noteLabel.setText("Consumable: used up, nothing to return.");
            noteLabel.setForeground(Theme.TEXT_DIM);
        } else if (status == TransactionStatus.RETURNED) {
            noteLabel.setText("Returned on " + DateUtil.format(t.getReturnDate()) + ".");
            noteLabel.setForeground(Theme.TEXT_DIM);
        } else if (status == TransactionStatus.OVERDUE) {
            long days = t.getDaysOverdue();
            noteLabel.setText(days + (days == 1 ? " day" : " days") + " past the expected return date.");
            noteLabel.setForeground(Theme.WARNING);
        } else {
            noteLabel.setText("Within the loan period.");
            noteLabel.setForeground(Theme.TEXT_MUTED);
        }
        // the button is disabled when there is nothing to return;
        // returnItem() refuses these cases as well
        boolean canReturn = t.isLoan() && !t.isReturned();
        returnButton.setEnabled(canReturn);
        returnButton.setText(!t.isLoan() ? "NOT RETURNABLE"
                : t.isReturned() ? "ALREADY RETURNED" : "RETURN COMPONENT");
    }

    private JComponent buildDetails() {
        RoundedPanel card = new RoundedPanel(new GridBagLayout(), 18, 20);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.caption("TRANSACTION"), BorderLayout.WEST);
        detailStatus.setIconTextGap(6);
        top.add(detailStatus, BorderLayout.EAST);
        c.gridy = 0;
        card.add(top, c);
        c.gridy = 1;
        c.insets = new Insets(10, 0, 0, 0);
        card.add(detailId, c);
        c.gridy = 2;
        c.insets = new Insets(14, 0, 6, 0);
        card.add(Theme.divider(), c);

        String[] names = {"Student", "Roll number", "Component", "Quantity", "Borrowed", "Expected return"};
        JLabel[] values = {studentValue, rollValue, itemValue, quantityValue, borrowedValue, expectedValue};
        c.gridwidth = 1;
        for (int i = 0; i < names.length; i++) {
            c.gridy = 3 + i;
            c.insets = new Insets(7, 0, 0, 0);
            c.gridx = 0;
            c.weightx = 0;
            card.add(Theme.label(names[i], Theme.SMALL, Theme.TEXT_DIM), c);
            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(7, 12, 0, 0);
            card.add(values[i], c);
        }

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy = 9;
        c.insets = new Insets(14, 0, 0, 0);
        card.add(noteLabel, c);
        c.gridy = 10;
        c.insets = new Insets(16, 0, 0, 0);
        card.add(returnButton, c);

        JPanel column = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(DETAILS_WIDTH, super.getPreferredSize().height);
            }
        };
        column.setOpaque(false);
        column.add(card, BorderLayout.NORTH);
        return column;
    }

    private static JLabel value() {
        JLabel label = Theme.label("-", Theme.SMALL, Theme.TEXT);
        label.setHorizontalAlignment(JLabel.RIGHT);
        label.setMinimumSize(new Dimension(1, label.getPreferredSize().height));   // long names are cut with "..."
        return label;
    }
}
