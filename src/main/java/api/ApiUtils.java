package api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class ApiUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String loadJson(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static String parameterizeJson(String jsonString, Map<String, String> params) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            jsonString = jsonString.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return jsonString;
    }
}