package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.cliens;
import hu.ppke.itk.tonyo.frontend.App;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class PollControlPage {
    private final VBox view;
    private final Label errorLabel;
    private final cliens Cliens;
    private final int pollId;
    private final String pollTitle;

    public PollControlPage(int pollId, String pollTitle, String currentStatus, cliens Cliens) {
        this.Cliens = Cliens;
        this.pollId = pollId;
        this.pollTitle = pollTitle;
        this.Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Szavazás kezelőfelület: " + pollTitle);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label statusLabel = new Label("Jelenlegi állapot: " + currentStatus);

        Button openButton = new Button("Szavazás megnyitása (NYITOTT)");
        openButton.setOnAction(e -> updatePollStatus("NYITOTT"));

        Button startButton = new Button("Szavazás indítása (SZAVAZAS)");
        startButton.setOnAction(e -> updatePollStatus("SZAVAZAS"));

        Button endButton = new Button("Szavazás befejezése (EREDMENY)");
        endButton.setOnAction(e -> updatePollStatus("EREDMENY"));

        Button closeButton = new Button("Szavazás lezárása (LEZART)");
        closeButton.setOnAction(e -> updatePollStatus("LEZART"));

        Button backButton = new Button("Vissza a kezelőoldalra");
        backButton.setOnAction(e -> goBackToManagePage());

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, statusLabel, openButton, startButton, endButton, closeButton, backButton, errorLabel);
    }

    public VBox getView() {
        return view;
    }

    private void updatePollStatus(String newStatus) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "update_poll_status");
        request.addProperty("pollId", pollId);
        request.addProperty("newStatus", newStatus);
        Cliens.sendRequest(request);
    }

    private void goBackToManagePage() {
        ManagePollPage page = new ManagePollPage();
        Scene scene = new Scene(page.getView(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Szavazások kezelése");
        App.getPrimaryStage().setScene(scene);
    }


    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        String action = message.get("action").getAsString();
        if (action.equals("update_poll_status")) {
            if (message.has("status") && message.get("status").getAsString().equals("success")) {
                showError("Szavazás állapota frissítve!");
                goBackToManagePage(); // Visszalépés a kezelőoldalra a sikeres állapotfrissítés után
            } else {
                String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
                showError(errorMessage);
            }
        }
    }

public void showError(String message) {
    errorLabel.setText(message);
}
}