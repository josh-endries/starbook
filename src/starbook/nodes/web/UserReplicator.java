package starbook.nodes.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

import starbook.common.BaseUser;
import starbook.common.NodeInfo;
import starbook.common.User;

/**
 * Periodically retrieves the parent node's users and determines if the parent node will start
 * tracking a new user or not. This determination is done by comparing a random double between zero
 * and one to 1/x where x is the current number of users, thereby slowing down the rate of new user
 * acceptance until it's practically zero.
 * 
 * If a new user is warranted, UserReplicator retrieves the latest neighbor list from the parent
 * node. It scans through this list and counts the number of instances of each user. If a user is
 * either (1) already being tracked by this node or (2) is being tracked by the requisite number of
 * other nodes (determined by ReplicaCount), it is skipped. Otherwise, valid users and their counts
 * are added to a map. The map is traversed and the first user with the lowest count is chosen to
 * be replicated. UserReplicator then takes this user's name and sends it in a UserRequest command
 * to the index, which should have the most up-to-date user list. Theoretically, the index will
 * send a UserUpload command back with the User object so it can be added to this node's user list.
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public class UserReplicator implements Runnable {
	private final static Logger log = Logger.getLogger(UserReplicator.class);
	private final WebNode node;
	private static final int Delay = 10000;
	private static final int ReplicaCount = 2;

	/**
	 * Creates a new UserReplicator with its parent node set to the specified node.
	 * 
	 * @param node The parent node of this UserReplicator.
	 */
	public UserReplicator(WebNode node) {
		this.node = node;
	}

	@Override
	public void run() {
		boolean running = true;
		while (running) {
			try {
				Thread.sleep(Delay);
			} catch (InterruptedException e) {
				log.debug("Interrupted, setting running to false.");
				running = false;
				continue;
			}
			log.debug("Replicating users...");

			/*
			 * Retrieve a copy of the user list.
			 */
			Set<User> users = node.getUsers();
			
			/*
			 * If we want to replicate a new user, do so.
			 */
			if (node.canReplicate()) {
				log.debug("Looking for an additional user to replicate.");
				
				/*
				 * Go through all known users that we don't already track and
				 * count them up to see if any are below the replication limit.
				 */
				Map<String, Integer> counts = new HashMap<String, Integer>();
				Iterator<Map.Entry<TCPAddress, NodeInfo>> iterator = node.getNeighbors().entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<TCPAddress, NodeInfo> entry = iterator.next();
					
					/*
					 * Get the current node's user list.
					 */
					Set<String> userNames = entry.getValue().getUserNames();
					
					/*
					 * Loop through this node's user list.
					 */
					Iterator<String> userIterator = userNames.iterator();
					while (userIterator.hasNext()) {
						String userName = userIterator.next();
						
						/*
						 * Get the current count for this user.
						 */
						Integer count = counts.get(userName);
						if (count == null) {
							counts.put(userName, 1);
						} else {
							counts.put(userName, counts.get(userName));
						}
					}
				}
				
				/*
				 * Now we have a count of the current users across all known nodes.
				 * Remove the users that we already track and users that exist more than ReplicaCount
				 * times.
				 */
				Iterator<Map.Entry<String, Integer>> i = counts.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<String, Integer> e = i.next();
					User u = new BaseUser(e.getKey());
					if (users.contains(u)) {
						i.remove();
					} else if (e.getValue() >= ReplicaCount) {
						i.remove();
					}
				}
				
				/*
				 * If we have remaining users, find the one with the lowest count
				 * and start tracking it.
				 */
				if (counts.size() > 0) {
					i = counts.entrySet().iterator();
					String userName = null;
					int count = 0;
					
					/*
					 * Loop through the user names and update the temporary user name if its count is
					 * lower.
					 */
					while (i.hasNext()) {
						Map.Entry<String, Integer> e = i.next();
						if (e.getValue() < count) {
							userName = e.getKey();
							count = e.getValue();
						}
					}
					
					/*
					 * We should now have our user name. Send a command to the index to fetch the latest
					 * version.
					 */
					if (userName != null) {
						node.fetchUser(userName);
					}
				}
			}
		}
	}
}
