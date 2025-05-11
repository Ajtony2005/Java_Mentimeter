package hu.ppke.itk.tonyo.backend;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A {@code server} osztály egy szavazási rendszer szerveroldali komponensét valósítja meg.
 * Inicializálja az SQLite adatbázist, és figyeli a kliensek csatlakozását a megadott porton.
 * Minden csatlakozó klienshez külön szálat indít a {@code ClientHandler} osztály segítségével.
 */
public class server {

    /** A szerver által használt port száma. */
    private static final int PORT = 42069;

    /** Az SQLite adatbázis neve. */
    private static final String DB_NAME = "mentimeter.db";

    /**
     * A szerver fő metódusa, amely inicializálja az adatbázist, létrehozza a szervert,
     * és kezeli a bejövő kliens csatlakozásokat.
     *
     * @param args parancssori argumentumok (jelenleg nem használatosak)
     */
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

        // Szerver indítása
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