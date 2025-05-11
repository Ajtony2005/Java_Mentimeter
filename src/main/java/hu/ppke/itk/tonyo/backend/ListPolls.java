package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * A {@code ListPolls} osztály a bejelentkezett felhasználó által létrehozott szavazások
 * listázására szolgál egy SQLite adatbázisban. A {@code Callable} interfészt implementálja,
 * így szálbiztos környezetben futtatható.

 */
public class ListPolls implements Callable<JsonObject> {

    private final String dbName;
    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a szavazások listázásához szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "mentimeter.db")
     * @param clientHandler a kliens kezelő objektuma
     */
    public ListPolls(String dbName, ClientHandler clientHandler) {
        this.dbName = dbName;
        this.clientHandler = clientHandler;
    }

    /**
     * Lekéri a bejelentkezett felhasználó által létrehozott szavazások listáját, és visszaadja
     * azok metaadatait JSON formátumban.
     *
     * @return a szavazások listájának JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "list_polls");

        if (clientHandler.getUserId() == -1) {
            response.addProperty("status", "error");
            response.addProperty("message", "Bejelentkezés szükséges");
            return response;
        }

        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            JsonArray polls = new JsonArray();
            String sql = "SELECT szavazas_id, cim, kerdes, tipus, csatlakozasi_kod, allapot FROM szavazasok WHERE felhasznalo_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, clientHandler.getUserId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        JsonObject poll = new JsonObject();
                        poll.addProperty("pollId", rs.getInt("szavazas_id"));
                        poll.addProperty("title", rs.getString("cim"));
                        poll.addProperty("question", rs.getString("kerdes"));
                        poll.addProperty("type", rs.getString("tipus"));
                        poll.addProperty("joinCode", rs.getString("csatlakozasi_kod"));
                        poll.addProperty("status", rs.getString("allapot"));
                        polls.add(poll);
                    }
                    response.addProperty("status", "success");
                    response.add("polls", polls);
                }
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Szavazások lekérdezése sikertelen: " + e.getMessage());
        }
        return response;
    }
}