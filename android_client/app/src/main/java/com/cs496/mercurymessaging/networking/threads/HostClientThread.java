package com.cs496.mercurymessaging.networking.threads;

import static com.cs496.mercurymessaging.database.MercuryDB.db;

import android.content.Intent;
import android.util.Log;

import com.cs496.mercurymessaging.App;
import com.cs496.mercurymessaging.activities.MainActivity;
import com.cs496.mercurymessaging.database.tables.Message;
import com.cs496.mercurymessaging.database.tables.User;
import com.cs496.mercurymessaging.networking.PeerSocketContainer;

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
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class HostClientThread extends Thread {
    Socket socket;
    DataOutputStream out;
    BufferedReader in;
    SecretKey aesKey;
    User user;
    String tag = "HostClientThread";

    HostClientThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            //open the input and output streams with the client peer
            print("Attempting to open data streams with client.");
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
            print("Expecting RSA public key.");
            String publicKeyString = in.readLine();
            print("Received " + publicKeyString);
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

            //generate AES key to send back
            print("Responding with AES key.");
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            aesKey = keyGenerator.generateKey();

            //send AES key, encrypted by RSA public key
            byte[] encryptedAESKey = encryptKey(publicKey, aesKey.getEncoded());
            String aesKeyString = new String(Base64.getEncoder().encode(encryptedAESKey));
            out.writeBytes(aesKeyString + "\n");

            //receive the connected client peer's user hash
            String userHash = receive();
            print("Received hash " + userHash);

            //get or create the user's db entry
            assert db != null;
            if(!db.doesUserExist(userHash)) {
                print("Client peer has never connected before. Adding user entry to db.");
                user = new User(userHash, userHash, true, Long.MAX_VALUE);
                db.addUser(user);
            } else {
                user = db.getUserByHash(userHash);
            }

            if(App.isMainActivity()) {
                App.mainActivity.runOnUiThread(() -> App.mainActivity.displayUserList());
            }

            App.peerSocketContainerHashMap.put(userHash, new PeerSocketContainer(this, user));

            //keep listening to client peer until disconnected
            while(true) {
                String incoming = receive();
                print("Received message: " + incoming);
                if(incoming.equals("disconnect")) {
                    print("Disconnect request received.");
                    PeerSocketContainer peerSocketContainer = App.peerSocketContainerHashMap.get(user.getHash());
                    if(peerSocketContainer != null) {
                        peerSocketContainer.disconnect();
                    } else interrupt();

                    //if the app is on the target user's message screen
                    if(App.isMessagesActivity()) {
                        if(App.messagesActivity.user == user) {
                            //intents that are passed to startActivity open a different screen
                            Intent intent = new Intent(App.messagesActivity.getBaseContext(), MainActivity.class);
                            App.messagesActivity.startActivity(intent);
                        }
                    }

                    return;
                }

                //anything else is an incoming message

                //receive the timestamp of the message from the sending peer
                String timestamp = receive();

                Log.d(tag, "Received a message from " + user.getHash() + ".");
                Log.d(tag, "Incoming message: " + incoming);

                //create a message object for the message
                Message message = new Message(user.getHash(), false, incoming, Long.parseLong(timestamp));

                //add the message to the database
                db.addMessage(message);
                print("Message added to database.");

                //refresh the message screen if on the target user's message list
                if(App.isMessagesActivity()) {
                    if(App.messagesActivity.user.getHash().equals(message.getHash())) {
                        App.messagesActivity.runOnUiThread(() -> App.messagesActivity.displayMessages());
                    }
                }
            }
        } catch (IOException e) {
            Log.e(tag, "Could not get/send message with client.");
            e.printStackTrace();
            interrupt();
        } catch (NoSuchAlgorithmException e) {
            Log.e(tag, "Algorithm not found.");
            e.printStackTrace();
            interrupt();
        } catch (InvalidKeySpecException e) {
            Log.e(tag, "Given key does not match X509 spec.");
            e.printStackTrace();
            interrupt();
        } catch (Exception e) {
            Log.e(tag, "Problem when encrypting/decrypting a message.");
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
            Log.e(tag, "Failed to close socket on interrupt, but it doesn't matter.");
            e.printStackTrace();
        }
        super.interrupt();
        App.hostThread.reduceThreads();
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
        byte[] nonce = Base64.getMimeDecoder().decode(parts[0]);
        byte[] decodedBytes = Base64.getMimeDecoder().decode(parts[1]);

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
        if(user != null) {
            Log.d(tag, user.getHash() + ": " + message);
        } else {
            Log.d(tag, message);
        }
    }
}
