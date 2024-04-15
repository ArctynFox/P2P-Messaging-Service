import javax.swing.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.net.Socket;
import java.sql.Connection;

//thread that handles all actions a connecting client should need
public class ClientConnectionThread extends Thread {
    Socket socket;
    Connection db;
    int threadID;
    DataOutputStream out;
    BufferedReader in;

    // Swing components for UI
    JFrame frame;
    JTextArea chatArea;
    JTextField messageField;
    JButton sendButton;

    ClientConnectionThread(Socket socket, Connection db, int id) {
        this.socket = socket;
        this.db = db;
        this.threadID = id;

        // Initialize UI components
        frame = new JFrame("Chat App");
        chatArea = new JTextArea();
        messageField = new JTextField();
        sendButton = new JButton("Send");

        // Set layout
        frame.setLayout(new BorderLayout());
        frame.add(chatArea, BorderLayout.CENTER);
        frame.add(messageField, BorderLayout.SOUTH);
        frame.add(sendButton, BorderLayout.EAST);

        // Set frame properties
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Add action listener to send button
        sendButton.addActionListener(e -> sendMessage());
    }

    // Method to send message
    private void sendMessage() {
        String message = messageField.getText();
        // Send message to server
        // You need to implement this part
    }

    @Override
    public void run() {
        //perform some sort of handshake to confirm this is a client of our software and not some random connection

        //switch statement that handles different actions that the client could request
        //such as getting a user hash, updating their own user IP entry, or obtaining the IP of a given user hash
    }
}

