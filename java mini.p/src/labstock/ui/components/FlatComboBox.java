package labstock.ui.components;

import labstock.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * A drop-down list in the dark style. The standard Swing look would show a
 * light grey box, so the box, the arrow and the list are painted here.
 *
 * Subclasses can override textFor() to change how an option is displayed.
 */
public class FlatComboBox<E> extends JComboBox<E> {

    private static final int BOX_HEIGHT = 34;
    private final int boxWidth;

    public FlatComboBox(int width) {
        this.boxWidth = width;
        setUI(new FlatComboBoxUI());
        setOpaque(false);
        setFont(Theme.BODY);
        setForeground(Theme.TEXT);
        setBackground(Theme.INPUT);
        setBorder(new EmptyBorder(1, 1, 1, 1));
        setMaximumRowCount(10);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setRenderer(new OptionRenderer());
    }

    /** The text shown for one option. Plain toString() by default. */
    protected String textFor(Object value) {
        return value == null ? "" : value.toString();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(boxWidth, BOX_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /** Draws each option: index -1 is the value shown inside the closed box. */
    private class OptionRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, false);
            setText(textFor(value));
            setFont(Theme.BODY);
            if (index < 0) {
                setBorder(new EmptyBorder(0, 10, 0, 4));
                setBackground(Theme.INPUT);
                setForeground(Theme.TEXT);
            } else {
                setBorder(new EmptyBorder(7, 10, 7, 10));
                setBackground(selected ? Theme.ACCENT_SOFT : Theme.PANEL_ALT);
                setForeground(selected ? Theme.TEXT : Theme.TEXT_MUTED);
            }
            return this;
        }
    }

    /** Paints the closed box (rounded, thin border) and a simple chevron arrow. */
    private static class FlatComboBoxUI extends BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            JButton arrow = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.applyQualityHints(g2);
                    g2.setColor(Theme.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    g2.drawPolyline(new int[] {cx - 4, cx, cx + 4}, new int[] {cy - 2, cy + 2, cy - 2}, 3);
                    g2.dispose();
                }
            };
            arrow.setBorder(BorderFactory.createEmptyBorder());
            arrow.setContentAreaFilled(false);
            arrow.setOpaque(false);
            arrow.setFocusable(false);
            return arrow;
        }

        @Override
        protected ComboPopup createPopup() {
            BasicComboPopup popup = new BasicComboPopup(comboBox) {
                @Override
                protected JScrollPane createScroller() {
                    JScrollPane scroller = super.createScroller();
                    ThinScrollBarUI.apply(scroller, Theme.PANEL_ALT);
                    return scroller;
                }
            };
            popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            return popup;
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQualityHints(g2);
            g2.setColor(Theme.INPUT);
            g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 6, 6);
            boolean active = comboBox.isFocusOwner() || isPopupVisible(comboBox);
            g2.setColor(active ? Theme.ACCENT : Theme.BORDER);
            g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 6, 6);
            g2.dispose();
            super.paint(g, c);         // draws the selected option
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // the background was already painted in paint()
        }

        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            super.paintCurrentValue(g, bounds, false);   // no blue highlight inside the box
        }
    }
}
