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
    

    ClientConnectionThread(Socket socket, Connection db, int id) {
        this.socket = socket;
        this.db = db;
        this.threadID = id;
    }

    @Override
    public void run() {
        try {
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

            //set up secure encryption for data passing
            //receive RSA public key for one-way message passing
            String publicKeyString = in.readLine();
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = (PublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            //generate AES key to send back
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            aesKey = keyGenerator.generateKey();

            //send AES key, encrypted by RSA public key
            byte[] encryptedAESKey = encryptKey(publicKey, aesKey.getEncoded());
            String aesKeyString = new String(Base64.getEncoder().encode(encryptedAESKey));
            out.writeBytes(aesKeyString + "\n");

            //receive passkey from client to determine if it is a client of the Mercury messaging group
            //stored as char array to prevent security issues when reading binary
            //(shouldn't really matter on serverside but we do it anyway)
            char[] passKey = {'a','$','F','f','G','@','2','8','m','s','4','e'};
            if(!receive().equals(new String(passKey))) {
                print("Client passkey does not match server's.");
                interrupt();
                return;
            }

            //keep listening to client until disconnected
            while(true) {
                //switch statement that handles different actions that the client could request
                //such as getting a user hash, updating their own user IP entry, or obtaining the IP of a given user hash
                String requestedAction = receive();
                switch(requestedAction) {
                    //TODO: add necessary actions
                    //case for if a new user has connected and needs a user ID hash
                    //case for when an existing user connects and needs to update their IP
                    //case for facilitating a p2p connection between two clients
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
        while(hash.length() < 32) {
            hash = "0" + hash;
        }

        return hash;
    }

    void print(String message) {
        System.out.println("Thread " + threadID + ": " + message);
    }
}

