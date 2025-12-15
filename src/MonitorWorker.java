import java.time.Duration;

public class MonitorWorker implements Runnable {
    private final long startMillis;
    private final int intervalSeconds;

    public MonitorWorker(long startMillis, int intervalSeconds) {
        this.startMillis = startMillis;
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(intervalSeconds * 1000L);

                int online = Server.clients.size();
                long msgs = Server.messagesTotal.get();

                long uptimeMs = System.currentTimeMillis() - startMillis;
                String uptime = formatUptime(uptimeMs);

                Server.log("[MONITOR] Online: " + online +
                        " | Messages: " + msgs +
                        " | Uptime: " + uptime);

            } catch (InterruptedException e) {
                return;
            } catch (Exception e) {
                Server.log("[MONITOR] Error: " + e.getMessage());
            }
        }
    }

    private static String formatUptime(long uptimeMs) {
        Duration d = Duration.ofMillis(uptimeMs);
        long hours = d.toHours();
        long minutes = d.toMinutes() % 60;
        long seconds = d.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
