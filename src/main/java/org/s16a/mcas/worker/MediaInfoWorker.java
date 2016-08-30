package org.s16a.mcas.worker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class MediaInfoWorker implements Runnable{

	private static final String TASK_QUEUE_NAME = MCAS.mediainfo.toString();

	public void run () {

		try {
            System.out.println(" [x] Execute : " + MediaInfoWorker.class.getSimpleName());
            executeWorker();
		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}
	public static void executeWorker() throws Exception {
        final Channel channel = Enqueuer.getChannel();

		channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] " + MediaInfoWorker.class.getSimpleName() + " is waiting for messages. To exit press CTRL+C");

		channel.basicQos(1);

		final Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String url = new String(body, "UTF-8");

                System.out.println(" [x] Received : " + MediaInfoWorker.class.getSimpleName());
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
                    System.out.println(" [x] Done : " + MediaInfoWorker.class.getSimpleName());
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

        FileWriter out = new FileWriter(modelFileName);

        printTurtle(model, out, cache);
	}

	private static void printTurtle(Model model, FileWriter writer, Cache cache) {
		try {
			System.out.println(" [x] Save turtle : " + MediaInfoWorker.class.getSimpleName());
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
