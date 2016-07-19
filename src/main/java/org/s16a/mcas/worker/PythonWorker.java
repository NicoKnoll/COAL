package org.s16a.mcas.worker;

import com.hp.hpl.jena.rdf.model.Property;
import com.rabbitmq.client.*;
import org.s16a.mcas.Cache;
import org.s16a.mcas.Enqueuer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

abstract class PythonWorker {

    public static void executePythonWorker (final Property taskQueueName, final String className, final String pathToPythonWorker) throws Exception, IOException {
        final Channel channel = Enqueuer.getChannel();

        channel.queueDeclare(taskQueueName.toString(), true, false, false, null);
        System.out.println(" [*] " + className + " is waiting for messages. To exit press CTRL+C");

        channel.basicQos(1);

        final Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");

                System.out.println(" [x] Received : " + className);

                try {
                    Cache cache = new Cache(message);

                    Process process = Runtime.getRuntime().exec(pathToPythonWorker + cache.getPath());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line;
                    String printedText = "";
                    String lastLine = "";
                    while ((line = reader.readLine()) != null) {
                        if(!line.equals("0")) {
                            line = " [E] " + line;
                        }

                        printedText = printedText + line + "\n";
                        lastLine = line;
                    }

                    if (lastLine.equals("0")) {
                        Enqueuer.workerFinished(taskQueueName, cache);
                    } else {
                        System.out.println(" [E] SOMETHING WENT WRONG IN " + taskQueueName.toString().toUpperCase());
                        System.out.println(" [E] ########### Python traceback ############ ");
                        System.out.print(printedText);
                        System.out.println(" [E] #########################################");
                    }
                } catch (Exception e) {
                        System.out.println(e.toString());
                }
                finally {
                    System.out.println(" [x] Done : " + className);
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }

            }
        };

        channel.basicConsume(taskQueueName.toString(), false, consumer);
    }

}
