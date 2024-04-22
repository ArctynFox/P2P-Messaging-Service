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
        frame = new JFrame("Mercury Messaging");
        chatArea = new JTextArea();
        messageField = new JTextField();
        sendButton = new JButton("Send");

        // Set font for chat area
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));

        // Disable editing in chat area
        chatArea.setEditable(false);

        // Set pastel blue background color for chat area
        Color pastelBlue = new Color(230, 242, 255); // RGB values for pastel blue
        chatArea.setBackground(pastelBlue);

        // Set white background color for message field
        messageField.setBackground(Color.WHITE);

        // Set size of message input field
        messageField.setPreferredSize(new Dimension(300, 50)); // Adjust dimensions as needed

        // Create a scroll pane for the chat area
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        // Create panel for message input and send button
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Set layout
        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Set frame properties
        frame.setSize(400, 400); // Increased height to accommodate larger message input box
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
        
        // Add the message to the chat area immediately
        chatArea.append("You: " + message + "\n");
        
        // Clear the message field after sending
        messageField.setText("");
        
        // Send message to server
        writer.println(message);
    }
}


