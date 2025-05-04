package hu.ppke.itk.tonyo.frontend;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class cliens extends Application {

    private Scene homeScene;
    private Scene loginScene;
    private Scene registerScene;

    @Override
    public void start(Stage primaryStage) {
        HomePage homePage = new HomePage(primaryStage);
        homeScene = new Scene(homePage.getPane(), 300, 220);

        LoginPage loginPage = new LoginPage(primaryStage);
        loginScene = new Scene(loginPage.getPane(), 300, 200);

        // Regisztrációs oldal
        RegisterPage registerPage = new RegisterPage(primaryStage);
        registerScene = new Scene(registerPage.getPane(), 300, 250);

        // Kezdő nézet: kezdőoldal
        primaryStage.setScene(homeScene);
        primaryStage.setTitle("Kezdőoldal");
        primaryStage.show();

        // Nézetváltás beállítása
        homePage.setSwitchAction(
                () -> {
                    primaryStage.setScene(loginScene);
                    primaryStage.setTitle("Bejelentkezés");
                },
                () -> {
                    primaryStage.setScene(registerScene);
                    primaryStage.setTitle("Regisztráció");
                }
        );
        loginPage.setSwitchAction(
                () -> {
            primaryStage.setScene(registerScene);
            primaryStage.setTitle("Regisztráció");
        },
                () -> {
                    primaryStage.setScene(homeScene);
                    primaryStage.setTitle("Kezdőoldal");
                }


        );
        registerPage.setSwitchAction(() -> {
            primaryStage.setScene(loginScene);
            primaryStage.setTitle("Bejelentkezés");
        }, () -> {
            primaryStage.setScene(homeScene);
            primaryStage.setTitle("Kezdőoldal");
        }
        );
    }

    public static void main(String[] args) {
        launch();
    }
}