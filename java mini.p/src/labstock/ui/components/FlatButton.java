package labstock.ui.components;

import labstock.ui.Theme;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Flat button in one of two styles:
 *   PRIMARY   - filled blue (BORROW COMPONENT, SAVE)
 *   SECONDARY - outlined (CLEAR, CANCEL, EDIT)
 * The colour changes slightly on hover and when pressed.
 */
public class FlatButton extends JButton {

    public enum Kind { PRIMARY, SECONDARY }

    public static final int BUTTON_HEIGHT = 34;
    private static final int ARC = 6;

    private final Kind kind;
    private boolean hovered;

    public FlatButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(Theme.BUTTON);
        setContentAreaFilled(false);   // the background is painted below
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRequestFocusEnabled(false); // a mouse click leaves the focus where it was
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(0, 16, 0, 16));
        setForeground(kind == Kind.PRIMARY ? Color.WHITE : Theme.TEXT_MUTED);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                updateForeground();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                updateForeground();
            }
        });
    }

    private void updateForeground() {
        if (kind == Kind.SECONDARY) {
            setForeground(hovered && isEnabled() ? Theme.TEXT : Theme.TEXT_MUTED);
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = BUTTON_HEIGHT;
        return size;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();   // stops BoxLayout from stretching buttons
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQualityHints(g2);
        int w = getWidth();
        int h = getHeight();
        boolean pressed = getModel().isPressed() && getModel().isArmed();
        boolean hot = hovered && isEnabled();

        if (kind == Kind.PRIMARY) {
            Color fill = !isEnabled() ? Theme.BORDER
                    : pressed ? Theme.ACCENT_PRESS
                    : hot ? Theme.ACCENT_HOVER
                    : Theme.ACCENT;
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, ARC, ARC);
        } else {
            g2.setColor(pressed ? Theme.BORDER_SOFT : hot ? Theme.PANEL_ALT : Theme.PANEL);
            g2.fillRoundRect(0, 0, w, h, ARC, ARC);
            g2.setColor(hot ? Theme.TEXT_DIM : Theme.BORDER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        }

        // thin ring when the button is reached with the TAB key
        if (isFocusOwner()) {
            g2.setColor(kind == Kind.PRIMARY ? Theme.TEXT : Theme.ACCENT);
            g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        }
        g2.dispose();
        super.paintComponent(g);   // draws the text
    }
}
