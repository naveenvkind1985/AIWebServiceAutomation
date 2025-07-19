package api;

import io.restassured.response.Response;

public class RestUtils {
    public static void checkResponse(Response response, int statusCode, String BASE_URL) {
        String msg = null;
        if (response.getStatusCode() != statusCode) {
            msg = String.format("Error in response: Expected Status code:"
                    + statusCode + "Actual Status code:" + response.getStatusCode() + ", Response: "
                    + response.asPrettyString() + ", Request URL: " + BASE_URL + ", Possible causes: 1. Check secrets, 2. Check logs: Isolate one test and assess logs 3. Check URL and method call type. 4. Check header values and body");

        }
        System.out.println(msg);
    }
}
