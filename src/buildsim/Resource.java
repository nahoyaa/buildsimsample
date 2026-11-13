package buildsim;

/** Base class of everything the project uses. Each subclass computes its own cost (polymorphism). */
public abstract class Resource {
    private final String name;

    protected Resource(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Resource name is required");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Cost of using this resource: amount = days for workers/machines, units for materials. */
    public abstract double calculateCost(double amount);

    @Override
    public String toString() {
        return name;
    }
}
