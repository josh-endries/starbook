package starbook.nodes.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import starbook.common.BaseMessage;
import starbook.common.Message;
import starbook.common.User;
import starbook.common.Util;

public class MessagesServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(MessagesServlet.class);
	private static final long serialVersionUID = 3816854113577854762L;
	private final WebNode node = (WebNode) Configuration.getParameter("node");



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



	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		/*
		 * First, retrieve the user.
		 */
		User user = getUser(req);

		/*
		 * By default, we retrieve messages from the previous week.
		 */
//		DateTime startDate = DateTime.now().minusDays(7);

		/*
		 * Check the request to see if there is a different start date.
		 */
//		String s = req.getParameter("days");
//		if (s != null) {
//			startDate = DateTime.now().minusDays(Integer.valueOf(s));
//		}

		/*
		 * Retrieve a list of topics in which this user is interested.
		 */
		Set<String> subscriptions = user.getSubscriptions();
		req.setAttribute("subscriptions", subscriptions);

		/*
		 * First, create a sorted list of the messages so they are output in order by date.
		 */
		TreeSet<Message> messages = new TreeSet<Message>(Collections.reverseOrder());
		for (String subscription : subscriptions) {
			messages.addAll(node.getStoredMessageStore().getMessagesByTopic(subscription));
		}

		/*
		 * Create a copy of sorted the list of messages that match topics to which this user is
		 * interested.
		 * 
		 * TODO: Implement the "days" feature.
		 */
		ArrayList<JSONObject> messageList = new ArrayList<JSONObject>();
		for (Message m: messages) {
			/*
			 * Turn the message into a JSON object.
			 */
			JSONObject o = Util.toJSON(m);

			/*
			 * We don't need/want to return the source or ID to the user.
			 */
			o.remove("s");
			o.remove("i");

			/*
			 * Add the sanitized JSON object to the list.
			 */
			messageList.add(o);
		}

		/*
		 * Create a JSON array from the list and write it out.
		 */
		JSONArray jsonArray = new JSONArray(messageList);
		resp.getWriter().write(jsonArray.toString());
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		User user = getUser(req);

		String content = req.getParameter("content");
		if (content != null && content.length() > 0) {
			Message message = new BaseMessage(content, node.getInetAddress(), node.getNextID(), user.getName());

			/*
			 * Add the message to both message stores.
			 */
			log.debug(String.format("Adding new message from user %s: %s", user.getName(), message));
			node.getStoredMessageStore().addMessage(message);
			node.getPublishedMessageStore().addMessage(message);
		}
	}
}
