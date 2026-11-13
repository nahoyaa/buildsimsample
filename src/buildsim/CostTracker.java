package buildsim;

public class CostTracker {
    private double labor;
    private double material;
    private double equipment;

    public void addLabor(double amount) { labor += amount; }
    public void addMaterial(double amount) { material += amount; }
    public void addEquipment(double amount) { equipment += amount; }

    public double getLabor() { return labor; }
    public double getMaterial() { return material; }
    public double getEquipment() { return equipment; }
    public double getTotal() { return labor + material + equipment; }

    public boolean isOverBudget(double budget) {
        return getTotal() > budget;
    }
}
