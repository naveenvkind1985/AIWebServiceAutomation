package models;

import api.Utils.ConfigReader;
import api.Utils.SSLUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * The StepDefinitionGenerator class automatically generates step definitions,
 * payload JSON files, and feature files based on a Swagger/OpenAPI specification.
 */
public class StepDefinitionGenerator {
    // Configurable paths from config.properties
    static final String FEATURES_DIR = ConfigReader.getProperty("features");
    static final String STEP_DEF_DIR = ConfigReader.getProperty("stepDefinition");
    static final String PAYLOAD_DIR = ConfigReader.getProperty("payloads");
    static final String BASE_URL = ConfigReader.getProperty("baseurl");
    static final String SWAGGER_URL = ConfigReader.getProperty("swaggerUrl");
    static final String ENDPOINTS_CONFIG = "src/main/resources/config.properties"; // Specify your properties file path

    private static Properties endpoints = new Properties(); // Store endpoints from config.properties

    static {
        try (FileInputStream input = new FileInputStream(ENDPOINTS_CONFIG)) {
            endpoints.load(input);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to load endpoints from config file: " + e.getMessage());
        }
    }

    /**
     * Main method that coordinates Swagger parsing, payload, feature, and step definition generation.
     *
     * @param swaggerUrl The URL or local path to the Swagger/OpenAPI specification.
     */
    public static void processAndGenerate(String swaggerUrl) {
        System.out.println("[INFO] Starting Step Definition generation...");
        try {
            SSLUtils.disableSSLValidation(); // Disable SSL validation for the generation process
            OpenAPI openAPI = parseSwagger(swaggerUrl);
            if (openAPI == null) {
                throw new IllegalStateException("Failed to parse Swagger file.");
            }
            System.out.println("[INFO] Parsed Swagger/OpenAPI Document successfully.");
            saveEndpointsToConfig(openAPI);
            generatePayloadFiles(openAPI); // Generate payload files based on OpenAPI specification
            generatePayloadsFromConfig(); // Generate payloads based on endpoints
            generateFeatureFiles(openAPI); // Generate feature files based on OpenAPI specification
            generateCommonStepDefinitionFile(); // Generate common step definitions
            System.out.println("[INFO] Step Definitions Generated Successfully!");
        } catch (Exception e) {
            System.out.println("[ERROR] Failed during the generation process: " + e.getMessage());
        }
    }

