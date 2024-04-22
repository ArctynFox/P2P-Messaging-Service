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
    String tag = this.getClass().getName();

    public PeerSocketContainer(ClientConnection clientConnection, User user) {
        this.clientConnection = clientConnection;
        this.user = user;
        isHostPeer = false;
    }

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
        Message message = new Message(user, true, text, timestamp);

        try {
            if(isHostPeer) {
                hostClientThread.send(text + "\0" + timestamp);
            } else {
                clientConnection.send(text + "\0" + timestamp);
            }
        } catch (Exception e){
            Log.d(tag, "Exception when trying to send message to " + user.getHash());
            e.printStackTrace();
            return;
        }

        assert db != null;
        db.addMessage(message);
    }
}
