package de.metalcon.musicStorageServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.metalcon.musicStorageServer.converting.Avconv;
import de.metalcon.musicStorageServer.converting.AvconvResponse;
import de.metalcon.musicStorageServer.converting.exceptions.ConverterExecutionException;
import de.metalcon.musicStorageServer.converting.exceptions.ConvertingFailedException;
import de.metalcon.musicStorageServer.protocol.create.CreateResponse;
import de.metalcon.musicStorageServer.protocol.delete.DeleteResponse;
import de.metalcon.musicStorageServer.protocol.read.ReadResponse;
import de.metalcon.musicStorageServer.protocol.update.UpdateResponse;

public class MusicStorageServer implements MusicStorageServerAPI {

	/**
	 * date formatter
	 */
	private static final Format FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * JSON parser
	 */
	private static final JSONParser PARSER = new JSONParser();

	/**
	 * program name used for music conversion
	 */
	private static final String CONVERTER_PROGRAM = "avconv";

	/**
	 * year represented by the current formatted year
	 */
	private static int YEAR;

	/**
	 * day of the year represented by the current formatted day
	 */
	private static int DAY;

	/**
	 * formatted year
	 */
	private static String FORMATTED_YEAR;

	/**
	 * formatted day
	 */
	private static String FORMATTED_DAY;

	/**
	 * root directory for the music storage server
	 */
	private final String musicDirectory;

	/**
	 * temporary directory for resampling
	 */
	private final String temporaryDirectory;

	/**
	 * quality of the basis files (in percent)
	 */
	private final int basisQuality;

	/**
	 * sample rate for stream files (in kbit/s)
	 */
	private final int sampleRateStreaming;

	/**
	 * database for music item meta data
	 */
	private final MusicMetaDatabase musicMetaDatabase;

	/**
	 * server running flag
	 */
	private boolean running;

	/**
	 * update the current date strings
	 */
	private static void updateDateLabels() {
		final Calendar calendar = Calendar.getInstance();
		final int day = calendar.get(Calendar.DAY_OF_YEAR);
		final int year = calendar.get(Calendar.YEAR);

		if ((day != DAY) || (year != YEAR)) {
			YEAR = year;
			DAY = day;

			FORMATTED_YEAR = String.valueOf(year);
			FORMATTED_DAY = FORMATTER.format(calendar.getTime());
		}
	}

	/**
	 * generate the has value for a key
	 * 
	 * @param key
	 *            object to generate the hash for
	 * @return hash for the key passed
	 */
	private static String generateHash(final String key) {
		return String.valueOf(key.hashCode());
	}

	/**
	 * generate the relative directory path for a music item
	 * 
	 * @param hash
	 *            music item identifier hash
	 * @param depth
	 *            number of sub directories
	 * @return relative directory path using hashes
	 */
	private static String getRelativeDirectory(final String hash,
			final int depth) {
		int pathLength = 0;
		for (int i = 0; i < depth; i++) {
			pathLength += i + 1;
		}

		final StringBuilder path = new StringBuilder(pathLength);
		for (int i = 0; i < depth; i++) {
			path.append(hash.substring(0, i + 1));
			if (i != (depth - 1)) {
				path.append(File.separator);
			}
		}

		return path.toString();
	}

	private static void storeBasisMusicItem(final File sourceFile,
			final File destinationFile, final int quality)
			throws ConverterExecutionException, ConvertingFailedException {
		// create the parental directories
		final File musicItemFileDir = destinationFile.getParentFile();
		if (musicItemFileDir != null) {
			musicItemFileDir.mkdirs();
		}

		final Avconv converter = new Avconv(sourceFile.getAbsolutePath(),
				destinationFile.getAbsolutePath());
		converter.setQuality(quality);

		try {
			final AvconvResponse response = converter.execute();
			if (!response.succeeded()) {
				throw new ConvertingFailedException(response.getErrorMessage());
			}

		} catch (final IOException e) {
			throw new ConverterExecutionException("failed to call ffmpeg");
		}
	}

	private static void storeStreamingMusicItem(final File sourceFile,
			final File destinationFile) {
		// create the parental directories
		final File musicItemFileDir = destinationFile.getParentFile();
		if (musicItemFileDir != null) {
			musicItemFileDir.mkdirs();
		}

		// TODO
	}

	/**
	 * delete the content of a directory (clear)
	 * 
	 * @param directory
	 *            directory that shall be cleared
	 * @param removeRoot
	 *            if set the directory passed will be removed too
	 */
	private static void deleteDirectoryContent(final File directory,
			final boolean removeRoot) {
		final File[] content = directory.listFiles();
		if (content != null) {
			for (File contentItem : content) {
				if (contentItem.isDirectory()) {
					deleteDirectoryContent(contentItem, true);
				} else {
					contentItem.delete();
				}
			}
		}
		if (removeRoot) {
			directory.delete();
		}
	}

	/**
	 * create a new music storage server
	 * 
	 * @param configPath
	 *            path to the configuration file
	 */
	public MusicStorageServer(final String configPath) {
		final MSSConfig config = new MSSConfig(configPath);
		MusicMetaDatabase musicMetaDatabase = null;
		this.musicDirectory = config.getMusicDirectory();
		this.temporaryDirectory = config.getTemporaryDirectory();
		this.basisQuality = config.getBasisQuality();
		this.sampleRateStreaming = config.getSampleRateStreaming();

		try {
			musicMetaDatabase = new MusicMetaDatabase(config.getDatabaseHost(),
					config.getDatabasePort(), config.getDatabaseName());
			this.running = true;
		} catch (final UnknownHostException e) {
			System.err
					.println("failed to connect to the mongoDB server at "
							+ config.getDatabaseHost() + ":"
							+ config.getDatabaseName());
			e.printStackTrace();
		}

		this.musicMetaDatabase = musicMetaDatabase;
	}

