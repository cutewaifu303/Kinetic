package secret.kinetic.api.gui.alt.comp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import secret.kinetic.api.gui.kinetic.KineticUi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The PandaService shop embedded in the game, following the shop's embed docs: the {@code /embed} page is
 * loaded top-level in a WebView, so the normal cookie session works; the User-Agent is fixed because web sessions
 * are bound to it; nothing secret is put in the URL; the page is loaded once and kept alive instead of reloaded
 * (it is rate limited per IP); and the page's {@code postMessage} events (ready, login, logout, route) are
 * bridged into the client.
 *
 * The game's Java usually has no JavaFX, so the WebView runs offscreen in {@link PandaWebHost}, a helper process
 * on a Java 8 with JavaFX (Azul Zulu FX, shared with the launcher's runtime folder and downloaded once if
 * missing). Changed frames come back over a loopback socket into memory (no disk involved) and are drawn as a
 * texture; input goes the other way over the helper's stdin.
 */
public final class PandaWebView {

    public static final String URL = "https://altshop.pandaservice.eu/embed";
    // web sessions are bound to IP + User-Agent, so this must never change between requests
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/605.1.15 (KHTML, like Gecko) Kinetic/1.8.9";

    // same runtime and marker as the launcher, so either of them can reuse what the other downloaded
    private static final String JRE_VERSION = "zulu8.96.0.205-ca-fx-jre8.0.504";
    private static final String HOST_PREFIX = "[panda-webview] ";

    private static volatile String state = "Starting...";
    private static volatile String user = "";
    private static volatile String view = "";
    private static volatile boolean starting;
    private static volatile Process host;
    private static volatile PrintWriter commands;
    private static volatile boolean connected;

    // filled by the socket reader, swapped under FRAME_LOCK, uploaded on the render thread
    private static final Object FRAME_LOCK = new Object();
    private static ByteBuffer front, back;
    private static int frontW, frontH;
    private static boolean frameReady;

    private static int texture = -1, textureW, textureH;
    private static boolean uploadedOnce;
    private static int sentW, sentH;
    private static volatile long lastDraw;
    private static volatile boolean activeSent;

    private PandaWebView() {
    }

    public static String state() {
        return state;
    }

    public static String user() {
        return user;
    }

    public static String view() {
        return view;
    }

    public static boolean running() {
        Process p = host;
        return p != null && p.isAlive() && connected;
    }

    public static boolean hasFrame() {
        return texture != -1 && uploadedOnce;
    }

    /** Starts the helper in the background if it is not running yet. Safe to call every frame. */
    public static void ensureStarted() {
        if (running() || starting) return;
        starting = true;
        Thread thread = new Thread(() -> {
            try {
                File java = fxJava();
                state = "Opening shop...";
                launchHost(java);
            } catch (Exception e) {
                e.printStackTrace();
                state = "Could not start the shop (" + e.getMessage() + ")";
            } finally {
                starting = false;
            }
        }, "PandaAlts-WebView");
        thread.setDaemon(true);
        thread.start();
    }

    public static void openInBrowser() {
        KineticUi.openUrl(URL);
    }

    public static void send(String command) {
        PrintWriter out = commands;
        if (out == null) return;
        out.println(command);
        out.flush();
    }

    /** Native pixel size the page should render at. Only sent when it changes. */
    public static void size(int w, int h) {
        if (w == sentW && h == sentH) return;
        sentW = w;
        sentH = h;
        send("size " + w + " " + h);
    }

    public static void setActive(boolean active) {
        activeSent = active;
        send("active " + (active ? 1 : 0));
    }

    public static void reload() {
        send("reload");
    }

