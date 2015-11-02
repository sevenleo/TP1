import javax.swing.*;
import java.awt.*;
import java.awt.event.*;


public class ServerGUI extends JFrame implements ActionListener, WindowListener {
	
	private static final long serialVersionUID = 1L;
	private JButton stopStart;
	private JTextArea chat, event;
	private JTextField tPortNumber;
	private Server server;
	
	
	// server constructor that receive the port to listen to for connection as parameter
	ServerGUI(int port) {
		super("Chat Server - TP2015.2");
		server = null;
		// painel superior contem as informacoes da porta e o botao 'iniciar'
		JPanel painelsuperior = new JPanel();
		painelsuperior.add(new JLabel("Porta: "));
		tPortNumber = new JTextField("  " + port);
		painelsuperior.add(tPortNumber);
		
		stopStart = new JButton("Iniciar servidor");
		stopStart.addActionListener(this);
		painelsuperior.add(stopStart);
		add(painelsuperior, BorderLayout.NORTH);
		
		// painel central contem o historico do chat e a a caixa de eventos
		JPanel painelcentral = new JPanel(new GridLayout(2,1));
		chat = new JTextArea(80,80);
		chat.setEditable(false);
		
		appendRoom("Sala de bate-papo TP2015.2:\n");
		
		painelcentral.add(new JScrollPane(chat));
		event = new JTextArea(80,80);
		event.setEditable(false);
		
		//appendEvent("caixa de eventos:\n");
		
	
		painelcentral.add(new JScrollPane(event));	
		add(painelcentral);
		
		// listener para os botoes default da janela(fecha,maximiza,...
		addWindowListener(this);
		setSize(400, 600);
		setVisible(true);
	}		

	
	// caixas de texto
	void appendRoom(String str) {
		chat.append(str);
		chat.setCaretPosition(chat.getText().length() - 1); //atualiza posicao da caixa de texto
	}
	void appendEvent(String str) {
		event.append(str);
		//event.setCaretPosition(chat.getText().length() - 1); //atualiza posicao da caixa de texto
		//nao entendi o porque do erro?? ///TODO:
		
	}
	


	//listener para o botao iniciar
	public void actionPerformed(ActionEvent e) {
		//se o servidor ja esta iniciado finalizar , caso contrario iniciar
		if(server != null) {
			server.stop();
			server = null;
			tPortNumber.setEditable(true);
			stopStart.setText("Reiniciar servidor");
			return;
		}

		//iniciar
		// a porta de comunicacao deve ser maior que 1000
		if (Integer.parseInt(tPortNumber.getText().trim()) <1024) {
			appendEvent("Porta invalida, tente alguma maior que 1024.\n");
			return;
		}
		int port;
		try {
			port = Integer.parseInt(tPortNumber.getText().trim());
		}
		catch(Exception er) {
			appendEvent("numero de porta invalida");
			return;
		}
		

		//inicia engine e a thread responsaveis pelo servidor		
		server = new Server(port, this);
		new ServerRunning().start();
		stopStart.setText("Encerrar servidor");
		tPortNumber.setEditable(false);
	}
	

	

	//MANIPULAÇAO DOS BOTOES DA JANELA
	public void windowClosing(WindowEvent e) {
		if(server != null) {
			try {
				server.stop();			//ENCERRAR O SERVICO CASO O BOTAO FECHAR FOR PRESSIONADO
			} catch(Exception eClose) {
			}
			server = null;
		}
		dispose();
		System.exit(0);
	}
	// I can ignore the other WindowListener method
	public void windowClosed(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}

	
	/**========= EXECUCAO INDIVIDUAL =============**/
	
	
	public static void main(String[] arg) {
		new ServerGUI(1500);
	}
	
	
	
	/**====================================================**/
	/**========= THREAD DO SERVIDOR =============**/
	/**====================================================**/

	class ServerRunning extends Thread {
		public void run() {
			server.start();         //executando em looping
			// the server failed
			stopStart.setText("Reiniciar servidor");
			tPortNumber.setEditable(true);
			appendEvent("Servidor offline\n");
			server = null;
		}
	}

}


