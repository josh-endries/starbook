package starbook.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class TCPCommandListener implements Runnable {
	private static final Logger log = Logger.getLogger(TCPCommandListener.class);
	private final ServerSocket serverSocket;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final CommandHandlerFactory factory;

	public TCPCommandListener(InetSocketAddress address, CommandHandlerFactory factory) throws IOException {
		super();
		serverSocket = new ServerSocket(address.getPort(), 100, address.getAddress());
		serverSocket.setSoTimeout(5000);
		this.factory = factory;
	}

	@Override
	public void run() {
		log.debug("Waiting for packets on "+serverSocket.getInetAddress()+":"+serverSocket.getLocalPort());
		boolean running = true;

		while (running) {
			try {
				/*
				 * Wait for a connection.
				 */
				Socket socket = serverSocket.accept();

				/*
				 * Get a handler for the connection and handle the command.
				 */
				CommandHandler ch = factory.getHandler(socket);
				executor.execute(ch);
			} catch (SocketTimeoutException e) {
				/*
				 * This is normal, and allows is to check if we're supposed to shut down.
				 */
				if (Thread.currentThread().isInterrupted()) {
					running = false;
				}
			} catch (IOException e) {
				/*
				 * Log the error and continue listening. Hopefully.
				 */
				e.printStackTrace();
			}
		}
		
		stopThreads();
	}

	public void stopThreads() {
		executor.shutdown();
		while (!executor.isTerminated()) {
			log.debug("Waiting for thread pool to shut down...");
			try {
				executor.awaitTermination(15, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
