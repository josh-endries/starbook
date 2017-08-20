package starbook.nodes;

import java.util.Set;

import starbook.common.MessageStore;

/**
 * A message discovery node implements methods that allow other classes access to message that it
 * stores and that it publishes for download. Typically, nodes that implement these interfaces
 * participate in a message discovery protocol, which uses these before gossiping and updating.
 *
 * @author Josh Endries (josh@endries.org)
 *
 */
public interface MessageStoreNode {
	/**
	 * Retrieve a reference to this node's list of subscribed topics.
	 * 
	 * @return The reference.
	 */
	public Set<String> getSubscribedTopics();


	
	/**
	 * Retrieve the message store containing messages that are available for download.
	 * 
	 * @return The {@link MessageStore}.
	 */
	public MessageStore getPublishedMessageStore();

	
	
	/**
	 * Retrieve the message store containing messages that are stored at this node.
	 * 
	 * @return The {@link MessageStore}.
	 */
	public MessageStore getStoredMessageStore();
}
