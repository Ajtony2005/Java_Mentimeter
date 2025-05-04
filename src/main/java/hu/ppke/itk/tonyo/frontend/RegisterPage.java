package hu.ppke.itk.tonyo.frontend;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public class RegisterPage {

    private final VBox pane;
    private final ServerCommunication serverComm;
    private final Stage stage;

    public RegisterPage(Stage stage) {
        this.stage = stage;
        this.serverComm = new ServerCommunication();
        this.pane = createPane();
    }

    private VBox createPane() {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Felhasználónév");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Jelszó");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Jelszó megerősítése");

        Button registerButton = new Button("Regisztráció");
        Button toLoginButton = new Button("Bejelentkezés");
        Button homePageButton = new Button("Kezdőoldal");
        Label statusLabel = new Label();

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            String confirmPassword = confirmPasswordField.getText().trim();

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                statusLabel.setText("Töltsd ki mindhárom mezőt!");
                return;
            }

            if (!password.equals(confirmPassword)) {
                statusLabel.setText("A jelszavak nem egyeznek!");
                return;
            }

            Task<String> registerTask = new Task<>() {
                @Override
                protected String call() {
                    return serverComm.sendRequest("register", username, password);
                }
            };
            registerTask.setOnSucceeded(event -> statusLabel.setText(registerTask.getValue()));
            registerTask.setOnFailed(event -> statusLabel.setText("Hiba: " + registerTask.getException().getMessage()));
            new Thread(registerTask).start();
        });

        VBox pane = new VBox(10, usernameField, passwordField, confirmPasswordField, registerButton, toLoginButton, homePageButton, statusLabel);
        pane.setPadding(new Insets(20));
        return pane;
    }

    public VBox getPane() {
        return pane;
    }

    public void setSwitchAction(Runnable switchToLogin, Runnable homePage) {
        ((Button) pane.getChildren().get(4)).setOnAction(e -> switchToLogin.run()); // toLoginButton
        ((Button) pane.getChildren().get(5)).setOnAction(e -> homePage.run()); // backButton
    }
}