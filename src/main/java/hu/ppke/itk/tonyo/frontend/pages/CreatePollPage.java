package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.cliens;
import hu.ppke.itk.tonyo.frontend.App;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * A {@code CreatePollPage} osztály egy JavaFX alapú felhasználói felületet biztosít új szavazások
 * létrehozására a szavazási rendszer kliensoldali alkalmazásában. Lehetővé teszi a szavazás címének,
 * kérdésének, típusának és opcióinak megadását, valamint a szerverrel való kommunikációt a szavazás
 * létrehozásához. Kezeli a szerver válaszait és a hibajelzéseket.
 */
public class CreatePollPage {

    /** A felhasználói felület gyökérkonténere. */
    private final VBox view;

    /** A szavazás címét tartalmazó szövegmező. */
    private final TextField titleField;

    /** A szavazás kérdését tartalmazó szövegmező. */
    private final TextField questionField;

    /** A szavazás típusát kiválasztó legördülő menü. */
    private final ComboBox<String> typeCombo;

    /** A többválasztós opciókat tartalmazó konténer. */
    private final VBox optionsBox;

    /** A hibaüzeneteket megjelenítő címke. */
    private final Label errorLabel;

    /** A szerverrel való kommunikációt kezelő kliens objektum. */
    private final cliens Cliens;

    /**
     * Konstruktor, amely inicializálja a szavazás létrehozási oldalt és a felhasználói felületet.
     * Beállítja a kliens objektumot és az aktuális oldalt a szerverüzenetek kezelésére.
     */
    public CreatePollPage() {
        Cliens = App.getCliens();
        Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Új szavazás létrehozása");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        titleField = new TextField();
        titleField.setPromptText("Szavazás címe");

        questionField = new TextField();
        questionField.setPromptText("Kérdés");

        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("TOBBVALASZTOS", "SZO_FELHO", "SKALA");
        typeCombo.setPromptText("Szavazás típusa");
        typeCombo.setOnAction(e -> updateOptionsBox());

        optionsBox = new VBox(10);
        optionsBox.setAlignment(Pos.CENTER);

        Button addOptionButton = new Button("Opció hozzáadása");
        addOptionButton.setOnAction(e -> addOptionField());

        Button createButton = new Button("Szavazás létrehozása");
        createButton.setOnAction(e -> createPoll());

        Button backButton = new Button("Vissza");
        backButton.setOnAction(e -> goBackToMainMenu());

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, titleField, questionField, typeCombo, optionsBox, addOptionButton, createButton, backButton, errorLabel);
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
     * Frissíti az opciókat tartalmazó konténert a kiválasztott szavazástípus alapján.
     * Többválasztós típus esetén alapértelmezésként két opciómezőt ad hozzá.
     */
    private void updateOptionsBox() {
        optionsBox.getChildren().clear();
        if (typeCombo.getValue() != null && typeCombo.getValue().equals("TOBBVALASZTOS")) {
            addOptionField();
            addOptionField();
        }
    }

    /**
     * Új opciómezőt ad hozzá a többválasztós szavazás opcióinak konténeréhez.
     * Az opciómező mellett egy törlés gombot is biztosít.
     */
    private void addOptionField() {
        HBox optionRow = new HBox(10);
        TextField optionField = new TextField();
        optionField.setPromptText("Opció szövege");
        Button removeButton = new Button("Törlés");
        removeButton.setOnAction(e -> optionsBox.getChildren().remove(optionRow));
        optionRow.getChildren().addAll(optionField, removeButton);
        optionsBox.getChildren().add(optionRow);
    }

    /**
     * Létrehozza a szavazást a megadott adatok alapján, és elküldi a kérést a szervernek.
     * Validálja a bevitt adatokat, és hiba esetén üzenetet jelenít meg.
     */
    private void createPoll() {
        String title = titleField.getText().trim();
        String question = questionField.getText().trim();
        String type = typeCombo.getValue();

        if (title.isEmpty() || question.isEmpty() || type == null) {
            showError("Kérlek, töltsd ki a cím, kérdés és típus mezőket!");
            return;
        }

        JsonObject request = new JsonObject();
        request.addProperty("action", "create_poll");
        request.addProperty("title", title);
        request.addProperty("question", question);
        request.addProperty("type", type);

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
                showError("Legalább két nem üres opció szükséges többválasztós szavazáshoz!");
                return;
            }
            settings.add("options", options);
        } else if (type.equals("SKALA")) {
            settings.addProperty("min", 0);
            settings.addProperty("max", 10);
        }

        request.add("settings", settings);
        Cliens.sendRequest(request);
    }

    /**
     * Visszatér a főmenübe, létrehozva egy új {@code HomePage} oldalt és beállítva azt az elsődleges ablakban.
     */
    private void goBackToMainMenu() {
        HomePage page = new HomePage();
        Scene scene = new Scene(page.getView(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Főmenü");
        App.getPrimaryStage().setScene(scene);
    }

    /**
     * Kezeli a szerverről érkező JSON üzeneteket. Ha a válasz a szavazás létrehozására vonatkozik,
     * megjeleníti a sikeres létrehozás üzenetét vagy a hibát, és sikeres esetben visszatér a főmenübe.
     *
     * @param message a szerverről érkezett JSON üzenet
     */
    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        if (message.get("action").getAsString().equals("create_poll")) {
            if (message.has("status") && message.get("status").getAsString().equals("success")) {
                showError("Szavazás sikeresen létrehozva! Csatlakozási kód: " + message.get("joinCode").getAsString());
                goBackToMainMenu();
            } else {
                String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
                showError(errorMessage);
            }
        }
    }

    /**
     * Megjeleníti a megadott hibaüzenetet a felhasználói felületen.
     *
     * @param message a megjelenítendő hibaüzenet
     */
    public void showError(String message) {
        errorLabel.setText(message);
    }
}