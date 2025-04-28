package hu.ppke.itk.tonyo.frontend;

import com.google.gson.Gson;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class cliens extends Application {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 42069;

    private final Gson gson = new Gson();

    @Override
    public void start(Stage primaryStage) {
        TextField usernameField = new TextField();
        usernameField.setPromptText("Felhasználónév");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Jelszó");

        Button loginButton = new Button("Bejelentkezés");
        Label statusLabel = new Label();

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Töltsd ki mindkét mezőt!");
                return;
            }

            String serverResponse = sendLogin(username, password);
            if (serverResponse != null) {
                // Kiírjuk a szerver válaszát a státusz mezőbe
                statusLabel.setText("Szerver válasz: " + serverResponse);
            } else {
                statusLabel.setText("Hiba történt a kapcsolódáskor.");
            }
        });

        VBox root = new VBox(10, usernameField, passwordField, loginButton, statusLabel);
        root.setPadding(new Insets(20));

        primaryStage.setScene(new Scene(root, 300, 180));
        primaryStage.setTitle("JavaFX Socket Login JSON");
        primaryStage.show();
    }

    private String sendLogin(String username, String password) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Map<String, String> loginData = new HashMap<>();
            loginData.put("action", "login");          // kötelező az action kulcs!
            loginData.put("username", username);
            loginData.put("password", password);

            String json = gson.toJson(loginData);
            writer.println(json);

            // Várjuk a szerver válaszát, pl: {"errorCode":0,"errorMessage":"Sikeres bejelentkezés"}
            String responseJson = reader.readLine();
            return responseJson;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
