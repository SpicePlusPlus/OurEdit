package server;

import java.io.*;
import java.net.*;

public class AdminThread extends Thread {
	/*
	 *  The admin thread opens a server-side console that constantly awaits input commands.
	 *  Implemented commands are "stop" that stops the server and "broadcast" that sends a
	 *  test message to all clients.
	 */
	
	public BufferedReader console; // Declare buffered reader for taking server console inputs

	public AdminThread() { // Instantiate class and create input stream from server console
		this.console = new BufferedReader(new InputStreamReader(System.in));
	}

	public void run() {
		try {
			String s; // String to store console commands
			System.out.println("Booting server");
			while (true) {
				s = console.readLine(); // Read line from input stream and store in s
				if (s.equals("stop")) { // "stop" command stops server
					System.out.println("Stopping server");
					ServerMain.running = false; // Stop while loop in server main by setting running = false
					ServerMain.welcome.close(); // Close server welcome socket which throws a SocketException
					break;
				} else if (s.equals("broadcast")) { // Broadcast a test message to all users by looping through hashmap
					ServerMain.clientThreadTable.forEach((k,v) -> v.sendMessage("Test message from server to client " + v.clientNumber, "MSG"));
				}
			}
		} catch (IOException ioe) { // Handle IO exception
			System.out.println("IO Exception occurred in admin console. Closing server.");
			ServerMain.running = false; 
			// Loop in server main checks if admin thread is alive, so this is not needed to close that,
			// but it is needed to stop client handler thread while loop if any clients are still connected.
			ioe.printStackTrace();
		}
	}
}
