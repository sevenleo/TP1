import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Server {
	
	private static int uniqueId;				//identifica cada conexao com um novo id (id++)
	private ArrayList<ClientThread> clients; 	//lista de clientes
	private ServerGUI serverGui; 				//parte grafica - Gui
	private SimpleDateFormat horario; 			//registro de horario com o padrao HH:mm:ss
	private int port;							//porta aberta para conexao
	
	//antigo boolean keepGoing
	private boolean serverRunning;				//servidor se mantem ativo se essa boolean=true
	private Map <String,ClientThread> clienteMap;						//linkar username<>thread.id de cada cliente


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
				display("Servidor online.\nConecte-se através da porta " + port + ".");
				// espera conexão - servidor ativo
				Socket socket = serverSocket.accept();  
				
				// desligar servidor
				if(!serverRunning) break;
				
				// caso o servidor permaneca ativo
				ClientThread t = new ClientThread(socket);  
				clients.add(t);
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
            String msg = horario.format(new Date()) + 
            		" Erro durante a inicializacao do ServerSocket:\n"+
            		"Provavelmente porta desejada\n"+ 
            		"nao foi liberada pelo S.O. "+
            		"tente uma porta mais alta";
			display(msg);
		}
	}		

	
	//encerra servidor
	protected void stop() {
		serverRunning = false;
		//TESTE: tentar se conectar para verficar se o socket fechou 
		try {
			new Socket("localhost", port); 
		}
		catch(Exception e) {
		}
	}
	
	
	//caixa de mensagens do servidor [modo terminal ou janela grafica]
	private void display(String msg) {
		String time = horario.format(new Date()) + " " + msg;
		if(serverGui == null) System.out.println(time);
		else serverGui.appendEvent(time + "\n");
	}
	
	
	/**================comunicacao sincronizada=========================**/
	
	
	// Mensagem broadcast, enviada a todos os clientes do chat
	private synchronized void broadcast(String message,String sender) {
		String time = horario.format(new Date());
		String messageBroadcast = time + " " + message + "\n";
		
		//display mensagem 
		if(serverGui == null) System.out.print(messageBroadcast);
		else serverGui.appendRoom(messageBroadcast);     
		
		// envia mensagem para todos os usuarios 
		for (Object key : clienteMap.keySet()) {
			
			ClientThread clientthread = (ClientThread) clienteMap.get((String) key);
			
			if(!((String)key).equals(sender)) {
				//se o envio da mensagem falhar, remover cliente da lista
				if(!clientthread.writeMsg(messageBroadcast)) {
					clients.remove(clienteMap.get((String)key));
					clienteMap.remove((String)key);
					display("O cliente @" + clientthread.username + " esta desconectado e foi removido da lista.");
				}
			}
		}
	}

	
	private synchronized void directmessage(String username, String targetuser, String message) {
		String time = horario.format(new Date());
		String menssagemDireta = time + " " +"["+username+ " > " + targetuser+ "] " + message + "\n";
		
		if(serverGui == null) System.out.print(menssagemDireta);
		else serverGui.appendRoom(menssagemDireta);     
		
		//Thread do remetente
		ClientThread remetenteThread = (ClientThread) clienteMap.get(username);
		
		try{
			//tenta enviar para a thread do destinatario de acordo o username (hashmap)
			ClientThread clientthread = (ClientThread) clienteMap.get(targetuser);
			if(!clientthread.writeMsg(menssagemDireta)) {
				clients.remove(clientthread);
				display("O cliente @" + clientthread.username + " esta desconectado e foi removido da lista.");
			}
			
			//envia para a caixa de msgs do proprio remetente
			//se o envio da mensagem falhar, remover cliente da lista
		/*	if(!remetenteThread.writeMsg(menssagemDireta)) {
				clients.remove(remetenteThread);
				display("O cliente @" + remetenteThread.username + " esta desconectado e foi removido da lista.");
			}*/
		}catch(Exception e) {
			//usuario nao encontrado
			display("@"+username+" esta falando sozinho");
			
			//envia alerta de erro para o destinatario
			//se o envio da mensagem falhar, remover cliente da lista
			if(!remetenteThread.writeMsg("::: O destinatario @"+targetuser+" nao esta na sala.\n")) {
				clients.remove(remetenteThread);
				display("O cliente @" + remetenteThread.username + " esta desconectado e foi removido da lista.");
			}
			
			
		}
	
	}
	
	//LOGOFF
	///////////////////////////////////TODO:
	//PODEMOS SUBSTITUIR ESSA BUSCA PELO NOVO HASHMAP
	synchronized void remove(int id) {
		//buscar o cliente na lista 
		for(int i = 0; i < clients.size(); ++i) {
			ClientThread ct = clients.get(i);
			// cliente encontrado
			if(ct.id == id) {
				clients.remove(i);
				return;
			}
		}
	}
	

	/**==================FUNCAO MAIN =========================**/
	// o servidor pode ser executado de forma indepentende da parte grafica, basta o argumento porta
	// java Server [porta]
	// se nenhuma porta for definida, a porta padrão é a porta 1500
	public static void main(String[] args) { 
		int portNumber = 1500;
		switch(args.length) {
			case 1:
				try {
					portNumber = Integer.parseInt(args[0]);
				}
				catch(Exception e) {
					System.out.println("Defina uma porta [int] para a execucao do servidor");
					return;
				}
			case 0:
				break;
			default:
				System.out.println("argumentos invalidos");
				return;
				
		}


		// criar/iniciar servidor
		Server server = new Server(portNumber);
		server.start();
	}

	
	
	
	
	/**====================================================**/
	/**========= UMA THREAD PARA CADA CLIENTE =============**/
	/**====================================================**/
	
	class ClientThread extends Thread {
		Socket socket;				//socket do cliente
		ObjectInputStream sInput;	//canal de saida do cliente > entrada do servidor
		ObjectOutputStream sOutput;	//canal de entrada do cliente < saida do servidor
		int id;						//identificacao
		String username;			//usuario
		ChatMessage msg;	// mensagem
		String date; 				//horario da conexao

		
		
		// construtor
		ClientThread(Socket socket) {
			id = ++uniqueId;		//cada cliente tem um id diferente
			this.socket = socket;
			
			//se conecta aos canais de entrada e saida criados pelo usuario ao conectar o socket
			
			try	{
				//cria canais
				sOutput = new ObjectOutputStream(socket.getOutputStream());
				sInput  = new ObjectInputStream(socket.getInputStream());

				
				// recebe o nome de usuario, como a primeira mensagem do stream
				username = (String) sInput.readObject();
				
	            //Verificar se o nome de usuario ja esta sendo usado.
	            //trocar pelo metodo do hashmap
				for (int i = clients.size(); --i >= 0;) {
					ClientThread clientthread = clients.get(i);
					if ( clientthread.username.equalsIgnoreCase(username) ) {
						///parar o try, pois se o usuario ja existe deve ser impedido
						throw new Exception();
					}
				}
				display("@"+username + " entrou na sala.");
			
			} catch (IOException e) {
				display("Erro na comunicacao i/o");
				return;
			} catch (ClassNotFoundException e) {
				display("Erro na comunicacao i/o");
				return;
			} catch (Exception e) {
				writeMsg("O nome de usuario desejado já está em uso, tente outro.\n\n");
				close();
				display("Um dos clientes tentou adquirir um nome de usuario que já esta sendo usado.");
			} 
			
            date = new Date().toString() + "\n";
            
            
            
            

			
			
			
            
            
            
		}


		//thread que escuta as mensagens do cliente
		public void run() {
			boolean clientRunning = true; //enquanto true a thread e o cliente permanecem sincronizados
			
			while(clientRunning) {
				
				//espera uma mensagem
				try {
					msg = (ChatMessage) sInput.readObject();
				}
				catch (IOException e) {
					display("Nao foi possivel ler a mensagem de @"+username);
					break;				
				}
				catch(ClassNotFoundException e2) {
					break;
				}
				

				//extrai o texto da mensagem
				String message = msg.getMessage();


				// verifica se a mensagem é do tipo normal, logoff ou sobre quem esta na sala
				switch(msg.getType()) {

				case ChatMessage.MESSAGE:
					String separator = "@";
					/*if (message.startsWith(separator)){
						display("Mensagem privada de "+ separator+username);
						String receiver = message.split(" ")[0].substring(1);
						message = message.substring(receiver.length()+1); 
						directmessage(username,receiver, message);*/
					if(message.startsWith(separator)) {
						java.util.List<String> receivers = new ArrayList<String>();
						
						while(message.startsWith(separator)) {
								receivers.add(message.split(" ")[0].substring(1));
								message = message.substring(message.split(" ")[0].length()).trim();
						}
						
						for(String receiver : receivers) {
							directmessage(username,receiver, message);
						}
					} else {
						display("Mensagem publica de "+ separator+username);
						broadcast("["+username + "]: " + message,username);
					}
					break;

				case ChatMessage.LOGOUT:
					display("@"+username + " fez LOGOFF");
					broadcast("@"+username+" saiu da sala.",username);
                    clientRunning = false;

				case ChatMessage.WHOISIN:
					writeMsg("--------\n" + horario.format(new Date()) + "\nUsuarios conectados:\n");

					//verifica todos os usuarios da lista
					for(int i = 0; i < clients.size(); ++i) {
						ClientThread ct = clients.get(i);
						writeMsg((i+1) + " - " + ct.username + " entrou na sala: " + ct.date);
					}
					writeMsg("--------\n");
					break;
				}
			}

			
			//temos clientRunning=false;
			//A thread deve finalizar e remover a comunicacao com o cliente
			remove(id);
			close();
		}
		

			//fecha canais e conexoes		
		private void close() {
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

		
		
		//envia mensagem para o cliente
		private boolean writeMsg(String msg) {
			//verfica se a conexao ataves do socket ainda esta ativa
			if(!socket.isConnected()) {
				close();
				return false;
			}

			try {
				sOutput.writeObject(msg);
			} catch(IOException e) {
				display("Nao foi possivel enviar a mensagem para @" + username);
			}
			return true;
		}
	}
}

