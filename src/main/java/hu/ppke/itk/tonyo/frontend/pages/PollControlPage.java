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

/**
 * A {@code PollControlPage} osztály a szavazás kezelőfelületét valósítja meg.
 * Ez az oldal lehetővé teszi a szavazás állapotának módosítását és a kezelőoldalra való visszalépést.
 */

public class PollControlPage {
    /**
     * A teljes nézet (VBox), amely tartalmazza az összes komponenst.
     */
    private final VBox view;
    /**
     * Címke, amely hibaüzeneteket jelenít meg.
     */
    private final Label errorLabel;
    /**
     * A kliens objektum, amely kezeli a szerverrel való kommunikációt.
     */
    private final cliens Cliens;
    /**
     * A szavazás azonosítója.
     */
    private final int pollId;
    /**
     * A szavazás címe.
     */
    private final String pollTitle;

    /**
     * Konstruktor, amely inicializálja a szavazás kezelőfelületét.
     */
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
    /**
     * Visszaadja az oldal grafikus felületét (VBox).
     *
     * @return a VBox típusú nézet
     */
    public VBox getView() {
        return view;
    }
    /**
     * Frissíti a szavazás állapotát a megadott új állapotra.
     *
     * @param newStatus az új állapot, amelyre frissíteni kell a szavazást
     */

    private void updatePollStatus(String newStatus) {
        JsonObject request = new JsonObject();
        request.addProperty("action", "update_poll_status");
        request.addProperty("pollId", pollId);
        request.addProperty("newStatus", newStatus);
        Cliens.sendRequest(request);
    }
    /**
     * Visszalép a szavazás kezelőoldalára.
     */

    private void goBackToManagePage() {
        ManagePollPage page = new ManagePollPage();
        Scene scene = new Scene(page.getView(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Szavazások kezelése");
        App.getPrimaryStage().setScene(scene);
    }

    /**
     * Kezeli a szerverről érkező üzeneteket.
     * Ha a szavazás állapota frissítve lett, akkor visszalép a kezelőoldalra.
     *
     * @param message a szervertől érkezett JSON üzenet
     */

    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        String action = message.get("action").getAsString();
        if (action.equals("update_poll_status")) {
            if (message.has("status") && message.get("status").getAsString().equals("success")) {
                showError("Szavazás állapota frissítve!");
                goBackToManagePage();
            } else {
                String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
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