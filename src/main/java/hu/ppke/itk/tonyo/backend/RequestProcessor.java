package hu.ppke.itk.tonyo.backend;

import com.google.gson.JsonObject;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A {@code RequestProcessor} osztály a kliens kérések feldolgozására szolgál egy szavazási
 * rendszerben. A kéréseket megfelelő {@code Callable} osztályokhoz irányítja, amelyek szálbiztos
 * környezetben futtathatók.
 */
public class RequestProcessor {

    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Feldolgozza a kliens kérését, és a megfelelő műveletet hajtja végre.
     *
     * @param dbName az adatbázis neve
     * @param request a kliens JSON kérése
     * @param clientHandler a kliens kezelő objektuma
     * @return a művelet JSON válasza
     */
    public static JsonObject processRequest(String dbName, JsonObject request, ClientHandler clientHandler) {
        String action = request.get("action").getAsString();
        Callable<JsonObject> task;

        switch (action) {
            case "register":
                task = new Registrations(dbName, request);
                break;
            case "login":
                task = new LoginProcessor(dbName, request, clientHandler);
                break;
            case "create_poll":
                task = new CreatePoll(dbName, request, clientHandler);
                break;
            case "list_polls":
                task = new ListPolls(dbName, clientHandler);
                break;
            case "list_my_polls":
                task = new ListMyPolls(dbName, clientHandler);
                break;
            case "join_poll":
                task = new JoinPolls(dbName, request, clientHandler);
                break;
            case "submit_vote":
                task = new SubmitVote(dbName, request, clientHandler);
                break;
            case "get_results":
                task = new GetResults(dbName, request);
                break;
            case "update_poll_status":
                task = new UpdatePollStatus(dbName, request);
                break;
            case "reset_poll_data":
                task = new ResetPollData(dbName, request, clientHandler);
                break;
            case "edit_poll":
                task = new EditPoll(dbName, request, clientHandler);
                break;
            case "get_poll_details":
                task = new GetPollDetails(dbName, request, clientHandler);
                break;
            case "get_poll_results":
                task = new GetPollResult(dbName, request, clientHandler);
                break;
            case "logout":
                task = new Logout(clientHandler);
                break;
            default:
                JsonObject response = new JsonObject();
                response.addProperty("status", "error");
                response.addProperty("message", "Ismeretlen művelet");
                return response;
        }

        try {
            Future<JsonObject> future = executor.submit(task);
            return future.get();
        } catch (Exception e) {
            JsonObject response = new JsonObject();
            response.addProperty("status", "error");
            response.addProperty("message", "Kérés feldolgozása sikertelen: " + e.getMessage());
            return response;
        }
    }
}