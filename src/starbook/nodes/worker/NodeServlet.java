package starbook.nodes.worker;

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
		WorkerNode workerNode = (WorkerNode) Configuration.getParameter("node");
		req.setAttribute("address", workerNode.getInetAddress());
		req.setAttribute("messages", workerNode.getStoredMessageStore().getMessageGUIDs());
		req.setAttribute("subscribedTopics", workerNode.getSubscribedTopics());
		req.setAttribute("neighbors", workerNode.getNeighbors());
		req.getRequestDispatcher("node.jsp").forward(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		/*
		 * <input type="text" name="message"> comes in as message=value in the Map.
		 */
		log.debug("POST data: "+req.getParameterMap());
		resp.sendRedirect("/node");
	}
}
