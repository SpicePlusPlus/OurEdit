package gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import client.ClientMain;

public class Signup implements ActionListener {
	
	private JFrame frame;
	
	JTextField userText;
	JPasswordField passwordText;
	JPasswordField repasswordText;
	
	private JButton signupButton;
	private JButton cancelButton;
	
	public Signup()
	{
		// Contains 3 text fields: One for the username, one for the password, and one for repeating the password
		// Send the 3 strings associated with each field to the server when the signup button is clicked
		
		try {
			// Use this if we want the Nimbus look and feel
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		frame = new JFrame();
		frame.setSize(350,200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel();
		frame.add(panel);
		panel.setLayout(null);
		
		JLabel label = new JLabel("User");
		label.setBounds(10,20,80,25);
		panel.add(label);
		
		userText = new JTextField(20);
		userText.setBounds(100,20,165,25);
		panel.add(userText);
		
		JLabel passwordLabel = new JLabel ("Password");
		passwordLabel.setBounds(10,50,80,25);
		panel.add(passwordLabel);
		
		passwordText = new JPasswordField();
		passwordText.setBounds(100,50,165,25);
		panel.add(passwordText);
		
		JLabel repasswordLabel = new JLabel ("Re-Enter Password");
		repasswordLabel.setBounds(10,80,80,25);
		panel.add(repasswordLabel);
		
		repasswordText = new JPasswordField();
		repasswordText.setBounds(100,80,165,25);
		panel.add(repasswordText);
		
		signupButton = new JButton("Sign Up");
		signupButton.setBounds(182,110,80,25);
		signupButton.addActionListener(this);
		panel.add(signupButton);
		
		cancelButton = new JButton("Cancel");
		cancelButton.setBounds(100,110,80,25);
		cancelButton.addActionListener(this);
		panel.add(cancelButton);
		
		frame.setVisible(true);
		
		frame.getRootPane().setDefaultButton(signupButton);
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == signupButton) {
			
			String username = userText.getText();
			String password = new String(passwordText.getPassword());
			String repeatPassword = new String(repasswordText.getPassword());
			
			String signupResponse = client.ClientMain.clientInterface.signup(username, password, repeatPassword);
			// Calls the signup function in clientInterface and waits for the response, then acts accordingly
			
			if (signupResponse.equals("Signup success!")) {
				JOptionPane.showMessageDialog(frame, signupResponse, "Success", JOptionPane.INFORMATION_MESSAGE);
				new WelcomePage();
				frame.dispose();
				// If success, goes back to the welcome page and disposes of this page
			} else {
				JOptionPane.showMessageDialog(frame, signupResponse, "Signup error", JOptionPane.ERROR_MESSAGE);
				// Otherwise shows an error dialog
			}
			
		} else if (e.getSource() == cancelButton) {
			new WelcomePage();
			frame.dispose();
		}
	}
	
	public static void main(String[] args) {
		new Signup();

	}
}


