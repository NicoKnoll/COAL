package org.s16a.mcas.worker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.s16a.mcas.Enqueuer;
import org.s16a.mcas.Cache;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.s16a.mcas.MCAS;

public class DownloadWorker implements Runnable {
	private static final String TASK_QUEUE_NAME = MCAS.download.toString();

	public void run () {

        try {
            System.out.println(" [x] Execute : " + DownloadWorker.class.getSimpleName());
            executeWorker();
		} catch (Exception e) {
            System.out.println(e.toString());
		}

	}

	public static void executeWorker() throws Exception {
        final Channel channel = Enqueuer.getChannel();

		channel.queueDeclare(TASK_QUEUE_NAME, true, false, false, null);
        System.out.println(" [*] " + DownloadWorker.class.getSimpleName() + " is waiting for messages. To exit press CTRL+C");

		channel.basicQos(1);

		final Consumer consumer = new DefaultConsumer(channel) {
			@Override
			public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
				String url = new String(body, "UTF-8");

                System.out.println(" [x] Received : " + DownloadWorker.class.getSimpleName());
				try {
					downloadAndUpdateModel(url);
				} finally {
                    System.out.println(" [x] Done : " + DownloadWorker.class.getSimpleName());
					channel.basicAck(envelope.getDeliveryTag(), false);
				}
			}
		};
		channel.basicConsume(TASK_QUEUE_NAME, false, consumer);
	}

	private static void downloadAndUpdateModel(String url) {
		// open model
		Cache cache = new Cache(url);

		Model model = ModelFactory.createDefaultModel();
		String modelFileName = cache.getFilePath("data.ttl");

		File modelFile = new File(modelFileName);
		File dataFile = new File(url);

		if (modelFile.exists()) {
			model.read(modelFileName);
		}

		String dataFileExtension = getFileExtension(dataFile);
		String dataFileName = cache.getFilePath("data." + dataFileExtension);

		try {
			saveUrl(dataFileName, url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Enqueuer.workerFinished(MCAS.download, cache);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static String getFileExtension(File file) {
		String name = file.getName();

		try {
			return name.substring(name.lastIndexOf(".") + 1);
		} catch (Exception e) {
			return "";
		}
	}
	
	
	private static void saveUrl(final String filename, final String urlString) throws MalformedURLException, IOException {
	    BufferedInputStream in = null;
	    FileOutputStream fout = null;

	    try {
	        in = new BufferedInputStream(new URL(urlString).openStream());
	        fout = new FileOutputStream(filename);

	        final byte data[] = new byte[1024];
	        int count;
	        while ((count = in.read(data, 0, 1024)) != -1) {
	            fout.write(data, 0, count);
	        }
	    } finally {
	        if (in != null) {
	            in.close();
	        }
	        if (fout != null) {
	            fout.close();
	        }
	    }
	}
}