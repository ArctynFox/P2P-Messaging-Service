package com.cs496.mercurymessaging.networking.threads;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cs496.mercurymessaging.App;

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

//thread which handles connection and data passing with central server
public class ServerConnection extends Thread {
    static Socket socket;
    BufferedReader fromServer;
    DataOutputStream toServer;
    SecretKey aesKey;
    Context context;

    SharedPreferences prefs;

    public ServerConnection(Context context, SharedPreferences prefs) {
        this.context = context;
        this.prefs = prefs;
    }

    //TODO: any use of the Log class is android-only and should be replaced with print statements on the PC client

    public void initialize() {
        initializerThread.start();
    }

    Thread initializerThread = new Thread() {
        @Override
        public void run() {
            try {
                connectToServer();
            } catch (IOException e) {
                Log.e("ServerThread", "Failure to connect to server.");
                e.printStackTrace();
                interrupt();
                return;
            }

            try {
                openDataPathways();
            } catch (IOException e) {
                Log.e("ServerThread", "Failed to open data streams with server.");
                e.printStackTrace();
                interrupt();
                return;
            }

            PublicKey publicKey;
            PrivateKey privateKey;
            try {
                KeyPair keyPair = generateRSAKeyPair();

                publicKey = keyPair.getPublic();
                privateKey = keyPair.getPrivate();
            } catch (NoSuchAlgorithmException e) {
                Log.e("ServerThread", "Unrecognized RSA algorithm.");
                e.printStackTrace();
                interrupt();
                return;
            }

            try {
                getAESKey(publicKey, privateKey);
            } catch (IOException e) {
                Log.e("ServerThread", "Failed to send or receive data with server when exchanging RSA key for AES key.");
                e.printStackTrace();
                interrupt();
                return;
            }

            try {
                assertPassKey(new String(new char[]{'a', '$', 'F', 'f', 'G', '@', '2', '8', 'm', 's', '4', 'e'}));
            } catch (Exception e) {
                Log.e("ServerThread", "Failed to receive message from server.");
                e.printStackTrace();
                interrupt();
                return;
            }

            try {
                exchangeHash();
            } catch (Exception e) {
                Log.e("ServerThread", "Failed to exchange user hash with server.");
                e.printStackTrace();
                interrupt();
            }

            //repeatedly wait for the central server to tell this client that another client wants to connect
            //TODO: for hole punching, but with the current TCP implementation this is useless
            while (true) {
                try {
                    String incoming = receive(); //TODO: at the current point in time this is literally just to make sure the socket doesn't close on its own for some reason
                    if (incoming.equals("disconnect")) {
                        interrupt();
                        return;
                    }
                    String[] connectInfo = incoming.split("\0");

                    //add a new client socket to the hashmap
                    App.clientConnectionHashMap.put(connectInfo[1], new ClientConnection(connectInfo[0], context, prefs, connectInfo[1]));
                } catch (Exception e) {
                    Log.e("ServerThread", "Failed to communicate with server.");
                    e.printStackTrace();
                    interrupt();
                    return;
                }
            }
        }
    };

    //open socket and try to connect to central server
    private void connectToServer() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress("192.168.1.47", 52761), 10000);
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

    //assert the passkey with the server
    private void assertPassKey(String passKey) throws Exception {
        send(passKey);
        String response = receive();
        if(response.equals("passkeyFail")) {
            interrupt();
        }
    }

    //notify the server of the user's hash or that it needs a hash, and receive one if latter
    private void exchangeHash() throws Exception {
        //get hash from prefs
        String hash = prefs.getString("hash", "N/A");

        //send it
        send(hash);

        //if the hash was the default not available value, receive a hash and save it to prefs
        if(hash.equals("N/A")) {
            prefs.edit().putString("hash", receive()).apply();
        }
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
        byte[] nonce = Base64.getDecoder().decode(parts[0]);
        byte[] decodedBytes = Base64.getDecoder().decode(parts[1]);

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

    //safe close data pathways without killing thread
    public void disconnect() {
        try {
            toServer.close();
            fromServer.close();
            socket.close();
        } catch (IOException e) {
            Log.e("ServerThread", "Issue when trying to close socket and data streams.");
            e.printStackTrace();
        }
    }
}
