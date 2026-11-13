package buildsim;

public class MachineryBreakdown extends Problem {
    private final Machine machine;
    private final int days;

    public MachineryBreakdown(Machine machine, int days) {
        super("Machinery problem: " + machine.getName() + " broken for " + days + " day(s)");
        this.machine = machine;
        this.days = days;
    }

    @Override
    public void apply(Project project) {
        machine.breakDown(days);
    }
}
