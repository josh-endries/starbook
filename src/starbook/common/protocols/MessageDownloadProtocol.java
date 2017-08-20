package starbook.common.protocols;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.annotations.GossipUpdate;
import org.princehouse.mica.base.annotations.Select;
import org.princehouse.mica.base.model.Protocol;
import org.princehouse.mica.base.net.model.Address;
import org.princehouse.mica.base.net.tcpip.TCPAddress;
import org.princehouse.mica.util.Distribution;

import starbook.common.CK;
import starbook.common.Command;
import starbook.common.Command.Type;
import starbook.common.Configuration;
import starbook.common.MessageStore;
import starbook.nodes.MessageStoreNode;

/**
 * <p>
 * The MessageDownloadProtocol allows one node to compare its list of messages to a random subset of
 * messages at another node.
 * </p>
 * <p>
 * Before gossiping, the "visiting" protocol instance wipes out any entries in its list of messages
 * to download. This instance then travels over the network to the remote ("host") node. Before
 * updating, the host node selects a random subset of messages from its published message store. The
 * number of messages that are selected is determined by the RandomMessageCount variable. During the
 * update, the visiting node simply copies the messages selected by the host node into a list of
 * messages to download, and then travels back over the network to its source node.
 * </p>
 * <p>
 * After gossiping, the visiting instance compares the messages in the list of messages to download
 * to its own message store. If a message matches a topic that the node is subscribed to and the
 * node's message store doesn't contain the message in question, the node sends a MessageRequest
 * command to the host node with which gossip was performed. Presumably, a MessageUpload command
 * will follow, adding that message to the visiting node's message store.
 * </p>
 * 
 * @author Josh Endries (josh@endries.org)
 * 
 */
public class MessageDownloadProtocol extends BaseDiscoveryProtocol {
	private static final Logger log = Logger.getLogger(MessageDownloadProtocol.class);
	private static final long serialVersionUID = -2018632141233468742L;
	private static final int PortNumber = Configuration.getInt(CK.MessageDownloadPort);

	/**
	 * The number of random message GUIDs to compare in each execution of the protocol.
	 */
//	private static final int RandomMessageCount = 50;
	private static final int RandomMessageListSize = 500000;

	/**
	 * A transient holding area for the message GUIDs that this instance's parent node contains. This
	 * is emptied and populated before being sent to a remote node for message comparison, and used
	 * in the comparison to determine if this instance already contains the message being compared.
	 * This is a Collection because the GUIDs are stored as a map, returned as a Set, or randomly
	 * sampled as a List.
	 */
	private final HashSet<String> messageGUIDs = new HashSet<String>();



	/**
	 * A transient holding area for message GUIDs that were determined, during message comparison at
	 * a remote node in the update method, should be downloaded to this node. This is emptied just
	 * before this instance is sent to a remote node to perform message comparison. Any message GUIDs
	 * in this set after comparison will cause a MessageRequest command to be sent to the node at
	 * which the comparison occurred, in order to download the message.
	 */
	private final HashSet<String> messageCandidates = new HashSet<String>();



	/**
	 * Create an instance of the MessageDownloadProtocol (MDP). This will set the protocol's prefix
	 * to "mdp" and the name to "MessageDownloadProtocol".
	 */
	public MessageDownloadProtocol() {
		super();
		setName("MessageDownloadProtocol");
		setPrefix("mdp");
//		setMinimumRate(0.5);
	}



