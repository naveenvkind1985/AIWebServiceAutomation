package api.auth0;

import api.Utils.ConfigReader;
import api.Utils.EnvConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.restassured.RestAssured;
import io.restassured.response.Response;

public class AuthTokenApi {

    private static final String AUTH0_URL;
    private static final String CLIENT_ID;
    private static final String CLIENT_SECRET;
    private static final String AUDIENCE;
    private static final String GRANT_TYPE;

    static {
        try {
            AUTH0_URL = ConfigReader.getProperty("auth0url") + ConfigReader.getProperty("authToken");
            CLIENT_ID = EnvConfig.getClientId();
            CLIENT_SECRET = EnvConfig.getClientSecret();
            AUDIENCE = EnvConfig.getAudience();
            GRANT_TYPE = EnvConfig.getGrantType();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to initialize AuthTokenApi: " + e.getMessage());
        }
    }

    public String generateAuthToken() {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("client_id", CLIENT_ID);
            requestBody.addProperty("client_secret", CLIENT_SECRET);
            requestBody.addProperty("audience", AUDIENCE);
            requestBody.addProperty("grant_type", GRANT_TYPE);

            System.out.println("Sending request to: " + AUTH0_URL);
            Response response = RestAssured.given()
                    .relaxedHTTPSValidation()
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .post(AUTH0_URL);
            response.then().statusCode(200);
            JsonObject responseBody = JsonParser.parseString(response.getBody().asString()).getAsJsonObject();
            return responseBody.get("access_token").getAsString();
        } catch (JsonSyntaxException e) {
            System.out.println("JSON Parsing Exception: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Exception in generateAuthToken: " + e.getMessage());
            return null;
        }
    }
}