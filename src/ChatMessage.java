import java.io.*;

/*
 * Meio de comunicacao entre os usuarios e o servidor
 * 	// WHOISIN - QUAIS USUARIOS ESTAO LOGADOS NO SERVIDOR
	// MESSAGE - MENSAGEM COMUM
	// LOGOUT  - PEDIDO DE LOGOUT
 */
public class ChatMessage implements Serializable { 

	protected static final long serialVersionUID = 1112122200L; //PORQUE SERIAL?

	static final int WHOISIN = 0, MESSAGE = 1, LOGOUT = 2;
	private int type;
	private String message;
	
	ChatMessage(int type, String message) {
		this.type = type;
		this.message = message;
	}
	
	int getType() {
		return type;
	}
	
	String getMessage() {
		return message;
	}
}

