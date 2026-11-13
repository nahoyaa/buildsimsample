package buildsim;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Setup screen: tasks on the left, resources (workers, machines, materials, shipments) on the right. */
@SuppressWarnings("serial")
public class SetupPanel extends JPanel {
    private final AppSession session;
    private final CardLayout screens = new CardLayout();
    private final List<JComponent> editors = new ArrayList<>();   // disabled when setup is locked
    private final JLabel lockBanner = new JLabel(
            "Setup is locked because the simulation has started. Create a new project (Project tab) to start over.");

    private final DefaultTableModel taskModel = readOnly("Task", "Work", "Where", "Workers", "Machine", "Material", "Needs first");
    private final DefaultTableModel workerModel = readOnly("Worker", "Daily wage");
    private final DefaultTableModel machineModel = readOnly("Machine", "Daily rate");
    private final DefaultTableModel materialModel = readOnly("Material", "Unit", "Cost per unit", "In stock");
    private final DefaultTableModel shipmentModel = readOnly("Material", "Quantity", "Expected", "Status");

    private final JTextField workerName = field("workerName", 10);
    private final JTextField workerWage = field("workerWage", 7);
    private final JTextField machineName = field("machineName", 10);
    private final JTextField machineRate = field("machineRate", 7);
    private final JTextField materialName = field("materialName", 10);
    private final JTextField materialUnit = field("materialUnit", 6);
    private final JTextField materialCost = field("materialCost", 7);
    private final JComboBox<Material> shipmentMaterial = new JComboBox<>();
    private final JTextField shipmentQty = field("shipmentQty", 7);
    private final JTextField shipmentDate = field("shipmentDate", 9);

    public SetupPanel(AppSession session) {
        this.session = session;
        setLayout(screens);
        add(new EmptyState("Create or load a project first (Project tab)."), "empty");
        add(buildMain(), "main");
        session.addListener(this::refresh);
        refresh();
    }

    // ---------- building the screen ----------

    private JPanel buildMain() {
        lockBanner.setForeground(new Color(0xB00020));
        lockBanner.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTasks(), buildResources());
        split.setResizeWeight(0.58);

        JPanel main = new JPanel(new BorderLayout());
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.add(lockBanner, BorderLayout.NORTH);
        main.add(split, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildTasks() {
        JButton add = button("addTask", "Add task");
        JButton assign = button("assignWorker", "Assign worker");
        JButton machine = button("setMachine", "Set machine");
        JButton material = button("setMaterial", "Set material");
        JButton prereq = button("addPrereq", "Add prerequisite");
        add.addActionListener(e -> addTask());
        assign.addActionListener(e -> assignWorker());
        machine.addActionListener(e -> setMachine());
        material.addActionListener(e -> setMaterial());
        prereq.addActionListener(e -> addPrerequisite());

        JPanel buttons = new JPanel(new GridLayout(2, 3, 6, 6));
        for (JButton b : new JButton[]{add, assign, machine, material, prereq}) {
            buttons.add(b);
            editors.add(b);
        }

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        p.add(heading("Tasks"), BorderLayout.NORTH);
        p.add(new JScrollPane(table(taskModel)), BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildResources() {
        CardLayout cards = new CardLayout();
        JPanel stack = new JPanel(cards);

        JButton addWorker = button("addWorker", "Add");
        addWorker.addActionListener(e -> addWorker());
        stack.add(resourceCard(workerModel, form("Name", workerName, "Daily wage", workerWage, addWorker)), "Workers");

        JButton addMachine = button("addMachine", "Add");
        addMachine.addActionListener(e -> addMachine());
        stack.add(resourceCard(machineModel, form("Name", machineName, "Daily rate", machineRate, addMachine)), "Machines");

        JButton addMaterial = button("addMaterial", "Add");
        addMaterial.addActionListener(e -> addMaterial());
        stack.add(resourceCard(materialModel, form("Name", materialName, "Unit", materialUnit, "Cost", materialCost, addMaterial)), "Materials");

        JButton addShipment = button("addShipment", "Add");
        addShipment.addActionListener(e -> addShipment());
        shipmentMaterial.setName("shipmentMaterial");
        stack.add(resourceCard(shipmentModel, form("Material", shipmentMaterial, "Qty", shipmentQty, "Arrives (yyyy-mm-dd)", shipmentDate, addShipment)), "Shipments");

        for (JComponent c : new JComponent[]{workerName, workerWage, addWorker, machineName, machineRate, addMachine,
                materialName, materialUnit, materialCost, addMaterial, shipmentMaterial, shipmentQty, shipmentDate, addShipment}) {
            editors.add(c);
        }

        JPanel toggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ButtonGroup group = new ButtonGroup();
        for (String name : new String[]{"Workers", "Machines", "Materials", "Shipments"}) {
            JToggleButton t = new JToggleButton(name, name.equals("Workers"));
            t.addActionListener(e -> cards.show(stack, name));
            group.add(t);
            toggles.add(t);
        }

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.add(heading("Resources"), BorderLayout.NORTH);
        top.add(toggles, BorderLayout.CENTER);

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        p.add(top, BorderLayout.NORTH);
        p.add(stack, BorderLayout.CENTER);
        return p;
    }

    private JPanel resourceCard(DefaultTableModel model, JPanel form) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.add(new JScrollPane(table(model)), BorderLayout.CENTER);
        p.add(form, BorderLayout.SOUTH);
        return p;
    }

    /** Builds a row of "label field" pairs ending with a button. */
    private JPanel form(Object... parts) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (Object part : parts) {
            if (part instanceof String s) {
                row.add(new JLabel(s));
            } else {
                row.add((Component) part);
            }
        }
        return row;
    }

    private static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setRowHeight(24);
        return t;
    }

