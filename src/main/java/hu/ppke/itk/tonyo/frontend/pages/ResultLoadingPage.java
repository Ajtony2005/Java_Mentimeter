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

/**
 * A {@code ResultLoadingPage} osztály a szavazás eredményeinek betöltését megjelenítő oldal.
 * Ez az oldal várakozik a szavazás eredményeinek megérkezésére, és lehetőséget biztosít
 * a felhasználónak, hogy visszatérjen a kezdőoldalra.
 */
public class ResultLoadingPage {
    /**
     * A felhasználói felület gyökérkonténere.
     */
    private final VBox view;
    /**
     * A hibaüzeneteket megjelenítő címke.
     */
    private final cliens client;
    /**
     * A szavazás azonosítója.
     */
    private final int pollId;

    /**
     * Konstruktor, amely inicializálja a szavazás eredményeinek betöltését megjelenítő oldalt.
     *
     * @param pollId a szavazás azonosítója
     * @param client a kliens objektum, amely kezeli a szerverrel való kommunikációt
     */
    public ResultLoadingPage(int pollId, cliens client) {
        this.pollId = pollId;
        this.client = client;
        client.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label loadingLabel = new Label("Várakozás eredményre...");
        loadingLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button homeButton = new Button("Kezdőoldalra");
        homeButton.setOnAction(e -> {
            HomePage page = new HomePage();
            Scene scene = new Scene(page.getView(), 600, 400);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Mentimeter - Kezdőoldal");
            App.getPrimaryStage().setScene(scene);
        });

        view.getChildren().addAll(loadingLabel, homeButton);
        System.out.println("LoadingPage konstruktor végén view gyermekek: " + view.getChildren());

        JsonObject request = new JsonObject();
        request.addProperty("action", "get_poll_results");
        request.addProperty("pollId", pollId);
        client.sendRequest(request);
        System.out.println("Küldött kérés: " + request);
    }
    /**
     * Visszaadja az oldal grafikus felületét (VBox).
     *
     * @return a VBox típusú nézet
     */

    public VBox getView() {
        return view;
    }
    /**
     * Eseménykezelő, amely kezeli a szerver válaszait.
     *
     * @param message a szerver válasza
     */

    public void handleServerMessage(JsonObject message) {
        System.out.println("LoadingPage: Kapott szerver válasz: " + message);
        if (message == null || !message.has("action")) {
            Platform.runLater(() -> showError("Hibás szerver válasz: hiányzó 'action' kulcs."));
            return;
        }

        String action = message.get("action").getAsString();
        if (action.equals("get_poll_results")) {
            Platform.runLater(() -> {
                System.out.println("LoadingPage: get_poll_results feldolgozás, status: " + message.get("status"));
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    PollResultsPage page = new PollResultsPage(pollId, client);
                    Scene scene = new Scene(page.getView(), 600, 400);
                    scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                    App.getPrimaryStage().setTitle("Szavazás Eredményei");
                    App.getPrimaryStage().setScene(scene);
                    page.handleServerMessage(message);
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Eredmények lekérése sikertelen.";
                    showError(errorMessage);
                }
            });
        }
    }
    /**
     * Hibát jelenít meg az oldalon.
     *
     * @param message a megjelenítendő hibaüzenet
     */

    public void showError(String message) {
        Label errorLabel = new Label(message);
        errorLabel.getStyleClass().add("error-label");
        view.getChildren().add(errorLabel);
        System.out.println("LoadingPage: Hibaüzenet megjelenítve: " + message);
    }
}