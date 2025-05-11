package hu.ppke.itk.tonyo.backend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 * Segédosztály a szavazási rendszer közös műveleteihez, például csatlakozási kód generálásához
 * és eredmények sugárzásához.

 */
public class PollUtils {

    private static final Gson gson = new Gson();

    /**
     * Generál egy egyedi, 8 karakteres csatlakozási kódot, amely nem létezik az adatbázisban.
     *
     * @param conn az adatbázis kapcsolat
     * @return a generált csatlakozási kód
     * @throws SQLException ha az adatbázis művelet során hiba történik
     */
    public static String generateJoinCode(Connection conn) throws SQLException {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        String code;
        boolean exists;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(characters.charAt(random.nextInt(characters.length())));
            }
            code = sb.toString();
            String sql = "SELECT COUNT(*) FROM szavazasok WHERE csatlakozasi_kod = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, code);
                try (ResultSet rs = pstmt.executeQuery()) {
                    exists = rs.next() && rs.getInt(1) > 0;
                }
            }
        } while (exists);
        return code;
    }

    /**
     * Ellenőrzi, hogy a megadott válasz érvényes-e (1-3 szó).
     *
     * @param answer a felhasználó által megadott válasz
     * @return igaz, ha a válasz érvényes, különben hamis
     */
    public static boolean isValidWordCloudAnswer(String answer) {
        String[] words = answer.trim().split("\\s+");
        return words.length >= 1 && words.length <= 3;
    }

    /**
     * Elküldi a szavazás eredményeit az adott szavazáshoz csatlakozott klienseknek.
     *
     * @param conn az adatbázis kapcsolat
     * @param pollId a szavazás azonosítója
     * @param type a szavazás típusa (SZO_FELHO, TOBBVALASZTOS, SKALA)
     * @throws SQLException ha az adatbázis művelet során hiba történik
     */
    public static void broadcastResults(Connection conn, int pollId, String type) throws SQLException {
        JsonObject message = new JsonObject();
        message.addProperty("action", "broadcast_results");
        message.addProperty("event", "results_update");
        message.addProperty("pollId", pollId);
        message.addProperty("pollType", type);
        JsonObject results = new JsonObject();

        switch (type) {
            case "SZO_FELHO":
                String sql = "SELECT szo_felho_valasz, COUNT(*) as count FROM valaszok WHERE szavazas_id = ? GROUP BY szo_felho_valasz ORDER BY count DESC, szo_felho_valasz";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, pollId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        JsonArray words = new JsonArray();
                        while (rs.next()) {
                            JsonObject word = new JsonObject();
                            word.addProperty("word", rs.getString("szo_felho_valasz"));
                            word.addProperty("count", rs.getInt("count"));
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
                    try (ResultSet rs = pstmt.executeQuery()) {
                        JsonArray options = new JsonArray();
                        while (rs.next()) {
                            JsonObject option = new JsonObject();
                            option.addProperty("option", rs.getString("opcio_szoveg"));
                            option.addProperty("count", rs.getInt("count"));
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
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            results.addProperty("average", rs.getDouble("average"));
                        }
                    }
                }
                break;
        }
        message.add("results", results);
        ClientHandler.broadcastToPoll(pollId, message);
    }
}