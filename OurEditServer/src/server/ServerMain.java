package server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.concurrent.*;

public class ServerMain {

	public static int portNumber = 3555; // Socket number 
	public static boolean running = true; // Boolean for the loop that checks socket connections
	public static ServerSocket welcome; 
	// Welcome socket and running boolean are available to AdminThread and ClientHandler classes.
	public static ConcurrentHashMap<Integer, ClientHandlerThread> clientThreadTable = new ConcurrentHashMap<>();
	// A ConcurrentHashMap that tracks instances of ClientHandlerThread. The key for each ClientHandlerThread is
	// the client's clientNumber, which is taken from the static int totalClients which is incremented whenever a
	// new client connects. Used to broadcast messages to all clients and keep track of currently connected clients.
	// ConcurrentHashMap is more thread-safe than a regular HashMap.
	
	public static void main(String[] args) throws Exception {

		welcome = new ServerSocket(portNumber); // Create new ServerSocket to accept client connections

		try {
			
			Class.forName("com.mysql.jdbc.Driver"); // Throws exception in case SQL driver is absent
			Connection DBConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/oureditdb", "root", "");
			// Connect to database
			
			AdminThread admin = new AdminThread(); 
			admin.start();
			// Instantiate and start admin thread which takes input from server console.
			// Two commands are implemented in the admin thread: a debug broadcast command, and the server stop command.
			
			while (running && admin.isAlive()) { // While running is true and admin thread is alive...
				
				Socket serverSocket = welcome.accept(); // Accept client connection and create new socket
				if (!welcome.isClosed()) { // If welcome socket is closed, skip this
					
					ClientHandlerThread clientThread = new ClientHandlerThread(serverSocket, DBConn);
					clientThreadTable.put(clientThread.clientNumber, clientThread);
					// Instantiate new client handler thread, and place it in hashmap with clientNumber as key
					
					clientThread.start();
					// Start client handler thread
				}
			}
			if (!welcome.isClosed()) { // If welcome socket is not closed, close it.
				welcome.close();
			}
		} catch (SocketException s) { // This will mostly be caught from welcome socket
									  // when server is stopped by console through stop command
			System.out.println("Socket Exception occurred. Stopping server.");
			s.printStackTrace();
		} catch (IOException ioe) { // Handle any other possible IO exception that might occur
			System.out.println("IO Exception occurred. Stopping server.");
			ioe.printStackTrace();
		}
	}
	
	
}
