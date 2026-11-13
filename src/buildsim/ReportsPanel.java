package buildsim;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Reports screen: cost summary with a progress chart, task progress, daily log, material stock, workforce. */
@SuppressWarnings("serial")
public class ReportsPanel extends JPanel {
    private final AppSession session;
    private final CardLayout screens = new CardLayout();
    private final CardLayout cards = new CardLayout();

    private final DefaultTableModel costModel = readOnly("Item", "Amount (PHP)");
    private final DefaultTableModel taskModel = readOnly("Task", "Progress", "Started", "Finished", "Status");
    private final DefaultTableModel logModel = readOnly("Date", "Progress", "Workers", "Salary", "Materials", "Equipment", "Events");
    private final DefaultTableModel stockModel = readOnly("Material", "In stock", "Unit", "Cost per unit", "Stock value");
    private final DefaultTableModel shipModel = readOnly("Material", "Quantity", "Expected", "Status");
    private final DefaultTableModel workerModel = readOnly("Worker", "Daily wage");
    private final JLabel workforceStats = new JLabel();
    private final ProgressChart chart = new ProgressChart();

    public ReportsPanel(AppSession session) {
        this.session = session;
        setLayout(screens);
        add(new EmptyState("Create or load a project first (Project tab)."), "empty");
        add(buildMain(), "main");
        session.addListener(this::refresh);
        refresh();
    }

    private JPanel buildMain() {
        JPanel stack = new JPanel(cards);
        stack.add(costCard(), "Cost summary");
        stack.add(scroll(taskTable()), "Task progress");
        stack.add(scroll(logTable()), "Daily log");
        stack.add(stockCard(), "Material stock");
        stack.add(workforceCard(), "Workforce");

        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ButtonGroup group = new ButtonGroup();
        for (String name : new String[]{"Cost summary", "Task progress", "Daily log", "Material stock", "Workforce"}) {
            JToggleButton t = new JToggleButton(name, name.equals("Cost summary"));
            t.setName("report" + name.replace(" ", ""));
            t.addActionListener(e -> cards.show(stack, name));
            group.add(t);
            toggles.add(t);
        }

        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.add(toggles, BorderLayout.NORTH);
        main.add(stack, BorderLayout.CENTER);
        return main;
    }

    private JPanel costCard() {
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.add(heading("Cost summary"), BorderLayout.NORTH);
        left.add(scroll(plainTable(costModel)), BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.add(heading("Progress over time"), BorderLayout.NORTH);
        right.add(chart, BorderLayout.CENTER);
        JPanel p = new JPanel(new GridLayout(1, 2, 14, 0));
        p.add(left);
        p.add(right);
        return p;
    }

    private JTable taskTable() {
        JTable t = plainTable(taskModel);
        t.getColumnModel().getColumn(1).setCellRenderer(new ProgressCellRenderer());
        return t;
    }

    private JTable logTable() {
        JTable t = plainTable(logModel);
        int[] widths = {90, 70, 70, 90, 100, 90, 420};
        for (int i = 0; i < widths.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        return t;
    }

    private JPanel stockCard() {
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.add(new JLabel("Shipments"), BorderLayout.NORTH);
        bottom.add(scroll(plainTable(shipModel)), BorderLayout.CENTER);
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(new JLabel("Material stock"), BorderLayout.NORTH);
        top.add(scroll(plainTable(stockModel)), BorderLayout.CENTER);
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 12));
        p.add(top);
        p.add(bottom);
        return p;
    }

