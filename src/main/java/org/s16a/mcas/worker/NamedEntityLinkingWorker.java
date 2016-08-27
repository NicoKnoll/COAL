package org.s16a.mcas.worker;

import com.hp.hpl.jena.rdf.model.Property;
import org.s16a.mcas.MCAS;

public class NamedEntityLinkingWorker extends PythonWorker implements Runnable {

    private static final Property TASK_QUEUE_NAME = MCAS.nel;

    public void run() {

        try {
            System.out.println(" [x] Execute : " + NamedEntityLinkingWorker.class.getSimpleName());
            executePythonWorker(
                    TASK_QUEUE_NAME,
                    NamedEntityLinkingWorker.class.getSimpleName(),
                    "/usr/bin/python ./src/main/java/org/s16a/mcas/worker/NamedEntityLinkingWorker.py "
            );

        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }

}