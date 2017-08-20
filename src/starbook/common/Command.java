package starbook.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Commands are used to tell other nodes what to do and who is telling them. Each command contains a
 * Map<String, Object> as data, with keys and values specified by that particular command's type.
 * The only globally-required map key is "source", an InetAddress identifying the sender of that
 * particular command.
 * 
 * @author Josh Endries (josh@endries.org)
 * 
 */
public class Command implements Serializable {
	private final static Logger log = Logger.getLogger(Command.class);
	protected final Map<String, Object> data;
	protected final Type type;
	private static final long serialVersionUID = 2952924549166796494L;

	/**
	 * Create a new command of the specified type and with the specified data map.
	 * 
	 * @param t The type for this command.
	 * @param o The data map for this command.
	 */
	public Command(Type t, Map<String, Object> o) {
		type = t;
		data = o;
	}

	/**
	 * Retrieve the data Map for this command.
	 * 
	 * @return The data Map.
	 */
	public Map<String, Object> getData() {
		return data;
	}

	/**
	 * Retrieve the type of this command.
	 * 
	 * @return The type.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Command types are used to determine what should be done when receiving a command. They each
	 * have specified map keys and values which <b>must</b> be present in the packet's data map. All
	 * command types are required to specify the sending machine via the "source" key and an
	 * InetAddress value.
	 * 
	 * @author Josh Endries (josh@endries.org)
	 * 
	 */
	public static enum Type {
		/**
		 * <p>
		 * Request that the specified User record be sent to the specified source node.
		 * </p>
		 * <p>
		 * Data map:
		 * </p>
		 * <ul>
		 * <li>"name" (String): The requested user's name.</li>
		 * <li>"source" (InetAddress): The sender.</li>
		 * </ul>
		 */
		UserRequest,

		/**
		 * <p>
		 * Request that the receiving node send the specified message to the source address.
		 * </p>
		 * <p>
		 * Data map:
		 * </p>
		 * <ul>
		 * <li>"guid" (String): The GUID of the message to download.</li>
		 * <li>"source" (InetAddress): The sender.</li>
		 * </ul>
		 */
		MessageRequest,

		/**
		 * <p>
		 * A Ping command is used to tell a monitor that the object node is still running.
		 * </p>
		 * <p>
		 * Data Map:
		 * </p>
		 * <ul>
		 * <li>"source" (InetAddress): The sender.</li>
		 * </ul>
		 */
		Ping,

		/**
		 * <p>
		 * The ToggleEdge command instructs the monitor to display an edge between two nodes in the
		 * graph, and then remove the edge after a predetermined period of time. The data object for
		 * this command must be an {@link Edge} object.
		 * </p>
		 * <p>
		 * Data Map:
		 * </p>
		 * <ul>
		 * <li>"edge" (Edge): The edge.</li>
		 * <li>"source" (InetAddress): The sender.</li>
		 * </ul>
		 */
		ToggleEdge,

		/**
		 * <p>
		 * Store a message in the receiving node's message store.
		 * </p>
		 * 
		 * <p>
		 * Data map:
		 * </p>
		 * <ul>
		 * <li>"message" (Message): The message to be stored.</li>
		 * <li>"source" (InetAddress): The sender.</li>
		 * </ul>
		 */
		MessageUpload,

		/**
		 * <p>
		 * The NewTopic command is sent from the index node to a worker node so that worker node knows
		 * it is supposed to start tracking the given topic.
		 * </p>
		 * <p>
		 * Data map:
		 * </p>
		 * <ul>
		 * <li>"source" (InetAddress) The sender.</li>
		 * <li>"user" (User): The new user.</li>
		 * </ul>
		 */
		AddTopic,

		/**
		 * <p>
		 * AddUser is used by the index to tell a node that it should start be responsible for a new
		 * user.
		 * </p>
		 * <p>
		 * Data map:
		 * </p>
		 * <ul>
		 * <li>"source" (InetAddress) The sender.</li>
		 * <li>"user" (User): The new user.</li>
		 * </ul>
		 */
		AddUser,

		/**
		 * <p>
		 * AddedUser is sent from a worker node to the index after adding a user to it's tracking
		 * list, either from discovery or being told to by the index.
		 * </p>
		 * <p>
		 * Data map:
		 * </p>
		 * <ul>
		 * <li>"source" (InetAddress): The sender.</li>
		 * <li>"user" (User): The user that was added to the user list.</li>
		 * </ul>
		 */
		AddedUser,
		
		/**
		 * <p>
		 * CreateUser is exactly like AddUser except it cannot be refused. Index nodes use this to
		 * ensure that users are replicated the required number of times among web nodes, and also
		 * to tell other index nodes about newly-created users (which is why the node parameter
		 * exists).
		 * </p>
		 * <p>
		 * Data map:
		 * </p>
		 * <ul>
		 * <li>"source" (InetAddress) The sender.</li>
		 * <li>"node" (InetAddress) The initial node for this user.
		 * <li>"user" (User): The new user.</li>
		 * </ul>
		 */
		CreateUser,

		/**
		 * <p>
		 * Sent in response to a UserRequest and contains the User object that was requested.
		 * </p>
		 * <p>
		 * Data map:
		 * </p>
		 * <ul>
		 * <li>"source" (InetAddress): The sender.</li>
		 * <li>"user" (User): The user that was requested.</li>
		 * </ul>
		 */
		UserUpload
	}
	
	public void sendViaTCP(InetSocketAddress isa) throws IOException {
		Socket socket = new Socket(isa.getAddress(), isa.getPort());
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(this);
		oos.flush();
		oos.close();
		log.debug(String.format("Sending %s to %s", this, isa));
	}

	/**
	 * The sendTo method will send the Command to a remote host over UDP.
	 * 
	 * @param isa The address and port to which the Command will be sent.
	 * @throws IOException If there are problems writing to or opening the socket.
	 */
	public void sendViaUDP(SocketAddress isa) throws IOException {
		DatagramSocket s = new DatagramSocket();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		oos = new ObjectOutputStream(baos);
		oos.writeObject(this);
		oos.flush();
		byte[] buffer = baos.toByteArray();
		DatagramPacket p = new DatagramPacket(buffer, buffer.length, isa);
		oos.close();
		baos.close();
		log.debug(String.format("Sending %s to %s (%d bytes)", this, isa, buffer.length));
		s.send(p);
	}

	@Override
	public String toString() {
		return String.format("%s[type=%s]", getClass().getSimpleName(), type);
	}
}
