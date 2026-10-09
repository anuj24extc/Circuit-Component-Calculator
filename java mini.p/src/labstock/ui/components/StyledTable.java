package labstock.ui.components;

import labstock.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

/**
 * A read-only JTable with the application's look: compact rows, subtle
 * alternating row colours, small uppercase headings and coloured status
 * "pills". Every table in the app is one of these.
 */
public class StyledTable extends JTable {

    private final DefaultTableModel model;
    private final Map<Integer, Font> columnFonts = new HashMap<>();
    private final Map<Integer, Color> columnColours = new HashMap<>();
    private String emptyText;

    public StyledTable(String... columns) {
        super(readOnlyModel(columns));
        model = (DefaultTableModel) getModel();

        setRowHeight(32);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setBackground(Theme.PANEL);
        setForeground(Theme.TEXT);
        setFont(Theme.BODY);
        setSelectionBackground(Theme.ACCENT_SOFT);
        setSelectionForeground(Theme.TEXT);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(true);
        setDefaultRenderer(Object.class, new CellRenderer());

        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setBackground(Theme.SIDEBAR);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(0, 32));
    }

    private static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // ------------------------------------------------------------ data

    public void clearRows() {
        model.setRowCount(0);
    }

    public void addRow(Object... values) {
        model.addRow(values);
    }

    // ------------------------------------------------------------ appearance

    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length; i++) {
            getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    /** Keeps short columns (IDs, numbers, status) at their width; only the others shrink or grow. */
    public void lockColumnWidths(int... columns) {
        for (int column : columns) {
            int width = getColumnModel().getColumn(column).getPreferredWidth();
            getColumnModel().getColumn(column).setMinWidth(width);
            getColumnModel().getColumn(column).setMaxWidth(width);
        }
    }

    /** Uses a special font/colour for some columns, e.g. monospaced IDs. */
    public void setColumnStyle(Font font, Color colour, int... columns) {
        for (int column : columns) {
            columnFonts.put(column, font);
            columnColours.put(column, colour);
        }
    }

    /** Shows this column's values (READY, LOW STOCK, OVERDUE...) as small coloured pills. */
    public void setStatusColumn(int column) {
        getColumnModel().getColumn(column).setCellRenderer(new StatusRenderer());
    }

    /** Text shown in the middle of the table when it has no rows. */
    public void setEmptyText(String text) {
        this.emptyText = text;
    }

    /** Puts the table in a dark scroll pane, with or without an outer border. */
    public JScrollPane inScrollPane(boolean withBorder) {
        JScrollPane scroll = new JScrollPane(this);
        scroll.setBorder(withBorder ? BorderFactory.createLineBorder(Theme.BORDER) : BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.PANEL);
        scroll.setBackground(Theme.PANEL);
        JPanel corner = new JPanel();
        corner.setBackground(Theme.SIDEBAR);
        scroll.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
        ThinScrollBarUI.apply(scroll, Theme.PANEL);
        return scroll;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getRowCount() == 0 && emptyText != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQualityHints(g2);
            g2.setFont(Theme.SMALL);
            g2.setColor(Theme.TEXT_DIM);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(emptyText, (getWidth() - fm.stringWidth(emptyText)) / 2, 36);
            g2.dispose();
        }
    }

    private static Color rowBackground(int row, boolean selected) {
        if (selected) {
            return Theme.ACCENT_SOFT;
        }
        return (row % 2 == 0) ? Theme.PANEL : Theme.PANEL_ALT;
    }

    // ------------------------------------------------------------ renderers

    /** Normal cells. */
    private class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setBorder(new EmptyBorder(0, 14, 0, 10));
            setBackground(rowBackground(row, selected));
            Font font = columnFonts.get(column);
            Color colour = columnColours.get(column);
            setFont(font != null ? font : Theme.BODY);
            setForeground(colour != null ? colour : Theme.TEXT);
            setToolTipText(value == null ? null : value.toString());   // full text if it is cut off
            return this;
        }
    }

    /** Column headings: small, uppercase, letter-spaced. */
    private static class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(Theme.CAPTION);
            setForeground(Theme.TEXT_DIM);
            setBackground(Theme.SIDEBAR);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    new EmptyBorder(0, 14, 0, 10)));
            return this;
        }
    }

    /** Status cells drawn as a small outlined pill in the status colour. */
    private static class StatusRenderer extends DefaultTableCellRenderer {

        private Color statusColour = Theme.TEXT_DIM;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            statusColour = Theme.statusColor(String.valueOf(value));
            setBackground(rowBackground(row, selected));
            setForeground(statusColour);
            setFont(Theme.PILL);
            setBorder(new EmptyBorder(0, 14, 0, 10));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyQualityHints(g2);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            String text = getText();
            if (text != null && !text.isEmpty()) {
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int pillWidth = fm.stringWidth(text) + 14;
                int pillHeight = 20;
                int x = getInsets().left;
                int y = (getHeight() - pillHeight) / 2;
                g2.setColor(Theme.withAlpha(statusColour, 30));
                g2.fillRoundRect(x, y, pillWidth, pillHeight, 6, 6);
                g2.setColor(Theme.withAlpha(statusColour, 110));
                g2.drawRoundRect(x, y, pillWidth - 1, pillHeight - 1, 6, 6);
                g2.setColor(statusColour);
                g2.drawString(text, x + 7, y + (pillHeight - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
    }
}
