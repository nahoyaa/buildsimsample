package buildsim;

public class Material extends Resource {
    private final String unit;
    private final double unitCost;
    private double stock;

    public Material(String name, String unit, double unitCost) {
        super(name);
        if (unitCost < 0) {
            throw new IllegalArgumentException("Unit cost cannot be negative");
        }
        this.unit = unit;
        this.unitCost = unitCost;
    }

    public String getUnit() {
        return unit;
    }

    public double getUnitCost() {
        return unitCost;
    }

    public double getStock() {
        return stock;
    }

    public void addStock(double quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        stock += quantity;
    }

    /** Stock can never go negative. */
    public void consume(double quantity) throws InsufficientStockException {
        if (quantity > stock + 1e-9) {
            throw new InsufficientStockException(getName(), quantity, stock);
        }
        stock = Math.max(0, stock - quantity);
    }

    /** Value of the unused stock (potential loss if it is never used). */
    public double stockValue() {
        return stock * unitCost;
    }

    /** amount = quantity of units */
    @Override
    public double calculateCost(double quantity) {
        return unitCost * quantity;
    }
}
