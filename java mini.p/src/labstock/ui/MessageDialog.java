package labstock.ui;

import labstock.ui.components.FlatButton;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Locale;

/**
 * Small modal dialogs in the app's dark style: errors, success messages
 * and yes/no confirmations. (JOptionPane would appear as a light grey box.)
 */
public final class MessageDialog {

    private MessageDialog() { }

    public static void showError(Component parent, String title, String message) {
        show(parent, title, message, "OK", null, Theme.ERROR, null);
    }

    public static void showSuccess(Component parent, String title, String message) {
        show(parent, title, message, "OK", null, Theme.ACCENT, Theme.check(Theme.ACCENT));
    }

    /** Asks a question; returns true only if the user pressed the confirm button. */
    public static boolean confirm(Component parent, String title, String message, String confirmText) {
        return show(parent, title, message, confirmText, "CANCEL", Theme.ACCENT, null);
    }

    /** Border shared by all dialogs: a coloured line on top, thin grey sides. */
    public static Border dialogBorder(Color markerColour) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, markerColour),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 1, 1, 1, Theme.BORDER),
                        new EmptyBorder(18, 22, 18, 22)));
    }

    private static boolean show(Component parent, String title, String message, String okText,
                                String cancelText, Color markerColour, Icon icon) {
        Window owner = (parent == null) ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        final boolean[] confirmed = {false};   // array, so the button's lambda can change it

        JLabel heading = Theme.label(title.toUpperCase(Locale.ROOT), Theme.CAPTION, Theme.TEXT);
        heading.setIcon(icon);
        heading.setIconTextGap(8);
        JLabel text = Theme.label(toHtml(message), Theme.BODY, Theme.TEXT_MUTED);

        FlatButton okButton = new FlatButton(okText, FlatButton.Kind.PRIMARY);
        okButton.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(8, 0, 0, 0));
        buttons.add(Box.createHorizontalGlue());
        if (cancelText != null) {
            FlatButton cancelButton = new FlatButton(cancelText, FlatButton.Kind.SECONDARY);
            cancelButton.addActionListener(e -> dialog.dispose());
            buttons.add(cancelButton);
            buttons.add(Box.createHorizontalStrut(10));
        } else {
            dialog.getRootPane().setDefaultButton(okButton);   // ENTER closes a plain message
        }
        buttons.add(okButton);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(Theme.PANEL);
        content.setBorder(dialogBorder(markerColour));
        content.add(heading, BorderLayout.NORTH);
        content.add(text, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        // ESC closes without confirming
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);   // waits here until the dialog is closed
        return confirmed[0];
    }

    /** HTML lets long messages wrap; "\n" becomes a line break. */
    private static String toHtml(String message) {
        return "<html><div style='width:300px'>"
                + Theme.escapeHtml(message).replace("\n", "<br>") + "</div></html>";
    }
}
