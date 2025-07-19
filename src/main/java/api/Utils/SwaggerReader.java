package api.Utils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Utility class for parsing Swagger/OpenAPI specifications.
 * This class provides methods to read and parse Swagger documentation to extract API details,
 * such as endpoints, request methods, and request body specifications.
 */
public class SwaggerReader {


    /**
     * Fetches the Swagger JSON specification from a given URL.
     *
     * <p>This method establishes an HTTP connection to the provided URL, sends a GET
     * request, and reads the response from the server. If the response code is 200
     * (HTTP_OK), the method reads the response body and returns it as a string.
     * If the response code is not 200, it logs the response code and returns null.</p>
     *
     * @param swaggerUrl The URL from which to fetch the Swagger JSON specification.
     * @return A string containing the raw Swagger JSON if the request is successful
     *         (HTTP_OK), or null if the request fails or the response code is not
     *         200.
     */
    public static String fetchSwaggerJson(String swaggerUrl) {
        StringBuilder response = new StringBuilder();
        try {
            URL url = new URL(swaggerUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Check if the response code is 200 (HTTP_OK)
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.out.println("Response Code: " + conn.getResponseCode());
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    /**
     * Reads a Swagger/OpenAPI specification from a provided URL or file path
     * and extracts endpoint details such as HTTP operations and request bodies.
     *
     * @param swaggerUrl The URL or file path to the Swagger/OpenAPI specification
     */
    public static void readSwagger(String swaggerUrl) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult result = parser.readLocation(swaggerUrl, null, null);
        OpenAPI openAPI = result.getOpenAPI();

        if (openAPI == null) {
            String swaggerJson = fetchSwaggerJson(swaggerUrl);
            System.out.println("Failed to parse Swagger file at: " + swaggerJson);
            if (result.getMessages() != null && !result.getMessages().isEmpty()) {
                System.out.println("Messages: " + result.getMessages());
            }
            return;
        }

        System.out.println("Successfully parsed Swagger file: " + swaggerUrl);
        System.out.println("Available endpoints and operations:");

        // Iterate over all paths in the Swagger specification
        openAPI.getPaths().forEach((path, pathItem) -> {
            System.out.println("Path: " + path);

            // Check for POST operation
            if (pathItem.getPost() != null) {
                System.out.println("  POST Operation detected for path: " + path);

                RequestBody requestBody = pathItem.getPost().getRequestBody();
                if (requestBody != null && requestBody.getContent() != null) {
                    Content content = requestBody.getContent();
                    MediaType mediaType = content.get("application/json"); // Assuming JSON request body
                    if (mediaType != null) {
                        Schema<?> schema = mediaType.getSchema();
                        if (schema != null) {
                            System.out.println("  Request Schema: " + schema.getName());
                            // Extract and print schema properties
                            Map<String, Schema> properties = schema.getProperties();
                            if (properties != null) {
                                System.out.println("  Request Body Properties:");
                                properties.forEach((key, value) -> {
                                    System.out.println("    - " + key + ": " + value.getType());
                                });
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Main method demonstrating how to parse a Swagger specification.
     * Replace `swaggerUrl` with the actual URL or file path to your Swagger/OpenAPI specification.
     *
     * @param args Command-line arguments
     */
    public static void main(String[] args) {
        String swaggerUrl = "https://sogrow-app-dev-weu.azurewebsites.net/swagger/v2/swagger.json"; // Replace with actual Swagger URL
        readSwagger(swaggerUrl);
    }
}