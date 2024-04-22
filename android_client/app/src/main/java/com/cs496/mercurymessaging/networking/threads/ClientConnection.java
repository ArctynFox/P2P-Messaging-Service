package com.cs496.mercurymessaging.networking.threads;

import static com.cs496.mercurymessaging.database.MercuryDB.db;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ClientConnection {
    Socket socket;
    BufferedReader fromServer;
    DataOutputStream toServer;
    SecretKey aesKey;
    SharedPreferences prefs;
    User user;
    String tag = "ClientConnection";

    public ClientConnection(String address, SharedPreferences prefs, String hash) {
        initialize(address);
        this.prefs = prefs;


        assert db != null;

        this.user = db.getUserByHash(hash);

        App.peerSocketContainerHashMap.put(hash, new PeerSocketContainer(this, user));
    }

    public boolean initialize(String ip) {
        try {
            connectToServer(ip);
        } catch (IOException e) {
            Log.e(tag, "Failure to connect to server.");
            e.printStackTrace();
            disconnect();
            return false;
        }

        try {
            openDataPathways();
        } catch (IOException e) {
            Log.e(tag, "Failed to open data streams with server.");
            e.printStackTrace();
            disconnect();
            return false;
        }

        PublicKey publicKey;
        PrivateKey privateKey;
        try {
            KeyPair keyPair = generateRSAKeyPair();

            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            Log.e(tag, "Unrecognized RSA algorithm.");
            e.printStackTrace();
            disconnect();
            return false;
        }

        try {
            getAESKey(publicKey, privateKey);
        } catch (IOException e) {
            Log.e(tag, "Failed to send or receive data with server when exchanging RSA key for AES key.");
            e.printStackTrace();
            disconnect();
            return false;
        }

        try {
            send(App.hash);
        } catch (Exception e) {
            Log.e(tag, "Failed to send hash to host peer.");
            e.printStackTrace();
            disconnect();
            return false;
        }

        receiveMessageThread.start();
        return true;
    }

    //thread that just listens for incoming messages and puts them in the database, and tells the UI to update if on the respective message screen
    Thread receiveMessageThread = new Thread() {
        @Override
        public void run() {
            while(true) {
                try {
                    String incoming = receive();

                    if(incoming.equals("disconnect")) {
                        PeerSocketContainer peerSocketContainer = App.peerSocketContainerHashMap.get(user.getHash());
                        if(peerSocketContainer != null) {
                            peerSocketContainer.disconnect();
                        } else disconnect();

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

                    String timestamp = receive();

                    Log.d(tag, "Received a message from " + user.getHash() + ".");
                    Log.d(tag, "Incoming message: " + incoming);

                    Message message = new Message(user.getHash(), false, incoming, Long.parseLong(timestamp));

                    assert db != null;
                    db.addMessage(message);
                    Log.d(tag, "Message added to database.");

                    if(App.isMessagesActivity()) {
                        if(App.messagesActivity.user.getHash().equals(message.getHash())) {
                            App.messagesActivity.runOnUiThread(() -> App.messagesActivity.displayMessages());
                        }
                    }
                } catch (Exception e) {
                    Log.e("ReceiveMessageThread","Failed to receive message from host.");
                    e.printStackTrace();
                    interrupt();
                    return;
                }
            }
        }
    };

    //open socket and try to connect to central server
    private void connectToServer(String ip) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, 52762), 10000);
    }

    //open the data streams to and from the server
    private void openDataPathways() throws IOException {
        fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        toServer = new DataOutputStream(socket.getOutputStream());
    }

    //generate the RSA key pair used to securely receive AES key
    private KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        //get an instance of a key pair generator for the RSA algorithm
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");

        //initialize the key pair with a key size of 2048
        keyPairGenerator.initialize(2048);

        return keyPairGenerator.generateKeyPair();
    }

    //trade the RSA public key for an AES key
    private void getAESKey(PublicKey publicKey, PrivateKey privateKey) throws IOException {
        //encode the key in base64
        Base64.Encoder encoder = Base64.getEncoder();
        String publicKeyString = new String(encoder.encode(publicKey.getEncoded()));

        //send the RSA public key to the server
        toServer.writeBytes(publicKeyString + '\n');

        //Receive AES key from server
        String encryptedAESKey = fromServer.readLine();

        byte[] decodedBytes = Base64.getDecoder().decode(encryptedAESKey);

        //Decrypt AES key using RSA private key
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        } catch (Exception ignored) {} //shouldn't ever occur
        try {
            assert cipher != null;
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
        } catch (Exception ignored) {} //shouldn't ever occur
        byte[] decryptedMessage = new byte[0];
        try {
            decryptedMessage = cipher.doFinal(decodedBytes);
        } catch (Exception ignored) {}

        //get static reference to AES key
        aesKey = new SecretKeySpec(decryptedMessage, "AES");
    }

    //send AES encrypted message to server
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
        String encryptedEncodedMessage = encoder.encodeToString(nonce) + ":" +
                encoder.encodeToString(encryptedMessage);

        toServer.writeBytes(encryptedEncodedMessage + '\n');
        toServer.flush();
    }

    //get AES encrypted message from server
    public String receive() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // Read nonce and encrypted message
        String encryptedEncodedMessage = fromServer.readLine();
        String[] parts = encryptedEncodedMessage.split(":");
        byte[] nonce = Base64.getMimeDecoder().decode(parts[0]);
        byte[] decodedBytes = Base64.getMimeDecoder().decode(parts[1]);

        cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, nonce));

        // Decrypt message
        byte[] decryptedMessage = cipher.doFinal(decodedBytes);
        return new String(decryptedMessage);
    }

    //generate nonce sector for AES encryption
    private static byte[] generateNonce(int size) {
        byte[] nonce = new byte[size];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    //safe close data pathways and remove itself from the client hashmap
    public void disconnect() {
        try {
            receiveMessageThread.interrupt();
            toServer.close();
            fromServer.close();
            socket.close();
        } catch (IOException e) {
            Log.e("ClientConnection", "Issue when trying to close socket and data streams.");
            e.printStackTrace();
        }
    }
}
