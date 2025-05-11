package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonArray;
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

public class PollResultsPage {
    private final VBox view;
    private final Label errorLabel;
    private final cliens client;
    private final int pollId;

    public PollResultsPage(int pollId, cliens client) {
        this.pollId = pollId;
        this.client = client;
        client.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Szavazás Eredményei");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        Button backButton = new Button("Vissza a kezelésekre");
        backButton.setOnAction(e -> {
            ManagePollPage page = new ManagePollPage();
            Scene scene = new Scene(page.getView(), 600, 400);
            // scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Szavazások kezelése");
            App.getPrimaryStage().setScene(scene);
        });

        Button homeButton = new Button("Kezdőoldalra");
        homeButton.setOnAction(e -> {
            HomePage page = new HomePage();
            Scene scene = new Scene(page.getView(), 600, 400);
            // scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Mentimeter - Kezdőoldal");
            App.getPrimaryStage().setScene(scene);
        });

        view.getChildren().addAll(titleLabel, errorLabel, backButton, homeButton);
        System.out.println("Konstruktor végén view gyermekek: " + view.getChildren());

        // Eredmények lekérése
        JsonObject request = new JsonObject();
        request.addProperty("action", "get_poll_results");
        request.addProperty("pollId", pollId);
        client.sendRequest(request);
        System.out.println("Küldött kérés: " + request);
    }

    public VBox getView() {
        return view;
    }

    public void handleServerMessage(JsonObject message) {
        System.out.println("PollResultsPage: Kapott szerver válasz: " + message);
        if (message == null || !message.has("action")) {
            Platform.runLater(() -> {
                showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
                System.out.println("Hiba: hiányzó action kulcs");
            });
            return;
        }

        String action = message.get("action").getAsString();
        System.out.println("Action: " + action);
        if (action.equals("get_poll_results")) {
            Platform.runLater(() -> {
                System.out.println("PollResultsPage: get_poll_results feldolgozás kezdete, status: " + message.get("status"));
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    System.out.println("Sikeres válasz, poll: " + message.get("poll"));
                    JsonObject poll = message.getAsJsonObject("poll");
                    String title = poll.get("title").getAsString();
                    String question = poll.get("question").getAsString();
                    String type = poll.get("type").getAsString();
                    JsonObject settings;
                    if (poll.has("settings") && !poll.get("settings").isJsonNull()) {
                        settings = poll.getAsJsonObject("settings");
                    } else {
                        settings = new JsonObject();
                    }
                    JsonArray results = message.getAsJsonArray("results");

                    System.out.println("Poll adatok: title=" + title + ", question=" + question + ", type=" + type);
                    System.out.println("Settings: " + settings);
                    System.out.println("Results: " + results);

                    Label pollInfoLabel = new Label(String.format("Cím: %s\nKérdés: %s\nTípus: %s", title, question, type));
                    pollInfoLabel.setFont(Font.font("Arial", 14));
                    view.getChildren().add(pollInfoLabel);
                    System.out.println("Hozzáadott pollInfoLabel: " + pollInfoLabel.getText());

                    System.out.println("Type ellenőrzés: " + type + ", settings.has(options)=" + settings.has("options"));
                    if (type.equals("TOBBVALASZTOS") && settings.has("options")) {
                        JsonArray options = settings.getAsJsonArray("options");
                        System.out.println("Többválasztós feltétel teljesül, options: " + options);
                        for (int i = 0; i < options.size(); i++) {
                            JsonObject option = options.get(i).getAsJsonObject();
                            int optionId = option.get("id").getAsInt();
                            String optionText = option.get("text").getAsString();
                            int voteCount = 0;

                            for (int j = 0; j < results.size(); j++) {
                                JsonObject result = results.get(j).getAsJsonObject();
                                if (result.get("optionId").getAsInt() == optionId) {
                                    voteCount = result.get("voteCount").getAsInt();
                                    break;
                                }
                            }

                            Label resultLabel = new Label(String.format("%s: %d szavazat", optionText, voteCount));
                            resultLabel.setFont(Font.font("Arial", 12));
                            view.getChildren().add(resultLabel);
                            System.out.println("Hozzáadott resultLabel: " + resultLabel.getText());
                        }
                    } else {
                        showError("Csak többválasztós szavazások eredményei támogatottak jelenleg.");
                        System.out.println("Többválasztós feltétel nem teljesült: type=" + type + ", has options=" + settings.has("options"));
                    }
                    System.out.println("UI frissítve, view gyermekek: " + view.getChildren().size());
                    System.out.println("view tartalma: " + view.getChildren());
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Eredmények lekérése sikertelen.";
                    showError(errorMessage);
                    System.out.println("Hibaüzenet: " + errorMessage);
                }
            });
        }
    }

    public void showError(String message) {
        errorLabel.setText(message);
        System.out.println("Hibaüzenet megjelenítve: " + message);
    }
}