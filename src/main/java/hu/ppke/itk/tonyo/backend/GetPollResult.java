package hu.ppke.itk.tonyo.backend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * A {@code GetPollResult} osztály egy szavazás részleteinek és eredményeinek lekérdezésére szolgál
 * egy SQLite adatbázisban. A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 */
public class GetPollResult implements Callable<JsonObject> {

    private static final Gson gson = new Gson();
    private final String dbName;
    private final JsonObject request;
    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a szavazás eredményeinek lekérdezéséhez szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "mentimeter.db")
     * @param request a kliens JSON kérése
     * @param clientHandler a kliens kezelő objektuma
     */
    public GetPollResult(String dbName, JsonObject request, ClientHandler clientHandler) {
        this.dbName = dbName;
        this.request = request;
        this.clientHandler = clientHandler;
    }

    /**
     * Lekéri a megadott szavazás részleteit és eredményeit, beleértve a szavazatok számát opciónként.
     *
     * @return a szavazás eredményeinek JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "get_poll_results");

        if (!request.has("pollId")) {
            response.addProperty("status", "error");
            response.addProperty("message", "Hiányzó pollId a kérésben.");
            return response;
        }

        int pollId = request.get("pollId").getAsInt();
        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            String pollSql = "SELECT szavazas_id, cim, kerdes, tipus, allapot, beallitasok FROM szavazasok WHERE szavazas_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(pollSql)) {
                pstmt.setInt(1, pollId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        JsonObject poll = new JsonObject();
                        poll.addProperty("pollId", rs.getInt("szavazas_id"));
                        poll.addProperty("title", rs.getString("cim"));
                        poll.addProperty("question", rs.getString("kerdes"));
                        poll.addProperty("type", rs.getString("tipus"));
                        poll.addProperty("status", rs.getString("allapot"));
                        String settingsJson = rs.getString("beallitasok");
                        JsonObject settings = settingsJson != null && !settingsJson.isEmpty() ?
                                gson.fromJson(settingsJson, JsonObject.class) : new JsonObject();
                        poll.add("settings", settings);
                        response.add("poll", poll);

                        String votesSql = "SELECT opcio_id, COUNT(*) as vote_count FROM valaszok WHERE szavazas_id = ? GROUP BY opcio_id";
                        try (PreparedStatement votesPstmt = conn.prepareStatement(votesSql)) {
                            votesPstmt.setInt(1, pollId);
                            try (ResultSet votesRs = votesPstmt.executeQuery()) {
                                JsonArray results = new JsonArray();
                                while (votesRs.next()) {
                                    JsonObject result = new JsonObject();
                                    result.addProperty("optionId", votesRs.getInt("opcio_id"));
                                    result.addProperty("voteCount", votesRs.getInt("vote_count"));
                                    results.add(result);
                                }
                                response.add("results", results);
                                response.addProperty("status", "success");
                            }
                        }
                    } else {
                        response.addProperty("status", "error");
                        response.addProperty("message", "Érvénytelen szavazás azonosító.");
                    }
                }
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Eredmények lekérése sikertelen: " + e.getMessage());
        }
        return response;
    }
}