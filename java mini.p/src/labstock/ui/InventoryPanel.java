package labstock.ui;

import labstock.model.Item;
import labstock.service.InventoryManager;
import labstock.service.LabException;
import labstock.ui.components.FlatButton;
import labstock.ui.components.FlatComboBox;
import labstock.ui.components.FlatTextField;
import labstock.ui.components.StyledTable;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * All components and equipment in one table, with search, a category
 * filter and Add / Edit / Delete.
 */
public class InventoryPanel extends BasePage {

    private static final String ALL_CATEGORIES = "All categories";

    private final FlatTextField searchField = new FlatTextField("Search by ID or name", 240);
    private final FlatComboBox<String> categoryFilter = new FlatComboBox<>(180);
    private final FlatButton addButton = new FlatButton("+ ADD ITEM", FlatButton.Kind.PRIMARY);
    private final FlatButton editButton = new FlatButton("EDIT", FlatButton.Kind.SECONDARY);
    private final FlatButton deleteButton = new FlatButton("DELETE", FlatButton.Kind.SECONDARY);
    private final StyledTable table =
            new StyledTable("ID", "ITEM", "CATEGORY", "TYPE", "AVAIL", "TOTAL", "MIN", "STATUS");
    private final JLabel footer = Theme.label(" ", Theme.SMALL, Theme.TEXT_DIM);

    /** The items currently shown, in the same order as the table rows. */
    private final List<Item> shownItems = new ArrayList<>();

    public InventoryPanel(InventoryManager manager) {
        super(manager, "Inventory", "Components and equipment held in the lab.");

        categoryFilter.addItem(ALL_CATEGORIES);
        for (String category : Item.CATEGORIES) {
            categoryFilter.addItem(category);
        }

        table.setColumnWidths(64, 230, 130, 100, 66, 72, 56, 124);
        table.lockColumnWidths(0, 3, 4, 5, 6, 7);
        table.setColumnStyle(Theme.MONO_FONT, Theme.TEXT_MUTED, 0, 5, 6);
        table.setColumnStyle(Theme.MONO_FONT, Theme.TEXT, 4);
        table.setColumnStyle(Theme.BODY, Theme.TEXT_MUTED, 2);
        table.setColumnStyle(Theme.SMALL, Theme.TEXT_MUTED, 3);
        table.setStatusColumn(7);
        table.setEmptyText("No items match the search.");

        // toolbar: search and filter on the left, actions on the right
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setOpaque(false);
        toolbar.add(searchField);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(categoryFilter);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(editButton);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(deleteButton);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(addButton);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.add(toolbar, BorderLayout.NORTH);
        body.add(table.inScrollPane(true), BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        setBody(body);

        // live search and filter
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
        categoryFilter.addActionListener(e -> refresh());

        addButton.addActionListener(e -> ItemDialog.showAdd(this, manager));
        editButton.addActionListener(e -> editSelected());
        deleteButton.addActionListener(e -> deleteSelected());
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {     // double-click a row to edit it
                    editSelected();
                }
            }
        });
    }

    @Override
    public void refresh() {
        Item previouslySelected = selectedItem();
        String search = searchField.getText().trim().toLowerCase();
        String category = (String) categoryFilter.getSelectedItem();

        table.clearRows();
        shownItems.clear();
        for (Item item : manager.getItems()) {
            if (matches(item, search, category)) {
                shownItems.add(item);
                table.addRow(item.getId(), item.getName(), item.getCategory(), item.getTypeLabel(),
                        item.getAvailableQuantity(), item.getTotalQuantity(), item.getMinimumStock(),
                        item.getStockStatus());
            }
        }

        // keep the same row selected after a refresh
        int row = shownItems.indexOf(previouslySelected);
        if (row >= 0) {
            table.setRowSelectionInterval(row, row);
        }
        footer.setText("Showing " + shownItems.size() + " of " + manager.getItems().size()
                + " items  \u00B7  double-click a row to edit");
        updateButtons();
    }

    private boolean matches(Item item, String search, String category) {
        boolean textMatches = search.isEmpty()
                || item.getId().toLowerCase().contains(search)
                || item.getName().toLowerCase().contains(search);
        boolean categoryMatches = category == null || ALL_CATEGORIES.equals(category)
                || category.equals(item.getCategory());
        return textMatches && categoryMatches;
    }

    private Item selectedItem() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < shownItems.size()) ? shownItems.get(row) : null;
    }

    private void updateButtons() {
        boolean hasSelection = selectedItem() != null;
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
    }

    private void editSelected() {
        Item item = selectedItem();
        if (item != null) {
            ItemDialog.showEdit(this, manager, item);
        }
    }

    private void deleteSelected() {
        Item item = selectedItem();
        if (item == null) {
            return;
        }
        boolean confirmed = MessageDialog.confirm(this, "Delete item",
                "Delete " + item.getId() + " " + item.getName() + " from the inventory?\nThis cannot be undone.",
                "DELETE");
        if (!confirmed) {
            return;
        }
        try {
            manager.deleteItem(item.getId());
        } catch (LabException e) {
            MessageDialog.showError(this, "Cannot delete item", e.getMessage());
        }
    }
}
