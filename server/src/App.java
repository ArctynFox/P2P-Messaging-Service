import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class App 
{
    static Connection db;

    public static void main( String[] args )
    {
        //get connection object for database
        try {
            db = getDBConnection();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Could not connect to database.");
            e.printStackTrace();
            return;
        }

    }

    static Connection getDBConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        
        //prompt for DB username and password
        Console cons = System.console();
        String user = cons.readLine("%s", "Username: ");
        char[] pwd = cons.readPassword("%s", "Password:");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/server_data", user, new String(pwd));
    }
}

