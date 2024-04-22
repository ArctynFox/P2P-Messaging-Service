package com.cs496.mercurymessaging.networking.threads;

import android.util.Log;

import com.cs496.mercurymessaging.App;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HostThread extends Thread {
    ServerSocket serverSocket;
    String tag = "HostThread";
    int currentThreads = 0;

    @Override
    public void run() {
        Log.d(tag, "Starting host peer ServerSocket.");
        try {
            //Modify the port on the following line if you wish to use a different one.
            //This is the port that clients should be given to connect to a specific Mercury server group.
            serverSocket = new ServerSocket(52762);
        } catch (IOException e) {
            Log.d(tag, "Could not open server socket. Please restart server.");
            e.printStackTrace();
            return;
        }

        App.hostThread = this;
        Log.d(tag, "ServerSocket created.");
        //accept connections indefinitely not exceeding a defined number
        int maxThreads = 100;
        /*try {
            Socket socketTest = serverSocket.accept();
            Log.d(tag, "Accepted an incoming client peer.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
        while(currentThreads < maxThreads) {
            try {
                Log.d(tag, "Waiting for incoming connections...");
                //accept an incoming connection and print the client's address
                Socket socket = serverSocket.accept();

                Log.d(tag, "Incoming connection from " + socket.getInetAddress().getHostAddress() + ", moving connection to new thread.");

                //pass the client's socket to a new thread
                HostClientThread thread = new HostClientThread(socket);
                thread.start();
                currentThreads += 1;
            } catch (IOException e) {
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    Log.d(tag, "Could not close server on fail to accept connection. Please restart app.");
                    e1.printStackTrace();
                    return;
                }
                Log.d(tag, "Could not accept connection. Please restart server.");
                e.printStackTrace();
                return;
            }
        }
    }

    public void reduceThreads() {
        if(currentThreads > 0) {
            currentThreads -= 1;
        }
    }

    @Override
    public void interrupt() {
        try {
            serverSocket.close();
            App.hostThread = null;
        } catch (IOException e) {
            Log.d(tag, "Could not close server on interrupt.");
            e.printStackTrace();
        }
    }
}
