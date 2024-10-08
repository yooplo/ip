package dateandtime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import errorhandling.ReginaException;

/**
 * The ReginaDateAndTime class represents a date and/or time,
 * providing functionality to format these values.
 */
public class ReginaDateAndTime {
    private static final String INPUT_DATE_PATTERN = "d/M/yyyy";
    private static final String INPUT_TIME_PATTERN = "HHmm";
    private static final String OUTPUT_DATE_PATTERN = "MMM dd yyyy";
    private static final String OUTPUT_TIME_PATTERN = "h.mm a";

    private LocalDate date;
    private LocalTime time;
    private String savedFormat;

    /**
     * Constructs a ReginaDateAndTime instance with the specified date and time.
     *
     * @param dateAndTime The date and time in the format 'd/M/yyyy HHmm'.
     */
    public ReginaDateAndTime(String dateAndTime) throws ReginaException {
        this.savedFormat = dateAndTime;
        String[] dateTime = dateAndTime.split(" ");
        if (dateTime.length < 2) {
            throw new ReginaException("Ehh you need to write BOTH the date and time");
        }
        String dateString = dateTime[0];
        String timeString = dateTime[1];
        parseInDateAndTime(dateString, timeString);
    }

    private void parseInDateAndTime(String dateString, String timeString) throws ReginaException {
        try {
            this.date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern(INPUT_DATE_PATTERN));
            this.time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern(INPUT_TIME_PATTERN));
        } catch (DateTimeParseException e) {
            throw new ReginaException("Invalid date or time format provided.");
        }
    }

    /**
     * Returns a formatted string representing the current date and time.
     * The date is formatted according to the DATE_PATTERN, and the time
     * is formatted according to the TIME_PATTERN.
     *
     * @return A string indicating day, date and time of this instant.
     */
    public static String getCurrentDateAndTime() {
        String nowDate = LocalDate.now().format(DateTimeFormatter.ofPattern(OUTPUT_DATE_PATTERN));
        String nowDayOfWeek = LocalDate.now().getDayOfWeek().name();
        String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern(OUTPUT_TIME_PATTERN));
        return String.format("Today is %s, %s, and the time is currently %s", nowDayOfWeek, nowDate, nowTime);
    }

    /**
     * Returns the stored date.
     *
     * @return The date as a LocalDate instance.
     */
    public LocalDate getDate() {
        return this.date;
    }

    /**
     * Returns the stored time.
     *
     * @return The time as a LocalTime instance.
     */
    public LocalTime getTime() {
        return this.time;
    }

    /**
     * Adds minutes to the time value.
     *
     * @param min The value of the number of minutes to add to the time.
     */
    public void pushBackTime(int min) {
        LocalTime originalTime = this.time;
        this.time = this.time.plusMinutes(min);

        // Check if pushing the time back crosses midnight
        if (this.time.isBefore(originalTime)) {
            this.pushBackDate(1); // Push the date back if we've crossed midnight
        }
        updateSavedFormat();
    }

    /**
     * Adds days to the date value.
     *
     * @param days The value of the number of days to add to the date.
     */
    public void pushBackDate(int days) {
        this.date = this.date.plusDays(days);
        updateSavedFormat();
    }

    /**
     * Updates the savedFormat to reflect the current date and time in the input pattern format.
     */
    private void updateSavedFormat() {
        String formattedDate = this.date.format(DateTimeFormatter.ofPattern(INPUT_DATE_PATTERN));
        String formattedTime = this.time.format(DateTimeFormatter.ofPattern(INPUT_TIME_PATTERN));
        this.savedFormat = formattedDate + " " + formattedTime; // Update savedFormat to the new pattern
    }

    /**
     * Checks if this date and time is before the given date and time.
     *
     * @param dateAndTime The ReginaDateAndTime instance to compare against.
     * @return true if this date and time is before the given instance; false otherwise.
     */
    public boolean isBefore(ReginaDateAndTime dateAndTime) {
        if (this.date.isAfter(dateAndTime.getDate())) {
            return false;
        }
        if (this.date.isEqual(dateAndTime.getDate()) && this.time.isAfter(dateAndTime.getTime())) {
            return false;
        }
        return true;
    }

    /**
     * Checks if this date and time is equal to the given date and time.
     *
     * @param dateAndTime The ReginaDateAndTime instance to compare against.
     * @return true if this date and time is equal to the given instance; false otherwise.
     */
    public boolean isEqual(ReginaDateAndTime dateAndTime) {
        return this.date.isEqual(dateAndTime.getDate()) && this.time.equals(dateAndTime.getTime());
    }

    /**
     * Checks if this date and time is after the given date and time.
     *
     * @param targetInstance The ReginaDateAndTime instance to compare against.
     * @return true if this date and time is after the given instance; false otherwise.
     */
    public boolean isAfter(ReginaDateAndTime targetInstance) {
        boolean currentDateBeforeTargetDate = this.date.isBefore(targetInstance.getDate());
        boolean currentTimeBeforeTargetTime = this.date.isEqual(targetInstance.getDate())
                && this.time.isBefore(targetInstance.getTime());
        boolean currentInstanceEqualTargetInstance = this.date.isEqual(targetInstance.getDate())
                && this.time.equals(targetInstance.getTime());
        return !currentInstanceEqualTargetInstance && !currentDateBeforeTargetDate && !currentTimeBeforeTargetTime;
    }

    /**
     * Returns the formatted date as a string based on the DATE_PATTERN.
     *
     * @return A formatted string representing the date.
     */
    private String getFormattedDate() {
        return this.date.format(DateTimeFormatter.ofPattern(OUTPUT_DATE_PATTERN));
    }

    /**
     * Returns the formatted time as a string based on the TIME_PATTERN.
     *
     * @return A formatted string representing the time.
     */
    private String getFormattedTime() {
        return this.time.format(DateTimeFormatter.ofPattern(OUTPUT_TIME_PATTERN));
    }

    /**
     * Returns a string representation of the date and time in a format suitable for saving.
     *
     * @return A string representing date and time for data saving.
     */
    public String toSavedFormatting() {
        return this.savedFormat;
    }

    /**
     * Returns a string representation of the date and time.
     *
     * @return A string in the format: "MMM dd yyyy hh:mm" or "MMM dd yyyy" if time is not provided.
     */
    @Override
    public String toString() {
        // Combine formatted date and time
        String formattedTimeIfExist = this.time != null ? " " + getFormattedTime() : "";
        return getFormattedDate() + "," + formattedTimeIfExist;
    }
}
