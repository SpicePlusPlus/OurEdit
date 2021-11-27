package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.io.*;

public class FilesPage implements ActionListener {

	JFrame frame;

	public DefaultTableModel dtm;
	public JTable table;
	JScrollPane scroll;

	String[] columnNames = { "Document ID", "Document Title", "Creator Name", "First Created", "Last Modified",
			"In Use", "Current User", "Marked for Deletion By", "Deletion Confirmations" };
	
	public DataOutputStream outToServer;
	String username;
	
	JButton newFile;
	JButton refresh;
	JButton exitButton;
	JButton openButton;
	JButton deleteButton;
	JButton denyDeleteButton;

	JTextPane messageBox;
	
	boolean selectionListenerFlag = true;
	// Flag that the ListSelectionListener added to the table model checks before executing.
	// This ensures that the listener *only* fires when the user selects an entry in the table.
	// Necessary since whenever the clearTable() method (shown underneath) is called to remove rows,
	// the listener and any listeners automatically get fired, and may get fired on an invalid row,
	// which throws an exception.

	public FilesPage(DataOutputStream outToServer, String username) {

		this.outToServer = outToServer;
		this.username = username;
		
		try {
			// Use this if we want the Nimbus look and feel
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		frame = new JFrame();
		frame.setSize(1280, 720);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel panel = new JPanel();
		frame.getContentPane().add(panel);
		panel.setLayout(null);

		newFile = new JButton("New");
		newFile.setBounds(1175, 46, 60, 25);
		panel.add(newFile);
		newFile.addActionListener(this);

		refresh = new JButton("Refresh");
		refresh.setBounds(1085, 46, 80, 25);
		panel.add(refresh);
		refresh.addActionListener(this);

		JLabel documents = new JLabel("Documents");
		documents.setFont(new Font("SansSerif", Font.PLAIN, 22));
		documents.setBounds(46, 49, 143, 25);
		panel.add(documents);

		JLabel ourEdit = new JLabel("OurEdit alpha");
		ourEdit.setFont(new Font("SansSerif", Font.BOLD, 24));
		ourEdit.setBounds(533, 0, 172, 39);
		panel.add(ourEdit);

		exitButton = new JButton("Exit");
		exitButton.setBounds(1128, 642, 100, 26);
		panel.add(exitButton);
		exitButton.addActionListener(this);

		openButton = new JButton("Open");
		openButton.setBounds(1128, 459, 100, 63);
		panel.add(openButton);
		openButton.addActionListener(this);

		deleteButton = new JButton("Delete");
		deleteButton.setBounds(1128, 534, 100, 63);
		panel.add(deleteButton);
		deleteButton.addActionListener(this);
		
		denyDeleteButton = new JButton("<html>Deny<br>deletion</html>");
		denyDeleteButton.setBounds(1128, 580, 100, 40);
		panel.add(denyDeleteButton);
		denyDeleteButton.addActionListener(this);
		denyDeleteButton.setVisible(false);

		dtm = new DefaultTableModel(null, columnNames);

		table = new JTable(dtm);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.setFillsViewportHeight(true);

		scroll = new JScrollPane(table);
		scroll.setBounds(46, 74, 1189, 373);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		panel.add(scroll);

		messageBox = new JTextPane();

		JScrollPane messageScroll = new JScrollPane(messageBox);
		messageScroll.setBounds(46, 500, 1035, 155);
		messageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		messageScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		panel.add(messageScroll);

		JLabel messages = new JLabel("Messages");
		messages.setFont(new Font("SansSerif", Font.PLAIN, 22));
		messages.setBounds(46, 474, 143, 25);
		panel.add(messages);
		
		JLabel usernameDisplay = new JLabel("Logged in as: " + username);
		usernameDisplay.setFont(new Font("SansSerif", Font.PLAIN, 14));
		usernameDisplay.setBounds(46, 6, 258, 25);
		panel.add(usernameDisplay);

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				if (selectionListenerFlag) {
					String isBeingDeleted = table.getValueAt(table.getSelectedRow(), 7).toString();
					if (!isBeingDeleted.equals("None")) {
						deleteButton.setText("<html>Confirm<br>deletion</html>");
						deleteButton.setBounds(1128, 534, 100, 40);
						denyDeleteButton.setVisible(true);
						panel.revalidate();
					} else {
						denyDeleteButton.setVisible(false);
						deleteButton.setText("Delete");
						deleteButton.setBounds(1128, 534, 100, 63);
						panel.revalidate();
					}
				}
			}
		});



		frame.setVisible(true);
		
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					outToServer.writeBytes("EXIT_0\n");
					outToServer.flush();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				System.exit(0);
			}
		});
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == refresh) {
			try {
				outToServer.writeBytes("GET_FILES\n");
				outToServer.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} else if (e.getSource() == newFile) {
			try {
				outToServer.writeBytes("NEW\n");
				outToServer.flush();
				String s = (String) JOptionPane.showInputDialog(frame, "Enter a name for the new document:",
						JOptionPane.PLAIN_MESSAGE);
				outToServer.writeBytes(s + "\n");
				outToServer.flush();
				
				outToServer.writeBytes("GET_FILES\n");
				outToServer.flush();
				
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} else if (e.getSource() == openButton && table.getSelectedRow() != -1) {
			try {
				outToServer.writeBytes("OPEN\n");
				outToServer.flush();
				outToServer.writeBytes(table.getValueAt(table.getSelectedRow(), 0).toString() + "\n");
				outToServer.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} else if (e.getSource() == deleteButton && table.getSelectedRow() != -1) {
			if (table.getValueAt(table.getSelectedRow(), 7).toString().equals("None")) {
				try {
					outToServer.writeBytes("DELETE\n");
					outToServer.flush();
					outToServer.writeBytes(table.getValueAt(table.getSelectedRow(), 0).toString() + "\n");
					outToServer.flush();
					
					outToServer.writeBytes("GET_FILES\n");
					outToServer.flush();
					
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			} else {
				try {
					outToServer.writeBytes("CONFIRM_DELETE\n");
					outToServer.flush();
					int doc_ID = Integer.parseInt(table.getValueAt(table.getSelectedRow(), 0).toString());
					outToServer.writeBytes(doc_ID + "\n");
					outToServer.flush();
					outToServer.writeBytes(client.ClientMain.clientInterface.docsMarkedForDeletion.contains(doc_ID)+"\n");
					outToServer.flush();

				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		} else if (e.getSource() == denyDeleteButton) {
			try {
				outToServer.writeBytes("DENY_DELETE\n");
				outToServer.flush();
				outToServer.writeBytes(table.getValueAt(table.getSelectedRow(), 0).toString() + "\n");
				outToServer.flush();
				
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		} else if (e.getSource() == exitButton) {
			try {
				outToServer.writeBytes("EXIT_0\n");
				outToServer.flush();
				frame.dispose();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				frame.dispose();
			}
		}
	}

	public void addRowToTable(String[] data) {
		selectionListenerFlag = false;
		dtm.addRow(data);
		selectionListenerFlag = true;
	}

	public void clearTable() {
		selectionListenerFlag = false;
		dtm.setRowCount(0);
		selectionListenerFlag = true;
	}

	public void postMessage(String message) {
		messageBox.setText(messageBox.getText() + "\n" + message);
	}

	public void errorDialogBox(String errorMessage) {
		try {
			JOptionPane.showMessageDialog(frame, errorMessage, "Cannot open file", JOptionPane.ERROR_MESSAGE);
			outToServer.writeBytes("GET_FILES\n");
			outToServer.flush();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new FilesPage(null,"");

	}
}