package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.cliens;
import hu.ppke.itk.tonyo.frontend.App;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Ez az osztály megvalósítja a szavazáshoz való csatlakozás oldalát.
 * A felhasználó itt tudja megadni a csatlakozási kódot és csatlakozni egy szavazáshoz.
 */
public class JoinPollPage {
    private final VBox view;
    private final Label errorLabel;
    private final cliens Cliens;

    /**
     * Konstruktor, amely létrehozza az oldal elemeit és beállítja az eseménykezelőket.
     */
    public JoinPollPage() {
        Cliens = App.getCliens();
        Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Szavazáshoz csatlakozás");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        TextField joinCodeField = new TextField();
        joinCodeField.setPromptText("Csatlakozási kód");
        joinCodeField.setMaxWidth(200);

        Button joinButton = new Button("Csatlakozás");
        joinButton.setOnAction(e -> {
            String joinCode = joinCodeField.getText().trim();
            if (joinCode.isEmpty()) {
                showError("Kérlek, add meg a csatlakozási kódot!");
                return;
            }
            JsonObject request = new JsonObject();
            request.addProperty("action", "join_poll");
            request.addProperty("joinCode", joinCode);
            Cliens.sendRequest(request);
        });

        Button backButton = new Button("Vissza a főmenübe");
        backButton.setOnAction(e -> {
            HomePage page = new HomePage();
            Scene scene = new Scene(page.getView(), 600, 400);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Mentimeter - Főmenü");
            App.getPrimaryStage().setScene(scene);
        });

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, joinCodeField, joinButton, backButton, errorLabel);
    }

    /**
     * Visszaadja az oldal fő nézetét (VBox).
     *
     * @return a VBox nézet
     */
    public VBox getView() {
        return view;
    }

    /**
     * Kezeli a szervertől érkező üzeneteket.
     * Az üzenet alapján eldönti, hogy a felhasználót melyik oldalra kell továbblépni.
     *
     * @param message a szervertől érkezett JSON objektum
     */
    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        String action = message.get("action").getAsString();
        if (action.equals("join_poll")) {
            if (message.has("status") && message.get("status").getAsString().equals("success")) {
                if (!message.has("poll") || message.get("poll").isJsonNull()) {
                    showError("Hibás szerver válasz: hiányzó vagy érvénytelen 'poll' objektum.");
                    return;
                }
                JsonObject poll = message.get("poll").getAsJsonObject();
                int pollId = poll.get("pollId").getAsInt();


                String pollStatus = poll.has("status") ? poll.get("status").getAsString() : null;

                if (pollStatus == null) {
                    showError("Hiba: A szerver nem küldte a szavazás állapotát.");
                    return;
                }

                if (pollStatus.equals("NYITOTT")) {
                    LoadingPage loadingPage = new LoadingPage(pollId, Cliens);
                    Scene scene = new Scene(loadingPage.getView(), 600, 400);
                    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    App.getPrimaryStage().setTitle("Várakozás");
                    App.getPrimaryStage().setScene(scene);
                } else if (pollStatus.equals("SZAVAZAS")) {
                    VotePage votePage = new VotePage(pollId, Cliens);
                    Scene scene = new Scene(votePage.getView(), 600, 400);
                    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    App.getPrimaryStage().setTitle("Szavazás");
                    App.getPrimaryStage().setScene(scene);
                } else {
                    showError("A szavazás nem elérhető ebben az állapotban: " + pollStatus);
                }
            } else {
                String errorMessage = message.has("message") ? message.get("message").getAsString() : "Csatlakozás sikertelen.";
                showError(errorMessage);
            }
        }
    }

    /**
     * Hibát jelenít meg az oldalon.
     *
     * @param message a megjelenítendő hibaüzenet
     */
    public void showError(String message) {
        errorLabel.setText(message);
    }
}
