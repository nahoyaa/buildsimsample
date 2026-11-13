package buildsim;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Working days are Monday to Saturday (Sundays off). */
final class Workdays {
    private Workdays() { }

    static LocalDate nextWorkday(LocalDate d) {
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
            d = d.plusDays(1);
        }
        return d;
    }

    /** The date that is n working days after start (start counts as day 0). */
    static LocalDate plus(LocalDate start, int n) {
        LocalDate d = nextWorkday(start);
        for (int i = 0; i < n; i++) {
            d = nextWorkday(d.plusDays(1));
        }
        return d;
    }
}
