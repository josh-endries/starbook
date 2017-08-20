package starbook.common;

import org.apache.log4j.Logger;

/**
 * Prunes this node's list of neighbors. This is necessary because MiCA will throw an exception
 * during select if it cannot connect, meaning update will only be called when a valid node is
 * selected. If there are no neighbors, or there are many unaccessible neighbors, this may take a
 * long time. This thread prevents that by pruning old neighbors via an external method. 
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public class NodePruner implements Runnable {
	private final static Logger log = Logger.getLogger(NodePruner.class);
	private final DiscoverableNode node;
	private static final int Delay = 15000;

	public NodePruner(DiscoverableNode node) {
		this.node = node;
	}

	@Override
	public void run() {
		boolean running = true;
		while (running) {
			try {
				Thread.sleep(Delay);
			} catch (InterruptedException e) {
				running = false;
				continue;
			}
			
			log.debug("Pruning node list...");
			int pruned = node.pruneNeighbors();
			if (pruned > 0) {
				log.debug(String.format("Pruned %s nodes.", pruned));
				node.getDiscoveryProtocolRuntme().getProtocolInstance().burst();
			}
		}
	}
}
