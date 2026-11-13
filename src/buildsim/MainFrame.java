package buildsim;

import javax.swing.*;
import java.awt.*;

/** The main window: a menu bar and five tabs that all share one AppSession. */
@SuppressWarnings("serial")
public class MainFrame extends JFrame {
    private static final int TAB_PROJECT = 0;
    private static final int TAB_SETUP = 1;
    private static final int TAB_SIMULATION = 2;

    private final AppSession session = new AppSession();
    private final JTabbedPane tabs = new JTabbedPane();
    private final ProjectPanel projectPanel;

    public MainFrame() {
        super("BuildSim - Construction Project Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        projectPanel = new ProjectPanel(session, sample -> tabs.setSelectedIndex(sample ? TAB_SIMULATION : TAB_SETUP));
        tabs.addTab("Project", projectPanel);
        tabs.addTab("Setup", new SetupPanel(session));
        tabs.addTab("Simulation", new SimulationPanel(session));
        tabs.addTab("Gantt", new GanttPanel(session));
        tabs.addTab("Reports", new ReportsPanel(session));
        tabs.setSelectedIndex(TAB_PROJECT);
        add(tabs, BorderLayout.CENTER);

        setJMenuBar(buildMenuBar());
        session.addListener(() -> setTitle(session.hasProject()
                ? "BuildSim - " + session.getProject().getName() : "BuildSim - Construction Project Simulator"));

        setSize(1100, 700);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenu file = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New project");
        JMenuItem sampleItem = new JMenuItem("Load sample project");
        JMenuItem exitItem = new JMenuItem("Exit");
        newItem.addActionListener(e -> tabs.setSelectedIndex(TAB_PROJECT));
        sampleItem.addActionListener(e -> projectPanel.loadSample());
        exitItem.addActionListener(e -> dispose());
        file.add(newItem);
        file.add(sampleItem);
        file.addSeparator();
        file.add(exitItem);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About BuildSim");
        about.addActionListener(e -> Dialogs.info(this,
                "BuildSim simulates a construction project day by day.\n"
                        + "Problems (rain, absences, breakdowns, late shipments) delay the work,\n"
                        + "and the schedule, progress, and costs update automatically."));
        help.add(about);

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(help);
        return bar;
    }
}
