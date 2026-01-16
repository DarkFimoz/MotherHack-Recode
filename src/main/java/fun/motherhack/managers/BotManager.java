package fun.motherhack.managers;

import fun.motherhack.utils.network.ChatUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public class BotManager {

    private final List<MinecraftBot> bots = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    
    private volatile boolean running = false;
    private String currentHost;
    private int currentPort;
    private String baseNickname;
    private String password;
    private boolean spamEnabled;
    private String spamMessage;
    private long spamDelay;
    private long joinDelay;
    private int botCount;

    public void startBots(String host, int port, String nickname, int count, String pass,
                          boolean spam, String spamMsg, long spamDel, long joinDel) {
        stopAllBots();
        
        this.currentHost = host;
        this.currentPort = port;
        this.baseNickname = nickname;
        this.password = pass;
        this.spamEnabled = spam;
        this.spamMessage = spamMsg;
        this.spamDelay = spamDel;
        this.joinDelay = joinDel;
        this.botCount = count;
        this.running = true;

        for (int i = 1; i <= count; i++) {
            final int botIndex = i;
            final String botName = nickname + botIndex;
            
            scheduler.schedule(() -> {
                if (running) {
                    MinecraftBot bot = new MinecraftBot(botName, host, port, pass, spam, spamMsg, spamDel, this);
                    bots.add(bot);
                    executor.submit(bot::connect);
                }
            }, joinDel * (botIndex - 1), TimeUnit.MILLISECONDS);
        }
        
        ChatUtils.sendMessage("§a[Bots] Starting " + count + " bots...");
    }

    public void stopAllBots() {
        running = false;
        
        for (MinecraftBot bot : bots) {
            bot.disconnect();
        }
        bots.clear();
        
        ChatUtils.sendMessage("§c[Bots] All bots stopped.");
    }

    public int getActiveBotCount() {
        return (int) bots.stream().filter(MinecraftBot::isConnected).count();
    }

    public void removeBot(MinecraftBot bot) {
        bots.remove(bot);
    }

    public void scheduleReconnect(MinecraftBot bot, long delayMs) {
        if (!running) return;
        
        scheduler.schedule(() -> {
            if (running) {
                ChatUtils.sendMessage("§e[Bots] Reconnecting " + bot.getUsername() + "...");
                bot.connect();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        stopAllBots();
        executor.shutdown();
        scheduler.shutdown();
    }

    @Getter
    public static class MinecraftBot {
        private final String username;
        private final String host;
        private final int port;
        private final String password;
        private final boolean spamEnabled;
        private final String spamMessage;
        private final long spamDelay;
        private final BotManager manager;
        
        private Socket socket;
        private DataInputStream in;
        private DataOutputStream out;
        private volatile boolean connected = false;
        private volatile boolean shouldRun = true;
        private ScheduledFuture<?> spamTask;
        private ScheduledFuture<?> keepAliveTask;
        private final AtomicBoolean loggedIn = new AtomicBoolean(false);
        private final AtomicBoolean registered = new AtomicBoolean(false);

        public MinecraftBot(String username, String host, int port, String password,
                           boolean spamEnabled, String spamMessage, long spamDelay, BotManager manager) {
            this.username = username;
            this.host = host;
            this.port = port;
            this.password = password;
            this.spamEnabled = spamEnabled;
            this.spamMessage = spamMessage;
            this.spamDelay = spamDelay;
            this.manager = manager;
        }

        public void connect() {
            try {
                shouldRun = true;
                loggedIn.set(false);
                registered.set(false);
                
                socket = new Socket();
                socket.setSoTimeout(30000);
                socket.connect(new InetSocketAddress(host, port), 10000);
                
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                
                // Handshake
                sendHandshake();
                
                // Login Start
                sendLoginStart();
                
                connected = true;
                ChatUtils.sendMessage("§a[Bots] " + username + " connected!");
                
                // Start reading packets
                readPackets();
                
            } catch (Exception e) {
                ChatUtils.sendMessage("§c[Bots] " + username + " failed to connect: " + e.getMessage());
                disconnect();
            }
        }

        private void sendHandshake() throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream packet = new DataOutputStream(buffer);
            
            writeVarInt(packet, 0x00); // Packet ID
            writeVarInt(packet, 767); // Protocol version (1.21.4)
            writeString(packet, host);
            packet.writeShort(port);
            writeVarInt(packet, 2); // Next state: Login
            
            byte[] data = buffer.toByteArray();
            writeVarInt(out, data.length);
            out.write(data);
            out.flush();
        }

        private void sendLoginStart() throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream packet = new DataOutputStream(buffer);
            
            writeVarInt(packet, 0x00); // Packet ID: Login Start
            writeString(packet, username);
            writeUUID(packet); // Player UUID (random for offline)
            
            byte[] data = buffer.toByteArray();
            writeVarInt(out, data.length);
            out.write(data);
            out.flush();
        }

        private void readPackets() {
            try {
                while (shouldRun && connected && !socket.isClosed()) {
                    int length = readVarInt(in);
                    if (length <= 0) continue;
                    
                    byte[] packetData = new byte[length];
                    in.readFully(packetData);
                    
                    DataInputStream packetIn = new DataInputStream(new ByteArrayInputStream(packetData));
                    int packetId = readVarInt(packetIn);
                    
                    handlePacket(packetId, packetIn, packetData);
                }
            } catch (Exception e) {
                if (shouldRun) {
                    String error = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                    if (error.contains("logging in too fast") || error.contains("too fast")) {
                        ChatUtils.sendMessage("§e[Bots] " + username + " kicked for logging too fast, reconnecting in 10s...");
                        manager.scheduleReconnect(this, 10000);
                    } else {
                        ChatUtils.sendMessage("§c[Bots] " + username + " disconnected: " + e.getMessage());
                    }
                }
            } finally {
                disconnect();
            }
        }

        private void handlePacket(int packetId, DataInputStream packetIn, byte[] rawData) throws IOException {
            switch (packetId) {
                case 0x00 -> { // Disconnect (Login) or Keep Alive response needed
                    if (!loggedIn.get()) {
                        // Login disconnect
                        String reason = readString(packetIn);
                        handleDisconnect(reason);
                    }
                }
                case 0x01 -> { // Encryption Request - offline mode servers skip this
                    // For offline servers, we don't need encryption
                }
                case 0x02 -> { // Login Success
                    ChatUtils.sendMessage("§a[Bots] " + username + " logged in successfully!");
                    loggedIn.set(true);
                    
                    // Send Login Acknowledged
                    sendLoginAcknowledged();
                }
                case 0x03 -> { // Set Compression
                    readVarInt(packetIn); // threshold - compression not implemented
                }
                case 0x19 -> { // Plugin Message (play state)
                    // Ignore
                }
                case 0x1F, 0x26 -> { // Keep Alive (different IDs for different states)
                    long keepAliveId = packetIn.readLong();
                    sendKeepAlive(keepAliveId);
                }
                case 0x6C -> { // System Chat Message
                    String message = readString(packetIn);
                    handleChatMessage(message);
                }
                case 0x1D -> { // Disconnect (Play)
                    String reason = readString(packetIn);
                    handleDisconnect(reason);
                }
            }
        }

        private void handleChatMessage(String message) {
            String lowerMessage = message.toLowerCase();
            
            // Check for registration prompt
            if ((lowerMessage.contains("reg") || lowerMessage.contains("register")) && !registered.get() && password != null && !password.isEmpty()) {
                manager.scheduler.schedule(() -> {
                    sendChat("/register " + password + " " + password);
                    registered.set(true);
                    ChatUtils.sendMessage("§a[Bots] " + username + " sent /register");
                }, 500, TimeUnit.MILLISECONDS);
            }
            
            // Check for login prompt
            if ((lowerMessage.contains("log") || lowerMessage.contains("login") || lowerMessage.contains("авториз")) && !loggedIn.get() && password != null && !password.isEmpty()) {
                manager.scheduler.schedule(() -> {
                    sendChat("/login " + password);
                    ChatUtils.sendMessage("§a[Bots] " + username + " sent /login");
                }, 500, TimeUnit.MILLISECONDS);
            }
        }

        private void handleDisconnect(String reason) {
            String lowerReason = reason.toLowerCase();
            
            if (lowerReason.contains("logging in too fast") || lowerReason.contains("too fast") || 
                lowerReason.contains("слишком быстро") || lowerReason.contains("подождите")) {
                ChatUtils.sendMessage("§e[Bots] " + username + " kicked: too fast, reconnecting in 10s...");
                shouldRun = false;
                manager.scheduleReconnect(this, 10000);
            } else {
                ChatUtils.sendMessage("§c[Bots] " + username + " disconnected: " + reason);
            }
        }

        private void sendLoginAcknowledged() {
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream packet = new DataOutputStream(buffer);
                writeVarInt(packet, 0x03); // Login Acknowledged
                
                byte[] data = buffer.toByteArray();
                writeVarInt(out, data.length);
                out.write(data);
                out.flush();
                
                // Start spam if enabled
                if (spamEnabled && spamMessage != null && !spamMessage.isEmpty()) {
                    startSpam();
                }
                
            } catch (IOException e) {
                // Ignore
            }
        }

        private void sendKeepAlive(long id) {
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream packet = new DataOutputStream(buffer);
                writeVarInt(packet, 0x18); // Keep Alive response
                packet.writeLong(id);
                
                byte[] data = buffer.toByteArray();
                writeVarInt(out, data.length);
                out.write(data);
                out.flush();
            } catch (IOException e) {
                // Ignore
            }
        }

        public void sendChat(String message) {
            if (!connected || socket.isClosed()) return;
            
            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                DataOutputStream packet = new DataOutputStream(buffer);
                
                if (message.startsWith("/")) {
                    // Chat Command packet
                    writeVarInt(packet, 0x04);
                    writeString(packet, message.substring(1)); // Remove leading /
                    packet.writeLong(System.currentTimeMillis()); // Timestamp
                    packet.writeLong(0); // Salt
                    writeVarInt(packet, 0); // Argument signatures count
                    writeVarInt(packet, 0); // Message count
                    packet.write(new byte[20]); // Acknowledged
                } else {
                    // Chat Message packet
                    writeVarInt(packet, 0x06);
                    writeString(packet, message);
                    packet.writeLong(System.currentTimeMillis());
                    packet.writeLong(0); // Salt
                    packet.writeBoolean(false); // Has signature
                    writeVarInt(packet, 0); // Message count
                    packet.write(new byte[20]); // Acknowledged
                }
                
                byte[] data = buffer.toByteArray();
                writeVarInt(out, data.length);
                out.write(data);
                out.flush();
            } catch (IOException e) {
                // Ignore
            }
        }

        private void startSpam() {
            if (spamTask != null) {
                spamTask.cancel(false);
            }
            
            spamTask = manager.scheduler.scheduleAtFixedRate(() -> {
                if (connected && shouldRun) {
                    sendChat(spamMessage);
                }
            }, spamDelay, spamDelay, TimeUnit.MILLISECONDS);
        }

        public void disconnect() {
            shouldRun = false;
            connected = false;
            
            if (spamTask != null) {
                spamTask.cancel(false);
                spamTask = null;
            }
            
            if (keepAliveTask != null) {
                keepAliveTask.cancel(false);
                keepAliveTask = null;
            }
            
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                // Ignore
            }
            
            socket = null;
            in = null;
            out = null;
        }

        // Protocol helpers
        private void writeVarInt(DataOutputStream out, int value) throws IOException {
            while ((value & ~0x7F) != 0) {
                out.writeByte((value & 0x7F) | 0x80);
                value >>>= 7;
            }
            out.writeByte(value);
        }

        private int readVarInt(DataInputStream in) throws IOException {
            int value = 0;
            int position = 0;
            byte currentByte;
            
            while (true) {
                currentByte = in.readByte();
                value |= (currentByte & 0x7F) << position;
                
                if ((currentByte & 0x80) == 0) break;
                
                position += 7;
                if (position >= 32) throw new RuntimeException("VarInt too big");
            }
            
            return value;
        }

        private void writeString(DataOutputStream out, String str) throws IOException {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            writeVarInt(out, bytes.length);
            out.write(bytes);
        }

        private String readString(DataInputStream in) throws IOException {
            int length = readVarInt(in);
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private void writeUUID(DataOutputStream out) throws IOException {
            // Random offline UUID
            out.writeLong(username.hashCode());
            out.writeLong(System.currentTimeMillis());
        }
    }
}
