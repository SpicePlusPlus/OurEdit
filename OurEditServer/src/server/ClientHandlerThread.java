package server;

import java.io.*;
import java.net.*;
import java.sql.*;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar;

public class ClientHandlerThread implements Runnable {
	/*
	 * Thread that handles client connections
	 */

	private Socket socket; 
	private BufferedReader inFromClient; 
	private DataOutputStream outToClient;
	// Declare socket member variable, and input and output streams attached to socket as member variables
	
	private Connection DBConn;
	// Declare database connection as member variable, which ServerMain passes as input to the constructor.
	// A single database connection is used throughout the server's lifetime.
	
	private static int totalClients = 0; 
	public int clientNumber;
	// totalClients tracks total client connections over server lifetime, incrementing with each new connection.
	// clientNumber is to track the number of this current instance of ClientHandlerThread for use in the clientThredTable
	// concurrent hashmap. Its value is copied from totalClients.
	
	public String username;
	// Client username entered after successful login.

	public ClientHandlerThread(Socket socket, Connection DBConn) {
		totalClients++; // Increment client number with every instantiation of this thread
		clientNumber = totalClients; // Save number of current client in a local variable associated with the instance
		
		this.socket = socket; // Allocate socket member variable to socket created externally
		this.DBConn = DBConn; // Allocate connection member variable to database connection created externally
	}

