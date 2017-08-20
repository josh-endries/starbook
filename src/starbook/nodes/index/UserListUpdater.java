package starbook.nodes.index;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

import starbook.common.BaseNode.Type;
import starbook.common.CK;
import starbook.common.NodeInfo;
import starbook.common.User;
import starbook.common.Util;

public class UserListUpdater implements Runnable {
	private final static Logger log = Logger.getLogger(UserListUpdater.class);
	private final IndexNode node;
	private static final int Delay = 5000;
	private static final int ReplicaCount = 2;
	private final HashSet<InetAddress> previousIndexAddresses = new HashSet<InetAddress>();

	public UserListUpdater(IndexNode node) {
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
			
			
			
			/*
			 * Determine if our node is the leader. If we aren't the leader, we don't update DNS, but
			 * we still update our user assignment list.
			 */
			boolean leader = (node.getLeaderAddress().equals(node.getInetAddress()));



			/*
			 * First, create a copy of the current node list (and convert it to InetAddress).
			 */
			Map<InetAddress, NodeInfo> currentNodes = new HashMap<InetAddress, NodeInfo>();
			Iterator<Map.Entry<TCPAddress, NodeInfo>> i = node.getNeighbors().entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<TCPAddress, NodeInfo> e = i.next();
				currentNodes.put(Util.copy(e.getKey().getInetAddressAddress()), new NodeInfo(e.getValue()));
			}



			if (leader) {
				/*
				 * Next, make sure the index list is up-to-date. To do this we first assemble a list of
				 * index nodes, then clear the index nodes out of DNS and insert new entries. This should
				 * reduce any gap where there are no index nodes in DNS. Even better would be to do this
				 * inside of a transaction, once I figure out how to do that.
				 */
				log.debug("Updating index list...");
				HashSet<InetAddress> indexAddresses = new HashSet<InetAddress>();
				Iterator<Map.Entry<TCPAddress, NodeInfo>> neighborIterator = node.getNeighbors().entrySet().iterator();
				while (neighborIterator.hasNext()) {
					Map.Entry<TCPAddress, NodeInfo> e = neighborIterator.next();
					if (e.getValue().getType().equals(Type.Index))
						indexAddresses.add(e.getKey().getInetAddressAddress());
				}
	
				/*
				 * Add this node to the list because it isn't in its own neighbor list.
				 */
				indexAddresses.add(node.getInetAddress());
	
				/*
				 * Update DNS if applicable.
				 */
				if (previousIndexAddresses.containsAll(indexAddresses) && indexAddresses.containsAll(previousIndexAddresses)) {
					/*
					 * The lists are the same, don't do anything.
					 */
				} else {
					/*
					 * The lists have at least one difference. Update DNS and reset the list to what we were
					 * given.
					 */
					DNS.Instance.modifyIndexNodes(indexAddresses);
					previousIndexAddresses.clear();
					previousIndexAddresses.addAll(indexAddresses);
				}
			}
			
			log.debug("Updating user list...");
			boolean changed = false;

			/*
			 * First, verify that the current user list has an accurate set of nodes.
			 */
			ConcurrentHashMap<User, ConcurrentSkipListSet<InetAddress>> users = node.getUserAssociations();
			log.debug("Current user associations: " + users);
			Iterator<Map.Entry<User, ConcurrentSkipListSet<InetAddress>>> userIterator = users.entrySet().iterator();
			while (userIterator.hasNext()) {
				Map.Entry<User, ConcurrentSkipListSet<InetAddress>> e = userIterator.next();
				User user = e.getKey();
				ConcurrentSkipListSet<InetAddress> userNodes = e.getValue();
				if (userNodes.size() > 0) {
					Iterator<InetAddress> nodeIterator = userNodes.iterator();
					while (nodeIterator.hasNext()) {
						InetAddress a = nodeIterator.next();
						if (!currentNodes.containsKey(a)) {
							/*
							 * This user's node doesn't exist in the current node list, we need to remove
							 * it. If this is the last node for this user, log an error--that means we've
							 * probably lost their data. Not awesome.
							 */
							log.debug(String.format("Removing non-existent node %s from user %s.", a, user));
							if (userNodes.size() == 1)
								log.error(String.format("%s exists on no active nodes.", user));
							nodeIterator.remove();

							/*
							 * Assign the user to a random node.
							 */
							try {
								node.sendAddUserCommand(user);
							} catch (IOException e1) {
								e1.printStackTrace();
							}

							changed = true;
						} else if (!currentNodes.get(a).getUserNames().contains(user.getName())) {
							/*
							 * The node exists but it doesn't have this user listed in it's user list.
							 * Remove it unless the user is newer than the node. In that case the user may
							 * be new and the node has not yet .
							 */
							log.debug(String.format("Removing user %s from node %s, which doesn't track the user.", user, a));
							if (userNodes.size() == 1)
								log.error(String.format("%s exists on no active nodes.", user));
							nodeIterator.remove();

							/*
							 * Assign the user to a random new node.
							 */
							try {
								node.sendAddUserCommand(user);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							changed = true;
						}
					}
				} else {
					/*
					 * The user has no nodes. Check the current node list and see if any nodes actually
					 * do track this user. If so, store them in the results variable.
					 */
					Map<InetAddress, NodeInfo> results = new HashMap<InetAddress, NodeInfo>();
					for (Map.Entry<InetAddress, NodeInfo> currentNodeEntry: currentNodes.entrySet()) {
						if (currentNodeEntry.getValue().getUserNames().contains(user.getName())) {
							results.put(currentNodeEntry.getKey(), currentNodeEntry.getValue());
						}
					}

					/*
					 * If we found some nodes out there that do track this user. Associate the current
					 * user with the discovered nodes.
					 */
					if (results.size() > 0) {
						log.debug(String.format("Found additional nodes that track %s: %s", user, results));
						for (InetAddress a: results.keySet()) {
							try {
								node.associateUser(user, a);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						changed = true;
					} else {
						/*
						 * Assign the user to a random new node.
						 */
						log.debug(String.format("User %s has no nodes; assigning a random node.", user));
						try {
							node.sendAddUserCommand(user);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						changed = true;
					}
				}
			}

			/*
			 * If we didn't make any changes this round, scan the list to see if every user is
			 * replicated on enough nodes. If not, add it to a new node if possible.
			 */
			if (!changed) {
				/*
				 * Reset the iterator.
				 */
				userIterator = users.entrySet().iterator();
				while (userIterator.hasNext()) {
					Map.Entry<User, ConcurrentSkipListSet<InetAddress>> e = userIterator.next();
					User user = e.getKey();
					ConcurrentSkipListSet<InetAddress> userNodes = e.getValue();
					if (userNodes.size() < ReplicaCount) {
						/*
						 * We need to replicate this user on more nodes. Try to find one and add it (just
						 * one per round).
						 */
						int attempt = 0;
						TCPAddress result = null;
						while (attempt < ReplicaCount && result == null) {
							try {
								TCPAddress r = node.getRandomNeighbor(false, Type.Web);
								if (!userNodes.contains(r.getInetAddressAddress()))
									result = r;
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							attempt++;
						}

						if (result != null) {
							try {
								log.debug(String.format("Adding new replication node for %s: %s", user, result));
								node.sendAddUserCommand(user,
										new InetSocketAddress(result.getInetAddressAddress().getHostAddress(), Configuration.getInt(CK.CommandPort)));
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
			}
		}
	}
}