    private static DefaultTableModel readOnly(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        return l;
    }

    private static JTextField field(String name, int columns) {
        JTextField f = new JTextField(columns);
        f.setName(name);
        return f;
    }

    private static JButton button(String name, String text) {
        JButton b = new JButton(text);
        b.setName(name);
        return b;
    }

    // ---------- actions: resources ----------

    private void addWorker() {
        Double wage = Dialogs.number(this, workerWage.getText(), "Daily wage", 0);
        if (wage == null) {
            return;
        }
        try {
            session.getProject().addWorker(new Worker(workerName.getText().trim(), wage));
            workerName.setText("");
            workerWage.setText("");
            session.fireChanged();
        } catch (IllegalArgumentException e) {
            Dialogs.error(this, e.getMessage());
        }
    }

    private void addMachine() {
        Double rate = Dialogs.number(this, machineRate.getText(), "Daily rate", 0);
        if (rate == null) {
            return;
        }
        try {
            session.getProject().addMachine(new Machine(machineName.getText().trim(), rate));
            machineName.setText("");
            machineRate.setText("");
            session.fireChanged();
        } catch (IllegalArgumentException e) {
            Dialogs.error(this, e.getMessage());
        }
    }

    private void addMaterial() {
        Double cost = Dialogs.number(this, materialCost.getText(), "Cost per unit", 0);
        if (cost == null) {
            return;
        }
        String unit = materialUnit.getText().trim();
        if (unit.isEmpty()) {
            Dialogs.error(this, "Please enter a unit (for example bags, pcs, m).");
            return;
        }
        try {
            session.getProject().addMaterial(new Material(materialName.getText().trim(), unit, cost));
            materialName.setText("");
            materialUnit.setText("");
            materialCost.setText("");
            session.fireChanged();
        } catch (IllegalArgumentException e) {
            Dialogs.error(this, e.getMessage());
        }
    }

    private void addShipment() {
        Material material = (Material) shipmentMaterial.getSelectedItem();
        if (material == null) {
            Dialogs.error(this, "Add a material first.");
            return;
        }
        Double qty = Dialogs.number(this, shipmentQty.getText(), "Quantity", 0.01);
        if (qty == null) {
            return;
        }
        try {
            LocalDate date = LocalDate.parse(shipmentDate.getText().trim());
            if (date.isBefore(session.getProject().getStartDate())) {
                Dialogs.error(this, "The arrival date is before the project start date.");
                return;
            }
            session.getProject().addShipment(new Shipment(material, qty, date));
            shipmentQty.setText("");
            shipmentDate.setText("");
            session.fireChanged();
        } catch (DateTimeParseException e) {
            Dialogs.error(this, "The date must look like 2026-11-03 (yyyy-mm-dd).");
        }
    }

    // ---------- actions: tasks ----------

