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
 * A {@code GetResults} osztály egy szavazás eredményeinek lekérdezésére szolgál egy SQLite
 * adatbázisban. A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 */
public class GetResults implements Callable<JsonObject> {

    private final String dbName;
    private final JsonObject request;

    /**
     * Konstruktor, amely inicializálja az eredmények lekérdezéséhez szükséges adatokat.
     *
     * @param dbName az adatbázis neve (pl. "mentimeter.db")
     * @param request a kliens JSON kérése
     */
    public GetResults(String dbName, JsonObject request) {
        this.dbName = dbName;
        this.request = request;
    }

    /**
     * Lekéri egy szavazás eredményeit a szavazás típusától függően (szófelhő, többválasztós, skála),
     * ha a szavazás EREDMENY állapotban van.
     *
     * @return az eredmények JSON válasza
     * @throws SQLException ha az adatbázis műveletek során hiba történik
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "get_results");
        int pollId = request.get("pollId").getAsInt();

        String url = "jdbc:sqlite:" + dbName;
        try (Connection conn = DriverManager.getConnection(url)) {
            String checkStatusSql = "SELECT allapot, tipus FROM szavazasok WHERE szavazas_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkStatusSql)) {
                checkStmt.setInt(1, pollId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next() || !rs.getString("allapot").equals("EREDMENY")) {
                        response.addProperty("status", "error");
                        response.addProperty("message", "Az eredmények nem érhetők el");
                        return response;
                    }
                    String type = rs.getString("tipus");

                    JsonObject results = new JsonObject();
                    switch (type) {
                        case "SZO_FELHO":
                            String sql = "SELECT szo_felho_valasz, COUNT(*) as count FROM valaszok WHERE szavazas_id = ? GROUP BY szo_felho_valasz ORDER BY count DESC, szo_felho_valasz";
                            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                                pstmt.setInt(1, pollId);
                                try (ResultSet resultSet = pstmt.executeQuery()) {
                                    JsonArray words = new JsonArray();
                                    while (resultSet.next()) {
                                        JsonObject word = new JsonObject();
                                        word.addProperty("word", resultSet.getString("szo_felho_valasz"));
                                        word.addProperty("count", resultSet.getInt("count"));
                                        words.add(word);
                                    }
                                    results.add("words", words);
                                }
                            }
                            break;
                        case "TOBBVALASZTOS":
                            sql = "SELECT o.opcio_szoveg, COUNT(v.opcio_id) as count FROM szavazasi_opciok o LEFT JOIN valaszok v ON o.opcio_id = v.opcio_id WHERE o.szavazas_id = ? GROUP BY o.opcio_id";
                            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                                pstmt.setInt(1, pollId);
                                try (ResultSet resultSet = pstmt.executeQuery()) {
                                    JsonArray options = new JsonArray();
                                    while (resultSet.next()) {
                                        JsonObject option = new JsonObject();
                                        option.addProperty("option", resultSet.getString("opcio_szoveg"));
                                        option.addProperty("count", resultSet.getInt("count"));
                                        options.add(option);
                                    }
                                    results.add("options", options);
                                }
                            }
                            break;
                        case "SKALA":
                            sql = "SELECT AVG(skala_ertek) as average FROM valaszok WHERE szavazas_id = ?";
                            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                                pstmt.setInt(1, pollId);
                                try (ResultSet resultSet = pstmt.executeQuery()) {
                                    if (resultSet.next()) {
                                        results.addProperty("average", resultSet.getDouble("average"));
                                    }
                                }
                            }
                            break;
                    }
                    response.addProperty("status", "success");
                    response.add("results", results);
                }
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Eredmények lekérdezése sikertelen: " + e.getMessage());
        }
        return response;
    }
}