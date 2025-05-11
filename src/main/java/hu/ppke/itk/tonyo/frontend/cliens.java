package hu.ppke.itk.tonyo.frontend;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import javafx.application.Platform;

public class cliens {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Gson gson = new Gson();
    private Object activePage;
    private int userId = -1; // Inicializálás: nincs bejelentkezett felhasználó

    public void setActivePage(Object pages) {
        this.activePage = pages;
        System.out.println("Aktív oldal beállítva: " + (pages != null ? pages.getClass().getSimpleName() : "null"));
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
        System.out.println("userId beállítva: " + userId);
    }

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

    public void sendRequest(JsonObject request) {
        if (out != null) {
            out.println(gson.toJson(request));
            System.out.println("Küldött kérés: " + gson.toJson(request));
        }
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            userId = -1; // Kijelentkezéskor userId visszaállítása
            System.out.println("Kapcsolat lezárva, userId visszaállítva: " + userId);
        } catch (IOException e) {
            showError("Hiba a kapcsolat lezárásakor: " + e.getMessage());
        }
    }

    private void handleServerMessage(JsonObject message) {
        System.out.println("handleServerMessage hívva, activePage: " + (activePage != null ? activePage.getClass().getSimpleName() : "null"));
        if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.LoginPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.LoginPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.RegisterPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.RegisterPage) activePage).handleServerMessage(message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.ConnectPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.ConnectPage) activePage).handleServerMessage(message);
            System.out.println("Továbbítás ConnectPage-nek: " + message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.HomePage) {
            ((hu.ppke.itk.tonyo.frontend.pages.HomePage) activePage).handleServerMessage(message);
            System.out.println("Továbbítás ProfilePage-nek: " + message);
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
            System.out.println("Továbbítás PollResultsPage-nek: " + message);
        } else if (activePage instanceof hu.ppke.itk.tonyo.frontend.pages.LoadingPage) {
            ((hu.ppke.itk.tonyo.frontend.pages.LoadingPage) activePage).handleServerMessage(message);
            System.out.println("Továbbítás LoadingPage-nek: " + message);
        } else {
            System.out.println("Ismeretlen activePage típus: " + (activePage != null ? activePage.getClass().getSimpleName() : "null"));
        }
    }

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