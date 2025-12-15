import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private String nick;

    private volatile boolean closed = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            send("Welcome! Use /help");

            String line;
            while (!closed && (line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("/")) {
                    handleCommand(line);
                } else {
                    if (nick == null) {
                        send("You must set nickname first: /nick yourName");
                    } else {
                        Server.broadcast(nick, line);
                    }
                }
            }
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg == null || !msg.toLowerCase().contains("socket closed")) {
                Server.log("Client error: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    private void handleCommand(String cmdLine) throws IOException {
        String[] parts = cmdLine.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "/help":
                sendHelp();
                break;

            case "/stats":
                send(Server.getStats());
                break;

            case "/nick":
                if (parts.length < 2) { send("Usage: /nick yourName"); return; }
                if (nick != null) { send("Nickname already set: " + nick + " (use /rename <new>)"); return; }

                String requested = parts[1];
                if (!isNickValid(requested)) return;

                if (Server.registerClient(requested, this)) {
                    nick = requested;
                    send("OK. Nick set to: " + nick);
                    Server.broadcast("SERVER", nick + " joined.");
                } else {
                    send("Nickname already taken.");
                }
                break;

            case "/rename":
                if (nick == null) { send("Set nickname first: /nick yourName"); return; }
                if (parts.length < 2) { send("Usage: /rename newName"); return; }

                String newNick = parts[1];
                if (!isNickValid(newNick)) return;

                String oldNick = nick;
                boolean ok = Server.renameClient(oldNick, newNick, this);
                if (!ok) {
                    send("Nickname already taken.");
                    return;
                }

                nick = newNick;
                send("OK. Nick changed: " + oldNick + " -> " + newNick);
                Server.broadcast("SERVER", oldNick + " changed nickname to " + newNick);
                break;

            case "/who":
                send("Online: " + Server.who());
                break;

            case "/all":
                if (nick == null) { send("Set nickname first: /nick yourName"); return; }
                if (parts.length < 2) { send("Usage: /all message"); return; }

                String msg = cmdLine.substring(5).trim();
                if (!msg.isEmpty()) Server.broadcast(nick, msg);
                break;

            case "/msg":
                if (nick == null) { send("Set nickname first: /nick yourName"); return; }
                if (parts.length < 3) { send("Usage: /msg user message"); return; }

                String to = parts[1];
                String text = parts[2];
                if (!Server.privateMsg(nick, to, text)) {
                    send("User not found: " + to);
                }
                break;

            case "/quit":
                send("Bye!");
                cleanup();
                break;

            default:
                send("Unknown command. Use /help");
        }
    }

    private boolean isNickValid(String requested) {
        if (requested.length() < 3) {
            send("Nickname too short (min 3).");
            return false;
        }
        if (requested.length() > 16) {
            send("Nickname too long (max 16).");
            return false;
        }
        if (!requested.matches("[A-Za-z0-9_]+")) {
            send("Nickname invalid. Allowed: letters, digits, underscore.");
            return false;
        }
        return true;
    }

    private void sendHelp() {
        send("Available commands:");
        send("/help              - show this help");
        send("/stats             - show server stats (online/messages/uptime)");
        send("/nick <name>       - set your nickname (once)");
        send("/rename <name>     - change your nickname");
        send("/who               - list online users");
        send("/all <message>     - send message to everyone");
        send("/msg <user> <msg>  - private message");
        send("/quit              - disconnect");
    }

    public void send(String msg) {
        try {
            out.write(msg);
            out.write("\n");
            out.flush();
        } catch (IOException ignored) {}
    }

    private void cleanup() {
        if (closed) return;
        closed = true;

        try {
            if (nick != null) {
                String leftNick = nick;
                nick = null;
                Server.broadcast("SERVER", leftNick + " left.");
                Server.unregisterClient(leftNick);
            }
        } catch (Exception ignored) {}

        try { socket.close(); } catch (IOException ignored) {}
    }
}