    /**
     * Parses the Swagger/OpenAPI specification from the given URL.
     *
     * @param swaggerUrl The URL of the Swagger/OpenAPI spec to parse.
     * @return An OpenAPI object representing the parsed specification.
     */
    private static OpenAPI parseSwagger(String swaggerUrl) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        SwaggerParseResult result = parser.readLocation(swaggerUrl, null, null);
        if (result == null || result.getOpenAPI() == null) {
            System.out.println("[ERROR] Failed to parse the Swagger spec from " + swaggerUrl);
            return null;
        }
        return result.getOpenAPI();
    }

    // --------------------------------------------------------------------------
    // STEP 1: Save Endpoints to Properties File
    // --------------------------------------------------------------------------

    /**
     * Saves the API endpoints to the specified properties file from the OpenAPI specification.
     *
     * @param openAPI The OpenAPI object containing the API paths.
     */
    private static void saveEndpointsToConfig(OpenAPI openAPI) {
        Properties properties = new Properties();
        // Load existing properties from the file
        try {
            Path configPath = Paths.get(ENDPOINTS_CONFIG);
            if (Files.exists(configPath)) {
                try (FileInputStream input = new FileInputStream(configPath.toFile())) {
                    properties.load(input);
                    System.out.println("[INFO] Loaded existing endpoints from configuration file: " + ENDPOINTS_CONFIG);
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to load existing endpoints from config file: " + e.getMessage());
            return; // Exit if we can't load existing properties
        }

        // Iterate through paths to collect endpoints
        openAPI.getPaths().forEach((path, pathItem) -> {
            // Clean the path for naming convention
            String cleanedPath = path.replaceAll("[/{ }]", ""); // Remove slashes and braces
            cleanedPath = cleanedPath.replace("-", ""); // Remove dashes
            cleanedPath = cleanedPath.replace("_", ""); // Remove underscores
            // Format to CamelCase
            String endpointKey = toCamelCase(cleanedPath); // Convert to CamelCase format
            if (!properties.containsKey(endpointKey)) { // Only add if it doesn't already exist
                properties.setProperty(endpointKey, path); // Save the endpoint path
                System.out.println("[INFO] Adding endpoint to properties: " + endpointKey + " = " + path);
            } else {
                System.out.println("[INFO] Endpoint already exists, skipping: " + endpointKey);
            }
        });

        // Save the updated properties back to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ENDPOINTS_CONFIG))) {
            properties.store(writer, "Auto-generated API Endpoints");
            System.out.println("[INFO] Endpoints saved to configuration file: " + ENDPOINTS_CONFIG);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to save endpoints to config file: " + e.getMessage());
        }
    }

    // Helper method to convert string to CamelCase
    private static String toCamelCase(String input) {
        StringBuilder camelCaseString = new StringBuilder();
        for (String part : input.split("[ _-]+")) { // Split by spaces, underscores, or dashes
            if (part.length() > 0) {
                String capitalizedPart = part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase();
                camelCaseString.append(capitalizedPart);
            }
        }
        return camelCaseString.toString();
    }

    // --------------------------------------------------------------------------
    // STEP 2: Generate Payload Files
    // --------------------------------------------------------------------------

    /**
     * Generates payload files for all POST and PUT requests defined in the endpoints from the configuration file.
     * <p>
     * This method reads endpoints stored in the configuration file and generates corresponding payload files
     * based on their definitions in the OpenAPI specification.
     */
    private static void generatePayloadsFromConfig() {
        Properties properties = new Properties();
        // Load existing endpoints from the properties file
        try (FileInputStream input = new FileInputStream(ENDPOINTS_CONFIG)) {
            properties.load(input);
            System.out.println("[INFO] Loaded endpoints from configuration file: " + ENDPOINTS_CONFIG);
            properties.forEach((key, value) -> {
                System.out.println("[DEBUG] Generating payload for endpoint: " + value.toString()); // Print the endpoint before processing
                generatePayloadForEndpoint(value.toString()); // Call method with endpoint
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to load endpoints from config file: " + e.getMessage());
        }
    }

    /**
     * Generates a payload file for a given endpoint by parsing the Swagger/OpenAPI specification,
     * thus ensuring that proper request body structures are created based on defined schemas.
     *
     * @param endpoint The API endpoint for which to generate the payload file.
     */
    private static void generatePayloadForEndpoint(String endpoint) {
        try {
            OpenAPI openAPI = parseSwagger(SWAGGER_URL);
            if (openAPI == null) {
                throw new IllegalStateException("No OpenAPI object available for endpoint: " + endpoint);
            }

            // Check if the endpoint exists in the OpenAPI paths and log
            PathItem pathItem = openAPI.getPaths().get(endpoint);
            if (pathItem == null) {
                System.out.println("[INFO] No path item found for endpoint: " + endpoint);
                return;
            }

            // Log details of available methods and their tags
            if (pathItem.getPost() != null) {
                System.out.println("[DEBUG] Found POST method for endpoint: " + endpoint);
                System.out.println("[DEBUG] Tags: " + pathItem.getPost().getTags());
                System.out.println("[DEBUG] Summary: " + pathItem.getPost().getSummary());
                System.out.println("[DEBUG] Operation ID: " + pathItem.getPost().getOperationId());

                generatePayloadFile(endpoint, "POST", pathItem.getPost());
            }
            if (pathItem.getPut() != null) {
                System.out.println("[DEBUG] Found PUT method for endpoint: " + endpoint);
                generatePayloadFile(endpoint, "PUT", pathItem.getPut());
            }

        } catch (Exception e) {
            System.out.println("[ERROR] Failed to generate payload for " + endpoint + ": " + e.getMessage());
        }
    }

    /**
     * Generates payload files for all POST and PUT requests defined in the endpoints from the OpenAPI specification.
     *
     * @param openAPI The OpenAPI object containing the API definitions.
     */
    private static void generatePayloadFiles(OpenAPI openAPI) {
        try {
            Path payloadDirPath = Paths.get(PAYLOAD_DIR);
            if (!Files.exists(payloadDirPath)) {
                Files.createDirectories(payloadDirPath);
                System.out.println("[INFO] Payloads directory created: " + PAYLOAD_DIR);
            }
            openAPI.getPaths().forEach((path, pathItem) -> {
                if (pathItem.getPost() != null) {
                    generatePayloadFile(path, "POST", pathItem.getPost());
                }
                if (pathItem.getPut() != null) {
                    generatePayloadFile(path, "PUT", pathItem.getPut());
                }
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to generate payloads: " + e.getMessage());
        }
    }

    /**
     * Generates a JSON payload file for a given operation and saves it in the `payloads` directory.
     *
     * @param apiPath   The API path for which the payload is generated.
     * @param method    The HTTP method (POST or PUT).
     * @param operation The Operation object containing the schema for the request body.
     */
    private static void generatePayloadFile(String apiPath, String method, Operation operation) {
        try {
            // Check if the request body exists
            if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
                System.out.println("[INFO] No request body available for " + method + " " + apiPath + ", skipping.");
                return; // Exit if no request body
            }

            // Extract the media type (application/json)
            MediaType mediaType = operation.getRequestBody().getContent().get("application/json");
            if (mediaType == null || mediaType.getSchema() == null) {
                System.out.println("[INFO] No media type or schema available for " + method + " " + apiPath + ", skipping.");
                return; // Exit if no media type or schema
            }

            // Generate the payload filename by replacing slashes with underscores
            String sanitizedFileName = method + "_" + apiPath.replace("/", "_").replace("{", "").replace("}", "") + ".json";
            Path payloadPath = Paths.get(PAYLOAD_DIR, sanitizedFileName);

            // Prevent overwriting existing files
            if (Files.exists(payloadPath)) {
                System.out.println("[INFO] Payload file already exists, skipping: " + payloadPath);
                return; // Exit if file already exists
            }

            // **New Code**: Generate the payload JSON based on the schema
            String payloadJson = generateJsonFromSchema(mediaType.getSchema());
            if (payloadJson.isEmpty()) { // Check for empty JSON
                System.out.println("[ERROR] Generated JSON is empty for " + apiPath);
                return; // Exit if generated JSON is empty
            }

            // Save the JSON payload to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(payloadPath.toFile()))) {
                System.out.println("[INFO] Payload content: " + payloadJson);
                writer.write(payloadJson); // Write the generated JSON to the file
            }
            System.out.println("[INFO] Payload generated: " + payloadPath); // Log the successful generation
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to generate payload for " + apiPath + ": " + e.getMessage());
        }
    }

    /**
     * Generates a JSON string from a Schema object.
     *
     * @param schema The Schema object containing properties for which to generate JSON.
     * @return A JSON string representation of the schema.
     */
    private static String generateJsonFromSchema(Schema<?> schema) {
        if (schema.get$ref() != null) {
            String refSchemaName = schema.get$ref().substring(schema.get$ref().lastIndexOf("/") + 1);
            System.out.println("[DEBUG] Resolving reference schema: " + refSchemaName);
            try {
                OpenAPI openAPI = parseSwagger(SWAGGER_URL); // Ensure we are fetching the full OpenAPI document

                // **New Code**: Check if openAPI is null before proceeding
                if (openAPI == null) {
                    System.out.println("[ERROR] Failed to retrieve OpenAPI document.");
                    return "{}"; // Return empty JSON object if openAPI is not valid
                }

                // Fetch the referenced schema from the OpenAPI components
                Schema<?> referencedSchema = openAPI.getComponents().getSchemas().get(refSchemaName);
                if (referencedSchema == null) {
                    System.out.println("[ERROR] Referenced schema " + refSchemaName + " not found.");
                    return "{}"; // Return an empty JSON object if the referenced schema does not exist
                }
                return generateJsonFromSchema(referencedSchema); // Recursively generate JSON for the referenced schema
            } catch (Exception e) {
                System.out.println("[ERROR] Failed to resolve referenced schema: " + e.getMessage());
                return "{}"; // Return empty JSON object if something goes wrong
            }
        }

        // Continue generating JSON for the schema if not a reference
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");

        // **New Code**: Get the properties of the current schema
        Map<String, Schema> properties = schema.getProperties();
        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                String exampleValue;

                // **New Code**: Use the example value if available
                if (entry.getValue().getExample() != null) {
                    exampleValue = entry.getValue().getExample().toString();
                } else {
                    // **New Code**: Default value for example generation based on type
                    exampleValue = getDefaultExampleValue(entry.getValue().getType());
                }

                if (exampleValue != null) {
                    jsonBuilder.append("  \"").append(entry.getKey()).append("\": ").append(exampleValue).append(",\n");
                }
            }

            // Remove the last comma and newline
            if (jsonBuilder.length() >= 2) {
                jsonBuilder.setLength(jsonBuilder.length() - 2); // Remove last comma
            }
        } else {
            System.out.println("[INFO] No properties available for schema, resulting in empty JSON.");
        }
        jsonBuilder.append("\n}"); // Close the JSON object
        return jsonBuilder.toString(); // Return the constructed JSON string
    }

    // Method to provide default example values based on schema type
    private static String getDefaultExampleValue(String type) {
        switch (type) {
            case "string":
                return "\"Sample String\""; // return default string
            case "integer":
                return "0"; // return default integer
            case "boolean":
                return "false"; // return default boolean
            case "array":
                return "[]"; // return default empty array
            case "object":
                return "{}"; // return default empty object
            default:
                return "\"Unknown Type\""; // Handle unknown types
        }
    }

    // --------------------------------------------------------------------------
    // STEP 3: Generate Feature Files
    // --------------------------------------------------------------------------

    /**
     * Generates Gherkin-style feature files for all API paths defined in the OpenAPI spec,
     * including parameterized examples for testing.
     *
     * @param openAPI The OpenAPI object containing the API specifications.
     */
    private static void generateFeatureFiles(OpenAPI openAPI) {
        try {
            Path featureDirPath = Paths.get(FEATURES_DIR);
            if (!Files.exists(featureDirPath)) {
                Files.createDirectories(featureDirPath);
                System.out.println("[INFO] Created feature files directory: " + FEATURES_DIR);
            }
            openAPI.getPaths().forEach((path, pathItem) -> {
                try {
                    StringBuilder featureContent = new StringBuilder("Feature: Auto-generated test cases for " + path + "\n\n");
                    // Generate scenarios for supported operations
                    if (pathItem.getGet() != null) {
                        addScenarioForOperation(featureContent, "GET", pathItem.getGet(), path);
                    }
                    if (pathItem.getPost() != null) {
                        addScenarioForOperation(featureContent, "POST", pathItem.getPost(), path);
                    }
                    if (pathItem.getPut() != null) {
                        addScenarioForOperation(featureContent, "PUT", pathItem.getPut(), path);
                    }
                    if (pathItem.getDelete() != null) {
                        addScenarioForOperation(featureContent, "DELETE", pathItem.getDelete(), path);
                    }

                    // Generate feature file name
                    String sanitizedPath = path.replace("/", "")
                            .replace("-", " ")
                            .replace("{", "")
                            .replace("}", "") + ".feature"; // Using space instead of dash for splitting
                    String camelCasePath = toCamelCase(sanitizedPath);
                    Path filePath = Paths.get(featureDirPath.toString(), camelCasePath);

                    // Check if the feature file already exists
                    if (Files.exists(filePath)) {
                        System.out.println("[INFO] Feature file already exists, skipping: " + filePath);
                    } else {
                        // Write the feature content to a file
                        String formattedContent = formatGherkinContent(featureContent.toString());
                        //Files.write(filePath, featureContent.toString().getBytes());
                        Files.write(filePath, formattedContent.getBytes());
                        System.out.println("[INFO] Feature file created: " + filePath);

                       // GherkinFormatter.formatGherkinFeatureFile(filePath.toString(), filePath.toString());
                    }
                } catch (Exception e) {
                    System.out.println("[ERROR] Failed to generate feature file for path " + path + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to generate feature files: " + e.getMessage());
        }
    }

    /**
     * Formats the Gherkin content to ensure it meets the desired output structure.
     *
     * @param content The raw Gherkin content as a string to be formatted.
     * @return The formatted Gherkin content as a string.
     */
    private static String formatGherkinContent(String content) {
        StringBuilder formatted = new StringBuilder();
        String[] lines = content.split("\n");

        for (String line : lines) {
            line = line.trim(); // Trim whitespace

            if (line.isEmpty()) {
                continue; // Skip empty lines
            }

            // Feature title
            if (line.startsWith("Feature:")) {
                formatted.append(line).append("\n"); // Feature title without alteration
            }
            // Scenario Outline
            else if (line.startsWith("Scenario Outline:")) {
                formatted.append("\n").append(line).append("\n"); // Append a new line before Scenario Outline
            }
            // Steps of the Scenario
            else if (line.startsWith("Given") || line.startsWith("When") || line.startsWith("Then") || line.startsWith("And")) {
                formatted.append("  ").append(line).append("\n"); // Indentation for steps
            }
            // Examples header
            else if (line.startsWith("Examples:")) {
                formatted.append("\n  ").append(line).append("\n"); // Indent Examples header and add a new line
            }
            // Example rows
            else if (line.contains("|")) {
                formatted.append("  ").append(line).append("\n"); // Indent example rows further
            }
            // Other lines that don't fit those criteria
            else {
                formatted.append(line).append("\n"); // Default appending
            }
        }

        return formatted.toString();
    }

    /**
     * Adds a scenario for API testing to the feature content, handling dynamic parameters.
     *
     * @param featureContent The StringBuilder used for accumulating feature content.
     * @param method         The HTTP method type (GET, POST, PUT, DELETE).
     * @param operation      The Operation object containing the request and response details.
     * @param path           The API path to be tested.
     */

    private static void addScenarioForOperation(StringBuilder featureContent, String method, Operation operation, String path) {
        // Create a key from the path to match properties
        String cleanedPathKey = path.replace("/", "").replace("{", "").replace("}", "").replace("-", "").toLowerCase(); // Cleaned path for the key
        String endpointKey = cleanedPathKey.substring(0, 1).toUpperCase() + cleanedPathKey.substring(1); // Capitalized first letter

        // Check if the endpoint key exists in the properties
        String endpointReference = endpoints.getProperty(endpointKey); // Retrieve variable name from properties
        // Add a newline before each scenario outline
        featureContent.append("\n"); // Newline before the scenario outline

        // Declare a list to keep track of parameter names
        List<String> paramNames = new ArrayList<>();


        // Prepare the formatted scenario line
        StringBuilder scenarioLine;

// Preferred to use mapped endpoint from properties
        if (endpointReference != null) {
            // If endpointReference is found, use the capitalized endpoint key with quotes
            scenarioLine = new StringBuilder("Given I set the endpoint to \"").append(endpointKey).append("\"");
        } else {
            // Handle case where the endpoint does not exist
            scenarioLine = new StringBuilder(" Given I set the endpoint to \"").append(endpointKey).append("\""); // Using the raw path
        }

// Check if the path includes any placeholders
        if (path.contains("{")) {
            String[] parts = path.split("/");
            for (String part : parts) {
                if (part.startsWith("{") && part.endsWith("}")) {
                    String paramName = part.substring(1, part.length() - 1); // Extract name without braces
                    scenarioLine.append(" as \"").append(paramName).append("\""); // Append the parameter as needed without angle brackets
                    paramNames.add(paramName); // Collect parameter names for Examples section
                }
            }
        }

// Append the scenario outline
        featureContent.append(" \n Scenario Outline: Verify the response for ")
                .append(method).append(" request to ")
                .append(endpointReference != null ? endpointKey : path) // Use endpointKey if exists, otherwise use raw path
                .append("\n")
                .append(scenarioLine).append("\n"); // Append the complete scenario line


        // Define request action based on HTTP method
        if ("GET".equalsIgnoreCase(method)) {
            // For GET request, no body is needed
            featureContent.append("    When I send a request without a body\n");
        } else {
            // For POST and PUT we need a request with a JSON file
            featureContent.append("    When I send a request with the body from \"<JSON File>\"\n"); // Updated
        }

        // Common response checks
        featureContent.append("    Then I receive a \"<statusCode>\" response\n")
                .append("    And the response body should contain the description: \"<responseDesc>\"\n\n");

        // Add examples for the scenario outline
        featureContent.append("  Examples:\n");

        // **New code**: Conditional check for POST and PUT methods to include JSON file in examples
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            featureContent.append(" | JSON file   | statusCode | responseDesc   |");
        } else {
            featureContent.append("  | statusCode | responseDesc  |");
        }

        // Check if any parameters exist and append them to the header
        if (!paramNames.isEmpty()) {
            for (String paramName : paramNames) {
                featureContent.append(" ").append(paramName).append(" |"); // Append parameter name directly without angle brackets
            }
        }

        // Prepare examples based on the responses
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((statusCode, response) -> {
                String responseDesc = response.getDescription();
                responseDesc = responseDesc.replaceAll("[\\r\\n]+", " "); // Flatten newlines to spaces

                // Create an entry for the example row for all methods
                StringBuilder exampleRow = new StringBuilder("\n    | ");
                // Only create an entry for the example row for POST and PUT methods
                if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                    // Include JSON file for POST and PUT methods
                    exampleRow.append("request_body_").append(method.toLowerCase()).append(".json | ")
                            .append(statusCode).append(" | ")
                            .append(responseDesc).append(" |"); // Ensure this value is on one line
                } else {
                    // Omit JSON file for GET or others without it
                    exampleRow.append(statusCode).append(" | ")
                            .append(responseDesc).append(" |"); // Only status and response description
                }


                // Append parameter names to the example row for dynamic scenarios
                for (String paramName : paramNames) {
                    exampleRow.append(" ").append(paramName).append(" |"); // Add parameter name directly
                }
                // Append the example row to the feature content
                featureContent.append(exampleRow);

            });
        }
    }

    // --------------------------------------------------------------------------
    // STEP 4: Generate Common Step Definition File
    // --------------------------------------------------------------------------

    /**
     * Creates the CommonStepDefinitions.java file containing reusable steps for API tests.
     */
    private static void generateCommonStepDefinitionFile() {
        try {
            Path stepDefPath = Paths.get(STEP_DEF_DIR);
            if (!Files.exists(stepDefPath)) {
                Files.createDirectories(stepDefPath);
                System.out.println("[INFO] Created step definition directory: " + STEP_DEF_DIR);
            }

            // Creating CommonStepDefinitions.java
            String commonStepDefFileName = "CommonStepDefinitions.java";
            Path commonStepDefFilePath = Paths.get(STEP_DEF_DIR, commonStepDefFileName);
            if (Files.exists(commonStepDefFilePath)) {
                System.out.println("[INFO] Common step definition file already exists, skipping.");
            } else {
                // Using StringBuilder to create CommonStepDefinitions class
                StringBuilder writerContent = new StringBuilder();
                writerContent.append("package stepDefinition;\n\n")
                        .append("import io.cucumber.java.en.*;\n")
                        .append("import io.restassured.RestAssured;\n")
                        .append("import io.restassured.response.Response;\n")
                        .append("import io.restassured.specification.RequestSpecification;\n")
                        .append("import org.testng.Assert;\n")
                        .append("import java.nio.file.Files;\n")
                        .append("import java.nio.file.Paths;\n")
                        .append("import java.io.IOException;\n\n")
                        .append("import api.Utils.ConfigReader;\n")
                        .append("import api.auth0.AuthTokenApi;\n\n") // Import AuthTokenApi
                        .append("public class CommonStepDefinitions {\n\n")
                        .append("    private String fullEndpoint; // Global variable for the endpoint\n")
                        .append("    private Response response; // Store response as a class variable\n")
                        .append("    private final String baseUrl = ConfigReader.getProperty(\"baseurl\"); // Read base URL from properties\n")
                        .append("    private String authToken; // Declare the authToken\n")
                        .append("    private final AuthTokenApi authTokenApi = new AuthTokenApi(); // Instance of AuthTokenApi\n")
                        .append("    private final String apiVersion = \"?api-version=1.0\";\n\n")
                        .append("    private void ensureAuthTokenIsValid() {\n")
                        .append("        if (authToken == null || authToken.isEmpty()) { // Check if the token is absent\n")
                        .append("            authToken = authTokenApi.generateAuthToken(); // Call to generate a new token\n")
                        .append("            System.out.println(\"[INFO] Auth token generated: \" + authToken);\n")
                        .append("        }\n")
                        .append("    }\n\n") // End of ensureAuthTokenIsValid
                        // Given step definition
                        .append("    @Given(\"I set the endpoint to {string} as {string}\")\n")
                        .append("    public void givenISetTheEndpointWithParams(String endpointKey, String paramValue) {\n")
                        .append("        String endpoint = ConfigReader.getProperty(endpointKey);\n")
                        .append("        if (endpoint == null) {\n")
                        .append("            throw new RuntimeException(\"[ERROR] Endpoint value is null for key: \" + endpointKey);\n")
                        .append("        }\n")
                        .append("        if (baseUrl == null) {\n")
                        .append("            throw new RuntimeException(\"[ERROR] Base URL is not set in config.properties.\");\n")
                        .append("        }\n")
                        .append("        // Replace the parameter in the endpoint string accordingly\n")
                        .append("        // Assumes that the endpoint has named placeholders matching the parameter names\n")
                        .append("        String fullEndpoint = baseUrl + endpoint.replace(\"{\" + paramValue + \"}\", paramValue);\n") // Utilize dynamic replacement
                        .append("        System.out.println(\"[INFO] API Endpoint set to: \" + fullEndpoint);\n")
                        .append("        RestAssured.baseURI = fullEndpoint; // Optional, if you want to use it for requests\n")
                        .append("       System.out.println(\"[INFO] API Endpoint set to: \" + fullEndpoint);\n")
                        .append("    }\n\n")

                        .append("    @Given(\"I set the endpoint to {string}\")\n")
                        .append("    public void givenISetTheEndpoint(String endpointKey) {\n")
                        .append("        String endpoint = ConfigReader.getProperty(endpointKey);\n")
                        .append("        if (endpoint == null) {\n")
                        .append("            throw new RuntimeException(\"[ERROR] Endpoint value is null for key: \" + endpointKey);\n")
                        .append("        }\n")
                        .append("        if (baseUrl == null) {\n")
                        .append("            throw new RuntimeException(\"[ERROR] Base URL is not set in config.properties.\");\n")
                        .append("        }\n")
                        .append("        fullEndpoint = baseUrl + endpoint;\n")
                        .append("        System.out.println(\"[INFO] API Endpoint set to: \" + fullEndpoint);\n")
                        .append("        RestAssured.baseURI = fullEndpoint;\n")
                        .append("       System.out.println(\"[INFO] API Endpoint set to: \" + fullEndpoint);\n")
                        .append("    }\n\n")
                        // When step for GET requests
                        .append("    @When(\"I send a request without a body\")\n")
                        .append("    public void whenISendARequestWithoutBody() {\n")
                        .append("        ensureAuthTokenIsValid(); // Ensure token is valid before request\n")
                        .append("         RequestSpecification request = RestAssured.given()\n")
                        .append("         .relaxedHTTPSValidation()\n")
                        .append("         .header(\"Authorization\", \"Bearer \" + authToken)\n")
                        .append("         .header(\"Content-Type\", \"application/json\");\n")
                        .append("          response = request.get(fullEndpoint+apiVersion); // Perform GET request\n")
                        .append("        System.out.println(\"[INFO] GET request sent \" + response.getBody());\n")
                        .append("        System.out.println(\"[INFO] GET request sent \" + response.getStatusCode());\n")
                        .append("    }\n\n")
                        // When step for requests with JSON body
                        .append("    @When(\"I send a request with the body from {string}\")\n")
                        .append("    public void whenISendARequestWithBodyFromJsonFile(String jsonFile) {\n")
                        .append("        ensureAuthTokenIsValid(); // Ensure token is valid before request\n")
                        .append("        try {\n")
                        .append("            String jsonBody = new String(Files.readAllBytes(Paths.get(\"path/to/json/files/\" + jsonFile)));\n")
                        .append("            RequestSpecification request = (RequestSpecification) RestAssured.given()\n")
                        .append("            .relaxedHTTPSValidation()\n")
                        .append("                .header(\"Authorization\", \"Bearer \" + authToken) // Adding the Authorization Header\n")
                        .append("                .header(\"Content-Type\", \"application/json\")\n")
                        .append("                .body(jsonBody)\n")
                        .append("                .post(fullEndpoint+apiVersion); \n")
                        .append("        } catch (IOException e) {\n")
                        .append("            System.err.println(\"Failed to read JSON file: \" + e.getMessage());\n")
                        .append("        }\n")
                        .append("    }\n\n")
                        // Then step for asserting the response status code
                        .append("    @Then(\"I receive a {string} response\")\n")
                        .append("    public void thenIReceiveAResponse(String expectedStatusCode) {\n")
                        .append("        Assert.assertEquals(Integer.parseInt(expectedStatusCode), response.getStatusCode()); // Parse expectedStatusCode to int\n")
                        .append("        System.out.println(\"[INFO] Verified response code: \" + expectedStatusCode);\n")
                        .append("    }\n\n")
                        // And step for checking response body description
                        .append("    @Then(\"the response body should contain the description: {string}\")\n")
                        .append("    public void thenResponseBodyShouldContain(String responseDesc) {\n")
                        .append("        String actualResponseBody = response.getBody().asString();\n")
                        .append("        Assert.assertTrue(actualResponseBody.contains(responseDesc), \"Expected description not found in response body.\");\n")
                        .append("        System.out.println(\"[INFO] Verified response body contains: \" + responseDesc);\n")
                        .append("    }\n\n");

                writerContent.append("}\n"); // Close the CommonStepDefinitions class

                // Write the CommonStepDefinitions to the file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(commonStepDefFilePath.toFile()))) {
                    writer.write(writerContent.toString());
                }
                System.out.println("[INFO] Common step definition file created: " + commonStepDefFilePath);
            }

            // Creating Hooks.java with provided content
            String hooksFileName = "Hooks.java"; // File name for hooks
            Path hooksFilePath = Paths.get(STEP_DEF_DIR, hooksFileName); // Path definition for hooks

            if (Files.exists(hooksFilePath)) {
                System.out.println("[INFO] Hooks file already exists, skipping.");
            } else {
                // Using StringBuilder to create Hooks class
                StringBuilder hooksWriterContent = new StringBuilder();
                hooksWriterContent.append("package stepDefinition;\n\n")
                        .append("import io.cucumber.java.Before;\n")
                        .append("import io.cucumber.java.Scenario;\n\n")
                        .append("public class Hooks {\n")
                        .append("    @Before\n")
                        .append("    public void testStart(Scenario scenario) {\n")
                        .append("    }\n")
                        .append("}\n"); // Close the Hooks class

                // Write the Hooks to the file
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(hooksFilePath.toFile()))) {
                    writer.write(hooksWriterContent.toString());
                }
                System.out.println("[INFO] Hooks file created: " + hooksFilePath);
            }

        } catch (Exception e) {
            System.out.println("[ERROR] Failed to generate common step definition file: " + e.getMessage());
        }

    }
}