package first_page;

public final class SessionContext {
    private static volatile String currentUser = "Guest";

    private SessionContext() {
    }

    public static String getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(String username) {
        if (username == null || username.isBlank()) {
            currentUser = "Guest";
            return;
        }
        currentUser = username.trim();
    }
}
