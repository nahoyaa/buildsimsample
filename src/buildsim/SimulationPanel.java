package buildsim;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The Simulation screen: dashboard strip, progress bar, task table, daily log, and controls.
 * It only displays data and calls SimulationEngine. Keep every rule in the engine, not here.
 */
@SuppressWarnings("serial")
public class SimulationPanel extends JPanel {
    private static final Color GOOD = new Color(0x1B7F3B);
    private static final Color BAD = new Color(0xB00020);
    private static final int MAX_DAYS = 1000;

    private final AppSession session;
    private final CardLayout screens = new CardLayout();
    private SimulationEngine shownEngine;   // used to notice when a new simulation begins
    private int shownLogs;
    private boolean finishedShown;

    private final JLabel dateValue = new JLabel();
    private final JLabel endValue = new JLabel();
    private final JLabel workersValue = new JLabel();
    private final JLabel salaryValue = new JLabel();
    private final JLabel costValue = new JLabel();
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private final DefaultTableModel taskModel = new DefaultTableModel(new String[]{"Task", "Progress", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTextArea logArea = new JTextArea();

    private final JComboBox<String> problemBox = new JComboBox<>(
            new String[]{"Rain", "Worker absence", "Machinery breakdown", "Shipment delay"});
    private final JButton addProblemButton = new JButton("Add problem for next day");
    private final JCheckBox randomBox = new JCheckBox("Random problems", true);
    private final JSpinner seedSpinner = new JSpinner(new SpinnerNumberModel(42, 0, 999_999, 1));
    private final JButton nextButton = new JButton("Next day");
    private final JButton severalButton = new JButton("Run several days");
    private final JButton endButton = new JButton("Run to end");

    public SimulationPanel(AppSession session) {
        this.session = session;
        setLayout(screens);
        add(new EmptyState("Create or load a project first (Project tab)."), "empty");
        add(buildMain(), "main");

        nextButton.setName("nextDay");
        severalButton.setName("runSeveral");
        endButton.setName("runToEnd");
        addProblemButton.setName("addProblem");
        randomBox.setName("randomBox");
        seedSpinner.setName("seedSpinner");
        problemBox.setName("problemBox");

        addProblemButton.addActionListener(e -> addManualProblem());
        nextButton.addActionListener(e -> nextDay());
        severalButton.addActionListener(e -> runSeveralDays());
        endButton.addActionListener(e -> runToEnd());

        session.addListener(this::refresh);
        refresh();
    }

    // ---------- building the screen ----------

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout(8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.add(buildTop(), BorderLayout.NORTH);
        main.add(buildCenter(), BorderLayout.CENTER);
        main.add(buildControls(), BorderLayout.SOUTH);
        return main;
    }

    private JPanel buildTop() {
        JPanel strip = new JPanel(new GridLayout(1, 5, 8, 0));
        strip.add(card("Date", dateValue));
        strip.add(card("Projected end", endValue));
        strip.add(card("Workers today", workersValue));
        strip.add(card("Salary today", salaryValue));
        strip.add(card("Total cost / budget (PHP)", costValue));

        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(100, 22));
        JPanel progressRow = new JPanel(new BorderLayout(8, 0));
        progressRow.add(new JLabel("Overall progress"), BorderLayout.WEST);
        progressRow.add(progressBar, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(strip, BorderLayout.CENTER);
        top.add(progressRow, BorderLayout.SOUTH);
        return top;
    }

    private JPanel card(String caption, JLabel value) {
        JLabel cap = new JLabel(caption);
        cap.setFont(cap.getFont().deriveFont(Font.PLAIN, 12f));
        cap.setForeground(new Color(0x555555));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 14f));
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x999999)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        p.add(cap, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        return p;
    }

    private JSplitPane buildCenter() {
        JTable table = new JTable(taskModel);
        table.setRowHeight(26);
        table.getColumnModel().getColumn(1).setCellRenderer(new ProgressCellRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(table), new JScrollPane(logArea));
        split.setResizeWeight(0.0);
        split.setDividerLocation(340);
        return split;
    }

