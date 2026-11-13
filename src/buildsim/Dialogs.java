package buildsim;

import javax.swing.JOptionPane;
import java.awt.Component;

/** Small helpers for the pop-up dialogs used by the screens. */
final class Dialogs {
    private Dialogs() { }

    @SuppressWarnings("unchecked")
    static <T> T choose(Component parent, String message, T[] options) {
        if (options.length == 0) {
            info(parent, "Nothing to choose from yet.");
            return null;
        }
        return (T) JOptionPane.showInputDialog(parent, message, "BuildSim",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
    }

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "BuildSim", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "BuildSim", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "BuildSim",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /** Reads a number from text; shows an error and returns null if it is not valid. */
    static Double number(Component parent, String text, String what, double min) {
        try {
            double v = Double.parseDouble(text.trim().replace(",", ""));
            if (!Double.isFinite(v) || v < min) {
                error(parent, what + " must be at least " + Fmt.num1(min) + ".");
                return null;
            }
            return v;
        } catch (NumberFormatException e) {
            error(parent, what + " must be a number.");
            return null;
        }
    }
}
