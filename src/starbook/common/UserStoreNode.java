package starbook.common;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public interface UserStoreNode {
	/**
	 * Add the user to this node's user list if it doesn't exist, or updates it to the given value if
	 * it already exists.
	 * 
	 * @param u The user to add or update.
	 * @returns True if the user was added or updated, false otherwise.
	 */
	public void addUser(User u);

	/**
	 * Get a copy of the named user from this node's user list.
	 * 
	 * @param name The name of the User to retrieve.
	 * @return The User object matching the given name, or null if the user doesn't exist.
	 */
	public User getUser(String name);

	/**
	 * Retrieves a copy of this node's user list.
	 * 
	 * @return A copy of the set of users for which this node is responsible.
	 */
	public ConcurrentSkipListSet<User> getUsers();

	/**
	 * Updates the users in this node's user list to the provided user objects (this method neither
	 * creates or deletes user objects).
	 * 
	 * @param users The set form which to copy and update this node's user list.
	 */
	public void updateUsers(Map<Operation, Set<User>> users, InetAddress tcpAddress);
}
