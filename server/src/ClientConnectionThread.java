import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Socket;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

//thread that handles all actions a connecting client should need
public class ClientConnectionThread extends Thread {
    Socket socket;
    Connection db;
    int threadID;
    DataOutputStream out;
    BufferedReader in;
    Key aesKey;
    String userHash;
    

    ClientConnectionThread(Socket socket, Connection db, int id) {
        this.socket = socket;
        this.db = db;
        this.threadID = id;
    }

    @Override
    public void run() {
        try {
            print("Attempting to open data streams with client.");
            //open the input and output streams with the client
            try {
                out = new DataOutputStream(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                print("Could not get input and output streams with client.");
                e.printStackTrace();
                interrupt();
                return;
            }

            print("Expecting RSA public key.");
            //set up secure encryption for data passing
            //receive RSA public key for one-way message passing
            String publicKeyString = in.readLine();
            print("Received " + publicKeyString);
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = (PublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            print("Responding with AES key.");
            //generate AES key to send back
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            aesKey = keyGenerator.generateKey();

            //send AES key, encrypted by RSA public key
            byte[] encryptedAESKey = encryptKey(publicKey, aesKey.getEncoded());
            String aesKeyString = new String(Base64.getEncoder().encode(encryptedAESKey));
            out.writeBytes(aesKeyString + "\n");

            print("Expecting passkey from client.");
            //receive passkey from client to determine if it is a client of the Mercury messaging group
            //stored as char array to prevent security issues when reading binary
            //(shouldn't really matter on serverside but we do it anyway)
            char[] passKey = {'a','$','F','f','G','@','2','8','m','s','4','e'};
            if(!receive().equals(new String(passKey))) {
                print("Client passkey does not match server's.");
                send("passkeyFail");
                interrupt();
                return;
            } else {
                print("Passkey matches.");
                send("passkeySucceed");
            }

            //receive the connected client's user hash
            userHash = receive();
            print("Received hash " + userHash);
            //if the user indicates that it has no hash, generate one for it and add a new user entry
            if(userHash.equals("N/A")) {
                String hash;
                print("User doesn't have a hash. Generating one...");
                //generate random hashes until the hash generated is unique
                while(true) {
                    //generate a user hash
                    hash = generateUserHash();
                    //query the DB to see if a user with that hash exists
                    PreparedStatement query = db.prepareStatement("SELECT COUNT(*) AS rowCount FROM users WHERE hash = ?");
                    query.setString(1, hash);
                    ResultSet results = query.executeQuery();
                    results.next();
                    //by getting the count of the results
                    int size = results.getInt("rowCount");
                    results.close();

                    //if there is not a user with that hash, break loop and send the hash to the user, as well as add it to the users table
                    if(size == 0) {
                        break;
                    }
                }
                //insert the new user and corresponding hash into the database
                PreparedStatement insert = db.prepareStatement("INSERT INTO users(hash, ip) VALUES(?, ?)");
                insert.setString(1, hash);
                insert.setString(2, socket.getInetAddress().getHostAddress());
                insert.execute();
                //send the hash
                send(hash);
                
                print("Generated and sent hash " + hash);
            } else {
                print("User entry exists. Updating IP...");
                //update the user's entry's IP address
                PreparedStatement update = db.prepareStatement("UPDATE users SET ip = ? WHERE hash = ?");
                update.setString(1, socket.getInetAddress().getHostAddress());
                update.setString(2, userHash);
                update.executeUpdate();
            }

            //keep listening to client until disconnected
            while(true) {
                print("Waiting for message from client...");
                //switch statement that handles different actions that the client could request
                //such as getting a user hash, updating their own user IP entry, or obtaining the IP of a given user hash
                String requestedAction = receive();
                switch(requestedAction) {
                    //case for facilitating a p2p connection between two clients
                    case "facilitateConnection":
                        print("Client wants to connect to another user.");
                        //receive the target user's hash from the connected client
                        String targetHash = receive();
                        print(userHash + " requested IP of " + targetHash);

                        PreparedStatement queryCount = db.prepareStatement("SELECT COUNT(*) AS hashExists FROM users WHERE hash = ?");
                        queryCount.setString(1, targetHash);
                        ResultSet resultCount = queryCount.executeQuery();
                        resultCount.next();
                        if(resultCount.getInt("hashExists") == 0) {
                            print(targetHash + " doesn't exist. Reporting to " + userHash);
                            send("N/A");
                            send(targetHash);
                        } else {
                            //excute a prepared query to find the user with that hash
                            PreparedStatement query = db.prepareStatement("SELECT * FROM users WHERE hash = ?");
                            query.setString(1, targetHash);
                            ResultSet results = query.executeQuery();
                            results.next();

                            //send the IP string back to the connected client
                            String targetIP = results.getString("ip");
                            print("Sending ip " + targetIP + " and received hash back to user.");
                            send(targetIP);
                            send(targetHash);
                            //TODO: at this point, the connected client should attempt to start sending UDP packets to the target IP until it receives a response (UDP hole punch)
                            //but for now, we're just seeing if the current concept will work, so we're handling it via TCP with open ports

                            //TODO: notify the target client that it needs to try to connect in hole-punched implementation
                        }
                        break;
                    //case for when anything else is received
                    default:
                        print("Did not receive an expected message, disconnecting from client.");
                        interrupt();
                        break;
                }
            
            }
        } catch (IOException e) {
            print("Could not get/send message with client.");
            e.printStackTrace();
            interrupt();
            return;
        } catch (NoSuchAlgorithmException e) {
            print("Algorithm not found.");
            e.printStackTrace();
            interrupt();
            return;
        } catch (InvalidKeySpecException e) {
            print("Given key does not match X509 spec.");
            e.printStackTrace();
            interrupt();
            return;
        } catch (Exception e) {
            print("Problem when encrypting/decrypting a message.");
            e.printStackTrace();
            interrupt();
            return;
        }
    }

    //override the thread interrupt function to make sure the thread attempts to safe-close the socket and data streams
    @Override
    public void interrupt() {
        try {
            send("disconnect");
            out.close();
            in.close();
            socket.close();
        } catch (Exception e) {
            print("Failed to close socket on interrupt, but it doesn't matter.");
            e.printStackTrace();
        }
        super.interrupt();
    }

    //encrypt an AES key using an RSA key
    public static byte[] encryptKey(Key publicKey, byte[] keyToEncrypt) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding"); // Use appropriate padding
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(keyToEncrypt);
    }

    //receive the incoming line, AES decrypt, and return it as a byte array
    public byte[] receiveBytes() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        
        // Read nonce and encrypted message
        String encryptedEncodedMessage = in.readLine();
        String[] parts = encryptedEncodedMessage.split(":");
        byte[] nonce = Base64.getDecoder().decode(parts[0]);
        byte[] decodedBytes = Base64.getDecoder().decode(parts[1]);
        
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, nonce));

        // Decrypt message
        return cipher.doFinal(decodedBytes);
    }

    //receive the incoming line, AES decrypt, and return as String
    public String receive() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        
        // Read nonce and encrypted message
        String encryptedEncodedMessage = in.readLine();
        String[] parts = encryptedEncodedMessage.split(":");
        byte[] nonce = Base64.getDecoder().decode(parts[0]);
        byte[] decodedBytes = Base64.getDecoder().decode(parts[1]);
        
        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, nonce));

        // Decrypt message
        byte[] decryptedMessage = cipher.doFinal(decodedBytes);
        return new String(decryptedMessage);
    }

    //encrypt and send a string to the client
    public void send(String message) throws Exception {
        Cipher AEScipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Generate nonce
        byte[] nonce = generateNonce(AEScipher.getBlockSize());

        // Initialize cipher with nonce
        AEScipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, nonce));

        // Encrypt message
        byte[] encryptedMessage = AEScipher.doFinal(message.getBytes());

        Base64.Encoder encoder = Base64.getEncoder();

        // Concatenate nonce and encrypted message
        String encryptedEncodedMessage = new String(encoder.encodeToString(nonce)) + ":" + new String(encoder.encodeToString(encryptedMessage));

        out.writeBytes(encryptedEncodedMessage + '\n');
        out.flush();
    }

    //generate a nonce the length of the AES block size
    private static byte[] generateNonce(int size) {
        byte[] nonce = new byte[size];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    //create an identifier hash when a new user joins the network
    String generateUserHash() throws NoSuchAlgorithmException {
        //securely generate a string of 32 random characters
        String SALTCHARS = "abcdef1234567890";
		StringBuilder salt = new StringBuilder();
		SecureRandom rnd = new SecureRandom();
        while (salt.length() < 8) {
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();
        //version that actually uses MD5 to generate hash strings 32 characters long... not used right now because it's just a pain to type out long hashes
		/*while (salt.length() < 32) {
			int index = (int) (rnd.nextFloat() * SALTCHARS.length());
			salt.append(SALTCHARS.charAt(index));
		}
		String saltStr = salt.toString();

        //hash the string with MD5
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] digestBytes = digest.digest(saltStr.getBytes());
        BigInteger signum = new BigInteger(1, digestBytes);
        String hash = signum.toString(16);
        while(hash.length() < 32) {
            hash = "0" + hash;
        }*/

        return saltStr;
    }

    //adapter to shorthand a print statement and include the thread's ID
    void print(String message) {
        System.out.println("Thread " + threadID + ": " + message);
    }
}