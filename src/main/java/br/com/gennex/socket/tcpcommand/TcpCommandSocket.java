package br.com.gennex.socket.tcpcommand;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import br.com.gennex.interfaces.TcpRequest;
import br.com.gennex.interfaces.TcpRequestCommand;
import br.com.gennex.interfaces.TcpRequestHandler;
import br.com.gennex.interfaces.TcpResponse;
import br.com.gennex.socket.tcpcommand.messages.requests.FppsRequestCommand;
import br.com.gennex.socket.tcpcommand.messages.requests.InvalidTcpRequest;
import br.com.gennex.socket.tcpcommand.messages.requests.InvalidTcpRequestHandler;
import br.com.gennex.socket.tcpcommand.messages.responses.FppsErrorResponse;

public class TcpCommandSocket extends br.com.gennex.socket.Socket {

	private static class RequestToRun {
		private final TcpRequestHandler handler;
		private final TcpRequest request;

		RequestToRun(TcpRequestHandler handler, TcpRequest request) {
			this.handler = handler;
			this.request = request;
		}
	}

	private class RequestRunner implements Runnable {
		private final TcpCommandSocket owner;
		private BlockingQueue<RequestToRun> queue = new LinkedBlockingQueue<RequestToRun>();

		RequestRunner(TcpCommandSocket owner) {
			this.owner = owner;
		}

		@Override
		public void run() {
			try {
				process();
			} catch (Exception e) {
				Logger.getLogger(getClass()).error(e.getMessage(), e);
			}
		}
		
		private boolean alive = true;
		
		public void terminate(){
			this.alive = false;
		}

		private void process() throws Exception {
			do {
				RequestToRun requestToRun = queue.poll(1, TimeUnit.SECONDS);
				if (requestToRun == null)
					continue;

				TcpResponse response = null;

				try {
					Calendar start = Calendar.getInstance();
					response = requestToRun.handler.process(owner,
							requestToRun.request);
					Calendar end = Calendar.getInstance();
					if (end.getTimeInMillis() - start.getTimeInMillis() > 1000)
						Logger.getLogger(getClass()).info(
								String.format(
										"%s execution runned for %dms",
										requestToRun.request.getTcpMessage(),
										Calendar.getInstance()
												.getTimeInMillis()
												- start.getTimeInMillis()));
				} catch (UnsupportedOperationException e) {
					response = handleInvalidRequest(requestToRun.request, e);
				} catch (Exception e) {
					response = handleRequestException(requestToRun.request, e);
				}

				if (response == null)
					continue;
				owner.send(response);
			} while (isAlive());
		}

		private boolean isAlive() {
			return alive;
		}
	}

	private final RequestRunner requestRunner;

	private Map<String, TcpRequestHandler> handlerList = new HashMap<String, TcpRequestHandler>();

	public TcpCommandSocket(java.net.Socket socket) {
		super(socket);
		addHandler(new InvalidTcpRequest(), new InvalidTcpRequestHandler());

		this.requestRunner = new RequestRunner(this);
		inicializaRequestRunner();
	}

	private void inicializaRequestRunner() {
		Thread requestRunner = new Thread(this.requestRunner, getClass()
				.getSimpleName().concat("-")
				.concat(this.requestRunner.getClass().getSimpleName()));
		requestRunner.setDaemon(true);
		requestRunner.start();
	}

	public void addHandler(TcpRequest request, TcpRequestHandler requestHandler) {
		handlerList.put(request.toString(), requestHandler);
	}

	public void clearHandlers() {
		handlerList.clear();
	}

	private boolean respondeInvalidRequest = true;

	private TcpResponse handleInvalidRequest(TcpRequest request,
			UnsupportedOperationException e) {
		if (Logger.getLogger(getClass()).isDebugEnabled())
			Logger.getLogger(getClass()).debug(
					new StringBuffer("Invalid request: ").append(request
							.getTcpMessage()));
		return respondeInvalidRequest ? new FppsErrorResponse(request, e)
				: null;
	}

	private TcpResponse handleRequestException(TcpRequest request, Exception e) {
		Logger.getLogger(getClass()).error(e.getMessage(), e);
		TcpResponse response;
		response = new FppsErrorResponse(request, e);
		return response;
	}

	public TcpResponse processRequest(TcpRequestCommand request) {
		String strRequest = request.getCommand().toUpperCase();
		if (!handlerList.containsKey(strRequest))
			throw new UnsupportedOperationException();

		TcpRequestHandler handler = handlerList.get(strRequest);
		if (!this.requestRunner.queue.offer(new RequestToRun(handler, request)))
			throw new RuntimeException();
		return null;
	}

	@Override
	protected void processStringRequest(String s) throws Exception {
		TcpResponse response = null;
		TcpRequestCommand request = new FppsRequestCommand(s);
		try {
			response = processRequest(request);
		} catch (UnsupportedOperationException e) {
			response = handleInvalidRequest(request, e);
		} catch (Exception e) {
			response = handleRequestException(request, e);
			Logger.getLogger(getClass()).error(e.getMessage(), e);
		} finally {
			if (response == null)
				return;
			send(response);
		}
	}

	public TcpRequestHandler removeHandler(TcpRequest request) {
		return handlerList.remove(request.toString());
	}

	protected void setRespondeInvalidRequest(boolean respondeInvalidRequest) {
		this.respondeInvalidRequest = respondeInvalidRequest;
	}

	@Override
	public void disconnect() throws IOException {
		super.disconnect();
		requestRunner.terminate();
	}

}
