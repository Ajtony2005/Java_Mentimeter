package hu.ppke.itk.tonyo.frontend;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.concurrent.Task;
import javafx.stage.Stage;

public class HomePage {

    private final VBox pane;
    private final ServerCommunication serverComm;
    private final Stage stage;

    public HomePage(Stage stage) {
        this.stage = stage;
        this.serverComm = new ServerCommunication();
        this.pane = createPane();
    }

    private VBox createPane() {
        TextField codeField = new TextField();
        codeField.setPromptText("Csatlakozási kód");

        Button joinButton = new Button("Csatlakozás");
        Button createButton = new Button("Létrehozás");
        Button toLoginButton = new Button("Bejelentkezés");
        Label statusLabel = new Label();

        joinButton.setOnAction(e -> {
            String code = codeField.getText().trim();

            if (code.isEmpty()) {
                statusLabel.setText("Add meg a csatlakozási kódot!");
                return;
            }

            Task<String> joinTask = new Task<>() {
                @Override
                protected String call() {
                    return serverComm.sendRequest("join", code, "");
                }
            };
            joinTask.setOnSucceeded(event -> {
                String result = joinTask.getValue();
                statusLabel.setText(result);
                if (result.startsWith("Kód: 0")) {
                    // Sikeres csatlakozás, navigálás a bejelentkezési oldalra
                    stage.setScene(new LoginPage(stage).getPane().getScene());
                    stage.setTitle("Bejelentkezés");
                }
            });
            joinTask.setOnFailed(event -> statusLabel.setText("Hiba: " + joinTask.getException().getMessage()));
            new Thread(joinTask).start();
        });

        createButton.setOnAction(e -> {
            Task<String> createTask = new Task<>() {
                @Override
                protected String call() {
                    return serverComm.sendRequest("create", "", "");
                }
            };
            createTask.setOnSucceeded(event -> {
                String result = createTask.getValue();
                statusLabel.setText(result);
                if (result.startsWith("Kód: 0")) {
                    // Sikeres létrehozás, navigálás a bejelentkezési oldalra
                    stage.setScene(new LoginPage(stage).getPane().getScene());
                    stage.setTitle("Bejelentkezés");
                }
            });
            createTask.setOnFailed(event -> statusLabel.setText("Hiba: " + createTask.getException().getMessage()));
            new Thread(createTask).start();
        });

        toLoginButton.setOnAction(e -> {
            stage.setScene(new LoginPage(stage).getPane().getScene());
            stage.setTitle("Bejelentkezés");
        });

        VBox pane = new VBox(10, codeField, joinButton, createButton, toLoginButton, statusLabel);
        pane.setPadding(new Insets(20));
        return pane;
    }

    public VBox getPane() {
        return pane;
    }

    public void setSwitchAction(Runnable switchToLogin, Runnable switchToRegister) {
        ((Button) pane.getChildren().get(3)).setOnAction(e -> switchToLogin.run());
    }
}