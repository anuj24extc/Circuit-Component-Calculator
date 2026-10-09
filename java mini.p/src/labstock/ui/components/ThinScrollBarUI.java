package labstock.ui.components;

import labstock.ui.Theme;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * A slim scroll bar without arrow buttons that matches the dark theme.
 */
public class ThinScrollBarUI extends BasicScrollBarUI {

    private final Color trackColor;

    public ThinScrollBarUI(Color trackColor) {
        this.trackColor = trackColor;
    }

    /** Gives both scroll bars of a scroll pane the slim look. */
    public static void apply(JScrollPane pane, Color trackColor) {
        pane.getVerticalScrollBar().setUI(new ThinScrollBarUI(trackColor));
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        pane.getVerticalScrollBar().setUnitIncrement(16);
        // some scroll panes (e.g. inside a drop-down list) have no horizontal bar at all
        if (pane.getHorizontalScrollBar() != null) {
            pane.getHorizontalScrollBar().setUI(new ThinScrollBarUI(trackColor));
            pane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        }
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return invisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return invisibleButton();
    }

    private static JButton invisibleButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        g.setColor(trackColor);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQualityHints(g2);
        g2.setColor(isDragging || isThumbRollover() ? Theme.TEXT_DIM : Theme.BORDER);
        g2.fillRoundRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4, 6, 6);
        g2.dispose();
    }
}
