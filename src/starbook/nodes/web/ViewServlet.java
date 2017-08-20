package starbook.nodes.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import starbook.common.CK;
import starbook.common.User;

/**
 * The ViewServlet creates a web page for a user that displays any messages this node has collected
 * regarding topics to which the user is subscribed.
 * 
 * @author Josh Endries <josh@endries.org>
 */
public class ViewServlet extends HttpServlet {
	private static final long serialVersionUID = -7873859804416584322L;
	private static final Logger log = Logger.getLogger(ViewServlet.class);
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
		req.setAttribute("address", node.getInetAddress());
		User user = getUser(req);
		log.debug("Found user: "+user);

		/*
		 * TODO: Change this to use the IP of the leader.
		 */
		if (user == null) {
			resp.sendRedirect(String.format("//%s/sign-up", Configuration.getStr(CK.IndexIP)));
			return;
		}

		req.setAttribute("user", user);
		req.getRequestDispatcher("view.jsp").forward(req, resp);
	}
}
