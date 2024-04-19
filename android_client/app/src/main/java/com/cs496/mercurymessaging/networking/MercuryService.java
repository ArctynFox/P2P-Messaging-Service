package com.cs496.mercurymessaging.networking;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.cs496.mercurymessaging.activities.MainActivity;
import com.cs496.mercurymessaging.networking.threads.ServerThread;

public class MercuryService extends Service {
    public static MercuryService singleton = null;
    public static boolean isRunning = false;
    String tag = this.getClass().getName();
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(tag, "Starting sockets for mercury connections...");

        //channel for foreground service notification
        final String CHANNELID = "Mercury Service";
        NotificationChannel channel = new NotificationChannel(
                CHANNELID,
                CHANNELID,
                NotificationManager.IMPORTANCE_LOW
        );

        //create the foreground service persistent notification
        Intent notificationClickedIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationClickedIntent, PendingIntent.FLAG_IMMUTABLE);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        Notification.Builder notification = new Notification.Builder(this, CHANNELID)
                .setContentText("Mercury is open to connections.")
                .setContentTitle("Mercury Service")
                .setContentIntent(pendingIntent);

        //start the foreground service
        startForeground(1001, notification.build());
        isRunning = true;

        singleton = this;

        return super.onStartCommand(intent, flags, startId);
    }

    Thread serverThread = new ServerThread(this, getSharedPreferences("mercury", Context.MODE_PRIVATE));

    @Override
    public void onDestroy() {
        isRunning = false;
        singleton = null;
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
