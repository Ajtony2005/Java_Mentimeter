package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.App;
import hu.ppke.itk.tonyo.frontend.cliens;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoadingPage {
    private final VBox view;
    private final Label errorLabel;
    private final cliens Cliens;
    private final int pollId;

    public LoadingPage(int pollId, cliens client) {
        if (client == null) {
            throw new IllegalArgumentException("Client cannot be null");
        }
        this.Cliens = client;
        this.pollId = pollId;
        client.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Várakozás a szavazás indulására");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label messageLabel = new Label("Kérlek, várj, amíg a kérdező elindítja a szavazást.");
        messageLabel.setFont(Font.font("Arial", 14));

        Button backButton = new Button("Vissza a kezdőoldalra");
        backButton.setOnAction(e -> goBackToMainMenu());

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, messageLabel, backButton, errorLabel);
    }

    public VBox getView() {
        return view;
    }

    private void goBackToMainMenu() {
        HomePage page = new HomePage();
        Scene scene = new Scene(page.getView(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Főmenü");
        App.getPrimaryStage().setScene(scene);
    }

    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        String action = message.get("action").getAsString();
        if (action.equals("poll_status_update")) {
            if (message.has("pollId") && message.get("pollId").getAsInt() == pollId) {
                String newStatus = message.get("newStatus").getAsString();
                if (newStatus.equals("SZAVAZAS")) {
                    VotePage votePage = new VotePage(pollId, Cliens);
                    Scene scene = new Scene(votePage.getView(), 600, 400);
                    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    App.getPrimaryStage().setTitle("Szavazás");
                    App.getPrimaryStage().setScene(scene);
                }
            }
        }
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }
}