	public boolean login(String username, String password) {
		/*
		 * login() checks inputed username and password against 'users' table in the database.
		 * If username is not found, sends error message.
		 * If password is incorrect, also sends an error message.
		 * Sends successful login message if username and pass match, which client parses accordingly.
		 */
		
		try {
			
			Statement stmt = DBConn.createStatement();
			ResultSet rset = stmt.executeQuery("select * from users where username = '" + username + "'");
			// Create statement and query username and password entry from 'users' table.
			
			if (!rset.next()) { // If username is not found (empty query result), send error message and return false.
				outToClient.writeBytes("Invalid username!\n");
				outToClient.flush();
				return false;
			}
			
			String truePassword = rset.getString("password");
			if (!truePassword.equals(password)) { 
				// If inputted password does not match password in table, send error message and return false
				outToClient.writeBytes("Invalid password!\n");
				outToClient.flush();
				return false;
			} else { // Otherwise, send success message if both username and pass are valid.
				outToClient.writeBytes("Login success!\n");
				outToClient.flush();
				this.username = username;
				return true;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void signup(String username, String password, String repeatPassword) {
		/*
		 *  Creates new username and password entry in 'users' table.
		 *  Only creates new entry if username does not exist, and if password and repeated password
		 *  match for confirmation.
		 */
		
		try {
			Statement stmt = DBConn.createStatement();
			ResultSet rset = stmt.executeQuery("select * from users where username = '" + username + "'");
			// Queries database to check whether username exists.
			
			if (rset.next()) { // If username exists (query result not empty), return error message and exit function.
				outToClient.writeBytes("Username already exists.\n");
				outToClient.flush();
				return;
			} else if (!password.equals(repeatPassword)) { // If repeated password does not match password, return error and exit.
				outToClient.writeBytes("Passwords do not match.\n");
				outToClient.flush();
				return;
			} 
			
			// Otherwise, insert new username and password into database, and send success message if no errors during insertion.
			int updateResult = stmt.executeUpdate("insert into users values ('" + username + "', '" + password + "')");
			if (updateResult != 0) {
				outToClient.writeBytes("Signup success!\n");
				outToClient.flush();
			} else { // If error occurs, print error message to server console and send to client.
				outToClient.writeBytes("Error occurred while creating username in database.\n");
				outToClient.flush();
				System.out.println("Failed to create user. An error occurred.");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pushFileList() {
		/*
		 * Pushes all entries in 'documents' table to client.
		 * Client then displays entries in GUI table so the user may choose a document and perform and operation
		 * based on that entry.
		 */
		
		try {
			
			outToClient.writeBytes("GET_FILES\n");
			outToClient.flush();
			// Send GET_FILES message to client interface so that it may call its own function to receive files.

			Statement stmt = DBConn.createStatement();
			String s;
			// Create statement from database connection

			String query = "select count(*) from documents";
			ResultSet count = stmt.executeQuery(query);
			count.next();
			outToClient.writeBytes(count.getInt("count(*)") + "\n");
			outToClient.flush();
			// Queries number of entries (rows) in 'documents' table, so client may know how many lines to read
			
			query = "select * from documents";
			ResultSet rset = stmt.executeQuery(query);
			while (rset.next()) {
				s = rset.getInt("doc_id") + ", " + rset.getString("doc_title") + ", " + rset.getString("creator_name")
						+ ", " + rset.getTimestamp("first_created") + ", " + rset.getTimestamp("last_modified") + ", "
						+ rset.getBoolean("in_use") + ", " + rset.getString("cur_user")
						+ ", " + rset.getString("marked_for_deletion_by") + ", " + rset.getString("deletion_confirmations");
				System.out.println(s);
				outToClient.writeBytes(s + "\n");
				outToClient.flush();
			}
			// Queries database to return all rows in 'documents' table, then send seach row line by line to client
			// after parsing it as a string.
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendMessage(String message, String messageCode) {
		/*
		 * Function to send messages with custom message codes to user.
		 * Message code determines the action the user will perform upon receiving the message.
		 * Used in most functions below.
		 */
		try {
		outToClient.writeBytes(messageCode+"\n");
		outToClient.flush();
		outToClient.writeBytes(message+"\n");
		outToClient.flush();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void broadcast(String message, String messageCode, boolean currentUserIncluded) {
		/*
		 * Broadcasts message with custom message code to all users by looping over all entries in ConcurrentHashMap
		 * clientThreadTable using its special forEach() function. 
		 * If currentUserIncluded is true, sends message to user communicating with this thread as well, otherwise
		 * sends to all other users only.
		 */
		ServerMain.clientThreadTable.forEach((k,v) -> {
			if (currentUserIncluded || k != clientNumber) v.sendMessage(message, messageCode);
			System.out.println("Broadcasting to client " + k);
		});
	}

	public void createNew(String docTitle) {
		/*
		 * Creates new document by creating a new entry in the database with the provided docTitle as a
		 * document title, issuing it a new ID (through a mechanism described shortly), and creating a new 
		 * text file in the server folder with the name format [docID]_[docTitle].
		 * Broadcasts message to all clients upon successful file creation.
		 */
		try {
			Statement stmt = DBConn.createStatement();
			// Create statement from database connection
			
			ResultSet docIDFetch = stmt.executeQuery("select * from ids where id_type = 'doc_id'");
			docIDFetch.next();
			int doc_ID = docIDFetch.getInt("cur_id");
			stmt.executeUpdate("update ids set cur_id = cur_id + 1 where id_type = 'doc_id'");
			// A table called 'docs' in the database has two columns: id_type (string) and cur_id (integer).
			// Only one row in the table is used for document ids, which has id_type = 'doc_id'.
			// Function queries database for cur_id, assigns it to the newly created document, then increments cur_id
			// for that entry. That way the server has a permanent tally of IDs that does not reset when the server is restarted.
			
			ResultSet rset = stmt.executeQuery("select * from documents where doc_id = " + doc_ID);
			System.out.println("Creating file.");
			
			if (rset.next()) {
				sendMessage("Cannot create file with duplicate ID.","MSG");
				return;
			}
			// Block of code to check for duplicate ID. Used to be that IDs were sent by the user before automatic ID system
			// was created. Is now redundant, but serves as a check if things go wrong.
			
			int updateResult = stmt.executeUpdate("insert into documents values (" 
					+ doc_ID + ", '" + docTitle + "', '" + username + "', now(), now(), false, 'None', 'None', 0)");
			// Updates table with new document entry. 
			
			if (updateResult != 0) { // If update is successful...
				
				File document = new File(doc_ID + "_" + docTitle + ".txt");
				boolean creationSuccess = document.createNewFile();
				if (creationSuccess) System.out.println("File created successfully.");
				else System.out.println("Failed to create file.");
				// Create a new text file in the server folder with the naming scheme docID_docTitle.txt.
				// Print success message if creation success, error message if fail.
				
				broadcast("Document " + docTitle + " with ID " + doc_ID + " created by user " + username + ". ", "REFRESH_MSG", true);
				// Broadcast creation of new document by the current user with given title and ID to all clients.
				
			} else { // Send error message to client if creation of database entry fails.
				sendMessage("Failed to create document " + docTitle + " with ID " + doc_ID + ". An error occurred.", "MSG");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void markForDeletion(String username, int doc_ID) {
		/*
		 * Called when user sends a DELETE command. 
		 * Marks a document for deletion by changing the marked_for_deletion_by column in the entry from 'None'
		 * to the username of the current user. If marking is successful, broadcasts to all clients requesting confirmation.
		 */
		try {
			System.out.println("Document ID: " + doc_ID);
			Statement stmt = DBConn.createStatement();
			// Create statement from database connection
			
			ResultSet docTitleContainer = stmt.executeQuery("select doc_title from documents where doc_id = " + doc_ID);
			docTitleContainer.next();
			String doc_title = docTitleContainer.getString("doc_title");
			// Get document title from querying the database from the ID for broadcasting

			int updateResult = stmt.executeUpdate("update documents set marked_for_deletion_by = '" + username + "' where doc_ID = " + doc_ID);
			if (updateResult != 0) { // Execute update to mark document for deletion, and if update executse successfully...
				
				confirmOrDenyDelete("CONFIRM", doc_ID, username, false);
				// Call confirmOrDenyDelete function to confirm the document's deletion by the current user
				// (server checks whether the deletion_confirmations parameter in the database is equal to the current
				// number of users, then deletes document).
				
				System.out.println("Marked document for deletion.");
				broadcast("Document " + doc_title + " with ID " + doc_ID + " marked for deletion. Confirm?", "REFRESH_MSG", false);
				// Broadcasts deletion message, with code "REFRESH_MSG" for user to request a refresh for the files table
				// (through issuing a new GET_FILES command)
				
			} else { // Otherwise print and send error messages
				System.out.println("Unable to mark document for deletion.");
				sendMessage("Unable to mark document for deletion. The document has already been marked by this user or does not exist.", "MSG");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void unmarkForDeletion(boolean byUsername, String identifier) {
		/*
		 * Unmarks a document already marked for deletion.
		 * If byUsername is true, unmarks all documents marked by the username "identifier".
		 * If byUsername is false, identifier must be the document ID, and the function unmarks
		 * the document with that ID.
		 */
		try {
			Statement stmt = DBConn.createStatement();
			if (byUsername) {
				stmt.executeUpdate("update documents set deletion_confirmations = 0 where marked_for_deletion_by = '" + identifier + "'");
				int result = stmt.executeUpdate(
						"update documents set marked_for_deletion_by = 'None' where marked_for_deletion_by = '"+ identifier + "'");
				System.out.println("Unmarked " + result + " documents for deletion.");
			} else {
				stmt.executeUpdate("update documents set deletion_confirmations = 0 where doc_id = '" + identifier + "'");
				stmt.executeUpdate(
						"update documents set marked_for_deletion_by = 'None' where doc_id = '"+ identifier + "'");
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void confirmOrDenyDelete(String command, int doc_ID, String username, boolean hasAlreadyMarked) {
		/*
		 * Confirms or denies deletion of the document with ID doc_ID, based on whether command == "CONFIRM"
		 * or command == "DENY". If the user issuing the confirmation is the same as the one who marked the document
		 * for deletion, refuses to confirm deletion, unless override is true (which is only done once in the markForDeletion()
		 * function).
		 */
		try {
			Statement stmt = DBConn.createStatement();
			ResultSet rset = stmt.executeQuery("select * from documents where doc_id = " + doc_ID);
			if (!rset.next()) {
				System.out.println("Document does not exist.");
				sendMessage("Cannot confirm nor deny deletion. Document with ID " + doc_ID + " does not exist.", "MSG");
				return;
			// Create statement from database connection, and query database for row with doc_id = doc_ID. If rset is empty
			// (rset.next() returns false), send error message to client and exit function.
				
			} else if (rset.getString("marked_for_deletion_by").equals("None")) {
				System.out.println("Document unmarked for deletion.");
				sendMessage("Document unmarked for deletion.", "MSG");
				return;
			}
			// If marked_for_deletion_by field is 'None', send error message telling user that document is unmarked
			// for deletion.
			
			if (command.equals("CONFIRM")) { // If the command is CONFIRM...
				if (hasAlreadyMarked) {
					System.out.println("User already marked document for deletion.");
					sendMessage("You have already marked this document for deletion.", "MSG");
					return;
				}
				// If user who marked this document for deletion has already marked it, refuse to confirm
				// document for deletion. Send error message and exit function.
				
				stmt.executeUpdate("update documents set deletion_confirmations = deletion_confirmations + 1 where doc_ID = " + doc_ID);
				// Otherwise, increment deletion_confirmations by 1
				
				rset = stmt.executeQuery("select * from documents where doc_id = " + doc_ID);
				rset.next();
				long currentUsers = ServerMain.clientThreadTable.mappingCount();
				String msgType = (currentUsers == rset.getInt("deletion_confirmations")) ? "MSG" : "REFRESH_MSG";
				// Above code is to check whether to send a refresh message to the client or a message that does not induce
				// a refresh (a GET_FILES request). If the number of confirmations is equal to the current number of users,
				// delete will be called and it will send a refresh message itself, so no need for another one.
				// Potentially helps in contention over input and output streams between getFiles and delete.
				
				broadcast("User " + username + " confirmed deletion of the document with ID " + doc_ID, msgType, false);
				// Broadcast confirmation of deletion to all users
				
				sendMessage(Integer.toString(doc_ID), "CONFIRM_SUCCESS");
				// Send confirmation message to current user to remove document ID from clientSide list of IDs marked for deletion
				// (docsMarkedForDeletion).
				
				delete(doc_ID, false);
				// Call delete on this document.
				
			} else { // If the command is DENY...
				unmarkForDeletion(false, Integer.toString(doc_ID));
				broadcast("User " + username + " denied deletion of the document with ID " + doc_ID, "REFRESH_MSG", true);
				System.out.println("Document deletion denied. ");
				// Document is immediately unmarked for deletion, and a broadcast is sent to all users regarding that.
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void checkAllForDeletion() {
		/*
		 * This function is called when the current user disconnects (sends an EXIT_0 message to the server or
		 * disconnects through a socket closure which throws an exception).
		 * Queries database for documents where the number of deletion confirmations is equal to the current number of 
		 * users, then calls the delete() function on the resulting entries.
		 */
		try {
			long currentUsers = ServerMain.clientThreadTable.mappingCount();
			// The number of mappings in the hashmap determines the current total number of users.
			
			Statement stmt = DBConn.createStatement();
			ResultSet rset = stmt.executeQuery("select doc_id from documents where deletion_confirmations = " + currentUsers);
			// Queries database for the number of documents with deletion confirmations  equal to currentUsers, then calls delete()
			// on the result set
			while(rset.next()) {
				delete(rset.getInt("doc_id"), false);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void delete(int doc_ID, boolean forceDelete) {
		/*
		 * Deletes document if the current document is marked for deletion and the number of deletion
		 * confirmations is equal to the current number of users on the server, or if forceDelete
		 * is true if a document needs to be deleted unconditionally.
		 * Document is then deleted through deleting the database entry and deleting the file in the
		 * server folder.
		 */
		try {
			
			Statement stmt = DBConn.createStatement();
			ResultSet rset = stmt.executeQuery("select * from documents where doc_id = " + doc_ID);
			if(!rset.next()) {
				System.out.println("Document does not exist.");
				return;
			}
			// Query database for document with given document ID. If the document does not exist (rset.next()
			// returns false), print an error message to the server console and exit the function.
			
			boolean markedForDeletion = !rset.getString("marked_for_deletion_by").equals("None");
			// Tells whether document is marked for deletion
			long currentUsers = ServerMain.clientThreadTable.mappingCount();
			// Checks current number of users through the number of mappings in the clientThreadTable hashmap
			int deletionConfirmations = rset.getInt("deletion_confirmations");
			// Get number of deletion confirmations from result set
		
			if (forceDelete || (markedForDeletion && deletionConfirmations == currentUsers)) {
				// If the above condition is true...
				
				String docTitle = rset.getString("doc_title");
				File document = new File(doc_ID + "_" + docTitle + ".txt");
				
				if (document.delete()) {
					System.out.println("File successfully deleted.");
				} else {
					System.out.println("File deletion failed. File does not exist.");
					
				}
				// Open the document as a File then try deleting the file on the server, printing an error message on failure
				
				int result = stmt.executeUpdate("delete from documents where doc_id = " + doc_ID);
				// Delete entry for this document in database
				
				if (result != 0) {
					System.out.println("File entry deleted successfully.");
					broadcast("Document " + docTitle + " with ID " + doc_ID + " has been deleted successfully.", "REFRESH_MSG", true);
					sendMessage(Integer.toString(doc_ID), "DELETION_SUCCESS");
					// If document is deleted successfully, broadcast a success message and send the document ID as a 
					// DELETION_SUCCESS message for the client to parse and remove it from its local list of documents
					// marked for deletion
				}
				else {
					System.out.println("Entry deletion unsuccessful.");
					broadcast("An error occurred. Document " + docTitle + " with ID " + doc_ID + " can not be deleted.", "REFRESH_MSG", true);
				} // Otherwise print an error message
				
			} else {
				System.out.println("File can not be deleted yet. " + deletionConfirmations + "/" + currentUsers + " confirmed deletion.");
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void openFile(int doc_ID) {
		/*
		 * Checks if document is currently open by a user. If so, sends an error message which opens a dialog.
		 * Otherwise, reads document file using a buffered reader into a string, and sends that string
		 * through the socket output stream to the client for it to display in the text editor, and marks
		 * document as in use.
		 */
		try {
			
			Statement stmt = DBConn.createStatement();
			ResultSet rset = stmt.executeQuery("select * from documents where doc_ID = " + doc_ID);
			rset.next();
			// Queries database for entry of document with the provided document ID
			
			boolean inUse = rset.getBoolean("in_use");
			String currentUser = rset.getString("cur_user");
			if (inUse) {
				sendMessage("Document currently being edited by " + currentUser, "OPEN_ERROR");
				return;
			}
			// If the document is in use, send a message that opens an error dialog box. Otherwise...
			
			String docTitle = rset.getString("doc_title");
			stmt.executeUpdate("update documents set in_use = 1, cur_user = '" + username + "' where doc_ID = " + doc_ID);
			// Mark document as in_use
			
			outToClient.writeBytes("OPEN\n");
			outToClient.flush();
			outToClient.writeBytes(doc_ID+"\n");
			outToClient.flush();
			outToClient.writeBytes(docTitle+"\n");
			outToClient.flush();
			// Send OPEN code to call openFile() function on clientSide
			
			File document = new File(doc_ID + "_" + docTitle + ".txt");
			int docLength = (int) document.length();
			
			outToClient.writeBytes(docLength + "\n");
			outToClient.flush();
			System.out.println(docLength);
			// Get document size in bytes (aka number of characters) and sends it to client to prepare to receive file string
			
			BufferedReader docReader = new BufferedReader(new FileReader(document));
			char[] docChars = new char[docLength];
			docReader.read(docChars, 0, docLength);
			String docContents = new String(docChars);
			// Read document contents from file
			
			outToClient.writeBytes(docContents);
			outToClient.flush();
			// Send document contents to client 
			
			broadcast("User " + username + " opened document " + docTitle + " with ID " + doc_ID + " for editing.", "REFRESH_MSG", false);
			// Broadcast document opening to users
			
			docReader.close();
			
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} catch (FileNotFoundException fle) {
			fle.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void closeFile(int doc_ID) { 
		/*
		 * Closes file by marking it as not in use (in_use = 0) and broadcasting that the file is now closed
		 */
		try {
			
			Statement stmt = DBConn.createStatement();
			stmt.executeUpdate("update documents set in_use = 0, cur_user = 'None', last_modified = now() where doc_ID = " + doc_ID);
			
			ResultSet rset = stmt.executeQuery("select * from documents where doc_id = " + doc_ID);
			rset.next();
			String docTitle = rset.getString("doc_title");
			
			broadcast("User " + username + " closed document " + docTitle + " with ID " + doc_ID + ".", "REFRESH_MSG", false);
			
		} catch(SQLException sqle) {
			sqle.printStackTrace();
		}
	}
	
	public void saveFile(int doc_ID) {
		/*
		 * Takes string containing document contents from client (from the client's text box) and saves it in the correct file
		 * with name format DocID_DocTitle.txt.
		 */
		try {
			
			Statement stmt = DBConn.createStatement();
			ResultSet rset = stmt.executeQuery("select * from documents where doc_id = " + doc_ID);
			if (!rset.next()) {
				System.out.println("File does not exist.");
				sendMessage("Error saving file. File does not exist.", "MSG");
				return;
			}
			
			String docTitle = rset.getString("doc_title");
			int docLength = Integer.parseInt(inFromClient.readLine());
			
			char[] docChars = new char[docLength];
			inFromClient.read(docChars,0,docLength);
			String docContents = new String(docChars);
			FileWriter docWriter = new FileWriter(doc_ID + "_" + docTitle + ".txt");
			docWriter.write(docContents);
			docWriter.close();
			
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} catch(SQLException sqle) {
			sqle.printStackTrace();
		}
	}
	
	public void run() {

		System.out.println("Client " + Integer.toString(clientNumber) + " connected.");
		try {
			inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			// Create input stream connected to socket
			outToClient = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
			// Create output stream connected to socket
			String s;
			// String to store sent message

			// Initially, only takes LOGIN and SIGNUP commands from user until the user signs into a particular account.
			// Signup calls the signup() function, and login calls the login() function.
			boolean loginSuccess = false;
			// While user is not logged in...
			while (!loginSuccess) {
				s = inFromClient.readLine(); 
				// Takes LOGIN or SIGNUP command from user, then the command parameters and calls functions accordingly
				if (s.equals("LOGIN")) {
					
					String username = inFromClient.readLine();
					String password = inFromClient.readLine();
					loginSuccess = login(username, password);
					// If login() returns true (login success), breaks loop and goes into main loop.
					
				} else if (s.equals("SIGNUP")) {
					String username = inFromClient.readLine();
					String password = inFromClient.readLine();
					String repeatPassword = inFromClient.readLine();
					signup(username, password, repeatPassword);
					// Calls signup function if user wants to signup.
				}
			}

			while (ServerMain.running) {// All clients must be closed before server is closed (stop command is written in
										// admin console), but in case stop command is written before clients are closed,
										// this ensures that all client connections will be closed eventually (allows a
										// single extra input to be typed from client, however)
				s = inFromClient.readLine();
				// Fetch sent message from input stream
				if (s.equals("EXIT_0")) { 
					
					outToClient.writeBytes("EXIT_0\n"); 
					outToClient.flush();
					System.out.println("Client " + Integer.toString(clientNumber) + " closing connection.");
					ServerMain.clientThreadTable.remove(clientNumber);
					unmarkForDeletion(true, username);
					checkAllForDeletion();
					broadcast("User " + username + " left.", "REFRESH_MSG", false);
					break;
					// If client sends exit message: 
					// - Write to client interface EXIT_0 message for it to close
					// - Remove current client from HashMap
					// - Call unmarkForDeletion() function to unmark all documents marked by this user for deletion
					// - Checks other documents marked for deletion to see if any should be deleted
					// - Broadcast that the user has left
					// Exit the loop
					
					// Otherwise, parse each command accordingly, and wait for extra command parameters as consequent lines,
					// then call the respective function to be called
				} else if (s.equals("GET_FILES")) { 
					
					pushFileList();
					// GET_FILES pushes files to client
					
				} else if (s.equals("NEW")) {
					
					String doc_title = inFromClient.readLine();
					createNew(doc_title);
					
					// NEW waits for a document name and creates a new document with that name
					
				} else if (s.equals("DELETE")) {
					
					int doc_ID = Integer.parseInt(inFromClient.readLine());
					markForDeletion(username, doc_ID);
					// DELETE marks a document for deletion
					
				} else if (s.equals("CONFIRM_DELETE")) {
					
					int doc_ID = Integer.parseInt(inFromClient.readLine());
					boolean alreadyMarked = Boolean.parseBoolean(inFromClient.readLine());
					confirmOrDenyDelete("CONFIRM",doc_ID,username,alreadyMarked);
					// CONFIRM_DELETE confirms deletion of a marked document by a user 
					
				} else if (s.equals("DENY_DELETE")) {
					
					int doc_ID = Integer.parseInt(inFromClient.readLine());
					confirmOrDenyDelete("DENY",doc_ID,username,true);
					// DENY_DELETE denies deletion of a document and immediately unmarks it
					
				} else if (s.equals("OPEN")) {
					
					int doc_ID = Integer.parseInt(inFromClient.readLine());
					openFile(doc_ID);
					// OPEN sends document as text to be displayed in text editor
					
				} else if (s.equals("CLOSE")) {
					
					int doc_ID = Integer.parseInt(inFromClient.readLine());
					closeFile(doc_ID);
					// CLOSE closes document and allows other users to edit it

				} else if (s.equals("SAVE")) {
					
					int doc_ID = Integer.parseInt(inFromClient.readLine());
					saveFile(doc_ID);
					// SAVE takes document string from user and saves it in the correct filename
					
				} else {
					
					System.out.println(s); // Print client message to server console
					
				}
			}
			socket.close(); // Close client socket after loop ends

		} catch (IOException ioe) { // Catch exception and perform same closing actions as EXIT_0
			System.out.println(
					"IO Exception occurred. Closing connection to client " + Integer.toString(clientNumber) + ".");
			ServerMain.clientThreadTable.remove(clientNumber);
			unmarkForDeletion(true, username);
			checkAllForDeletion();
			broadcast("User " + username + " left.", "REFRESH_MSG", false);
			ioe.printStackTrace();
		}
	}

	public void start() { // This thread does not extend Thread, so create a start method that handles
							// thread creation
		Thread t = new Thread(this);
		t.start();
	}

}