    private void addTask() {
        JTextField name = new JTextField(16);
        JTextField work = new JTextField(8);
        JCheckBox outdoor = new JCheckBox("Outdoor work (stopped by rain)", true);
        int ok = JOptionPane.showConfirmDialog(this,
                new Object[]{"Task name", name, "Total work in worker-days (for example 40)", work, outdoor},
                "Add task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        Double amount = Dialogs.number(this, work.getText(), "Total work", 0.01);
        if (amount == null) {
            return;
        }
        if (name.getText().trim().isEmpty()) {
            Dialogs.error(this, "Please enter a task name.");
            return;
        }
        session.getProject().addTask(new Task(name.getText().trim(), amount, outdoor.isSelected()));
        session.fireChanged();
    }

    private Task[] tasks() {
        return session.getProject().getTasks().toArray(new Task[0]);
    }

    private void assignWorker() {
        Task task = Dialogs.choose(this, "Assign a worker to which task?", tasks());
        if (task == null) {
            return;
        }
        Worker worker = Dialogs.choose(this, "Which worker?", session.getProject().getWorkers().toArray(new Worker[0]));
        if (worker == null) {
            return;
        }
        if (task.getAssignedWorkers().contains(worker)) {
            Dialogs.info(this, worker.getName() + " is already assigned to " + task.getName() + ".");
            return;
        }
        task.assignWorker(worker);
        session.fireChanged();
    }

    private void setMachine() {
        Task task = Dialogs.choose(this, "Which task needs a machine?", tasks());
        if (task == null) {
            return;
        }
        Machine machine = Dialogs.choose(this, "Which machine?", session.getProject().getMachines().toArray(new Machine[0]));
        if (machine == null) {
            return;
        }
        task.setRequiredMachine(machine);
        session.fireChanged();
    }

    private void setMaterial() {
        Task task = Dialogs.choose(this, "Which task needs a material?", tasks());
        if (task == null) {
            return;
        }
        Material material = Dialogs.choose(this, "Which material?", session.getProject().getMaterials().toArray(new Material[0]));
        if (material == null) {
            return;
        }
        String answer = JOptionPane.showInputDialog(this,
                "Units of " + material.getName() + " used per worker-day of work:", "1");
        if (answer == null) {
            return;
        }
        Double perUnit = Dialogs.number(this, answer, "Units per worker-day", 0.01);
        if (perUnit == null) {
            return;
        }
        task.setRequiredMaterial(material, perUnit);
        session.fireChanged();
    }

    private void addPrerequisite() {
        Task task = Dialogs.choose(this, "Which task depends on another?", tasks());
        if (task == null) {
            return;
        }
        List<Task> options = new ArrayList<>();
        for (Task t : session.getProject().getTasks()) {
            if (t != task && !task.getPrerequisites().contains(t)) {
                options.add(t);
            }
        }
        Task prerequisite = Dialogs.choose(this,
                "Which task must finish before '" + task.getName() + "' can start?", options.toArray(new Task[0]));
        if (prerequisite == null) {
            return;
        }
        try {
            task.addPrerequisite(prerequisite);
            session.fireChanged();
        } catch (CyclicDependencyException e) {
            Dialogs.error(this, "Rejected: " + e.getMessage());
        }
    }

    // ---------- display ----------

    private void refresh() {
        if (!session.hasProject()) {
            screens.show(this, "empty");
            return;
        }
        screens.show(this, "main");
        Project p = session.getProject();

        taskModel.setRowCount(0);
        for (Task t : p.getTasks()) {
            taskModel.addRow(new Object[]{t.getName(), Fmt.num1(t.getTotalWork()), t.isOutdoor() ? "outdoor" : "indoor",
                    t.getAssignedWorkers().size(),
                    t.getRequiredMachine() == null ? "-" : t.getRequiredMachine().getName(),
                    t.getRequiredMaterial() == null ? "-" : t.getRequiredMaterial().getName(),
                    t.getPrerequisites().isEmpty() ? "-" : t.getPrerequisites().toString().replaceAll("[\\[\\]]", "")});
        }
        workerModel.setRowCount(0);
        for (Worker w : p.getWorkers()) {
            workerModel.addRow(new Object[]{w.getName(), Fmt.money(w.getDailyWage())});
        }
        machineModel.setRowCount(0);
        for (Machine m : p.getMachines()) {
            machineModel.addRow(new Object[]{m.getName(), Fmt.money(m.getDailyRate())});
        }
        materialModel.setRowCount(0);
        for (Material m : p.getMaterials()) {
            materialModel.addRow(new Object[]{m.getName(), m.getUnit(), Fmt.money(m.getUnitCost()), Fmt.num1(m.getStock())});
        }
        shipmentModel.setRowCount(0);
        for (Shipment s : p.getShipments()) {
            shipmentModel.addRow(new Object[]{s.getMaterial().getName(), Fmt.num1(s.getQuantity()) + " " + s.getMaterial().getUnit(),
                    s.getExpectedDate(), s.isDelivered() ? "delivered" : (s.wasDelayed() ? "pending (delayed)" : "pending")});
        }

        Object selected = shipmentMaterial.getSelectedItem();
        shipmentMaterial.setModel(new DefaultComboBoxModel<>(p.getMaterials().toArray(new Material[0])));
        if (selected != null) {
            shipmentMaterial.setSelectedItem(selected);
        }

        boolean locked = session.isSimulationStarted();
        lockBanner.setVisible(locked);
        for (JComponent c : editors) {
            c.setEnabled(!locked);
        }
    }
}
