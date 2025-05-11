package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * A {@code UpdatePollStatus} osztály egy szavazás állapotának frissítésére szolgál egy SQLite
 * adatbázisban. A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 */
public class UpdatePollStatus implements Callable<JsonObject> {

    private final String dbName;
    private final JsonObject request;

    /**
     * Konstruktor, amely inicializálja az állapotfrissítéshez szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "mentimeter.db")
     * @param request a kliens JSON kérése
     */
    public UpdatePollStatus(String dbName, JsonObject request) {
        this.dbName = dbName;
        this.request = request;
    }

    /**
     * Frissíti a megadott szavazás állapotát, és sugározza az eredményeket, ha az állapot EREDMENY-re változik.
     *
     * @return az állapotfrissítés JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "update_poll_status");
        int pollId = request.get("pollId").getAsInt();
        String newStatus = request.get("newStatus").getAsString();

        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            String sql = "UPDATE szavazasok SET allapot = ? WHERE szavazas_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newStatus);
                pstmt.setInt(2, pollId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    response.addProperty("status", "success");
                    response.addProperty("message", "Szavazás állapota frissítve: " + newStatus);
                    JsonObject broadcastMessage = new JsonObject();
                    broadcastMessage.addProperty("event", "status_update");
                    broadcastMessage.addProperty("pollId", pollId);
                    broadcastMessage.addProperty("newStatus", newStatus);
                    ClientHandler.broadcastToPoll(pollId, broadcastMessage);
                    if (newStatus.equals("EREDMENY")) {
                        String typeSql = "SELECT tipus FROM szavazasok WHERE szavazas_id = ?";
                        try (PreparedStatement typeStmt = conn.prepareStatement(typeSql)) {
                            typeStmt.setInt(1, pollId);
                            try (ResultSet rs = typeStmt.executeQuery()) {
                                if (rs.next()) {
                                    PollUtils.broadcastResults(conn, pollId, rs.getString("tipus"));
                                }
                            }
                        }
                    }
                } else {
                    response.addProperty("status", "error");
                    response.addProperty("message", "Érvénytelen szavazás azonosító");
                }
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Állapot frissítése sikertelen: " + e.getMessage());
        }
        return response;
    }
}