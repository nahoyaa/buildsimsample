package buildsim;

/** Rain: outdoor tasks cannot progress today. */
public class WeatherProblem extends Problem {
    public WeatherProblem() {
        super("Weather problem: heavy rain");
    }

    @Override
    public void apply(Project project) {
        project.setRainToday(true);
    }
}
