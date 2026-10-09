package labstock.ui;

import labstock.model.Activity;
import labstock.model.Item;
import labstock.service.DateUtil;
import labstock.service.InventoryManager;
import labstock.ui.components.RoundedPanel;
import labstock.ui.components.StyledTable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalTime;
import java.util.List;

/**
 * Overview of the lab: four small statistics, the latest activity and the
 * items that are running low.
 */
public class DashboardPanel extends BasePage {

    private static final int ACTIVITY_ROWS = 8;

    private final StatBox totalBox = new StatBox("TOTAL ITEMS");
    private final StatBox availableBox = new StatBox("AVAILABLE");
    private final StatBox borrowedBox = new StatBox("BORROWED");
    private final StatBox lowStockBox = new StatBox("LOW STOCK");
    private final StyledTable activityTable = new StyledTable("TIME", "ACTIVITY");
    private final StyledTable lowStockTable = new StyledTable("ID", "ITEM", "AVAIL", "MIN");

    public DashboardPanel(InventoryManager manager) {
        super(manager, "Lab Inventory", null);

        JPanel stats = new JPanel(new GridLayout(1, 4, 14, 0));
        stats.setOpaque(false);
        stats.add(totalBox);
        stats.add(availableBox);
        stats.add(borrowedBox);
        stats.add(lowStockBox);

        activityTable.setColumnWidths(132, 420);
        activityTable.lockColumnWidths(0);
        activityTable.setColumnStyle(Theme.MONO_SMALL, Theme.TEXT_DIM, 0);
        activityTable.setEmptyText("No activity yet.");
        activityTable.setRowSelectionAllowed(false);

        lowStockTable.setColumnWidths(60, 170, 64, 52);
        lowStockTable.lockColumnWidths(0, 2, 3);
        lowStockTable.setColumnStyle(Theme.MONO_FONT, Theme.TEXT_MUTED, 0, 3);
        lowStockTable.setColumnStyle(Theme.MONO_FONT, Theme.WARNING, 2);
        lowStockTable.setEmptyText("Every item is above its minimum stock.");
        lowStockTable.setRowSelectionAllowed(false);

        // the two cards share the width 56 : 44
        JPanel lower = new JPanel(new GridBagLayout());
        lower.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.56;
        c.insets = new Insets(0, 0, 0, 14);
        lower.add(card("RECENT ACTIVITY", "LATEST FIRST", activityTable), c);
        c.gridx = 1;
        c.weightx = 0.44;
        c.insets = new Insets(0, 0, 0, 0);
        lower.add(card("LOW STOCK", "BELOW MINIMUM", lowStockTable), c);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(stats, BorderLayout.NORTH);
        body.add(lower, BorderLayout.CENTER);
        setBody(body);
    }

    @Override
    public void refresh() {
        setOverline(greeting());

        int totalUnits = manager.getTotalUnits();
        int availableUnits = manager.getAvailableUnits();
        int borrowedUnits = manager.getBorrowedUnits();
        int openLoans = manager.getOpenTransactions().size();
        int overdue = manager.getOverdueCount();
        List<Item> lowItems = manager.getLowStockItems();

        totalBox.set(manager.getItems().size(), totalUnits + " units in the lab", Theme.TEXT);
        availableBox.set(availableUnits, "units ready to issue", Theme.TEXT);
        borrowedBox.set(borrowedUnits, openLoans + " open loans"
                + (overdue > 0 ? "  \u00B7  " + overdue + " overdue" : ""), Theme.TEXT);
        lowStockBox.set(lowItems.size(), lowItems.isEmpty() ? "all items stocked" : "items need restocking",
                lowItems.isEmpty() ? Theme.TEXT : Theme.WARNING);

        activityTable.clearRows();
        for (Activity activity : manager.getRecentActivity(ACTIVITY_ROWS)) {
            activityTable.addRow(DateUtil.friendly(activity.getTime()), activity.getMessage());
        }

        lowStockTable.clearRows();
        for (Item item : lowItems) {
            lowStockTable.addRow(item.getId(), item.getName(), item.getAvailableQuantity(), item.getMinimumStock());
        }
    }

    private static String greeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return "Good morning";
        }
        if (hour < 17) {
            return "Good afternoon";
        }
        return "Good evening";
    }

    /** A card with a caption row and a table filling the rest. */
    private static JComponent card(String title, String hint, StyledTable table) {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.caption(title), BorderLayout.WEST);
        top.add(Theme.label(hint, Theme.PILL, Theme.TEXT_DIM), BorderLayout.EAST);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12), 16, 16);
        card.add(top, BorderLayout.NORTH);
        card.add(table.inScrollPane(true), BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(1, 1));   // let the layout share the width by weight
        return card;
    }

    /** One small statistic: caption, number and a short note underneath. */
    private static class StatBox extends RoundedPanel {

        private final JLabel value = Theme.label("0", Theme.STAT_VALUE, Theme.TEXT);
        private final JLabel note = Theme.label(" ", Theme.SMALL, Theme.TEXT_DIM);

        StatBox(String caption) {
            super(new GridBagLayout(), 14, 18);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.weightx = 1;
            c.anchor = GridBagConstraints.WEST;
            c.gridy = 0;
            add(Theme.caption(caption), c);
            c.gridy = 1;
            c.insets = new Insets(4, 0, 0, 0);
            add(value, c);
            c.gridy = 2;
            c.insets = new Insets(0, 0, 0, 0);
            add(note, c);
        }

        void set(int number, String text, Color colour) {
            value.setText(String.valueOf(number));
            value.setForeground(colour);
            note.setText(text);
        }
    }
}
