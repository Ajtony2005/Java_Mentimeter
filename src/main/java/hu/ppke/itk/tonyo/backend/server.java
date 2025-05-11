package hu.ppke.itk.tonyo.backend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

public class server {
    private static final int PORT = 42069;
    private static final String DB_NAME = "mentimeter.db";

    public static void main(String[] args) {
        // Adatbázis inicializálása
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME)) {
            createDB createTable = new createDB(DB_NAME);
            createTable.CreateDB();
            System.out.println("Adatbázis inicializálva");
        } catch (SQLException e) {
            System.err.println("Adatbázis inicializálási hiba: " + e.getMessage());
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Szerver elindult. Figyel a " + PORT + " porton");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Kliens csatlakozott: " + clientSocket.getInetAddress());
                // Új szál indítása a kliens kiszolgálására
                Thread clientThread = new Thread(new ClientHandler(clientSocket, DB_NAME));
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Szerver kivétel: " + e.getMessage());
        }
    }
}