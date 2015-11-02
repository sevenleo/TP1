import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;



public class ClientGUI extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	private JLabel label;
	private JTextField tf;
	private JTextField tfServer, tfPort;
	private JButton login, logout, whoIsIn;
	private JTextArea ta;
	private boolean connected;
	private Client client;
	private int defaultPort;
	private String defaultHost;
	public int random;

	//construtor recebe um socket
	ClientGUI(String host, int port) {

		super("Bate-papo: TP2015.2");
		defaultPort = port;
		defaultHost = host;
		
		// O painel superior contem as informacoes de porta e servidor
		JPanel painelsuperior = new JPanel(new GridLayout(3,1));
		JPanel serverAndPort = new JPanel(new GridLayout(1,5, 1, 3));

		tfServer = new JTextField(host);
		tfPort = new JTextField("" + port);
		tfPort.setHorizontalAlignment(SwingConstants.RIGHT);

		serverAndPort.add(new JLabel("Servidor:  "));
		serverAndPort.add(tfServer);
		serverAndPort.add(new JLabel("Porta:  "));
		serverAndPort.add(tfPort);
		serverAndPort.add(new JLabel(""));

		painelsuperior.add(serverAndPort);

		// campos de nome do usuario
		label = new JLabel("Com qual nome de usuario deseja entrar na sala?", SwingConstants.CENTER);
		painelsuperior.add(label);
		
		random=new Random().nextInt(100);
		tf = new JTextField("aluno"+random);
		
		tf.setBackground(Color.WHITE);
		painelsuperior.add(tf);
		add(painelsuperior, BorderLayout.NORTH);

		// caixa de texto da sala
		ta = new JTextArea("Bem vindo\n", 80, 80);
		JPanel centerPanel = new JPanel(new GridLayout(1,1));
		centerPanel.add(new JScrollPane(ta));
		ta.setEditable(false);
		add(centerPanel, BorderLayout.CENTER);

		// botoes
		login = new JButton("LOGAR");
		login.addActionListener(this);
		logout = new JButton("SAIR");
		logout.addActionListener(this);
		logout.setEnabled(false);		
		whoIsIn = new JButton("Usuarios?");
		whoIsIn.addActionListener(this);
		whoIsIn.setEnabled(false);		

		JPanel southPanel = new JPanel();
		southPanel.add(login);
		southPanel.add(logout);
		southPanel.add(whoIsIn);
		add(southPanel, BorderLayout.SOUTH);

		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(600, 600);
		setVisible(true);
		tf.requestFocus();

	}

	// called by the Client to append text in the TextArea 
	void append(String str) {
		ta.append(str);
		ta.setCaretPosition(ta.getText().length() - 1);
	}
	
	// caso a conexao seja perdida, reverter as configurações da janela
	void connectionFailed() {
		login.setEnabled(true);
		logout.setEnabled(false);
		whoIsIn.setEnabled(false);
		label.setText("Com qual nome de usuario deseja entrar na sala?");
		tf.setText("aluno"+random);
		tfPort.setText("" + defaultPort);
		tfServer.setText(defaultHost);
		tfServer.setEditable(false);
		tfPort.setEditable(false);
		tf.removeActionListener(this);
		connected = false;
	}
		

	// listener dos botoes
	public void actionPerformed(ActionEvent e) {
		Object o = e.getSource();
		if(o == logout) {
			client.sendMessage(new ChatMessage(ChatMessage.LOGOUT, ""));
			return;
		}

		if(o == whoIsIn) {
			client.sendMessage(new ChatMessage(ChatMessage.WHOISIN, ""));				
			return;
		}


		if(connected) {
			client.sendMessage(new ChatMessage(ChatMessage.MESSAGE, tf.getText()));				
			tf.setText("");
			return;
		}
		

		if(o == login) {
			String username = tf.getText().trim();
			if(username.length() == 0)			//se o usuario esta em branco, ignorar
				return;

			String server = tfServer.getText().trim();
			if(server.length() == 0)			//se o servidor esta em branco, ignorar
				return;
			
			String portNumber = tfPort.getText().trim();//se a porta esta em branco ou for invalida, ignorar
			if(portNumber.length() == 0)
				return;
			int port = 0;
			try {
				port = Integer.parseInt(portNumber);
			}
			catch(Exception en) {
				return;   
			}

			
			//cria cliente
			client = new Client(server, port, username, this);
			if(!client.start()) 
				return;
			tf.setText("");
			label.setText("Envie suas mensagens aos outros participantes.");
			connected = true;		//seta conexao para que ocorram alteracoes na janela
			
			//ajusta janela
			login.setEnabled(false);
			logout.setEnabled(true);
			whoIsIn.setEnabled(true);
			tfServer.setEditable(false);
			tfPort.setEditable(false);
			tf.addActionListener(this);
		}

	}
	
	/**========= EXECUCAO INDIVIDUAL =============**/
	//por padrao conectar-se na maquina local pela porta 1500
	public static void main(String[] args) {
		new ClientGUI("localhost", 1500);
	}

}
