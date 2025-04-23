package hu.ppke.itk.tonyo.backend;
/*
 //https://www.tutorialspoint.com/sqlite/sqlite_java.htm
 */

import java.sql.*;

public class createDB implements Runnable {
    private final String dbName;
    public createDB(String dbName) {
        this.dbName = dbName;
    }

    @Override
    public void run() {
        String url = "jdbc:sqlite:" + dbName;

        try (Connection connection = DriverManager.getConnection(url)){
            System.out.println("Opened database succesfully");
            Thread createTable = new Thread(new Createtable(dbName, connection));
            createTable.start();
            try {
                createTable.join();
            } catch (InterruptedException e ){
                System.out.println("Table Generated fail:" + e.getMessage());
                Thread.currentThread().interrupt();
            }
        } catch (SQLException e){
            System.err.println("Database connection faild:" + e.getMessage());
        }
    }
}
