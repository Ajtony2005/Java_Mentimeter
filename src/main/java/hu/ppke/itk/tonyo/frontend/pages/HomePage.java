package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.cliens;
import hu.ppke.itk.tonyo.frontend.App;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * A {@code HomePage} osztály a szavazási rendszer kliensoldali JavaFX alkalmazásának főmenüjét
 * biztosítja. Lehetővé teszi új szavazások létrehozását, meglévő szavazások listázását,
 * szavazásokhoz való csatlakozást, saját szavazások kezelését és a felhasználó kijelentkeztetését.
 * Kezeli a szerverről érkező üzeneteket, és megjeleníti a szavazások listáját vagy hibaüzeneteket.
 */
public class HomePage {

    /** A felhasználói felület gyökérkonténere. */
    private final VBox view;

    /** A szavazások listáját megjelenítő lista nézet. */
    private final ListView<String> pollsListView;

    /** A hibaüzeneteket megjelenítő címke. */
    private final Label errorLabel;

    /** A szerverrel való kommunikációt kezelő kliens objektum. */
    private final cliens Cliens;

    /**
     * Konstruktor, amely inicializálja a főmenü oldalt és a felhasználói felületet.
     * Beállítja a kliens objektumot és az aktuális oldalt a szerverüzenetek kezelésére.
     */
    public HomePage() {
        Cliens = App.getCliens();
        Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Főmenü");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button createPollButton = new Button("Új szavazás létrehozása");
        createPollButton.setOnAction(e -> openCreatePoll());

        Button listPollsButton = new Button("Szavazások listázása");
        listPollsButton.setOnAction(e -> listPolls());

        Button joinPollButton = new Button("Csatlakozás szavazáshoz");
        joinPollButton.setOnAction(e -> openJoinPoll());

        Button managePollsButton = new Button("Saját szavazások kezelése");
        managePollsButton.setOnAction(e -> openManagePolls());

        Button logoutButton = new Button("Kijelentkezés");
        logoutButton.setOnAction(e -> {
            System.out.println("Kijelentkezés gomb megnyomva");
            JsonObject request = new JsonObject();
            request.addProperty("action", "logout");
            Cliens.sendRequest(request);
            System.out.println("Kijelentkezési kérés elküldve: " + request);
            ConnectPage page = new ConnectPage();
            Scene scene = new Scene(page.getView(), 400, 300);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Mentimeter - Kezdőoldal");
            App.getPrimaryStage().setScene(scene);
            Cliens.setUserId(-1); //Javított
           });

        pollsListView = new ListView<>();
        pollsListView.setItems(FXCollections.observableArrayList());

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, createPollButton, listPollsButton, joinPollButton, managePollsButton, logoutButton, pollsListView, errorLabel);
        System.out.println("HomePage konstruktor végén view gyermekek: " + view.getChildren());
    }

    /**
     * Visszaadja a felhasználói felület gyökérkonténerét.
     *
     * @return a felhasználói felület {@code VBox} objektuma
     */
    public VBox getView() {
        return view;
    }

    /**
     * Megnyitja az új szavazás létrehozására szolgáló oldalt.
     */
    private void openCreatePoll() {
        CreatePollPage page = new CreatePollPage();
        Scene scene = new Scene(page.getView(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Szavazás létrehozása");
        App.getPrimaryStage().setScene(scene);
    }

    /**
     * Megnyitja a szavazáshoz csatlakozás oldalát.
     */
    private void openJoinPoll() {
        JoinPollPage page = new JoinPollPage();
        Scene scene = new Scene(page.getView(), 400, 200);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Csatlakozás szavazáshoz");
        App.getPrimaryStage().setScene(scene);
    }

    /**
     * Megnyitja a saját szavazások kezelésére szolgáló oldalt.
     */
    private void openManagePolls() {
        ManagePollPage page = new ManagePollPage();
        Scene scene = new Scene(page.getView(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Szavazások kezelése");
        App.getPrimaryStage().setScene(scene);
    }

    /**
     * Kérést küld a szervernek a felhasználó által létrehozott szavazások listázására.
     */
    private void listPolls() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "list_polls");
        Cliens.sendRequest(request);
        System.out.println("Szavazások listázása kérés elküldve: " + request);
    }

    /**
     * Kezeli a szerverről érkező JSON üzeneteket. A {@code list_polls} akció esetén
     * frissíti a szavazások listáját, a {@code logout} akció esetén megjeleníti
     * a kijelentkezés eredményét.
     *
     * @param message a szerverről érkezett JSON üzenet
     */
    public void handleServerMessage(JsonObject message) {
        System.out.println("HomePage: Kapott szerver válasz: " + message);
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        String action = message.get("action").getAsString();
        if (action.equals("list_polls")) {
            Platform.runLater(() -> {
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    JsonArray polls = message.getAsJsonArray("polls");
                    pollsListView.getItems().clear();
                    polls.forEach(poll -> {
                        JsonObject pollObj = poll.getAsJsonObject();
                        String item = String.format("ID: %d, Cím: %s, Kérdés: %s, Típus: %s, Kód: %s, Állapot: %s",
                                pollObj.get("pollId").getAsInt(),
                                pollObj.get("title").getAsString(),
                                pollObj.get("question").getAsString(),
                                pollObj.get("type").getAsString(),
                                pollObj.get("joinCode").getAsString(),
                                pollObj.get("status").getAsString());
                        pollsListView.getItems().add(item);
                    });
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
                    showError(errorMessage);
                }
            });
        } else if (action.equals("logout")) {
            Platform.runLater(() -> {
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    showError("Kijelentkezés sikeres.");
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Kijelentkezés sikertelen.";
                    showError(errorMessage);
                }
            });
        }
    }

    /**
     * Megjeleníti a megadott hibaüzenetet a felhasználói felületen a JavaFX
     * alkalmazási szálon.
     *
     * @param message a megjelenítendő hibaüzenet
     */
    public void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            System.out.println("HomePage: Hibaüzenet megjelenítve: " + message);
        });
    }
}