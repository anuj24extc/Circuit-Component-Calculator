package labstock;

import labstock.service.FileManager;
import labstock.service.InventoryManager;
import labstock.ui.MainFrame;
import labstock.ui.MessageDialog;
import labstock.ui.Theme;

import javax.swing.SwingUtilities;
import java.io.IOException;

/**
 * Entry point of LABSTOCK - Electronics Lab Component Borrowing & Inventory System.
 */
public class Main {

    /** Folder (next to where the app is started) that holds the data files. */
    private static final String DATA_FOLDER = "data";

    public static void main(String[] args) {
        // Swing must be used from the Event Dispatch Thread
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        Theme.install();
        InventoryManager manager = new InventoryManager(new FileManager(DATA_FOLDER));

        IOException loadError = null;
        try {
            manager.load();          // reads the files, or creates sample data on the first run
        } catch (IOException e) {
            loadError = e;
        }

        MainFrame frame = new MainFrame(manager);
        frame.setVisible(true);

        if (loadError != null) {
            MessageDialog.showError(frame, "Could not load data",
                    "The saved data could not be read:\n" + loadError.getMessage()
                            + "\nThe app has started without it.");
        }
    }
}
