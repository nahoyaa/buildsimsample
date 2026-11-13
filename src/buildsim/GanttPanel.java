package buildsim;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gantt screen: one row per task. Outlined bar = plan, blue = work done, hatched = delay beyond the plan,
 * dashed line = last simulated day. Click a task to see its details.
 */
@SuppressWarnings("serial")
public class GanttPanel extends JPanel {
    private static final int ROW_H = 38;
    private static final int HEADER_H = 46;
    private static final int NAME_W = 150;
    private static final Color BLUE = new Color(0x3D5A80);
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final AppSession session;
    private final CardLayout screens = new CardLayout();
    private final Canvas canvas = new Canvas();
    private final Names names = new Names();
    private final JLabel info = new JLabel("Click a task to see its details.");

    private int pxPerDay = 11;
    private Task selected;
    private Map<Task, PlannedSchedule.Window> plan;
    private LocalDate rangeStart;
    private int totalDays;
    private LocalDate today;   // last simulated day, or null

    public GanttPanel(AppSession session) {
        this.session = session;
        setLayout(screens);
        add(new EmptyState("Create or load a project first (Project tab)."), "empty");
        add(buildMain(), "main");
        session.addListener(this::refresh);
        refresh();
    }

    private JPanel buildMain() {
        JScrollPane scroll = new JScrollPane(canvas);
        scroll.setRowHeaderView(names);
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        zoom.add(new JLabel("Zoom:"));
        ButtonGroup group = new ButtonGroup();
        String[] labels = {"Day", "Week", "Month"};
        int[] widths = {28, 11, 4};
        for (int i = 0; i < labels.length; i++) {
            JToggleButton b = new JToggleButton(labels[i], i == 1);
            b.setName("zoom" + labels[i]);
            int px = widths[i];
            b.addActionListener(e -> {
                pxPerDay = px;
                refresh();
            });
            group.add(b);
            zoom.add(b);
        }

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        legend.add(new Swatch(0));
        legend.add(new JLabel("Planned"));
        legend.add(new Swatch(1));
        legend.add(new JLabel("Work done"));
        legend.add(new Swatch(2));
        legend.add(new JLabel("Delay beyond plan"));
        legend.add(new JLabel("| Dashed line = today"));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.add(zoom, BorderLayout.WEST);
        toolbar.add(legend, BorderLayout.EAST);

        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x999999)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        box.add(info, BorderLayout.CENTER);
        box.setPreferredSize(new Dimension(100, 76));

