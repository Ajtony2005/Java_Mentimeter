package hu.ppke.itk.tonyo.backend;

import java.sql.*;
import java.util.concurrent.Callable;

public class login implements Callable<Integer> {
    private final String dbName;
    private final String username;
    private final String password;

    public login(String dbName, String username, String password) {
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    @Override
    public Integer call() {
        String url = "jdbc:sqlite:" + dbName;
        try (Connection connection = DriverManager.getConnection(url)) {
            System.out.println("Opened database successfully");
            String sql = "SELECT * FROM users WHERE name = ? AND password = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    // Sikeres bejelentkezés
                    return 0;
                } else {
                    // Hibás felhasználónév vagy jelszó
                    return 3;
                }
            } catch (SQLException e) {
                System.err.println("Login query failed: " + e.getMessage());
                return 2;
            }
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Unknown error: " + e.getMessage());
            return 4;
        }
    }
}
