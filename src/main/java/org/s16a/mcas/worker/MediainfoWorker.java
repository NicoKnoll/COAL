package org.s16a.mcas.worker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import org.s16a.mcas.Cache;
import org.s16a.mcas.Enqueuer;
import org.s16a.mcas.MCAS;
import org.s16a.mcas.util.mediainfo.MediaInfo;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class MediainfoWorker implements Runnable{

	private static final String TASK_QUEUE_NAME = MCAS.mediainfo.toString();

	public void run () {

		try {
            System.out.println(" [x] Execute : " + MediainfoWorker.class.getSimpleName());
            executeWorker();
		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}
	public static void executeWorker() throws Exception {
        final Channel channel = Enqueuer.getChannel();

		channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] " + MediainfoWorker.class.getSimpleName() + " is waiting for messages. To exit press CTRL+C");

		channel.basicQos(1);

		final Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String url = new String(body, "UTF-8");

                System.out.println(" [x] Received : " + MediainfoWorker.class.getSimpleName());
				try {

                    Cache cache = new Cache(url);

                    Model model = ModelFactory.createDefaultModel();
                    model.read(cache.getFilePath("data.ttl"));

                    String mimetype = simplifyMime(model.getResource(url).getProperty(DC.format).getString());

                    if(mimetype.equals("audio")) {
                        System.out.println(" [x] Mime type: audio ");
                        extractMediainfoAudio(url);
                    } else if(mimetype == "image") {
                        System.out.println(" [x] Mime type: Image ");
                        extractMediainfoImage(url);
                    } else {
                        System.out.println(" [x] Unsupported mime type: " + mimetype);
                    }

				} finally {
                    System.out.println(" [x] Done : " + MediainfoWorker.class.getSimpleName());
					channel.basicAck(envelope.getDeliveryTag(), false);
				}
			}
		};
		channel.basicConsume(TASK_QUEUE_NAME, false, consumer);
	}

    private static String simplifyMime(String mimetype) {
        return mimetype.split("/")[0];
    }

	private static void extractMediainfoImage(String url) throws IOException {
		Cache cache = new Cache(url);

		// open model
		Model model = ModelFactory.createDefaultModel();
		String modelFileName = cache.getFilePath("data.ttl");
		File f = new File(modelFileName);

		if (f.exists()) {
			model.read(modelFileName);
		}

		String dataFileName = cache.getResourceFilePath();

		MediaInfo info = new MediaInfo();
		info.open(new File(dataFileName));		

		String format = info.get(MediaInfo.StreamKind.Image, 0, "Format", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String width = info.get(MediaInfo.StreamKind.Image, 0, "Width", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String height = info.get(MediaInfo.StreamKind.Image, 0, "Height", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String bits = info.get(MediaInfo.StreamKind.Image, 0, "Bit depth", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String compressionMode = info.get(MediaInfo.StreamKind.Image, 0, "Compression mode", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		
		Resource r = model.createResource();
		r.addLiteral(DC.format, format);
		r.addLiteral(model.createProperty("http://ogp.me/ns#image:height"), height);
		r.addLiteral(model.createProperty("http://ogp.me/ns#image:width"), width);
		model.getResource(url).addProperty(MCAS.mediainfo, r);
		System.out.println(model.getResource(url).addProperty(MCAS.mediainfo, r));
		FileWriter out = new FileWriter(modelFileName);

		printTurtle(model, out, cache);
	}

	private static void extractMediainfoAudio(String url) throws IOException {
        Cache cache = new Cache(url);

        // open model
        Model model = ModelFactory.createDefaultModel();
        String modelFileName = cache.getFilePath("data.ttl");
        String COAL_SERVER_URI = "http://coal.s16a.org/resource";
        String MEDIA_URI = cache.getUrl();

        File f = new File(modelFileName);

        if (f.exists()) {
            model.read(modelFileName);
        }

        String dataFileName = cache.getFilePath("data.mp3");

        MediaInfo info = new MediaInfo();
        info.open(new File(dataFileName));

        String fileExtension = info.get(MediaInfo.StreamKind.General, 0, "FileExtension", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        Long fileSize = Long.parseLong(info.get(MediaInfo.StreamKind.General, 0, "FileSize", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name));
        String format = info.get(MediaInfo.StreamKind.Audio, 0, "Format", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        String formatVersion = info.get(MediaInfo.StreamKind.Audio, 0, "Format_Version", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        String formatProfile = info.get(MediaInfo.StreamKind.Audio, 0, "Format_Profile", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        String codecId = info.get(MediaInfo.StreamKind.Audio, 0, "CodecID", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        Long duration = Long.parseLong(info.get(MediaInfo.StreamKind.Audio, 0, "Duration", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name));
        String bitRateMode = info.get(MediaInfo.StreamKind.Audio, 0, "BitRate_Mode", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        Integer bitRate = Integer.parseInt(info.get(MediaInfo.StreamKind.Audio, 0, "BitRate", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name));
        Integer channels = Integer.parseInt(info.get(MediaInfo.StreamKind.Audio, 0, "Channels", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name));
        String channelPositions = info.get(MediaInfo.StreamKind.Audio, 0, "ChannelPositions", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
        Integer samplingRate = Integer.parseInt(info.get(MediaInfo.StreamKind.Audio, 0, "SamplingRate", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name));
        Long streamSize = Long.parseLong(info.get(MediaInfo.StreamKind.Audio, 0, "StreamSize", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name));


        Resource r = model.getResource(url);
        String nfo = "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo/#";
        model.setNsPrefix("nfo", nfo);
        String nie = "http://www.semanticdesktop.org/ontologies/2007/03/22/nfo/#";
        model.setNsPrefix("nie", nie);
        String dbo = "http://dbpedia.org/ontology/";
        model.setNsPrefix("dbo", dbo);
        String ebu = "http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#";
        model.setNsPrefix("ebu", ebu);

        r.addLiteral(model.createProperty(nfo + "fileSize"), fileSize);
        r.addLiteral(model.createProperty(dbo + "fileExtension"), fileExtension);
/*
		r.addLiteral(model.createProperty("format"), format);
		r.addLiteral(model.createProperty("formatVersion"), formatVersion);
		r.addLiteral(model.createProperty("formatProfile"), formatProfile);
*/
        r.addLiteral(model.createProperty(nfo + "duration"), duration);
        r.addLiteral(model.createProperty(nfo + "averageBitrate"), bitRate);
        r.addLiteral(model.createProperty(nfo + "bitRateMode"), bitRateMode);

        r.addLiteral(model.createProperty(ebu + "audioChannelNumber"), channels);
        r.addLiteral(model.createProperty(nfo + "sampleRate"), samplingRate);

        String nif = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";
        model.setNsPrefix("nif", nif);
        String dcterms = "http://purl.org/dc/terms/";
        model.setNsPrefix("dcterms", dcterms);
        Resource segment = model.createResource(MEDIA_URI + "#t=" + "1,43");
        segment.addProperty(DCTerms.isPartOf, MEDIA_URI);
        segment.addProperty(RDF.type, model.createResource(nif + "RFC5147String"));
        String text1 = "first rule about Fight Club is you don't talk about Fight Club I tell Walter\n" +
                "I did this to myself before the presentation when I said\n" +
                "I said across from my boss telling him where in the script each slide cues and when I wanted to run the VIN\n" +
                "my boss says what do you get yourself into every weekend\n" +
                "I just don't want to die without a few scars I'd say it's nothing anymore to have\n" +
                "more to have a beautiful stock body you see those cars that are completely stock cherry\n" +
                "Cherry right out of a dealer showroom in 1955 I always think what a waste\n" +
                "the second rule about Fight Club is don't talk about Fight Club\n" +
                "Flight Club";

        segment.addProperty(model.createProperty(dcterms + "description"), text1);

        segment = model.createResource(MEDIA_URI + "#t=" + "276,304");
        segment.addProperty(DCTerms.isPartOf, MEDIA_URI);
        segment.addProperty(RDF.type, model.createResource(nif + "RFC5147String"));
        String text2 = "because Fight Club exists only in the hours between one fight club store\n" +
                "the club and you saw the kid who works in the copy\n" +
                "copy center a month ago you saw this kid who can't remember the 3 hole punch in order to put colored slips\n" +
                "slip sheets between the coffee packets but this kid wasn't God for 10 minutes when you saw him\n" +
                "the air out of an account representative twice his size then land on the moon in pounds\n" +
                "until the kid had to stop";
        segment.addProperty(model.createProperty(dcterms + "description"), text2);

        FileWriter out = new FileWriter(modelFileName);

        printTurtle(model, out, cache);
	}

	private static void printTurtle(Model model, FileWriter writer, Cache cache) {
		try {
			System.out.println(" [x] Save turtle : " + MediainfoWorker.class.getSimpleName());
			model.write(writer, "TURTLE");
		} finally {
			try {
				writer.close();

				try {
					Enqueuer.workerFinished(MCAS.mediainfo, cache);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} catch (IOException closeException) {
				// ignore
				System.out.println("Fehler!!!");
			}
		}
	}

}
