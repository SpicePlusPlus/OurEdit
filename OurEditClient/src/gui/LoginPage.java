package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;

import client.ClientMain;
public class LoginPage implements ActionListener{
	
	private JFrame frame;
	JTextField userText;
	JPasswordField passwordText;
	private JButton loginButton;
	private JButton cancelButton;
	
	
	public LoginPage() {
		
		try {
			// Use this if we want the Nimbus look and feel
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		frame = new JFrame(); 
		frame.setSize(300,200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel();
		frame.add(panel);
		panel.setLayout(null);
		
		JLabel label = new JLabel("Username");
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
		
		loginButton = new JButton("Log In");
		loginButton.addActionListener(this);
		loginButton.setBounds(185,80,80,25);
		panel.add(loginButton);
			
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setBounds(100,80,80,25);
		panel.add(cancelButton);
		
		frame.setVisible(true);
		
		frame.getRootPane().setDefaultButton(loginButton);
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == loginButton)
		{
			String username = userText.getText();
			String password = new String(passwordText.getPassword());
			String loginResponse = client.ClientMain.clientInterface.login(username, password);
			client.ClientMain.clientInterface.username = username;
			
			if (loginResponse.equals("Login success!")) {
				client.ClientMain.clientInterface.start();
				frame.dispose();
			} else {
				JOptionPane.showMessageDialog(frame,loginResponse,"Login error",JOptionPane.ERROR_MESSAGE);
			}
				
		} else if (e.getSource() == cancelButton) {
			new WelcomePage();
			frame.dispose();
		}
	}
	
	public static void main(String[] args) {
		new LoginPage();
	}

}
