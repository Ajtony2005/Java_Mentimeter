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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
/**
 * A kezdőoldalt megvalósító osztály, ahol a felhasználó csatlakozhat egy szavazáshoz
 * vagy létrehozhat egy új szavazást.
 */
public class ConnectPage {
    /** Az oldal fő elrendezése (VBox típusú konténer). */
    private final VBox view;

    /** Szövegmező, ahova a felhasználó beírja a csatlakozási kódot. */
    private final TextField joinCodeField;

    /** Címke, amely a hibaüzenetek megjelenítésére szolgál. */
    private final Label errorLabel;

    /** Az ügyfél (kliens) objektum, amely kapcsolatot tart a szerverrel. */
    private final cliens Cliens;

    /**
     * Az oldal konstruktora. Inicializálja az összes komponenst, és beállítja az eseménykezelőket.
     */

    public ConnectPage() {
        Cliens = App.getCliens();
        Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Mentimeter - Kezdőoldal");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label joinCodeLabel = new Label("Csatlakozási kód:");
        joinCodeField = new TextField();
        joinCodeField.setPromptText("Add meg a kódot");
        joinCodeField.setMaxWidth(200);

        Button joinButton = new Button("Csatlakozás");
        joinButton.setOnAction(e -> joinPoll());

        Button createPollButton = new Button("Szavazás létrehozása");
        createPollButton.setOnAction(e -> openCreatePoll());

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, joinCodeLabel, joinCodeField, joinButton, createPollButton, errorLabel);
    }
    /**
     * Visszaadja az oldal fő nézetét, amely a JavaFX Scene-hez adható.
     *
     * @return a VBox típusú nézet
     */
    public VBox getView() {
        return view;
    }
    /**
     * Eseménykezelő, amely akkor fut le, amikor a felhasználó a "Csatlakozás" gombra kattint.
     * Elkészíti és elküldi a csatlakozási kérést a szerverre.
     */
    private void joinPoll() {
        String joinCode = joinCodeField.getText().trim();
        if (joinCode.isEmpty()) {
            showError("Kérlek, add meg a csatlakozási kódot!");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "join_poll");
        request.addProperty("joinCode", joinCode);
        Cliens.sendRequest(request);
        System.out.println("Csatlakozási kérés elküldve: " + request);
    }
    /**
     * Eseménykezelő, amely akkor fut le, amikor a felhasználó a "Szavazás létrehozása" gombra kattint.
     * Átirányítja a felhasználót a megfelelő oldalra.
     */
    private void openCreatePoll() {
        int userId = Cliens.getUserId();
        System.out.println("openCreatePoll: userId = " + userId);
        if (userId == -1) {
            System.out.println("Nincs bejelentkezett felhasználó, navigálás a LoginPage-re");
            LoginPage page = new LoginPage();
            Scene scene = new Scene(page.getView(), 400, 300);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Mentimeter - Bejelentkezés");
            App.getPrimaryStage().setScene(scene);
        } else {
            System.out.println("Bejelentkezett felhasználó, navigálás a CreatePollPage-re");
            CreatePollPage page = new CreatePollPage();
            Scene scene = new Scene(page.getView(), 600, 400);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Mentimeter - Szavazás létrehozása");
            App.getPrimaryStage().setScene(scene);
        }
    }
    /**
     * Kezeli a szervertől érkező válaszokat.
     * Ha a csatlakozás sikeres, átlépteti a felhasználót a szavazási oldalra.
     * Ha sikertelen, hibaüzenetet jelenít meg.
     *
     * @param message a szervertől kapott JSON üzenet
     */
    public void handleServerMessage(JsonObject message) {
        System.out.println("ConnectPage: Kapott szerver válasz: " + message);
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        String action = message.get("action").getAsString();
        if (action.equals("join_poll")) {
            Platform.runLater(() -> {
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    int pollId = message.getAsJsonObject("poll").get("pollId").getAsInt();
                    VotePage page = new VotePage(pollId, Cliens);
                    Scene scene = new Scene(page.getView(), 600, 400);
                    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    App.getPrimaryStage().setTitle("Mentimeter - Szavazás");
                    App.getPrimaryStage().setScene(scene);
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Csatlakozás sikertelen.";
                    showError(errorMessage);
                }
            });
        }
    }
    /**
     * Hibaüzenet megjelenítése a felhasználónak.
     * A megjelenítés mindig a JavaFX UI szálán történik.
     *
     * @param message a megjelenítendő hibaüzenet
     */
    public void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            System.out.println("ConnectPage: Hibaüzenet megjelenítve: " + message);
        });
    }
}