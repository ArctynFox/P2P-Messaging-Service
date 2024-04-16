import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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

    //create an identifier hash when a new user joins the network
    String generateUserHash() throws NoSuchAlgorithmException {
        //securely generate a string of 32 random characters
        String SALTCHARS = "abcdefghijklmnopqrstuvwxyz1234567890";
		StringBuilder salt = new StringBuilder();
		SecureRandom rnd = new SecureRandom();
		while (salt.length() < 32) {
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();

        //hash the string with MD5
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] digestBytes = digest.digest(saltStr.getBytes());
        BigInteger signum = new BigInteger(1, digestBytes);
        String hash = signum.toString(16);
        while(hash.length() < 32){
            hash = "0" + hash;
        }

        return hash;
    }
}
