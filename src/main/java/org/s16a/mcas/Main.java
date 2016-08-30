package org.s16a.mcas;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.s16a.mcas.worker.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;
import java.net.URI;

public class Main {

	public static final String BASE_URI = "http://0.0.0.0:8080/coal/";

    // todo: number of threads dynamic
    private static final int THREADS = 20;

	public static HttpServer startServer() {
		// create a resource config that scans for JAX-RS resources and
		// providers
		// in org.s16a.mcas package
		final ResourceConfig rc = new ResourceConfig().packages("org.s16a.mcas");

		// create and start a new instance of grizzly http server
		// exposing the Jersey application at BASE_URI
		return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
	}

	private static void startWorkers() {

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        Runnable downloadWorker = new DownloadWorker();
        executor.execute(downloadWorker);

        Runnable converterWorker = new ConverterWorker();
        executor.execute(converterWorker);

        Runnable id3TagWorker = new ID3TagWorker();
        executor.execute(id3TagWorker);

        Runnable mediainfoWorker = new MediaInfoWorker();
        executor.execute(mediainfoWorker);

        Runnable segmentationWorker = new SegmentationWorker();
        executor.execute(segmentationWorker);

        Runnable speechRecognitionWorker = new SpeechRecognitionWorker();
        executor.execute(speechRecognitionWorker);

        Runnable namedEntityLinkingWorker = new NamedEntityLinkingWorker();
        executor.execute(namedEntityLinkingWorker);

        Runnable musicRecognitionWorker = new MusicRecognitionWorker();
        executor.execute(musicRecognitionWorker);

//        executor.shutdown();
//
//        while (!executor.isTerminated()) {
//
//        }
//
//        System.out.println("\nFinished all threads");

	}

	public static void main(String[] args) throws IOException {
        System.out.println("Start Workers..");
        startWorkers();

		final HttpServer server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at " + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));

        System.in.read();
		server.shutdownNow();
	}
}
