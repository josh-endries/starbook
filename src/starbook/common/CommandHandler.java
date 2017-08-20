package starbook.common;

import java.net.DatagramPacket;
import java.net.Socket;

public interface CommandHandler extends Runnable {
	public void setSocket(Socket socket);
	public void setPacket(DatagramPacket packet);
	public void processCommand(Command command);
}
