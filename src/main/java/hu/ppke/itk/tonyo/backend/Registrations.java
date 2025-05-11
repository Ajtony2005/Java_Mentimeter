package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * A {@code Register} osztály egy új felhasználó regisztrálására szolgál egy SQLite adatbázisban.
 * A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 *
 * @author [Szerző neve]
 */
public class Registrations implements Callable<JsonObject> {

    private final String dbName;
    private final JsonObject request;

    /**
     * Konstruktor, amely inicializálja a regisztrációhoz szükséges adatokat.
     *
     * @param dbName az adatbázis neve
     * @param request a kliens JSON kérése
     */
    public Registrations(String dbName, JsonObject request) {
        this.dbName = dbName;
        this.request = request;
    }

    /**
     * Feldolgozza a regisztrációs kérelmet, és visszaadja a művelet eredményét.
     *
     * @return a regisztráció JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "register");
        String username = request.get("username").getAsString();
        String password = request.get("password").getAsString();

        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            String checkSql = "SELECT COUNT(*) FROM felhasznalok WHERE felhasznalonev = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    response.addProperty("status", "error");
                    response.addProperty("message", "A felhasználónév már foglalt");
                    return response;
                }
            }

            String sql = "INSERT INTO felhasznalok (felhasznalonev, jelszo) VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.executeUpdate();
                response.addProperty("status", "success");
                response.addProperty("message", "Sikeres regisztráció");
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Regisztrációs hiba: " + e.getMessage());
        }
        return response;
    }
}