package labstock.ui;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Colours, fonts and small helpers shared by every screen, so the whole
 * application can be re-styled from this one class.
 */
public final class Theme {

    private Theme() { }

    // ------------------------------------------------------------ colours
    // Dark lab palette: navy-black surfaces, off-white text, one blue accent.
    // Amber and red are used only for warning statuses (low stock, overdue).

    public static final Color BACKGROUND    = new Color(0x0B0F16);
    public static final Color SIDEBAR       = new Color(0x0E131B);
    public static final Color SIDEBAR_HOVER = new Color(0x111723);
    public static final Color PANEL         = new Color(0x121924);
    public static final Color PANEL_ALT     = new Color(0x161E2B);
    public static final Color INPUT         = new Color(0x0D121A);
    public static final Color BORDER        = new Color(0x253041);
    public static final Color BORDER_SOFT   = new Color(0x1B2331);
    public static final Color TEXT          = new Color(0xE6E9EF);
    public static final Color TEXT_MUTED    = new Color(0x8D97A8);
    public static final Color TEXT_DIM      = new Color(0x5A6476);
    public static final Color ACCENT        = new Color(0x3B8EFF);
    public static final Color ACCENT_HOVER  = new Color(0x5BA1FF);
    public static final Color ACCENT_PRESS  = new Color(0x2C77E0);
    public static final Color ACCENT_SOFT   = new Color(0x16263F);
    public static final Color SELECTION     = new Color(0x264F86);
    public static final Color WARNING       = new Color(0xD29922);
    public static final Color ERROR         = new Color(0xE5534B);

    // ------------------------------------------------------------ fonts
    // Segoe UI / Consolas on Windows, with safe fallbacks elsewhere.

    private static final Set<String> INSTALLED_FONTS = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

    private static final String SANS     = firstInstalled(Font.SANS_SERIF, "Segoe UI", "Helvetica Neue", "Arial");
    private static final String SEMIBOLD = firstInstalled(null, "Segoe UI Semibold");
    private static final String MONO     = firstInstalled(Font.MONOSPACED, "Consolas", "Menlo", "DejaVu Sans Mono");

    public static final Font BRAND      = tracked(new Font(SANS, Font.BOLD, 15), 0.14f);
    public static final Font TITLE      = semibold(24);
    public static final Font STAT_VALUE = semibold(26);
    public static final Font HEADING    = semibold(16);
    public static final Font BODY       = new Font(SANS, Font.PLAIN, 13);
    public static final Font SMALL      = new Font(SANS, Font.PLAIN, 12);
    public static final Font CAPTION    = tracked(semibold(11), 0.12f);
    public static final Font PILL       = tracked(semibold(10), 0.08f);
    public static final Font NAV        = tracked(semibold(12), 0.06f);
    public static final Font BUTTON     = tracked(semibold(12), 0.08f);
    public static final Font MONO_FONT  = new Font(MONO, Font.PLAIN, 13);
    public static final Font MONO_SMALL = new Font(MONO, Font.PLAIN, 11);
    public static final Font MONO_LARGE = new Font(MONO, Font.PLAIN, 17);

    private static String firstInstalled(String fallback, String... names) {
        for (String name : names) {
            if (INSTALLED_FONTS.contains(name)) {
                return name;
            }
        }
        return fallback;
    }

    public static Font semibold(int size) {
        return SEMIBOLD != null ? new Font(SEMIBOLD, Font.PLAIN, size) : new Font(SANS, Font.BOLD, size);
    }

    /** Adds letter-spacing, used for the small uppercase labels. */
    public static Font tracked(Font font, float tracking) {
        return font.deriveFont(Collections.singletonMap(TextAttribute.TRACKING, tracking));
    }

    // ------------------------------------------------------------ setup

    /** Dark defaults for the few standard Swing parts we do not paint ourselves. */
    public static void install() {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        UIManager.put("Button.disabledText", TEXT_DIM);
        UIManager.put("ComboBox.background", INPUT);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("ComboBox.selectionBackground", ACCENT_SOFT);
        UIManager.put("ComboBox.selectionForeground", TEXT);
        UIManager.put("ToolTip.background", PANEL_ALT);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.font", SMALL);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(4, 8, 4, 8)));
    }

    // ------------------------------------------------------------ label helpers

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    /** Small, letter-spaced uppercase label such as "STUDENT NAME". */
    public static JLabel caption(String text) {
        return label(text, CAPTION, TEXT_DIM);
    }

    /** A caption placed above a component (used for form fields). */
    public static JPanel captioned(String caption, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        panel.add(caption(caption), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    /** A 1 px horizontal line. */
    public static JComponent divider() {
        JComponent line = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(BORDER_SOFT);
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        line.setPreferredSize(new Dimension(1, 1));
        return line;
    }

    // ------------------------------------------------------------ status colours and icons

    /** Colour for a status word in the tables. */
    public static Color statusColor(String status) {
        switch (status) {
            case "READY":
            case "BORROWED":
                return ACCENT;
            case "LOW STOCK":
            case "OVERDUE":
                return WARNING;
            case "OUT OF STOCK":
                return ERROR;
            case "ISSUED":                 // consumable given out for good
                return TEXT_MUTED;
            default:                       // RETURNED
                return TEXT_DIM;
        }
    }

    /** Runs the given code every time the text in a box changes (typing, pasting, deleting). */
    public static void onTextChange(JTextComponent field, final Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    /** A small filled circle, used as a status dot next to text. */
    public static Icon dot(final Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyQualityHints(g2);
                g2.setColor(color);
                g2.fillOval(x, y + 1, 7, 7);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 7;
            }

            @Override
            public int getIconHeight() {
                return 9;
            }
        };
    }

    /** A small check mark, drawn rather than typed so it looks the same on every font. */
    public static Icon check(final Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                applyQualityHints(g2);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[] {x + 1, x + 5, x + 12}, new int[] {y + 7, y + 11, y + 3}, 3);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 13;
            }

            @Override
            public int getIconHeight() {
                return 14;
            }
        };
    }

    // ------------------------------------------------------------ small utilities

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** "#5A6476" - for colouring part of an HTML label. */
    public static String hex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /** Makes text safe to put inside an HTML label. */
    public static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Smooth edges and system-quality text for custom painted components. */
    public static void applyQualityHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Object desktopHints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        if (desktopHints instanceof Map) {
            g2.addRenderingHints((Map<?, ?>) desktopHints);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
    }
}
