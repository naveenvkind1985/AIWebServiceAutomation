package api.Utils;

import io.github.cdimascio.dotenv.Dotenv;

public class CredentialsUtil {
    private static final Dotenv dotenv = Dotenv.configure().filename("local.env").ignoreIfMissing().load();
    public static String AUDIENCE = getEnvOrElse("AUTH0_AUDIENCE");
    public static String CLIENT_ID = getEnvOrElse("AUTH0_CLIENT_ID");
    public static String CLIENT_SECRET = getEnvOrElse("AUTH0_CLIENT_SECRET");
    public static String AUTH0_URL = getEnvOrElse("AUTH0_URL");
    public static String BE_BASE_URI = getEnvOrElse("BE_BASE_URI");
    public static String BE_GRANT_TYPE = getEnvOrElse("BE_GRANT_TYPE");
    public static String BE_CLIENT_ID = getEnvOrElse("BE_CLIENT_ID");
    public static String BE_CLIENT_SECRET = getEnvOrElse("BE_CLIENT_SECRET");
    public static String BE_AUDIENCE = getEnvOrElse("BE_AUDIENCE");
    public static String BE_REALM = getEnvOrElse("BE_REALM");

    private static String getEnvOrElse(String key) {
        String value = dotenv.get(key);
        if (value == null || value.isEmpty()) {
            value = System.getProperty(key);
        }
        return value;
    }
}
