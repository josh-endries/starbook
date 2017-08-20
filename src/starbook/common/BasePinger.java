package starbook.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import starbook.common.Command.Type;
import starbook.nodes.index.IndexNode;

public class BasePinger implements Pinger {
	public static final int DefaultPingDelay = 30000;
	private final int pingDelay;
	private final InetAddress sourceAddress;
	private final InetSocketAddress destinationAddress;
	private Command.Type type;
	
	public BasePinger(InetAddress source, InetSocketAddress destination) {
		this(source, destination, DefaultPingDelay);
	}
	public BasePinger(InetAddress source, InetSocketAddress destination, int delay) {
		if (destination == null) throw new IllegalArgumentException("Destination address must not be null.");
		pingDelay = delay;
		this.destinationAddress = destination;
		this.sourceAddress = source;
		this.type = Command.Type.Ping;
	}
	
	/**
	 * Creates the packet's data payload and sets the "source" parameter to the provided address.
	 * 
	 * @return The data map.
	 */
	@Override
	public Map<String, Object> createData() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("source", sourceAddress);
		
		/*
		 * TODO: Remove this.
		 */
		Node n = (Node) Configuration.getParameter("node");
		data.put("leader", false);
		try {
			if (n instanceof IndexNode) {
				IndexNode in = (IndexNode) n;
				if (in.getLeaderAddress() != null && in.getLeaderAddress().equals(in.getInetAddress())) {
					data.put("leader", true);
				}
			}
		} catch (NoClassDefFoundError e) {
			/*
			 * Non-index nodes don't know about IndexNode...
			 */
		}

		return data;
	}
	
	public InetSocketAddress getDestinationAddress() {
		return destinationAddress;
	}
	
	public int getPingDelay() {
		return pingDelay;
	}

	@Override
	public void run() {
		boolean running = true;
		while (running) {
			try {
				Thread.sleep(pingDelay);
				Map<String, Object> data = createData();
				Command c = new Command(type, data);
				c.sendViaUDP(destinationAddress);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				running = false;
			}
		}
	}

	@Override
	public void setType(Type type) {
		this.type = type;
	}
}
