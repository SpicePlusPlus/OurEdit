package client;

import java.awt.EventQueue;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class ClientInterface implements Runnable {

	public Thread t;
	private Socket socket;
	
	
	public String username;
	
	private DataOutputStream outToServer;
	private BufferedReader inFromServer;
	private BufferedReader inFromUser;
	
	public boolean loginSuccess;
	
	
	public ArrayList<Integer> docsMarkedForDeletion;

	public ClientInterface(Socket socket, DataOutputStream outToServer, BufferedReader inFromServer,
			BufferedReader inFromUser) {
		this.socket = socket;
		this.outToServer = outToServer;
		this.inFromServer = inFromServer;
		this.inFromUser = inFromUser;
		this.loginSuccess = false;
		docsMarkedForDeletion = new ArrayList<>();
		// Initialize all member variables
		
		new gui.WelcomePage();
		System.out.println("GUI created.");
		// Create a new WelcomePage to start the GUI

	}

	public String login(String username, String password) {
		// Sends username and password to server, then returns response
		// Called from LoginPage
		try {
			outToServer.writeBytes("LOGIN\n");
			outToServer.flush();
			outToServer.writeBytes(username + "\n");
			outToServer.flush();
			outToServer.writeBytes(password + "\n");
			outToServer.flush();
			
			return inFromServer.readLine();

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String signup(String username, String password, String repeatPassword) {
		try {
			// Sends the three strings to the server and waits for the response (success or failure).
			// Called from Signup.
			outToServer.writeBytes("SIGNUP\n");
			outToServer.flush();
			outToServer.writeBytes(username + "\n");
			outToServer.flush();
			outToServer.writeBytes(password + "\n");
			outToServer.flush();
			outToServer.writeBytes(repeatPassword + "\n");
			outToServer.flush();
			
			return inFromServer.readLine();

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void getFileList(gui.FilesPage filesPage) {
		// Clears the table in Filespage, then 
		// gets server files as separate strings for each entry, 
		// then displays them in the table.
		try {
			int numFiles = Integer.parseInt(inFromServer.readLine());
			
			filesPage.clearTable();
			
			String s;
			for (int i = 0; i < numFiles; ++i) {
				s = inFromServer.readLine();
				String[] separated = s.split(", ");
				filesPage.addRowToTable(separated);
			}
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void openFile() {
		// Sends file ID to server then waits for the string with the document contents and displays it in the text editor
		try {
			int docID = Integer.parseInt(inFromServer.readLine());
			String docName = inFromServer.readLine();
			int size = Integer.parseInt(inFromServer.readLine());
			
			char[] docChars = new char[size];
			inFromServer.read(docChars, 0, size);
			String document = new String(docChars);
			
			new gui.TextEditor(document, outToServer, docName, docID);
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	

	public void run() {
		try {

			//RequestThread requestThread = new RequestThread(socket, inFromUser, outToServer);
			//requestThread.start();
			// Non-GUI request thread for debugging
		 
			gui.FilesPage filesPage = new gui.FilesPage(outToServer, username);
			
			outToServer.writeBytes("GET_FILES\n");
			outToServer.flush();
			
			String s;
			while (!socket.isClosed()) {
				s = inFromServer.readLine();

				if (s.equals("EXIT_0")) { // If client enters EXIT_0, close connection
					System.out.println("Disconnecting from server.");
					break;
				} else if (s.equals("GET_FILES")) {
					getFileList(filesPage);
				} else if (s.equals("MSG")) { // MSG is the code for a normal message, possibly from a debug broadcast
											  // Displays message in Messages box in the GUI
					s = inFromServer.readLine();
					filesPage.postMessage(s);
				} else if (s.equals("REFRESH_MSG")) { 
					// Special message code, used by the various delete functions and createNew()
					// Aside from displaying the message in the Messages box, automatically refreshes table
					s = inFromServer.readLine();
					filesPage.postMessage(s);
					outToServer.writeBytes("GET_FILES\n");
					outToServer.flush();
				} else if (s.equals("OPEN_ERROR")) { // OPEN_ERROR opens an error dialog box
					s = inFromServer.readLine();
					filesPage.errorDialogBox(s);
				} else if (s.equals("CONFIRM_SUCCESS")) {
					int confirmedDocID = Integer.parseInt(inFromServer.readLine());
					filesPage.postMessage("Confirmed deletion of document with ID " + confirmedDocID);
					docsMarkedForDeletion.add(confirmedDocID);
					outToServer.writeBytes("GET_FILES\n");
					outToServer.flush();
				} else if (s.equals("DELETE_SUCCESS") || s.equals("DELETE_FAIL")) {
					int deletedDocID = Integer.parseInt(inFromServer.readLine());
					docsMarkedForDeletion.remove(deletedDocID);
				} else if (s.equals("OPEN")) {
					openFile();
				}
			}
			socket.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void start() {
		t = new Thread(this);
		t.start();
	}

}
