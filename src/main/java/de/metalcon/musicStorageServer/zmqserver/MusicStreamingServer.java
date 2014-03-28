package de.metalcon.musicStorageServer.zmqserver;

import de.metalcon.zmqworker.ZMQWorker;

/**
 * launcher for the zmq server
 * 
 * @author rpickhardt
 * 
 */
public class MusicStreamingServer {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        ZMQWorker worker =
                new ZMQWorker("tcp://127.0.0.1:6666",
                        new MusicStreamingRequestHandler());
        if (!worker.start()) {
            throw new IllegalStateException("failed to start worker");
        }
        worker.waitForShutdown();
        worker.stop();
    }

}
