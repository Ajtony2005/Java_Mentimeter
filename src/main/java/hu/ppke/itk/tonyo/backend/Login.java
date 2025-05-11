package hu.ppke.itk.tonyo.backend;

import java.sql.*;
import java.util.concurrent.Callable;

/**
 * A {@code Login} osztály egy SQLite adatbázisban tárolt felhasználók bejelentkeztetésére szolgál.
 * A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható. A bejelentkezés
 * eredményét egy {@code LoginResult} objektumban adja vissza, amely a státuszkódot és a felhasználói
 * azonosítót tartalmazza.
 *
 * @author [Szerző neve]
 */
public class Login implements Callable<Login.LoginResult> {

    /** Az SQLite adatbázis neve. */
    private final String dbName;

    /** A bejelentkezéshez megadott felhasználónév. */
    private final String username;

    /** A bejelentkezéshez megadott jelszó. */
    private final String password;

    /**
     * Konstruktor, amely inicializálja a bejelentkezéshez szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "voting.db")
     * @param username a felhasználónév
     * @param password a jelszó
     */
    public Login(String dbName, String username, String password) {
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    /**
     * A bejelentkezés eredményét tároló belső osztály.
     */
    public static class LoginResult {

        /**
         * A bejelentkezés státuszkódja:
         * <ul>
         *     <li>0: Sikeres bejelentkezés</li>
         *     <li>1: Adatbázis kapcsolati hiba</li>
         *     <li>2: SQL lekérdezési hiba</li>
         *     <li>3: Hibás felhasználónév vagy jelszó</li>
         * </ul>
         */
        private final int status;

        /** A bejelentkezett felhasználó azonosítója, vagy -1, ha a bejelentkezés sikertelen. */
        private final int userId;

        /**
         * Konstruktor, amely inicializálja a bejelentkezés eredményét.
         *
         * @param status a bejelentkezés státuszkódja
         * @param userId a felhasználó azonosítója
         */
        public LoginResult(int status, int userId) {
            this.status = status;
            this.userId = userId;
        }

        /**
         * Visszaadja a bejelentkezés státuszkódját.
         *
         * @return a státuszkód
         */
        public int getStatus() {
            return status;
        }

        /**
         * Visszaadja a felhasználó azonosítóját.
         *
         * @return a felhasználó azonosítója
         */
        public int getUserId() {
            return userId;
        }
    }

    /**
     * Ellenőrzi a felhasználó hitelesítő adatait az adatbázisban, és visszaadja a bejelentkezés
     * eredményét. A metódus szálbiztos környezetben futtatható.
     *
     * @return egy {@code LoginResult} objektum, amely a státuszkódot és a felhasználói azonosítót tartalmazza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public LoginResult call() {
        String url = "jdbc:sqlite:" + dbName;
        try (Connection connection = DriverManager.getConnection(url)) {
            String sql = "SELECT felhasznalo_id, jelszo FROM felhasznalok WHERE felhasznalonev = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedPassword = rs.getString("jelszo");
                    int userId = rs.getInt("felhasznalo_id");
                    if (password.equals(storedPassword)) {
                        return new LoginResult(0, userId); // Sikeres bejelentkezés
                    } else {
                        return new LoginResult(3, -1); // Hibás jelszó
                    }
                } else {
                    return new LoginResult(3, -1); // Felhasználó nem található
                }
            } catch (SQLException e) {
                System.err.println("Bejelentkezési lekérdezés sikertelen: " + e.getMessage());
                return new LoginResult(2, -1); // Lekérdezési hiba
            }
        } catch (SQLException e) {
            System.err.println("Adatbázis kapcsolat sikertelen: " + e.getMessage());
            return new LoginResult(1, -1); // Adatbázis kapcsolat hiba
        }
    }
}