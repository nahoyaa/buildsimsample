package buildsim;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Menu-driven console version of BuildSim. This class only handles menus, input, and printing;
 * all the rules live in SimulationEngine and the model classes.
 */
public class ConsoleApp {
    private static final int MAX_DAYS = 1000;

    private final Input in = new Input();
    private Project project;
    private SimulationEngine engine;   // null until the simulation is started

    public static void main(String[] args) {
        try {
            new ConsoleApp().run();
        } catch (IllegalStateException e) {
            System.out.println("\nInput closed. Goodbye!");
        }
    }

    // =====================================================================
    // MAIN MENU
    // =====================================================================

    private void run() {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = in.readInt("Choose: ", 0, 6);
            switch (choice) {
                case 1 -> createProject();
                case 2 -> loadSample();
                case 3 -> resourcesMenu();
                case 4 -> tasksMenu();
                case 5 -> simulationMenu();
                case 6 -> reportsMenu();
                default -> running = false;
            }
        }
        System.out.println("Goodbye!");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("==============================================");
        System.out.println("   BUILDSIM - Construction Project Simulator");
        System.out.println("==============================================");
        System.out.println(" " + statusLine());
        System.out.println("----------------------------------------------");
        System.out.println(" 1. Create new project");
        System.out.println(" 2. Load sample project (two-storey house)");
        System.out.println(" 3. Resources (workers, machines, materials)");
        System.out.println(" 4. Tasks");
        System.out.println(" 5. Simulation (run the days)");
        System.out.println(" 6. Reports");
        System.out.println(" 0. Exit");
        System.out.println("----------------------------------------------");
    }

    private String statusLine() {
        if (project == null) {
            return "No project loaded yet.";
        }
        if (engine == null) {
            return "Project: " + project.getName() + " | Status: setup";
        }
        String state = project.isComplete() ? "FINISHED" : "running";
        return String.format(Locale.US, "Project: %s | %s | %.1f%% complete",
                project.getName(), state, project.getOverallProgressPercent());
    }

    // =====================================================================
    // PROJECT
    // =====================================================================

    private void createProject() {
        if (project != null && !in.readYesNo("This replaces the current project. Continue? (y/n): ")) {
            return;
        }
        System.out.println("\n--- New project ---");
        String name = in.readText("Project name: ");
        LocalDate start = in.readDate("Start date (yyyy-mm-dd): ");
        LocalDate end = in.readDate("Target end date (yyyy-mm-dd): ");
        double budget = in.readDouble("Budget in PHP: ", 0);
        try {
            project = new Project(name, start, end, budget);
            engine = null;
            System.out.println("Project created. Next: add resources (3) and tasks (4).");
        } catch (InvalidDateException | IllegalArgumentException e) {
            System.out.println("  Could not create project: " + e.getMessage());
        }
    }

    private void loadSample() {
        if (project != null && !in.readYesNo("This replaces the current project. Continue? (y/n): ")) {
            return;
        }
        try {
            project = Main.buildSampleProject();
            engine = null;
            System.out.println("Sample project loaded: " + project.getName());
            System.out.println("Go to Simulation (5) to run it, or Reports (6) to look around.");
        } catch (CyclicDependencyException e) {
            System.out.println("  Sample project error: " + e.getMessage());
        }
    }

    private boolean requireProject() {
        if (project == null) {
            System.out.println("  Create or load a project first (option 1 or 2).");
            return false;
        }
        return true;
    }

    private boolean requireSetupMode() {
        if (!requireProject()) {
            return false;
        }
        if (engine != null) {
            System.out.println("  The simulation has started, so setup is locked.");
            System.out.println("  Create a new project (1) or reload the sample (2) to start over.");
            return false;
        }
        return true;
    }

    // =====================================================================
    // RESOURCES MENU
    // =====================================================================

    private void resourcesMenu() {
        if (!requireProject()) {
            return;
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Resources ---");
            System.out.println(" 1. Add worker");
            System.out.println(" 2. Add machine");
            System.out.println(" 3. Add material");
            System.out.println(" 4. Add shipment (material order)");
            System.out.println(" 5. View all resources");
            System.out.println(" 0. Back");
            int choice = in.readInt("Choose: ", 0, 5);
            switch (choice) {
                case 1 -> addWorker();
                case 2 -> addMachine();
                case 3 -> addMaterial();
                case 4 -> addShipment();
                case 5 -> viewResources();
                default -> back = true;
            }
        }
    }

    private void addWorker() {
        if (!requireSetupMode()) {
            return;
        }
        String name = in.readText("Worker name: ");
        double wage = in.readDouble("Daily wage in PHP: ", 0);
        try {
            project.addWorker(new Worker(name, wage));
            System.out.println("  Added worker " + name + ".");
        } catch (IllegalArgumentException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    private void addMachine() {
        if (!requireSetupMode()) {
            return;
        }
        String name = in.readText("Machine name: ");
        double rate = in.readDouble("Daily rental rate in PHP: ", 0);
        try {
            project.addMachine(new Machine(name, rate));
            System.out.println("  Added machine " + name + ".");
        } catch (IllegalArgumentException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    private void addMaterial() {
        if (!requireSetupMode()) {
            return;
        }
        String name = in.readText("Material name: ");
        String unit = in.readText("Unit (for example bags, pcs, m): ");
        double cost = in.readDouble("Cost per unit in PHP: ", 0);
        try {
            project.addMaterial(new Material(name, unit, cost));
            System.out.println("  Added material " + name + ". Add a shipment so it has stock.");
        } catch (IllegalArgumentException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    private void addShipment() {
        if (!requireSetupMode()) {
            return;
        }
        Material material = in.choose("Which material is being delivered?", project.getMaterials());
        if (material == null) {
            return;
        }
        double quantity = in.readDouble("Quantity (" + material.getUnit() + "): ", 0.01);
        LocalDate date = in.readDate("Expected arrival date (yyyy-mm-dd): ");
        if (date.isBefore(project.getStartDate())) {
            System.out.println("  Arrival date is before the project start. Shipment not added.");
            return;
        }
        project.addShipment(new Shipment(material, quantity, date));
        System.out.println("  Shipment added.");
    }

    private void viewResources() {
        System.out.println("\nWORKERS");
        if (project.getWorkers().isEmpty()) {
            System.out.println("  (none)");
        }
        for (Worker w : project.getWorkers()) {
            System.out.println(String.format(Locale.US, "  %-20s %s per day", w.getName(), money(w.getDailyWage())));
        }
        System.out.println("MACHINES");
        if (project.getMachines().isEmpty()) {
            System.out.println("  (none)");
        }
        for (Machine m : project.getMachines()) {
            System.out.println(String.format(Locale.US, "  %-20s %s per day%s", m.getName(), money(m.getDailyRate()),
                    m.isBroken() ? "  [BROKEN]" : ""));
        }
        System.out.println("MATERIALS");
        if (project.getMaterials().isEmpty()) {
            System.out.println("  (none)");
        }
        for (Material m : project.getMaterials()) {
            System.out.println(String.format(Locale.US, "  %-20s %s per %s | stock %,.1f", m.getName(),
                    money(m.getUnitCost()), m.getUnit(), m.getStock()));
        }
        System.out.println("SHIPMENTS");
        if (project.getShipments().isEmpty()) {
            System.out.println("  (none)");
        }
        for (Shipment s : project.getShipments()) {
            System.out.println(String.format(Locale.US, "  %-20s %,.1f %s | %s | %s", s.getMaterial().getName(),
                    s.getQuantity(), s.getMaterial().getUnit(), s.getExpectedDate(),
                    s.isDelivered() ? "delivered" : "pending"));
        }
    }

    // =====================================================================
    // TASKS MENU
    // =====================================================================

    private void tasksMenu() {
        if (!requireProject()) {
            return;
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Tasks ---");
            System.out.println(" 1. Add task");
            System.out.println(" 2. Assign worker to task");
            System.out.println(" 3. Set machine needed by task");
            System.out.println(" 4. Set material needed by task");
            System.out.println(" 5. Add prerequisite (task that must finish first)");
            System.out.println(" 6. View tasks");
            System.out.println(" 0. Back");
            int choice = in.readInt("Choose: ", 0, 6);
            switch (choice) {
                case 1 -> addTask();
                case 2 -> assignWorker();
                case 3 -> setMachine();
                case 4 -> setMaterial();
                case 5 -> addPrerequisite();
                case 6 -> viewTasks();
                default -> back = true;
            }
        }
    }

    private void addTask() {
        if (!requireSetupMode()) {
            return;
        }
        String name = in.readText("Task name: ");
        double work = in.readDouble("Total work in worker-days (for example 40): ", 0.01);
        boolean outdoor = in.readYesNo("Is it outdoor work (stopped by rain)? (y/n): ");
        try {
            project.addTask(new Task(name, work, outdoor));
            System.out.println("  Added task " + name + ".");
        } catch (IllegalArgumentException e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }

    private void assignWorker() {
        if (!requireSetupMode()) {
            return;
        }
        Task task = in.choose("Assign a worker to which task?", project.getTasks());
        if (task == null) {
            return;
        }
        Worker worker = in.choose("Which worker?", project.getWorkers());
        if (worker == null) {
            return;
        }
        if (task.getAssignedWorkers().contains(worker)) {
            System.out.println("  " + worker.getName() + " is already assigned to " + task.getName() + ".");
            return;
        }
        task.assignWorker(worker);
        System.out.println("  Assigned " + worker.getName() + " to " + task.getName() + ".");
    }

    private void setMachine() {
        if (!requireSetupMode()) {
            return;
        }
        Task task = in.choose("Which task needs a machine?", project.getTasks());
        if (task == null) {
            return;
        }
        Machine machine = in.choose("Which machine?", project.getMachines());
        if (machine == null) {
            return;
        }
        task.setRequiredMachine(machine);
        System.out.println("  " + task.getName() + " now needs " + machine.getName() + ".");
    }

    private void setMaterial() {
        if (!requireSetupMode()) {
            return;
        }
        Task task = in.choose("Which task needs a material?", project.getTasks());
        if (task == null) {
            return;
        }
        Material material = in.choose("Which material?", project.getMaterials());
        if (material == null) {
            return;
        }
        double perUnit = in.readDouble("Units of " + material.getName() + " used per worker-day of work: ", 0.01);
        task.setRequiredMaterial(material, perUnit);
        System.out.println("  " + task.getName() + " now uses " + material.getName() + ".");
    }

    private void addPrerequisite() {
        if (!requireSetupMode()) {
            return;
        }
        Task task = in.choose("Which task depends on another?", project.getTasks());
        if (task == null) {
            return;
        }
        List<Task> options = new ArrayList<>();
        for (Task t : project.getTasks()) {
            if (t != task && !task.getPrerequisites().contains(t)) {
                options.add(t);
            }
        }
        Task prerequisite = in.choose("Which task must finish before '" + task.getName() + "' can start?", options);
        if (prerequisite == null) {
            return;
        }
        try {
            task.addPrerequisite(prerequisite);
            System.out.println("  " + task.getName() + " will start only after " + prerequisite.getName() + ".");
        } catch (CyclicDependencyException e) {
            System.out.println("  Rejected: " + e.getMessage());
        }
    }

    private void viewTasks() {
        if (project.getTasks().isEmpty()) {
            System.out.println("  No tasks yet.");
            return;
        }
        System.out.println();
        System.out.println(String.format("%-3s %-16s %6s %-8s %-8s %-14s %-16s %s",
                "#", "Task", "Work", "Where", "Workers", "Machine", "Material", "Needs first"));
        int i = 1;
        for (Task t : project.getTasks()) {
            String machine = t.getRequiredMachine() == null ? "-" : t.getRequiredMachine().getName();
            String material = t.getRequiredMaterial() == null ? "-" : t.getRequiredMaterial().getName();
            String prereq = t.getPrerequisites().isEmpty() ? "-" : t.getPrerequisites().toString();
            System.out.println(String.format(Locale.US, "%-3d %-16s %6.0f %-8s %-8d %-14s %-16s %s",
                    i++, t.getName(), t.getTotalWork(), t.isOutdoor() ? "outdoor" : "indoor",
                    t.getAssignedWorkers().size(), machine, material, prereq));
        }
    }

    // =====================================================================
    // SIMULATION MENU
    // =====================================================================

    private void simulationMenu() {
        if (!requireProject()) {
            return;
        }
        if (engine == null && !startSimulation()) {
            return;
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Simulation | next working day: " + engine.getNextWorkingDate() + " ---");
            System.out.println(" 1. Next working day");
            System.out.println(" 2. Run several days");
            System.out.println(" 3. Run to the end");
            System.out.println(" 4. Add a problem (rain, absence, breakdown, late shipment)");
            System.out.println(" 5. Show dashboard");
            System.out.println(" 0. Back");
            int choice = in.readInt("Choose: ", 0, 5);
            switch (choice) {
                case 1 -> runOneDay();
                case 2 -> runSeveralDays();
                case 3 -> runToEnd();
                case 4 -> addManualProblem();
                case 5 -> showDashboard();
                default -> back = true;
            }
        }
    }

    private boolean startSimulation() {
        List<String> issues = new ArrayList<>();
        if (project.getTasks().isEmpty()) {
            issues.add("There are no tasks yet.");
        }
        for (Task t : project.getTasks()) {
            if (t.getAssignedWorkers().isEmpty()) {
                issues.add("Task '" + t.getName() + "' has no workers assigned.");
            }
        }
        if (!issues.isEmpty()) {
            System.out.println("  Cannot start the simulation yet:");
            for (String issue : issues) {
                System.out.println("   - " + issue);
            }
            return false;
        }

        System.out.println("\nSetup will be locked once the simulation starts.");
        boolean random = in.readYesNo("Generate random problems automatically? (y/n): ");
        ProblemGenerator generator = null;
        if (random) {
            int seed = in.readInt("Random seed, same number gives the same result (0-999999): ", 0, 999_999);
            generator = new ProblemGenerator(seed);
        }
        engine = new SimulationEngine(project, generator);
        System.out.println("Simulation started on " + project.getStartDate() + ".");
        return true;
    }

    private boolean projectOpen() {
        if (project.isComplete()) {
            System.out.println("  The project is already finished. See the reports for the results.");
            return false;
        }
        return true;
    }

    private void runOneDay() {
        if (!projectOpen()) {
            return;
        }
        System.out.println();
        System.out.println(engine.nextDay());
        announceIfFinished();
    }

    private void runSeveralDays() {
        if (!projectOpen()) {
            return;
        }
        int days = in.readInt("How many working days (1-365)? ", 1, 365);
        System.out.println();
        for (int i = 0; i < days && !project.isComplete(); i++) {
            System.out.println(engine.nextDay());
        }
        announceIfFinished();
    }

    private void runToEnd() {
        if (!projectOpen()) {
            return;
        }
        boolean showDays = in.readYesNo("Show every day? (y/n): ");
        int before = engine.getLogs().size();
        engine.runToEnd(MAX_DAYS);
        if (showDays) {
            System.out.println();
            List<DailyLog> logs = engine.getLogs();
            for (int i = before; i < logs.size(); i++) {
                System.out.println(logs.get(i));
            }
        }
        if (!announceIfFinished()) {
            System.out.println("\nStopped after " + MAX_DAYS + " working days and the project is still not finished.");
            System.out.println("Check that every task has workers and that needed materials have shipments.");
        }
    }

    private boolean announceIfFinished() {
        if (!project.isComplete()) {
            return false;
        }
        LocalDate finished = engine.getLastProcessedDate();
        long late = ChronoUnit.DAYS.between(project.getTargetEndDate(), finished);
        System.out.println("\n*** PROJECT FINISHED on " + finished + " (target " + project.getTargetEndDate() + ") ***");
        if (late > 0) {
            System.out.println("*** " + late + " calendar day(s) late ***");
        } else {
            System.out.println("*** On time or early ***");
        }
        System.out.println("See Reports (6) for costs and the daily log.");
        return true;
    }

    private void addManualProblem() {
        if (!projectOpen()) {
            return;
        }
        System.out.println("\nWhat happened?");
        System.out.println(" 1. Heavy rain");
        System.out.println(" 2. Worker absent");
        System.out.println(" 3. Machinery breakdown");
        System.out.println(" 4. Shipment delayed");
        System.out.println(" 0. Cancel");
        int type = in.readInt("Choose: ", 0, 4);
        Problem problem = null;

        switch (type) {
            case 1 -> problem = new WeatherProblem();
            case 2 -> {
                Worker w = in.choose("Which worker is absent?", project.getWorkers());
                if (w != null) {
                    problem = new WorkerAbsence(w);
                }
            }
            case 3 -> {
                Machine m = in.choose("Which machine breaks down?", project.getMachines());
                if (m != null) {
                    int days = in.readInt("For how many days (1-10)? ", 1, 10);
                    problem = new MachineryBreakdown(m, days);
                }
            }
            case 4 -> {
                List<Shipment> pending = new ArrayList<>();
                for (Shipment s : project.getShipments()) {
                    if (!s.isDelivered()) {
                        pending.add(s);
                    }
                }
                Shipment s = in.choose("Which shipment is delayed?", pending);
                if (s != null) {
                    int days = in.readInt("Late by how many days (1-30)? ", 1, 30);
                    problem = new ShipmentDelay(s, days);
                }
            }
            default -> { }
        }
        if (problem == null) {
            return;
        }

        LocalDate next = engine.getNextWorkingDate();
        LocalDate date = in.readDateOrBlank("Date it happens (yyyy-mm-dd), or Enter for " + next + ": ");
        if (date == null) {
            date = next;
        }
        if (date.isBefore(next)) {
            System.out.println("  That day has already been processed. Problem not added.");
            return;
        }
        engine.logProblem(date, problem);
        System.out.println("  Recorded for " + date + ": " + problem.getDescription());
    }

    // =====================================================================
    // REPORTS MENU
    // =====================================================================

    private void reportsMenu() {
        if (!requireProject()) {
            return;
        }
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Reports ---");
            System.out.println(" 1. Dashboard");
            System.out.println(" 2. Task progress");
            System.out.println(" 3. Daily log");
            System.out.println(" 4. Cost summary");
            System.out.println(" 5. Material stock");
            System.out.println(" 6. Workforce");
            System.out.println(" 0. Back");
            int choice = in.readInt("Choose: ", 0, 6);
            switch (choice) {
                case 1 -> showDashboard();
                case 2 -> showTaskProgress();
                case 3 -> showDailyLog();
                case 4 -> showCosts();
                case 5 -> showMaterialStock();
                case 6 -> showWorkforce();
                default -> back = true;
            }
        }
    }

    private void showDashboard() {
        System.out.println();
        System.out.println("================== DASHBOARD ==================");
        System.out.println("Project        : " + project.getName());
        System.out.println("Timeframe      : " + project.getStartDate() + " to " + project.getTargetEndDate());
        if (engine == null || engine.getLogs().isEmpty()) {
            System.out.println("Status         : simulation not started");
            System.out.printf(Locale.US, "Setup          : %d task(s), %d worker(s), %d machine(s), %d material(s)%n",
                    project.getTasks().size(), project.getWorkers().size(),
                    project.getMachines().size(), project.getMaterials().size());
            System.out.println("Budget         : " + money(project.getBudget()));
            return;
        }
        DailyLog last = engine.getLogs().get(engine.getLogs().size() - 1);
        double pct = project.getOverallProgressPercent();
        System.out.println("Current date   : " + last.getDate() + (project.isComplete() ? "  (FINISHED)" : ""));
        System.out.printf(Locale.US, "Progress       : %5.1f%%  %s%n", pct, progressBar(pct));

        LocalDate projected = engine.projectedEndDate();
        long diff = ChronoUnit.DAYS.between(project.getTargetEndDate(), projected);
        String verdict = diff > 0 ? diff + " day(s) LATE" : "on time";
        System.out.println("Projected end  : " + projected + "  (" + verdict + ")");
        System.out.printf(Locale.US, "Workers today  : %d%n", last.getWorkersPresent());
        System.out.println("Salary today   : " + money(last.getLaborCost()));
        System.out.println("Materials today: " + money(last.getMaterialCost()));
        CostTracker c = engine.getCosts();
        double used = project.getBudget() == 0 ? 0 : c.getTotal() / project.getBudget() * 100;
        System.out.printf(Locale.US, "Total cost     : %s of %s (%.0f%% of budget)%s%n", money(c.getTotal()),
                money(project.getBudget()), used, c.isOverBudget(project.getBudget()) ? "  OVER BUDGET" : "");
        System.out.println("Unused stock   : " + money(project.getUnusedStockValue()));
        if (!last.getEvents().isEmpty()) {
            System.out.println("Latest events  :");
            for (String e : last.getEvents()) {
                System.out.println("   - " + e);
            }
        }
    }

    private void showTaskProgress() {
        if (project.getTasks().isEmpty()) {
            System.out.println("  No tasks yet.");
            return;
        }
        System.out.println();
        System.out.println(String.format("%-16s %8s  %-22s %-12s %-12s %s",
                "Task", "Progress", "", "Started", "Finished", "Status"));
        for (Task t : project.getTasks()) {
            String status = t.isComplete() ? "Done"
                    : t.getActualStart() != null ? "In progress"
                    : t.isReady() ? "Ready" : "Waiting for prerequisites";
            System.out.println(String.format(Locale.US, "%-16s %7.1f%%  %-22s %-12s %-12s %s",
                    t.getName(), t.getProgressPercent(), progressBar(t.getProgressPercent()),
                    t.getActualStart() == null ? "-" : t.getActualStart().toString(),
                    t.getActualEnd() == null ? "-" : t.getActualEnd().toString(), status));
        }
    }

    private void showDailyLog() {
        if (engine == null || engine.getLogs().isEmpty()) {
            System.out.println("  No days have been simulated yet.");
            return;
        }
        List<DailyLog> logs = engine.getLogs();
        int count = in.readInt("How many of the latest days to show (0 = all)? ", 0, 10_000);
        int from = (count == 0) ? 0 : Math.max(0, logs.size() - count);
        System.out.println();
        for (int i = from; i < logs.size(); i++) {
            System.out.println(logs.get(i));
        }
    }

    private void showCosts() {
        if (engine == null || engine.getLogs().isEmpty()) {
            System.out.println("  No costs yet: the simulation has not run.");
            return;
        }
        CostTracker c = engine.getCosts();
        double remaining = project.getBudget() - c.getTotal();
        System.out.println();
        System.out.println("=============== COST SUMMARY ===============");
        System.out.println("Labor (salaries) : " + money(c.getLabor()));
        System.out.println("Materials used   : " + money(c.getMaterial()));
        System.out.println("Equipment rental : " + money(c.getEquipment()));
        System.out.println("--------------------------------------------");
        System.out.println("TOTAL            : " + money(c.getTotal()));
        System.out.println("Budget           : " + money(project.getBudget()));
        System.out.println((remaining >= 0 ? "Remaining        : " : "OVER BUDGET BY   : ") + money(Math.abs(remaining)));
        System.out.println("Unused stock     : " + money(project.getUnusedStockValue()) + "  (possible loss if never used)");
    }

    private void showMaterialStock() {
        if (project.getMaterials().isEmpty()) {
            System.out.println("  No materials yet.");
            return;
        }
        System.out.println();
        System.out.println(String.format("%-20s %12s %-8s %14s", "Material", "In stock", "Unit", "Stock value"));
        for (Material m : project.getMaterials()) {
            System.out.println(String.format(Locale.US, "%-20s %,12.1f %-8s %14s",
                    m.getName(), m.getStock(), m.getUnit(), money(m.stockValue())));
        }
        System.out.println("\nPending shipments:");
        boolean any = false;
        for (Shipment s : project.getShipments()) {
            if (!s.isDelivered()) {
                any = true;
                System.out.println(String.format(Locale.US, "  %-20s %,.1f %s expected %s%s", s.getMaterial().getName(),
                        s.getQuantity(), s.getMaterial().getUnit(), s.getExpectedDate(),
                        s.wasDelayed() ? "  (delayed)" : ""));
            }
        }
        if (!any) {
            System.out.println("  (none)");
        }
    }

    private void showWorkforce() {
        if (project.getWorkers().isEmpty()) {
            System.out.println("  No workers yet.");
            return;
        }
        double payroll = 0;
        System.out.println();
        for (Worker w : project.getWorkers()) {
            payroll += w.getDailyWage();
            System.out.println(String.format(Locale.US, "  %-20s %s per day", w.getName(), money(w.getDailyWage())));
        }
        System.out.println("\nFull daily payroll if everyone works: " + money(payroll));
        if (engine != null && !engine.getLogs().isEmpty()) {
            int total = 0;
            for (DailyLog l : engine.getLogs()) {
                total += l.getWorkersPresent();
            }
            System.out.printf(Locale.US, "Average workers on site per day  : %.1f over %d working day(s)%n",
                    (double) total / engine.getLogs().size(), engine.getLogs().size());
            System.out.println("Total salary paid so far          : " + money(engine.getCosts().getLabor()));
        }
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private static String money(double amount) {
        return String.format(Locale.US, "PHP %,.2f", amount);
    }

    private static String progressBar(double percent) {
        int filled = (int) Math.round(percent / 5.0);
        filled = Math.max(0, Math.min(20, filled));
        return "[" + "#".repeat(filled) + "-".repeat(20 - filled) + "]";
    }
}
