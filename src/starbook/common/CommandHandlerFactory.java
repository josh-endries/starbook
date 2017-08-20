package starbook.common;

import java.net.DatagramPacket;
import java.net.Socket;

public interface CommandHandlerFactory {
	public CommandHandler getHandler(Socket socket);
	public CommandHandler getHandler(DatagramPacket packet);
}