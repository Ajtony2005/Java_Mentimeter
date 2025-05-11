package hu.ppke.itk.tonyo.frontend.pages;

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
 * A bejelentkező oldal osztálya.
 *
 * Ez az osztály megjeleníti a felhasználónév és jelszó mezőket,
 * valamint kezeli a bejelentkezési és regisztrációs folyamatot.
 */

public class LoginPage {
    /**
     * A teljes nézet (VBox), amely tartalmazza az összes komponenst.
     */
    private final VBox view;
    /**
     * Szövegmező a felhasználónév beírásához.
     */
    private final TextField usernameField;
    /**
     * Jelszómező a jelszó beírásához.
     */
    private final PasswordField passwordField;
    /**
     * Label, amely hibaüzeneteket jelenít meg.
     */
    private final Label errorLabel;
    /**
     * A kliens objektum, amely kezeli a szerverrel való kommunikációt.
     */
    private final cliens Cliens;
    /**
     * Létrehozza a bejelentkező oldalt és inicializálja a nézetet.
     */
    public LoginPage() {
        Cliens = App.getCliens();
        Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Bejelentkezés");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        HBox usernameBox = new HBox(10);
        usernameBox.setAlignment(Pos.CENTER);
        Label usernameLabel = new Label("Felhasználónév:");
        usernameField = new TextField();
        usernameField.setPromptText("Felhasználónév");
        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        HBox passwordBox = new HBox(10);
        passwordBox.setAlignment(Pos.CENTER);
        Label passwordLabel = new Label("Jelszó:");
        passwordField = new PasswordField();
        passwordField.setPromptText("Jelszó");
        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        Button loginButton = new Button("Bejelentkezés");
        loginButton.setOnAction(e -> login());
        Button registerButton = new Button("Regisztráció");
        registerButton.setOnAction(e -> openRegister());
        buttonBox.getChildren().addAll(loginButton, registerButton);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, usernameBox, passwordBox, buttonBox, errorLabel);
    }
    /**
     * Visszaadja az oldal teljes nézetét, amely megjeleníthető a jelenetben.
     *
     * @return a VBox nézet
     */
    public VBox getView() {
        return view;
    }
    /**
     * Kezeli a bejelentkezési gomb megnyomását.
     * Ellenőrzi, hogy minden mező ki van-e töltve, majd küldi a szervernek a login kérést.
     */
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        if (username.isEmpty() || password.isEmpty()) {
            showError("Kérlek, töltsd ki az összes mezőt!");
            return;
        }
        JsonObject request = new JsonObject();
        request.addProperty("action", "login");
        request.addProperty("username", usernameField.getText());
        request.addProperty("password", passwordField.getText());
        Cliens.sendRequest(request);
    }
    /**
     * Megnyitja a regisztrációs oldalt új oldalon.
     */
    private void openRegister() {
        RegisterPage page = new RegisterPage();
        Scene scene = new Scene(page.getView(), 400, 300);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        App.getPrimaryStage().setTitle("Mentimeter - Regisztráció");
        App.getPrimaryStage().setScene(scene);
    }
    /**
     * Kezeli a szervertől érkező üzeneteket.
     * A login válaszok feldolgozása történik itt.
     *
     * @param message a szervertől kapott JSON objektum
     */
    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("action")) {
            showError("Hibás szerver válasz: hiányzó 'action' kulcs.");
            return;
        }

        if (message.get("action").getAsString().equals("login")) {
            if (message.has("status") && message.get("status").getAsString().equals("success")) {
                int userId = message.has("userId") ? message.get("userId").getAsInt() : -1;
                Cliens.setUserId(userId);
                HomePage page = new HomePage();
                Scene scene = new Scene(page.getView(), 600, 400);
                scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
                App.getPrimaryStage().setTitle("Mentimeter - Főmenü");
                App.getPrimaryStage().setScene(scene);
            } else {
                String errorMessage = message.has("message") ? message.get("message").getAsString() : "Ismeretlen hiba történt.";
                showError(errorMessage);
            }
        }
    }
    /**
     * Hibaüzenetet jelenít meg az oldalon.
     *
     * @param message a megjelenítendő hibaüzenet
     */
    public void showError(String message) {
        errorLabel.setText(message);
    }
}