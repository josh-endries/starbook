package starbook.common;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.princehouse.mica.base.model.Runtime;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.base.simple.SimpleRuntime;

import starbook.common.protocols.NodeDiscoveryProtocol;

public abstract class BaseDiscoverableNode extends BaseNode implements DiscoverableNode {
	private final static Logger log = Logger.getLogger(BaseDiscoverableNode.class);
	private final ConcurrentHashMap<TCPAddress, NodeInfo> neighbors = new ConcurrentHashMap<TCPAddress, NodeInfo>();
	private Runtime<NodeDiscoveryProtocol> discoveryProtocolRuntime = null;
	private final NodeDiscoveryProtocol discoveryProtocol = new NodeDiscoveryProtocol();
	protected final TCPAddress discoveryProtocolAddress = new TCPAddress(getInetAddress(), Configuration.getInt(CK.NodeDiscoveryPort));
	
	/**
	 * Discoverable nodes contact the index, so create an address entry for it.
	 */
	private final InetAddress indexAddress;
	private final HashSet<InetAddress> indexAddresses = new HashSet<InetAddress>();

	/**
	 * This indicates whether the NodeUpdater has updated the node list yet.
	 */
	private boolean nodesUpdated = false;

	public BaseDiscoverableNode(InetAddress address, Type type) throws UnknownHostException {
		super(address, type);
		
		/*
		 * Set the monitor address.
		 */
		setMonitorAddress(InetAddress.getByName(Configuration.getStr(CK.MonitorIP)));
		
		/*
		 * Set up the bootstrap index addresses.
		 */
		String op = Configuration.getStr(CK.IndexOperator);
		String net = Configuration.getStr(CK.IndexBaseNet);
		int host = Configuration.getInt(CK.IndexBaseHost);
		for (int i=0; i<3; i++) {
			int h = host;
			if (op.equals("+")) {
				h+=i;
			} else if (op.equals("-")) {
				h-=i;
			} else {
				throw new RuntimeException("Unknown index bootstrap operator.");
			}
			
			String ias = net+'.'+h;
			InetAddress ia = InetAddress.getByName(ias);
			if (!ia.equals(address)) {
				indexAddresses.add(ia);
			}
		}
		log.debug("Index address(es): "+indexAddresses);
		
		/*
		 * Set the index address.
		 * 
		 * TODO: Remove this.
		 */
		indexAddress = indexAddresses.iterator().next();

		/*
		 * Set up the discovery protocol.
		 */
		HashSet<TCPAddress> ignoredAddresses = new HashSet<TCPAddress>(1);
		ignoredAddresses.add(discoveryProtocolAddress);
		discoveryProtocol.setIgnoredAddresses(ignoredAddresses);
		HashSet<TCPAddress> persistentAddresses = new HashSet<TCPAddress>(indexAddresses.size());
		for (InetAddress ia: indexAddresses) persistentAddresses.add(new TCPAddress(ia, Configuration.getInt(CK.NodeDiscoveryPort)));
		discoveryProtocol.setPersistentAddresses(persistentAddresses);
		discoveryProtocol.setOrigin(address);
	}
	
	@Override
	public NodeDiscoveryProtocol getDiscoveryProtocol() throws RuntimeException {
		if (discoveryProtocolRuntime != null) throw new RuntimeException("Attempt to retrieve protocol but it has already started running.");
		return discoveryProtocol;
	}

	@Override
	public Runtime<NodeDiscoveryProtocol> getDiscoveryProtocolRuntme() {
		return discoveryProtocolRuntime;
	}

	@Override
	public InetAddress getIndexAddress() {
		return indexAddress;
	}
	public HashSet<InetAddress> getIndexAddresses() {
		return indexAddresses;
	}

	@Override
	public ConcurrentHashMap<TCPAddress, NodeInfo> getNeighbors() {
		return neighbors;
	}

	@Override
	public TCPAddress getRandomNeighbor(boolean self, Type type) throws IOException {
		List<TCPAddress> addresses = new ArrayList<TCPAddress>();
		
		/*
		 * Copy all nodes of the type we're interested in into the set.
		 */
		Iterator<Map.Entry<TCPAddress, NodeInfo>> i = neighbors.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<TCPAddress, NodeInfo> e = i.next();
			if (e.getValue().getType().equals(type)) addresses.add(new TCPAddress(e.getKey()));
		}
		
