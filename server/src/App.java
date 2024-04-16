import java.io.Console;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//root of the server application
public class App 
{
    static Connection db;

    public static void main( String[] args )
    {
        //get connection object for database
        try {
            db = getDBConnection();
        } catch (SQLException e) {
            System.out.println("Could not connect to database.");
            e.printStackTrace();
            return;
        }

        serverThread.start();
    }

    //prompts the user to enter the database credentials, returns the connection to the database on successful login
    static Connection getDBConnection() throws SQLException {
        //prompt for DB username and password
        Console cons = System.console();
        String user = cons.readLine("%s", "Username: ");
        char[] pwd = cons.readPassword("%s", "Password: ");

        //connect to the MySQL server
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/server_data", user, new String(pwd));
    }

    static Thread serverThread = new Thread() {
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                //Modify the port on the following line if you wish to use a different one.
                //This is the port that clients should be given to connect to a specific Mercury server group.
                serverSocket = new ServerSocket(52761);
            } catch (IOException e) {
                System.out.println("Could not open server socket. Please restart server.");
                e.printStackTrace();
                return;
            }

            //accept connections indefinitely not exceeding a defined number
            int maxThreads = 1000;
            while(true) {
                if(Thread.activeCount() < maxThreads) {
                    try {
                        System.out.println("Waiting for incoming connections...");
                        //accept an incoming connection and print the client's address
                        Socket socket = serverSocket.accept();

                        System.out.println("Incoming connection from " + socket.getInetAddress().getHostAddress() + ", moving connecti0on to new thread.");

                        //pass the client's socket to a new thread
                        ClientConnectionThread thread = new ClientConnectionThread(socket, db, Thread.activeCount());
                        thread.start();
                    } catch (IOException e) {
                        try {
                            serverSocket.close();
                        } catch (IOException e1) {
                            System.out.println("Could not close server on fail to accept connection. Please restart server.");
                            e1.printStackTrace();
                        }
                        System.out.println("Could not accept connection. Please restart server.");
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void interrupt() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Could not close server on interrupt.");
                e.printStackTrace();
            }
        }
    };
}

