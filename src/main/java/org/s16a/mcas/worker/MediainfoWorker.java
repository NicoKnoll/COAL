package org.s16a.mcas.worker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.s16a.mcas.Cache;
import org.s16a.mcas.Enqueuer;
import org.s16a.mcas.MCAS;
import org.s16a.mcas.util.MediaInfo;

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

public class MediainfoWorker {
	private static final String TASK_QUEUE_NAME = MCAS.mediainfo.toString();

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		final Connection connection = factory.newConnection();
		final Channel channel = connection.createChannel();

		channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

		channel.basicQos(1);

		final Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String message = new String(body, "UTF-8");

				System.out.println(" [x] Received '" + message + "'");
				try {
					extractMediainfoSound(message);
				} finally {
					System.out.println(" [x] Done");
					channel.basicAck(envelope.getDeliveryTag(), false);
				}
			}
		};
		channel.basicConsume(TASK_QUEUE_NAME, false, consumer);
	}

	private static void extractMediainfo(String url) throws IOException {
		Cache cache = new Cache(url);

		// open model
		Model model = ModelFactory.createDefaultModel();
		String modelFileName = cache.getFilePath("data.ttl");
		File f = new File(modelFileName);

		if (f.exists()) {
			model.read(modelFileName);
		}

		String dataFileName = cache.getFilePath("data.jpg");

		MediaInfo info = new MediaInfo();
		info.open(new File(dataFileName));		

		String format = info.get(MediaInfo.StreamKind.Image, 0, "Format", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String width = info.get(MediaInfo.StreamKind.Image, 0, "Width", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String height = info.get(MediaInfo.StreamKind.Image, 0, "Height", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String bits = info.get(MediaInfo.StreamKind.Image, 0, "Bit depth", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		String compressionMode = info.get(MediaInfo.StreamKind.Image, 0, "Compression mode", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);
		
		// Format : PNG
		// Format/Info : Portable Network Graphic
		// File size : 133 KiB
		//
		// Image
		// Format : PNG
		// Format/Info : Portable Network Graphic
		// Width : 954 pixels
		// Height : 503 pixels
		// Bit depth : 32 bits
		// Compression mode : Lossless
		// Stream size : 133 KiB (100%)
		
		Resource r = model.createResource();
		r.addLiteral(DC.format, format);
		r.addLiteral(model.createProperty("http://ogp.me/ns#image:height"), height);
		r.addLiteral(model.createProperty("http://ogp.me/ns#image:width"), width);
		model.getResource(url).addProperty(MCAS.mediainfo, r);
		System.out.println(model.getResource(url).addProperty(MCAS.mediainfo, r));
		FileWriter out = new FileWriter(modelFileName);

		try {
			System.out.println("Mediainfo try save");
			model.write(out, "TURTLE");
		} finally {
			try {
				out.close();

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

	private static void extractMediainfoSound(String url) throws IOException {
		Cache cache = new Cache(url);

		// open model
		Model model = ModelFactory.createDefaultModel();
		String modelFileName = cache.getFilePath("data.ttl");
		File f = new File(modelFileName);

		if (f.exists()) {
			model.read(modelFileName);
		}

		String dataFileName = cache.getFilePath("data.mp3");

		MediaInfo info = new MediaInfo();
		info.open(new File(dataFileName));

		String bitRate = info.get(MediaInfo.StreamKind.Audio, 0, "BitRate", MediaInfo.InfoKind.Text, MediaInfo.InfoKind.Name);

		Resource r = model.createResource();
		/*r.addLiteral(DC.format, format);*/
		r.addLiteral(model.createProperty("Bitrate"), bitRate);

		model.getResource(url).addProperty(MCAS.mediainfo, r);
		System.out.println(model.getResource(url).addProperty(MCAS.mediainfo, r));
		FileWriter out = new FileWriter(modelFileName);

		try {
			System.out.println("Mediainfo try save");
			model.write(out, "TURTLE");
		} finally {
			try {
				out.close();

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