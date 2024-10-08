package gui;

import java.util.Objects;

import errorhandling.ReginaException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import regina.Regina;
import tasks.TaskList;

/**
 * Controller for the main GUI of the Regina chatbot application.
 * This class manages user input and displays the dialog container for chat messages and task checkboxes.
 */
public class MainWindow extends AnchorPane {
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox dialogContainer; // To handle user messages
    @FXML
    private TextField userInput; // Input field for user messages
    @FXML
    private Button sendButton; // Button to send messages
    @FXML
    private VBox checkboxContainer; // This VBox will hold dynamically created checkboxes

    private Regina regina; // Reference to the Regina instance

    // Images for user and Regina Chatbot
    private final Image userImage = new Image(Objects.requireNonNull(
            this.getClass().getResourceAsStream("/images/User.jpg")));
    private final Image reginaImage = new Image(Objects.requireNonNull(
            this.getClass().getResourceAsStream("/images/Regina.jpg")));

    /**
     * Initializes the MainWindow by binding the scroll pane's value property
     * to the height of the dialog container, and loading the initial greeting
     * from Regina.
     */
    @FXML
    public void initialize() {
        scrollPane.vvalueProperty().bind(dialogContainer.heightProperty());
        if (regina != null) {
            dialogContainer.getChildren().addAll(DialogBox.getReginaDialog(regina.greet(), reginaImage));
            loadCheckboxes(); // Load task checkboxes when the main window is initialized
        }
    }

    /**
     * Sets the Regina instance for the MainWindow and loads the task checkboxes.
     *
     * @param r The instance of Regina to be set.
     */
    public void setRegina(Regina r) {
        regina = r; // Set the Regina instance
        if (regina != null) {
            loadCheckboxes(); // Load checkboxes whenever Regina is set
        }
    }

    /**
     * Loads the task checkboxes from the Regina instance into the checkbox container,
     * clearing any existing checkboxes first.
     */
    public void loadCheckboxes() {
        TaskList tasks = regina.getListOfTasks(); // Fetch the task list
        checkboxContainer.getChildren().clear(); // Clear existing checkboxes

        for (int i = 0; i < tasks.size(); i++) { // Iterate through tasks
            String taskDescription = tasks.get(i).toString().substring(7);
            boolean isDone = tasks.get(i).isDone();
            CheckBox taskCheckBox = getCheckBox((i + 1) + ". " + taskDescription, isDone, i);
            taskCheckBox.setWrapText(true);

            checkboxContainer.getChildren().add(taskCheckBox); // Add to the checkbox container
        }
    }

    /**
     * Creates a CheckBox for a task with the specified description, completion status, and index.
     * It sets an action handler to mark or unmark the task when the checkbox is toggled.
     *
     * @param taskDescription The description of the task.
     * @param isDone The completion status of the task.
     * @param i The index of the task in the task list.
     * @return A CheckBox control representing the task.
     */
    private CheckBox getCheckBox(String taskDescription, boolean isDone, int i) {
        CheckBox taskCheckBox = new CheckBox(taskDescription);
        taskCheckBox.setSelected(isDone);

        taskCheckBox.setOnAction(e -> markTaskAndSendResponse(taskCheckBox, i));
        return taskCheckBox;
    }

    private void markTaskAndSendResponse(CheckBox checkbox, int i) {
        try {
            String response = checkbox.isSelected() ? regina.mark(i) : regina.unmark(i);
            dialogContainer.getChildren().addAll(
                    DialogBox.getReginaDialog(response, reginaImage)
            );
        } catch (ReginaException ex) {
            System.out.println(ex.getMessage());
        }
    }

    @FXML
    private void handleUserInput() {
        String input = userInput.getText();
        if (!input.isEmpty()) {
            String response = regina.getResponse(input.toLowerCase());
            dialogContainer.getChildren().addAll(
                    DialogBox.getUserDialog(input, userImage),
                    DialogBox.getReginaDialog(response, reginaImage)
            );
        }
        loadCheckboxes();
        // Clear the input field after handler
        userInput.clear();
        if (input.equals("bye")) {
            // Create a countdown before closing
            final int[] countdown = {3}; // Countdown starts at 3
            Timeline timeline = new Timeline(new KeyFrame(
                    Duration.seconds(1),
                    event -> {
                        if (countdown[0] > 0) {
                            dialogContainer.getChildren().add(DialogBox.getReginaDialog(
                                    "Closing in " + countdown[0], reginaImage
                            ));
                            countdown[0]--;
                        } else {
                            Platform.exit();
                        }
                    }
            ));
            timeline.setCycleCount(4); // Run 4 times: 3, 2, 1, and then close
            timeline.play();
        }
    }
}
