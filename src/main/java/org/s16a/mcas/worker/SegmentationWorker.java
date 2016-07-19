package org.s16a.mcas.worker;

import com.hp.hpl.jena.rdf.model.Property;
import org.s16a.mcas.MCAS;

public class SegmentationWorker extends PythonWorker implements Runnable {

	private static final Property TASK_QUEUE_NAME = MCAS.segments;

	public void run() {

        try {
            System.out.println(" [x] Execute : " + SegmentationWorker.class.getSimpleName());
            executePythonWorker(
                    TASK_QUEUE_NAME,
                    SegmentationWorker.class.getSimpleName(),
                    "/usr/bin/python ./src/main/java/org/s16a/mcas/worker/SegmentationWorker.py "
            );

        } catch (Exception e) {
            System.out.println(e.toString());
        }

	}

}

