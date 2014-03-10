package de.metalcon.musicStorageServer.protocol.create;

import java.io.IOException;
import java.io.InputStream;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.metalcon.musicStorageServer.protocol.ProtocolConstants;
import de.metalcon.utils.FormFile;
import de.metalcon.utils.FormItemList;

/**
 * basic create request object
 * 
 * @author sebschlicht
 * 
 */
public class CreateRequest {

	/**
	 * JSON parser
	 */
	protected static final JSONParser PARSER = new JSONParser();

	/**
	 * music item identifier
	 */
	private final String musicItemIdentifier;

	/**
	 * music item stream
	 */
	private final InputStream imageStream;

	/**
	 * meta data to be stored
	 */
	private final String metaData;

	/**
	 * create a new create request object
	 * 
	 * @param musicItemIdentifier
	 *            music item identifier
	 * @param imageStream
	 *            music item stream
	 * @param metaData
	 *            meta data to be stored
	 */
	public CreateRequest(final String musicItemIdentifier,
			final InputStream imageStream, final String metaData) {
		this.musicItemIdentifier = musicItemIdentifier;
		this.imageStream = imageStream;
		this.metaData = metaData;
	}

	/**
	 * @return music item identifier
	 */
	public String getMusicItemIdentifier() {
		return this.musicItemIdentifier;
	}

	/**
	 * @return music item stream
	 */
	public InputStream getImageStream() {
		return this.imageStream;
	}

	/**
	 * @return meta data to be stored
	 */
	public String getMetaData() {
		return this.metaData;
	}

	/**
	 * check a create request for validity concerning MSSP
	 * 
	 * @param request
	 *            form item list extracted from the request
	 * @param response
	 *            create response object to add status messages to
	 * @return create request object<br>
	 *         <b>null</b> if the request is invalid
	 */
	public static CreateRequest checkRequest(final FormItemList request,
			final CreateResponse response) {
		final String musicItemIdentifier = checkMusicItemIdentifier(request,
				response);
		if (musicItemIdentifier != null) {
			final InputStream musicItemStream = checkMusicItem(request,
					response);
			if (musicItemStream != null) {
				final String metaData = checkMetaData(request, response);
				if (metaData != null) {
					return new CreateRequest(musicItemIdentifier,
							musicItemStream, metaData);
				}
			}
		}

		return null;
	}

	/**
	 * check if the request contains a music item identifier
	 * 
	 * @param formItemList
	 *            form item list extracted
	 * @param response
	 *            create response object
	 * @return music item identifier<br>
	 *         <b>null</b> if the music item identifier is missing
	 */
	protected static String checkMusicItemIdentifier(
			final FormItemList formItemList, final CreateResponse response) {
		try {
			return formItemList
					.getField(ProtocolConstants.Parameter.Create.MUSIC_ITEM_IDENTIFIER);
		} catch (final IllegalArgumentException e) {
			response.musicItemIdentifierMissing();
		}

		return null;
	}

	/**
	 * check if the request contains a music item
	 * 
	 * @param formItemList
	 *            form item list extracted
	 * @param response
	 *            create response object
	 * @return input stream of the music item<br>
	 *         <b>null</b> if the music item is missing
	 */
	protected static InputStream checkMusicItem(
			final FormItemList formItemList, final CreateResponse response) {
		try {
			final FormFile musicItem = formItemList
					.getFile(ProtocolConstants.Parameter.Create.MUSIC_ITEM);
			if (musicItem != null) {
				return musicItem.getFormItem().getInputStream();
			}
		} catch (final IllegalArgumentException e) {
			response.musicItemMissing();
		} catch (final IOException e) {
			response.internalServerError();
		}

		return null;
	}

	/**
	 * check if the request contains valid meta data
	 * 
	 * @param formItemList
	 *            form item list extracted
	 * @param response
	 *            create response object
	 * @return valid meta data<br>
	 *         <b>null</b> if the meta data is missing or malformed
	 */
	protected static String checkMetaData(final FormItemList formItemList,
			final CreateResponse response) {
		try {
			final String metaData = formItemList
					.getField(ProtocolConstants.Parameter.Create.META_DATA);
			PARSER.parse(metaData);
			return metaData;
		} catch (final IllegalArgumentException e) {
			response.metaDataMissing();
		} catch (final ParseException e) {
			response.metaDataMalformed();
		}

		return null;
	}
}