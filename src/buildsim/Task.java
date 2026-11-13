package buildsim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Task {
    private final String name;
    private final double totalWork;          // in worker-days
    private final boolean outdoor;
    private double workDone;
    private final List<Task> prerequisites = new ArrayList<>();
    private final List<Worker> assignedWorkers = new ArrayList<>();
    private Machine requiredMachine;
    private Material requiredMaterial;
    private double materialPerWorkUnit;
    private LocalDate actualStart;
    private LocalDate actualEnd;

    public Task(String name, double totalWork, boolean outdoor) {
        if (totalWork <= 0) {
            throw new IllegalArgumentException("Total work must be greater than 0");
        }
        this.name = name;
        this.totalWork = totalWork;
        this.outdoor = outdoor;
    }

    public void addPrerequisite(Task other) throws CyclicDependencyException {
        if (other == this || other.dependsOn(this)) {
            throw new CyclicDependencyException(name + " and " + other.name + " depend on each other");
        }
        prerequisites.add(other);
    }

    private boolean dependsOn(Task other) {
        for (Task p : prerequisites) {
            if (p == other || p.dependsOn(other)) {
                return true;
            }
        }
        return false;
    }

    public void assignWorker(Worker worker) {
        assignedWorkers.add(worker);
    }

    public void setRequiredMachine(Machine machine) {
        this.requiredMachine = machine;
    }

    public void setRequiredMaterial(Material material, double perWorkUnit) {
        this.requiredMaterial = material;
        this.materialPerWorkUnit = perWorkUnit;
    }

    /** A task can start only when all prerequisites are finished. */
    public boolean isReady() {
        for (Task p : prerequisites) {
            if (!p.isComplete()) {
                return false;
            }
        }
        return true;
    }

    public boolean isComplete() {
        return workDone >= totalWork - 1e-9;
    }

    public void addWork(double work, LocalDate date) {
        if (work < 0) {
            throw new IllegalArgumentException("Work cannot be negative");
        }
        if (work > 0 && actualStart == null) {
            actualStart = date;
        }
        workDone = Math.min(totalWork, workDone + work);
        if (isComplete() && actualEnd == null) {
            actualEnd = date;
        }
    }

    public double getProgressPercent() {
        return workDone / totalWork * 100.0;
    }

    public double getRemainingWork() {
        return totalWork - workDone;
    }

    public String getName() { return name; }
    public double getTotalWork() { return totalWork; }
    public double getWorkDone() { return workDone; }
    public boolean isOutdoor() { return outdoor; }
    public Machine getRequiredMachine() { return requiredMachine; }
    public Material getRequiredMaterial() { return requiredMaterial; }
    public double getMaterialPerWorkUnit() { return materialPerWorkUnit; }
    public LocalDate getActualStart() { return actualStart; }
    public LocalDate getActualEnd() { return actualEnd; }
    public List<Worker> getAssignedWorkers() { return Collections.unmodifiableList(assignedWorkers); }
    public List<Task> getPrerequisites() { return Collections.unmodifiableList(prerequisites); }

    @Override
    public String toString() {
        return name;
    }
}
