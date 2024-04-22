package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLiteClient {
    // Database URL
    static final String DB_URL = "jdbc:sqlite:path_to_your_database_file.db";

    // Database credentials (if any)
    static final String USER = "";
    static final String PASS = "";

    public static void main(String[] args) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Establish connection to SQLite database
            conn = DriverManager.getConnection(DB_URL, USER, PASS);

            // Example: Inserting a new user into the 'users' table
            String insertUserSQL = "INSERT INTO users (hash, ip, nickname, isConnected) VALUES (?, ?, ?, ?)";
            stmt = conn.prepareStatement(insertUserSQL);
            stmt.setString(1, "user_hash");
            stmt.setString(2, "192.168.1.1");
            stmt.setString(3, "John");
            stmt.setBoolean(4, true);
            stmt.executeUpdate();

            // Example: Querying all users from the 'users' table
            String selectUsersSQL = "SELECT * FROM users";
            stmt = conn.prepareStatement(selectUsersSQL);
            rs = stmt.executeQuery();

            // Process the result set
            while (rs.next()) {
                System.out.println("User ID: " + rs.getLong("id"));
                System.out.println("Hash: " + rs.getString("hash"));
                System.out.println("IP: " + rs.getString("ip"));
                System.out.println("Nickname: " + rs.getString("nickname"));
                System.out.println("Is Connected: " + rs.getBoolean("isConnected"));
                System.out.println("--------------------------");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Close resources
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

