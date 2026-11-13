package buildsim;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the current project and simulation engine and tells every screen when something changed.
 * Screens call fireChanged() after they change the project or run the engine.
 */
public class AppSession {
    private Project project;
    private SimulationEngine engine;
    private final List<Runnable> listeners = new ArrayList<>();

    public Project getProject() { return project; }
    public SimulationEngine getEngine() { return engine; }
    public boolean hasProject() { return project != null; }

    /** Once the engine exists, the setup is locked so costs and progress stay consistent. */
    public boolean isSimulationStarted() { return engine != null; }

    public void setProject(Project project) {
        this.project = project;
        this.engine = null;
        fireChanged();
    }

    public void startSimulation(ProblemGenerator generator) {
        this.engine = new SimulationEngine(project, generator);
        fireChanged();
    }

    /** Things that must be fixed before the simulation can start. */
    public List<String> startIssues() {
        List<String> issues = new ArrayList<>();
        if (project.getTasks().isEmpty()) {
            issues.add("There are no tasks yet.");
        }
        for (Task t : project.getTasks()) {
            if (t.getAssignedWorkers().isEmpty()) {
                issues.add("Task '" + t.getName() + "' has no workers assigned.");
            }
        }
        return issues;
    }

    public void addListener(Runnable listener) { listeners.add(listener); }

    public void fireChanged() {
        for (Runnable r : listeners) {
            r.run();
        }
    }
}
