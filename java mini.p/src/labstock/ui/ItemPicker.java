package labstock.ui;

import labstock.model.Item;
import labstock.ui.components.FlatTextField;
import labstock.ui.components.ThinScrollBarUI;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A search box for choosing an item. Typing part of a name, ID or category
 * ("555", "sensor", "E00") shows the matching items in a list underneath;
 * pick one with the mouse, or with the arrow keys and ENTER.
 *
 * With 68 items a normal drop-down is too long to scroll through, so this
 * replaces it on the Borrow page.
 */
public class ItemPicker extends FlatTextField {

    private static final int VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 34;

    private final DefaultListModel<Item> matches = new DefaultListModel<>();
    private final JList<Item> list = new JList<>(matches);
    private final JScrollPane scroll = new JScrollPane(list);
    private final JLabel noMatches = Theme.label(" ", Theme.SMALL, Theme.TEXT_DIM);
    private final JPopupMenu popup = new JPopupMenu();
    private final List<Runnable> listeners = new ArrayList<>();
    private List<Item> allItems = new ArrayList<>();
    private Item selected;
    private boolean settingText;      // true while the picker changes its own text

    public ItemPicker(int width) {
        super("Type a name, ID or category, e.g. 555", width);

        list.setCellRenderer(new MatchRenderer());
        list.setFixedCellHeight(ROW_HEIGHT);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.PANEL_ALT);
        list.setFocusable(false);                 // the keyboard stays in the text box
        scroll.setBorder(BorderFactory.createEmptyBorder());
        ThinScrollBarUI.apply(scroll, Theme.PANEL_ALT);
        noMatches.setBorder(new EmptyBorder(10, 12, 10, 12));
        noMatches.setOpaque(true);
        noMatches.setBackground(Theme.PANEL_ALT);

        popup.setFocusable(false);                // so the popup never takes the focus
        popup.setLayout(new BorderLayout());
        popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        // typing filters the list
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTyped();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTyped();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onTyped();
            }
        });

        // mouse: hovering highlights a row, clicking picks it
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    list.setSelectedIndex(index);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0) {
                    pick(matches.get(index));
                }
            }
        };
        list.addMouseListener(mouse);
        list.addMouseMotionListener(mouse);

        // clicking the box opens the list; leaving it closes the list
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showMatches();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popup.setVisible(false);
            }
        });

        // keyboard: arrows move, ENTER picks, ESC closes
        bindKey(KeyEvent.VK_DOWN, "picker-down", () -> moveHighlight(1));
        bindKey(KeyEvent.VK_UP, "picker-up", () -> moveHighlight(-1));
        bindKey(KeyEvent.VK_ENTER, "picker-pick", () -> {
            if (popup.isVisible() && list.getSelectedValue() != null) {
                pick(list.getSelectedValue());
            }
        });
        bindKey(KeyEvent.VK_ESCAPE, "picker-close", () -> popup.setVisible(false));
    }

    // ------------------------------------------------------------ public methods

    /** Sets the items to choose from. Keeps the current choice if it still exists. */
    public void setItems(List<Item> items) {
        allItems = new ArrayList<>(items);
        if (selected != null && !allItems.contains(selected)) {
            selected = null;
            setTextQuietly("");
        } else if (selected != null) {
            setTextQuietly(selected.getName());   // the name may have been edited
        }
    }

    public Item getSelectedItem() {
        return selected;
    }

    public void setSelectedItem(Item item) {
        selected = item;
        setTextQuietly(item == null ? "" : item.getName());
        fireChange();
    }

    /** Runs the given code whenever the chosen item changes. */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    // ------------------------------------------------------------ behaviour

    private void onTyped() {
        if (settingText) {
            return;
        }
        if (selected != null) {          // the user is changing their choice
            selected = null;
            fireChange();
        }
        showMatches();
    }

    /** Fills the list with the items that match the text, and shows it. */
    private void showMatches() {
        String query = (selected != null) ? "" : getText().trim().toLowerCase();
        matches.clear();
        for (Item item : allItems) {
            if (query.isEmpty()
                    || item.getName().toLowerCase().contains(query)
                    || item.getId().toLowerCase().contains(query)
                    || item.getCategory().toLowerCase().contains(query)) {
                matches.addElement(item);
            }
        }

        popup.removeAll();
        int height;
        if (matches.isEmpty()) {
            noMatches.setText("No items match \"" + getText().trim() + "\"");
            popup.add(noMatches, BorderLayout.CENTER);
            height = noMatches.getPreferredSize().height + 2;
        } else {
            list.setSelectedIndex(Math.max(0, matches.indexOf(selected)));
            popup.add(scroll, BorderLayout.CENTER);
            height = Math.min(matches.size(), VISIBLE_ROWS) * ROW_HEIGHT + 2;
        }
        popup.setPopupSize(new Dimension(getWidth(), height));

        if (!isShowing()) {
            return;                      // the window is not on screen yet
        }
        if (popup.isVisible()) {
            popup.pack();                // resize the open list
        } else {
            popup.show(this, 0, getHeight() + 2);
        }
        list.ensureIndexIsVisible(list.getSelectedIndex());
    }

    private void moveHighlight(int step) {
        if (!popup.isVisible()) {
            showMatches();
            return;
        }
        if (matches.isEmpty()) {
            return;
        }
        int index = Math.max(0, Math.min(matches.size() - 1, list.getSelectedIndex() + step));
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
    }

    private void pick(Item item) {
        popup.setVisible(false);
        setSelectedItem(item);
    }

    private void setTextQuietly(String text) {
        settingText = true;
        setText(text);
        settingText = false;
    }

    private void fireChange() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private void bindKey(int keyCode, String name, Runnable action) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    /** One row of the list: name and ID on the left, availability on the right. */
    private static class MatchRenderer extends JPanel implements ListCellRenderer<Item> {

        private final JLabel name = Theme.label(" ", Theme.BODY, Theme.TEXT);
        private final JLabel id = Theme.label(" ", Theme.MONO_SMALL, Theme.TEXT_DIM);
        private final JLabel stock = Theme.label(" ", Theme.SMALL, Theme.TEXT_MUTED);

        MatchRenderer() {
            super(new BorderLayout());
            setBorder(new EmptyBorder(0, 12, 0, 12));
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            left.setOpaque(false);
            id.setBorder(new EmptyBorder(0, 10, 0, 0));
            left.add(name);
            left.add(id);
            JPanel middle = new JPanel(new GridBagLayout());  // centres the text vertically
            middle.setOpaque(false);
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.weightx = 1;
            middle.add(left, c);
            add(middle, BorderLayout.CENTER);
            add(stock, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Item> list, Item item, int index,
                                                      boolean selected, boolean focused) {
            setBackground(selected ? Theme.ACCENT_SOFT : Theme.PANEL_ALT);
            name.setText(item.getName());
            id.setText(item.getId() + "  \u00B7  " + item.getCategory());
            if (item.isOutOfStock()) {
                stock.setText("out of stock");
                stock.setForeground(Theme.ERROR);
            } else {
                stock.setText(item.getAvailableQuantity() + " available");
                stock.setForeground(item.isLowStock() ? Theme.WARNING : Theme.TEXT_MUTED);
            }
            return this;
        }
    }
}
