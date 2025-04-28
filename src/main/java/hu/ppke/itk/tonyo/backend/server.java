package hu.ppke.itk.tonyo.backend;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class server {
    public static void main(String[] args) {
        int port = 42069;
        Gson gson = new Gson();
        errorCodeLoader errorCodeLoader = new errorCodeLoader("/errorcode.json");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String requestJson = in.readLine();
                System.out.println("Received: " + requestJson);

                JsonObject request = gson.fromJson(requestJson, JsonObject.class);
                String action = request.get("action").getAsString(); // pl. "login"

                JsonObject response = new JsonObject();

                if ("login".equals(action)) {
                    String username = request.get("username").getAsString();
                    String password = request.get("password").getAsString();

                    login loginTask = new login("test.db", username, password);
                    int errorCode = loginTask.call();

                    response.addProperty("errorCode", errorCode);
                    response.addProperty("errorMessage", errorCodeLoader.getMessage("login", errorCode));
                } else {
                    response.addProperty("errorCode", 4); // Unknown error
                    response.addProperty("errorMessage", "Unknown action");
                }


                out.println(gson.toJson(response));


                clientSocket.close();
            }

        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }
}
