package hu.ppke.itk.tonyo.frontend;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import javafx.application.Platform;

/**
 * A {@code cliens} osztály a szavazási rendszer kliensoldali szerverkommunikációját kezeli.
 * Csatlakozik a szerverhez, küld és fogad JSON üzeneteket, valamint továbbítja a szerver
 * üzeneteit az aktuális JavaFX oldalnak. A szerverüzenetek aszinkron feldolgozására külön
 * szálat használ, és biztosítja a kapcsolat megfelelő lezárását.
 */
public class cliens {

    /** A szerverhez való kapcsolatot biztosító socket. */
    private Socket socket;

    /** A szerverről érkező üzeneteket olvasó BufferedReader. */
    private BufferedReader in;

    /** A szerverre küldött üzeneteket író PrintWriter. */
    private PrintWriter out;

    /** JSON üzenetek serializálására és deserializálására szolgáló Gson objektum. */
    private final Gson gson = new Gson();

    /** Az aktuális JavaFX oldal, amely a szerverüzeneteket kezeli. */
    private Object activePage;

    /** A bejelentkezett felhasználó azonosítója (-1, ha nincs bejelentkezve). */
    private int userId = -1;

    /**
     * Beállítja az aktuális JavaFX oldalt, amely a szerverüzeneteket fogadja.
     *
     * @param page az aktuális oldal objektuma
     */
    public void setActivePage(Object page) {
        this.activePage = page;
    }

    /**
     * Visszaadja a bejelentkezett felhasználó azonosítóját.
     *
     * @return a felhasználó azonosítója (-1, ha nincs bejelentkezve)
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Beállítja a bejelentkezett felhasználó azonosítóját, és naplózza a változást.
     *
     * @param userId a felhasználó azonosítója
     */
    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("userId: " + userId);
    }

    /**
     * Csatlakozik a megadott szerverhez a megadott hoszt és port használatával.
     * Inicializálja a bemeneti és kimeneti streameket, és elindít egy külön szálat
     * a szerverüzenetek aszinkron olvasására.
     *
     * @param host a szerver hosztneve vagy IP-címe (pl. "localhost")
     * @param port a szerver portja (pl. 42069)
     * @throws IOException ha a kapcsolat létrehozása során hiba történik
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Üzenet érkezett a szervertől: " + message);
                    JsonObject json = gson.fromJson(message, JsonObject.class);
                    Platform.runLater(() -> handleServerMessage(json));
                }
            } catch (IOException e) {
                Platform.runLater(() -> showError("Kapcsolat megszakadt: " + e.getMessage()));
            } finally {
                disconnect();
            }
        }).start();
    }

    /**
     * JSON kérést küld a szervernek, ha a kapcsolat aktív.
     *
     * @param request a küldendő JSON kérés
     */
    public void sendRequest(JsonObject request) {
        if (out != null) {
            out.println(gson.toJson(request));
            System.out.println("Küldött kérés: " + gson.toJson(request));
        }
    }

    /**
     * Lezárja a szerverrel való kapcsolatot, beleértve a socketet, a bemeneti és
     * kimeneti streameket, valamint visszaállítja a felhasználó azonosítóját.
     */
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            userId = -1;
        } catch (IOException e) {
            showError("Hiba a kapcsolat lezárásakor: " + e.getMessage());
        }
    }

    /**
     * Kezeli a szerverről érkező JSON üzeneteket, és továbbítja őket az aktuális
     * JavaFX oldal megfelelő kezelő metódusához. Az üzenetfeldolgozás a JavaFX
     * alkalmazási szálon történik.
     *
     * @param message a szerverről érkezett JSON üzenet
     */
    private void handleServerMessage(JsonObject message) {
        if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.LoginPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.LoginPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.RegisterPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.RegisterPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.ConnectPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.ConnectPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.HomePage) {
            ((hu.ppke.itk.tonyo.frontend.pages.HomePage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.CreatePollPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.CreatePollPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.JoinPollPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.JoinPollPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.VotePage) {
            ((hu.ppke.itk.tonyo.frontend.pages.VotePage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.ResultPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.ResultPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.ManagePollPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.ManagePollPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.PollResultsPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.PollResultsPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.LoadingPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.LoadingPage) activePage).handleServerMessage(message);
        } else {
            System.out.println("Ismeretlen activePage típus: " + (activePage != null ? activePage.getClass().getSimpleName() : "null"));
        }
    }

    /**
     * Megjeleníti a megadott hibaüzenetet az aktuális JavaFX oldalon, ha az támogatja
     * a {@code showError} metódust. Ha a metódus nem létezik, a hibát a konzolra írja.
     *
     * @param message a megjelenítendő hibaüzenet
     */
    private void showError(String message) {
        if (activePage != null) {
            try {
                activePage.getClass().getMethod("showError", String.class).invoke(activePage, message);
            } catch (Exception e) {
                System.err.println("Hiba az error üzenet megjelenítésekor: " + e.getMessage());
            }
        }
    }
}