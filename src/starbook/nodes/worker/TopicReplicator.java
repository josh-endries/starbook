package starbook.nodes.worker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

import starbook.common.BaseNode.Type;
import starbook.common.NodeInfo;

public class TopicReplicator implements Runnable {
	private final static Logger log = Logger.getLogger(TopicReplicator.class);
	private final WorkerNode node;
	private static final int Delay = 10000;
	private static final int ReplicaCount = 3;

	public TopicReplicator(WorkerNode node) {
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
			log.debug("Replicating topics...");

			/*
			 * Determine if we want to subscribe to a new topic. As the number of
			 * subscribed topics grows, the limit will get smaller, so we generate
			 * a random number and check if that random number is lower than the
			 * limit.
			 */
//			int size = node.getSubscribedTopics().size();
//			double limit = (size == 0) ? 1.0 : (1.0 / size);
//			double chance = Configuration.rng.nextDouble();
//			log.debug(String.format("Limit: %s, chance: %s", limit, chance));
			
//			if (chance < limit) {
			if (node.canReplicate()) {
				log.debug("Replicating an additional topic.");
				Set<String> subscribedTopics = node.getSubscribedTopics();
				
				/*
				 * Go through all known topics that we don't already track and
				 * count them up.
				 */
				Map<String, Integer> counts = new HashMap<String, Integer>();
				Iterator<Map.Entry<TCPAddress, NodeInfo>> neighborIterator = node.getNeighbors().entrySet().iterator();
				while (neighborIterator.hasNext()) {
					Map.Entry<TCPAddress, NodeInfo> e = neighborIterator.next();
					NodeInfo ni = e.getValue();
					Collection<String> topics = ni.getSubscribedTopics();
					Iterator<String> topicIterator = topics.iterator();
					while (topicIterator.hasNext()) {
						String topic = topicIterator.next();
						
						/*
						 * We already have this topic, skip it.
						 */
						if (subscribedTopics.contains(topic)) continue;
						
						/*
						 * If the topic isn't in the list, add it so we record its existence. If it is in
						 * the list, only increment the counter if the node is a worker node. Web nodes
						 * don't count towards topic storage since their state is transient.
						 */
						if (counts.containsKey(topic) && ni.getType().equals(Type.Worker)) {
							counts.put(topic, counts.get(topic) + 1);
						} else {
							counts.put(topic, 1);
						}
					}
				}
				log.debug("Counted topics: "+counts);

				if (counts.size() > 0) {
					/*
					 * Loop through the counts, retrieving the last one with the lowest
					 * count (multiple entries might have the same count, including the
					 * lowest count).
					 */
					Map.Entry<String, Integer> first = counts.entrySet().iterator().next();
					String topic = first.getKey();
					Integer count = first.getValue();
					for (Map.Entry<String, Integer> e : counts.entrySet()) {
						if (e.getValue() < count) {
							topic = e.getKey();
							count = e.getValue();
						}
					}
					
					/*
					 * At this point we will have a new topic (and its count). If
					 * its count is less than the minimum required, add it to the
					 * list of subscribed topics.
					 */
					if (count < ReplicaCount) {
						log.debug("Subscribing to new replicated topic: "+topic);
						node.getSubscribedTopics().add(topic);
					}
				}
			}
		}
	}
}
