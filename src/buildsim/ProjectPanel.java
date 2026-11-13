package buildsim;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Consumer;

/** Project screen: create a new project or load the sample, and see the current project. */
@SuppressWarnings("serial")
public class ProjectPanel extends JPanel {
    private final AppSession session;
    private final Consumer<Boolean> onReady;   // true when the sample was loaded

    private final JTextField nameField = new JTextField("My Construction Project", 22);
    private final JTextField budgetField = new JTextField("350000", 12);
    private final JTextField startField = new JTextField("2026-11-02", 12);
    private final JTextField endField = new JTextField("2026-12-31", 12);
    private final JButton createButton = new JButton("Create project");
    private final JButton sampleButton = new JButton("Load sample project");
    private final JLabel summary = new JLabel();

    public ProjectPanel(AppSession session, Consumer<Boolean> onReady) {
        this.session = session;
        this.onReady = onReady;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        nameField.setName("projectName");
        budgetField.setName("projectBudget");
        startField.setName("projectStart");
        endField.setName("projectEnd");
        createButton.setName("createProject");
        sampleButton.setName("loadSample");

        JPanel center = new JPanel(new GridLayout(1, 2, 20, 0));
        center.add(buildForm());
        center.add(buildSummary());
        add(center, BorderLayout.CENTER);

        createButton.addActionListener(e -> create());
        sampleButton.addActionListener(e -> loadSample());
        session.addListener(this::refresh);
        refresh();
    }

    private JPanel buildForm() {
        JLabel title = new JLabel("New project");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));

        JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 0));
        row1.add(labeled("Project name", nameField));
        row1.add(labeled("Budget (PHP)", budgetField));
        JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
        row2.add(labeled("Start date (yyyy-mm-dd)", startField));
        row2.add(labeled("Target end date (yyyy-mm-dd)", endField));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.add(createButton);
        buttons.add(sampleButton);

        JPanel form = new JPanel(new GridLayout(0, 1, 0, 14));
        form.add(title);
        form.add(row1);
        form.add(row2);
        form.add(buttons);
        JPanel left = new JPanel(new BorderLayout());
        left.add(form, BorderLayout.NORTH);
        return left;
    }

    private JPanel buildSummary() {
        summary.setVerticalAlignment(SwingConstants.TOP);
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Current project"),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        box.add(summary, BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout());
        right.add(box, BorderLayout.NORTH);
        return right;
    }

    private JPanel labeled(String label, JComponent field) {
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private void create() {
        try {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                Dialogs.error(this, "Please enter a project name.");
                return;
            }
            LocalDate start = LocalDate.parse(startField.getText().trim());
            LocalDate end = LocalDate.parse(endField.getText().trim());
            double budget = Double.parseDouble(budgetField.getText().trim().replace(",", ""));
            if (session.hasProject() && !Dialogs.confirm(this, "This replaces the current project. Continue?")) {
                return;
            }
            session.setProject(new Project(name, start, end, budget));
            onReady.accept(false);
        } catch (DateTimeParseException e) {
            Dialogs.error(this, "Dates must look like 2026-11-02 (yyyy-mm-dd).");
        } catch (NumberFormatException e) {
            Dialogs.error(this, "The budget must be a number.");
        } catch (InvalidDateException | IllegalArgumentException e) {
            Dialogs.error(this, e.getMessage());
        }
    }

    public void loadSample() {
        if (session.hasProject() && !Dialogs.confirm(this, "This replaces the current project. Continue?")) {
            return;
        }
        try {
            session.setProject(Main.buildSampleProject());
            onReady.accept(true);
        } catch (CyclicDependencyException e) {
            Dialogs.error(this, e.getMessage());
        }
    }

    private void refresh() {
        if (!session.hasProject()) {
            summary.setText("<html>No project yet.<br>Create one on the left, or load the sample project.</html>");
            return;
        }
        Project p = session.getProject();
        String status = !session.isSimulationStarted() ? "setup (not started)"
                : p.isComplete() ? "finished" : "simulation running (setup locked)";
        summary.setText("<html><b>" + p.getName() + "</b><br>"
                + p.getStartDate() + " to " + p.getTargetEndDate() + "<br>"
                + "Budget: " + Fmt.money(p.getBudget()) + "<br><br>"
                + p.getTasks().size() + " tasks, " + p.getWorkers().size() + " workers, "
                + p.getMachines().size() + " machines,<br>" + p.getMaterials().size() + " materials, "
                + p.getShipments().size() + " shipments<br><br>"
                + "Status: " + status + "</html>");
    }
}
