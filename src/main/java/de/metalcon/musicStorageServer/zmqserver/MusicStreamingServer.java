package de.metalcon.musicStorageServer.zmqserver;

import net.hh.request_dispatcher.server.ZmqWorker;

import org.zeromq.ZMQ;

import de.metalcon.musicstreamingserver.api.requests.MusicStreamingRequest;
import de.metalcon.musicstreamingserver.api.responses.MusicStreamingResponse;

/**
 * launcher for the zmq server
 * 
 * @author rpickhardt
 * 
 */
public class MusicStreamingServer {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ZMQ.Context ctx = ZMQ.context(1);
        ZmqWorker<MusicStreamingRequest, MusicStreamingResponse> worker =
                new ZmqWorker<MusicStreamingRequest, MusicStreamingResponse>(
                        ctx, "tcp://127.0.0.1:6666",
                        new MusicStreamingRequestHandler());
        worker.run();
    }
}
