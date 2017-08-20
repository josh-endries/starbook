package starbook.common;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.BaseProtocol;

public abstract class UpdateableProtocol extends BaseProtocol {
	private static final Logger log = Logger.getLogger(UpdateableProtocol.class);
	private static final long serialVersionUID = -7791952654813906393L;
	
	private SortedSet<NodeInfo> nodes;
	
	public UpdateableProtocol() {
		nodes = new TreeSet<NodeInfo>();
		setName("UpdateableProtocol");
	}

	public void setNodes(Collection<NodeInfo> newNodes) {
//		log.debug("Setting nodes from: "+newNodes);
		SortedSet<NodeInfo> newNodeCopy = new TreeSet<NodeInfo>();
		for (NodeInfo ni: newNodes) {
			newNodeCopy.add(ni.copy());
		}
		synchronized (nodes) {
			nodes = newNodeCopy;
		}
		log.debug("Nodes is now: "+nodes);
	}
	
	/**
	 * Returns an unmodifiable set of this protocol's nodes.
	 * 
	 * @return The unmodifiable set of nodes.
	 */
	public SortedSet<NodeInfo> getNodes() {
		return Collections.unmodifiableSortedSet(nodes);
	}
	
	@Override
	public String toString() {
		return String.format("%s[name=%s, address=%s]", "UpdateableProtocol", getName(), getAddress());
	}
}
