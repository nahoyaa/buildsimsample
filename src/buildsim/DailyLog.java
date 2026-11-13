package buildsim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One row of the daily report. */
public class DailyLog {
    private final LocalDate date;
    private final double overallProgress;
    private final int workersPresent;
    private final double laborCost;
    private final double materialCost;
    private final double equipmentCost;
    private final List<String> events;

    public DailyLog(LocalDate date, double overallProgress, int workersPresent,
                    double laborCost, double materialCost, double equipmentCost, List<String> events) {
        this.date = date;
        this.overallProgress = overallProgress;
        this.workersPresent = workersPresent;
        this.laborCost = laborCost;
        this.materialCost = materialCost;
        this.equipmentCost = equipmentCost;
        this.events = new ArrayList<>(events);
    }

    public LocalDate getDate() { return date; }
    public double getOverallProgress() { return overallProgress; }
    public int getWorkersPresent() { return workersPresent; }
    public double getLaborCost() { return laborCost; }
    public double getMaterialCost() { return materialCost; }
    public double getEquipmentCost() { return equipmentCost; }
    public List<String> getEvents() { return events; }

    @Override
    public String toString() {
        String line = String.format(Locale.US,
                "%s | %5.1f%% | workers %2d | salary %,9.0f | materials %,9.0f | equipment %,7.0f",
                date, overallProgress, workersPresent, laborCost, materialCost, equipmentCost);
        StringBuilder sb = new StringBuilder(line);
        for (String e : events) {
            sb.append("\n             - ").append(e);
        }
        return sb.toString();
    }
}