    private JPanel buildControls() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.add(new JLabel("Problem:"));
        left.add(problemBox);
        left.add(addProblemButton);
        left.add(Box.createHorizontalStrut(10));
        left.add(randomBox);
        left.add(new JLabel("Seed:"));
        left.add(seedSpinner);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(nextButton);
        right.add(severalButton);
        right.add(endButton);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(left, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);
        return bottom;
    }

    // ---------- actions ----------

    /** Starts the engine the first time a day is run (this locks the setup). */
    private boolean ensureStarted() {
        if (session.isSimulationStarted()) {
            return true;
        }
        List<String> issues = session.startIssues();
        if (!issues.isEmpty()) {
            Dialogs.error(this, "Cannot start the simulation yet:\n - " + String.join("\n - ", issues));
            return false;
        }
        ProblemGenerator generator = randomBox.isSelected()
                ? new ProblemGenerator((Integer) seedSpinner.getValue()) : null;
        session.startSimulation(generator);
        return true;
    }

    private boolean canRun() {
        return session.hasProject() && !session.getProject().isComplete() && ensureStarted();
    }

    private void nextDay() {
        if (!canRun()) {
            return;
        }
        session.getEngine().nextDay();
        session.fireChanged();
        checkFinished();
    }

    private void runSeveralDays() {
        if (!canRun()) {
            return;
        }
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(7, 1, 365, 1));
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{"How many working days?", spinner},
                "Run several days", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        int days = (Integer) spinner.getValue();
        Project project = session.getProject();
        for (int i = 0; i < days && !project.isComplete(); i++) {
            session.getEngine().nextDay();
        }
        session.fireChanged();
        checkFinished();
    }

    private void runToEnd() {
        if (!canRun()) {
            return;
        }
        session.getEngine().runToEnd(MAX_DAYS);
        session.fireChanged();
        if (!checkFinished()) {
            Dialogs.error(this, "Stopped after " + MAX_DAYS + " working days and the project is still not finished.\n"
                    + "Check that every task has workers and that needed materials have shipments.");
        }
    }

    private void addManualProblem() {
        if (!canRun()) {
            return;
        }
        Project project = session.getProject();
        SimulationEngine engine = session.getEngine();
        LocalDate date = engine.getNextWorkingDate();
        Problem problem = null;

        switch (problemBox.getSelectedIndex()) {
            case 0 -> problem = new WeatherProblem();
            case 1 -> {
                Worker w = Dialogs.choose(this, "Which worker is absent?", project.getWorkers().toArray(new Worker[0]));
                if (w != null) {
                    problem = new WorkerAbsence(w);
                }
            }
            case 2 -> {
                Machine m = Dialogs.choose(this, "Which machine breaks down?", project.getMachines().toArray(new Machine[0]));
                Integer days = (m == null) ? null : Dialogs.choose(this, "For how many days?", new Integer[]{1, 2, 3, 4, 5});
                if (m != null && days != null) {
                    problem = new MachineryBreakdown(m, days);
                }
            }
            case 3 -> {
                List<Shipment> pending = new ArrayList<>();
                for (Shipment s : project.getShipments()) {
                    if (!s.isDelivered()) {
                        pending.add(s);
                    }
                }
                Shipment s = Dialogs.choose(this, "Which shipment is delayed?", pending.toArray(new Shipment[0]));
                Integer days = (s == null) ? null : Dialogs.choose(this, "Late by how many days?", new Integer[]{1, 2, 3, 4, 5, 7, 10});
                if (s != null && days != null) {
                    problem = new ShipmentDelay(s, days);
                }
            }
            default -> { }
        }

        if (problem != null) {
            engine.logProblem(date, problem);
            logArea.append("Queued for " + date + ": " + problem.getDescription() + "\n");
        }
    }

    /** Shows the finish message once. Returns true if the project is finished. */
    private boolean checkFinished() {
        Project project = session.getProject();
        if (!project.isComplete()) {
            return false;
        }
        if (!finishedShown) {
            finishedShown = true;
            LocalDate finished = session.getEngine().getLastProcessedDate();
            long late = ChronoUnit.DAYS.between(project.getTargetEndDate(), finished);
            String verdict = late > 0 ? late + " calendar day(s) late" : "on time or early";
            Dialogs.info(this, "Project finished on " + finished + "\nTarget end date: "
                    + project.getTargetEndDate() + "\nResult: " + verdict);
        }
        return true;
    }

    // ---------- display ----------

    private void refresh() {
        if (!session.hasProject()) {
            screens.show(this, "empty");
            shownEngine = null;
            return;
        }
        screens.show(this, "main");
        SimulationEngine engine = session.getEngine();
        if (engine != shownEngine) {              // a new project or a new simulation
            shownEngine = engine;
            shownLogs = 0;
            finishedShown = false;
            logArea.setText("");
        }
        if (engine != null) {
            List<DailyLog> logs = engine.getLogs();
            for (int i = shownLogs; i < logs.size(); i++) {
                logArea.append(logs.get(i) + "\n");
            }
            shownLogs = logs.size();
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
        refreshTasks();
        updateDashboard();

        boolean done = session.getProject().isComplete();
        boolean started = session.isSimulationStarted();
        for (JComponent c : new JComponent[]{nextButton, severalButton, endButton, addProblemButton, problemBox}) {
            c.setEnabled(!done);
        }
        randomBox.setEnabled(!started);
        seedSpinner.setEnabled(!started);
    }

    private void refreshTasks() {
        taskModel.setRowCount(0);
        for (Task t : session.getProject().getTasks()) {
            String status = t.isComplete() ? "Done" : t.getActualStart() != null ? "In progress"
                    : t.isReady() ? "Ready" : "Waiting";
            taskModel.addRow(new Object[]{t.getName(), t.getProgressPercent(), status});
        }
    }

    private void updateDashboard() {
        Project project = session.getProject();
        SimulationEngine engine = session.getEngine();
        DailyLog last = (engine == null || engine.getLogs().isEmpty()) ? null : engine.getLogs().get(engine.getLogs().size() - 1);

        dateValue.setText(last == null ? "Not started" : last.getDate().toString());
        if (engine == null) {
            endValue.setText(project.getTargetEndDate() + " (target)");
            endValue.setForeground(Color.DARK_GRAY);
        } else {
            LocalDate projected = engine.projectedEndDate();
            boolean late = projected.isAfter(project.getTargetEndDate());
            endValue.setText(projected + (late ? " (late)" : " (on time)"));
            endValue.setForeground(late ? BAD : GOOD);
        }
        workersValue.setText(String.valueOf(last == null ? 0 : last.getWorkersPresent()));
        salaryValue.setText(Fmt.money(last == null ? 0 : last.getLaborCost()));
        double total = engine == null ? 0 : engine.getCosts().getTotal();
        costValue.setText(Fmt.num(total) + " / " + Fmt.num(project.getBudget()));
        costValue.setForeground(total > project.getBudget() ? BAD : GOOD);

        double pct = project.getOverallProgressPercent();
        progressBar.setValue((int) Math.round(pct));
        progressBar.setString(Fmt.pct(pct));
    }
}
