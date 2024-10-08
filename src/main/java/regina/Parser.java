package regina;

import java.util.Optional;

/**
 * The Parser class is responsible for interpreting user commands input into the Regina chatbot.
 * It validates command types and extracts relevant information from user input, facilitating
 * the command execution process.
 */
public class Parser {
    /**
     * Parses the user input and returns the command type and relevant data.
     *
     * @param input The user input string.
     * @return An Optional of regina.CommandData containing command details (command type, task details).
     */
    public Optional<CommandData> parse(String input) {
        assert input != null : "Input should not be null";
        // Check for command types
        String command = input.split(" ")[0];

        if (isValidCommand(command)) {
            return Optional.of(new CommandData(command, input));
        } else {
            return Optional.of(new CommandData("add", input));
        }
    }

    /**
     * Checks if a command is valid.
     *
     * @param command The command to check.
     * @return True if valid, false otherwise.
     */
    private boolean isValidCommand(String command) {
        return command.equals("help")
                || command.equals("now")
                || command.equals("clear")
                || command.equals("list")
                || command.startsWith("occurring")
                || command.startsWith("mark")
                || command.startsWith("unmark")
                || command.startsWith("delete")
                || command.equals("bye")
                || command.startsWith("find")
                || command.startsWith("snooze");
    }
}
