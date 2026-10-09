package labstock.ui.components;

import labstock.ui.Theme;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;

/**
 * A panel with mildly rounded corners, a thin border and a faint shadow.
 * Used for the cards: statistics, forms, details.
 */
public class RoundedPanel extends JPanel {

    private static final int ARC = 8;
    private static final int SHADOW = 2;   // pixels reserved below the card for the shadow

    public RoundedPanel(LayoutManager layout, int paddingY, int paddingX) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(paddingY, paddingX, paddingY + SHADOW, paddingX));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyQualityHints(g2);
        int w = getWidth();
        int h = getHeight() - SHADOW;

        g2.setColor(new Color(0, 0, 0, 70));          // shadow
        g2.fillRoundRect(0, SHADOW, w, h, ARC, ARC);

        g2.setColor(Theme.PANEL);                      // card
        g2.fillRoundRect(0, 0, w, h, ARC, ARC);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);

        g2.setColor(new Color(255, 255, 255, 10));     // faint highlight along the top edge
        g2.drawLine(ARC / 2, 1, w - ARC / 2, 1);
        g2.dispose();
    }
}