	/**
	 * Inspect the result set of random message GUIDs discovered at the remote node. If there exist
	 * any that this node should download, send MessageRequest commands to the remote node so the
	 * messages get downloaded. Hopefully.
	 */
	private void inspectCandidates(Address source) {
		MessageStoreNode node = (MessageStoreNode) Configuration.getParameter("node");
		Collection<String> subscriptions = node.getSubscribedTopics();
		MessageStore ms = node.getStoredMessageStore();

		log.debug(String.format("Found %s potential messages to download from %s.", messageCandidates.size(), source));
		boolean changed = false;
		for (String guid : messageCandidates) {
			String[] parts = guid.split(Pattern.quote("|"));
			String topic = parts[1].toLowerCase();

			if (subscriptions.contains(topic)) {
				/*
				 * This message is regarding a topic that we are subscribed to. Determine if we already
				 * have a copy of it.
				 */
				if (ms.getMessageByGUID(guid) == null) {
					/*
					 * We don't have this message. Download it from the remote node.
					 */
					Map<String, Object> m = new HashMap<String, Object>(1);
					m.put("guid", guid);
					m.put("source", getOrigin());
					Command c = new Command(Type.MessageRequest, m);
					try {
						log.debug(String.format("Downloading message %s", guid));
						c.sendViaUDP(new InetSocketAddress(source.getInetAddressAddress(), Configuration.getInt(CK.CommandPort)));
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					changed = true;
				} else {
					log.debug(String.format("Message %s already exists.", guid));
				}
			} else {
				log.debug(String.format("Not subscribed to %s, skipping message %s.", topic, guid));
			}
		}
		
		/*
		 * Since there was at least one new message, either there is a node out there with messages
		 * we might be interested in, or a user is posting messages. Either way, bump up the gossip
		 * rate to try and catch even more messages if they're out there...
		 */
		if (changed) burst();
	}

	
	
	@Override
	public void postGossip(final Address other) {
		super.postGossip(other);
		inspectCandidates(other);
	}
	
	@Override
	public void postUpdate(final Protocol other) {
		super.postUpdate(other);
		inspectCandidates(new TCPAddress(other.getOrigin(), PortNumber));
	}



	/**
	 * Clear out the local state and select a random sample of published messages before serializing
	 * and traveling over the network to the remote node.
	 */
	@Override
	public void preGossip(final Address other) {
		super.preGossip(other);
		prepareLists();
	}

	/**
	 * Reset the transient GUID lists and populate the random message list with GUIDs to exchange.
	 */
	private void prepareLists() {
		MessageStoreNode node = (MessageStoreNode) Configuration.getParameter("node");
		messageCandidates.clear();
		messageGUIDs.clear();
		messageGUIDs.addAll(node.getPublishedMessageStore().getRandomGUIDsByByte(RandomMessageListSize));
	}

	/**
	 * preUpdate is called just before the update method is called on the visiting protocol and this
	 * protocol instance is passed in as a parameter. Therefore, we need to set up the messages that
	 * we want to offer for exchange. The host node will extract RandomMessageCount GUIDs from its
	 * list to offer for comparison with the visiting node.
	 */
	@Override
	public void preUpdate(final Protocol p) {
		super.preUpdate(p);
		prepareLists();
	}



	/**
	 * We don't have any nodes, so we copy them from the parent node (which retrieves them through
	 * the discovery protocol). However, we run on a different port from that protocol so we need to
	 * iterate through them all and reconstruct the map. We then take this new map and set this
	 * instance's node list to the contents and run the super class's select method, which will take
	 * care of any ignore rules and select an address.
	 */
	@Override
	@Select
	public Distribution<Address> select() {
		/*
		 * Run the normal select.
		 */
		Distribution<Address> distribution = super.select();
		
		/*
		 * Recreate the map but with different port numbers.
		 */
		Distribution<Address> d = new Distribution<Address>();
		Iterator<Map.Entry<Address, Double>> i = distribution.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<Address, Double> e = i.next();
			TCPAddress a = (TCPAddress) e.getKey();
			Double v = e.getValue();
			TCPAddress newAddress = new TCPAddress(a.getInetAddressAddress(), PortNumber);
			d.put(newAddress, v);
		}
		
		return d;
	}



	/**
	 * Copy each instance's list of published messages to the other side for post processing and
	 * comparison to their respective message stores after gossip is finished.
	 * 
	 * @param that The "host" protocol instance.
	 */
	@GossipUpdate
	public void update(MessageDownloadProtocol that) {
		this.messageCandidates.addAll(that.messageGUIDs);
		that.messageCandidates.addAll(this.messageGUIDs);
		
		/*
		 * Wipe out the message list to save bandwidth.
		 */
		this.messageGUIDs.clear();
	}
}
