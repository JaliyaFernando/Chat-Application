import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */

public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    // TODO: Add a list box
    JList<String> clients =new JList<>();
    HashSet<String> setActiveClients=new HashSet<>();
    String recipient;
    String currentUser;
    JCheckBox checkBox = new JCheckBox("Send to all users");
    JPanel panel = new JPanel();
    
    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
    	textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();
        checkBox.setSelected(false);
        JLabel jlabel = new JLabel("Active Users");
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(checkBox);
		panel.add(jlabel);
		panel.add(clients);
		frame.getContentPane().add(panel, "East");
		clients.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // TODO: You may have to edit this event handler to handle point to point messaging,
        // where one client can send a message to a specific client. You can add some header to 
        // the message to identify the recipient. You can get the receipient name from the listbox.
		
		checkBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(checkBox.isSelected())
					clients.setEnabled(false);
				else
					clients.setEnabled(true);
				
			}
		}); 
		
		textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
        	public void actionPerformed(ActionEvent e) {
                if (textField.getText().contains(":")||checkBox.isSelected()) {
					sendMsgAll();
				} 
                else if (!(checkBox.isSelected()) && recipient==null) {
					JOptionPane.showMessageDialog(null,"Please select a recipient or select all users");					
				} 
                else {
					sendMsgRecipient();
				}
                textField.setText("");
            }
        	
        	
        	private void sendMsgRecipient() {	
        		out.println(recipient+":"+textField.getText());
			}

			private void sendMsgAll() {
        		out.println(textField.getText());

			}
        });

        clients.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JList<String> string =  (JList<String>)e.getSource();
                recipient=string.getSelectedValue();
            }
        });
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
                frame,
                "Enter IP Address of the Server:",
                "Welcome to the Chatter",
                JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
                frame,
                "Choose a screen name:",
                "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }


    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
    	String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        
        // TODO: You may have to extend this protocol to achieve task 9 in the lab sheet

        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
            	currentUser = getName();
				frame.setTitle("Welcome " + currentUser);
				out.println(currentUser);
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            }else if(line.startsWith("ACTIVECLIENTS:")) {
            	setActiveClients.addAll(Arrays.asList(line.substring(line.indexOf(":")+1).split(",")));
            	messageArea.append(in.readLine()+"\n");
            	
                DefaultListModel<String> gList =new DefaultListModel<>();

                for (String client:setActiveClients) {
                	String user = in.readLine();
                	if(client.equals(currentUser)) {                		
                		continue;
                	}
                    gList.addElement(client);
                }
                clients.setModel(gList);

            }
        }
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}