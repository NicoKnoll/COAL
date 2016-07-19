package org.s16a.mcas.worker;

import java.io.FileWriter;
import java.io.IOException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import org.s16a.mcas.Enqueuer;
import org.s16a.mcas.MCAS;
import org.s16a.mcas.Cache;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.s16a.mcas.util.acoustid.AcoustID;
import org.s16a.mcas.util.acoustid.ChromaPrint;
import org.s16a.mcas.util.musicbrainz.MusicBrainz;
import org.s16a.mcas.util.TrackInformation;

public class MusicRecognitionWorker implements Runnable {
    private static final String TASK_QUEUE_NAME = MCAS.music.toString();
    private static final String FPCALC = "/knowmin/chromaprint-fpcalc-1.3.2-linux-x86_64/fpcalc";
    private static final int QUERYRATE = 10;

    public void run () {

        try {
            executeWorker();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }

    // todo: naming of "main"-method in java workers
    public static void executeWorker() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(System.getenv().get("RABBIT_HOST"));

        final Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        channel.basicQos(1);

        final Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String url = new String(body, "UTF-8");

                System.out.println(" [x] Received '" + url + "'");
                try {
                    Cache cache = new Cache(url);

                    Map<String, TrackInformation> mapping = new HashMap<>();

                    // Walk cache directory and process .wav audio files
                    final File[] files = new File(cache.getPath()).listFiles();

                    for (File file : files) {
                        if (file.isFile()) {
                            if (isValidForProcessing(file.getName())) {
                                try {
                                    TrackInformation currentTrackInformation = processAudioFile(file.getAbsolutePath());

                                    if (currentTrackInformation != null)
                                        mapping.put(file.getName(), currentTrackInformation);

                                    // Sleep to prevent musicbrainz from overloading
                                    Thread.sleep(QUERYRATE * 1000);
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    extractTrackInfo(url, mapping);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(" [x] Done");
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };
        channel.basicConsume(TASK_QUEUE_NAME, false, consumer);
    }

    private static TrackInformation processAudioFile(String url) throws Exception {
        File file = new File(url);

        final ChromaPrint chromaprint = AcoustID.chromaprint(file, FPCALC);
        final String musicbrainzId = AcoustID.lookup(chromaprint);

        if (musicbrainzId != null) {
            return  MusicBrainz.lookup(musicbrainzId);
        }

        return null;
    }

    private static void extractTrackInfo(String url, Map<String, TrackInformation> mapping) throws IOException {
        Cache cache = new Cache(url);

        Model model = ModelFactory.createDefaultModel();
        String modelFileName = cache.getFilePath("data.ttl");
        String MEDIA_URI = cache.getUrl();

        File f = new File(modelFileName);

        if (f.exists()) {
            model.read(modelFileName);
        }

        String mo = "http://purl.org/ontology/mo/";
        model.setNsPrefix("mo", mo);
        String dc = "http://purl.org/dc/elements/1.1/";
        model.setNsPrefix("dc", dc);
        String xsd = "http://www.w3.org/2001/XMLSchema#";
        model.setNsPrefix("xsd", xsd);
        String foaf = "http://xmlns.com/foaf/0.1/";
        model.setNsPrefix("foaf", foaf);
        String nif = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
        model.setNsPrefix("nif", nif);
        // todo: implement named entity linking
        String dbp = "http://de.dbpedia.org/resource/";
        model.setNsPrefix("dbpedia-de", dbp);

        for (String filename : mapping.keySet()) {
            TrackInformation trackInformation = mapping.get(filename);

            // todo: add range of music segment
            Resource resource = model.createResource(url + "#t="+ getRangeAsString(filename));

            resource.addProperty(DCTerms.isPartOf, MEDIA_URI);
            resource.addProperty(RDF.type, model.createResource(nif + "RFC5147String"));
            resource.addProperty(RDF.type, mo + "Track");

            if (trackInformation.getTitle() != null)
                resource.addLiteral(model.createProperty(dc + "title"), trackInformation.getTitle());

            if (trackInformation.getArtist() != null)
                resource.addProperty(model.createProperty(mo + "artist"), model.createResource(dbp + trackInformation.getArtist()));

            if (trackInformation.getRelease() != null)
                resource.addLiteral(model.createProperty(mo + "release"), trackInformation.getRelease());

//            resource.addLiteral(model.createProperty(mo + "artwork"), trackInformation.getArtwork());

            if (trackInformation.getIsrc() != null)
                resource.addLiteral(model.createProperty(mo + "isrc"), trackInformation.getIsrc());

            if (trackInformation.getMusicbrainzid() != null)
                resource.addLiteral(model.createProperty(mo + "musicbrainz_guid"), trackInformation.getMusicbrainzid());

        }

        FileWriter out = new FileWriter(modelFileName);
        printTurtle(model, out, cache);
    }

    private static boolean isValidForProcessing(String filename) {
        if(!filename.toLowerCase().endsWith(".wav"))
            return false;

        // Cut .wav
        filename = filename.substring(0, filename.length() - 4);

        if (filename.equals("data"))
            return false;

        String[] segments = filename.split("_");

        if(segments[1].equals("speech"))
            return false;

        String[] range = segments[0].split("-");
        int start = Integer.parseInt(range[0]);
        int end = Integer.parseInt(range[1]);

        return (end-start) > 60;
    }

    private static String getRangeAsString(String filename) {
        filename = filename.substring(0, filename.length() - 10);

        String[] range = filename.split("-");

        return range[0] + "," + range[1];
    }

    private static void printTurtle(Model model, FileWriter writer, Cache cache) {
        try {
            System.out.println("MusicMetaData try save");
            model.write(writer, "TURTLE");
        } finally {
            try {
                writer.close();

                try {
                    Enqueuer.workerFinished(MCAS.music, cache);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException closeException) {
                // todo: exception handling
                System.out.println("Fehler!!!");
            }
        }
    }
}