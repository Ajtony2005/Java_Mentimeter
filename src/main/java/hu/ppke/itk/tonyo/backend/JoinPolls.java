package hu.ppke.itk.tonyo.backend;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * A {@code JoinPoll} osztály egy szavazáshoz való csatlakozás kezelésére szolgál egy SQLite
 * adatbázisban, csatlakozási kód alapján. A {@code Callable} interfészt implementálja,
 * így szálbiztos környezetben futtatható.
 */
public class JoinPolls implements Callable<JsonObject> {

    private static final Gson gson = new Gson();
    private final String dbName;
    private final JsonObject request;
    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a szavazáshoz csatlakozáshoz szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "mentimeter.db")
     * @param request a kliens JSON kérése
     * @param clientHandler a kliens kezelő objektuma
     */
    public JoinPolls(String dbName, JsonObject request, ClientHandler clientHandler) {
        this.dbName = dbName;
        this.request = request;
        this.clientHandler = clientHandler;
    }

    /**
     * Csatlakozik egy szavazáshoz a megadott csatlakozási kód alapján, és visszaadja a szavazás
     * részleteit JSON formátumban.
     *
     * @return a csatlakozás JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "join_poll");
        String joinCode = request.get("joinCode").getAsString();

        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            String sql = "SELECT szavazas_id, cim, kerdes, tipus, beallitasok, allapot FROM szavazasok WHERE csatlakozasi_kod = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, joinCode);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        JsonObject poll = new JsonObject();
                        int pollId = rs.getInt("szavazas_id");
                        poll.addProperty("pollId", pollId);
                        poll.addProperty("title", rs.getString("cim"));
                        poll.addProperty("question", rs.getString("kerdes"));
                        poll.addProperty("type", rs.getString("tipus"));
                        poll.addProperty("status", rs.getString("allapot"));
                        String settingsJson = rs.getString("beallitasok");
                        JsonObject settings = settingsJson != null && !settingsJson.isEmpty() ?
                                gson.fromJson(settingsJson, JsonObject.class) : new JsonObject();
                        poll.add("settings", settings);
                        response.addProperty("status", "success");
                        response.add("poll", poll);
                    } else {
                        response.addProperty("status", "error");
                        response.addProperty("message", "Érvénytelen csatlakozási kód");
                    }
                }
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Csatlakozás sikertelen: " + e.getMessage());
        }
        return response;
    }
}