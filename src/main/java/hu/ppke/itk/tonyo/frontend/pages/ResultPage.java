package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.cliens;
import hu.ppke.itk.tonyo.frontend.App;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * A {@code ResultPage} osztály a szavazás eredményeit megjelenítő oldal megvalósításáért felelős.
 * Az oldal lehetővé teszi a felhasználó számára, hogy megtekintse a szavazás eredményeit különböző típusú kérdésekhez.
 */

public class ResultPage {
    /** A felhasználói felület gyökérkonténere. */
    private final VBox view;
    /** A szavazás eredményeit megjelenítő pane. */
    private final Pane resultsPane;
    /** A hibaüzeneteket megjelenítő címke. */
    private final Label errorLabel;
    /** A szerverrel való kommunikációt kezelő kliens objektum. */
    private final cliens Cliens;
    /**
     * Konstruktor, amely inicializálja az eredményoldalt és a felhasználói felületet.
     * Beállítja a kliens objektumot és az aktuális oldalt a szerverüzenetek kezelésére.
     */

    public ResultPage() {
        Cliens = App.getCliens();
        Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Eredmények");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        resultsPane = new Pane();
        resultsPane.setPrefSize(500, 300);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, resultsPane, errorLabel);
    }
    /**
     * Visszaadja az oldal grafikus felületét (VBox).
     *
     * @return a szavazás eredményeit megjelenítő oldal
     */

    public VBox getView() {
        return view;
    }
    /**
     * Beállítja a szavazás eredményeit a megadott típus és adatok alapján.
     *
     * @param results a szavazás eredményei JSON formátumban
     * @param type    a szavazás típusa (pl. "SZO_FELHO", "TOBBVALASZTOS", "SKALA")
     */

    public void setResultsData(JsonObject results, String type) {
        resultsPane.getChildren().clear();
        Text text = new Text();
        switch (type) {
            case "SZO_FELHO":
                JsonArray words = results.get("words").getAsJsonArray();
                StringBuilder wordCloudText = new StringBuilder("Szófelhő eredmények:\n");
                words.forEach(word -> {
                    JsonObject w = word.getAsJsonObject();
                    wordCloudText.append(w.get("word").getAsString()).append(": ").append(w.get("count").getAsInt()).append("\n");
                });
                text.setText(wordCloudText.toString());
                break;
            case "TOBBVALASZTOS":
                JsonArray options = results.get("options").getAsJsonArray();
                StringBuilder mcText = new StringBuilder("Többválasztós eredmények:\n");
                options.forEach(option -> {
                    JsonObject opt = option.getAsJsonObject();
                    mcText.append(opt.get("option").getAsString()).append(": ").append(opt.get("count").getAsInt()).append("\n");
                });
                text.setText(mcText.toString());
                break;
            case "SKALA":
                text.setText("Skála átlag: " + results.get("average").getAsDouble());
                break;
        }
        resultsPane.getChildren().add(text);
    }
    /**
     * Kezeli a szerverről érkező üzeneteket.
     * Ha a szavazás állapota "SZAVAZAS", akkor átlép a szavazó oldalra.
     *
     * @param message a szervertől érkezett JSON üzenet
     */

    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("event")) {
            showError("Hibás szerver válasz: hiányzó 'event' kulcs.");
            return;
        }

        String event = message.get("event").getAsString();
        if (event.equals("results_update")) {
            String pollType = message.has("pollType") ? message.get("pollType").getAsString() : "";
            setResultsData(message.get("results").getAsJsonObject(), pollType);
        } else if (event.equals("status_update")) {
            String newStatus = message.has("newStatus") ? message.get("newStatus").getAsString() : "ismeretlen";
            showError("Szavazás állapota frissült: " + newStatus);
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