package hu.ppke.itk.tonyo.backend;

import java.sql.*;

public class Createtable implements Runnable {
    private final String dbName;
    private final Connection connection;

    public Createtable(String dbname, Connection connection) {
        this.dbName = dbname;
        this.connection = connection;
    }

    @Override
    public void run() {
        try (Statement stmt = connection.createStatement()) {
                String createUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    email TEXT,
                    password TEXT
                );
            """;

                String createQuestions = """
                CREATE TABLE IF NOT EXISTS questions (
                    id INTEGER PRIMARY KEY,
                    users_id INTEGER,
                    question TEXT,
                    question_type TEXT,
                    FOREIGN KEY(users_id) REFERENCES users(id)
                );
            """;

                String createAnswers = """
                CREATE TABLE IF NOT EXISTS answers (
                    id INTEGER PRIMARY KEY,
                    question_id INTEGER,
                    users_id INTEGER,
                    answer TEXT,
                    FOREIGN KEY(question_id) REFERENCES questions(id),
                    FOREIGN KEY(users_id) REFERENCES users(id)
                );
            """;

            stmt.execute(createUsers);
            stmt.execute(createQuestions);
            stmt.execute(createAnswers);
            System.out.println("Create Table succesfully");
        } catch(SQLException e){
            System.err.println("Create Table fail: " + e.getMessage());
        }
    }
}
