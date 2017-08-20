package starbook.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.Socket;

/**
 * Implements most of a command handler, minus the actual command processing. A network listener
 * (either TCP or UDP) will call the setter methods of this class to assign a packet or socket.
 * This class will extract the Command object from the data and process it (the processing is
 * defined in the subclass).
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public abstract class BaseCommandHandler implements CommandHandler {
	protected Socket socket = null;
	protected DatagramPacket packet = null;
	
	@Override
	public final void setSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public final void setPacket(DatagramPacket packet) {
		this.packet = packet;
	}
	
	@Override
	public final void run() {
		try {
			Command command = getCommand();
			processCommand(command);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (socket != null) {
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private Command getCommand() throws IOException {
		/*
		 * Extract the command.
		 */
		ObjectInputStream ois;
		if (packet != null) {
			ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
			ois = new ObjectInputStream(bais);
		} else if (socket != null) {
			ois = new ObjectInputStream(socket.getInputStream());
		} else {
			throw new IOException("Both packet and socket are null.");
		}
		
		/*
		 * Extract the command.
		 */
		try {
			Object o = ois.readObject();
			if (o instanceof Command) {
				return (Command) o;
			} else {
				throw new IOException("Received data is not a command.");
			}
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
}
