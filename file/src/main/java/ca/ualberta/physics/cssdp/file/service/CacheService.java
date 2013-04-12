package ca.ualberta.physics.cssdp.file.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.BatchUpdateException;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.joda.time.LocalDateTime;

import ca.ualberta.physics.cssdp.domain.file.CachedFile;
import ca.ualberta.physics.cssdp.file.FileServer;
import ca.ualberta.physics.cssdp.file.dao.CachedFileDao;
import ca.ualberta.physics.cssdp.service.ManualTransaction;
import ca.ualberta.physics.cssdp.service.ServiceResponse;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;

public class CacheService {

	private final File cacheRoot = new File(FileServer.properties().getString(
			"file.cache.root"));

	@Inject
	private CachedFileDao cachedFileDao;

	@Inject
	private EntityManager em;

	public ServiceResponse<String> put(final String filename,
			final String externalKey, final InputStream fileData) {

		final ServiceResponse<String> sr = new ServiceResponse<String>();

		File tempDir = Files.createTempDir();
		final File tempFile = new File(tempDir, UUID.randomUUID().toString());
		FileOutputStream fos = null;
		try {
			Files.touch(tempFile);
			fos = new FileOutputStream(tempFile);
			ByteStreams.copy(fileData, fos);

		} catch (IOException e) {
			sr.error("Could not copy file data into temp file because "
					+ e.getMessage());

		} finally {
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				} catch (IOException ignore) {
				}
			}
		}

		final String md5 = getMD5(tempFile);
		new ManualTransaction(sr, em) {

			@Override
			public void onError(Exception e, ServiceResponse<?> sr) {
				Throwable t = Throwables.getRootCause(e);
				sr.error(((BatchUpdateException) t).getNextException()
						.getMessage());
			}

			@Override
			public void doInTransaction() {

				CachedFile existing = cachedFileDao.get(md5);
				if (existing != null) {
					if (existing.getExternalKeys().contains(externalKey)) {
						sr.info("File with signature " + md5
								+ " already in cache with key " + externalKey);
					} else {
						existing.getExternalKeys().add(externalKey);
						cachedFileDao.update(existing);
					}
				} else {

					StringBuffer path = new StringBuffer();
					for (String subdir : Splitter.fixedLength(4).split(md5)) {
						path.append("/" + subdir);
					}
					path.append("/");

					File cachePath = new File(cacheRoot, path.toString());
					cachePath.mkdirs();
					File cacheFile = new File(cachePath, ""
							+ (cachePath.list().length + 1));

					try {
						Files.touch(cacheFile);
						Files.copy(tempFile, cacheFile);
					} catch (IOException e) {
						sr.error("Could not copy temp file into cache because "
								+ e.getMessage());

					}

					// sanity check
					if (cacheFile.length() == 0) {
						cacheFile.delete();
						sr.error("Zero byte file, not caching.");
					}

					CachedFile cachedFile = new CachedFile(filename, md5,
							cacheFile);
					cachedFile.getExternalKeys().add(externalKey);

					cachedFileDao.save(cachedFile);

				}

			}
		};

		tempFile.delete();
		tempDir.delete();

		if (sr.isRequestOk()) {
			sr.setPayload(md5);
		}

		return sr;
	}

	public ServiceResponse<CachedFile> get(String md5) {

		ServiceResponse<CachedFile> sr = new ServiceResponse<CachedFile>();

		final CachedFile cachedFile = cachedFileDao.get(md5);
		if (cachedFile != null) {

			new ManualTransaction(sr, em) {

				@Override
				public void onError(Exception e, ServiceResponse<?> sr) {
					sr.error(e.getMessage());
				}

				@Override
				public void doInTransaction() {
					cachedFile.setLastAccessed(new LocalDateTime());
					cachedFileDao.update(cachedFile);
				}
			};
			if (cachedFile.exists()) {
				sr.setPayload(cachedFile);
			} else {
				sr.error("File cache is inconsistent! The actual file is missing.  "
						+ "Remove this MD5 from cache to clean up the inconsistent state.");
			}
		} else {
			sr.warn("No cached file found for MD5 " + md5);
		}

		return sr;
	}

	public ServiceResponse<CachedFile> remove(final String md5) {

		final ServiceResponse<CachedFile> sr = new ServiceResponse<CachedFile>();
		new ManualTransaction(sr, em) {

			@Override
			public void doInTransaction() {
				CachedFile cachedFile = cachedFileDao.get(md5);
				if (cachedFile != null) {
					cachedFileDao.delete(cachedFile);
					cachedFile.getFile().delete();
					sr.setPayload(cachedFile);
				}
			}

			@Override
			public void onError(Exception e, ServiceResponse<?> sr) {
				sr.error("Cache removal failed due to " + e.getMessage());
			}

		};
		return sr;
	}

	private String getMD5(File file) {

		try {

			HashCode hashcode = Files.hash(file, Hashing.md5());

			String md5HexString = hashcode.toString();
			return md5HexString;

		} catch (IOException e) {
			throw Throwables.propagate(e);
		}
	}

	public ServiceResponse<CachedFile> find(String key) {
		return new ServiceResponse<CachedFile>(cachedFileDao.find(key));
	}

	public ServiceResponse<CachedFile> updateKeys(final String md5,
			final String key) {

		final ServiceResponse<CachedFile> sr = new ServiceResponse<CachedFile>();
		new ManualTransaction(sr, em) {

			@Override
			public void doInTransaction() {
				CachedFile cachedFile = cachedFileDao.get(md5);
				if (cachedFile != null) {
					cachedFile.getExternalKeys().add(key);
					cachedFileDao.update(cachedFile);
					sr.setPayload(cachedFile);
				}
			}

			@Override
			public void onError(Exception e, ServiceResponse<?> sr) {
				sr.error("Update failed, key not added to cached file entry: "
						+ e.getMessage());
			}

		};
		return sr;

	}

	// @Transactional
	// public void deleteHostEntry(HostEntry hostEntry) {
	// if (hostEntryDao.exists(hostEntry.getHostname())) {
	// hostEntryDao.delete(hostEntry);
	// }
	// }
	//
	// public HostEntry getHostEntry(String hostname) {
	// return hostEntryDao.find(hostname);
	// }

	// @Transactional
	// public void saveHostEntry(HostEntry hostEntry) {
	// if (!hostEntryDao.exists(hostEntry.getHostname())) {
	// hostEntryDao.save(hostEntry);
	// }
	// }
}