    private JPanel workforceCard() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.add(scroll(plainTable(workerModel)), BorderLayout.CENTER);
        p.add(workforceStats, BorderLayout.SOUTH);
        return p;
    }

    // ---------- helpers ----------

    private static DefaultTableModel readOnly(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable plainTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(24);
        return t;
    }

    private static JScrollPane scroll(JTable t) {
        return new JScrollPane(t);
    }

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        return l;
    }

    // ---------- data ----------

    private void refresh() {
        if (!session.hasProject()) {
            screens.show(this, "empty");
            return;
        }
        screens.show(this, "main");
        Project p = session.getProject();
        SimulationEngine e = session.getEngine();
        List<DailyLog> logs = e == null ? new ArrayList<>() : e.getLogs();

        double labor = e == null ? 0 : e.getCosts().getLabor();
        double material = e == null ? 0 : e.getCosts().getMaterial();
        double equipment = e == null ? 0 : e.getCosts().getEquipment();
        double total = labor + material + equipment;
        double remaining = p.getBudget() - total;
        costModel.setRowCount(0);
        costModel.addRow(new Object[]{"Labor (salaries)", Fmt.num(labor)});
        costModel.addRow(new Object[]{"Materials used", Fmt.num(material)});
        costModel.addRow(new Object[]{"Equipment rental", Fmt.num(equipment)});
        costModel.addRow(new Object[]{"TOTAL", Fmt.num(total)});
        costModel.addRow(new Object[]{"Budget", Fmt.num(p.getBudget())});
        costModel.addRow(new Object[]{remaining >= 0 ? "Remaining" : "OVER BUDGET BY", Fmt.num(Math.abs(remaining))});
        costModel.addRow(new Object[]{"Unused stock (possible loss)", Fmt.num(p.getUnusedStockValue())});
        chart.setLogs(logs);

        taskModel.setRowCount(0);
        for (Task t : p.getTasks()) {
            String status = t.isComplete() ? "Done" : t.getActualStart() != null ? "In progress"
                    : t.isReady() ? "Ready" : "Waiting for prerequisites";
            taskModel.addRow(new Object[]{t.getName(), t.getProgressPercent(),
                    t.getActualStart() == null ? "-" : t.getActualStart(),
                    t.getActualEnd() == null ? "-" : t.getActualEnd(), status});
        }

        logModel.setRowCount(0);
        for (DailyLog l : logs) {
            logModel.addRow(new Object[]{l.getDate(), Fmt.pct(l.getOverallProgress()), l.getWorkersPresent(),
                    Fmt.num(l.getLaborCost()), Fmt.num(l.getMaterialCost()), Fmt.num(l.getEquipmentCost()),
                    String.join("; ", l.getEvents())});
        }

        stockModel.setRowCount(0);
        for (Material m : p.getMaterials()) {
            stockModel.addRow(new Object[]{m.getName(), Fmt.num1(m.getStock()), m.getUnit(),
                    Fmt.money(m.getUnitCost()), Fmt.money(m.stockValue())});
        }
        shipModel.setRowCount(0);
        for (Shipment s : p.getShipments()) {
            shipModel.addRow(new Object[]{s.getMaterial().getName(), Fmt.num1(s.getQuantity()) + " " + s.getMaterial().getUnit(),
                    s.getExpectedDate(), s.isDelivered() ? "delivered" : (s.wasDelayed() ? "pending (delayed)" : "pending")});
        }

        workerModel.setRowCount(0);
        double payroll = 0;
        for (Worker w : p.getWorkers()) {
            payroll += w.getDailyWage();
            workerModel.addRow(new Object[]{w.getName(), Fmt.money(w.getDailyWage())});
        }
        StringBuilder sb = new StringBuilder("<html>Full daily payroll if everyone works: <b>" + Fmt.money(payroll) + "</b>");
        if (!logs.isEmpty()) {
            int present = 0;
            for (DailyLog l : logs) {
                present += l.getWorkersPresent();
            }
            sb.append("<br>Average workers on site per day: <b>").append(Fmt.num1((double) present / logs.size()))
                    .append("</b> over ").append(logs.size()).append(" working day(s)")
                    .append("<br>Total salary paid so far: <b>").append(Fmt.money(labor)).append("</b>");
        }
        workforceStats.setText(sb.append("</html>").toString());
    }

    /** Line chart of overall progress (%) after each simulated day. */
    private static class ProgressChart extends JPanel {
        private List<DailyLog> logs = new ArrayList<>();

        ProgressChart() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(0x999999)));
        }

        void setLogs(List<DailyLog> logs) {
            this.logs = new ArrayList<>(logs);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = 46;
            int right = getWidth() - 18;
            int top = 16;
            int bottom = getHeight() - 36;
            int w = right - left;
            int h = bottom - top;
            g.setFont(getFont().deriveFont(11f));

            for (int pct = 0; pct <= 100; pct += 25) {
                int y = bottom - pct * h / 100;
                g.setColor(new Color(0xE3E3E3));
                g.drawLine(left, y, right, y);
                g.setColor(new Color(0x444444));
                g.drawString(pct + "%", left - 34, y + 4);
            }
            g.setColor(new Color(0x666666));
            g.drawLine(left, top, left, bottom);
            g.drawLine(left, bottom, right, bottom);

            if (logs.isEmpty()) {
                g.setColor(new Color(0x666666));
                g.drawString("Run the simulation to see the chart.", left + 20, top + h / 2);
                g.dispose();
                return;
            }
            int n = logs.size();
            int[] xs = new int[n];
            int[] ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = n == 1 ? left : left + i * w / (n - 1);
                ys[i] = bottom - (int) Math.round(logs.get(i).getOverallProgress() * h / 100.0);
            }
            g.setColor(new Color(0x3D5A80));
            g.setStroke(new BasicStroke(2f));
            g.drawPolyline(xs, ys, n);
            if (n <= 40) {
                for (int i = 0; i < n; i++) {
                    g.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
                }
            }
            g.setColor(new Color(0x444444));
            g.drawString(logs.get(0).getDate().toString(), left, bottom + 16);
            String last = logs.get(n - 1).getDate().toString();
            g.drawString(last, right - g.getFontMetrics().stringWidth(last), bottom + 16);
            String caption = "Working days";
            g.drawString(caption, left + (w - g.getFontMetrics().stringWidth(caption)) / 2, bottom + 30);
            g.dispose();
        }
    }
}
