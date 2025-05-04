package hu.ppke.itk.tonyo.backend;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.sql.*;

public class server {
    public static void main(String[] args) {
        int port = 42069;
        Gson gson = new Gson();

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
                String action = request.get("action").getAsString();

                JsonObject response = new JsonObject();

                if ("login".equals(action)) {
                    String username = request.get("username").getAsString();
                    String password = request.get("password").getAsString();

                    login loginTask = new login("test.db", username, password);
                    int errorCode = loginTask.call();

                    response.addProperty("errorCode", errorCode);
                } else if ("register".equals(action)) {
                    String username = request.get("username").getAsString();
                    String password = request.get("password").getAsString();

                    registrations registerTask = new registrations("test.db", username, password);
                    int errorCode = registerTask.call();

                    response.addProperty("errorCode", errorCode);
                } else if ("join".equals(action)) {
                    String code = request.get("code").getAsString();

                    int errorCode = joinLobby("test.db", code);
                    response.addProperty("errorCode", errorCode);
                } else if ("create".equals(action)) {
                    int errorCode = createLobby("test.db");
                    response.addProperty("errorCode", errorCode);
                } else {
                    response.addProperty("errorCode", 4); // Unknown error
                }

                out.println(gson.toJson(response));

                clientSocket.close();
            }

        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static int joinLobby(String dbName, String code) {
        String url = "jdbc:sqlite:" + dbName;
        try (Connection connection = DriverManager.getConnection(url)) {
            String sql = "SELECT COUNT(*) FROM lobbies WHERE code = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, code);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return 0; // Sikeres csatlakozás
                } else {
                    return 2; // Érvénytelen kód
                }
            } catch (SQLException e) {
                System.err.println("Join query failed: " + e.getMessage());
                return 2;
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Unknown error: " + e.getMessage());
            return 4;
        }
    }

    private static int createLobby(String dbName) {
        String url = "jdbc:sqlite:" + dbName;
        try (Connection connection = DriverManager.getConnection(url)) {
            String code = generateRandomCode(); // Véletlenszerű kód generálása
            String sql = "INSERT INTO lobbies (code) VALUES (?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, code);
                pstmt.executeUpdate();
                return 0; // Sikeres létrehozás
            } catch (SQLException e) {
                System.err.println("Create query failed: " + e.getMessage());
                return 2;
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Unknown error: " + e.getMessage());
            return 4;
        }
    }

    private static String generateRandomCode() {
        // Egyszerű véletlenszerű kód generálása (pl. 6 karakteres)
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return code.toString();
    }
}