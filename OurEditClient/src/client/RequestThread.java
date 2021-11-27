package client;

import java.io.*;
import java.net.*;

public class RequestThread implements Runnable {

	private BufferedReader inFromUser;
	private DataOutputStream outToServer;
	private Socket socket;

	public RequestThread(Socket socket, BufferedReader inFromUser, DataOutputStream outToServer) {
		this.socket = socket;
		this.inFromUser = inFromUser;
		this.outToServer = outToServer;
	}

	public void saveFile() {
		try {
			
			String id = inFromUser.readLine();
			outToServer.writeBytes(id+"\n");
			outToServer.flush();
			
			String s = "This string is going to be supplied by the\ntext editor.";
			
			int docLength = s.length();
			outToServer.writeBytes(docLength+"\n");
			outToServer.flush();
			
			outToServer.writeBytes(s);
			outToServer.flush();
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void run() {
		try {
			
			String s;
			while (!socket.isClosed()) {
				s = inFromUser.readLine();
				outToServer.writeBytes(s + "\n");
				outToServer.flush();
				System.out.println("Sent request to server.");
				if (s.equals("EXIT_0")) {
					break;
				} else if (s.equals("SAVE")) {
					saveFile();
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public void start() {
		Thread t = new Thread(this);
		t.start();
	}
}
