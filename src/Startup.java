import javax.swing.*;
import java.awt.event.*;

public class Startup {

	
	public static void main (String[] args){    
	  JFrame frame = new JFrame("Escolha");
	  frame.setVisible(true);
	  frame.setSize(200,100);
	  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	  JPanel panel = new JPanel();
	  frame.add(panel);
	  JButton button = new JButton("Servidor");
	  panel.add(button);
	  button.addActionListener (new server());
	
	  JButton button2 = new JButton("Cliente");
	  panel.add(button2);
	  button2.addActionListener (new client());
	  
	  JButton button3 = new JButton("TESTE");
	  panel.add(button3);
	  button3.addActionListener (new test());
	  
	}
	
	
	static class server implements ActionListener {        
	  public void actionPerformed (ActionEvent e) {     
		  new ServerGUI(1500);
	  }
	}
	
	static class client implements ActionListener {        
	  public void actionPerformed (ActionEvent e) {     
		  new ClientGUI("localhost", 1500);
	  }
	}
	
	static class test implements ActionListener {        
		  public void actionPerformed (ActionEvent e) {     
			  new ClientGUI("localhost", 1500);
			  new ClientGUI("localhost", 1500);
			  new ServerGUI(1500);
		  }
		}   

}