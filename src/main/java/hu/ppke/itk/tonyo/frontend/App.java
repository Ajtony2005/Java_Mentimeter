package hu.ppke.itk.tonyo.frontend;

import hu.ppke.itk.tonyo.frontend.pages.ConnectPage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private static cliens Cliens;
    private static Stage primaryStage;

    public static cliens getCliens() {
        return Cliens;
    }
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void start(Stage stage) throws Exception {
        Cliens = new cliens();
        Cliens.connect("localhost", 42069);
        primaryStage = stage;

        ConnectPage page = new ConnectPage();
        Scene scene = new Scene(page.getView(), 400, 300);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setTitle("Mentimeter - Bejelentkezés");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}