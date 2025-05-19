package hu.ppke.itk.tonyo.backend;

import java.sql.*;

/**
 * A {@code createDB} osztály egy SQLite adatbázis inicializálására és tábla létrehozására szolgál.
 * Az osztály az SQLite JDBC driver-t használja az adatbázis kezelésére, és biztosítja, hogy az
 * adatbázis és a szükséges táblák létrejöjjenek a megadott név alapján.
 */
public class createDB {

    /** Az adatbázis neve, amely az SQLite adatbázis fájlneve. */
    private final String dbName;

    /**
     * Konstruktor, amely inicializálja az adatbázis nevét.
     *
     * @param dbName az adatbázis neve (pl. "voting.db")
     */
    public createDB(String dbName) {
        this.dbName = dbName;
    }

    /**
     * Inicializálja az SQLite adatbázist és létrehozza a szükséges táblát.
     * Az adatbázis automatikusan létrejön, ha még nem létezik, és a tábla létrehozása
     * a {@code Createtable} osztályon keresztül történik tranzakciókezeléssel.
     * Az autoCommit mód kikapcsolva marad.
     *
     * @throws SQLException ha az adatbázis kapcsolódás vagy a tábla létrehozása során hiba történik
     */
    public void CreateDB() {
        String url = "jdbc:sqlite:" + dbName;

        try (Connection connection = DriverManager.getConnection(url)) {
            System.out.println("Opened database successfully");

            // AutoCommit kikapcsolása
            connection.setAutoCommit(false);

            // Tranzakcióban tábla létrehozása
            try {
                Createtable n = new Createtable(dbName, connection);
                n.createTable();
                connection.commit();
                System.out.println("Table created successfully");
            } catch (SQLException e) {
                System.err.println("Table creation failed: " + e.getMessage());
                connection.rollback();
            }

        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
}