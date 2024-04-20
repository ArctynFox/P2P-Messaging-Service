package com.cs496.mercurymessaging.networking.threads;

import static com.cs496.mercurymessaging.database.MercuryDB.db;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HostThread extends Thread {
    ServerSocket serverSocket;

    @Override
    public void run() {
        try {
            //Modify the port on the following line if you wish to use a different one.
            //This is the port that clients should be given to connect to a specific Mercury server group.
            serverSocket = new ServerSocket(52761);
        } catch (IOException e) {
            System.out.println("Could not open server socket. Please restart server.");
            e.printStackTrace();
            return;
        }

        //accept connections indefinitely not exceeding a defined number
        int maxThreads = 1000;
        while(true) {
            if(Thread.activeCount() < maxThreads) {
                try {
                    System.out.println("Waiting for incoming connections...");
                    //accept an incoming connection and print the client's address
                    Socket socket = serverSocket.accept();

                    System.out.println("Incoming connection from " + socket.getInetAddress().getHostAddress() + ", moving connecti0on to new thread.");

                    //pass the client's socket to a new thread
                    HostClientThread thread = new HostClientThread(socket, db);
                    thread.start();
                } catch (IOException e) {
                    try {
                        serverSocket.close();
                    } catch (IOException e1) {
                        System.out.println("Could not close server on fail to accept connection. Please restart server.");
                        e1.printStackTrace();
                    }
                    System.out.println("Could not accept connection. Please restart server.");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void interrupt() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Could not close server on interrupt.");
            e.printStackTrace();
        }
    }
}
