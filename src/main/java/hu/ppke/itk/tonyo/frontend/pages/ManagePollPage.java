package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.cliens;
import hu.ppke.itk.tonyo.frontend.App;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ManagePollPage {
    private final VBox view;
    private final ListView<HBox> pollsListView;
    private final Label errorLabel;
    private final cliens Cliens;

    public ManagePollPage() {
        Cliens = App.getCliens();
        Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Szavazások kezelése");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Button listPollsButton = new Button("Saját szavazások listázása");
        listPollsButton.setOnAction(e -> listMyPolls());

        Button backButton = new Button("Vissza a főmenübe");
        backButton.setOnAction(e -> goBackToMainMenu());

        pollsListView = new ListView<>();
        pollsListView.setPrefSize(600, 400);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, listPollsButton, pollsListView, backButton, errorLabel);

        // Kezdetben listázzuk a szavazásokat
        listMyPolls();
    }

    public VBox getView() {
        return view;
    }

    private void listMyPolls() {
        JsonObject request = new JsonObject();
        request.addProperty("action", "list_my_polls");
        Cliens.sendRequest(request);
    }

    private void goBackToMainMenu() {
        HomePage page = new HomePage();
        Scene scene = new Scene(page.getView(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Főmenü");
        App.getPrimaryStage().setScene(scene);
    }

    private void updatePollStatus(int pollId, String newStatus) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "update_poll_status");
        request.addProperty("pollId", pollId);
        request.addProperty("newStatus", newStatus);
        Cliens.sendRequest(request);
    }

    private void resetPollData(int pollId) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "reset_poll_data");
        request.addProperty("pollId", pollId);
        Cliens.sendRequest(request);
    }

    private void editPoll(int pollId, String title, String question, String type, JsonObject settings) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "edit_poll");
        request.addProperty("pollId", pollId);
        request.addProperty("title", title);
        request.addProperty("question", question);
        request.addProperty("type", type);
        request.add("settings", settings);
        Cliens.sendRequest(request);
    }

    private void showPollDetails(JsonObject pollObj) {
        int pollId = pollObj.get("pollId").getAsInt();
        String title = pollObj.get("title").getAsString();
        String question = pollObj.get("question").getAsString();
        String type = pollObj.get("type").getAsString();
        String joinCode = pollObj.get("joinCode").getAsString();
        String status = pollObj.get("status").getAsString();
        JsonObject settings = pollObj.has("settings") && !pollObj.get("settings").isJsonNull() ?
                pollObj.get("settings").getAsJsonObject() : new JsonObject();

        VBox detailsView = new VBox(10);
        detailsView.setAlignment(Pos.CENTER);
        detailsView.setPadding(new Insets(20));

        Label detailsLabel = new Label("Szavazás részletei");
        detailsLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label titleLabel = new Label("Cím: " + title);
        Label questionLabel = new Label("Kérdés: " + question);
        Label typeLabel = new Label("Típus: " + type);
        Label joinCodeLabel = new Label("Csatlakozási kód: " + joinCode);
        Label statusLabel = new Label("Állapot: " + status);

        TextArea settingsArea = new TextArea();
        settingsArea.setEditable(false);
        settingsArea.setPrefHeight(100);
        if (type.equals("TOBBVALASZTOS") && settings.has("options")) {
            JsonArray options = settings.get("options").getAsJsonArray();
            StringBuilder optionsText = new StringBuilder("Opciók:\n");
            options.forEach(opt -> {
                JsonObject option = opt.getAsJsonObject();
                optionsText.append("- ").append(option.get("text").getAsString()).append(" (ID: ").append(option.get("id").getAsInt()).append(")\n");
            });
            settingsArea.setText(optionsText.toString());
        } else if (type.equals("SKALA") && settings.has("min") && settings.has("max")) {
            settingsArea.setText("Skála: " + settings.get("min").getAsInt() + " - " + settings.get("max").getAsInt());
        } else {
            settingsArea.setText("Nincsenek specifikus beállítások");
        }

        Button backButton = new Button("Vissza");
        backButton.setOnAction(e -> {
            ManagePollPage newPage = new ManagePollPage();
            Scene scene = new Scene(newPage.getView(), 600, 400);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Szavazások kezelése");
            App.getPrimaryStage().setScene(scene);
        });

        detailsView.getChildren().addAll(detailsLabel, titleLabel, questionLabel, typeLabel, joinCodeLabel, statusLabel, settingsArea, backButton);

        Scene detailsScene = new Scene(detailsView, 600, 400);
        detailsScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Szavazás részletei");
        App.getPrimaryStage().setScene(detailsScene);
    }

    private void showEditPollForm(JsonObject pollObj) {
        int pollId = pollObj.get("pollId").getAsInt();
        String currentTitle = pollObj.get("title").getAsString();
        String currentQuestion = pollObj.get("question").getAsString();
        String currentType = pollObj.get("type").getAsString();
        JsonObject currentSettings = pollObj.has("settings") ? pollObj.get("settings").getAsJsonObject() : new JsonObject();

        VBox editView = new VBox(10);
        editView.setAlignment(Pos.CENTER);
        editView.setPadding(new Insets(20));

        Label editLabel = new Label("Szavazás szerkesztése");
        editLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        TextField titleField = new TextField(currentTitle);
        titleField.setPromptText("Szavazás címe");

        TextField questionField = new TextField(currentQuestion);
        questionField.setPromptText("Kérdés");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("TOBBVALASZTOS", "SZO_FELHO", "SKALA"));
        typeCombo.setValue(currentType);

        VBox optionsBox = new VBox(10);
        Button addOptionButton = new Button("Opció hozzáadása");
        addOptionButton.setOnAction(e -> {
            HBox optionRow = new HBox(10);
            TextField optionField = new TextField();
            optionField.setPromptText("Opció szövege");
            Button removeButton = new Button("Törlés");
            removeButton.setOnAction(re -> optionsBox.getChildren().remove(optionRow));
            optionRow.getChildren().addAll(optionField, removeButton);
            optionsBox.getChildren().add(optionRow);
        });

        if (currentType.equals("TOBBVALASZTOS") && currentSettings.has("options")) {
            JsonArray options = currentSettings.get("options").getAsJsonArray();
            options.forEach(opt -> {
                JsonObject option = opt.getAsJsonObject();
                HBox optionRow = new HBox(10);
                TextField optionField = new TextField(option.get("text").getAsString());
                Button removeButton = new Button("Törlés");
                removeButton.setOnAction(e -> optionsBox.getChildren().remove(optionRow));
                optionRow.getChildren().addAll(optionField, removeButton);
                optionsBox.getChildren().add(optionRow);
            });
        }

        typeCombo.setOnAction(e -> {
            optionsBox.getChildren().clear();
            if (typeCombo.getValue().equals("TOBBVALASZTOS")) {
                addOptionButton.fire();
                addOptionButton.fire();
            }
        });

        Button saveButton = new Button("Mentés");
        saveButton.setOnAction(e -> {
            String title = titleField.getText().trim();
            String question = questionField.getText().trim();
            String type = typeCombo.getValue();

            if (title.isEmpty() || question.isEmpty() || type == null) {
                showError("Kérlek, töltsd ki a cím, kérdés és típus mezőket!");
                return;
            }

            JsonObject settings = new JsonObject();
            if (type.equals("TOBBVALASZTOS")) {
                JsonArray options = new JsonArray();
                int validOptions = 0;
                for (int i = 0; i < optionsBox.getChildren().size(); i++) {
                    HBox optionRow = (HBox) optionsBox.getChildren().get(i);
                    TextField optionField = (TextField) optionRow.getChildren().get(0);
                    String optionText = optionField.getText().trim();
                    if (!optionText.isEmpty()) {
                        JsonObject option = new JsonObject();
                        option.addProperty("id", validOptions + 1);
                        option.addProperty("text", optionText);
                        options.add(option);
                        validOptions++;
                    }
                }
                if (validOptions < 2) {
                    showError("Legalább két nem üres opció szükséges!");
                    return;
                }
                settings.add("options", options);
            } else if (type.equals("SKALA")) {
                settings.addProperty("min", 0);
                settings.addProperty("max", 10);
            }

            editPoll(pollId, title, question, type, settings);
            ManagePollPage newPage = new ManagePollPage();
            Scene scene = new Scene(newPage.getView(), 600, 400);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Szavazások kezelése");
            App.getPrimaryStage().setScene(scene);
        });

        Button cancelButton = new Button("Mégse");
        cancelButton.setOnAction(e -> {
            ManagePollPage newPage = new ManagePollPage();
            Scene scene = new Scene(newPage.getView(), 600, 400);
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            App.getPrimaryStage().setTitle("Szavazások kezelése");
            App.getPrimaryStage().setScene(scene);
        });

        editView.getChildren().addAll(editLabel, titleField, questionField, typeCombo, optionsBox, addOptionButton, saveButton, cancelButton);

        Scene editScene = new Scene(editView, 600, 400);
        editScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Szavazás szerkesztése");
        App.getPrimaryStage().setScene(editScene);
    }

    private void openControlPanel(int pollId, String title, String currentStatus) {
        PollControlPage controlPage = new PollControlPage(pollId, title, currentStatus, Cliens);
        Scene scene = new Scene(controlPage.getView(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Szavazás kezelőfelület");
        App.getPrimaryStage().setScene(scene);
    }

    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        String action = message.get("action").getAsString();
        switch (action) {
            case "list_my_polls":
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    pollsListView.getItems().clear();
                    JsonArray polls = message.get("polls").getAsJsonArray();
                    polls.forEach(poll -> {
                        JsonObject pollObj = poll.getAsJsonObject();
                        int pollId = pollObj.get("pollId").getAsInt();
                        String title = pollObj.get("title").getAsString();
                        String question = pollObj.get("question").getAsString();
                        String type = pollObj.get("type").getAsString();
                        String joinCode = pollObj.get("joinCode").getAsString();
                        String status = pollObj.get("status").getAsString();

                        HBox pollBox = new HBox(10);
                        pollBox.setAlignment(Pos.CENTER_LEFT);
                        Label pollLabel = new Label(String.format("ID: %d, Cím: %s, Kérdés: %s, Típus: %s, Kód: %s, Állapot: %s",
                                pollId, title, question, type, joinCode, status));
                        Button detailsButton = new Button("Részletek");
                        detailsButton.setOnAction(e -> showPollDetails(pollObj));
                        Button resetButton = new Button("Adatok resetelése");
                        resetButton.setOnAction(e -> resetPollData(pollId));
                        Button editButton = new Button("Szerkesztés");
                        editButton.setOnAction(e -> showEditPollForm(pollObj));
                        Button controlButton = new Button("Kezelőfelület");
                        controlButton.setOnAction(e -> openControlPanel(pollId, title, status));
                        Button resultsButton = new Button("Eredmények megtekintése"); // Új gomb
                        resultsButton.setOnAction(e -> {
                            PollResultsPage page = new PollResultsPage(pollId, Cliens);
                            Scene scene = new Scene(page.getView(), 600, 400);
                            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                            App.getPrimaryStage().setTitle("Szavazás Eredményei");
                            App.getPrimaryStage().setScene(scene);
                        });
                        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("NYITOTT", "SZAVAZAS", "EREDMENY", "LEZART"));
                        statusCombo.setValue(status);
                        statusCombo.setOnAction(e -> updatePollStatus(pollId, statusCombo.getValue()));
                        pollBox.getChildren().addAll(pollLabel, detailsButton, resetButton, editButton, controlButton, resultsButton, statusCombo);
                        pollsListView.getItems().add(pollBox);
                    });
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
                    showError(errorMessage);
                }
                break;
            case "update_poll_status":
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    showError("Szavazás állapota frissítve!");
                    listMyPolls();
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
                    showError(errorMessage);
                }
                break;
            case "reset_poll_data":
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    showError("Szavazás adatai resetelve!");
                    listMyPolls();
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
                    showError(errorMessage);
                }
                break;
            case "edit_poll":
                if (message.has("status") && message.get("status").getAsString().equals("success")) {
                    showError("Szavazás sikeresen szerkesztve!");
                    listMyPolls();
                } else {
                    String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
                    showError(errorMessage);
                }
                break;
        }
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }
}