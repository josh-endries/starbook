package starbook.common;

/**
 * Configuration keys. These are just "standardized shortcuts" to get to the String value, which is
 * what is actually used by the Configuration class.
 * 
 * @author Josh Endries <josh@endries.org>
 * 
 */
public final class CK {
	public static final String CloudFrontURL = "starbook.nodes.web.cloudFrontURL";
	public static final String CommandPort = "starbook.common.net.services.commands.port";
	public static final String DataDirectory = "starbook.common.dataDirectory";
	public static final String DownloadCount = "starbook.nodes.web.downloadCount";
	public static final String IndexBaseHost = "starbook.nodes.index.base_host";
	public static final String IndexBaseNet = "starbook.nodes.index.base_net";
	public static final String IndexOperator = "starbook.nodes.index.operator";
	public static final String IndexIP = "starbook.nodes.index.ip";
	public static final String IRPPort = "starbook.common.net.services.indexReplication.port";
	public static final String NodeDiscoveryPort = "starbook.common.net.services.nodeDiscovery.port";
	public static final String NDPCutoffSeconds = "starbook.common.cutoffSeconds";
	public static final String MessageDownloadPort = "starbook.common.net.services.messageDownload.port";
	public static final String MonitorIP = "starbook.monitor.ip";
	public static final String WorkerIP = "starbook.nodes.worker.ip";
	public static final String UDPPort = "starbook.common.net.services.userDiscovery.port";
	public static final String WebIP = "starbook.nodes.web.ip";
}
