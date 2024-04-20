package com.cs496.mercurymessaging.networking.threads;

import android.util.Log;

import com.cs496.mercurymessaging.database.MercuryDB;
import com.cs496.mercurymessaging.database.Message;
import com.cs496.mercurymessaging.database.User;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

public class HostClientThread extends Thread {
    Socket socket;
    DataOutputStream out;
    BufferedReader in;
    Key aesKey;
    User user;
    MercuryDB db;
    String tag = this.getClass().getName();

    HostClientThread(Socket socket, MercuryDB db) {
        this.socket = socket;
        this.db = db;
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

            //receive the connected client's user hash
            String userHash = receive();

            //get or create the user's db entry
            if(!db.doesUserExist(userHash)) {
                user = new User(userHash, Objects.requireNonNull(socket.getInetAddress().getHostAddress()), "", true);
                db.addUser(user);
            } else {
                user = db.getUserByHash(userHash);
            }

            //keep listening to client peer until disconnected
            while(true) {
                String incoming = receive();

                if(incoming.equals("disconnect")) {
                    interrupt();
                }

                print("Received a message from " + user.getHash() + ".");

                String[] messageInfo = incoming.split("\0");

                print("Incoming message: " + messageInfo[0]);

                Message message = new Message(user, false, messageInfo[0], Long.parseLong(messageInfo[1]), null);

                db.addMessage(message);
            }
        } catch (IOException e) {
            print("Could not get/send message with client.");
            e.printStackTrace();
            interrupt();
        } catch (NoSuchAlgorithmException e) {
            print("Algorithm not found.");
            e.printStackTrace();
            interrupt();
        } catch (InvalidKeySpecException e) {
            print("Given key does not match X509 spec.");
            e.printStackTrace();
            interrupt();
        } catch (Exception e) {
            print("Problem when encrypting/decrypting a message.");
            e.printStackTrace();
            interrupt();
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
        String encryptedEncodedMessage = encoder.encodeToString(nonce) + ":" + encoder.encodeToString(encryptedMessage);

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

    //adapter to shorthand a print statement and include the thread's ID
    void print(String message) {
        Log.d(tag,user.getHash() + ": " + message);
    }
}
