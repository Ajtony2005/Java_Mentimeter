package hu.ppke.itk.tonyo.backend;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class errorCodeLoader {
    private final Map<String, Map<String, String>> errorCodes;

    public errorCodeLoader(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new RuntimeException("Resource not found: " + resourcePath);
            }
            InputStreamReader reader = new InputStreamReader(is);
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
            errorCodes = gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load error codes");
        }
    }

    public String getMessage(String category, int code) {
        Map<String, String> categoryCodes = errorCodes.get(category);
        if (categoryCodes == null) {
            return "Unknown category";
        }
        return categoryCodes.getOrDefault(String.valueOf(code), "Unknown error");
    }
}
