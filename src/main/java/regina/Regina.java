package regina;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dateandtime.ReginaDateAndTime;
import errorhandling.ReginaException;
import file.Storage;
import tasks.DeadlinesTask;
import tasks.EventsTask;
import tasks.Task;
import tasks.TaskList;
import tasks.ToDosTask;


/**
 * The regina.Regina class represents a chatbot designed to help users track their tasks and activities.
 * It provides functionalities to add, mark, unmark, delete, and list tasks.
 */
public class Regina {
    private static final String NAME = "Regina";
    private static final String DEFAULT_SNOOZE_TYPE = "minute";
    private static final int DEFAULT_SNOOZE_VALUE = 30;

    private final Parser parser;
    private final Marker marker;
    private final Ui ui;
    private TaskList listOfTasks;

    /**
     * Constructs a regina.Regina instance containing an empty task list and initializes the marker.
     */
    public Regina() {
        this.ui = new Ui();
        try {
            listOfTasks = Storage.readSavedData();
        } catch (FileNotFoundException | ReginaException e) {
            ui.printMessage("Saved data file is missing....");
        }
        this.marker = new Marker(listOfTasks);
        this.parser = new Parser();
    }

    public TaskList getListOfTasks() {
        return this.listOfTasks;
    }

    public void setMainWindow() {
    }

    /**
     * Greets the user and provides instructions on how to interact with the chatbot.
     */
    public String greet() {
        return ui.greet(NAME);
    }

    /**
     * Validates whether a given task type is recognized.
     *
     * @param type The task type to check.
     * @return True if the task type is valid; false otherwise.
     */
    private boolean isValidTaskType(String type) {
        return type.equals("todo") || type.equals("deadline") || type.equals("event");
    }

    /**
     * Deletes all the tasks in the list
     */
    public String clearTaskList() {
        if (this.listOfTasks.isEmpty()) {
            return ui.printMessage("Nothing left to clear lah!");
        } else {
            this.listOfTasks.clear();
            saveFile();
            return ui.printMessage("Cleared all tasks!");
        }
    }

