package starbook.nodes.index;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import starbook.common.User;

public class NodeServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(NodeServlet.class);
	private static final long serialVersionUID = -6749726873844504517L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("");
		IndexNode node = (IndexNode) Configuration.getParameter("node");
		ConcurrentHashMap<User, ConcurrentSkipListSet<InetAddress>> users = node.getUserAssociations();
		req.setAttribute("address", node.getInetAddress());
		req.setAttribute("leader", node.getInetAddress().equals(node.getLeaderAddress()));
		req.setAttribute("users", users);
		req.setAttribute("neighbors", node.getNeighbors());
		req.getRequestDispatcher("node.jsp").forward(req, resp);
	}
}
