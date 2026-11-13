package buildsim;

public class Machine extends Resource {
    private final double dailyRate;
    private int brokenDaysLeft;

    public Machine(String name, double dailyRate) {
        super(name);
        if (dailyRate < 0) {
            throw new IllegalArgumentException("Rate cannot be negative");
        }
        this.dailyRate = dailyRate;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public boolean isBroken() {
        return brokenDaysLeft > 0;
    }

    public void breakDown(int days) {
        if (days < 1) {
            throw new IllegalArgumentException("Breakdown must last at least 1 day");
        }
        brokenDaysLeft = Math.max(brokenDaysLeft, days);
    }

    /** Called by the engine at the end of every working day. */
    public void endOfDay() {
        if (brokenDaysLeft > 0) {
            brokenDaysLeft--;
        }
    }

    /** amount = number of days rented */
    @Override
    public double calculateCost(double days) {
        return dailyRate * days;
    }
}