		/*
		 * Get a random node from the set.
		 */
		if (addresses.size() > 0) {
			int attempts = 0;
			while (attempts < 10) {
				int index = Configuration.rng.nextInt(addresses.size());
				TCPAddress result = addresses.get(index);
				if (!self && result.getInetAddressAddress().equals(getInetAddress())) {
					/*
					 * We aren't allowed to return ourselves, and we picked ourself as the node. Don't return anything, just
					 * continue on through the loop.
					 */
				} else {
					/*
					 * We didn't pick ourself or we are allowed to return ourselves, so return the node we picked.
					 */
					return result;
				}
				attempts++;
			}
		}

		throw new IOException("Unable to retrieve a random node.");
	}

	@Override
	public TCPAddress getRandomNeighbor(boolean self) throws IOException {
		TCPAddress[] addresses = Util.copyCCCHM(neighbors).keySet().toArray(new TCPAddress[0]);

		if (addresses.length > 0) {
			int attempts = 0;
			while (attempts < 10) {
				int index = Configuration.rng.nextInt(addresses.length);
				TCPAddress result = addresses[index];
				if (!self && result.getInetAddressAddress().equals(getInetAddress())) {
					/*
					 * We aren't allowed to return ourselves, and we picked ourself as the node. Don't return anything, just
					 * continue on through the loop.
					 */
				} else {
					/*
					 * We didn't pick ourself or we are allowed to return ourselves, so return the node we picked.
					 */
					return result;
				}
				attempts++;
			}
		}

		throw new IOException("Unable to retrieve a random node.");
	}
	
	@Override
	public boolean isNodesUpdated() {
		return nodesUpdated;
	}

	@Override
	public int pruneNeighbors() {
		DateTime cutoff = DateTime.now().minusSeconds(Configuration.getInt(CK.NDPCutoffSeconds));
		
		/*
		 * Remove any stale nodes.
		 */
		int pruned = 0;
		Iterator<Map.Entry<TCPAddress, NodeInfo>> i = neighbors.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<TCPAddress, NodeInfo> e = i.next();
			if (e.getValue().getLatestActivity().isBefore(cutoff)) {
				log.debug("Removing stale local node: "+e.getKey());
				i.remove();
				pruned++;
			}
		}
		
		return pruned;
	}
	
	@Override
	public void modifyNeighbors(Map<Operation, Map<TCPAddress, NodeInfo>> map) {
		log.debug(String.format("Updating neighbors: %s", map));
		
		for (Operation o: Operation.values()) {
			if (map.containsKey(o)) {
				switch (o) {
					case Remove:
						for (Map.Entry<TCPAddress, NodeInfo> e: map.get(o).entrySet()) neighbors.remove(e.getKey());
						break;
					case Add:
					case Modify:
						for (Map.Entry<TCPAddress, NodeInfo> e: map.get(o).entrySet()) neighbors.put(e.getKey(), e.getValue());
						break;
					default:
						log.error(String.format("Unrecognized operation %s", o));
				}
			}
		}
		
//		log.debug(String.format("Neighbors: %s", neighbors));
	}
	
	@Override
	public void start() throws IOException {
		super.start();
		discoveryProtocolRuntime = SimpleRuntime.launchDaemon(discoveryProtocol, discoveryProtocolAddress);
		
		/*
		 * Start the node pruner.
		 */
		NodePruner np = new NodePruner(this);
		Thread npt = new Thread(np, "Node Pruner");
		addThread(npt);
//		npt.start();
	}
	
	@Override
	public void stopThreads() {
		super.stopThreads();
		
		if (discoveryProtocolRuntime != null) {
			log.debug("Stopping the runtime...");
			discoveryProtocolRuntime.stop();
		}
	}

	@Override
	public String toString() {
		return String.format("%s[address=%s, nodes=%s]", getClass().getSimpleName(), getInetAddress(), neighbors);
	}
}
