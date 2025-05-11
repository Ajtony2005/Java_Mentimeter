package hu.ppke.itk.tonyo.backend;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {
    private static final ConcurrentHashMap<Integer, List<ClientHandler>> pollClients = new ConcurrentHashMap<>();
    private final Socket clientSocket;
    private final String dbName;
    private final Gson gson = new Gson();
    private int userId = -1; //
    private int currentPollId = -1;

    public ClientHandler(Socket clientSocket, String dbName) {
        this.clientSocket = clientSocket;
        this.dbName = dbName;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void joinPoll(int pollId) {
        this.currentPollId = pollId;
        pollClients.computeIfAbsent(pollId, k -> new CopyOnWriteArrayList<>()).add(this);
        System.out.println("Kliens csatlakozott a szavazáshoz: pollId=" + pollId);
    }

    public void leavePoll(int pollId) {
        List<ClientHandler> clients = pollClients.get(pollId);
        if (clients != null) {
            synchronized (clients) {
                clients.remove(this);
                if (clients.isEmpty()) {
                    pollClients.remove(pollId); // Töröljük, ha nincs több kliens
                }
            }
        }
        this.currentPollId = -1;
        System.out.println("Kliens kilépett a szavazásból: pollId=" + pollId);
    }

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
                        client.leavePoll(pollId); // Hibás kliens eltávolítása
                    }
                }
            }
        }
    }

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