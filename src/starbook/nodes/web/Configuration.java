package starbook.nodes.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.princehouse.mica.base.model.Runtime;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import starbook.common.CK;
import starbook.common.protocols.NodeDiscoveryProtocol;

public class Configuration extends starbook.common.Configuration implements ServletContextListener {
	private static final Logger log = Logger.getLogger(Configuration.class);
	public static Random rng = new Random();
	protected Runtime<NodeDiscoveryProtocol> runtime;
	protected WebNode node = null;
	protected Properties properties = new Properties();

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		log.info("Context destroyed.");
		if (node != null) node.stopThreads();
	}
	
	/**
	 * Initialize the configuration by reading the servlet context file.
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			ServletContext sc = sce.getServletContext();
			InputStream is = sc.getResourceAsStream("/META-INF/MANIFEST.MF");
			properties.load(is);
			log.info("WorkerNode "+properties.get("Build-Version")+" starting up...");
			log.debug("Build number "+properties.get("Build-Number")+" built on "+properties.get("Build-Timestamp")+" by "+properties.get("Build-User")+"@"+properties.get("Build-Host"));
			
			/*
			 * Create the S3 client.
			 */
			AmazonS3 s3Client = new AmazonS3Client(new PropertiesCredentials(sc.getResourceAsStream("/META-INF/AwsCredentials.properties")));
			Configuration.setParameter("s3", s3Client);
		} catch (IOException e) {
			e.printStackTrace();
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
		
		/*
		 * Start the worker node.
		 */
		try {
			node = new WebNode(InetAddress.getByName(Configuration.getStr(CK.WebIP)));
			Configuration.setParameter("node", node);
			node.start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
