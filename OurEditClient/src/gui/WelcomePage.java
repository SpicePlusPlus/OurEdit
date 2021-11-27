package gui;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import java.awt.Font;

public class WelcomePage implements ActionListener{
		private JFrame frame;
		private JButton LoginButton;
		private JButton SignUpButton;
		private JLabel Welcome;
		private JPanel panel;
				
	public WelcomePage() {
		
		// Welcome page contains two buttons: LoginButton and SignupButton, which have ActionListeners that
		// open the LoginPage and the Signup page respectively
		
		try {
			// Use this if we want the Nimbus look and feel
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		frame = new JFrame();
		LoginButton = new JButton("Log in");
		LoginButton.setFont(new Font("Tahoma", Font.PLAIN, 16));
		LoginButton.addActionListener(this);
		
		SignUpButton = new JButton("Sign up");
		SignUpButton.setFont(new Font("Tahoma", Font.PLAIN, 16));
		SignUpButton.addActionListener(this);
		
		Welcome = new JLabel("Welcome to OurEdit!");
		Welcome.setFont(new Font("Tahoma", Font.PLAIN, 23));
		
		panel = new JPanel();
		panel.setBorder(BorderFactory.createEmptyBorder(30,30,10,30));
		panel.setLayout(new GridLayout(0,1));
		
		panel.add(Welcome);
		panel.add(LoginButton);
		panel.add(SignUpButton);
		
		frame.getContentPane().add(panel,BorderLayout.CENTER);
		frame.setSize(363,273);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Welcome to OurEdit");

		frame.setVisible(true);

	}
	
	public static void main(String[] args) {
		new WelcomePage();
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()== LoginButton)
		{ // If login is pressed, a new LoginPage is created, and this page is killed
			new LoginPage();
			frame.dispose();
		}
		if(e.getSource()== SignUpButton)
		{ // Likewise, a new Signup page is created, and this page is killed
			new Signup();
			frame.dispose();
		}
		
	}

}
