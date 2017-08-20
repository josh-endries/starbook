package starbook.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


public interface MessageStore extends Serializable {
	/**
	 * Add a copy of the given message to the message store.
	 * 
	 * @param m The message to copy and add to the store.
	 */
	public abstract void addMessage(Message m);

	/**
	 * Retrieve a copy of the message associated with the given GUID and increment the message's
	 * download count. If the count is higher than the given limit, the message is deleted from the
	 * message store.
	 * 
	 * @param guid The GUID of the message to download.
	 * @param downloadLimit The number of times this message must be downloaded before removal.
	 * @return The message or null if the message is not in the list.
	 */
	public abstract Message downloadMessage(String guid, int downloadLimit);

	/**
	 * Returns the message specified by the given GUID, or null if the message doesn't exist.
	 * 
	 * @param guid The GUID of the message to retrieve.
	 * @return The message identified by the given GUID, or null if there is no message with that
	 *         GUID.
	 */
	public abstract Message getMessageByGUID(String guid);

	/**
	 * Retrieve a copy of the message GUIDs currently stored in the message store.
	 * 
	 * @return The set of message GUIDs.
	 */
	public abstract Set<String> getMessageGUIDs();

	/**
	 * Retrieve a copy of the messages in the message store.
	 * 
	 * @return A set of message copies.
	 */
	public abstract Set<Message> getMessages();

	/**
	 * Retrieves a set of messages (by reference) associated with a given topic. If there are no
	 * messages for the give topic, an empty set is returned.
	 * 
	 * @param topic The topic for which to retrieve messages.
	 * @return The set of messages.
	 */
	public abstract ConcurrentSkipListSet<Message> getMessagesByTopic(String topic);

	/**
	 * Retrieve a random subset of GUIDs of the messages in this message store, or all messages in
	 * the message store, whichever is smaller (i.e., if there are not resultSize items in the
	 * message store, all are returned).
	 * 
	 * @param resultSize The number of message GUIDs to retrieve.
	 * @return The list of random message GUIDs.
	 */
	public abstract ArrayList<String> getRandomGUIDs(int resultSize);

	/**
	 * Remove the given message from the message store.
	 * 
	 * @param m The message to remove.
	 */
	public abstract void removeMessage(Message m);

	/**
	 * Retrieve a list of random messages, approximately bounded by the specified size (in bytes).
	 * Since it is difficult to determine the actual size of a String (without specifying the
	 * encoding type, at least), this determination is only approximate. Due to overhead of Java
	 * objects, the actual size of a list of messages will be higher than the returned data.
	 * 
	 * @param maxSize The maximum size, in bytes, of the total of the returned messages.
	 * @return The list of messages.
	 */
	HashSet<String> getRandomGUIDsByByte(int maxSize);
}