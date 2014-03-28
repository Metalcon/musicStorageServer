package de.metalcon.musicStorageServer.zmqserver;

import de.metalcon.api.responses.Response;
import de.metalcon.api.responses.errors.UsageErrorResponse;
import de.metalcon.musicStorageServer.MusicStorageServer;
import de.metalcon.musicStorageServer.protocol.create.CreateResponse;
import de.metalcon.musicstreamingserver.api.requests.registration.CreateRequestData;
import de.metalcon.musicstreamingserver.api.responses.MusicStreamingCreateResponse;
import de.metalcon.zmqworker.ZMQRequestHandler;

/**
 * this is the entry point for the server to handle ZMQ requests coming e.g.
 * from the middleware.
 * The requests have to follow the metalcon music streaming server api
 * 
 * @author rpickhardt
 * 
 */
public class MusicStreamingRequestHandler implements ZMQRequestHandler {

    MusicStorageServer musicStorageServer;

    public MusicStreamingRequestHandler() {
        musicStorageServer = new MusicStorageServer("test.mss.config");
    }

    @Override
    public Response handleRequest(Object request) {
        if (request instanceof CreateRequestData) {
            return handleCreateRequest((CreateRequestData) request);
        }

        return new UsageErrorResponse("unknown request type",
                "use requests defined in URL mapping server API");
    }

    private Response handleCreateRequest(CreateRequestData request) {

        CreateResponse response = new CreateResponse();
        musicStorageServer.createMusicItem(request.getSerializedMuid(),
                request.getMusicFileInputStream(), request.getMetaData(),
                response);
        MusicStreamingCreateResponse resp =
                new MusicStreamingCreateResponse(response.toString());
        return resp;
    }
}