	/**
	 * store a stream to a file
	 * 
	 * @param inputStream
	 *            stream to be stored
	 * @param destinationFile
	 *            destination file to write the stream to
	 * @throws IOException
	 *             if the writing failed
	 */
	private static void storeToFile(final InputStream inputStream,
			final File destinationFile) throws IOException {
		final File parentalDirectory = destinationFile.getParentFile();
		if (parentalDirectory != null) {
			parentalDirectory.mkdirs();
		}

		final OutputStream fileOutputStream = new FileOutputStream(
				destinationFile);
		IOUtils.copy(inputStream, fileOutputStream);
	}

	@Override
	public boolean createMusicItem(final String musicItemIdentifier,
			final InputStream musicItemStream, final String metaData,
			final CreateResponse response) {
		if (!this.musicMetaDatabase.hasEntryWithIdentifier(musicItemIdentifier)) {
			final String hash = generateHash(musicItemIdentifier);
			JSONObject metaDataJSON = null;
			if (metaData != null) {
				try {
					metaDataJSON = (JSONObject) PARSER.parse(metaData);
				} catch (final ParseException e) {
					// TODO error: meta data format invalid
					return false;
				}
			}

			try {
				final File tmpFile = this.storeConvertedMusicItems(hash,
						musicItemStream);
				if (tmpFile != null) {
					// store original music item
					storeToFile(new FileInputStream(tmpFile),
							this.getOriginalFile(hash));

					// create music item in database
					this.musicMetaDatabase.addDatabaseEntry(
							musicItemIdentifier, metaDataJSON);

					return true;
				} else {
					// TODO: hash collision
				}
			} catch (final ConvertingFailedException e) {
				// TODO error: invalid music item stream
				e.printStackTrace();
			} catch (final ConverterExecutionException e) {
				// TODO internal server error
				e.printStackTrace();
			} catch (final IOException e) {
				// TODO internal server error
			}
		} else {
			// TODO error: music item identifier in use
		}

		return false;
	}

	@Override
	public MusicData readMusicItem(final String musicItemIdentifier,
			final ReadResponse response) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] readMusicItemMetaData(final String[] musicItemIdentifiers,
			final ReadResponse response) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateMetaData(final String musicItemIdentifier,
			final String metaData, final UpdateResponse response) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteMusicItem(final String musicItemIdentifier,
			final DeleteResponse response) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * generate the file handle to the original version of a music item
	 * 
	 * @param hash
	 *            music item identifier hash
	 * @return file handle to the original version of the music item passed
	 */
	private File getOriginalFile(final String hash) {
		updateDateLabels();
		return new File(this.musicDirectory + "originals" + File.separator
				+ FORMATTED_YEAR + File.separator + FORMATTED_DAY
				+ File.separator + getRelativeDirectory(hash, 2), hash);
	}

	/**
	 * generate the file handle to the basis version of a music item
	 * 
	 * @param hash
	 *            music item identifier hash
	 * @return file handle to the basis version of the music item passed
	 */
	private File getBasisFile(final String hash) {
		return new File(this.musicDirectory + getRelativeDirectory(hash, 3)
				+ File.separator + "basis", hash + ".ogg");
	}

	/**
	 * generate the file handle to the temporary version of a music item
	 * 
	 * @param hash
	 *            music item identifier hash
	 * @return file handle to the temporary version of the music item passed
	 */
	private File getTemporaryFile(final String hash) {
		return new File(this.temporaryDirectory, hash);
	}

	/**
	 * generate the file handle to the streaming version of a music item
	 * 
	 * @param hash
	 *            music item identifier hash
	 * @return file handle to the streaming version of the music item passed
	 */
	private File getStreamingFile(final String hash) {
		return new File(this.musicDirectory + getRelativeDirectory(hash, 3)
				+ File.separator + "streaming", hash + ".ogg");
	}

	/**
	 * store converted versions of a music item
	 * 
	 * @param hash
	 *            music item identifier hash
	 * @param musicItemStream
	 *            music item input stream
	 * @return handle to a copy of the original music file<br>
	 *         <b>null</b> if there was a collision between music files
	 * @throws IOException
	 *             failed to store one of the music file versions
	 * @throws ConverterExecutionException
	 *             if the converting failed due to internal errors
	 * @throws ConvertingFailedException
	 *             if the converting process failed
	 */
	private File storeConvertedMusicItems(final String hash,
			final InputStream musicItemStream) throws IOException,
			ConverterExecutionException, ConvertingFailedException {
		// store a copy of the original music file
		final File tmpFile = this.getTemporaryFile(hash);

		if (!tmpFile.exists()) {
			storeToFile(musicItemStream, tmpFile);

			try {
				// try to create converted files
				final File basisFile = this.getBasisFile(hash);
				if (!basisFile.exists()) {
					storeBasisMusicItem(tmpFile, basisFile, this.basisQuality);
					storeStreamingMusicItem(tmpFile,
							this.getStreamingFile(hash));

					return tmpFile;
				}
			} finally {

			}
		}

		// delete copy of the (music) file
		tmpFile.delete();

		// collision between music item files
		return null;
	}

	/**
	 * @return true - if the server is running<br>
	 *         false - otherwise
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * clear the server<br>
	 * <b>removes all images and meta data</b>
	 */
	public void clear() {
		deleteDirectoryContent(new File(this.musicDirectory), false);
		deleteDirectoryContent(new File(this.temporaryDirectory), false);
		this.musicMetaDatabase.clear();
	}

}