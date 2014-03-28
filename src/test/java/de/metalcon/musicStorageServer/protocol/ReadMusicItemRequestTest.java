package de.metalcon.musicStorageServer.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import de.metalcon.musicStorageServer.MusicItemVersion;
import de.metalcon.musicStorageServer.protocol.read.ReadMusicItemRequest;
import de.metalcon.musicStorageServer.protocol.read.ReadResponse;
import de.metalcon.utils.formItemList.FormItemList;

public class ReadMusicItemRequestTest extends RequestTest {

    private static final MusicItemVersion VALID_MUSIC_ITEM_VERSION_ORIGINAL =
            MusicItemVersion.ORIGINAL;

    private static final MusicItemVersion VALID_MUSIC_ITEM_VERSION_BASIS =
            MusicItemVersion.BASIS;

    private static final MusicItemVersion VALID_MUSIC_ITEM_VERSION_STREAM =
            MusicItemVersion.STREAM;

    private static final String INVALID_MUSIC_ITEM_VERSION = "nuff";

    private ReadMusicItemRequest readRequest;

    private void fillRequest(
            final String musicItemIdentifier,
            final String musicItemVersion) {
        // create and fill form item list
        final FormItemList formItemList = new FormItemList();

        if (musicItemIdentifier != null) {
            formItemList.addField(
                    ProtocolConstants.Parameter.Read.MUSIC_ITEM_IDENTIFIER,
                    musicItemIdentifier);
        }
        if (musicItemVersion != null) {
            formItemList.addField(
                    ProtocolConstants.Parameter.Read.MUSIC_ITEM_VERSION,
                    musicItemVersion);
        }

        // check request and extract the response
        final ReadResponse readResponse = new ReadResponse();
        readRequest =
                ReadMusicItemRequest.checkRequest(formItemList, readResponse);
        extractJson(readResponse);
    }

    @Test
    public void testReadMusicItemRequest() {
        // original music file
        fillRequest(VALID_IDENTIFIER,
                VALID_MUSIC_ITEM_VERSION_ORIGINAL.toString());
        assertNotNull(readRequest);
        assertEquals(VALID_IDENTIFIER, readRequest.getMusicItemIdentifier());
        assertEquals(VALID_MUSIC_ITEM_VERSION_ORIGINAL,
                readRequest.getMusicItemVersion());

        // converted (basis version)
        fillRequest(VALID_IDENTIFIER, VALID_MUSIC_ITEM_VERSION_BASIS.toString());
        assertNotNull(readRequest);
        assertEquals(VALID_IDENTIFIER, readRequest.getMusicItemIdentifier());
        assertEquals(VALID_MUSIC_ITEM_VERSION_BASIS,
                readRequest.getMusicItemVersion());

        // converted (streaming version)
        fillRequest(VALID_IDENTIFIER,
                VALID_MUSIC_ITEM_VERSION_STREAM.toString());
        assertNotNull(readRequest);
        assertEquals(VALID_IDENTIFIER, readRequest.getMusicItemIdentifier());
        assertEquals(VALID_MUSIC_ITEM_VERSION_STREAM,
                readRequest.getMusicItemVersion());
    }

    @Test
    public void testMusicItemIdentifierMissing() {
        fillRequest(null, VALID_MUSIC_ITEM_VERSION_ORIGINAL.toString());
        checkForMissingParameterMessage(ProtocolConstants.Parameter.Read.MUSIC_ITEM_IDENTIFIER);
        assertNull(readRequest);
    }

    @Test
    public void testMusicItemVersionMissing() {
        fillRequest(VALID_IDENTIFIER, null);
        checkForMissingParameterMessage(ProtocolConstants.Parameter.Read.MUSIC_ITEM_VERSION);
        assertNull(readRequest);
    }

    @Test
    public void testMusicItemVersionInvalid() {
        fillRequest(VALID_IDENTIFIER, INVALID_MUSIC_ITEM_VERSION);
        checkForStatusMessage(ProtocolConstants.StatusMessage.Read.MUSIC_ITEM_VERSION_INVALID);
        assertNull(readRequest);
    }

}
