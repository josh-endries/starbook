package starbook.tests;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;

import starbook.common.BaseMessage;
import starbook.common.Message;

public class JSONTest {
	protected static final Logger root = Logger.getRootLogger();
	
	static {
		root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
	}
	
	public static void main(String[] args) throws UnknownHostException, JSONException {
		ArrayList<Message> messageList = new ArrayList<Message>();
		messageList.add(new BaseMessage("content1", DateTime.now().minusDays(1), InetAddress.getByName("localhost"), 1, "topic1"));
		messageList.add(new BaseMessage("content2", DateTime.now().minusDays(2), InetAddress.getByName("localhost"), 2, "topic2"));
		messageList.add(new BaseMessage("content3", DateTime.now().minusDays(3), InetAddress.getByName("localhost"), 3, "topic3"));

		HashSet<String> strings = new HashSet<String>();
		for (Message m: messageList) {
			strings.add(m.getContent());
		}
		JSONArray ja = new JSONArray(strings);
		System.out.println(ja);
	}
}
