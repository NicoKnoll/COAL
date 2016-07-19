package org.s16a.mcas.worker;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.rabbitmq.client.*;
import org.s16a.mcas.Cache;
import org.s16a.mcas.Enqueuer;
import org.s16a.mcas.MCAS;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ID3TagWorker implements Runnable {

    private static final String TASK_QUEUE_NAME = MCAS.id3tags.toString();

    public void run() {

        try {
            System.out.println(" [x] Execute : " + ID3TagWorker.class.getSimpleName());
            executeWorker();
        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }

    public static void executeWorker() throws Exception {
        final Channel channel = Enqueuer.getChannel();

        channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] " + ID3TagWorker.class.getSimpleName() + " is waiting for messages. To exit press CTRL+C");

        channel.basicQos(1);

        final Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String url = new String(body, "UTF-8");

                System.out.println(" [x] Received : " + ID3TagWorker.class.getSimpleName());
                try {
                    Cache cache = new Cache(url);

                    Process process = Runtime.getRuntime().exec("mutagen-inspect " + cache.getFilePath("data.mp3"));
                    process.waitFor();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    extractID3tags(url, reader);

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(" [x] Done : " + ID3TagWorker.class.getSimpleName());
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }
            }
        };
        channel.basicConsume(TASK_QUEUE_NAME, false, consumer);
    }

    private static void extractID3tags(String url, BufferedReader reader) throws IOException{
        Cache cache = new Cache(url);

        Model model = ModelFactory.createDefaultModel();
        String modelFileName = cache.getFilePath("data.ttl");
        File f = new File(modelFileName);

        if (f.exists()) {
            model.read(modelFileName);
        }
        Map<String, String> id3Tags = new HashMap<>();
        id3Tags.put("COMM", "comment");
        id3Tags.put("TALB", "albumTitle");
        id3Tags.put("TIT2", "title");
        id3Tags.put("APIC", "attachedPicture");
        id3Tags.put("TDRC", "recordingYear");
        id3Tags.put("TENC", "encodedBy");
        id3Tags.put("TPE1", "leadArtist");
        id3Tags.put("TPE2", "backgroundArtist");
        id3Tags.put("TPOS", "partOfSet");
        id3Tags.put("TRCK", "trackNumber");
        id3Tags.put("COMM", "comments");
        id3Tags.put("TSIZ", "audiofileSize");
        id3Tags.put("TBPM", "beatsPerMinute");
        id3Tags.put("WCOM", "commercialInformationURL");
        id3Tags.put("TDAT", "date");
        id3Tags.put("TSSE", "encodingSettings");
        id3Tags.put("TFLT", "fileType");


        String nid3 = "http://www.semanticdesktop.org/ontologies/2007/05/10/nid3/#";
        model.setNsPrefix("nid3", nid3);
        Resource r = model.getResource(url);

        // parse the output from mutagen output
        //skip first two lines
        reader.readLine(); reader.readLine();
        String s = null;
        try {
            while ((s = reader.readLine()) != null) {
                String[] parts = s.split("=", 2);

                String key = parts[0];
                String value = parts[1];

                if (id3Tags.containsKey(key)){
                    r.addLiteral(model.createProperty(nid3 + id3Tags.get(key)), value);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            //ignore because it is an invalid line
        }

        FileWriter out = new FileWriter(modelFileName);

        printTurtle(model, out, cache);

    }

    private static void printTurtle(Model model, FileWriter writer, Cache cache) {
        try {
            model.write(writer, "TURTLE");
            System.out.println(" [x] Save turtle : " + ID3TagWorker.class.getSimpleName());
        } finally {
            try {
                writer.close();
                try {
                    Enqueuer.workerFinished(MCAS.id3tags, cache);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException closeException) {
                System.out.println(closeException.getMessage());
            }
        }
    }

}
