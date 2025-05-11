package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * A {@code CreatePoll} osztály egy új szavazás létrehozására szolgál egy SQLite adatbázisban.
 * A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 */
public class CreatePoll implements Callable<JsonObject> {

    private final String dbName;
    private final JsonObject request;
    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a szavazás létrehozásához szükséges adatokat.
     *
     * @param dbName az adatbázis neve
     * @param request a kliens JSON kérése
     * @param clientHandler a kliens kezelő objektuma
     */
    public CreatePoll(String dbName, JsonObject request, ClientHandler clientHandler) {
        this.dbName = dbName;
        this.request = request;
        this.clientHandler = clientHandler;
    }

    /**
     * Létrehoz egy új szavazást, és visszaadja a művelet eredményét.
     *
     * @return a szavazás létrehozásának JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "create_poll");
        if (clientHandler.getUserId() == -1) {
            response.addProperty("status", "error");
            response.addProperty("message", "Bejelentkezés szükséges");
            return response;
        }

        String title = request.get("title").getAsString();
        String question = request.get("question").getAsString();
        String type = request.get("type").getAsString();
        String settings = request.get("settings").toString();

        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            String joinCode = PollUtils.generateJoinCode(conn);

            String sql = "INSERT INTO szavazasok (felhasznalo_id, cim, kerdes, tipus, beallitasok, csatlakozasi_kod, allapot) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, clientHandler.getUserId());
                pstmt.setString(2, title);
                pstmt.setString(3, question);
                pstmt.setString(4, type);
                pstmt.setString(5, settings);
                pstmt.setString(6, joinCode);
                pstmt.setString(7, "LEZART");
                pstmt.executeUpdate();
                response.addProperty("status", "success");
                response.addProperty("message", "Szavazás létrehozva");
                response.addProperty("joinCode", joinCode);
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Szavazás létrehozása sikertelen: " + e.getMessage());
        }
        return response;
    }
}