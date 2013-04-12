package ca.ualberta.physics.cssdp.file.remote.command;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.ualberta.physics.cssdp.domain.file.Host.Protocol;
import ca.ualberta.physics.cssdp.domain.file.RemoteFile;
import ca.ualberta.physics.cssdp.file.remote.protocol.RemoteConnection;
import ca.ualberta.physics.cssdp.util.UrlParser;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;

public class RecursiveLs extends RemoteServerCommand<List<RemoteFile>> {

	private static final Logger logger = LoggerFactory
			.getLogger(RecursiveLs.class);

	private final String rootDir;
	private final int maxDepth;

	private final List<RemoteFile> fileList = new ArrayList<RemoteFile>();

	public RecursiveLs(String hostname, String rootDir, Integer maxDepth) {
		super(hostname);
		if (Strings.isNullOrEmpty(rootDir)) {
			throw new IllegalArgumentException(
					"Must supply a root dir to start listing at");
		}
		this.rootDir = rootDir;
		if (maxDepth != null && maxDepth > 250) {
			error("Max Depth must be <= 250");
			this.maxDepth = 0;
		} else {
			this.maxDepth = maxDepth == null ? 0 : maxDepth;
		}
	}

	@Override
	public void execute(RemoteConnection connection) {

		if (!hasError()) {
			try {
				connection.connect();
				String rootUrl = connection.getHostEntry().getProtocol().name()
						+ "://"
						+ (connection.getHostEntry().getProtocol()
								.equals(Protocol.file) ? "" : connection
								.getHostEntry().getHostname()) + rootDir;

				RemoteFile root = new RemoteFile(rootUrl, 0,
						new LocalDateTime(), true);

				fileList.addAll(visit(connection, root));

			} catch (Exception e) {
				logger.error("Recursive LS failed ", e);
				error(Throwables.getRootCause(e).getMessage());
			} finally {
				connection.disconnect();
				setDone(true);
			}

		}

	}

	private int currentDepth = 0;

	private List<RemoteFile> visit(RemoteConnection connection,
			RemoteFile remoteFile) {
		currentDepth++;
		String url = remoteFile.getUrl();
		String path = UrlParser.getPath(url);
		List<RemoteFile> contents = connection.ls(path);
		List<RemoteFile> files = new ArrayList<RemoteFile>();
		for (RemoteFile file : contents) {
			if (file.isDir()) {
				if (currentDepth <= maxDepth) {
					files.addAll(visit(connection, file));
					currentDepth--;
				}
			} else {
				files.add(file);
			}
		}
		return files;
	}

	@Override
	public List<RemoteFile> getResult() {
		return fileList;
	}
}