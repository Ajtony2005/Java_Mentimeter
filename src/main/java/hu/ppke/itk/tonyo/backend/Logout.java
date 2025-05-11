package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonObject;

import java.util.concurrent.Callable;

/**
 * A {@code Logout} osztály a felhasználó kijelentkeztetésére szolgál egy szavazási rendszerben.
 * A {@code Callable} interfészt implementálja, így szálbiztos környezetben futtatható.
 */
public class Logout implements Callable<JsonObject> {

    private final ClientHandler clientHandler;

    /**
     * Konstruktor, amely inicializálja a kijelentkezéshez szükséges adatokat.
     *
     * @param clientHandler a kliens kezelő objektuma
     */
    public Logout(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    /**
     * Kijelentkezteti a felhasználót, visszaállítva a kliens azonosítóját.
     *
     * @return a kijelentkezés JSON válasza
     */
    @Override
    public JsonObject call() {
        JsonObject response = new JsonObject();
        response.addProperty("action", "logout");
        clientHandler.setUserId(-1);
        response.addProperty("status", "success");
        response.addProperty("message", "Sikeres kijelentkezés");
        return response;
    }
}