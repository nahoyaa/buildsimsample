package buildsim;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The heart of BuildSim: processDay() runs one working day (Mon-Sat).
 * Problems come from the user (manual mode) and/or from a ProblemGenerator (simulation mode).
 */
public class SimulationEngine {
    private final Project project;
    private final ProblemGenerator generator;   // null = manual mode only
    private final Map<LocalDate, List<Problem>> manualProblems = new HashMap<>();
    private final CostTracker costs = new CostTracker();
    private final List<DailyLog> logs = new ArrayList<>();
    private LocalDate currentDate;
    private LocalDate lastProcessed;
    private int workingDaysProcessed;

    public SimulationEngine(Project project, ProblemGenerator generator) {
        this.project = project;
        this.generator = generator;
        this.currentDate = project.getStartDate();
        this.lastProcessed = project.getStartDate();
    }

    /** Manual mode: the user logs a problem for a given date. */
    public void logProblem(LocalDate date, Problem problem) {
        if (date.isBefore(project.getStartDate())) {
            throw new InvalidDateException("Problem date is before the project start");
        }
        manualProblems.computeIfAbsent(date, d -> new ArrayList<>()).add(problem);
    }

    /** The date that nextDay() will process (Sundays are skipped). */
    public LocalDate getNextWorkingDate() {
        LocalDate d = currentDate;
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d;
    }

    /** Processes the next working day (Sundays are skipped). */
    public DailyLog nextDay() {
        while (currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            currentDate = currentDate.plusDays(1);
        }
        DailyLog log = processDay(currentDate);
        lastProcessed = currentDate;
        currentDate = currentDate.plusDays(1);
        return log;
    }

    /** Runs until every task is done (maxDays is a safety limit). */
    public void runToEnd(int maxDays) {
        while (!project.isComplete() && logs.size() < maxDays) {
            nextDay();
        }
    }

    private DailyLog processDay(LocalDate today) {
        List<String> events = new ArrayList<>();

        // 1. reset daily state
        project.setRainToday(false);
        for (Worker w : project.getWorkers()) {
            w.setAbsentToday(false);
        }

        // 2. apply problems (random + manual)
        List<Problem> problems = new ArrayList<>();
        if (generator != null) {
            problems.addAll(generator.generate(project, today));
        }
        problems.addAll(manualProblems.getOrDefault(today, new ArrayList<>()));
        for (Problem p : problems) {
            p.apply(project);
            events.add(p.getDescription());
        }

        // 3. deliveries that have arrived
        for (Shipment s : project.getShipments()) {
            if (!s.isDelivered() && !s.getExpectedDate().isAfter(today)) {
                s.deliver();
                events.add("Delivered: " + s.getMaterial().getName());
            }
        }

        // 4. work on every ready task
        Set<Worker> usedToday = new HashSet<>();   // a worker works on one task per day
        int workersPresent = 0;
        double laborToday = 0, materialToday = 0, equipmentToday = 0;

        for (Task t : project.getTasks()) {
            if (t.isComplete() || !t.isReady()) {
                continue;
            }

            List<Worker> present = new ArrayList<>();
            for (Worker w : t.getAssignedWorkers()) {
                if (!w.isAbsentToday() && usedToday.add(w)) {
                    present.add(w);
                }
            }
            workersPresent += present.size();
            for (Worker w : present) {
                laborToday += w.calculateCost(1);   // present workers are paid even if work is halted
            }

            Machine machine = t.getRequiredMachine();
            if (machine != null) {
                equipmentToday += machine.calculateCost(1);
            }

            double factor = 1.0;
            if (t.isOutdoor() && project.isRainToday()) {
                factor = 0;
                events.add(t.getName() + ": halted by rain");
            }
            if (machine != null && machine.isBroken()) {
                factor = 0;
                events.add(t.getName() + ": waiting for " + machine.getName() + " (broken)");
            }

            double work = Math.min(present.size() * factor, t.getRemainingWork());

            Material material = t.getRequiredMaterial();
            if (work > 0 && material != null) {
                double needed = work * t.getMaterialPerWorkUnit();
                try {
                    material.consume(needed);
                    materialToday += material.calculateCost(needed);
                } catch (InsufficientStockException e) {
                    work = 0;
                    events.add(t.getName() + ": waiting for " + material.getName() + " (not enough stock)");
                }
            }

            if (work > 0) {
                t.addWork(work, today);
                if (t.isComplete()) {
                    events.add("Task finished: " + t.getName());
                }
            }
        }

        // 5. end of day
        for (Machine m : project.getMachines()) {
            m.endOfDay();
        }
        costs.addLabor(laborToday);
        costs.addMaterial(materialToday);
        costs.addEquipment(equipmentToday);
        workingDaysProcessed++;

        DailyLog log = new DailyLog(today, project.getOverallProgressPercent(), workersPresent,
                laborToday, materialToday, equipmentToday, events);
        logs.add(log);
        return log;
    }

    /** Simple forecast: remaining work / average daily output so far, counted in working days. */
    public LocalDate projectedEndDate() {
        double done = project.getTotalWorkDone();
        if (done <= 0 || workingDaysProcessed == 0) {
            return project.getTargetEndDate();
        }
        double averagePerDay = done / workingDaysProcessed;
        long daysLeft = (long) Math.ceil(project.getRemainingWork() / averagePerDay);
        LocalDate cursor = lastProcessed;
        while (daysLeft > 0) {
            cursor = cursor.plusDays(1);
            if (cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                daysLeft--;
            }
        }
        return cursor;
    }

    public Project getProject() { return project; }
    public CostTracker getCosts() { return costs; }
    public List<DailyLog> getLogs() { return logs; }
    public LocalDate getLastProcessedDate() { return lastProcessed; }
}
