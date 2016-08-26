package org.s16a.mcas.worker;

import com.hp.hpl.jena.rdf.model.Property;
import org.s16a.mcas.MCAS;

public class MusicRecognitionWorker extends PythonWorker implements Runnable {

    private static final Property TASK_QUEUE_NAME = MCAS.music;

    public void run() {

        try {
            System.out.println(" [x] Execute : " + MusicRecognitionWorker.class.getSimpleName());
            executePythonWorker(
                    TASK_QUEUE_NAME,
                    MusicRecognitionWorker.class.getSimpleName(),
                    "/usr/bin/python ./src/main/java/org/s16a/mcas/worker/MusicRecognitionWorker.py "
            );

        } catch (Exception e) {
            System.out.println(e.toString());
        }

    }
}

