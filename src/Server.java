import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Server {

    private static final int PORT = 5555;

    static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();

    static final AtomicLong messagesTotal = new AtomicLong(0);
    static final long startMillis = System.currentTimeMillis();

    public static void main(String[] args) {
        int port = PORT;
        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        Thread logger = new Thread(new LoggerWorker(logQueue, "server.log"), "logger-thread");
        logger.start();

        Thread monitor = new Thread(new MonitorWorker(startMillis, 10), "monitor-thread");
        monitor.setDaemon(true);
        monitor.start();

        log("Server starting on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                log("New connection from " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket);
                Thread t = new Thread(handler, "client-" + socket.getPort());
                t.start();
            }
        } catch (IOException e) {
            log("Server error: " + e.getMessage());
        }
    }

    static void log(String msg) {
        logQueue.offer("[" + System.currentTimeMillis() + "] " + msg);
        System.out.println(msg);
    }

    static boolean registerClient(String nick, ClientHandler handler) {
        return clients.putIfAbsent(nick, handler) == null;
    }

    static boolean renameClient(String oldNick, String newNick, ClientHandler handler) {
        if (oldNick.equals(newNick)) return true;

        synchronized (clients) {
            if (!clients.containsKey(oldNick)) return false;
            if (clients.containsKey(newNick)) return false;

            clients.remove(oldNick);
            clients.put(newNick, handler);
        }

        log("User renamed: " + oldNick + " -> " + newNick);
        return true;
    }

    static void unregisterClient(String nick) {
        if (nick != null) {
            clients.remove(nick);
        }
    }

    static void broadcast(String from, String text) {
        String msg = "[ALL] " + from + ": " + text;
        for (ClientHandler ch : clients.values()) {
            ch.send(msg);
        }
        messagesTotal.incrementAndGet();
        log("Broadcast from " + from + ": " + text);
    }

    static boolean privateMsg(String from, String to, String text) {
        ClientHandler target = clients.get(to);
        if (target == null) {
            return false;
        }
        target.send("[PM] " + from + ": " + text);

        ClientHandler sender = clients.get(from);
        if (sender != null) {
            sender.send("[PM -> " + to + "] " + text);
        }

        messagesTotal.incrementAndGet();
        log("PM " + from + " -> " + to + ": " + text);
        return true;
    }

    static String who() {
        return String.join(", ", clients.keySet());
    }

    static String getStats() {
        int online = clients.size();
        long msgs = messagesTotal.get();

        long uptimeMs = System.currentTimeMillis() - startMillis;
        Duration d = Duration.ofMillis(uptimeMs);
        long hours = d.toHours();
        long minutes = d.toMinutes() % 60;
        long seconds = d.getSeconds() % 60;

        String uptime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        return "Server stats:\n" +
                "Online clients: " + online + "\n" +
                "Total messages: " + msgs + "\n" +
                "Uptime: " + uptime;
    }
}
