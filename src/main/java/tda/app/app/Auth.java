package tda.app.app;

public class Auth {
    public static final String TOKEN = "TDA_LECTURER_TOKEN";

    public static boolean isValid(String token) {
        return TOKEN.equals(token);
    }

    public static boolean isBearerValid(String authorizationHeader) {
        if (authorizationHeader == null) return false;
        String expected = "Bearer " + TOKEN;
        return expected.equals(authorizationHeader);
    }
}
