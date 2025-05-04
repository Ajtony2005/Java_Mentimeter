package hu.ppke.itk.tonyo.frontend;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerCommunication {

    private static final String SERVER_HOST = System.getenv().getOrDefault("SERVER_HOST", "localhost");
    private static final int SERVER_PORT = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "42069"));
    private final Gson gson = new Gson();

    public String sendRequest(String action, String usernameOrCode, String password) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            socket.setSoTimeout(5000); // 5 másodperces timeout

            Map<String, String> requestData = new HashMap<>();
            requestData.put("action", action);
            if (action.equals("join")) {
                requestData.put("code", usernameOrCode);
            } else if (action.equals("create")) {
                // Nincs szükség username vagy password mezőre
            } else {
                requestData.put("username", usernameOrCode);
                requestData.put("password", password);
            }

            writer.println(gson.toJson(requestData));

            String responseJson = reader.readLine();
            if (responseJson != null) {
                Map<String, Object> response = gson.fromJson(responseJson, Map.class);
                double errorCode = (Double) response.get("errorCode");
                return getErrorMessage(action, (int) errorCode);
            }
            return "Nincs válasz a szervertől.";

        } catch (IOException e) {
            return "Kapcsolódási hiba: " + e.getMessage();
        } catch (Exception e) {
            return "JSON feldolgozási hiba: " + e.getMessage();
        }
    }

    private String getErrorMessage(String action, int errorCode) {
        if (action.equals("login")) {
            switch (errorCode) {
                case 0: return "Kód: 0, Sikeres bejelentkezés";
                case 1: return "Kód: 1, Adatbázis kapcsolódási hiba";
                case 2: return "Kód: 2, Hibás felhasználónév vagy jelszó";
                case 4: return "Kód: 4, Ismeretlen hiba";
                default: return "Kód: " + errorCode + ", Érvénytelen hibakód";
            }
        } else if (action.equals("register")) {
            switch (errorCode) {
                case 0: return "Kód: 0, Sikeres regisztráció";
                case 1: return "Kód: 1, Adatbázis kapcsolódási hiba";
                case 2: return "Kód: 2, Regisztrációs lekérdezés sikertelen";
                case 4: return "Kód: 4, Ismeretlen hiba";
                default: return "Kód: " + errorCode + ", Érvénytelen hibakód";
            }
        } else if (action.equals("join")) {
            switch (errorCode) {
                case 0: return "Kód: 0, Sikeres csatlakozás";
                case 1: return "Kód: 1, Adatbázis kapcsolódási hiba";
                case 2: return "Kód: 2, Érvénytelen csatlakozási kód";
                case 4: return "Kód: 4, Ismeretlen hiba";
                default: return "Kód: " + errorCode + ", Érvénytelen hibakód";
            }
        } else if (action.equals("create")) {
            switch (errorCode) {
                case 0: return "Kód: 0, Sikeres lobby létrehozás";
                case 1: return "Kód: 1, Adatbázis kapcsolódási hiba";
                case 2: return "Kód: 2, Lobby létrehozása sikertelen";
                case 4: return "Kód: 4, Ismeretlen hiba";
                default: return "Kód: " + errorCode + ", Érvénytelen hibakód";
            }
        }
        return "Kód: 4, Ismeretlen művelet";
    }
}