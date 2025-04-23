//package server.tonyo.itk.ppke.hu
package hu.ppke.itk.tonyo.backend;



public class server  {
    public static void main(String[] args) {
        createDB dbCreator = new createDB("test.db");
        Thread dbThread = new Thread(dbCreator);
        dbThread.start();
        try{
            dbThread.join();
        } catch (InterruptedException e){
            System.err.println("Database fail: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}
