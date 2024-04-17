import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.net.Socket;

public class App {
    // Swing components for UI
    static JFrame frame;
    static JTextArea chatArea;
    static JTextField messageField;
    static JButton sendButton;
    static PrintWriter writer; // send data to the server

    public static void main(String[] args) throws Exception {
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

        // Connect to the server
        Socket socket = new Socket("IP_here", 52761); // Replace IP_here with IP address of the server machine

        // Create a PrintWriter object for sending data to the server
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    // Method to send message
    private static void sendMessage() {
        String message = messageField.getText();
        // Send message to server
        writer.println(message);
        messageField.setText(""); // Clear the message field after sending
    }
}

