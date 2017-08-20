package starbook.nodes.web;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;

import starbook.common.BaseMessage;
import starbook.common.CK;
import starbook.common.Message;
import starbook.common.User;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@MultipartConfig
public class ImagesServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(ImagesServlet.class);
	private static final long serialVersionUID = 3816854113577854762L;
	private final WebNode node = (WebNode) Configuration.getParameter("node");

	/**
	 * Get a copy of the User object that this request represents. This could either be based on a
	 * URL parameter (which has priority) or the host name to which the request was submitted.
	 * 
	 * @param req The HTTP request.
	 * @return The User object associated with the user that submitted the request.
	 */
	private User getUser(HttpServletRequest req) {
		String userName = req.getParameter("user");
		if (userName == null) {
			String serverName = req.getServerName();
			userName = serverName.substring(0, serverName.indexOf('.'));
		}
		return node.getUser(userName);
	}
	
	/**
	 * Extract a file name from HTML form data. Taken from:
	 * 
	 * http://stackoverflow.com/questions/2422468/how-to-upload-files-in-jsp-servlet
	 * 
	 * @param part The multi-part HTML form part that contains the file.
	 * @return The file name or null if it wasn't found.
	 */
	private static String getFilename(Part part) {
	    for (String cd : part.getHeader("content-disposition").split(";")) {
	        if (cd.trim().startsWith("filename")) {
	            String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
	            return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1); // MSIE fix.
	        }
	    }
	    return null;
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		User user = getUser(request);

		Part filePart = request.getPart("file");
		if (filePart == null) {
			log.warn("Uploaded file part is null.");
		} else {
			String fileName = getFilename(filePart);
			InputStream fileContent = filePart.getInputStream();
			String contentType = filePart.getContentType();
			long contentLength = filePart.getSize();
			log.debug(String.format("File \"%s\" (%s bytes of %s) uploaded by %s", fileName, contentLength, contentType, user.getName()));

			/*
			 * Upload the file to S3.
			 */
			String bucketName = "starbook";
			AmazonS3 s3Client = (AmazonS3) Configuration.getParameter("s3");
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentType(contentType);
			metadata.setContentLength(contentLength);
			log.debug("Uploading to S3...");
			PutObjectRequest s3Request = new PutObjectRequest(bucketName, fileName, fileContent, metadata);
			s3Request = s3Request.withCannedAcl(CannedAccessControlList.PublicRead);
			s3Client.putObject(s3Request);

			/*
			 * Give S3 a little time to do its thing.
			 */
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				/*
				 * Do nothing and hope for the best.
				 */
			}

			/*
			 * Hopefully it worked...create a message to display it.
			 */
			String messageContent = String.format("<img src=\"%s%s\"/>", Configuration.getStr(CK.CloudFrontURL), fileName);
			Message message = new BaseMessage(messageContent, node.getInetAddress(), node.getNextID(), user.getName());

			/*
			 * Add the message to both message stores.
			 */
			log.debug(String.format("Adding new message from user %s: %s", user.getName(), message));
			node.getStoredMessageStore().addMessage(message);
			node.getPublishedMessageStore().addMessage(message);

			/*
			 * Redirect the user back to their page.
			 */
			response.sendRedirect(String.format("http://%s.starbook.l/view", user.getName()));
			return;
		}
	}
}
