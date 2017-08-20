package starbook.nodes.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;

import starbook.common.Operation;
import starbook.common.BaseUser;
import starbook.common.User;

public class SubscriptionsServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(SubscriptionsServlet.class);
	private static final long serialVersionUID = 3816854113577854762L;
	private final WebNode node = (WebNode) Configuration.getParameter("node");



	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		/*
		 * First, retrieve the user.
		 */
		User user = getUser(req);

		/*
		 * Retrieve a list of topics in which this user is interested.
		 */
		Set<String> subscriptions = user.getSubscriptions();

		/*
		 * Create a JSON array from the list and write it out.
		 */
		JSONArray jsonArray = new JSONArray(subscriptions);
		resp.getWriter().write(jsonArray.toString());
	}



	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		User user = getUser(req);
		
		String topic = req.getParameter("topic");
		if (topic == null) return;
		
		String action = req.getParameter("action");
		if (action == null) return;

		if (action.equalsIgnoreCase("remove")) {
			if (topic.equalsIgnoreCase(user.getName())) {
				/*
				 * This shouldn't happen with the way the UI is designed; someone is monkeying around.
				 * Log it.
				 */
				log.warn(String.format("User %s attempted to unsubscribe from themselves.", user.getName()));
			} else {
				log.debug(String.format("Unsubscribing user %s from %s", user.getName(), topic));

				/*
				 * Create an updated User object and tell the node to update its entry.
				 */
				User newUser = new BaseUser(user);
				newUser.getSubscriptions().remove(topic);
				newUser.setLatestActivity(new DateTime());
				
				Map<Operation, Set<User>> m = new HashMap<Operation, Set<User>>(1);
				Set<User> s = new HashSet<User>(1);
				s.add(newUser);
				m.put(Operation.Modify, s);
				node.updateUsers(m, null);
			}
		} else if (action.equalsIgnoreCase("add")) {
			if (topic.equalsIgnoreCase(user.getName())) {
				/*
				 * This isn't really an error, just silly. Ignore it.
				 */
				log.debug(String.format("User %s attempted subscribe to themselves.", user.getName()));
			} else {
				log.debug(String.format("Subscribing user %s to %s", user.getName(), topic));

				/*
				 * Create an updated User object and tell the node to update its entry.
				 */
				User newUser = new BaseUser(user);
				newUser.getSubscriptions().add(topic);
				newUser.setLatestActivity(new DateTime());
				
				Map<Operation, Set<User>> m = new HashMap<Operation, Set<User>>(1);
				Set<User> s = new HashSet<User>(1);
				s.add(newUser);
				m.put(Operation.Modify, s);
				node.updateUsers(m, null);
			}
		} else {
			log.debug(String.format("No such action %s", action));
		}
	}



	/**
	 * Get a copy of the user.
	 */
	private User getUser(HttpServletRequest req) {
		String userName = req.getParameter("user");
		if (userName == null) {
			String serverName = req.getServerName();
			userName = serverName.substring(0, serverName.indexOf('.'));
		}
		return node.getUser(userName);
	}
}
