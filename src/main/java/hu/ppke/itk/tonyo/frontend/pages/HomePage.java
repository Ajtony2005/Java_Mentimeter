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

public class HomePage {
    private final VBox view;
    private final ListView<String> pollsListView;
    private final Label errorLabel;
    private final cliens Cliens;

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
            // scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Mentimeter - Kezdőoldal");
            App.getPrimaryStage().setScene(scene);
            Cliens.disconnect();
            try {
                Cliens.connect("localhost", 42069);
                System.out.println("Újracsatlakozás sikeres");
            } catch (Exception ex) {
                showError("Újracsatlakozás sikertelen: " + ex.getMessage());
            }
        });

        pollsListView = new ListView<>();
        pollsListView.setItems(FXCollections.observableArrayList());

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, createPollButton, listPollsButton, joinPollButton, managePollsButton, logoutButton, pollsListView, errorLabel);
        System.out.println("HomePage konstruktor végén view gyermekek: " + view.getChildren());
    }

    public VBox getView() {
        return view;
    }

    private void openCreatePoll() {
        CreatePollPage page = new CreatePollPage();
        Scene scene = new Scene(page.getView(), 600, 400);
        // scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Szavazás létrehozása");
        App.getPrimaryStage().setScene(scene);
    }

    private void openJoinPoll() {
        JoinPollPage page = new JoinPollPage();
        Scene scene = new Scene(page.getView(), 400, 200);
        // scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Csatlakozás szavazáshoz");
        App.getPrimaryStage().setScene(scene);
    }

    private void openManagePolls() {
        ManagePollPage page = new ManagePollPage();
        Scene scene = new Scene(page.getView(), 600, 400);
        // scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Szavazások kezelése");
        App.getPrimaryStage().setScene(scene);
    }

    private void listPolls() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "list_polls");
        Cliens.sendRequest(request);
        System.out.println("Szavazások listázása kérés elküldve: " + request);
    }

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

    public void showError(String message) {
        Platform.runLater(() -> {
            errorLabel.setText(message);
            System.out.println("HomePage: Hibaüzenet megjelenítve: " + message);
        });
    }
}