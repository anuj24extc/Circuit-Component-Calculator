package labstock.ui;

import labstock.service.InventoryManager;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The main window: sidebar on the left; on the right a top bar, the current
 * page (switched with a CardLayout) and a status bar.
 */
public class MainFrame extends JFrame {

    // keys used by the sidebar and the CardLayout
    private static final String DASHBOARD = "dashboard";
    private static final String INVENTORY = "inventory";
    private static final String BORROW = "borrow";
    private static final String RETURNS = "returns";
    private static final String HISTORY = "history";

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("EEE dd MMM uuuu   HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter SAVED_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final InventoryManager manager;
    private final CardLayout cards = new CardLayout();
    private final JPanel pageArea = new JPanel(cards);
    private final Map<String, BasePage> pages = new LinkedHashMap<>();
    private final Map<String, String> pageNames = new HashMap<>();
    private final Sidebar sidebar = new Sidebar(this::showPage);
    private final JLabel breadcrumb = Theme.caption(" ");
    private final JLabel clock = Theme.label(" ", Theme.MONO_SMALL, Theme.TEXT_MUTED);
    private final JLabel statusLeft = Theme.label(" ", Theme.MONO_SMALL, Theme.TEXT_DIM);
    private final JLabel statusRight = Theme.label(" ", Theme.MONO_SMALL, Theme.TEXT_DIM);

    public MainFrame(InventoryManager manager) {
        super("LABSTOCK - Electronics Lab Inventory");
        this.manager = manager;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(createIcon());

        // one object of each page
        pageArea.setBackground(Theme.BACKGROUND);
        addPage(DASHBOARD, "DASHBOARD", new DashboardPanel(manager));
        addPage(INVENTORY, "INVENTORY", new InventoryPanel(manager));
        addPage(BORROW, "BORROW", new BorrowPanel(manager));
        addPage(RETURNS, "RETURNS", new ReturnPanel(manager));
        addPage(HISTORY, "HISTORY", new HistoryPanel(manager));

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BACKGROUND);
        content.add(buildTopBar(), BorderLayout.NORTH);
        content.add(pageArea, BorderLayout.CENTER);
        content.add(buildStatusBar(), BorderLayout.SOUTH);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BACKGROUND);
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        // every change made through the manager refreshes all screens
        manager.addChangeListener(this::refreshAll);

        // the clock in the top bar updates every 20 seconds
        updateClock();
        new Timer(20_000, e -> updateClock()).start();

        // about 1100 x 700, but never larger than the screen
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1100, screen.width), Math.min(700, screen.height));
        setMinimumSize(new Dimension(Math.min(1000, screen.width), Math.min(640, screen.height)));
        setLocationRelativeTo(null);

        refreshAll();
        sidebar.select(DASHBOARD);
    }

    private void addPage(String key, String name, BasePage page) {
        pages.put(key, page);
        pageNames.put(key, name);
        pageArea.add(page, key);
        sidebar.addItem(key, name);
    }

    /** Called by the sidebar. Pages are refreshed when shown so OVERDUE stays correct. */
    private void showPage(String key) {
        pages.get(key).refresh();
        cards.show(pageArea, key);
        breadcrumb.setText("LABSTOCK  /  " + pageNames.get(key));
    }

    private void refreshAll() {
        for (BasePage page : pages.values()) {
            page.refresh();
        }
        sidebar.setDetail(INVENTORY, String.valueOf(manager.getItems().size()));
        sidebar.setDetail(RETURNS, String.valueOf(manager.getOpenTransactions().size()));

        int overdue = manager.getOverdueCount();
        statusLeft.setText(manager.getItems().size() + " items  \u00B7  "
                + manager.getBorrowedUnits() + " units on loan  \u00B7  "
                + overdue + (overdue == 1 ? " loan overdue" : " loans overdue"));
        LocalTime saved = manager.getLastSaved();
        statusRight.setText(manager.getDataFolder() + "/  \u00B7  "
                + (saved == null ? "loaded" : "saved " + saved.format(SAVED_FORMAT)));
    }

    private void updateClock() {
        clock.setText(LocalDateTime.now().format(CLOCK_FORMAT).toUpperCase(Locale.ROOT));
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SIDEBAR);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(14, 28, 13, 24)));
        bar.add(breadcrumb, BorderLayout.WEST);
        bar.add(clock, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.SIDEBAR);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                new EmptyBorder(6, 28, 6, 24)));
        bar.add(statusLeft, BorderLayout.WEST);
        bar.add(statusRight, BorderLayout.EAST);
        return bar;
    }

    /** Window icon: a blue IC outline on a dark square. */
    private static Image createIcon() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Theme.applyQualityHints(g);
        g.setColor(Theme.PANEL);
        g.fillRoundRect(0, 0, 32, 32, 8, 8);
        g.setColor(Theme.ACCENT);
        g.setStroke(new BasicStroke(2f));
        g.drawRect(10, 7, 12, 18);                  // chip body
        for (int y = 10; y <= 22; y += 4) {         // pins
            g.drawLine(5, y, 9, y);
            g.drawLine(23, y, 27, y);
        }
        g.dispose();
        return image;
    }
}
