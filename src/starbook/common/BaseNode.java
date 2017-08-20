package starbook.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import starbook.nodes.MonitoredNode;

public abstract class BaseNode extends BaseThreadedObject implements DiscoverableNode, MonitoredNode {
	public static enum Type { Index, Web, Worker; }
	private static final Logger log = Logger.getLogger(BaseNode.class);
	private final InetAddress address;
	private final Type type;
	private InetAddress monitorAddress;

	/**
	 * Creates a BaseNode object.
	 * 
	 * @param address
	 *           The IP address of this node.
	 */
	public BaseNode(InetAddress address, Type type) {
		this.address = address;
		this.type = type;
	}

	public InetAddress getInetAddress() {
		return address;
	}

	@Override
	public NodeInfo getInfo() {
		NodeInfo ni = new NodeInfo(type);
		return ni;
	}
	
	@Override
	public InetAddress getMonitorAddress() {
		return monitorAddress;
	}
	
	@Override
	public Type getType() {
		return type;
	}
	
	@Override
	public void setMonitorAddress(InetAddress address) {
		monitorAddress = address;
	}
	
	@Override
	public void start() throws IOException {
		try {
			InetSocketAddress monitorAddress = new InetSocketAddress(InetAddress.getByName(Configuration.getStr(CK.MonitorIP)), Configuration.getInt(CK.CommandPort));
			Pinger monitorPinger = new MonitorPinger(this, monitorAddress, 5000);
			Thread monitorPingerThread = new Thread(monitorPinger, "Monitor Pinger");
			addThread(monitorPingerThread);
			log.debug("Starting monitor pinger.");
			monitorPingerThread.start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return String.format("%s[address=%s]", getClass().getSimpleName(), address);
	}
}
