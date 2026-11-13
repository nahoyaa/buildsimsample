package buildsim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Creates random problems for simulation mode. A fixed seed gives the same result every run. */
public class ProblemGenerator {
    private final Random random;
    private double absenceChance = 0.05;        // per worker per day
    private double rainChance = 0.20;           // per day
    private double breakdownChance = 0.03;      // per machine per day
    private double shipmentDelayChance = 0.15;  // per pending shipment per day (max once each)

    public ProblemGenerator(long seed) {
        this.random = new Random(seed);
    }

    public void setAbsenceChance(double v) { absenceChance = v; }
    public void setRainChance(double v) { rainChance = v; }
    public void setBreakdownChance(double v) { breakdownChance = v; }
    public void setShipmentDelayChance(double v) { shipmentDelayChance = v; }

    public List<Problem> generate(Project project, LocalDate today) {
        List<Problem> problems = new ArrayList<>();

        if (random.nextDouble() < rainChance) {
            problems.add(new WeatherProblem());
        }
        for (Worker w : project.getWorkers()) {
            if (random.nextDouble() < absenceChance) {
                problems.add(new WorkerAbsence(w));
            }
        }
        for (Machine m : project.getMachines()) {
            if (!m.isBroken() && random.nextDouble() < breakdownChance) {
                problems.add(new MachineryBreakdown(m, 1 + random.nextInt(3)));
            }
        }
        for (Shipment s : project.getShipments()) {
            boolean pending = !s.isDelivered() && !s.wasDelayed() && s.getExpectedDate().isAfter(today);
            if (pending && random.nextDouble() < shipmentDelayChance) {
                problems.add(new ShipmentDelay(s, 1 + random.nextInt(5)));
            }
        }
        return problems;
    }
}