    /** Uploads the newest frame (render thread only) and draws it into the given GUI rectangle. */
    public static void draw(float x, float y, float w, float h) {
        lastDraw = System.currentTimeMillis();
        if (!activeSent && commands != null) setActive(true);
        // take and upload under one lock, so the socket reader never writes into the buffer being uploaded
        synchronized (FRAME_LOCK) {
            if (frameReady && front != null) {
                int fw = frontW, fh = frontH;
                frameReady = false;
                if (texture == -1) texture = GL11.glGenTextures();
                GlStateManager.bindTexture(texture);
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
                front.position(0).limit(fw * fh * 4);
                if (fw != textureW || fh != textureH) {
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
                    GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, fw, fh, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, front);
                    textureW = fw;
                    textureH = fh;
                } else {
                    GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, fw, fh, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, front);
                }
                uploadedOnce = true;
            }
        }
        if (texture == -1 || textureW == 0) return;

        // the frame may be a pixel larger than requested (snapshot rounding), so map it 1:1 and crop
        float u = Math.min(1f, sentW > 0 ? (float) sentW / textureW : 1f);
        float v = Math.min(1f, sentH > 0 ? (float) sentH / textureH : 1f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.bindTexture(texture);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        wr.pos(x, y + h, 0).tex(0, v).endVertex();
        wr.pos(x + w, y + h, 0).tex(u, v).endVertex();
        wr.pos(x + w, y, 0).tex(u, 0).endVertex();
        wr.pos(x, y, 0).tex(0, 0).endVertex();
        tessellator.draw();
        GlStateManager.enableBlend();
    }

    // ---- helper process ----

    private static File dataDir() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            return new File(local != null && !local.isEmpty() ? new File(local) : new File(home, "AppData/Local"), "KineticClient");
        }
        if (os.contains("mac")) return new File(home, "Library/Application Support/KineticClient");
        String xdg = System.getenv("XDG_DATA_HOME");
        return new File(xdg != null && !xdg.isEmpty() ? new File(xdg) : new File(home, ".local/share"), "KineticClient");
    }

    private static boolean windows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static File javaIn(File jre) {
        String bin = windows() ? "bin/javaw.exe" : "bin/java";
        for (String path : new String[]{bin, "jre/" + bin, "Contents/Home/" + bin, "zulu-8.jre/Contents/Home/" + bin}) {
            File file = new File(jre, path);
            if (file.isFile()) return file;
        }
        return null;
    }

    private static File fxJava() throws IOException {
        File jre = new File(dataDir(), "jre");
        File marker = new File(jre, ".kinetic-jre");
        if (marker.isFile() && JRE_VERSION.equals(new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim())) {
            File java = javaIn(jre);
            if (java != null) return java;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean arm = arch.contains("aarch64") || arch.contains("arm64");
        String suffix = windows() ? "-win_x64.zip" : os.contains("mac") ? (arm ? "-macosx_aarch64.tar.gz" : "-macosx_x64.tar.gz") : "-linux_x64.tar.gz";
        String url = "https://cdn.azul.com/zulu/bin/" + JRE_VERSION + suffix;

        File dir = dataDir();
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
        File archive = new File(dir, "jre-download" + (suffix.endsWith(".zip") ? ".zip" : ".tar.gz"));
        download(url, archive);
        File temp = new File(dir, "jre.tmp");
        deleteTree(temp);
        state = "Unpacking shop runtime...";
        if (suffix.endsWith(".zip")) {
            unzipStripTop(archive, temp);
        } else {
            if (!temp.mkdirs()) throw new IOException("cannot create " + temp);
            try {
                if (new ProcessBuilder("tar", "-xzf", archive.getAbsolutePath(), "--strip-components=1", "-C", temp.getAbsolutePath())
                        .inheritIO().start().waitFor() != 0) {
                    throw new IOException("tar failed");
                }
            } catch (InterruptedException e) {
                throw new IOException("interrupted");
            }
        }
        deleteTree(jre);
        if (!temp.renameTo(jre)) throw new IOException("cannot move " + temp + " to " + jre);
        Files.write(marker.toPath(), JRE_VERSION.getBytes(StandardCharsets.UTF_8));
        boolean ignored = archive.delete();
        File java = javaIn(jre);
        if (java == null) throw new IOException("Java missing after unpacking");
        if (!windows()) {
            File[] tools = java.getParentFile().listFiles();
            if (tools != null) for (File tool : tools) tool.setExecutable(true, false);
        }
        return java;
    }

    private static void download(String url, File target) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);
        connection.setRequestProperty("User-Agent", "Kinetic/1.8.9");
        long total = connection.getContentLengthLong();
        try (InputStream in = connection.getInputStream(); OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[65536];
            long done = 0;
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
                done += read;
                state = total > 0 ? "Downloading shop runtime (" + (done * 100 / total) + "%, only once)..." : "Downloading shop runtime...";
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void unzipStripTop(File archive, File target) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive.toPath()))) {
            ZipEntry entry;
            byte[] buffer = new byte[65536];
            String root = target.getCanonicalPath();
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                int slash = name.indexOf('/');
                if (slash < 0 || slash == name.length() - 1) continue;
                File out = new File(target, name.substring(slash + 1));
                if (!out.getCanonicalPath().startsWith(root)) continue;
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                out.getParentFile().mkdirs();
                try (OutputStream os = new FileOutputStream(out)) {
                    int read;
                    while ((read = zip.read(buffer)) > 0) os.write(buffer, 0, read);
                }
            }
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.isDirectory() ? file.listFiles() : null;
        if (children != null) for (File child : children) deleteTree(child);
        boolean ignored = file.delete();
    }

    private static void launchHost(File java) throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress("127.0.0.1", 0));
        int port = ((InetSocketAddress) server.getLocalAddress()).getPort();
        uploadedOnce = false;

        File self = new File(PandaWebView.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> command = new ArrayList<>();
        command.add(java.getAbsolutePath());
        command.add("-Xmx256m");
        command.add("-cp");
        command.add(self.getAbsolutePath());
        command.add(PandaWebHost.class.getName());
        command.add(String.valueOf(port));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        host = process;
        commands = new PrintWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8), true);
        if (sentW > 0) send("size " + sentW + " " + sentH);
        state = "Loading...";
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
        Thread frameReader = new Thread(() -> readFrames(server), "PandaAlts-WebView-frames");
        frameReader.setDaemon(true);
        frameReader.start();
        activeSent = true;
        // stop rendering while the shop is not on screen, the page itself stays loaded and logged in
        Thread idle = new Thread(() -> {
            while (process.isAlive()) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    return;
                }
                if (activeSent && System.currentTimeMillis() - lastDraw > 1000L) setActive(false);
            }
        }, "PandaAlts-WebView-idle");
        idle.setDaemon(true);
        idle.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(HOST_PREFIX)) continue;
                String payload = line.substring(HOST_PREFIX.length());
                if (payload.startsWith("state ")) {
                    state = payload.substring(6);
                    if ("Open".equals(state)) autoConnect();
                } else if (payload.startsWith("message ")) {
                    handleMessage(payload.substring(8));
                } else if (payload.startsWith("auth ")) {
                    handleAuth(payload.substring(5));
                } else if (payload.startsWith("purchase ")) {
                    handlePurchase(payload.substring(9), false);
                } else if (payload.startsWith("order ")) {
                    handlePurchase(payload.substring(6), true);
                } else if (payload.startsWith("login ")) {
                    handleLogin(payload.substring(6));
                }
            }
        }
        process.waitFor();
        autoConnectDone = false;
        connected = false;
        try {
            server.close();
        } catch (IOException ignored) {
        }
        host = null;
        commands = null;
        state = "The shop stopped - click to restart";
    }

    private static void readFrames(ServerSocketChannel server) {
        try (SocketChannel channel = server.accept()) {
            connected = true;
            ByteBuffer header = ByteBuffer.allocate(8);
            while (true) {
                header.clear();
                while (header.hasRemaining()) if (channel.read(header) < 0) return;
                header.flip();
                int w = header.getInt(), h = header.getInt();
                if (w <= 0 || h <= 0 || w > PandaWebHost.MAX_W || h > PandaWebHost.MAX_H) return;
                if (back == null) back = BufferUtils.createByteBuffer(PandaWebHost.MAX_PIXELS);
                back.clear();
                back.limit(w * h * 4);
                while (back.hasRemaining()) if (channel.read(back) < 0) return;
                synchronized (FRAME_LOCK) {
                    ByteBuffer swap = front;
                    front = back;
                    back = swap;
                    frontW = w;
                    frontH = h;
                    frameReady = true;
                }
            }
        } catch (IOException ignored) {
        } finally {
            connected = false;
        }
    }

    // ---- shop login through the API (embed docs 1b): no captcha inside the WebView ----

    private static volatile String authStatus = "";
    private static volatile String accountName = "";
    private static volatile String pendingId = "";
    private static volatile String refresh = "", apiKey = "";
    private static volatile boolean accountLoaded, autoConnectDone;

    public static String authStatus() {
        return authStatus;
    }

    public static boolean awaitingCode() {
        return !pendingId.isEmpty();
    }

    /** True when a login is saved, so the page logs itself in on every start. */
    public static boolean remembered() {
        loadAccount();
        return !refresh.isEmpty() || !apiKey.isEmpty();
    }

    public static String accountName() {
        return accountName;
    }

    public static void connectWithPassword(String username, String password) {
        accountName = username.trim();
        authStatus = "Logging in...";
        shop("login", username.trim(), password);
    }

    public static void connectWithKey(String key) {
        authStatus = "Logging in...";
        shop("key", key.replaceAll("\\s+", ""));
    }

    public static void submitCode(String code) {
        authStatus = "Checking code...";
        shop("code", pendingId, code.trim());
    }

    /** Forgets the saved login. The page session itself ends with the shop's own Logout button. */
    public static void forget() {
        refresh = apiKey = pendingId = "";
        authStatus = "";
        saveAccount();
    }

    private static void shop(String action, String... args) {
        StringBuilder command = new StringBuilder("shop ").append(action);
        for (String arg : args) command.append(' ').append(PandaShopActions.encode(arg));
        send(command.toString());
    }

    /** Logs the page in again with the saved login, once per helper start. */
    private static void autoConnect() {
        loadAccount();
        if (autoConnectDone) return;
        autoConnectDone = true;
        if (!apiKey.isEmpty()) shop("key", apiKey);
        else if (!refresh.isEmpty()) shop("refresh", refresh);
    }

    private static File accountFile() {
        return new File(new File(net.minecraft.client.Minecraft.getMinecraft().mcDataDir, secret.kinetic.Kinetic.NAME), "pandaalts-login.txt");
    }

    private static void loadAccount() {
        if (accountLoaded) return;
        accountLoaded = true;
        File file = accountFile();
        if (!file.isFile()) return;
        try {
            String decrypted = TokenEncryption.decrypt(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim());
            if (decrypted == null) return;
            JsonObject json = new JsonParser().parse(decrypted).getAsJsonObject();
            refresh = string(json, "refresh");
            apiKey = string(json, "key");
            accountName = string(json, "username");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveAccount() {
        JsonObject json = new JsonObject();
        json.addProperty("refresh", refresh);
        json.addProperty("key", apiKey);
        json.addProperty("username", accountName);
        try {
            File file = accountFile();
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), TokenEncryption.encrypt(json.toString()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String string(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }

    private static String errorText(JsonObject result) {
        JsonElement body = result.get("body");
        if (body != null && body.isJsonObject()) {
            JsonObject object = body.getAsJsonObject();
            JsonElement data = object.get("data");
            // the shop nests errors as {"success":false,"data":{"error":"..."}}
            if (data != null && data.isJsonObject()) object = data.getAsJsonObject();
            String message = string(object, "error");
            if (message.isEmpty()) message = string(object, "message");
            if (!message.isEmpty()) return message;
        }
        int status = result.has("status") ? result.get("status").getAsInt() : 0;
        if (status == 429) return "Too many tries - wait a minute";
        if (status == 401 || status == 403) return "Wrong username, password or key";
        return "Login failed" + (status > 0 ? " (HTTP " + status + ")" : "");
    }

    private static void handleAuth(String json) {
        JsonObject result;
        try {
            result = new JsonParser().parse(json).getAsJsonObject();
        } catch (RuntimeException e) {
            return;
        }
        switch (string(result, "step")) {
            case "done": {
                String key = string(result, "key"), newRefresh = string(result, "refresh");
                if (!key.isEmpty()) apiKey = key;
                if (!newRefresh.isEmpty()) refresh = newRefresh;
                String name = string(result, "username");
                if (!name.isEmpty()) accountName = name;
                pendingId = "";
                authStatus = "Logged in";
                saveAccount();
                break;
            }
            case "code":
                pendingId = string(result, "pending_id");
                authStatus = "Enter the code from your email";
                break;
            case "expired":
                refresh = "";
                authStatus = "Saved login expired - log in again";
                saveAccount();
                break;
            default:
                authStatus = errorText(result);
                break;
        }
    }

    // ---- bought accounts: the page's purchase answers are handed to the alt manager to log in ----

    /** Receives what a purchase in the shop page delivered. */
    public interface Deliveries {
        void delivered(String orderId, List<LocaltsClient.OrderItem> items);

        void status(String message, boolean error);

        /** The "Log in" button on an account in the shop page, e.g. one of an older order. */
        void login(LocaltsClient.OrderItem item);
    }

    private static volatile Deliveries deliveries;
    // orders bought in this session that were paid but not delivered yet, and orders already logged in
    private static final Set<String> awaiting = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> delivered = Collections.synchronizedSet(new HashSet<>());

    public static void setDeliveries(Deliveries listener) {
        deliveries = listener;
    }

    private static void deliveryStatus(String message, boolean error) {
        Deliveries listener = deliveries;
        if (listener != null) listener.status(message, error);
    }

    /**
     * A purchase answer, or (lookup) one of the page's order lookups. Lookups only count for an order bought in
     * this session that is still awaited, so opening old orders in the shop never logs anything in.
     */
    private static void handlePurchase(String json, boolean lookup) {
        JsonObject root;
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject()) return;
            root = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            return;
        }
        // failed purchases (no stock, no credits, ...) are already shown by the page itself
        JsonElement success = root.get("success");
        if (success != null && success.isJsonPrimitive() && !success.getAsBoolean()) return;
        JsonElement data = root.get("data");
        JsonObject order = data != null && data.isJsonObject() ? data.getAsJsonObject() : root;

        String orderId = orderId(order);
        if (lookup && !awaiting.contains(orderId)) return;
        if (!orderId.isEmpty() && delivered.contains(orderId)) return;

        JsonObject transaction = object(order, "transaction");
        if (transaction != null && bool(transaction, "pending_refunded")) {
            awaiting.remove(orderId);
            deliveryStatus("Order " + orderId + " could not be delivered - credits were refunded", true);
            return;
        }
        if (PandaAltsClient.isPending(order)) {
            // the page keeps polling the order (every 5s), its answers come back here as lookups
            if (!orderId.isEmpty() && awaiting.add(orderId)) {
                deliveryStatus("Paid - waiting for PandaAlts to deliver order " + orderId + "...", false);
            }
            return;
        }
        awaiting.remove(orderId);
        if (!orderId.isEmpty()) delivered.add(orderId);
        List<LocaltsClient.OrderItem> items = PandaAltsClient.deliverables(order);
        if (items.isEmpty()) {
            deliveryStatus("Order " + (orderId.isEmpty() ? "" : orderId + " ") + "delivered nothing", true);
            return;
        }
        Deliveries listener = deliveries;
        if (listener != null) listener.delivered(orderId.isEmpty() ? "?" : orderId, items);
    }

    private static void handleLogin(String json) {
        JsonObject product;
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject()) return;
            product = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            return;
        }
        // same reading as a delivery, so a login in a field of its own (access_token, ...) is kept
        JsonArray products = new JsonArray();
        products.add(product);
        JsonObject order = new JsonObject();
        order.add("products", products);
        List<LocaltsClient.OrderItem> items = PandaAltsClient.deliverables(order);
        Deliveries listener = deliveries;
        if (!items.isEmpty() && listener != null) listener.login(items.get(0));
    }

    /** The shop's own transaction id, which the page also uses to look the order up (not the provider's id). */
    private static String orderId(JsonObject order) {
        JsonObject transaction = object(order, "transaction");
        String id = transaction == null ? "" : string(transaction, "id");
        if (id.isEmpty()) id = string(order, "purchase_id");
        return id.isEmpty() ? PandaAltsClient.purchaseId(order) : id;
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static boolean bool(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean() && value.getAsBoolean();
    }

    private static void handleMessage(String json) {
        try {
            JsonElement parsed = new JsonParser().parse(json);
            if (!parsed.isJsonObject()) return;
            JsonObject msg = parsed.getAsJsonObject();
            String type = msg.has("type") ? msg.get("type").getAsString() : "";
            switch (type) {
                case "ready":
                    state = user.isEmpty() ? "Ready" : "Logged in";
                    break;
                case "login":
                    user = msg.has("username") ? msg.get("username").getAsString() : "";
                    pendingId = "";
                    state = "Logged in";
                    break;
                case "logout":
                    user = "";
                    state = "Logged out";
                    break;
                case "route":
                    view = msg.has("view") ? msg.get("view").getAsString() : "";
                    break;
                default:
                    break;
            }
        } catch (RuntimeException ignored) {
        }
    }
}
