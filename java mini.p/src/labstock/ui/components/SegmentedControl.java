package labstock.ui.components;

import labstock.ui.Theme;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * A row of joined buttons where exactly one option is selected, e.g.
 *   [ ALL | BORROWED | OVERDUE | RETURNED ]
 */
public class SegmentedControl extends JComponent {

    private static final int SEGMENT_HEIGHT = 34;
    private static final int PADDING = 14;

    private final String[] options;
    private final List<Runnable> listeners = new ArrayList<>();
    private int selectedIndex;
    private int hoverIndex = -1;

    public SegmentedControl(String... options) {
        this.options = options;
        setFont(Theme.CAPTION);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoverIndex(indexAt(e.getX()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoverIndex(-1);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int index = indexAt(e.getX());
                if (index >= 0 && index != getSelectedIndex()) {
                    setSelectedIndex(index);
                    for (Runnable listener : listeners) {
                        listener.run();
                    }
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public String getSelectedOption() {
        return options[selectedIndex];
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        repaint();
    }

    /** Runs the given code every time the user picks a different option. */
    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    private void setHoverIndex(int index) {
        if (index != hoverIndex) {
            hoverIndex = index;
            repaint();
        }
    }

    /** x position where each segment starts; the last value is the total width. */
    private int[] edges() {
        FontMetrics fm = getFontMetrics(getFont());
        int[] edges = new int[options.length + 1];
        for (int i = 0; i < options.length; i++) {
            edges[i + 1] = edges[i] + fm.stringWidth(options[i]) + 2 * PADDING;
        }
        return edges;
    }

    private int indexAt(int x) {
        int[] edges = edges();
        for (int i = 0; i < options.length; i++) {
            if (x >= edges[i] && x < edges[i + 1]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(edges()[options.length], SEGMENT_HEIGHT);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQualityHints(g2);
        int[] edges = edges();
        int w = edges[options.length];
        int h = getHeight();

        g2.setColor(Theme.INPUT);
        g2.fillRoundRect(0, 0, w, h, 6, 6);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

        for (int i = 0; i < options.length; i++) {
            int x = edges[i];
            int segmentWidth = edges[i + 1] - x;
            boolean selected = (i == selectedIndex);

            if (selected) {
                g2.setColor(Theme.ACCENT_SOFT);
                g2.fillRoundRect(x + 2, 2, segmentWidth - 4, h - 4, 4, 4);
                g2.setColor(new java.awt.Color(0x28579A));
                g2.drawRoundRect(x + 2, 2, segmentWidth - 5, h - 5, 4, 4);
            } else if (i == hoverIndex) {
                g2.setColor(Theme.PANEL_ALT);
                g2.fillRoundRect(x + 2, 2, segmentWidth - 4, h - 4, 4, 4);
            }

            // thin divider between two unselected neighbours
            if (i > 0 && !selected && i - 1 != selectedIndex) {
                g2.setColor(Theme.BORDER_SOFT);
                g2.drawLine(x, 9, x, h - 10);
            }

            g2.setColor(selected || i == hoverIndex ? Theme.TEXT : Theme.TEXT_MUTED);
            g2.drawString(options[i], x + PADDING, textY);
        }

        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
        g2.dispose();
    }
}
