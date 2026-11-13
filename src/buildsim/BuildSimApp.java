package buildsim;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Starts the Swing GUI. (ConsoleApp is the text-menu version.) */
public class BuildSimApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to the default look and feel
        }
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
