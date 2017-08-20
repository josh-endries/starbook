package starbook.common.protocols;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

import starbook.common.Configuration;
import starbook.common.DiscoverableNode;
import starbook.common.Node;
import starbook.common.NodeInfo;
import starbook.common.Operation;

/**
 * NodeDiscoveryProtocol (NDP) attempts to maintain an up-to-date, complete view of the nodes that
 * exist in the network. NDP does this by recording the time at which a node was last heard from and
 * exchanging that time with every other node. If newer times are discovered at other protocol
 * instances, their time is updated at the local instance. The parent node of each instance is told
 * to prune its node list every round in order to remove stale nodes.
 * 
 * If the list of neighbors does not significantly change in a given round (meaning no nodes are
 * added or removed), then the gossip rate decreases. If the neighbor list does change, or if nodes
 * are pruned from the list before starting gossip, the rate is reset to 1.0.
 * 
 * Testing has shown that, even though MiCA is "sleeping", it still responds to incoming updates, so
 * this only really affects the initiation of an update from the local node. In other words, if the
 * rate is very low (therefore the delay between rounds is very high), an incoming protocol instance
 * will still be handled but the local node which has the low rate will not initiate a round until
 * the delay has lapsed. Therefore, having a low rate doesn't necessarily cause the whole protocol
 * to "freeze", rather just slows it down.
 * 
 * @author Josh Endries (josh@endries.org)
 * @history 1.0 Initial version.
 * @since 0.1
 * @version 2.0
 */
public class NodeDiscoveryProtocol extends BaseDiscoveryProtocol {
	private static final Logger log = Logger.getLogger(NodeDiscoveryProtocol.class);
	private static final long serialVersionUID = -3146348508253958886L;
	private final Map<Operation, Map<TCPAddress, NodeInfo>> neighborUpdates = new HashMap<Operation, Map<TCPAddress, NodeInfo>>(Operation.values().length);
	private final Map<TCPAddress, NodeInfo> nodes = new HashMap<TCPAddress, NodeInfo>();

	public NodeDiscoveryProtocol() {
		super();
		setName("NodeDiscoveryProtocol");
		setPrefix("ndp");
	}

	@Override
	public void postGossip(final Address other) {
		super.postGossip(other);
		pushParentNodes();
	}

	@Override
	public void postUpdate(final Protocol other) {
		super.postUpdate(other);
		pushParentNodes();
	}

	@Override
	public void preGossip(final Address other) {
		super.preGossip(other);
		pullParentNodes();

		/*
		 * Reset the modifications list.
		 */
		neighborUpdates.clear();
		for (Operation o : Operation.values())
			neighborUpdates.put(o, new HashMap<TCPAddress, NodeInfo>());
	}

	@Override
	public void preUpdate(final Protocol other) {
		super.preUpdate(other);
		pullParentNodes();

		/*
		 * Reset the modifications list.
		 */
		neighborUpdates.clear();
		for (Operation o : Operation.values())
			neighborUpdates.put(o, new HashMap<TCPAddress, NodeInfo>());
	}

