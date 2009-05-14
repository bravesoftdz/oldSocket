package br.com.gennex.socket.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.util.Observable;
import java.util.Observer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import br.com.gennex.interfaces.SocketFactory;
import br.com.gennex.socket.Socket;

/**
 * 
 * Classe que implementa um client TCP que se mantem conectado.
 * 
 * @author Daniel Jurado
 * 
 */
public class ClientSocket extends TimerTask implements Observer {

	private Socket socket = null;
	private String host;
	private int port;
	private SocketFactory socketFactory;
	private int reconnectInterval = 10000;

	public ClientSocket(String host, int port, SocketFactory socketFactory) {
		super();
		this.host = host;
		this.port = port;
		this.socketFactory = socketFactory;
	}

	private void checkConnection() {
		if (socket != null)
			return;

		if (getHost() == null || getHost().length() == 0 || getPort() <= 0) {
			return;
		}

		InetAddress addr = null;
		try {
			addr = InetAddress.getByName(getHost());
		} catch (UnknownHostException e) {
			Logger.getLogger(getClass()).error(e.getMessage(), e);
			return;
		}

		SocketAddress sockaddr = new InetSocketAddress(addr, getPort());

		java.net.Socket rawSocket = new java.net.Socket();

		try {
			rawSocket.connect(sockaddr);
		} catch (IOException e) {
			Logger.getLogger(getClass()).error(e.getMessage(), e);
			return;
		}

		setSocket(socketFactory.createSocket(rawSocket));
		socket.addObserver(this);
		new Thread(socket, "Server " + rawSocket.getInetAddress().getHostName())
				.start();
	}

	/**
	 * @return o host atual onde o socket se conecta.
	 */
	public final String getHost() {
		return host;
	}

	/**
	 * @return a porta onde o socket atualmente se conecta.
	 */
	public final int getPort() {
		return port;
	}

	/**
	 * @return o intervalo de reconexao automatica.
	 */
	public final int getReconnectInterval() {
		return reconnectInterval;
	}

	public class SocketNaoConectado extends Exception {
		public SocketNaoConectado(String string) {
			super(string);
		}

		private static final long serialVersionUID = 1L;

	}

	public Socket getSocket() throws SocketNaoConectado {
		if (socket == null)
			throw new SocketNaoConectado("not connected");
		return socket;
	}

	/**
	 * @return retorna a factory respons�vel pela gera��o dos sockets de conex�o
	 *         que sao mantidos por este objeto.
	 */
	public final SocketFactory getSocketFactory() {
		return socketFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run() {
		checkConnection();
	}

	/**
	 * @param host
	 *            modifica o host onde o socket se conecta. Se o valor for
	 *            modificado enquanto uma conex�o est� ativa, ela � terminada e
	 *            uma nova se inicia.
	 */
	public final void setHost(String host) {
		if (host == null || host.length() <= 0)
			throw new InvalidParameterException("invalid host.");
		if (getHost().equalsIgnoreCase(host))
			return;
		if (socket != null && socket.isConnected())
			try {
				socket.disconnect();
			} catch (IOException e) {
				Logger.getLogger(getClass()).error(e.getMessage(), e);
			}
		this.host = host;
	}

	/**
	 * @param port
	 *            modifica a porta onde o socket se conecta. Se o valor for
	 *            modificado enquanto uma conex�o est� ativa, ela � terminada e
	 *            uma nova se inicia.
	 */
	public final void setPort(int port) {
		if (port <= 0)
			throw new InvalidParameterException("invalid port");
		if (getPort() == port)
			return;
		if (socket != null && socket.isConnected())
			try {
				socket.disconnect();
			} catch (IOException e) {
				Logger.getLogger(getClass()).error(e.getMessage(), e);
			}
		this.port = port;
	}

	/**
	 * @param reconnectInterval
	 * 
	 *            Altera o intervalo em milisegindos entre as tentativas de
	 *            conexao. O valor minimo eh de 1000 ms. Caso seja informado um
	 *            valor maior, lanca uma
	 *            {@link java.security.InvalidParameterException}.
	 */
	public final void setReconnectInterval(int reconnectInterval) {
		if (reconnectInterval < 1000)
			throw new InvalidParameterException(
					"interval must be at least 1000 ms");
		this.reconnectInterval = reconnectInterval;
	}

	private void setSocket(Socket socket) {
		this.socket = socket;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public final void update(Observable o, Object arg) {
		if (!(arg instanceof Socket.EventDisconnected))
			return;
		setSocket(null);
	}

	public void disconnect() throws IOException {
		this.host = null;
		this.port = 0;
		this.socket.disconnect();
	}

}
