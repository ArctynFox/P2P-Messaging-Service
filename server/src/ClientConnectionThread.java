import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.Socket;
import java.sql.Connection;

//thread that handles all actions a connecting client should need
public class ClientConnectionThread extends Thread {
    Socket socket;
    Connection db;
    int threadID;
    DataOutputStream out;
    BufferedReader in;

    ClientConnectionThread(Socket socket, Connection db, int id) {
        this.socket = socket;
        this.db = db;
        this.threadID = id;
    }

    @Override
    public void run() {
        //perform some sort of handshake to confirm this is a client of our software and not some random connection

        //switch statement that handles different actions that the client could request
        //such as getting a user hash, updating their own user IP entry, or obtaining the IP of a given user hash
    }
}
