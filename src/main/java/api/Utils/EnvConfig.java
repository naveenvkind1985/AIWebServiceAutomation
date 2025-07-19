package api.Utils;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    // Load the local.env file specifically
    private static final Dotenv dotenv = Dotenv.configure().filename("local.env").load();

    public static String getClientId() {
        return dotenv.get("AUTH0_CLIENT_ID"); // Ensure this key exists in your local.env file
    }

    public static String getClientSecret() {
        return dotenv.get("AUTH0_CLIENT_SECRET");
    }

    public static String getAudience() {
        return dotenv.get("AUTH0_AUDIENCE");
    }

    public static String getGrantType() {
        return dotenv.get("AUTH0_GRANT_TYPE");
    }
}