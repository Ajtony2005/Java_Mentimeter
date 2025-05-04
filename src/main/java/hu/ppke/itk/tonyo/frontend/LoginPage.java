package hu.ppke.itk.tonyo.frontend;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public class LoginPage {

    private final VBox pane;
    private final ServerCommunication serverComm;
    private final Stage stage;

    public LoginPage(Stage stage) {
        this.stage = stage;
        this.serverComm = new ServerCommunication();
        this.pane = createPane();
    }

    private VBox createPane() {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Felhasználónév");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Jelszó");

        Button loginButton = new Button("Bejelentkezés");
        Button toRegisterButton = new Button("Regisztráció");
        Button homePageButton = new Button ("Kezdőoldal");
        Label statusLabel = new Label();

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Töltsd ki mindkét mezőt!");
                return;
            }

            Task<String> loginTask = new Task<>() {
                @Override
                protected String call() {
                    return serverComm.sendRequest("login", username, password);
                }
            };
            loginTask.setOnSucceeded(event -> statusLabel.setText(loginTask.getValue()));
            loginTask.setOnFailed(event -> statusLabel.setText("Hiba: " + loginTask.getException().getMessage()));
            new Thread(loginTask).start();
        });

        VBox pane = new VBox(10, usernameField, passwordField, loginButton, toRegisterButton,  homePageButton, statusLabel);
        pane.setPadding(new Insets(20));
        return pane;
    }

    public VBox getPane() {
        return pane;
    }

    public void setSwitchAction(Runnable switchAction, Runnable homePage) {
        ((Button) pane.getChildren().get(3)).setOnAction(e -> switchAction.run());
        ((Button) pane.getChildren().get(4)).setOnAction(e-> homePage.run());
    }
}