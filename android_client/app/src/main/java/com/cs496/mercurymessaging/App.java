package com.cs496.mercurymessaging;

import android.content.Context;

import com.cs496.mercurymessaging.activities.MainActivity;
import com.cs496.mercurymessaging.activities.MessagesActivity;
import com.cs496.mercurymessaging.networking.PeerSocketContainer;
import com.cs496.mercurymessaging.networking.threads.ClientConnection;
import com.cs496.mercurymessaging.networking.threads.HostClientThread;
import com.cs496.mercurymessaging.networking.threads.HostThread;
import com.cs496.mercurymessaging.networking.threads.ServerConnection;

import java.util.HashMap;

public class App {
    //references to these activities if they are currently active so that incoming messages or connections can signal the corresponding screen to update
    public static MainActivity mainActivity = null;
    public static MessagesActivity messagesActivity = null;

    public static String hash = null;


    //check for if the current activity is MainActivity (user list)
    public static boolean isMainActivity() {
        return mainActivity != null;
    }

    //check for if the current activity is MessagesActivity
    public static boolean isMessagesActivity() {
        return messagesActivity != null;
    }

    public static ServerConnection serverConnection = null;
    public static HostThread hostThread = null;

    //hashmap that contains PeerSocketContainers which reference either a ClientConnection or HostClientThread depending on how it was created
    //used to send messages at any time
    public static HashMap<String, PeerSocketContainer> peerSocketContainerHashMap = new HashMap<>();
}
