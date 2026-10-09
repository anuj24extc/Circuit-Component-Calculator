package labstock.ui;

import labstock.service.InventoryManager;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Base class for the five screens (Dashboard, Inventory, Borrow, Returns,
 * History). It gives every page the same padding and heading, and makes
 * each page provide a refresh() method that re-reads the data.
 */
public abstract class BasePage extends JPanel {

    protected final InventoryManager manager;
    private final JLabel overlineLabel = Theme.label(" ", Theme.BODY, Theme.TEXT_MUTED);
    private final JLabel subtitleLabel = Theme.label(" ", Theme.BODY, Theme.TEXT_MUTED);

    protected BasePage(InventoryManager manager, String title, String subtitle) {
        this.manager = manager;
        setLayout(new BorderLayout(0, 18));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        header.add(overlineLabel, c);
        c.gridy = 1;
        header.add(Theme.label(title, Theme.TITLE, Theme.TEXT), c);
        c.gridy = 2;
        c.insets = new Insets(2, 0, 0, 0);
        header.add(subtitleLabel, c);
        add(header, BorderLayout.NORTH);

        overlineLabel.setVisible(false);
        setSubtitle(subtitle);
    }

    /** Re-reads everything from the InventoryManager and updates the screen. */
    public abstract void refresh();

    /** Small line above the title (used for "Good evening" on the dashboard). */
    protected void setOverline(String text) {
        overlineLabel.setText(text);
        overlineLabel.setVisible(true);
    }

    protected void setSubtitle(String text) {
        subtitleLabel.setText(text == null ? " " : text);
        subtitleLabel.setVisible(text != null);
    }

    /** Places the page's main content below the heading. */
    protected void setBody(JComponent body) {
        add(body, BorderLayout.CENTER);
    }
}
