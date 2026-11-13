package buildsim;

import java.time.LocalDate;

/** Console demo of the engine. Build the GUI on top of SimulationEngine after this works. */
public class Main {
    public static void main(String[] args) throws Exception {
        Project house = buildSampleProject();

        // Simulation mode with a fixed seed (same result every run).
        SimulationEngine engine = new SimulationEngine(house, new ProblemGenerator(42));

        // Manual mode: the user can also log a problem on a date.
        engine.logProblem(LocalDate.of(2026, 11, 9), new WorkerAbsence(house.getWorkers().get(0)));

        engine.runToEnd(300);

        for (DailyLog log : engine.getLogs()) {
            System.out.println(log);
        }

        System.out.println("\n===== SUMMARY =====");
        System.out.println("Target end date : " + house.getTargetEndDate());
        System.out.println("Actual end date : " + engine.getLastProcessedDate());
        System.out.printf("Labor cost      : %,.0f%n", engine.getCosts().getLabor());
        System.out.printf("Material cost   : %,.0f%n", engine.getCosts().getMaterial());
        System.out.printf("Equipment cost  : %,.0f%n", engine.getCosts().getEquipment());
        System.out.printf("TOTAL cost      : %,.0f (budget %,.0f)%n",
                engine.getCosts().getTotal(), house.getBudget());
        System.out.printf("Unused stock    : %,.0f%n", house.getUnusedStockValue());
    }

    static Project buildSampleProject() throws CyclicDependencyException {
        Project p = new Project("Two-Storey House",
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 12, 31), 350_000);

        // Workers (daily wage in pesos)
        for (int i = 1; i <= 8; i++) {
            p.addWorker(new Worker("Worker " + i, 600));
        }
        Machine excavator = new Machine("Excavator", 5000);
        p.addMachine(excavator);

        Material cement = new Material("Cement", "bags", 280);
        Material blocks = new Material("Hollow blocks", "pcs", 14);
        Material sheets = new Material("Roofing sheets", "pcs", 650);
        Material wire = new Material("Electrical wire", "m", 35);
        Material paint = new Material("Paint", "gal", 900);
        for (Material m : new Material[]{cement, blocks, sheets, wire, paint}) {
            p.addMaterial(m);
        }

        // Orders and their expected arrival dates
        p.addShipment(new Shipment(cement, 90, LocalDate.of(2026, 11, 9)));
        p.addShipment(new Shipment(blocks, 950, LocalDate.of(2026, 11, 23)));
        p.addShipment(new Shipment(sheets, 40, LocalDate.of(2026, 12, 7)));
        p.addShipment(new Shipment(wire, 160, LocalDate.of(2026, 12, 1)));
        p.addShipment(new Shipment(paint, 30, LocalDate.of(2026, 12, 14)));

        // Tasks (work in worker-days)
        Task excavation = new Task("Excavation", 24, true);
        Task foundation = new Task("Foundation", 40, true);
        Task walls = new Task("Walls", 60, true);
        Task roofing = new Task("Roofing", 36, true);
        Task electrical = new Task("Electrical", 30, false);
        Task painting = new Task("Painting", 24, false);

        excavation.setRequiredMachine(excavator);
        foundation.setRequiredMaterial(cement, 2);
        walls.setRequiredMaterial(blocks, 15);
        roofing.setRequiredMaterial(sheets, 1);
        electrical.setRequiredMaterial(wire, 5);
        painting.setRequiredMaterial(paint, 1);

        foundation.addPrerequisite(excavation);
        walls.addPrerequisite(foundation);
        roofing.addPrerequisite(walls);
        electrical.addPrerequisite(walls);
        painting.addPrerequisite(roofing);
        painting.addPrerequisite(electrical);

        assign(excavation, p, 0, 4);
        assign(foundation, p, 0, 4);
        assign(walls, p, 0, 6);
        assign(roofing, p, 0, 4);
        assign(electrical, p, 6, 8);
        assign(painting, p, 0, 4);

        for (Task t : new Task[]{excavation, foundation, walls, roofing, electrical, painting}) {
            p.addTask(t);
        }
        return p;
    }

    private static void assign(Task task, Project p, int fromIndex, int toIndexExclusive) {
        for (int i = fromIndex; i < toIndexExclusive; i++) {
            task.assignWorker(p.getWorkers().get(i));
        }
    }
}
