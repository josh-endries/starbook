package starbook.nodes.index;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import starbook.common.CK;

public class Configuration extends starbook.common.Configuration implements ServletContextListener {
	private static final Logger log = Logger.getLogger(Configuration.class);
	protected static Collection<Thread> threads = new ArrayList<Thread>();
	protected IndexNode indexNode = null;
	protected Properties properties = new Properties();

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("Context destroyed.");
		if (indexNode != null)
			indexNode.stopThreads();
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		InputStream is = null;
		try {
			ServletContext sc = sce.getServletContext();
			is = sc.getResourceAsStream("/META-INF/MANIFEST.MF");
			properties.load(is);
			log.info("IndexNode "+properties.get("Build-Version")+" starting up...");
			log.debug("Build number "+properties.get("Build-Number")+" built on "+properties.get("Build-Timestamp")+" by "+properties.get("Build-User")+"@"+properties.get("Build-Host"));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/*
		 * Load the configuration from the context file.
		 */
		Map<String, Object> m = new HashMap<String, Object>();
		Enumeration<String> parameterNames = sce.getServletContext().getInitParameterNames();
		while (parameterNames.hasMoreElements()) {
			String parameterName = parameterNames.nextElement();
			String parameterValue = sce.getServletContext().getInitParameter(parameterName);
			m.put(parameterName, parameterValue);
		}
		loadParameters(m);
		
		log.debug("Configuration parameters: "+m);

		/*
		 * Start the index node.
		 */
		try {
			InetAddress address = InetAddress.getByName(Configuration.getStr(CK.IndexIP));
			indexNode = new IndexNode(address);
			Configuration.setParameter("node", indexNode);
			indexNode.start();
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}