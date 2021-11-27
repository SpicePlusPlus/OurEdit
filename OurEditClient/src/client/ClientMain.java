package client;

import java.io.*;
import java.net.*;

public class ClientMain {
	
	public static ClientInterface clientInterface;
	
	public static void main(String[] args) throws Exception {

		int portNumber = 3555;
		InetAddress hostname = InetAddress.getLocalHost();

		try {
			// Creating client socket
			Socket clientSocket = new Socket(hostname, portNumber);
			
			// Creating output stream attached to socket
			DataOutputStream outToServer = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
			// Creating input stream attached to socket
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			// Creating input stream from user console
			BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
			
			clientInterface = new ClientInterface(clientSocket, outToServer, inFromServer, inFromUser);
			// Instantiates a new ClientInterface object (which is a Runnable), then waits for LoginPage to call start()
			// Wait for LoginPage to call start()
			
		} catch (ConnectException c) {
			System.out.println("Can not connect to server. Closing connection.");
			c.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("IO Exception occurred. Closing connection.");
			ioe.printStackTrace();
		}
	}
}
