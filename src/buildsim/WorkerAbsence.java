package buildsim;

public class WorkerAbsence extends Problem {
    private final Worker worker;

    public WorkerAbsence(Worker worker) {
        super("Worker absent: " + worker.getName());
        this.worker = worker;
    }

    @Override
    public void apply(Project project) {
        worker.setAbsentToday(true);
    }
}
