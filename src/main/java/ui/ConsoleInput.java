package ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Reads and validates data typed by the user in the console.
 * It only knows how to read text, numbers, options and dates;
 * it does not know anything about the parking business rules.
 * Typing the cancel key at any prompt cancels the current operation.
 */
public class ConsoleInput {

    /**
     * Key the user types to cancel the current operation and go back to the menu.
     */
    public static final String CANCEL_KEY = "*";

    /**
     * Thrown when the user types the cancel key. The menu catches it
     * and returns to the main menu without changing anything.
     */
    public static class InputCancelledException extends RuntimeException {

        public InputCancelledException() {
            super("Operation cancelled.");
        }
    }

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Reads a non-empty line of text.
     */
    public String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = readLine();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("  This field cannot be empty.");
        }
    }

    /**
     * Reads a whole number between min and max (both included).
     */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            String text = readText(prompt);
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                // handled below with the same message
            }
            System.out.println("  Enter a number between " + min + " and " + max + ".");
        }
    }

    /**
     * Shows the values of an enumeration as a numbered list and returns
     * the one the user picks. It works for any enum, without a switch.
     */
    public <E extends Enum<E>> E readOption(String title, E[] values) {
        System.out.println(title);
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i]);
        }
        int option = readInt("Option: ", 1, values.length);
        return values[option - 1];
    }

    /**
     * Reads a date and time in the format yyyy-MM-dd HH:mm.
     * Pressing Enter without typing anything uses the current date and time.
     */
    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd HH:mm, Enter = now): ");
            String line = readLine();
            if (line.isEmpty()) {
                return LocalDateTime.now().withSecond(0).withNano(0);
            }
            try {
                return LocalDateTime.parse(line, DATE_TIME_FORMAT);
            } catch (DateTimeParseException exception) {
                System.out.println("  Invalid format. Example: 2026-10-02 08:30");
            }
        }
    }

    /**
     * Reads one line and checks whether the user wants to cancel.
     */
    private String readLine() {
        String line = scanner.nextLine().trim();
        if (line.equals(CANCEL_KEY)) {
            throw new InputCancelledException();
        }
        return line;
    }

    public String format(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMAT);
    }
}