package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * A {@code ResetPollData} osztály egy szavazás szavazatainak törlésére szolgál egy SQLite
 * adatbázisban. A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 */
public class ResetPollData implements Callable<JsonObject> {

    private final String dbName;
    private final JsonObject request;
    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a szavazat törléséhez szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "mentimeter.db")
     * @param request a kliens JSON kérése
     * @param clientHandler a kliens kezelő objektuma
     */
    public ResetPollData(String dbName, JsonObject request, ClientHandler clientHandler) {
        this.dbName = dbName;
        this.request = request;
        this.clientHandler = clientHandler;
    }

    /**
     * Törli a megadott szavazás szavazatait a valaszok táblából, lehetővé téve a szavazás újraindítását.
     *
     * @return a szavazat törlésének JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "reset_poll_data");

        if (clientHandler.getUserId() == -1) {
            response.addProperty("status", "error");
            response.addProperty("message", "Bejelentkezés szükséges");
            return response;
        }

        int pollId = request.get("pollId").getAsInt();
        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            String sql = "DELETE FROM valaszok WHERE szavazas_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, pollId);
                int rowsAffected = pstmt.executeUpdate();
                response.addProperty("status", "success");
                response.addProperty("message", "Szavazás adatai resetelve, törölt sorok: " + rowsAffected);
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Adatok resetelése sikertelen: " + e.getMessage());
        }
        return response;
    }
}