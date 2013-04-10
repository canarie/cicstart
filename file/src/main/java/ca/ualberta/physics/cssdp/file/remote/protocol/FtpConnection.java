package ca.ualberta.physics.cssdp.file.remote.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ualberta.physics.cssdp.domain.file.Host;
import ca.ualberta.physics.cssdp.domain.file.RemoteFile;
import ca.ualberta.physics.cssdp.util.UrlParser;

public class FtpConnection extends RemoteConnection {

	private static final Logger logger = LoggerFactory
			.getLogger(FtpConnection.class);

	protected FTPClient ftpClient = new FTPClient();

	public FtpConnection(Host hostEntry) {
		super(hostEntry);

		// give us more detail about what's happening for debugging purposes.
		if (logger.isDebugEnabled()) {
			ftpClient.addProtocolCommandListener(new PrintCommandListener(
					new PrintWriter(System.out)));
		}
	}

	@Override
	public boolean connect() {

		Host hostEntry = getHostEntry();
		ftpClient.setConnectTimeout(hostEntry.getTimeout());
		ftpClient.setDataTimeout(hostEntry.getTimeout());
		ftpClient.setDefaultTimeout(hostEntry.getTimeout());
		String hostname = hostEntry.getHostname();
		try {
			ftpClient.connect(hostname);
			boolean success = ftpClient.login(hostEntry.getUsername(),
					hostEntry.getPassword());
			if (!success) {
				throw new ProtocolException("Unable to login to FTP server "
						+ hostEntry.getHostname(), false);
			}
			if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
				// assume files are binary
				throw new ProtocolException(
						"Cannot set FTP connection to binary mode.", false);
			}
			return success;

		} catch (SocketTimeoutException timedOut) {
			throw new ProtocolException("Timed out connecting by ftp to "
					+ hostname, true);
		} catch (Exception e) {
			throw new ProtocolException("Failed connecting by ftp to "
					+ hostname, false, e);
		}

	}

	@Override
	public boolean disconnect() {

		try {
			ftpClient.disconnect();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isConnected() {
		return ftpClient.isConnected();
	}

	@Override
	public List<RemoteFile> ls(String path) {

		logger.debug("Listing files at " + path);

		try {
			ftpClient.enterLocalPassiveMode();
			FTPFile[] files = ftpClient.listFiles(path);

			if (files == null || files.length == 0) {
				return new ArrayList<RemoteFile>();
			}

			List<RemoteFile> list = new ArrayList<RemoteFile>();
			for (FTPFile file : files) {

				String name = file.getName();
				long size = file.getSize();
				boolean isDir = file.isDirectory();
				LocalDateTime modifiedTstamp = new LocalDateTime(
						file.getTimestamp());

				RemoteFile remoteFile = new RemoteFile("ftp://"
						+ getHostEntry().getHostname() + path + name, size,
						modifiedTstamp, isDir);
				list.add(remoteFile);
			}

			return list;

		} catch (SocketTimeoutException timeout) {
			throw new ProtocolException("Timedout listing " + path, true,
					timeout);

		} catch (IOException e) {
			throw new ProtocolException("Could not get file listing for "
					+ path, false, e);
		}

	}

	@Override
	public InputStream download(String url) throws IOException {

		ftpClient.enterLocalPassiveMode();
		InputStream is = ftpClient.retrieveFileStream(UrlParser.getPath(url));
		return is;
	}

	public void nudge() throws IOException {
		ftpClient.completePendingCommand();
	}
}
