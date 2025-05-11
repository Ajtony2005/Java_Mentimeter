package hu.ppke.itk.tonyo.frontend;

import hu.ppke.itk.tonyo.frontend.pages.ConnectPage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A {@code App} osztály a szavazási rendszer kliensoldali JavaFX alkalmazásának
 * belépési pontja. Inicializálja a kliens kapcsolatot a szerverhez, és megjeleníti a
 * bejelentkezési oldalt. A {@code javafx.application.Application} osztályt bővíti.

 */
public class App extends Application {

    /** A kliens objektum, amely a szerverrel való kommunikációt kezeli. */
    private static cliens cliens;

    /** Az alkalmazás elsődleges ablakának referenciája. */
    private static Stage primaryStage;

    /**
     * Visszaadja a kliens objektumot, amely a szerverrel való kommunikációt kezeli.
     *
     * @return a kliens objektum
     */
    public static cliens getCliens() {
        return cliens;
    }

    /**
     * Visszaadja az alkalmazás elsődleges ablakát.
     *
     * @return az elsődleges ablak ({@code Stage})
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Inicializálja és megjeleníti a JavaFX alkalmazást. Létrehozza a kliens objektumot,
     * csatlakozik a szerverhez, és beállítja a bejelentkezési oldalt.
     *
     * @param stage az alkalmazás elsődleges ablaka
     * @throws Exception ha a kliens kapcsolat vagy az ablak inicializálása során hiba történik
     */
    @Override
    public void start(Stage stage) throws Exception {
        cliens = new cliens();
        cliens.connect("localhost", 42069);
        primaryStage = stage;

        ConnectPage page = new ConnectPage();
        Scene scene = new Scene(page.getView(), 400, 300);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setTitle("Mentimeter - Bejelentkezés");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Az alkalmazás fő metódusa, amely elindítja a JavaFX alkalmazást.
     *
     * @param args parancssori argumentumok (jelenleg nem használatosak)
     */
    public static void main(String[] args) {
        launch(args);
    }
}