package starbook.common;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

import starbook.common.BaseNode.Type;
import starbook.common.protocols.NodeDiscoveryProtocol;

public interface DiscoverableNode extends Node {
	/**
	 * Retrieve a reference to this node's discovery protocol instance. This method will throw an
	 * exception if the protocol has already started running, because this object changes at runtime
	 * as gossip progresses. At any point after starting, the protocol instance may not be local to
	 * this machine, might be an old reference, etc..
	 * 
	 * @return The protocol instance.
	 * @throws RuntimeException
	 *            If the protocol has already been started.
	 */
	public NodeDiscoveryProtocol getDiscoveryProtocol() throws RuntimeException;

	public Runtime<NodeDiscoveryProtocol> getDiscoveryProtocolRuntme();

	public InetAddress getIndexAddress();
	public Set<InetAddress> getIndexAddresses();
	
	/**
	 * Returns a reference to this node's neighbor list.
	 * 
	 * @return The node Map.
	 */
	public ConcurrentHashMap<TCPAddress, NodeInfo> getNeighbors();

	/**
	 * Retrieves a copy of a random neighbor Address.
	 * 
	 * @return The copy of the neighbor Address object in the neighbor list.
	 * @param self
	 *           True if this node's address may be included as the result.
	 * @throws IOException
	 *            If a random node cannot be retrieved.
	 */
	public TCPAddress getRandomNeighbor(boolean self) throws IOException;

	public TCPAddress getRandomNeighbor(boolean self, Type type) throws IOException;

	boolean isNodesUpdated();

	/**
	 * This method is to be called once, the first time setNeighbors is called.
	 */
	public void converge(Class<?> t);

	/**
	 * Remove neighbors whose latest activity is before the current time minus the configured cutoff
	 * interval.
	 * 
	 * @return The number of nodes pruned.
	 */
	public int pruneNeighbors();
	
	/**
	 * Modify this node's neighbor list according to the specified map.
	 * 
	 * @param map
	 *           The map which specifies the operations to apply to the neighbor list.
	 */
	public void modifyNeighbors(Map<Operation, Map<TCPAddress, NodeInfo>> map);
}
