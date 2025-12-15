import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class LoggerWorker implements Runnable {
    private final BlockingQueue<String> queue;
    private final String fileName;

    public LoggerWorker(BlockingQueue<String> queue, String fileName) {
        this.queue = queue;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileName, true), StandardCharsets.UTF_8))) {

            while (true) {
                String line = queue.take();
                w.write(line);
                w.write("\n");
                w.flush();
            }
        } catch (Exception e) {
            System.out.println("Logger stopped: " + e.getMessage());
        }
    }
}
