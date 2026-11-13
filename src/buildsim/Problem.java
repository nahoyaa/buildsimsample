package buildsim;

/** Something that goes wrong on site. Each subclass applies its own effect (polymorphism). */
public abstract class Problem {
    private final String description;

    protected Problem(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public abstract void apply(Project project);
}
