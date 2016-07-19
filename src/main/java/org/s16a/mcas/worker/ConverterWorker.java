package org.s16a.mcas.worker;

import com.hp.hpl.jena.rdf.model.Property;
import org.s16a.mcas.MCAS;

public class ConverterWorker extends PythonWorker implements Runnable {

	private static final Property TASK_QUEUE_NAME = MCAS.converter;

	public void run() {

		try {
            System.out.println(" [x] Execute : " + ConverterWorker.class.getSimpleName());
			executePythonWorker(
					TASK_QUEUE_NAME,
                    ConverterWorker.class.getSimpleName(),
					"/usr/bin/python ./src/main/java/org/s16a/mcas/worker/ConverterWorker.py "
			);

		} catch (Exception e) {
			System.out.println(e.toString());
		}

	}

}

