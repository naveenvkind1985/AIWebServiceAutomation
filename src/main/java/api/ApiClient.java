package api;

import io.restassured.RestAssured;
import io.restassured.response.Response;

public class ApiClient {

    public static Response get(String endpoint) {
        return RestAssured.get(endpoint);
    }

    public static Response post(String endpoint, String jsonPayload) {
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .body(jsonPayload)
                .post(endpoint);
    }

    public static Response put(String endpoint, String jsonPayload) {
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .body(jsonPayload)
                .put(endpoint);
    }

    public static Response delete(String endpoint) {
        return RestAssured.delete(endpoint);
    }
}