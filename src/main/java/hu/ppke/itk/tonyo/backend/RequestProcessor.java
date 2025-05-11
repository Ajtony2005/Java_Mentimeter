package hu.ppke.itk.tonyo.backend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.*;
import java.util.Random;

public class RequestProcessor {
    private static final Gson gson = new Gson();

    public static JsonObject processRequest(String dbName, JsonObject request, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        String action = request.get("action").getAsString();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbName)) {
            switch (action) {
                case "register":
                    return handleRegister(conn, request);
                case "login":
                    return handleLogin(conn, request, clientHandler);
                case "create_poll":
                    return handleCreatePoll(conn, request, clientHandler);
                case "list_polls":
                    return handleListPolls(conn, clientHandler);
                case "list_my_polls":
                    return handleListMyPolls(conn, clientHandler);
                case "join_poll":
                    return handleJoinPoll(conn, request, clientHandler);
                case "submit_vote":
                    return handleSubmitVote(conn, request, clientHandler);
                case "get_results":
                    return handleGetResults(conn, request);
                case "update_poll_status":
                    return handleUpdatePollStatus(conn, request);
                case "reset_poll_data":
                    return handleResetPollData(conn, request, clientHandler);
                case "edit_poll":
                    return handleEditPoll(conn, request, clientHandler);
                case "get_poll_details":
                    return handleGetPollDetails(conn, request, clientHandler);
                case "get_poll_results":
                    return handleGetPollResults(conn, request, clientHandler);
                case "logout":
                    clientHandler.setUserId(-1);
                    response.addProperty("status", "success");
                    response.addProperty("message", "Sikeres kijelentkezés");
                    break;
                default:
                    response.addProperty("status", "error");
                    response.addProperty("message", "Ismeretlen művelet");
                    return response;
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Adatbázis hiba: " + e.getMessage());
            return response;
        }
        return response;
    }


    private static JsonObject handleRegister(Connection conn, JsonObject request) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "register");
        String username = request.get("username").getAsString();
        String password = request.get("password").getAsString();

        Registrations reg = new Registrations("mentimeter.db", username, password);
        int result = reg.call();

        switch (result) {
            case 0:
                response.addProperty("status", "success");
                response.addProperty("message", "Sikeres regisztráció");
                break;
            case 3:
                response.addProperty("status", "error");
                response.addProperty("message", "A felhasználónév már foglalt");
                break;
            default:
                response.addProperty("status", "error");
                response.addProperty("message", "Regisztrációs hiba");
        }
        return response;
    }

    private static JsonObject handleLogin(Connection conn, JsonObject request, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "login");
        String username = request.get("username").getAsString();
        String password = request.get("password").getAsString();

        Login login = new Login("mentimeter.db", username, password);
        Login.LoginResult result = login.call();

        response.addProperty("status", result.getStatus() == 0 ? "success" : "error");
        response.addProperty("userId", result.getUserId());
        if (result.getStatus() == 0) {
            clientHandler.setUserId(result.getUserId());
            response.addProperty("message", "Sikeres bejelentkezés");
        } else {
            response.addProperty("message", "Hibás felhasználónév vagy jelszó");
        }
        return response;
    }

    private static JsonObject handleCreatePoll(Connection conn, JsonObject request, ClientHandler clientHandler) {
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
        String joinCode = generateJoinCode(conn);

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
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Szavazás létrehozása sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleListPolls(Connection conn, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "list_polls");
        if (clientHandler.getUserId() == -1) {
            response.addProperty("status", "error");
            response.addProperty("message", "Bejelentkezés szükséges");
            return response;
        }

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
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Szavazások lekérdezése sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleListMyPolls(Connection conn, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "list_my_polls");
        if (clientHandler.getUserId() == -1) {
            response.addProperty("status", "error");
            response.addProperty("message", "Bejelentkezés szükséges");
            return response;
        }

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
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Saját szavazások lekérdezése sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleJoinPoll(Connection conn, JsonObject request, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "join_poll");
        String joinCode = request.get("joinCode").getAsString();

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
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Csatlakozás sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleGetPollDetails(Connection conn, JsonObject request, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "get_poll_details");

        int pollId = request.get("pollId").getAsInt();
        String sql = "SELECT szavazas_id, cim, kerdes, tipus, allapot, beallitasok FROM szavazasok WHERE szavazas_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
                    response.addProperty("status", "success");
                    response.add("poll", poll);
                } else {
                    response.addProperty("status", "error");
                    response.addProperty("message", "Érvénytelen szavazas_id");
                }
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Szavazás adatainak lekérése sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleSubmitVote(Connection conn, JsonObject request, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "submit_vote");

        int pollId = request.get("pollId").getAsInt();
        int optionId = request.get("optionId").getAsInt();

        // Ellenőrizzük a szavazás létezését és állapotát
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

                // Beállítások lekérése és opció ellenőrzése
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

                // Szavazat rögzítése
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
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Szavazat leadása sikertelen: " + e.getMessage());
        }

        return response;
    }

    private static JsonObject handleGetResults(Connection conn, JsonObject request) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "get_results");
        int pollId = request.get("pollId").getAsInt();

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
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Eredmények lekérdezése sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleUpdatePollStatus(Connection conn, JsonObject request) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "update_poll_status");
        int pollId = request.get("pollId").getAsInt();
        String newStatus = request.get("newStatus").getAsString();

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
                                broadcastResults(conn, pollId, rs.getString("tipus"));
                            }
                        }
                    }
                }
            } else {
                response.addProperty("status", "error");
                response.addProperty("message", "Érvénytelen szavazás azonosító");
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Állapot frissítése sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleResetPollData(Connection conn, JsonObject request, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "reset_poll_data");

        if (clientHandler.getUserId() == -1) {
            response.addProperty("status", "error");
            response.addProperty("message", "Bejelentkezés szükséges");
            return response;
        }

        int pollId = request.get("pollId").getAsInt();
        String sql = "DELETE FROM szavazatok WHERE szavazas_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pollId);
            int rowsAffected = pstmt.executeUpdate();
            response.addProperty("status", "success");
            response.addProperty("message", "Szavazás adatai resetelve, törölt sorok: " + rowsAffected);
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Adatok resetelése sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleEditPoll(Connection conn, JsonObject request, ClientHandler clientHandler) {
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

        // Ellenőrizzük, hogy a felhasználó jogosult-e szerkeszteni a szavazást
        String checkSql = "SELECT felhasznalo_id FROM szavazasok WHERE szavazas_id = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, pollId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt("felhasznalo_id") != clientHandler.getUserId()) {
                    response.addProperty("status", "error");
                    response.addProperty("message", "Nincs jogosultság a szavazás szerkesztéséhez");
                    return response;
                }
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Hiba az ellenőrzés során: " + e.getMessage());
            return response;
        }

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
            }
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Szavazás szerkesztése sikertelen: " + e.getMessage());
        }
        return response;
    }

    private static JsonObject handleGetPollResults(Connection conn, JsonObject request, ClientHandler clientHandler) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "get_poll_results");

        if (!request.has("pollId")) {
            response.addProperty("status", "error");
            response.addProperty("message", "Hiányzó pollId a kérésben.");
            return response;
        }

        int pollId = request.get("pollId").getAsInt();

        // Szavazás adatainak lekérése
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

                    // Szavazatok megszámlálása opciónként
                    String votesSql = "SELECT opcio_id, COUNT(*) as vote_count " +
                            "FROM valaszok WHERE szavazas_id = ? GROUP BY opcio_id";
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
        } catch (SQLException e) {
            response.addProperty("status", "error");
            response.addProperty("message", "Eredmények lekérése sikertelen: " + e.getMessage());
        }

        return response;
    }


    private static void broadcastResults(Connection conn, int pollId, String type) throws SQLException {
        JsonObject message = new JsonObject();
        message.addProperty("action", "broadcast_results");
        message.addProperty("event", "results_update");
        message.addProperty("pollId", pollId);
        message.addProperty("pollType", type); // Hozzáadva a pollType a kliens számára
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

    private static boolean isValidWordCloudAnswer(String answer) {
        String[] words = answer.trim().split("\\s+");
        return words.length >= 1 && words.length <= 3;
    }

    private static String generateJoinCode(Connection conn) {
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
            } catch (SQLException e) {
                exists = true; // Biztonsági okokból új kódot generálunk
            }
        } while (exists);
        return code;
    }
}