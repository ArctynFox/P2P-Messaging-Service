package com.cs496.mercurymessaging.networking;

import static com.cs496.mercurymessaging.database.MercuryDB.db;

import android.util.Log;

import com.cs496.mercurymessaging.App;
import com.cs496.mercurymessaging.database.tables.Message;
import com.cs496.mercurymessaging.database.tables.User;
import com.cs496.mercurymessaging.networking.threads.ClientConnection;
import com.cs496.mercurymessaging.networking.threads.HostClientThread;

//adapter class that adapts outgoing messages to their respective thread
public class PeerSocketContainer {
    ClientConnection clientConnection;
    HostClientThread hostClientThread;
    User user;
    boolean isHostPeer;
    String tag = "PeerSocketContainer";

    //PeerSocketContainer constructor for ClientConnections
    public PeerSocketContainer(ClientConnection clientConnection, User user) {
        this.clientConnection = clientConnection;
        this.user = user;
        isHostPeer = false;
    }

    //PeerSocketContainer constructor for HostClientThreads
    public PeerSocketContainer(HostClientThread hostClientThread, User user) {
        this.hostClientThread = hostClientThread;
        this.user = user;
        isHostPeer = true;
    }

    //adapter to disconnect from a peer
    public void disconnect() {
        Log.d(tag, "Disconnecting from " + user.getHash());
        if(isHostPeer) {
            hostClientThread.interrupt();
            hostClientThread = null;
        } else {
            clientConnection.disconnect();
            clientConnection = null;
        }

        App.peerSocketContainerHashMap.remove(user.getHash());

        user = null;
    }

    //adapter to send a message to a peer
    public void send(String text) {
        long timestamp = System.currentTimeMillis();
        Message message = new Message(user.getHash(), true, text, timestamp);

        Log.d(tag, "Attempting to send message: " + text);

        new Thread(() -> {
            try {
                if (isHostPeer) {
                    hostClientThread.send(text);
                    hostClientThread.send(Long.toString(timestamp));
                } else {
                    clientConnection.send(text);
                    clientConnection.send(Long.toString(timestamp));
                }
            } catch (Exception e) {
                Log.e(tag, "Exception when trying to send message to " + user.getHash());
                e.printStackTrace();
            }
        }).start();

        assert db != null;
        db.addMessage(message);

        if(App.isMessagesActivity()) {
            if(App.messagesActivity.user.getHash().equals(message.getHash())) {
                App.messagesActivity.runOnUiThread(() -> App.messagesActivity.displayMessages());
            }
        }
    }
}
