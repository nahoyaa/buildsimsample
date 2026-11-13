package buildsim;

public class InsufficientStockException extends Exception {
    private static final long serialVersionUID = 1L;

    public InsufficientStockException(String material, double needed, double available) {
        super(String.format("Not enough %s: needed %.1f, available %.1f", material, needed, available));
    }
}
