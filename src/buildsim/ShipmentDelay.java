package buildsim;

public class ShipmentDelay extends Problem {
    private final Shipment shipment;
    private final int days;

    public ShipmentDelay(Shipment shipment, int days) {
        super("Shipment delayed: " + shipment.getMaterial().getName() + " by " + days + " day(s)");
        this.shipment = shipment;
        this.days = days;
    }

    @Override
    public void apply(Project project) {
        shipment.delay(days);
    }
}
