package de.metalcon.musicStorageServer.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import de.metalcon.musicStorageServer.protocol.read.ReadMetaDataRequest;
import de.metalcon.musicStorageServer.protocol.read.ReadResponse;
import de.metalcon.utils.formItemList.FormItemList;

public class ReadMetaDataRequestTest extends RequestTest {

    private static final String VALID_MUSIC_ITEM_IDENTIFIERS = ","
            + VALID_IDENTIFIER + "," + VALID_IDENTIFIER;

    private ReadMetaDataRequest readRequest;

    private void fillRequest(final String musicItemIdentifiers) {
        // create and fill form item list
        final FormItemList formItemList = new FormItemList();

        if (musicItemIdentifiers != null) {
            formItemList.addField(
                    ProtocolConstants.Parameter.Read.MUSIC_ITEM_IDENTIFIERS,
                    musicItemIdentifiers);
        }

        // check request and extract the response
        final ReadResponse readResponse = new ReadResponse();
        readRequest =
                ReadMetaDataRequest.checkRequest(formItemList, readResponse);
        extractJson(readResponse);
    }

    @Test
    public void testReadMetaDataRequest() {
        fillRequest(VALID_MUSIC_ITEM_IDENTIFIERS);
        assertNotNull(readRequest);

        final String[] musicItemIdentifiers =
                VALID_MUSIC_ITEM_IDENTIFIERS.split(",");
        for (int i = 0; i < musicItemIdentifiers.length; i++) {
            assertEquals(musicItemIdentifiers[i],
                    readRequest.getMusicItemIdentifiers()[i]);
        }
    }

    @Test
    public void testMusicItemIdentifiersMissing() {
        fillRequest(null);
        checkForMissingParameterMessage(ProtocolConstants.Parameter.Read.MUSIC_ITEM_IDENTIFIERS);
        assertNull(readRequest);
    }

}
