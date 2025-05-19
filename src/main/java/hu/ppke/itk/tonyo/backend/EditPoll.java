package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * A {@code EditPoll} osztály egy szavazás metaadatainak szerkesztésére szolgál egy SQLite
 * adatbázisban. A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 */
public class EditPoll implements Callable<JsonObject> {

    private final String dbName;
    private final JsonObject request;
    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a szavazás szerkesztéséhez szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "mentimeter.db")
     * @param request a kliens JSON kérése
     * @param clientHandler a kliens kezelő objektuma
     */
    public EditPoll(String dbName, JsonObject request, ClientHandler clientHandler) {
        this.dbName = dbName;
        this.request = request;
        this.clientHandler = clientHandler;
    }

    /**
     * Szerkeszti a megadott szavazás metaadatait (cím, kérdés, típus, beállítások), ha a felhasználó jogosult rá.
     *
     * @return a szerkesztés JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "edit_poll");

        if (clientHandler.getUserId() == -1) {
            response.addProperty("status", "error");
            response.addProperty("message", "Bejelentkezés szükséges");
            return response;
        }

        int pollId = request.get("pollId").getAsInt();
        String title = request.get("title").getAsString();
        String question = request.get("question").getAsString();
        String type = request.get("type").getAsString();
        String settingsJson = request.get("settings").toString();

        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            // Tranzakció kezdete
            conn.setAutoCommit(false);

            try {
                // Jogosultság ellenőrzése
                String checkSql = "SELECT felhasznalo_id FROM szavazasok WHERE szavazas_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, pollId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next() && rs.getInt("felhasznalo_id") != clientHandler.getUserId()) {
                            response.addProperty("status", "error");
                            response.addProperty("message", "Nincs jogosultság a szavazás szerkesztéséhez");
                            conn.rollback();
                            return response;
                        }
                    }
                }

                // Szavazás szerkesztése
                String sql = "UPDATE szavazasok SET cim = ?, kerdes = ?, tipus = ?, beallitasok = ? WHERE szavazas_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, title);
                    pstmt.setString(2, question);
                    pstmt.setString(3, type);
                    pstmt.setString(4, settingsJson);
                    pstmt.setInt(5, pollId);
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        response.addProperty("status", "success");
                        response.addProperty("message", "Szavazás sikeresen szerkesztve");
                    } else {
                        response.addProperty("status", "error");
                        response.addProperty("message", "Érvénytelen szavazás azonosító");
                        conn.rollback();
                        return response;
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                response.addProperty("status", "error");
                response.addProperty("message", "Szavazás szerkesztése sikertelen: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Adatbázis kapcsolat sikertelen: " + e.getMessage());
        }
        return response;
    }
}