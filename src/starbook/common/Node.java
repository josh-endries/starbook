package starbook.common;

import java.io.IOException;
import java.net.InetAddress;

import starbook.common.BaseNode.Type;

public interface Node extends Stoppable {
	/**
	 * Start the node. Classes extending this class must call this method.
	 * @throws IOException 
	 */
	public void start() throws IOException;
	
	/**
	 * Retrieve this node's NodeInfo object with the time stamp updated to the
	 * current time.
	 * 
	 * @return The NodeInfo object.
	 */
	public NodeInfo getInfo();
	
	public InetAddress getInetAddress();

	public Type getType();
}
