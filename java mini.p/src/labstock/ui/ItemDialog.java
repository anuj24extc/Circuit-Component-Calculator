package labstock.ui;

import labstock.model.Consumable;
import labstock.model.ElectronicComponent;
import labstock.model.Equipment;
import labstock.model.Item;
import labstock.service.InputValidator;
import labstock.service.InventoryManager;
import labstock.service.LabException;
import labstock.ui.components.FlatButton;
import labstock.ui.components.FlatComboBox;
import labstock.ui.components.FlatTextField;
import labstock.ui.components.SegmentedControl;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;

/**
 * The form used to add a new item or edit an existing one.
 * Rule violations (duplicate ID, bad numbers...) are shown in red inside
 * the dialog, so the user can correct them without losing what was typed.
 */
public class ItemDialog extends JDialog {

    private static final int FIELD_WIDTH = 360;

    private final InventoryManager manager;
    private final Item existing;                  // null when adding a new item
    /** Options of the TYPE selector, in the same order as TYPES. */
    private static final String[] TYPES = {ElectronicComponent.TYPE, Consumable.TYPE, Equipment.TYPE};

    private final SegmentedControl typeSelector = new SegmentedControl(TYPES);
    private final FlatTextField idField = new FlatTextField("e.g. C014", FIELD_WIDTH);
    private final FlatTextField nameField = new FlatTextField("e.g. LM7805 Regulator", FIELD_WIDTH);
    private final FlatComboBox<String> categoryBox = new FlatComboBox<>(FIELD_WIDTH);
    private final FlatTextField totalField = new FlatTextField("e.g. 10", 170);
    private final FlatTextField minimumField = new FlatTextField("e.g. 3", 170);
    private final JLabel errorLabel = Theme.label(" ", Theme.SMALL, Theme.ERROR);
    private String lastSuggestedId = "";

    public static void showAdd(Component parent, InventoryManager manager) {
        new ItemDialog(parent, manager, null).setVisible(true);
    }

    public static void showEdit(Component parent, InventoryManager manager, Item item) {
        new ItemDialog(parent, manager, item).setVisible(true);
    }

    private ItemDialog(Component parent, InventoryManager manager, Item existing) {
        super(SwingUtilities.getWindowAncestor(parent), Dialog.ModalityType.APPLICATION_MODAL);
        this.manager = manager;
        this.existing = existing;
        setUndecorated(true);

        for (String category : Item.CATEGORIES) {
            categoryBox.addItem(category);
        }

        if (existing == null) {
            suggestId();
            typeSelector.addChangeListener(this::suggestId);
        } else {
            idField.setText(existing.getId());
            idField.setEditable(false);       // the ID identifies the item, so it cannot change
            idField.setForeground(Theme.TEXT_MUTED);
            nameField.setText(existing.getName());
            if (((javax.swing.DefaultComboBoxModel<String>) categoryBox.getModel()).getIndexOf(existing.getCategory()) < 0) {
                categoryBox.addItem(existing.getCategory());   // a category typed into the file by hand
            }
            categoryBox.setSelectedItem(existing.getCategory());
            totalField.setText(String.valueOf(existing.getTotalQuantity()));
            minimumField.setText(String.valueOf(existing.getMinimumStock()));
        }

        setContentPane(buildContent());
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** Fills in the next free ID for the chosen type, unless the user typed their own. */
    private void suggestId() {
        if (idField.getText().trim().isEmpty() || idField.getText().equals(lastSuggestedId)) {
            lastSuggestedId = manager.suggestItemId(TYPES[typeSelector.getSelectedIndex()]);
            idField.setText(lastSuggestedId);
        }
    }

    private void save() {
        try {
            String name = InputValidator.requireText(nameField.getText(), "item name");
            String category = (String) categoryBox.getSelectedItem();
            int total = InputValidator.parseWholeNumber(totalField.getText(), "total quantity");
            int minimum = InputValidator.parseWholeNumber(minimumField.getText(), "minimum stock");

            if (existing == null) {
                String id = InputValidator.requireText(idField.getText(), "item ID");
                manager.addItem(TYPES[typeSelector.getSelectedIndex()], id, name, category, total, minimum);
            } else {
                manager.updateItem(existing.getId(), name, category, total, minimum);
            }
            dispose();
        } catch (LabException e) {
            errorLabel.setText("<html>" + Theme.escapeHtml(e.getMessage()).replace("\n", "<br>") + "</html>");
            pack();
        }
    }

    private JPanel buildContent() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;

        int row = 0;
        c.gridy = row++;
        if (existing == null) {
            JPanel typeRow = new JPanel(new BorderLayout());
            typeRow.setOpaque(false);
            typeRow.add(typeSelector, BorderLayout.WEST);
            form.add(Theme.captioned("TYPE", typeRow), c);
        } else {
            form.add(Theme.captioned("TYPE", Theme.label(existing.getTypeLabel(), Theme.BODY, Theme.TEXT_MUTED)), c);
        }

        c.insets = new Insets(14, 0, 0, 0);
        c.gridy = row++;
        form.add(Theme.captioned("ITEM ID", idField), c);
        c.gridy = row++;
        form.add(Theme.captioned("NAME", nameField), c);
        c.gridy = row++;
        form.add(Theme.captioned("CATEGORY", categoryBox), c);

        JPanel numbers = new JPanel(new GridLayout(1, 2, 16, 0));
        numbers.setOpaque(false);
        numbers.add(Theme.captioned("TOTAL QUANTITY", totalField));
        numbers.add(Theme.captioned("MINIMUM STOCK", minimumField));
        c.gridy = row++;
        form.add(numbers, c);

        c.insets = new Insets(12, 0, 0, 0);
        c.gridy = row;
        form.add(errorLabel, c);

        FlatButton cancel = new FlatButton("CANCEL", FlatButton.Kind.SECONDARY);
        FlatButton save = new FlatButton(existing == null ? "ADD ITEM" : "SAVE CHANGES", FlatButton.Kind.PRIMARY);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> save());
        getRootPane().setDefaultButton(save);   // ENTER saves

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setOpaque(false);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(cancel);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(save);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(Theme.PANEL);
        content.setBorder(MessageDialog.dialogBorder(Theme.ACCENT));
        content.add(Theme.label(existing == null ? "ADD ITEM" : "EDIT " + existing.getId(),
                Theme.CAPTION, Theme.TEXT), BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        return content;
    }
}
