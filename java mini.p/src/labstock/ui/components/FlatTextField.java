package labstock.ui.components;

import labstock.ui.Theme;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * Text box used in every form:
 *  - dark background with a thin grey border
 *  - the border turns blue while the box has keyboard focus
 *  - a dim placeholder is shown while the box is empty
 */
public class FlatTextField extends JTextField {

    public static final int FIELD_HEIGHT = 34;
    private static final int ARC = 6;

    private final String placeholder;
    private final int fieldWidth;

    public FlatTextField(String placeholder, int width) {
        this.placeholder = placeholder;
        this.fieldWidth = width;
        setFont(Theme.BODY);
        setForeground(Theme.TEXT);
        setCaretColor(Theme.TEXT);
        setSelectionColor(Theme.SELECTION);
        setSelectedTextColor(Theme.TEXT);
        setOpaque(false);
        setBorder(new EmptyBorder(0, 10, 0, 10));

        // repaint so the border colour follows the focus
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(fieldWidth, FIELD_HEIGHT);
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
        g2.setColor(Theme.INPUT);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);

        if (getText().isEmpty() && placeholder != null) {
            g2.setFont(getFont());
            g2.setColor(Theme.TEXT_DIM);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(placeholder, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        }
        g2.dispose();

        super.paintComponent(g);   // text, caret and selection

        Graphics2D border = (Graphics2D) g.create();
        Theme.applyQualityHints(border);
        border.setColor(isFocusOwner() ? Theme.ACCENT : Theme.BORDER);
        border.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        border.dispose();
    }
}
