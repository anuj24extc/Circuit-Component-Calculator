package labstock.ui;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Narrow navigation bar on the left. The selected page gets a slightly
 * lighter background and a thin blue line on its left edge.
 */
public class Sidebar extends JPanel {

    private static final int SIDEBAR_WIDTH = 200;
    private static final int ITEM_HEIGHT = 38;
    private static final int TEXT_LEFT = 22;

    private final Consumer<String> onSelect;
    private final JPanel list = new JPanel();
    private final Map<String, NavItem> items = new HashMap<>();
    private NavItem selected;

    /** @param onSelect called with the page key whenever an item is clicked */
    public Sidebar(Consumer<String> onSelect) {
        super(new BorderLayout());
        this.onSelect = onSelect;
        setBackground(Theme.SIDEBAR);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.add(buildBrand());

        JLabel menu = Theme.caption("MENU");
        menu.setBorder(new EmptyBorder(26, TEXT_LEFT, 10, 0));
        menu.setAlignmentX(LEFT_ALIGNMENT);
        list.add(menu);

        add(list, BorderLayout.NORTH);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    public void addItem(String key, String text) {
        NavItem item = new NavItem(key, text);
        items.put(key, item);
        list.add(item);
    }

    /** Highlights a page and tells the main window to show it. */
    public void select(String key) {
        NavItem item = items.get(key);
        if (item == null) {
            return;
        }
        selected = item;
        list.repaint();
        onSelect.accept(key);
    }

    /** Small grey text on the right of an item, e.g. the number of open loans. */
    public void setDetail(String key, String detail) {
        NavItem item = items.get(key);
        if (item != null) {
            item.detail = detail;
            item.repaint();
        }
    }

    private JComponent buildBrand() {
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        brand.setBorder(new EmptyBorder(22, TEXT_LEFT, 0, 12));
        brand.setAlignmentX(LEFT_ALIGNMENT);

        JComponent mark = new JComponent() {     // short blue bar beside the name
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.ACCENT);
                g.fillRect(0, 2, 3, getHeight() - 4);
            }
        };
        mark.setPreferredSize(new Dimension(3, 36));

        JPanel names = new JPanel(new BorderLayout(0, 3));
        names.setOpaque(false);
        names.add(Theme.label("LABSTOCK", Theme.BRAND, Theme.TEXT), BorderLayout.NORTH);
        names.add(Theme.label("Electronics Lab Inventory", Theme.SMALL, Theme.TEXT_MUTED), BorderLayout.CENTER);

        brand.add(mark, BorderLayout.WEST);
        brand.add(names, BorderLayout.CENTER);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, brand.getPreferredSize().height));
        return brand;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 3));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER_SOFT),
                new EmptyBorder(14, TEXT_LEFT, 16, 12)));
        footer.add(Theme.label("Java Mini Project", Theme.SMALL, Theme.TEXT_MUTED), BorderLayout.NORTH);
        footer.add(Theme.label("ECS  \u00B7  v1.1", Theme.MONO_SMALL, Theme.TEXT_DIM), BorderLayout.CENTER);
        return footer;
    }

    /** One clickable row of the sidebar. */
    private class NavItem extends JComponent {

        private final String key;
        private final String text;
        private String detail = "";
        private boolean hovered;

        NavItem(String key, String text) {
            this.key = key;
            this.text = text;
            setAlignmentX(LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    select(NavItem.this.key);
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIDEBAR_WIDTH, ITEM_HEIGHT);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, ITEM_HEIGHT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQualityHints(g2);
            int w = getWidth();
            int h = getHeight();
            boolean isSelected = (this == selected);

            if (isSelected) {
                g2.setColor(Theme.PANEL);
                g2.fillRect(0, 0, w, h);
                g2.setColor(Theme.ACCENT);
                g2.fillRect(0, 0, 3, h);                // accent line on the left edge
            } else if (hovered) {
                g2.setColor(Theme.SIDEBAR_HOVER);
                g2.fillRect(0, 0, w, h);
            }

            g2.setFont(Theme.NAV);
            FontMetrics fm = g2.getFontMetrics();
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(isSelected || hovered ? Theme.TEXT : Theme.TEXT_MUTED);
            g2.drawString(text, TEXT_LEFT, y);

            if (!detail.isEmpty()) {
                g2.setFont(Theme.MONO_SMALL);
                FontMetrics small = g2.getFontMetrics();
                int detailY = (h - small.getHeight()) / 2 + small.getAscent();
                g2.setColor(isSelected ? Theme.ACCENT : Theme.TEXT_DIM);
                g2.drawString(detail, w - 20 - small.stringWidth(detail), detailY);
            }
            g2.dispose();
        }
    }
}
