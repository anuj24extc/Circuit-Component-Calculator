package labstock.ui;

import labstock.model.BorrowTransaction;
import labstock.service.DateUtil;
import labstock.service.InventoryManager;
import labstock.ui.components.FlatTextField;
import labstock.ui.components.SegmentedControl;
import labstock.ui.components.StyledTable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Every transaction ever recorded, newest first, with a text search and a
 * status filter.
 */
public class HistoryPanel extends BasePage {

    private static final String ALL = "ALL";

    private final FlatTextField searchField = new FlatTextField("Search name, roll no, item, TXN", 230);
    private final SegmentedControl statusFilter =
            new SegmentedControl(ALL, "BORROWED", "OVERDUE", "RETURNED", "ISSUED");
    private final JLabel countLabel = Theme.caption(" ");
    private final StyledTable table = new StyledTable(
            "TXN ID", "STUDENT", "ROLL NO", "ITEM", "QTY", "BORROWED", "EXPECTED", "STATUS");

    public HistoryPanel(InventoryManager manager) {
        super(manager, "Borrowing History", "Every transaction saved in the transactions file.");

        table.setColumnWidths(94, 140, 86, 170, 52, 102, 102, 110);
        table.lockColumnWidths(0, 2, 4, 5, 6, 7);
        table.setColumnStyle(Theme.MONO_FONT, Theme.TEXT_MUTED, 0, 2, 5, 6);
        table.setColumnStyle(Theme.MONO_FONT, Theme.TEXT, 4);
        table.setStatusColumn(7);
        table.setEmptyText("No transactions match the filter.");

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setOpaque(false);
        toolbar.add(searchField);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(statusFilter);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(countLabel);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(table.inScrollPane(true), BorderLayout.CENTER);
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
        statusFilter.addChangeListener(this::refresh);
    }

    @Override
    public void refresh() {
        String search = searchField.getText().trim().toLowerCase();
        String status = statusFilter.getSelectedOption();
        List<BorrowTransaction> transactions = manager.getTransactions();

        table.clearRows();
        int shown = 0;
        for (int i = transactions.size() - 1; i >= 0; i--) {     // newest first
            BorrowTransaction t = transactions.get(i);
            if (matches(t, search, status)) {
                table.addRow(t.getTransactionId(), t.getStudentName(), t.getRollNumber(), t.getItemName(),
                        t.getQuantity(), DateUtil.format(t.getBorrowDate()),
                        DateUtil.format(t.getExpectedReturnDate()), t.getStatus().name());
                shown++;
            }
        }
        countLabel.setText(shown + " OF " + transactions.size());
    }

    private boolean matches(BorrowTransaction t, String search, String status) {
        boolean statusMatches = ALL.equals(status) || t.getStatus().name().equals(status);
        boolean textMatches = search.isEmpty()
                || t.getTransactionId().toLowerCase().contains(search)
                || t.getStudentName().toLowerCase().contains(search)
                || t.getRollNumber().toLowerCase().contains(search)
                || t.getItemName().toLowerCase().contains(search);
        return statusMatches && textMatches;
    }
}
