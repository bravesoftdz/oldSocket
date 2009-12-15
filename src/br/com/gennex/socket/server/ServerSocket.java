package br.com.gennex.socket.server;

import java.security.InvalidParameterException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import br.com.gennex.interfaces.SocketFactory;
import br.com.gennex.socket.Socket;
import br.com.gennex.socket.model.ServerPort;

/**
 * Classe que implementa um servidor TCP que ouve as conex�es e cria threads
 * espec�ficas para cada comportamento.
 * 
 * @author Daniel Jurado
 * 
 */
public class ServerSocket implements Runnable {

	private ServerPort serverPort;

	private final SocketAccepter socketAccepter;

	private class SocketAccepter implements Runnable {

		private SocketAccepter(SocketFactory socketFactory) {
			this.socketFactory = socketFactory;
		}

		private final SocketFactory socketFactory;

		private BlockingQueue<java.net.Socket> sockets = new LinkedBlockingQueue<java.net.Socket>();

		@Override
		public void run() {
			do {
				java.net.Socket socket = null;
				try {
					socket = sockets.take();
				} catch (InterruptedException e) {
					Logger.getLogger(getClass()).error(e.getMessage(), e);
				}

				if (socket == null)
					continue;

				Socket threadSocket = socketFactory.createSocket(socket);
				new Thread(threadSocket, "Client "
						+ socket.getInetAddress().getHostName()).start();

			} while (Thread.currentThread().isAlive());
		}

		public void addSocket(java.net.Socket socket) {
			sockets.offer(socket);
		}

	}

	/**
	 * @param port
	 *            a porta que o servidor dever� aceitar novas conex�es.
	 * @param socketFactory
	 *            a factory que deve criar os sockets de acordo com os
	 *            comportamentos esperados.
	 */
	public ServerSocket(ServerPort serverPort, SocketFactory socketFactory) {
		super();
		if (serverPort == null)
			throw new InvalidParameterException("invalid port");
		if (socketFactory == null)
			throw new InvalidParameterException("invalid socketFactory");
		this.serverPort = serverPort;

		this.socketAccepter = new SocketAccepter(socketFactory);

		Thread t = new Thread(socketAccepter);
		t.setDaemon(true);
		t.start();
	}

	/**
	 * @return a porta onde o servidor atualmente escuta.
	 */
	public final ServerPort getServerPort() {
		return serverPort;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public final void run() {
		do {
			try {
				java.net.ServerSocket server = new java.net.ServerSocket(
						getServerPort().getServerPort());
				Logger.getLogger(getClass()).info(
						"Ready to accept connections...");
				do {
					java.net.Socket socket = server.accept();
					this.socketAccepter.addSocket(socket);
				} while (Thread.currentThread().isAlive());
			} catch (Exception e) {
				Logger.getLogger(getClass()).fatal(e.getMessage(), e);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException f) {
					Logger.getLogger(getClass()).error(f.getMessage(), f);
				}
			}
		} while (Thread.currentThread().isAlive());

	}

}
