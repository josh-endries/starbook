package starbook.common;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

/**
 * Periodically saves the provided object (by reference) to the specified file location.
 * 
 * @author Josh Endries (josh@endries.org)
 * 
 */
public class PeriodicFileWriter implements Runnable {
	private final static Logger log = Logger.getLogger(PeriodicFileWriter.class);
	private final Path filePath;
	private final Serializable object;
	public final static int DelaySeconds = 60;

	public PeriodicFileWriter(Serializable object, Path filePath) {
		this.object = object;
		this.filePath = filePath;
	}

	@Override
	public void run() {
		boolean running = true;
		while (running) {
			/*
			 * We sleep first to create an initial delay.
			 */
			try {
				Thread.sleep(DelaySeconds * 1000);
			} catch (InterruptedException e) {
				running = false;
				continue;
			}

			/*
			 * Attempt to save the object to disk.
			 */
			saveData();
		}

		/*
		 * We were interrupted, attempt to save one last time.
		 */
		saveData();
	}

	private void saveData() {
		log.debug("Saving data...");

		/*
		 * Determine the temporary file's name. First, start with the current file name (not path) and
		 * append a time-based suffix, e.g. if the original file was "file", this will result in
		 * "file-20120315173422".
		 */
		String dateSuffix = (new StringBuilder()).append('-').append(DateTime.now().toString("YYYYMMddHHmmss")).toString();
		String tempCurrentName = (new StringBuilder()).append(filePath.getFileName()).append("-new").append(dateSuffix).toString();
		String tempOriginalName = (new StringBuilder()).append(filePath.getFileName()).append("-old").append(dateSuffix).toString();

		/*
		 * "Resolve" the temporary file names with the current path. This, for a relative path name,
		 * effectively replaces the file name. E.g. for a path of "/tmp/foo", resolving with "bar"
		 * results in "/tmp/bar" because "bar" is relative.
		 */
		Path tempCurrentPath = filePath.resolveSibling(tempCurrentName);
		Path tempOriginalPath = filePath.resolveSibling(tempOriginalName);

		/*
		 * Make sure the temporary files don't exist.
		 */
		if (Files.exists(tempCurrentPath)) {
			log.warn(String.format("Temporary file \"%s\" already exists, unable to save data.", tempCurrentPath));
			return;
		}
		if (Files.exists(tempOriginalPath)) {
			log.warn(String.format("Temporary file \"%s\" already exists, unable to save data.", tempOriginalPath));
			return;
		}

		/*
		 * Attempt to write the object to the temporary file.
		 */
		OutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = Files.newOutputStream(tempCurrentPath);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(object);
			log.debug(String.format("Successfully wrote to \"%s\"", tempCurrentPath));

			/*
			 * Move the original file out of the way.
			 */
			try {
				Files.move(filePath, tempOriginalPath, StandardCopyOption.ATOMIC_MOVE);
			} catch (NoSuchFileException e) {
				/*
				 * It's okay if the original file doesn't exist; we are going to overwrite it anyway
				 * with the current object contents.
				 */
			}

			/*
			 * Move the temporary file to the original location. If this fails, we need to attempt to
			 * move the original file back to where it was.
			 */
			try {
				Files.move(tempCurrentPath, filePath, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				log.warn(String.format("Unable to move temporary file \"%s\" to original location \"%s\".", tempCurrentPath, filePath));

				/*
				 * Move the original file back.
				 */
				try {
					Files.move(tempOriginalPath, filePath, StandardCopyOption.ATOMIC_MOVE);
				} catch (NoSuchFileException e1) {
					/*
					 * Now we're really hosed.
					 */
					log.error(String.format("Unable to move original file \"%s\" back to original location \"%s\" after previous failure.",
							filePath.resolveSibling(tempOriginalPath), filePath));
					throw e;
				}

				throw e;
			}

			/*
			 * Remove the original file.
			 */
			try {
				Files.delete(tempOriginalPath);
			} catch (NoSuchFileException e) {
				/*
				 * It's okay if the original file doesn't exist; we overwrote it anyway with the current
				 * object contents.
				 */
			}
			
			log.info("Successfully saved data to disk.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (oos != null)
					oos.close();
				if (fos != null)
					fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
