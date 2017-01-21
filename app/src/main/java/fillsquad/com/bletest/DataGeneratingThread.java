package fillsquad.com.bletest;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by lee on 8/30/16.
 */

public class DataGeneratingThread extends Thread {

    private LinkedBlockingQueue queue;
    private static final String TAG = "DataGeneratingThread";

    public DataGeneratingThread(LinkedBlockingQueue queue) {
        this.queue = queue;
    }

    public void run() {
        int j = 1;
        while (true) {
            short[] a = new short[12];
            short wave = (short) (Math.sin( Math.toRadians(j++)) * 500);
            j = j + 2;

            for (int i = 0; i < 1; i++) {
                a[i] = wave;
            }

            try {
                queue.put(a);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(4);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }


}
