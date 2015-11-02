import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
	
	private static int uniqueId;				//identifica cada conexao
	private ArrayList<ClientThread> clients; 	//lista de clientes
	private ServerGUI serverGui; 				//parte grafica - Gui
	private SimpleDateFormat horario; 			//registro de horario com o padrao HH:mm:ss
	private int port;							//porta aberta para conexao
	
	//antigo boolean keepGoing
	private boolean serverRunning;				//servidor se mantem ativo se essa boolean=true
	private Map clienteMap;						//linkar username<>thread.id de cada cliente


	//construtor usado para servidor SEM janela grafica
	public Server(int port) {
		this(port, null);
	}
	
	//construtor usado para servidor COM janela grafica
	public Server(int port, ServerGUI servergui) {
		this.serverGui = servergui;
		this.port = port;
		horario = new SimpleDateFormat("HH:mm:ss");
		clients = new ArrayList<ClientThread>();
		clienteMap = new HashMap<String,ClientThread>();
	}
	
	
	/**====================================================**/
	/**================comunicacao=========================**/
	/**====================================================**/
	
	//inicia servidor
	public void start() {
		serverRunning = true;
		try {
			// Abre o seu socket para conexões de clientes
			ServerSocket serverSocket = new ServerSocket(port);

			// espera conexoes de clientes
			while(serverRunning) 
			{
				display("Servidor ativo e pronto para conexões na porta " + port + ".\n");
				// espera conexão - servidor ativo
				Socket socket = serverSocket.accept();  
				
				// desligar servidor
				if(!serverRunning) break;
				
				// caso o servidor permaneca ativo
				ClientThread t = new ClientThread(socket);  // make a thread of it
				clients.add(t);// save it in the ArrayList
				clienteMap.put(t.username, t);
				t.start();
			}
			
			//desligando servidor
			try {
					//fechar socket, novas conexoes nao serao aceitas
					serverSocket.close();
					
					//fechar canais de entrada e saida para cada cliente
					for(int i = 0; i < clients.size(); ++i) {
						ClientThread clientthread = clients.get(i);
						try {
							clientthread.sInput.close();
							clientthread.sOutput.close();
							clientthread.socket.close();
						} catch(IOException ioE) {}
					}
			} catch(Exception e) {
				display("Houve um erro durante o encerramento das conexões.");
			}
		} catch (IOException e) {
            String msg = horario.format(new Date()) + " Erro durante a inicializacao do ServerSocket: \n";
			display(msg);
		}
	}		

	
	//encerra servidor
	protected void stop() {
		serverRunning = false;
		////////////////////////////////////////////////////////////////////////
		// connect to myself as Client to exit statement 
		// Socket socket = serverSocket.accept();
		try {
			new Socket("localhost", port); 
		}
		catch(Exception e) {}
	}
	
	
	//caixa de mensagens do servidor [modo terminal ou janela grafica]
	private void display(String msg) {
		String time = horario.format(new Date()) + " " + msg;
		if(serverGui == null) System.out.println(time);
		else serverGui.appendEvent(time + "\n");
	}
	
	
	// Mensagem broadcast, enviada a todos os clientes do chat
	private synchronized void broadcast(String message) {
		String time = horario.format(new Date());
		String messageBroadcast = time + " " + message + "\n";
		
		//display mensagem 
		if(serverGui == null) System.out.print(messageBroadcast);
		else serverGui.appendRoom(messageBroadcast);     
		
		// envia mensagem para todos os usuarios 
		for (int i = clients.size(); --i >= 0;) {
			
			ClientThread ct = clients.get(i);
			
			//se o envio da mensagem falhar, remover cliente da lista
			if(!ct.writeMsg(messageBroadcast)) {
				clients.remove(i);
				display("O cliente @" + ct.username + " esta desconectado e foi removido da lista.");
			}
		}
	}

	
	private synchronized void directmessage(String username, String targetuser, String message) {
		// add HH:mm:ss and \n to the message
		String time = horario.format(new Date());
		String messageLf = time + " " +"["+username+ "> " + targetuser+ "] " + message + "\n";
		// display message on console or GUI
		if(serverGui == null)
			System.out.print(messageLf);
		else
			serverGui.appendRoom(messageLf);     // append in the room window
		

		
		try{
			//tenta enviar para o destinatario
			ClientThread ct = (ClientThread) clienteMap.get(targetuser);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(messageLf)) {
				clients.remove(ct);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
			
			//envia para a sua propria caixa de msgs
			ct = (ClientThread) clienteMap.get(username);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg(messageLf)) {
				clients.remove(ct);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
			
			
		}catch(Exception e) {
			//TODO: usuario nao encontrado
			display(" username ... falando com um usuario que nao existe");
			//tenta enviar para o destinatario
			ClientThread ct = (ClientThread) clienteMap.get(username);
			// try to write to the Client if it fails remove it from the list
			if(!ct.writeMsg("O DESTINATARIO @"+targetuser+" NAO ESTA NA SALA.")) {
				clients.remove(ct);
				display("Disconnected Client " + ct.username + " removed from list.");
			}
			
			
		}
	
	}
	
	// for a client who logoff using the LOGOUT message
	synchronized void remove(int id) {
		// scan the array list until we found the Id
		for(int i = 0; i < clients.size(); ++i) {
			ClientThread ct = clients.get(i);
			// found it
			if(ct.id == id) {
				clients.remove(i);
				return;
			}
		}
	}
	
	/*
	 *  To run as a console application just open a console window and: 
	 * > java Server
	 * > java Server portNumber
	 * If the port number is not specified 1500 is used
	 */ 
	
	public static void main(String[] args) {
		// start server on port 1500 unless a PortNumber is specified 
		int portNumber = 1500;
		switch(args.length) {
			case 1:
				try {
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Invalid port number.");
					System.out.println("Usage is: > java Server [portNumber]");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("Usage is: > java Server [portNumber]");
				return;
				
		}
		// create a server object and start it
		Server server = new Server(portNumber);
		server.start();
	}

	/** One instance of this thread will run for each client */
	class ClientThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream sInput;
		ObjectOutputStream sOutput;
		// my unique id (easier for deconnection)
		int id;
		// the Username of the Client
		String username;
		// the only type of message a will receive
		ChatMessage cm;
		// the date I connect
		String date;

		// Constructore
		ClientThread(Socket socket) {
			// a unique id
			id = ++uniqueId;
			this.socket = socket;
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				// create output first
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());
				// read the username
				username = (String) sInput.readObject();
				display(username + " just connected.");
			}
			catch (IOException e) {
				display("Exception creating new Input/output Streams: " + e);
				return;
			}
			// have to catch ClassNotFoundException
			// but I read a String, I am sure it will work
			catch (ClassNotFoundException e) {
			}
            date = new Date().toString() + "\n";
		}

		// what will run forever
		public void run() {
			// to loop until LOGOUT
			boolean keepGoing = true;
			while(keepGoing) {
				// read a String (which is an object)
				try {
					cm = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) {
					display(username + " Exception reading Streams: " + e);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				// the messaage part of the ChatMessage
				String message = cm.getMessage();

				// Switch on the type of message receive
				switch(cm.getType()) {

				case ChatMessage.MESSAGE:
					String separator = "@";
					if (message.startsWith(separator)){
						display("Mensagem privada de "+ separator+username);
						String receiver = message.split(" ")[0].substring(1);
						message = message.substring(receiver.length()+1); 
						directmessage(username,receiver, message);
					} else {
						display("Mensagem publica de "+ separator+username);
						broadcast(username + ": " + message);
					}
					break;

				case ChatMessage.LOGOUT:
					display(username + " disconnected with a LOGOUT message.");
					broadcast(username+" logged out.");
                    keepGoing = false;

				case ChatMessage.WHOISIN:
					writeMsg("List of the users connected at " + horario.format(new Date()) + "\n");
					// scan al the users connected
					for(int i = 0; i < clients.size(); ++i) {
						ClientThread ct = clients.get(i);
						writeMsg((i+1) + ") " + ct.username + " since " + ct.date);
					}
					break;
				}
			}
			// remove myself from the arrayList containing the list of the
			// connected Clients
			remove(id);
			close();
		}
		
		// try to close everything
		private void close() {
			// try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}

		/*
		 * Write a String to the Client output stream
		 */
		private boolean writeMsg(String msg) {
			// if Client is still connected send the message to it
			if(!socket.isConnected()) {
				close();
				return false;
			}
			// write the message to the stream
			try {
				sOutput.writeObject(msg);
			}
			// if an error occurs, do not abort just inform the user
			catch(IOException e) {
				display("Error sending message to " + username);
				display(e.toString());
			}
			return true;
		}
	}
}

