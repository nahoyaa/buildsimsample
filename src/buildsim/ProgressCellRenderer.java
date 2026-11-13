package buildsim;

import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/** Draws a progress bar inside a table cell (the cell value is a percentage number). */
@SuppressWarnings("serial")
class ProgressCellRenderer extends JProgressBar implements TableCellRenderer {
    ProgressCellRenderer() {
        super(0, 100);
        setStringPainted(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                   boolean focused, int row, int column) {
        double pct = (value instanceof Number n) ? n.doubleValue() : 0;
        setValue((int) Math.round(pct));
        setString(Fmt.pct(pct));
        return this;
    }
}
