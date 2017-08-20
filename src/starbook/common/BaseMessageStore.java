package starbook.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;


/**
 * Manages multiple lists of messages for quick retrieval while presenting a
 * single interface for CRUD operations.
 * 
 * @author Josh Endries <josh@endries.org>
 */
public class BaseMessageStore implements MessageStore {
	private static final long serialVersionUID = 2980172550137448784L;
	private final static Logger log = Logger.getLogger(BaseMessageStore.class);
	
	/**
	 * The "actual" Message object store.
	 */
	private final ConcurrentSkipListSet<Message> messages = new ConcurrentSkipListSet<Message>();

	/**
	 * A map of download counts, indexed by GUID.
	 */
	private final ConcurrentHashMap<String, Integer> downloadCounts = new ConcurrentHashMap<String, Integer>();
	
	/**
	 * A message list indexed by the message's GUIDs.
	 */
	private final ConcurrentHashMap<String, Message> messagesByGUID = new ConcurrentHashMap<String, Message>();

	/**
	 * A message list indexed by topic.
	 */
	private final ConcurrentHashMap<String, ConcurrentSkipListSet<Message>> messagesByTopic = new ConcurrentHashMap<String, ConcurrentSkipListSet<Message>>();

	@Override
	public void addMessage(Message m) {
		/*
		 * Make a copy of the incoming message.
		 */
		Message message = new BaseMessage(m);

		/*
		 * Add the message to the "actual" message list.
		 */
		messages.add(message);

		/*
		 * Add the message to the GUID list.
		 */
		messagesByGUID.put(message.getGUID(), message);
		
		/*
		 * Add the message to the download count map.
		 */
		downloadCounts.put(message.getGUID(), 0);

		/*
		 * Add the message to the topic list. Since we need to do two calls
		 * here, we synchronize on the list...
		 */
		synchronized (messagesByTopic) {
			if (messagesByTopic.containsKey(message.getTopic())) {
				messagesByTopic.get(message.getTopic()).add(message);
			} else {
				ConcurrentSkipListSet<Message> list = new ConcurrentSkipListSet<Message>();
				list.add(message);
				messagesByTopic.put(message.getTopic(), list);
			}
		}
	}

	@Override
	public Message downloadMessage(String guid, int downloadLimit) {
		Message message = messagesByGUID.get(guid);
		if (message == null) return null;
		Integer currentCount = downloadCounts.get(guid);
		if (currentCount == null) {
			log.warn(String.format("Non-existent download count for existing message %s.", guid));
			currentCount = 0;
		}
		currentCount++;
		downloadCounts.put(guid, currentCount);
		if (currentCount >= downloadLimit) {
			log.debug(String.format("Message %s has been downloaded %s times, removing it.", guid, downloadLimit));
			removeMessage(message);
		}
		return message;
	}

	@Override
	public Message getMessageByGUID(String guid) {
		return messagesByGUID.get(guid);
	}
	
	@Override
	public Set<String> getMessageGUIDs() {
		Set<String> s = new HashSet<String>();
		Iterator<String> i = messagesByGUID.keySet().iterator();
		while (i.hasNext()) {
			s.add(new String(i.next()));
		}
		return s;
	}
	
	@Override
	public Set<Message> getMessages() {
		Set<Message> set = new HashSet<Message>();
		Iterator<Message> i = messages.iterator();
		while (i.hasNext()) set.add(new BaseMessage(i.next()));
		return set;
	}

	@Override
	public ConcurrentSkipListSet<Message> getMessagesByTopic(String topic) {
		ConcurrentSkipListSet<Message> m = messagesByTopic.get(topic);
		if (m == null) {
			return new ConcurrentSkipListSet<Message>();
		} else {
			return m;
		}		
	}

	@Override
	public void removeMessage(Message m) {
		String topic = m.getTopic();
		messages.remove(m);
		messagesByGUID.remove(m.getGUID());
		messagesByTopic.get(topic).remove(m);
		downloadCounts.remove(m);

		/*
		 * This should be synchronized somehow.
		 */
		if (messagesByTopic.containsKey(topic) && messagesByTopic.get(topic).size() < 1) {
			messagesByTopic.remove(topic);
		}
	}
	
	@Override
	public HashSet<String> getRandomGUIDsByByte(int maxSize) {
		HashSet<String> GUIDs = new HashSet<String>();
		if (this.messages.size() < 1) return GUIDs;
		ArrayList<Integer> indices = new ArrayList<Integer>(this.messages.size());
		for (int i=0; i<this.messages.size(); i++) indices.add(i);
		Collections.shuffle(indices);
		int size = 0;
		Iterator<Integer> i = indices.iterator();
		while (i.hasNext() && size < maxSize) {
			int index = i.next();
			Iterator<Message> mi = this.messages.iterator();
			Message message = mi.next();
			for (int x=0; x<index; x++) if (mi.hasNext()) message = mi.next();
			int messageSize = (message.getContent().length()*2); // Java uses 16-bit chars by default.
			if ((messageSize+size) < maxSize) {
				size += messageSize;
				GUIDs.add(message.getGUID());
			}
		}
		
		return GUIDs;
	}

	/**
	 * Retrieves a random subset of message GUIDs by copying the first N GUIDs where N is
	 * resultSize, then iterates through the GUIDs randomly replacing elements in the result list.
	 * 
	 * @see MessageStore#getRandomGUIDs(int)
	 */
	@Override
	public ArrayList<String> getRandomGUIDs(int resultSize) {
		ArrayList<String> s = new ArrayList<String>();
		
		int count = 0;
		for (String guid: messagesByGUID.keySet()) {
			count++;
			if (count <= resultSize) {
				s.add(guid);
			} else {
				int r = Configuration.rng.nextInt(count);
				if (r < resultSize) {
					s.set(r, guid);
				}
			}
		}
		
		return s;
	}
}
