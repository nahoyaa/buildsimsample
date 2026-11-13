package buildsim;

import javax.swing.*;
import java.awt.*;

/** Shown on a screen when there is no project yet. */
@SuppressWarnings("serial")
class EmptyState extends JPanel {
    EmptyState(String message) {
        setLayout(new GridBagLayout());
        JLabel l = new JLabel(message);
        l.setForeground(new Color(0x666666));
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 14f));
        add(l);
    }
}
