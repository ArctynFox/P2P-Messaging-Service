package com.cs496.mercurymessaging.networking;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.cs496.mercurymessaging.App;
import com.cs496.mercurymessaging.activities.MainActivity;
import com.cs496.mercurymessaging.networking.threads.HostThread;
import com.cs496.mercurymessaging.networking.threads.ServerConnection;

public class MercuryService extends Service {
    public static MercuryService singleton = null;
    HostThread hostThread;
    public static boolean isRunning = false;
    String tag = "MercuryService";

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
        startForeground(1001, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING);

        isRunning = true;

        singleton = this;

        App.serverConnection = new ServerConnection(getSharedPreferences("mercury", Context.MODE_PRIVATE), this);
        App.serverConnection.initialize();

        //create HostThread and run
        hostThread = new HostThread();
        hostThread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        singleton = null;

        //close all peer sockets
        for(PeerSocketContainer peerSocketContainer : App.peerSocketContainerHashMap.values()) {
            peerSocketContainer.disconnect();
        }

        //close connection with central server
        App.serverConnection.close();

        //close host peer ServerSocket
        hostThread.interrupt();

        //kill the service
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
