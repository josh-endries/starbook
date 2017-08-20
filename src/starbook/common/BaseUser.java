package starbook.common;

import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;

import org.joda.time.DateTime;

/**
 * Implements a basic user. One user is "equal" to another user if the user names match.
 * 
 * @author Josh Endries <josh@endries.org>
 */
public class BaseUser implements User {
	private static final long serialVersionUID = 2726168823442149262L;
	private final String name;
	private final ConcurrentSkipListSet<String> subscriptions = new ConcurrentSkipListSet<String>();
	private DateTime latestActivity = new DateTime();

	
	
	public BaseUser(String name) {
		super();
		if (!checkName(name))
			throw new IllegalArgumentException("Invalid character in name.");
		this.name = name;
		subscriptions.add(name);
	}

	
	
	/**
	 * Creates a new BaseUser with values copied from the given User.
	 * 
	 * TODO: Currently require both User and BaseUser versions because copyCCS doesn't work when
	 * passing in a "User", throws NoSuchMethodException for the constructor, I guess due to passing
	 * in a BaseUser but specifying only a User constructor here...
	 * 
	 * @param b
	 *           The user from which to copy values.
	 */
	public BaseUser(User b) {
		this(new String(b.getName()));
		Iterator<String> i = b.getSubscriptions().iterator();
		while (i.hasNext()) {
			String s = new String(i.next());
			subscriptions.add(s);
		}
		latestActivity = new DateTime(b.getLatestActivity());
	}
	public BaseUser(BaseUser b) {
		this(new String(b.getName()));
		Iterator<String> i = b.getSubscriptions().iterator();
		while (i.hasNext()) {
			String s = new String(i.next());
			subscriptions.add(s);
		}
		latestActivity = new DateTime(b.getLatestActivity());
	}

	
	
	public static boolean checkName(String name) {
		if (name.matches("[a-zA-Z0-9-]+"))
			return true;
		return false;
	}

	
	
	@Override
	public int compareTo(User o) {
		return getName().compareTo(o.getName());
	}

	
	
	/**
	 * Two User objects are "equal" if their user names are the same when compared
	 * case-insensitively.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof User) {
			User u = (User) obj;
			return getName().equalsIgnoreCase(u.getName());
		}
		return false;
	}

	
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	
	
	public String getName() {
		return name;
	}

	
	
	/**
	 * Retrieve a reference to this user's subscription list.
	 */
	public ConcurrentSkipListSet<String> getSubscriptions() {
		return subscriptions;
	}

	
	
	public String toString() {
		return String.format("User<name=\"%s\", subscriptions=%s, latestActivity=%s>", name, subscriptions, latestActivity);
	}

	
	
	@Override
	public DateTime getLatestActivity() {
		return latestActivity;
	}

	
	
	@Override
	public void setLatestActivity(DateTime time) {
		latestActivity = time;
	}
}
