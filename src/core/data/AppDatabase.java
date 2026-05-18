package core.data;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class AppDatabase {
    private static final String DB_URL = "jdbc:sqlite:" + new File("leaderboard.db").getPath();
    private static volatile boolean initialized = false;

    private AppDatabase() {
    }

    public static synchronized void initialize() throws SQLException {
        if (initialized) {
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS leaderboard (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }
        initialized = true;
    }

    public static boolean registerUser(String username, String password) throws SQLException {
        initialize();
        String normalizedUser = normalizeUsername(username);
        if (normalizedUser == null || password == null || password.isBlank()) {
            return false;
        }
        if (usernameExists(normalizedUser)) {
            return false;
        }
        String sql = "INSERT INTO users(username, password_hash) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedUser);
            pstmt.setString(2, hashPassword(password));
            pstmt.executeUpdate();
            return true;
        }
    }

    public static boolean authenticate(String username, String password) throws SQLException {
        initialize();
        String normalizedUser = normalizeUsername(username);
        if (normalizedUser == null || password == null || password.isBlank()) {
            return false;
        }

        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedUser);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return rs.getString("password_hash").equals(hashPassword(password));
            }
        }
    }

    public static void saveScore(String username, int score) throws SQLException {
        initialize();
        String normalizedUser = normalizeUsername(username);
        if (normalizedUser == null) {
            normalizedUser = "Guest";
        }
        String sql = "INSERT INTO leaderboard(username, score) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedUser);
            pstmt.setInt(2, Math.max(0, score));
            pstmt.executeUpdate();
        }
    }

    public static List<ScoreEntry> getTopScores(int limit) throws SQLException {
        initialize();
        List<ScoreEntry> result = new ArrayList<>();
        String sql = """
                SELECT username, score, created_at
                FROM leaderboard
                ORDER BY score DESC, id ASC
                LIMIT ?
                """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new ScoreEntry(
                            rs.getString("username"),
                            rs.getInt("score"),
                            rs.getString("created_at")
                    ));
                }
            }
        }
        return result;
    }

    private static boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 24 ? trimmed.substring(0, 24) : trimmed;
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record ScoreEntry(String username, int score, String createdAt) {
    }
}
