package hu.ppke.itk.tonyo.backend;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@code ClientHandler} osztály egy kliens kezelésére szolgál egy többklienses szavazási rendszerben.
 * Minden kliens külön szálon fut, és a szerverrel TCP kapcsolaton keresztül kommunikál. Az osztály
 * felelős a klienssel való kommunikációért, a szavazásokhoz való csatlakozásért és kilépésért,
 * valamint az üzenetek továbbításáért a szavazásokhoz csatlakozott klienseknek.
 */
public class ClientHandler implements Runnable {

    /**
     * Szavazásokhoz csatlakozott kliensek tárolására szolgáló szálbiztos térkép.
     * Kulcs: szavazás azonosítója ({@code pollId}), érték: a csatlakozott kliensek listája.
     */
    private static final ConcurrentHashMap<Integer, List<ClientHandler>> pollClients = new ConcurrentHashMap<>();

    /** A klienshez tartozó socket, amelyen keresztül a kommunikáció zajlik. */
    private final Socket clientSocket;

    /** Az adatbázis neve, amelyet a kliens kéréseinek feldolgozásához használ. */
    private final String dbName;

    /** JSON szerializációhoz és deszerializációhoz használt Gson objektum. */
    private final Gson gson = new Gson();

    /** A kliens egyedi azonosítója. Alapértelmezés: -1 (nincs beállítva). */
    private int userId = -1;

    /** Az aktuális szavazás azonosítója, amelyhez a kliens csatlakozott. Alapértelmezés: -1 (nincs csatlakozva). */
    private int currentPollId = -1;

    /**
     * Konstruktor, amely inicializálja a kliens kezelőt.
     *
     * @param clientSocket a klienshez tartozó socket
     * @param dbName az adatbázis neve
     */
    public ClientHandler(Socket clientSocket, String dbName) {
        this.clientSocket = clientSocket;
        this.dbName = dbName;
    }

    /**
     * Visszaadja a kliens egyedi azonosítóját.
     *
     * @return a kliens azonosítója
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Beállítja a kliens egyedi azonosítóját.
     *
     * @param userId a kliens azonosítója
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Csatlakoztatja a klienst egy adott szavazáshoz.
     *
     * @param pollId a szavazás azonosítója
     */
    public void joinPoll(int pollId) {
        this.currentPollId = pollId;
        pollClients.computeIfAbsent(pollId, k -> new CopyOnWriteArrayList<>()).add(this);
        System.out.println("Kliens csatlakozott a szavazáshoz: pollId=" + pollId);
    }

    /**
     * Kilépteti a klienst egy adott szavazásból.
     *
     * @param pollId a szavazás azonosítója
     */
    public void leavePoll(int pollId) {
        List<ClientHandler> clients = pollClients.get(pollId);
        if (clients != null) {
            synchronized (clients) {
                clients.remove(this);
                if (clients.isEmpty()) {
                    pollClients.remove(pollId);
                }
            }
        }
        this.currentPollId = -1;
        System.out.println("Kliens kilépett a szavazásból: pollId=" + pollId);
    }

    /**
     * Üzenetet küld az adott szavazáshoz csatlakozott összes kliensnek.
     *
     * @param pollId a szavazás azonosítója
     * @param message a küldendő JSON üzenet
     */
    public static void broadcastToPoll(int pollId, JsonObject message) {
        List<ClientHandler> clients = pollClients.get(pollId);
        if (clients != null) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    try {
                        PrintWriter out = new PrintWriter(client.clientSocket.getOutputStream(), true);
                        out.println(client.gson.toJson(message));
                        System.out.println("Üzenet elküldve kliensnek: pollId=" + pollId + ", üzenet=" + message);
                    } catch (IOException e) {
                        System.err.println("Üzenet küldése sikertelen: " + e.getMessage());
                        client.leavePoll(pollId);
                    }
                }
            }
        }
    }

    /**
     * A kliens szál futási logikája. Feldolgozza a kliens JSON kéréseit, és válaszokat küld.
     */
    @Override
    public void run() {
        try {
            System.out.println("Kliens kezelése szálon: " + Thread.currentThread().getName());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            String requestJson;
            while ((requestJson = in.readLine()) != null) {
                System.out.println("Kapott kérés: " + requestJson);

                JsonObject request = gson.fromJson(requestJson, JsonObject.class);
                JsonObject response = RequestProcessor.processRequest(dbName, request, this);

                out.println(gson.toJson(response));
            }
        } catch (IOException e) {
            System.err.println("Kliens kezelése sikertelen: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
                System.out.println("Kliens kapcsolat lezárva: " + clientSocket.getInetAddress());
            } catch (IOException e) {
                System.err.println("Kliens kapcsolat lezárása sikertelen: " + e.getMessage());
            }
        }
    }
    }
