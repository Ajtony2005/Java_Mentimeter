package hu.ppke.itk.tonyo.frontend.pages;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hu.ppke.itk.tonyo.frontend.cliens;
import hu.ppke.itk.tonyo.frontend.App;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class ResultPage {
    private final VBox view;
    private final Pane resultsPane;
    private final Label errorLabel;
    private final cliens Cliens;

    public ResultPage() {
        Cliens = App.getCliens();
        Cliens.setActivePage(this);

        view = new VBox(15);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(20));

        Label titleLabel = new Label("Eredmények");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        resultsPane = new Pane();
        resultsPane.setPrefSize(500, 300);

        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");

        view.getChildren().addAll(titleLabel, resultsPane, errorLabel);
    }

    public VBox getView() {
        return view;
    }

    public void setResultsData(JsonObject results, String type) {
        resultsPane.getChildren().clear();
        Text text = new Text();
        switch (type) {
            case "SZO_FELHO":
                JsonArray words = results.get("words").getAsJsonArray();
                StringBuilder wordCloudText = new StringBuilder("Szófelhő eredmények:\n");
                words.forEach(word -> {
                    JsonObject w = word.getAsJsonObject();
                    wordCloudText.append(w.get("word").getAsString()).append(": ").append(w.get("count").getAsInt()).append("\n");
                });
                text.setText(wordCloudText.toString());
                break;
            case "TOBBVALASZTOS":
                JsonArray options = results.get("options").getAsJsonArray();
                StringBuilder mcText = new StringBuilder("Többválasztós eredmények:\n");
                options.forEach(option -> {
                    JsonObject opt = option.getAsJsonObject();
                    mcText.append(opt.get("option").getAsString()).append(": ").append(opt.get("count").getAsInt()).append("\n");
                });
                text.setText(mcText.toString());
                break;
            case "SKALA":
                text.setText("Skála átlag: " + results.get("average").getAsDouble());
                break;
        }
        resultsPane.getChildren().add(text);
    }

    public void handleServerMessage(JsonObject message) {
        if (message == null || !message.has("event")) {
            showError("Hibás szerver válasz: hiányzó 'event' kulcs.");
            return;
        }

        String event = message.get("event").getAsString();
        if (event.equals("results_update")) {
            String pollType = message.has("pollType") ? message.get("pollType").getAsString() : "";
            setResultsData(message.get("results").getAsJsonObject(), pollType);
        } else if (event.equals("status_update")) {
            String newStatus = message.has("newStatus") ? message.get("newStatus").getAsString() : "ismeretlen";
            showError("Szavazás állapota frissült: " + newStatus);
        }
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }
}