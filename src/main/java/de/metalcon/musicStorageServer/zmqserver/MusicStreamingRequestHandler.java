package de.metalcon.musicStorageServer.zmqserver;

import net.hh.request_dispatcher.server.RequestHandler;
import de.metalcon.musicStorageServer.MusicStorageServer;
import de.metalcon.musicStorageServer.protocol.create.CreateResponse;
import de.metalcon.musicStorageServer.protocol.delete.DeleteResponse;
import de.metalcon.musicStorageServer.protocol.read.ReadResponse;
import de.metalcon.musicStorageServer.protocol.update.UpdateResponse;
import de.metalcon.musicstreamingserver.api.MusicData;
import de.metalcon.musicstreamingserver.api.requests.MusicStreamingRequest;
import de.metalcon.musicstreamingserver.api.requests.create.MusicStreamingCreateRequest;
import de.metalcon.musicstreamingserver.api.requests.delete.MusicStreamingDeleteRequest;
import de.metalcon.musicstreamingserver.api.requests.read.MusicStreamingReadMusicItemMetaDataRequest;
import de.metalcon.musicstreamingserver.api.requests.read.MusicStreamingReadMusicItemRequest;
import de.metalcon.musicstreamingserver.api.requests.update.MusicStreamingUpdateRequest;
import de.metalcon.musicstreamingserver.api.responses.MusicStreamingResponse;
import de.metalcon.musicstreamingserver.api.responses.create.MusicStreamingCreateResponse;
import de.metalcon.musicstreamingserver.api.responses.delete.MusicStreamingDeleteResponse;
import de.metalcon.musicstreamingserver.api.responses.read.MusicStreamingReadMusicItemMetaDataResponse;
import de.metalcon.musicstreamingserver.api.responses.read.MusicStreamingReadMusicItemResponse;
import de.metalcon.musicstreamingserver.api.responses.update.MusicStreamingUpdateResponse;

/**
 * this is the entry point for the server to handle ZMQ requests coming e.g.
 * from the middleware.
 * The requests have to follow the metalcon music streaming server api
 * 
 * @author rpickhardt
 * 
 */
public class MusicStreamingRequestHandler implements
        RequestHandler<MusicStreamingRequest, MusicStreamingResponse> {

    /**
     * 
     */
    private static final long serialVersionUID = -8601031539509477119L;

    MusicStorageServer musicStorageServer;

    public MusicStreamingRequestHandler() {
        musicStorageServer = new MusicStorageServer("test.mss.config");
    }

    @Override
    public MusicStreamingResponse handleRequest(MusicStreamingRequest request) {
        MusicStreamingResponse resp = handleRequestGateway(request);
        if (resp != null) {
            return resp;
        }
        // TODO: don't know how to also return Usage Error
        //        return new UsageErrorResponse(
        //                "unknown request type use requests defined in Music Streaming Server API");
        return null;
    }

    private MusicStreamingResponse handleRequestGateway(
            MusicStreamingRequest request) {
        if (request instanceof MusicStreamingCreateRequest) {
            return handleCreateRequest((MusicStreamingCreateRequest) request);
        } else if (request instanceof MusicStreamingReadMusicItemMetaDataRequest) {
            return handleReadMusicItemMetaDataRequest((MusicStreamingReadMusicItemMetaDataRequest) request);
        } else if (request instanceof MusicStreamingReadMusicItemRequest) {
            return handleReadMusicItemRequest((MusicStreamingReadMusicItemRequest) request);
        } else if (request instanceof MusicStreamingUpdateRequest) {
            return handleUpdateRequest((MusicStreamingUpdateRequest) request);
        } else if (request instanceof MusicStreamingDeleteRequest) {
            return handleDeleteRequest((MusicStreamingDeleteRequest) request);
        }
        return null;
    }

    private MusicStreamingResponse handleDeleteRequest(
            MusicStreamingDeleteRequest request) {
        DeleteResponse response = new DeleteResponse();
        if (musicStorageServer.deleteMusicItem(request.getSerializedMuid(),
                response)) {
            return new MusicStreamingDeleteResponse(response.toString());
        } else {
            return null;
        }
    }

    private MusicStreamingResponse handleUpdateRequest(
            MusicStreamingUpdateRequest request) {
        UpdateResponse response = new UpdateResponse();
        if (musicStorageServer.updateMetaData(request.getSerializedMuid(),
                request.getMetaData(), response)) {
            return new MusicStreamingUpdateResponse(response.toString());
        } else {
            return null;
        }
    }

    private MusicStreamingResponse handleReadMusicItemRequest(
            MusicStreamingReadMusicItemRequest request) {
        ReadResponse response = new ReadResponse();
        MusicData tmp =
                musicStorageServer.readMusicItem(request.getSerializedMuid(),
                        request.getVersion(), response);
        if (tmp != null) { // success
            return new MusicStreamingReadMusicItemResponse(response.toString(),
                    tmp);
        } else {
            return null;
        }
    }

    private MusicStreamingResponse handleReadMusicItemMetaDataRequest(
            MusicStreamingReadMusicItemMetaDataRequest request) {
        ReadResponse response = new ReadResponse();
        String[] tmp =
                musicStorageServer.readMusicItemMetaData(
                        request.getSerializedMuid(), response);
        if (tmp != null) { // success
            return new MusicStreamingReadMusicItemMetaDataResponse(
                    response.toString(), tmp);
        } else {
            return null;
        }
    }

    private MusicStreamingResponse handleCreateRequest(
            MusicStreamingCreateRequest request) {
        CreateResponse response = new CreateResponse();
        if (musicStorageServer.createMusicItem(request.getSerializedMuid(),
                request.getMusicFileInputStream(), request.getMetaData(),
                response)) {
            return new MusicStreamingCreateResponse(response.toString());
        } else {
            return null;
        }
    }
}