	/**
	 * Compare the node lists between the two protocol instances and determine which nodes should be
	 * updated on either side. This method is called on a "visiting" protocol instance, at a remote
	 * node, and is passed the "resident" protocol instance at that remote node.
	 * 
	 * @param that The resident protocol instance.
	 */
	@GossipUpdate
	public void update(NodeDiscoveryProtocol that) {
//		log.debug(String.format("Modify at %s with %s", that.getOrigin(), this.getOrigin()));

		/*
		 * First, pull in any nodes in the remote list that are not in our list.
		 */
		Iterator<Map.Entry<TCPAddress, NodeInfo>> thatIterator = that.nodes.entrySet().iterator();
		while (thatIterator.hasNext()) {
			Map.Entry<TCPAddress, NodeInfo> e = thatIterator.next();
			if (!this.nodes.containsKey(e.getKey())) {
				/*
				 * We don't have this node, add the node to the Add list.
				 */
				this.neighborUpdates.get(Operation.Add).put(e.getKey(), e.getValue());
			}
		}

		/*
		 * Next, push out any nodes not in the remote list that we have.
		 */
		Iterator<Map.Entry<TCPAddress, NodeInfo>> thisIterator = this.nodes.entrySet().iterator();
		while (thisIterator.hasNext()) {
			Map.Entry<TCPAddress, NodeInfo> e = thisIterator.next();
			if (!that.nodes.containsKey(e.getKey())) {
				/*
				 * That node doesn't have this node, add it to their Add list.
				 */
				that.neighborUpdates.get(Operation.Add).put(e.getKey(), e.getValue());
			}
		}

		/*
		 * Lastly, deal with common nodes.
		 */
		thatIterator = that.nodes.entrySet().iterator();
		while (thatIterator.hasNext()) {
			Map.Entry<TCPAddress, NodeInfo> thatEntry = thatIterator.next();
			thisIterator = this.nodes.entrySet().iterator();
			while (thisIterator.hasNext()) {
				Map.Entry<TCPAddress, NodeInfo> thisEntry = thisIterator.next();
				if (thatEntry.getKey().equals(thisEntry.getKey())) {
					NodeInfo thatInfo = thatEntry.getValue();
					NodeInfo thisInfo = thisEntry.getValue();
					DateTime thatTime = thatInfo.getLatestActivity();
					DateTime thisTime = thisInfo.getLatestActivity();

					/*
					 * We have the same node, figure out which protocol instance has the most recent
					 * version. Note that secondsBetween(a,b) processes b-a.
					 */
					int diff = Seconds.secondsBetween(thatTime, thisTime).getSeconds();
					if (diff == 0) {
						/*
						 * The times are the same, so don't do anything.
						 */
					} else if (diff < 0) {
						/*
						 * That instance is more recent.
						 */
						this.neighborUpdates.get(Operation.Modify).put(thisEntry.getKey(), thatInfo);
					} else if (diff > 0) {
						/*
						 * This instance is more recent.
						 */
						that.neighborUpdates.get(Operation.Modify).put(thisEntry.getKey(), thisInfo);
					} else {
						log.error("Unable to determine time difference between " + thatTime + " and " + thisTime);
					}
				}
			}
		}

		/*
		 * Wipe out the nodes list on the visiting node to save bandwidth when sending it back over
		 * the wire.
		 */
		this.nodes.clear();
	}

	/**
	 * Prepare our node list for comparison with another instance. Copy in the current parent node's
	 * list and update our own node's info to contain a current time stamp.
	 */
	public void pullParentNodes() {
		DiscoverableNode node = (DiscoverableNode) Configuration.getParameter("node");

		/*
		 * Copy the current neighbor list into the transient node list.
		 */
		nodes.clear();
//		ConcurrentHashMap<TCPAddress, NodeInfo> m = Util.copyCCCHM(node.getNeighbors());
		nodes.putAll(node.getNeighbors());

		/*
		 * Add a recent entry for ourselves.
		 */
		NodeInfo ni = ((Node) node).getInfo();
		nodes.put((TCPAddress) getAddress(), ni);
	}

	/**
	 * Reset our parent node's list to a copy of the current local node list.
	 */
	public void pushParentNodes() {
		DiscoverableNode node = (DiscoverableNode) Configuration.getParameter("node");
		node.modifyNeighbors(neighborUpdates);

		if (neighborUpdates.get(Operation.Add).size() > 0 || neighborUpdates.get(Operation.Remove).size() > 0) {
			/*
			 * "Burst" the protocol to deal with changes more quickly.
			 */
			burst();
		}
		
		/*
		 * If the node list has changed, gossip more quickly. This needs to happen somewhere in
		 * order to prevent old nodes from being exchanged. Nowhere else in the NDP are the node
		 * dates checked--only here and by the pruning thread. It's processed here because this is
		 * after we receive new nodes from a neighbor, which might contain nodes that we think are
		 * stale, but really aren't. If we did this in select(), they would be pruned even though we
		 * would have received updated versions...
		 */
		if (node.pruneNeighbors() > 0) burst();
	}
}