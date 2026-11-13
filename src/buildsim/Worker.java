package buildsim;

public class Worker extends Resource {
    private final double dailyWage;
    private boolean absentToday;

    public Worker(String name, double dailyWage) {
        super(name);
        if (dailyWage < 0) {
            throw new IllegalArgumentException("Wage cannot be negative");
        }
        this.dailyWage = dailyWage;
    }

    public double getDailyWage() {
        return dailyWage;
    }

    public boolean isAbsentToday() {
        return absentToday;
    }

    public void setAbsentToday(boolean absentToday) {
        this.absentToday = absentToday;
    }

    /** amount = number of days worked */
    @Override
    public double calculateCost(double days) {
        return dailyWage * days;
    }
}