        JPanel main = new JPanel(new BorderLayout(0, 8));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.add(toolbar, BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);
        main.add(box, BorderLayout.SOUTH);
        return main;
    }

    // ---------- data ----------

    private void refresh() {
        if (!session.hasProject()) {
            screens.show(this, "empty");
            return;
        }
        screens.show(this, "main");
        Project p = session.getProject();
        plan = PlannedSchedule.compute(p);
        SimulationEngine e = session.getEngine();
        today = (e == null || e.getLogs().isEmpty()) ? null : e.getLastProcessedDate();

        rangeStart = p.getStartDate();
        LocalDate end = p.getTargetEndDate();
        for (PlannedSchedule.Window w : plan.values()) {
            if (w.end.isAfter(end)) {
                end = w.end;
            }
        }
        for (Task t : p.getTasks()) {
            if (t.getActualEnd() != null && t.getActualEnd().isAfter(end)) {
                end = t.getActualEnd();
            }
        }
        if (today != null && today.isAfter(end)) {
            end = today;
        }
        totalDays = (int) ChronoUnit.DAYS.between(rangeStart, end) + 8;
        if (selected != null && !p.getTasks().contains(selected)) {
            selected = null;
        }
        updateInfo();
        canvas.revalidate();
        canvas.repaint();
        names.revalidate();
        names.repaint();
    }

    private void select(int row) {
        List<Task> tasks = session.getProject().getTasks();
        if (row >= 0 && row < tasks.size()) {
            selected = tasks.get(row);
            updateInfo();
            canvas.repaint();
            names.repaint();
        }
    }

    private void updateInfo() {
        if (selected == null || plan == null || !plan.containsKey(selected)) {
            info.setText("Click a task to see its details.");
            return;
        }
        Task t = selected;
        PlannedSchedule.Window w = plan.get(t);
        StringBuilder sb = new StringBuilder("<html><b>Selected task: ").append(t.getName()).append("</b><br>");
        sb.append("Planned ").append(w.start).append(" to ").append(w.end)
                .append(" | Progress ").append(Fmt.pct(t.getProgressPercent()))
                .append(" | Workers ").append(t.getAssignedWorkers().size()).append("<br>");
        if (t.isComplete()) {
            long late = ChronoUnit.DAYS.between(w.end, t.getActualEnd());
            sb.append("Started ").append(t.getActualStart()).append(", finished ").append(t.getActualEnd())
                    .append(late > 0 ? " (" + late + " day(s) late)" : late < 0 ? " (" + (-late) + " day(s) early)" : " (on plan)");
        } else if (t.getActualStart() != null) {
            long late = ChronoUnit.DAYS.between(w.start, t.getActualStart());
            sb.append("Started ").append(t.getActualStart())
                    .append(late > 0 ? " (" + late + " day(s) after the plan)" : "").append(", planned to finish ").append(w.end);
        } else if (t.isReady()) {
            sb.append("Not started yet (ready to start)");
        } else {
            sb.append("Waiting for prerequisites: ").append(t.getPrerequisites().toString().replaceAll("[\\[\\]]", ""));
        }
        info.setText(sb.append("</html>").toString());
    }

    private int x(LocalDate d) {
        return (int) (ChronoUnit.DAYS.between(rangeStart, d) * pxPerDay);
    }

    private int canvasHeight() {
        return HEADER_H + (session.hasProject() ? session.getProject().getTasks().size() : 0) * ROW_H + 4;
    }

    // ---------- drawing ----------

    private static void hatch(Graphics2D g, int x, int y, int w, int h) {
        if (w <= 0) {
            return;
        }
        g.setColor(new Color(0xE3E3E3));
        g.fillRect(x, y, w, h);
        Shape old = g.getClip();
        g.clipRect(x, y, w, h);
        g.setColor(new Color(0x888888));
        for (int k = -h; k < w; k += 6) {
            g.drawLine(x + k, y + h, x + k + h, y);
        }
        g.setClip(old);
        g.setColor(new Color(0x333333));
        g.drawRect(x, y, w - 1, h);
    }

    private class Canvas extends JPanel {
        Canvas() {
            setBackground(Color.WHITE);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getY() >= HEADER_H) {
                        select((e.getY() - HEADER_H) / ROW_H);
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(Math.max(100, totalDays * pxPerDay), canvasHeight());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (!session.hasProject() || rangeStart == null || plan == null) {
                return;
            }
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            List<Task> tasks = session.getProject().getTasks();

            // selected row highlight
            for (int i = 0; i < tasks.size(); i++) {
                if (tasks.get(i) == selected) {
                    g.setColor(new Color(0xE8F0FA));
                    g.fillRect(0, HEADER_H + i * ROW_H, w, ROW_H);
                }
            }
            // Sundays and day grid
            for (int d = 0; d < totalDays; d++) {
                LocalDate date = rangeStart.plusDays(d);
                int xx = d * pxPerDay;
                if (pxPerDay >= 8 && date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    g.setColor(new Color(0, 0, 0, 14));
                    g.fillRect(xx, HEADER_H, pxPerDay, h - HEADER_H);
                }
                if (pxPerDay >= 24) {
                    g.setColor(new Color(0xE6E6E6));
                    g.drawLine(xx, HEADER_H, xx, h);
                }
            }
            // header
            g.setColor(new Color(0xE4E4E4));
            g.fillRect(0, 0, w, HEADER_H);
            g.setColor(new Color(0x999999));
            g.drawLine(0, HEADER_H, w, HEADER_H);
            g.setFont(getFont().deriveFont(11f));
            for (int d = 0; d < totalDays; d++) {
                LocalDate date = rangeStart.plusDays(d);
                int xx = d * pxPerDay;
                boolean tick = pxPerDay >= 8 ? date.getDayOfWeek() == DayOfWeek.MONDAY || d == 0
                        : date.getDayOfMonth() == 1 || d == 0;
                if (tick) {
                    g.setColor(new Color(0xAAAAAA));
                    g.drawLine(xx, 0, xx, HEADER_H);
                    g.setColor(new Color(0x333333));
                    g.drawString(pxPerDay >= 8 ? date.format(DAY_LABEL) : date.format(MONTH_LABEL), xx + 4, 16);
                }
                if (pxPerDay >= 24) {
                    g.setColor(new Color(0x555555));
                    g.drawString(String.valueOf(date.getDayOfMonth()), xx + 4, 38);
                }
            }
            // rows
            for (int i = 0; i < tasks.size(); i++) {
                Task t = tasks.get(i);
                int rowY = HEADER_H + i * ROW_H;
                g.setColor(new Color(0xDDDDDD));
                g.drawLine(0, rowY + ROW_H, w, rowY + ROW_H);

                PlannedSchedule.Window win = plan.get(t);
                int barY = rowY + 9;
                int barH = 20;
                int x1 = x(win.start);
                int x2 = x(win.end.plusDays(1));

                g.setColor(new Color(0xF4F6FA));
                g.fillRect(x1, barY, x2 - x1, barH);

                LocalDate as = t.getActualStart();
                LocalDate ae = t.isComplete() ? t.getActualEnd() : (as != null ? today : null);
                if (as != null && ae != null) {
                    int ax1 = x(as);
                    int ax2 = x(ae.plusDays(1));
                    int blueEnd = Math.min(ax2, x2);
                    if (blueEnd > ax1) {
                        g.setColor(BLUE);
                        g.fillRect(ax1, barY + 1, blueEnd - ax1, barH - 1);
                    }
                    int hatchStart = Math.max(ax1, x2);
                    if (ax2 > hatchStart) {
                        hatch(g, hatchStart, barY, ax2 - hatchStart, barH);
                    }
                } else if (as == null && today != null && !t.isComplete() && !today.isBefore(win.start)) {
                    hatch(g, x1, barY, x(today.plusDays(1)) - x1, barH);   // waiting past its planned start
                }
                g.setColor(new Color(0x333333));
                g.drawRect(x1, barY, x2 - x1 - 1, barH);
            }
            // today line
            if (today != null) {
                int tx = x(today.plusDays(1));
                g.setColor(new Color(0x222222));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f));
                g.drawLine(tx, 0, tx, h);
            }
            g.dispose();
        }
    }

    private class Names extends JPanel {
        Names() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getY() >= HEADER_H) {
                        select((e.getY() - HEADER_H) / ROW_H);
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(NAME_W, canvasHeight());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(0xE4E4E4));
            g.fillRect(0, 0, getWidth(), HEADER_H);
            g.setColor(new Color(0x999999));
            g.drawLine(0, HEADER_H, getWidth(), HEADER_H);
            g.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
            g.setColor(new Color(0x222222));
            g.setFont(getFont().deriveFont(Font.BOLD, 12f));
            g.drawString("Task", 10, 28);
            if (session.hasProject()) {
                List<Task> tasks = session.getProject().getTasks();
                for (int i = 0; i < tasks.size(); i++) {
                    int rowY = HEADER_H + i * ROW_H;
                    if (tasks.get(i) == selected) {
                        g.setColor(new Color(0xE8F0FA));
                        g.fillRect(0, rowY, getWidth() - 1, ROW_H);
                    }
                    g.setColor(new Color(0xDDDDDD));
                    g.drawLine(0, rowY + ROW_H, getWidth(), rowY + ROW_H);
                    g.setColor(new Color(0x222222));
                    g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
                    g.drawString(tasks.get(i).getName(), 10, rowY + 24);
                }
            }
            g.dispose();
        }
    }

    /** Small legend icon: 0 = planned, 1 = work done, 2 = delay. */
    private static class Swatch extends JComponent {
        private final int type;

        Swatch(int type) {
            this.type = type;
            setPreferredSize(new Dimension(24, 14));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            if (type == 1) {
                g.setColor(BLUE);
                g.fillRect(0, 1, 22, 12);
            } else if (type == 2) {
                hatch(g, 0, 1, 22, 12);
            } else {
                g.setColor(new Color(0xF4F6FA));
                g.fillRect(0, 1, 22, 12);
            }
            g.setColor(new Color(0x333333));
            g.drawRect(0, 1, 21, 12);
            g.dispose();
        }
    }
}
