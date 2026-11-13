package buildsim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Project {
    private final String name;
    private final LocalDate startDate;
    private final LocalDate targetEndDate;
    private final double budget;
    private boolean rainToday;

    private final List<Task> tasks = new ArrayList<>();
    private final List<Worker> workers = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();
    private final List<Machine> machines = new ArrayList<>();
    private final List<Shipment> shipments = new ArrayList<>();

    public Project(String name, LocalDate startDate, LocalDate targetEndDate, double budget) {
        if (!targetEndDate.isAfter(startDate)) {
            throw new InvalidDateException("Target end date must be after the start date");
        }
        if (budget < 0) {
            throw new IllegalArgumentException("Budget cannot be negative");
        }
        this.name = name;
        this.startDate = startDate;
        this.targetEndDate = targetEndDate;
        this.budget = budget;
    }

    public void addTask(Task t) { tasks.add(t); }
    public void addWorker(Worker w) { workers.add(w); }
    public void addMaterial(Material m) { materials.add(m); }
    public void addMachine(Machine m) { machines.add(m); }
    public void addShipment(Shipment s) { shipments.add(s); }

    public double getTotalWork() {
        double sum = 0;
        for (Task t : tasks) sum += t.getTotalWork();
        return sum;
    }

    public double getTotalWorkDone() {
        double sum = 0;
        for (Task t : tasks) sum += t.getWorkDone();
        return sum;
    }

    public double getRemainingWork() {
        return getTotalWork() - getTotalWorkDone();
    }

    public double getOverallProgressPercent() {
        double total = getTotalWork();
        return total == 0 ? 0 : getTotalWorkDone() / total * 100.0;
    }

    public boolean isComplete() {
        for (Task t : tasks) {
            if (!t.isComplete()) return false;
        }
        return !tasks.isEmpty();
    }

    public double getUnusedStockValue() {
        double sum = 0;
        for (Material m : materials) sum += m.stockValue();
        return sum;
    }

    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getTargetEndDate() { return targetEndDate; }
    public double getBudget() { return budget; }
    public boolean isRainToday() { return rainToday; }
    public void setRainToday(boolean rainToday) { this.rainToday = rainToday; }
    public List<Task> getTasks() { return Collections.unmodifiableList(tasks); }
    public List<Worker> getWorkers() { return Collections.unmodifiableList(workers); }
    public List<Material> getMaterials() { return Collections.unmodifiableList(materials); }
    public List<Machine> getMachines() { return Collections.unmodifiableList(machines); }
    public List<Shipment> getShipments() { return Collections.unmodifiableList(shipments); }
}
