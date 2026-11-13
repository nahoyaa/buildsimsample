package buildsim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/** Small helper for reading validated input from the console. */
public class Input {
    private final Scanner scanner = new Scanner(System.in);

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("Input closed");
        }
        return scanner.nextLine().trim();
    }

    public String readText(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (!s.isEmpty()) {
                return s;
            }
            System.out.println("  Please enter a value.");
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt);
            try {
                int v = Integer.parseInt(s);
                if (v >= min && v <= max) {
                    return v;
                }
                System.out.println("  Enter a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    public double readDouble(String prompt, double min) {
        while (true) {
            String s = readLine(prompt).replace(",", "");
            try {
                double v = Double.parseDouble(s);
                if (Double.isFinite(v) && v >= min) {
                    return v;
                }
                System.out.println("  Enter a number that is at least " + min + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a number.");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String s = readLine(prompt);
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("  Use the format yyyy-mm-dd, for example 2026-11-02.");
            }
        }
    }

    /** Returns null when the user just presses Enter. */
    public LocalDate readDateOrBlank(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("  Use the format yyyy-mm-dd, or press Enter to skip.");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String s = readLine(prompt).toLowerCase();
            if (s.equals("y") || s.equals("yes")) {
                return true;
            }
            if (s.equals("n") || s.equals("no")) {
                return false;
            }
            System.out.println("  Please answer y or n.");
        }
    }

    /** Shows a numbered list and returns the chosen item, or null if empty or cancelled. */
    public <T> T choose(String title, List<T> items) {
        if (items.isEmpty()) {
            System.out.println("  Nothing available yet for: " + title);
            return null;
        }
        System.out.println(title);
        for (int i = 0; i < items.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + items.get(i));
        }
        System.out.println("  0. Cancel");
        int c = readInt("Choose: ", 0, items.size());
        return c == 0 ? null : items.get(c - 1);
    }
}
