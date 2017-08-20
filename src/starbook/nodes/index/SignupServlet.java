package starbook.nodes.index;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.net.tcpip.TCPAddress;

import starbook.common.BaseNode;
import starbook.common.BaseUser;
import starbook.common.CK;
import starbook.common.Command;
import starbook.common.Command.Type;
import starbook.common.NodeInfo;
import starbook.common.User;

public class SignupServlet extends HttpServlet {
	private static final long serialVersionUID = 4321665739456183945L;
	private static final Logger log = Logger.getLogger(SignupServlet.class);
	private final IndexNode node = (IndexNode) Configuration.getParameter("node");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		log.debug("");
		req.getRequestDispatcher("sign-up.jsp").forward(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("");
		try {
			String name = req.getParameter("name");
			if (name == null) throw new IllegalArgumentException("Name parameter does not exist.");
			if (name.length() < 1) throw new IllegalArgumentException("Name parameter is too short.");
			if (!name.matches("[a-zA-Z0-9-]+")) throw new IllegalArgumentException("Only alphanumeric characters allowed.");
			
			name = name.toLowerCase();
			log.debug("New user attempt: " + req.getParameter("name"));
			
			/*
			 * Create the User object for this user and add themselves as a
			 * subscription (so they see their own posts).
			 */
			User user = new BaseUser(name);
			
			/*
			 * See if this user already exists.
			 */
			if (node.getUserAssociations().containsKey(user)) {
				log.debug("Redirecting existing user: "+user);
				
				/*
				 * The user already exists; redirect them to their page.
				 */
				resp.sendRedirect(String.format("http://%s.starbook.l/view", name));
				return;
			} else {
				log.debug("Creating new user: "+user);

				/*
				 * Add the user and associate it with a random node. We use a CreateUser command here
				 * because it cannot be refused, so the user will have somewhere to go. An AddUser
				 * command checks to see of the receiving node has enough resources to replicate a new
				 * user, but a CreateUser command does not.
				 */
				TCPAddress userNode = node.getRandomNeighbor(false, BaseNode.Type.Web);
				HashMap<String, Object> data = new HashMap<String, Object>(2);
				data.put("source", node.getInetAddress());
				data.put("node", userNode.getInetAddressAddress());
				data.put("user", user);
				Command c = new Command(Type.CreateUser, data);
				c.sendViaUDP(new InetSocketAddress(userNode.getInetAddressAddress(), Configuration.getInt(CK.CommandPort)));
				node.associateUser(user, userNode.getInetAddressAddress());
				
				/*
				 * Replicate the user at the other index nodes.
				 */
				Iterator<Map.Entry<TCPAddress, NodeInfo>> i = node.getNeighbors().entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<TCPAddress, NodeInfo> e = i.next();
					if (e.getValue().getType().equals(BaseNode.Type.Index) && !e.getKey().equals(node.getInetAddress())) {
						c.sendViaUDP(new InetSocketAddress(e.getKey().getInetAddressAddress(), Configuration.getInt(CK.CommandPort)));
					}
				}
				
				/*
				 * Redirect the user to the node after waiting for the above UDP command to process.
				 * 
				 * TODO: Send them to a welcome page?
				 */
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				resp.sendRedirect(String.format("http://%s.starbook.l/view", name));
				return;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		req.getRequestDispatcher("sign-up.jsp").forward(req, resp);
	}
}
