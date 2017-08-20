package starbook.nodes.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class NodeServlet extends HttpServlet {
	private static final long serialVersionUID = -7873859804416584322L;
	private static final Logger log = Logger.getLogger(NodeServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("");
		WebNode node = (WebNode) Configuration.getParameter("node");
		req.setAttribute("address", node.getInetAddress());
		req.setAttribute("publishedMessages", node.getPublishedMessageStore().getMessageGUIDs());
		req.setAttribute("storedMessages", node.getStoredMessageStore().getMessageGUIDs());
		req.setAttribute("subscribedTopics", node.getSubscribedTopics());
		req.setAttribute("users", node.getUsers());
		req.setAttribute("neighbors", node.getNeighbors());
		req.getRequestDispatcher("node.jsp").forward(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		/*
		 * <input type="text" name="message"> comes in as message=value in the Map.
		 */
		log.debug("POST data: "+req.getParameterMap());
		resp.sendRedirect("/view");
	}
}
