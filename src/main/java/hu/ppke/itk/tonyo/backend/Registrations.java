package hu.ppke.itk.tonyo.backend;

import java.sql.*;
import java.util.concurrent.Callable;

public class Registrations implements Callable<Integer> {
    private final String dbName;
    private final String username;
    private final String password;

    public Registrations(String dbName, String username, String password) {
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    @Override
    public Integer call() {
        String url = "jdbc:sqlite:" + dbName;
        try (Connection connection = DriverManager.getConnection(url)) {
            String checkSql = "SELECT COUNT(*) FROM felhasznalok WHERE felhasznalonev = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return 3;
                }
            }

            String sql = "INSERT INTO felhasznalok (felhasznalonev, jelszo) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                pstmt.executeUpdate();
                return 0;
            }
        } catch (SQLException e) {
            System.err.println("Regisztrációs hiba: " + e.getMessage());
            return 2;
        } catch (Exception e) {
            System.err.println("Ismeretlen hiba: " + e.getMessage());
            return 4;
        }
    }
}