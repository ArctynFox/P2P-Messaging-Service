package com.cs496.mercurymessaging;

import android.content.Context;

import com.cs496.mercurymessaging.activities.MainActivity;
import com.cs496.mercurymessaging.activities.MessagesActivity;

public class App {
    //reference to a context so the database can be updated even if the app is closed
    public static Context context;

    //references to these activities if they are currently active so that incoming messages or connections can signal the corresponding screen to update
    public static MainActivity mainActivity = null;
    public static MessagesActivity messagesActivity = null;

    //check for if the current activity is MainActivity (user list)
    public static boolean isMainActivity() {
        return mainActivity != null;
    }

    //check for if the current activity is MessagesActivity
    public static boolean isMessagesActivity() {
        return messagesActivity != null;
    }
}
