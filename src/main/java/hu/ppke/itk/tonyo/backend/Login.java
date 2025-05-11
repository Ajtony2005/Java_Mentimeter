package hu.ppke.itk.tonyo.backend;

import java.sql.*;
import java.util.concurrent.Callable;

public class Login implements Callable<Login.LoginResult> {
    private final String dbName;
    private final String username;
    private final String password;

    public Login(String dbName, String username, String password) {
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    public static class LoginResult {
        private final int status;
        private final int userId;

        public LoginResult(int status, int userId) {
            this.status = status;
            this.userId = userId;
        }

        public int getStatus() {
            return status;
        }

        public int getUserId() {
            return userId;
        }
    }

    @Override
    public LoginResult call() {
        String url = "jdbc:sqlite:" + dbName;
        try (Connection connection = DriverManager.getConnection(url)) {
            String sql = "SELECT felhasznalo_id, jelszo FROM felhasznalok WHERE felhasznalonev = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, username);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String storedPassword = rs.getString("jelszo");
                    int userId = rs.getInt("felhasznalo_id");
                    if (password.equals(storedPassword)) {
                        return new LoginResult(0, userId); // Sikeres bejelentkezés
                    } else {
                        return new LoginResult(3, -1); // Hibás jelszó
                    }
                } else {
                    return new LoginResult(3, -1); // Felhasználó nem található
                }
            } catch (SQLException e) {
                System.err.println("Bejelentkezési lekérdezés sikertelen: " + e.getMessage());
                return new LoginResult(2, -1); // Lekérdezési hiba
            }
        } catch (SQLException e) {
            System.err.println("Adatbázis kapcsolat sikertelen: " + e.getMessage());
            return new LoginResult(1, -1); // Adatbázis kapcsolat hiba
        }
    }
}