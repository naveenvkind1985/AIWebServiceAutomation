package api.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static Properties properties = new Properties();

    static {
        // Load properties file from the classpath
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Warning: Unable to find config.properties.");
            } else {
                // Load the properties into the 'properties' object
                properties.load(input);
                System.out.println("Properties loaded: " + properties); // Debug: Print loaded properties for verification
            }
        } catch (IOException e) {
            System.out.println("Error: Unable to read the Config Property file due to: " + e.getMessage());
        }
    }

    // Retrieve the property value for a given key
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    // Retrieve the endpoint using the base URL and a given path
    public static String getEndpoint(String path) {
        String baseUrl = getProperty("baseurl"); // Use "baseurl" as defined in config.properties
        if (baseUrl == null) {
            System.out.println("Error: Base URL is not set in config.properties.");
            return null; // Optionally consider throwing an exception or handling this more robustly
        }
        return baseUrl + path;
    }
}