    /**
     * Validates if the command for marking or unmarking contains a number.
     *
     * @param parts The parts of the input command split into an array.
     * @throws ReginaException If the command does not properly contain a task number.
     */
    public boolean haveNumber(String[] parts) throws ReginaException {
        if (parts.length < 2) {
            throw new ReginaException("Which task you referring to lah!");
        }
        if (parts.length > 2) {
            throw new ReginaException("Follow the proper format please!\nType 'help' for reference.");
        }
        try {
            Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    /**
     * Snoozes target task by duration specified
     *
     * @param taskIndex An integer representing the index of the targeted task in the list of tasks.
     * @param durationType A string representing the type duration in days, hours or minutes.
     * @param durationValue An integer representing the value of the specified type to push back the endTime.
     *
     * @return A string representing the message to the user of the action done.
     */
    public String snoozeTask(int taskIndex, String durationType, int durationValue) {
        Task task = this.listOfTasks.get(taskIndex);
        String message = task.snoozeTask(durationType, durationValue);
        saveFile();
        return message;
    }

    /**
     * Lists all tasks occurring at the specified date and time.
     * <p>
     * This method takes a date and time input, creates an instance of ReginaDateAndTime,
     * and retrieves all tasks from the task list that occur on that specific date and time.
     * The resulting list of tasks is then displayed to the user via the regina.Ui class.
     *
     * @param dateAndTime A string representing the date and time to check for occurring tasks.
     * @throws ReginaException If the dateAndTime format is invalid or if an error occurs while
     *                         retrieving tasks.
     */
    public String occurringOn(String dateAndTime) throws ReginaException {
        ReginaDateAndTime occurringInstance = new ReginaDateAndTime(dateAndTime);
        TaskList tempList = this.listOfTasks.findTasksOccurringOn(occurringInstance);
        return ui.printMessage(tempList.toString());
    }

    /**
     * Searches for tasks in the list that contain the specified keyword and displays them to the user.
     *
     * <p>This method utilizes the {@link TaskList#findTasksWithKeyword(String)} method
     * to retrieve a list of tasks matching the given keyword. It then prints the result
     * to the user via the {@link Ui} instance. The method returns the filtered list of tasks.
     *
     * @param keyword The keyword to search for within task descriptions.
     *                The search is case-sensitive, and only tasks that contain
     *                the keyword in their string representation will be included in the result.
     * @return A {@link TaskList} containing tasks that match the keyword.
     */
    public TaskList find(String keyword) {
        TaskList tempList = this.listOfTasks.findTasksWithKeyword(keyword);
        ui.printMessage(tempList.toString());
        return tempList;
    }

    /**
     * Adds a new task based on the input command.
     *
     * @param input The user input string containing the task details.
     * @throws ReginaException If the input format is incorrect or invalid.
     */
    public String add(String input) throws ReginaException {
        String[] parts = input.split(" "); // Split input by spaces
        String taskType = parts[0];
        boolean isTaskDescriptionPresent = parts.length >= 2;
        if (!isTaskDescriptionPresent && isValidTaskType(taskType)) {
            String message = String.format("OOPS!!! Add your %s task description lah!", taskType);
            throw new ReginaException(message);
        }
        Task task = getTask(input, taskType);
        listOfTasks.add(task);
        saveFile();
        int noOfTasks = listOfTasks.size();
        return ui.printMessage(String.format(
                "Got it. I've added this task: \n  %s\nNow you have %d task%s in the list.\nJiayous!\n",
                task,
                noOfTasks,
                noOfTasks > 1 ? "s" : ""));
    }

    private static Task getTask(String input, String taskType) throws ReginaException {
        Task task;
        switch (taskType) {
        case "todo":
            String todoDescription = input.substring(5).trim();
            task = new ToDosTask(todoDescription);
            break;
        case "deadline":
            String[] deadlineParts = input.substring(9).trim().split(" /by ");
            // check if deadline was added for this task
            if (deadlineParts.length < 2) {
                throw new ReginaException("""
                        So....when's the deadline for this task?
                        Follow this format for the date and time please
                        (e.g. 01/10/2024 1700)""");
            }
            String deadlineDescription = deadlineParts[0];
            String deadline = deadlineParts[1];
            task = new DeadlinesTask(deadlineDescription, deadline);
            break;
        case "event":
            String[] eventParts = getEventStringSubpart(input);
            String eventDescription = eventParts[0];
            if (!eventParts[1].contains(" ")) {
                throw new ReginaException("NEITHER the start-time OR end-time can be left blank!\n"
                        + "Type 'help' for reference.");
            }
            String startTime = eventParts[1].substring(5).trim(); // take the substring after "from"
            String endTime = eventParts[2].substring(3).trim(); // take the substring after "to"
            task = new EventsTask(eventDescription, startTime, endTime);
            break;
        default:
            throw new ReginaException("Unknown task type. Use: todo, deadline, or event.");
        }
        return task;
    }

    private static String[] getEventStringSubpart(String input) throws ReginaException {
        String[] eventParts = input.substring(6).trim().split(" /");
        int length = eventParts.length;
        // check if there is the expected number of sub-parts
        if (length != 3) {
            throw new ReginaException("You need to add BOTH the start-time AND the end-time!\n"
                    + "Type 'help' for reference.");
        }
        // if the correct number of sub-parts then check if format is correct
        if (!(eventParts[1].contains("from") && eventParts[2].contains("to"))) {
            throw new ReginaException("OI! Use the correct format lah!\n"
                    + "Type 'help' for reference.");
        }
        return eventParts;
    }

    /**
     * Deletes a task at the specified index.
     *
     * @param index The index of the task to be deleted.
     * @throws ReginaException If the index is out of bounds or if there are no tasks to delete.
     */
    public String delete(int index) throws ReginaException {
        if (listOfTasks.isEmpty()) {
            throw new ReginaException("No more tasks to delete alr lah!");
        }
        if (index < 0) {
            throw new ReginaException("Choose index greater than 1 please!");
        }
        int taskCount = listOfTasks.size();
        if (index >= taskCount) {
            String message = String.format("You cannot count ah! There %s only %d task%s!",
                    taskCount > 1 ? "are" : "is",
                    taskCount,
                    taskCount > 1 ? "s" : "");
            throw new ReginaException(message);
        }
        Task task = listOfTasks.get(index);
        listOfTasks.remove(index);
        saveFile();
        taskCount = listOfTasks.size(); // update the number of tasks
        return ui.printMessage(String.format("Wah shiok!\nCan forget about %s liao!\nList now has %d task%s!\n",
                task.toString(),
                taskCount,
                taskCount > 1 ? "s" : ""));
    }

    /**
     * Lists all tasks currently in the task list.
     *
     * @throws ReginaException If there are no tasks to display.
     */
    public String list() throws ReginaException {
        if (listOfTasks.isEmpty()) {
            throw new ReginaException("HEHE no tasks for now!");
        }
        String taskList = IntStream.range(0, listOfTasks.size())
                .mapToObj(i -> (i + 1) + "." + listOfTasks.get(i).toString())
                .collect(Collectors.joining("\n"));
        return ui.printMessage(taskList);
    }

    /**
     * Marks a task based on the given index.
     *
     * @param index The index of the task to mark as done.
     * @throws ReginaException If the index is out of bounds.
     */
    public String mark(int index) throws ReginaException {
        this.marker.mark(index);
        Task task = this.listOfTasks.get(index);
        saveFile();
        return ui.printMessage(String.format("YAY! This task finish liao!\n%s\n", task.toString()));
    }

    /**
     * Unmarks a task based on the given index.
     *
     * @param index The index of the task to unmark.
     * @throws ReginaException If the index is out of bounds.
     */
    public String unmark(int index) throws ReginaException {
        assert index >= 0 && index < listOfTasks.size() : "Out of bounds index for mark: " + index;
        this.marker.unmark(index);
        Task task = this.listOfTasks.get(index);
        saveFile();
        return ui.printMessage(String.format("Hais! Need to do this task again!:\n%s\n", task.toString()));
    }

    /**
     * Saves the current list of tasks to a file.
     * This method attempts to persist the task data by calling the FileSaver's saveData method.
     * If an IOException occurs during this process, an error message will be displayed to inform the user.
     */
    public void saveFile() {
        try {
            Storage.saveData(listOfTasks.toSavedFormatting());
        } catch (IOException e) {
            ui.printMessage("******Error in syncing data******");
        }
    }

    /**
     * Processes user input and generates an appropriate response based on the command.
     *
     * <p>This method parses the input string for commands and executes the corresponding
     * actions based on the command type detected. It interacts with the Regina model
     * to update or retrieve information about tasks. If the command is one that requires
     * a task number, it validates the input and executes the appropriate method.</p>
     *
     * @param input the command input from the user as a String.
     * @return a String representing the response to the user, which may include error messages
     *         or confirmation of actions performed, such as marking, unmarking, or adding tasks.
     */
    public String getResponse(String input) {
        try {
            Optional<CommandData> commandData = parser.parse(input.toLowerCase());
            if (commandData.isPresent()) {
                CommandData data = commandData.get();
                switch (data.getCommandType()) {
                case "help":
                    return this.ui.help(); // Create a method to build the help response
                case "now":
                    return ReginaDateAndTime.getCurrentDateAndTime(); // Return current date and time as a string
                case "clear":
                    return clearTaskList();
                case "list":
                    return list(); // Create a method that returns the list of tasks as a string
                case "occurring":
                    return occurringOn(data.getRawInput().substring(10));
                case "find":
                    String[] parts1 = data.getRawInput().split(" ");
                    if (parts1.length >= 2) {
                        return find(data.getRawInput().substring(5)).toString();
                    } else {
                        throw new ReginaException("Find what lah!");
                    }
                case "mark":
                case "unmark":
                case "bye":
                    return ui.exit();
                case "delete":
                    String[] parts2 = data.getRawInput().split(" "); // Split raw input to get parts
                    if (haveNumber(parts2)) {
                        return getReplyForNumberedCommand(parts2, data);
                    } else {
                        throw new ReginaException("Use the task index in the list lah!");
                    }
                case "snooze":
                    return handleSnoozeCommand(data);
                case "add":
                    return add(input); // Add a new task
                default:
                    throw new ReginaException("Invalid input");
                }
            }
        } catch (ReginaException e) {
            return e.getMessage(); // Return the error messages
        }
        return "I'm not sure how to respond to that."; // Default response
    }

    private String handleSnoozeCommand(CommandData data) throws ReginaException {
        String[] parts = data.getRawInput().split(" ");
        // Check if custom duration is provided
        if (parts.length == 2) {
            // No custom duration: use the default snooze value (30 minutes)
            return snoozeByDefaultValue(parts);
        } else if (parts.length == 5) {
            // Custom duration format: snooze <task_number> /by <duration_value> <duration_type>
            if (!parts[2].equals("/by")) {
                throw new ReginaException("Invalid format for snooze command. "
                        + "Use: snooze <task_number> /by <duration_value> <duration_type>");
            }
            return snoozeByCustomValue(parts);
        } else {
            throw new ReginaException("Invalid format for snooze command. "
                    + "Use: snooze <task_number> [ /by <duration_value> <duration_type> ]");
        }
    }

    private String snoozeByCustomValue(String[] parts) throws ReginaException {
        int index = Integer.parseInt(parts[1]) - 1; // Convert to zero-based index
        int durationValue;
        try {
            durationValue = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            throw new ReginaException("Invalid duration value. Please enter a number.");
        }
        String durationType = getDurationType(parts[4].toLowerCase());
        if (!isValidDurationType(durationType)) {
            throw new ReginaException("Invalid duration type. Use: day, hour or minute.");
        }
        return snoozeTask(index, durationType, durationValue);
    }

    private String snoozeByDefaultValue(String[] parts) throws ReginaException {
        try {
            int index = Integer.parseInt(parts[1]) - 1; // Convert to zero-based index
            return snoozeTask(index, DEFAULT_SNOOZE_TYPE, DEFAULT_SNOOZE_VALUE);
        } catch (NumberFormatException e) {
            throw new ReginaException("Use task index number lah!");
        }
    }

    private String getDurationType(String input) {
        return switch (input) {
        case "days" -> "day";
        case "hours" -> "hour";
        case "minutes", "min" -> "minute";
        default -> input;
        };
    }

    private boolean isValidDurationType(String type) {
        return "day".equals(type)
                || "hour".equals(type)
                || "minute".equals(type);
    }

    private String getReplyForNumberedCommand(String[] parts, CommandData data) throws ReginaException {
        int index = Integer.parseInt(parts[1]) - 1; // Convert to zero-based index
        return switch (data.getCommandType()) {
        case "mark" -> mark(index);
        case "unmark" -> unmark(index);
        case "delete" -> delete(index);
        default -> this.ui.printMessage("Give a proper command lah!");
        };
    }

    public static void main(String[] args) {
    }
}
