package buildsim;

import java.time.LocalDate;

/** A material order that arrives on a date. A ShipmentDelay problem pushes the date back. */
public class Shipment {
    private final Material material;
    private final double quantity;
    private LocalDate expectedDate;
    private boolean delivered;
    private boolean delayed;

    public Shipment(Material material, double quantity, LocalDate expectedDate) {
        this.material = material;
        this.quantity = quantity;
        this.expectedDate = expectedDate;
    }

    public Material getMaterial() {
        return material;
    }

    public double getQuantity() {
        return quantity;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public boolean wasDelayed() {
        return delayed;
    }

    public void delay(int days) {
        expectedDate = expectedDate.plusDays(days);
        delayed = true;
    }

    public void deliver() {
        material.addStock(quantity);
        delivered = true;
    }

    @Override
    public String toString() {
        return material.getName() + " (expected " + expectedDate + ")";
    }
}
