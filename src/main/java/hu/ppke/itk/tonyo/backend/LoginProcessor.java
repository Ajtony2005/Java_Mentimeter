package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonObject;

import java.util.concurrent.Callable;

/**
 * A {@code LoginProcessor} osztály a felhasználók bejelentkeztetésére szolgál egy SQLite adatbázisban.
 * A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.

 */
public class LoginProcessor implements Callable<JsonObject> {

    private final String dbName;
    private final JsonObject request;
    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a bejelentkezéshez szükséges adatokat.
     *
     * @param dbName az adatbázis neve
     * @param request a kliens JSON kérése
     * @param clientHandler a kliens kezelő objektuma
     */
    public LoginProcessor(String dbName, JsonObject request, ClientHandler clientHandler) {
        this.dbName = dbName;
        this.request = request;
        this.clientHandler = clientHandler;
    }

    /**
     * Feldolgozza a bejelentkezési kérelmet, és visszaadja a művelet eredményét.
     *
     * @return a bejelentkezés JSON válasza
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "login");
        String username = request.get("username").getAsString();
        String password = request.get("password").getAsString();

        Login login = new Login(dbName, username, password);
        Login.LoginResult result = login.call();

        response.addProperty("status", result.getStatus() == 0 ? "success" : "error");
        response.addProperty("userId", result.getUserId());
        if (result.getStatus() == 0) {
            clientHandler.setUserId(result.getUserId());
            response.addProperty("message", "Sikeres bejelentkezés");
        } else {
            response.addProperty("message", "Hibás felhasználónév vagy jelszó");
        }
        return response;
    }
}