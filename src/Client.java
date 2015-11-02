import java.net.*;
import java.io.*;


public class Client  {

	// JANELA DE INTEREÇÃO
	private ClientGUI clientGui;
	
	// SERVIDOR
	private String server;
	
	//CLIENTE
	private String username;
	private int port;
	
	// ENTRADA E SAIDA DE DADOS
	private ObjectInputStream sInput;		// LEITURA ATRAVES DO CAMINHO ABERTO PELA CONEXAO AO SOCKET
	private ObjectOutputStream sOutput;		// ENVIO DE DADOS ATRAVES DO CAMINHO ABERTO PELA CONEXAO AO SOCKET
	private Socket socket;					// SOCKET: CAMINHO ATE O SERVIDOR [IP:PORTA]
	

	//construtor
	Client(String server, int port, String username, ClientGUI clientgui) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.clientGui = clientgui;
	}
	
	
	//mostrar texto no terminal ou no painel grafico
	private void display(String msg) {
			if(clientGui == null)
				System.out.println(msg);     	// terminal
			else
				clientGui.append(msg + "\n");	// JTextArea
	}
	
	
	/**====================================================**/
	/**================comunicacao=========================**/
	/**====================================================**/
	
	/*inicio da conexao*/
	
	public boolean start() {
		// Tenta se conectar ao servidor
		try {
			socket = new Socket(server, port); //assimila o socket e busca conexão caso a porta esteja aberta
		} 
		catch(Exception ec) {
			display("Não foi possível estabelecer a conexão com o servidor");
			return false;
		}
		
		String msg = "Conexão estabelecida com servidor [" + socket.getInetAddress() + ":" + socket.getPort()+"]";
		display(msg);
	
		
		
		//	Cria canal de entrada e canal de saida com servidor
		try
		{
			sInput  = new ObjectInputStream(socket.getInputStream());
			sOutput = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException eIO) {
			display("Não foi possível criar canais de comunicação");
			return false;
		}

		// INCIA THREAD ( escuta o servidor ) 
		new ListenFromServer().start();
		
		
		// tenta enviar a primeira mensagem
		try	{
			sOutput.writeObject(username);
		} catch (IOException eIO) {
			display("A comunicação não foi estabelecida por completo, a conexão será desfeita");
			disconnect();
			return false;
		}

		//retorna true para validar a conexão estabelecida
		return true;
	}



	// ENVIA MENSAGENS ATRAVES DO STREAM DE SAIDA
	void sendMessage(ChatMessage msg) {
		try {
			sOutput.writeObject(msg);
		}
		catch(IOException e) {
			display("Não foi possível enviar a mesagem !!!");
		}
	}


	// DESCONECTAR
	// FECHA OS DOIS STREAMS i/o E A CONEXAO COM O SOCKET
	private void disconnect() {
		
		try { 
			if(sInput != null) sInput.close();
		} catch(Exception e) {} 
		
		
		try {
			if(sOutput != null) sOutput.close();
		} catch(Exception e) {} 
		
		
        try{
			if(socket != null) socket.close();
		} catch(Exception e) {}
		
        // informa ao Gui
        if(clientGui != null) clientGui.connectionFailed();
			
	}

	
	
	/**====================================================**/
	/**============ Thread de escuta contínua =============**/
	/**====================================================**/

	// o cliente espera pelas mensagens que virão através do servidor
	
	class ListenFromServer extends Thread {

		public void run() {
			display("Bem vindo à sala TP-2015.2:\n");
			while(true) {
				try {
					
					String msg = (String) sInput.readObject();
					if(clientGui == null) {
						System.out.println(msg);
						System.out.print("> "); //sinal que indica que o terminal esta pronto para uso
					}
					else {
						clientGui.append(msg); //envia ao GUi
					}
					
				} catch(IOException e) {
					display("A conexão com o servidor foi interrompida/finalizada.");
					if(clientGui != null) 
						clientGui.connectionFailed();
					break;
				} catch(ClassNotFoundException e) {
					display("A conexão com o servidor foi interrompida/finalizada.");
					if(clientGui != null) 
						clientGui.connectionFailed();
					break;
				}
			}
		}
	}
}
