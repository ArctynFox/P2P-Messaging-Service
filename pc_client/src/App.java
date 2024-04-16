import javax.swing.*;

public class App {
    // Swing components for UI
    static JFrame frame;
    static JTextArea chatArea;
    static JTextField messageField;
    static JButton sendButton;

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
    }

    // Method to send message
    private void sendMessage() {
        String message = messageField.getText();
        // Send message to server
        // You need to implement this part
    }
}
