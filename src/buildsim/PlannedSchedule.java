package buildsim;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The plan before any problems happen: a task starts the working day after all its prerequisites
 * finish, and lasts ceil(totalWork / assigned workers) working days. Used by the Gantt screen.
 */
final class PlannedSchedule {
    static final class Window {
        final LocalDate start;
        final LocalDate end;

        Window(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }

    private PlannedSchedule() { }

    static Map<Task, Window> compute(Project project) {
        Map<Task, Window> map = new LinkedHashMap<>();
        for (Task t : project.getTasks()) {
            plan(t, project, map);
        }
        return map;
    }

    private static Window plan(Task t, Project project, Map<Task, Window> map) {
        Window done = map.get(t);
        if (done != null) {
            return done;
        }
        LocalDate start = project.getStartDate();
        for (Task pre : t.getPrerequisites()) {
            LocalDate candidate = plan(pre, project, map).end.plusDays(1);
            if (candidate.isAfter(start)) {
                start = candidate;
            }
        }
        start = Workdays.nextWorkday(start);
        int workers = Math.max(1, t.getAssignedWorkers().size());
        int days = Math.max(1, (int) Math.ceil(t.getTotalWork() / workers));
        Window w = new Window(start, Workdays.plus(start, days - 1));
        map.put(t, w);
        return w;
    }
}
