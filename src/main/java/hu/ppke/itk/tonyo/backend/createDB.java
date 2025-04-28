package hu.ppke.itk.tonyo.backend;
/*
 //https://www.tutorialspoint.com/sqlite/sqlite_java.htm
 */

import java.sql.*;

public class createDB {
    private final String dbName;
    public createDB(String dbName) {
        this.dbName = dbName;
    }

    public void CreateDB() {
        String url = "jdbc:sqlite:" + dbName;

        try (Connection connection = DriverManager.getConnection(url)){
            System.out.println("Opened database succesfully");
            Createtable createTable = new Createtable(dbName, connection);
            createTable.createTable();
        } catch (SQLException e){
            System.err.println("Database connection faild:" + e.getMessage());
        }
    }
}
