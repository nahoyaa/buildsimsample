package buildsim;

import java.util.Locale;

/** Small formatting helpers shared by the screens. */
final class Fmt {
    private Fmt() { }

    static String money(double v) { return String.format(Locale.US, "PHP %,.0f", v); }
    static String num(double v) { return String.format(Locale.US, "%,.0f", v); }
    static String num1(double v) { return String.format(Locale.US, "%,.1f", v); }
    static String pct(double v) { return String.format(Locale.US, "%.1f%%", v); }
}
