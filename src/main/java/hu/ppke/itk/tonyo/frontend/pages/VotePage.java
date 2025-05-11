package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.App;
import hu.ppke.itk.tonyo.frontend.cliens;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class VotePage {
    private final VBox view;
    private final cliens client;
    private final int pollId;
    private final Label errorLabel;

    public VotePage(int pollId, cliens client) {
        this.pollId = pollId;
        this.client = client;
        client.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Szavazás");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        // Példa: egy gomb az "igen" opcióra
        Button voteButton = new Button("Igen szavazat leadása");
        voteButton.setOnAction(e -> {
            JsonObject request = new JsonObject();
            request.addProperty("action", "submit_vote");
            request.addProperty("pollId", pollId);
            request.addProperty("optionId", 2); // Példa: "igen" opció
            client.sendRequest(request);
            System.out.println("Szavazás kérés elküldve: " + request);

            // Navigálás a LoadingPage-re
            ResultLoadingPage page = new ResultLoadingPage(pollId, client);
            Scene scene = new Scene(page.getView(), 600, 400);
             scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Várakozás eredményre");
            App.getPrimaryStage().setScene(scene);
        });

        view.getChildren().addAll(titleLabel, errorLabel, voteButton);
        System.out.println("VotePage konstruktor végén view gyermekek: " + view.getChildren());
    }

    public VBox getView() {
        return view;
    }

    public void handleServerMessage(JsonObject message) {
        System.out.println("VotePage: Kapott szerver válasz: " + message);
        if (message == null || !message.has("action")) {
            Platform.runLater(() -> showError("Hibás szerver válasz: hiányzó 'action' kulcs."));
            return;
        }

        String action = message.get("action").getAsString();
        if (action.equals("submit_vote")) {
            Platform.runLater(() -> {
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    System.out.println("Szavazás sikeresen leadva");
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Szavazás leadása sikertelen.";
                    showError(errorMessage);
                }
            });
        }
    }

    public void showError(String message) {
        errorLabel.setText(message);
        System.out.println("VotePage: Hibaüzenet megjelenítve: " + message);
    }
}