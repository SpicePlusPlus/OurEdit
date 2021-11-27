package gui;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.awt.event.*;
import javax.swing.text.*;

// Basic text editor code adapted (and heavily modified to suit our needs)
// from GeeksForGeeks: https://www.geeksforgeeks.org/java-swing-create-a-simple-text-editor/

public class TextEditor implements ActionListener {

	public DataOutputStream outToServer;
	public String docName;
	public int docID;

	private JTextArea textArea;
	private JFrame frame;

	private JMenuItem cut;
	private JMenuItem copy;
	private JMenuItem paste;
	private JMenuItem save;
	private JMenuItem close;

	// Constructor
	public TextEditor(String document, DataOutputStream outToServer, String docName, int docID) {

		this.docName = docName;
		this.docID = docID;
		this.outToServer = outToServer;

		try {
			// Use this if we want the Nimbus look and feel
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create a frame
		frame = new JFrame("editor");
		frame.setTitle(docName);

		// Text component
		textArea = new JTextArea();

		// Create a menubar
		JMenuBar menuBar = new JMenuBar();

		// Create a menu
		JMenu file = new JMenu("File");

		// Create menu items
		save = new JMenuItem("Save");
		save.addActionListener(this);

		file.add(save);

		// Create a menu
		JMenu edit = new JMenu("Edit");

		// Create menu items
		cut = new JMenuItem("Cut");
		copy = new JMenuItem("Copy");
		paste = new JMenuItem("Paste");

		cut.addActionListener(this);
		copy.addActionListener(this);
		paste.addActionListener(this);

		edit.add(cut);
		edit.add(copy);
		edit.add(paste);

		close = new JMenuItem("Close");

		close.addActionListener(this);

		menuBar.add(file);
		menuBar.add(edit);
		menuBar.add(close);

		frame.setJMenuBar(menuBar);
		frame.add(textArea);
		frame.setSize(500, 500);
		frame.setVisible(true);

		textArea.setText(document);
		
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				try {
					outToServer.writeBytes("CLOSE\n");
					outToServer.flush();

					outToServer.writeBytes(docID + "\n");
					outToServer.flush();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				frame.dispose();
			}
		});
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == cut) {
			textArea.cut();
		} else if (e.getSource() == copy) {
			textArea.copy();
		} else if (e.getSource() == paste) {
			textArea.paste();
		} else if (e.getSource() == save) {

			try {
				String words = textArea.getText();

				outToServer.writeBytes("SAVE\n");
				outToServer.flush();

				outToServer.writeBytes(docID + "\n");
				outToServer.flush();

				int docLength = words.length();
				outToServer.writeBytes(docLength + "\n");
				outToServer.flush();

				outToServer.writeBytes(words);
				outToServer.flush();

			} catch (Exception evt) {
				JOptionPane.showMessageDialog(frame, evt.getMessage());
			}
			// If the user cancelled the operation

		} else if (e.getSource() == close) {
			try {
				outToServer.writeBytes("CLOSE\n");
				outToServer.flush();

				outToServer.writeBytes(docID + "\n");
				outToServer.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			frame.dispose();
		}

	}

	// Main class
	public static void main(String args[]) {
		new TextEditor("", null, "", -1);
	}
}