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
 * A {@code SubmitVote} osztály egy szavazat leadására szolgál egy SQLite adatbázisban.
 * A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 */
public class SubmitVote implements Callable<JsonObject> {

    private static final Gson gson = new Gson();
    private final String dbName;
    private final JsonObject request;
    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a szavazat leadásához szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "mentimeter.db")
     * @param request a kliens JSON kérése
     * @param clientHandler a kliens kezelő objektuma
     */
    public SubmitVote(String dbName, JsonObject request, ClientHandler clientHandler) {
        this.dbName = dbName;
        this.request = request;
        this.clientHandler = clientHandler;
    }

    /**
     * Lead egy szavazatot a megadott szavazásra és opcióra, ellenőrzi a szavazás állapotát
     * és az opció érvényességét, majd rögzíti a szavazatot.
     *
     * @return a szavazat leadásának JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "submit_vote");

        int pollId = request.get("pollId").getAsInt();
        int optionId = request.get("optionId").getAsInt();

        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            try {
                String sql = "SELECT allapot, beallitasok FROM szavazasok WHERE szavazas_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, pollId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (!rs.next()) {
                            response.addProperty("status", "error");
                            response.addProperty("message", "Érvénytelen szavazás azonosító.");
                            return response;
                        }

                        String pollStatus = rs.getString("allapot");
                        if (!pollStatus.equals("SZAVAZAS")) {
                            response.addProperty("status", "error");
                            response.addProperty("message", "A szavazás nem aktív szavazási fázisban van.");
                            return response;
                        }

                        String settingsJson = rs.getString("beallitasok");
                        if (settingsJson == null || settingsJson.isEmpty()) {
                            response.addProperty("status", "error");
                            response.addProperty("message", "A szavazás beállításai hiányoznak.");
                            return response;
                        }

                        JsonObject settings = gson.fromJson(settingsJson, JsonObject.class);
                        if (!settings.has("options")) {
                            response.addProperty("status", "error");
                            response.addProperty("message", "A szavazás nem tartalmaz opciókat.");
                            return response;
                        }

                        JsonArray options = settings.getAsJsonArray("options");
                        boolean validOption = false;
                        for (int i = 0; i < options.size(); i++) {
                            JsonObject option = options.get(i).getAsJsonObject();
                            if (option.get("id").getAsInt() == optionId) {
                                validOption = true;
                                break;
                            }
                        }

                        if (!validOption) {
                            response.addProperty("status", "error");
                            response.addProperty("message", "Érvénytelen opció azonosító.");
                            return response;
                        }

                        String insertSql = "INSERT INTO valaszok (szavazas_id, opcio_id) VALUES (?, ?)";
                        try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                            insertPstmt.setInt(1, pollId);
                            insertPstmt.setInt(2, optionId);
                            int rowsAffected = insertPstmt.executeUpdate();
                            if (rowsAffected > 0) {
                                response.addProperty("status", "success");
                                response.addProperty("message", "Szavazat sikeresen leadva!");
                            } else {
                                response.addProperty("status", "error");
                                response.addProperty("message", "Szavazat rögzítése sikertelen.");
                            }
                        }
                    }
                }
                conn.commit();
            }catch (SQLException e){
                conn.rollback();
                response.addProperty("status", "error");
                response.addProperty("message", "Szavazat leadása sikertelen: " + e.getMessage());

            } finally{
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Szavazat leadása sikertelen: " + e.getMessage());
        }
        return response;
    }
}