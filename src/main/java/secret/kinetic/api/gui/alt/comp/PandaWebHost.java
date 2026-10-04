package secret.kinetic.api.gui.alt.comp;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The helper process behind the in-game PandaAlts shop. It runs on a Java 8 with JavaFX, keeps the shop's
 * {@code /embed} page in an offscreen WebView, streams rendered frames over a loopback socket the game draws
 * as a texture, and replays the game's mouse and keyboard input into the page.
 *
 * Frames are int width, int height, then the BGRA (premultiplied) pixels. A frame is only sent when the page
 * actually changed, so a page that just sits there costs nothing on the game's side.
 *
 * Commands on stdin, one per line: {@code size W H}, {@code move X Y}, {@code press X Y BUTTON},
 * {@code release X Y BUTTON}, {@code scroll X Y DELTA}, {@code key NAME CHARHEX SHIFT CTRL ALT},
 * {@code active 0|1}, {@code reload}. The process exits when stdin closes, i.e. when the game is gone.
 * Events go back on stdout prefixed with {@code [panda-webview]}: {@code state}, {@code message} (the page's
 * postMessage events), {@code auth} (results of the shop actions), {@code purchase} and {@code order} (the answers
 * of the page's purchase and order lookups, so the game can log the bought account in) and {@code login} (the
 * "Log in" button the bridge adds to every delivered account, also on old orders).
 *
 * JavaFX is only touched through reflection so the client still compiles and runs without it.
 */
public final class PandaWebHost {

    public static final int MAX_W = 2560, MAX_H = 1600;
    public static final int MAX_PIXELS = MAX_W * MAX_H * 4;

    private static final String PREFIX = "[panda-webview] ";
    private static final String SHOP_ORIGIN = "https://altshop.pandaservice.eu/";
    private static final String BRIDGE_SCRIPT =
            "(function(){if(window.__kineticBridge)return;window.__kineticBridge=true;"
                    + "window.addEventListener('message',function(e){var m=e.data||{};"
                    + "if(m.source!=='pandaservice-shop')return;"
                    + "try{window.kinetic.message(JSON.stringify(m));}catch(x){}});"
                    // the page posts no purchase event, so the answers of its own purchase calls are passed on
                    // (deliverables included) for the game to log the bought account in: POST /api/app/purchase,
                    // and GET /api/app/purchases/:id, which the page polls every 5s while an order is pending
                    + "function buy(m,u){return String(m||'GET').toUpperCase()==='POST'&&/purchase/i.test(String(u||''))"
                    + "&&!/quote|progress|preorder/i.test(String(u||''));}"
                    + "function look(m,u){return String(m||'GET').toUpperCase()==='GET'&&/\\/purchases\\/[^\\/?#]+([?#].*)?$/.test(String(u||''));}"
                    + "function pass(t){try{if(t)window.kinetic.purchase(String(t));}catch(x){}}"
                    + "function seen(t){try{if(t)window.kinetic.order(String(t));}catch(x){}}"
                    + "var f=window.fetch;if(f){window.fetch=function(i,o){var u=typeof i==='string'?i:(i&&i.url)||'';"
                    + "var m=(o&&o.method)||(i&&i.method)||'GET';var p=f.apply(this,arguments);"
                    + "var h=buy(m,u)?pass:look(m,u)?seen:null;var keep=/\\/api\\/app\\/purchase/i.test(u)&&!/quote|progress|download/i.test(u);"
                    + "if(h||keep)p.then(function(r){r.clone().text().then(function(t){if(h)h(t);if(keep)remember(t);},function(){});},function(){});"
                    + "return p;};}"
                    // every delivered account the page shows (a new purchase or an old order under Purchases) gets
                    // a "Log in" button next to Copy/Download. The page is an ES module, so its own functions are out
                    // of reach: the order answers it loaded are kept here and matched to the drawn cards by title,
                    // the same way the page titles them
                    + "var K=[];function list(p){var q=p&&(p.purchase||p.response)||p||{};"
                    + "var c=[q.products,q.items,q.deliverables,q.accounts,q.purchase&&q.purchase.products,p&&p.products,p&&p.deliverables];"
                    + "for(var i=0;i<c.length;i++)if(Array.isArray(c[i]))return c[i];return [];}"
                    + "function title(o,i){o=o&&typeof o==='object'?o:{};var ks=['username','name','email','login','account','mc_username'];"
                    + "for(var k=0;k<ks.length;k++){var v=o[ks[k]];if(v!==undefined&&v!==null&&v!=='')return String(v);}return 'Deliverable '+(i+1);}"
                    + "function remember(t){try{var j=JSON.parse(t),l=list(j&&j.data||j);if(l.length){K.unshift(l);if(K.length>10)K.pop();tag();}}catch(e){}}"
                    + "function tag(){var cards=document.querySelectorAll('.deliverable-card'),groups=[];"
                    + "for(var i=0;i<cards.length;i++){var g=cards[i].parentNode;if(groups.indexOf(g)<0)groups.push(g);}"
                    + "groups.forEach(function(g){var cs=[].filter.call(g.children,function(c){return c.classList&&c.classList.contains('deliverable-card');});"
                    + "var l=K.filter(function(l){return l.length===cs.length&&cs.every(function(c,i){var s=c.querySelector('.deliverable-main strong');"
                    + "return s&&s.textContent===title(l[i],i);});})[0];if(!l)return;"
                    + "cs.forEach(function(c,i){var a=c.querySelector('.deliverable-actions');if(!a||a.querySelector('[data-kinetic-login]'))return;"
                    + "var raw=l[i]&&typeof l[i]==='object'?l[i]:{data:l[i]},b=document.createElement('button');b.type='button';"
                    + "b.setAttribute('data-kinetic-login','');b.textContent='Log in';b.addEventListener('click',function(){"
                    + "try{window.kinetic.login(JSON.stringify(raw));}catch(e){}b.textContent='Logging in...';"
                    + "setTimeout(function(){b.textContent='Log in';},3000);});a.insertBefore(b,a.firstChild);});});}"
                    + "var tq=0,wo=false;function watch(){if(wo||!document.body)return;wo=true;new MutationObserver(function(){if(K.length&&!tq)"
                    + "tq=setTimeout(function(){tq=0;tag();},50);}).observe(document.body,{childList:true,subtree:true});}"
                    + "watch();document.addEventListener('DOMContentLoaded',watch);window.addEventListener('load',watch);"
                    + "var X=window.XMLHttpRequest;if(X){var op=X.prototype.open,sd=X.prototype.send;"
                    + "X.prototype.open=function(m,u){this.__kineticBuy=buy(m,u);return op.apply(this,arguments);};"
                    + "X.prototype.send=function(){var x=this;if(x.__kineticBuy)x.addEventListener('load',function(){pass(x.responseText);});"
                    + "return sd.apply(this,arguments);};}})();";

    private static SocketChannel socket;
    private static ByteBuffer pixels;
    private static final ByteBuffer frameHeader = ByteBuffer.allocate(8);
    private static long lastHash;
    private static int lastW, lastH;
    private static Object webView, engine, stage, image;
    private static volatile int width = 1000, height = 700;
    private static volatile boolean active = true;
    private static final AtomicBoolean snapshotQueued = new AtomicBoolean();
    private static boolean primaryDown, secondaryDown, middleDown;

    private PandaWebHost() {
    }

    public static void main(String[] args) throws Exception {
        socket = SocketChannel.open(new InetSocketAddress("127.0.0.1", Integer.parseInt(args[0])));
        socket.socket().setTcpNoDelay(true);

        Class.forName("javafx.embed.swing.JFXPanel").newInstance();
        Class<?> platform = Class.forName("javafx.application.Platform");
        platform.getMethod("setImplicitExit", boolean.class).invoke(null, false);
        runLater(PandaWebHost::createView);

        Thread snapshots = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(40L);
                } catch (InterruptedException e) {
                    return;
                }
                if (active && webView != null && snapshotQueued.compareAndSet(false, true)) {
                    try {
                        runLater(PandaWebHost::snapshot);
                    } catch (Exception e) {
                        snapshotQueued.set(false);
                    }
                }
            }
        }, "panda-snapshots");
        snapshots.setDaemon(true);
        snapshots.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = in.readLine()) != null) {
            final String command = line.replace("\uFEFF", "").trim();
            if (command.isEmpty()) continue;
            runLater(() -> handle(command));
        }
        System.exit(0);
    }

    static void report(String text) {
        System.out.println(PREFIX + text);
        System.out.flush();
    }

    // ---- view ----

    private static void createView() {
        try {
            report("state Loading...");
            webView = Class.forName("javafx.scene.web.WebView").newInstance();
            engine = call(webView, "getEngine");
            call(engine, "setUserAgent", PandaWebView.USER_AGENT);

            Class<?> listenerType = Class.forName("javafx.beans.value.ChangeListener");
            Object loadListener = listener(listenerType, value -> {
                if ("SUCCEEDED".equals(value)) {
                    installBridge();
                    report("state Open");
                } else if ("FAILED".equals(value)) {
                    report("state Could not load the shop");
                }
            });
            call(call(call(engine, "getLoadWorker"), "stateProperty"), "addListener", loadListener);
            call(call(engine, "documentProperty"), "addListener", listener(listenerType, value -> installBridge()));

            // a Group root lets the WebView keep its own size instead of following the (off-screen) window
            Object group = Class.forName("javafx.scene.Group").newInstance();
            Object children = call(group, "getChildren");
            call(children, "add", webView);
            Object scene = construct("javafx.scene.Scene", group, (double) width, (double) height);
            stage = Class.forName("javafx.stage.Stage").newInstance();
            Object utility = staticField("javafx.stage.StageStyle", "UTILITY");
            call(stage, "initStyle", utility);
            call(stage, "setScene", scene);
            // the window only exists so WebKit renders, it lives far off-screen and never takes focus from the game
            call(stage, "setX", -32000d);
            call(stage, "setY", -32000d);
            call(stage, "setOpacity", 0.0d);
            resize(width, height);
            call(stage, "show");
            focusPage();
            call(engine, "load", PandaWebView.URL);
        } catch (Throwable t) {
            t.printStackTrace();
            report("state WebView failed");
            System.exit(1);
        }
    }

    private interface ValueHandler {
        void changed(String value);
    }

    private static Object listener(Class<?> type, ValueHandler handler) {
        return Proxy.newProxyInstance(PandaWebHost.class.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "PandaWebHost listener";
                default:
                    if (args != null && args.length == 3 && args[2] != null) handler.changed(args[2].toString());
                    return null;
            }
        });
    }

    private static void installBridge() {
        try {
            Object window = call(engine, "executeScript", "window");
            Class.forName("netscape.javascript.JSObject").getMethod("setMember", String.class, Object.class)
                    .invoke(window, "kinetic", Bridge.INSTANCE);
            call(engine, "executeScript", BRIDGE_SCRIPT);
        } catch (Throwable ignored) {
        }
    }

    /** The page only shows a caret and takes keys while WebKit thinks it is focused; the hidden window never is. */
    private static void focusPage() {
        try {
            call(webView, "requestFocus");
            Field page = Class.forName("javafx.scene.web.WebView").getDeclaredField("page");
            page.setAccessible(true);
            Object webPage = page.get(webView);
            Method setFocused = webPage.getClass().getMethod("setFocused", boolean.class);
            setFocused.setAccessible(true);
            setFocused.invoke(webPage, true);
        } catch (Throwable ignored) {
        }
    }

    private static void resize(int w, int h) throws Exception {
        width = Math.max(200, Math.min(MAX_W, w));
        height = Math.max(150, Math.min(MAX_H, h));
        call(webView, "setPrefSize", (double) width, (double) height);
        call(webView, "setMinSize", (double) width, (double) height);
        call(webView, "setMaxSize", (double) width, (double) height);
        call(webView, "resize", (double) width, (double) height);
        call(stage, "setWidth", (double) width + 16);
        call(stage, "setHeight", (double) height + 40);
        image = null;
    }

    private static void snapshot() {
        try {
            if (webView == null) return;
            // the wanted size can arrive before the view exists, so keep reconciling it
            if (((Number) call(webView, "getWidth")).intValue() != width || ((Number) call(webView, "getHeight")).intValue() != height) {
                resize(width, height);
            }
            Object params = null;
            image = call(webView, "snapshot", params, image);
            int w = ((Number) call(image, "getWidth")).intValue();
            int h = ((Number) call(image, "getHeight")).intValue();
            w = Math.min(w, MAX_W);
            h = Math.min(h, MAX_H);
            int bytes = w * h * 4;
            if (pixels == null || pixels.capacity() < bytes) pixels = ByteBuffer.allocateDirect(MAX_PIXELS);
            pixels.clear();
            pixels.limit(bytes);
            Object format = Class.forName("javafx.scene.image.PixelFormat").getMethod("getByteBgraPreInstance").invoke(null);
            Object reader = call(image, "getPixelReader");
            call(reader, "getPixels", 0, 0, w, h, format, pixels, w * 4);

            // only send frames that changed (hashing every pixel, so a blinking caret or one typed letter counts)
            long hash = 1125899906842597L;
            int i = 0;
            for (; i + 8 <= bytes; i += 8) hash = 31 * hash + pixels.getLong(i);
            for (; i < bytes; i++) hash = 31 * hash + pixels.get(i);
            if (hash == lastHash && w == lastW && h == lastH) return;
            lastHash = hash;
            lastW = w;
            lastH = h;

            frameHeader.clear();
            frameHeader.putInt(w).putInt(h).flip();
            while (frameHeader.hasRemaining()) socket.write(frameHeader);
            pixels.position(0).limit(bytes);
            while (pixels.hasRemaining()) socket.write(pixels);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            snapshotQueued.set(false);
        }
    }

    // ---- input ----

    private static void handle(String command) {
        try {
            String[] p = command.split(" ");
            switch (p[0]) {
                case "size":
                    width = Math.max(200, Math.min(MAX_W, Integer.parseInt(p[1])));
                    height = Math.max(150, Math.min(MAX_H, Integer.parseInt(p[2])));
                    if (webView != null) resize(width, height);
                    break;
                case "active":
                    active = "1".equals(p[1]);
                    if (active) focusPage();
                    break;
                case "reload":
                    call(engine, "reload");
                    break;
                case "shop": {
                    // one of the fixed PandaService actions, never free-form script
                    String script = PandaShopActions.script(p[1], Arrays.copyOfRange(p, 2, p.length));
                    if (script != null) {
                        installBridge();
                        call(engine, "executeScript", script);
                    }
                    break;
                }
                case "move":
                    mouse(primaryDown || secondaryDown || middleDown ? "MOUSE_DRAGGED" : "MOUSE_MOVED", d(p[1]), d(p[2]), "NONE", 0);
                    break;
                case "press": {
                    String button = button(p[3]);
                    setDown(button, true);
                    focusPage();
                    mouse("MOUSE_PRESSED", d(p[1]), d(p[2]), button, 1);
                    break;
                }
                case "release": {
                    String button = button(p[3]);
                    mouse("MOUSE_RELEASED", d(p[1]), d(p[2]), button, 1);
                    setDown(button, false);
                    mouse("MOUSE_CLICKED", d(p[1]), d(p[2]), button, 1);
                    break;
                }
                case "scroll":
                    scroll(d(p[1]), d(p[2]), d(p[3]));
                    break;
                case "key":
                    key(p[1], p.length > 2 ? unhex(p[2]) : "", p.length > 3 && "1".equals(p[3]), p.length > 4 && "1".equals(p[4]),
                            p.length > 5 && "1".equals(p[5]));
                    break;
                default:
                    break;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }

    private static String button(String index) {
        switch (index) {
            case "1":
                return "SECONDARY";
            case "2":
                return "MIDDLE";
            default:
                return "PRIMARY";
        }
    }

    private static void setDown(String button, boolean down) {
        if ("PRIMARY".equals(button)) primaryDown = down;
        else if ("SECONDARY".equals(button)) secondaryDown = down;
        else middleDown = down;
    }

    private static void fire(Object event) throws Exception {
        Class<?> eventClass = Class.forName("javafx.event.Event");
        Class<?> target = Class.forName("javafx.event.EventTarget");
        eventClass.getMethod("fireEvent", target, eventClass).invoke(null, webView, event);
    }

    private static void mouse(String type, double x, double y, String button, int clicks) throws Exception {
        Object eventType = staticField("javafx.scene.input.MouseEvent", type);
        Object mouseButton = staticField("javafx.scene.input.MouseButton", button);
        Object event = construct("javafx.scene.input.MouseEvent", eventType, x, y, x, y, mouseButton, clicks,
                false, false, false, false, primaryDown, middleDown, secondaryDown, false, false, true, null);
        fire(event);
    }

    private static void scroll(double x, double y, double delta) throws Exception {
        Object eventType = staticField("javafx.scene.input.ScrollEvent", "SCROLL");
        Object hUnits = staticField("javafx.scene.input.ScrollEvent$HorizontalTextScrollUnits", "NONE");
        Object vUnits = staticField("javafx.scene.input.ScrollEvent$VerticalTextScrollUnits", "NONE");
        Object event = construct("javafx.scene.input.ScrollEvent", eventType, x, y, x, y, false, false, false, false, false, false,
                0d, delta, 0d, delta, hUnits, 0d, vUnits, 0d, 0, null);
        fire(event);
    }

    private static final Map<String, String> KEYS = new HashMap<>();

    static {
        String[][] pairs = {{"RETURN", "ENTER"}, {"NUMPADENTER", "ENTER"}, {"BACK", "BACK_SPACE"}, {"TAB", "TAB"}, {"SPACE", "SPACE"},
                {"ESCAPE", "ESCAPE"}, {"DELETE", "DELETE"}, {"LEFT", "LEFT"}, {"RIGHT", "RIGHT"}, {"UP", "UP"}, {"DOWN", "DOWN"},
                {"HOME", "HOME"}, {"END", "END"}, {"PRIOR", "PAGE_UP"}, {"NEXT", "PAGE_DOWN"}, {"INSERT", "INSERT"},
                {"PERIOD", "PERIOD"}, {"COMMA", "COMMA"}, {"MINUS", "MINUS"}, {"EQUALS", "EQUALS"}, {"SLASH", "SLASH"},
                {"SEMICOLON", "SEMICOLON"}, {"APOSTROPHE", "QUOTE"}, {"LBRACKET", "OPEN_BRACKET"}, {"RBRACKET", "CLOSE_BRACKET"},
                {"BACKSLASH", "BACK_SLASH"}, {"GRAVE", "BACK_QUOTE"}};
        for (String[] pair : pairs) KEYS.put(pair[0], pair[1]);
    }

    private static String keyCode(String lwjglName) {
        String mapped = KEYS.get(lwjglName);
        if (mapped != null) return mapped;
        if (lwjglName.length() == 1 && Character.isLetter(lwjglName.charAt(0))) return lwjglName.toUpperCase();
        if (lwjglName.length() == 1 && Character.isDigit(lwjglName.charAt(0))) return "DIGIT" + lwjglName;
        if (lwjglName.matches("F[0-9]{1,2}")) return lwjglName;
        return "UNDEFINED";
    }

    private static void key(String lwjglName, String character, boolean shift, boolean ctrl, boolean alt) throws Exception {
        Object code = staticField("javafx.scene.input.KeyCode", keyCode(lwjglName));
        Object undefined = staticField("javafx.scene.input.KeyCode", "UNDEFINED");
        Object pressed = staticField("javafx.scene.input.KeyEvent", "KEY_PRESSED");
        Object typed = staticField("javafx.scene.input.KeyEvent", "KEY_TYPED");
        Object released = staticField("javafx.scene.input.KeyEvent", "KEY_RELEASED");
        String undefinedChar = (String) staticField("javafx.scene.input.KeyEvent", "CHAR_UNDEFINED");
        fire(construct("javafx.scene.input.KeyEvent", pressed, undefinedChar, "", code, shift, ctrl, alt, false));
        if (!character.isEmpty() && !ctrl && !alt) {
            fire(construct("javafx.scene.input.KeyEvent", typed, character, "", undefined, shift, ctrl, alt, false));
        }
        fire(construct("javafx.scene.input.KeyEvent", released, undefinedChar, "", code, shift, ctrl, alt, false));
    }

    private static String unhex(String hex) {
        if (hex.isEmpty() || "-".equals(hex)) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 4 <= hex.length(); i += 4) sb.append((char) Integer.parseInt(hex.substring(i, i + 4), 16));
        return sb.toString();
    }

    // ---- reflection helpers (JavaFX hands out private implementation classes, so only public types are used) ----

    private static void runLater(Runnable runnable) throws Exception {
        Class.forName("javafx.application.Platform").getMethod("runLater", Runnable.class).invoke(null, runnable);
    }

    private static Object staticField(String className, String name) throws Exception {
        return Class.forName(className).getField(name).get(null);
    }

    private static Object construct(String className, Object... args) throws Exception {
        for (Constructor<?> constructor : Class.forName(className).getConstructors()) {
            if (fits(constructor.getParameterTypes(), args)) return constructor.newInstance(args);
        }
        throw new NoSuchMethodException(className + " constructor with " + args.length + " args");
    }

    static Object call(Object target, String method, Object... args) throws Exception {
        for (Method candidate : publicMethods(target.getClass())) {
            if (candidate.getName().equals(method) && fits(candidate.getParameterTypes(), args)) return candidate.invoke(target, args);
        }
        throw new NoSuchMethodException(target.getClass().getName() + "." + method);
    }

    private static boolean fits(Class<?>[] params, Object[] args) {
        if (params.length != args.length) return false;
        for (int i = 0; i < params.length; i++) {
            if (args[i] == null) {
                if (params[i].isPrimitive()) return false;
                continue;
            }
            if (!wrap(params[i]).isAssignableFrom(args[i].getClass())) return false;
        }
        return true;
    }

    private static List<Method> publicMethods(Class<?> type) {
        List<Method> found = new ArrayList<>();
        Set<Class<?>> seen = new HashSet<>();
        ArrayDeque<Class<?>> queue = new ArrayDeque<>();
        queue.add(type);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (!seen.add(current)) continue;
            if (Modifier.isPublic(current.getModifiers())) {
                for (Method m : current.getMethods()) {
                    if (Modifier.isPublic(m.getDeclaringClass().getModifiers())) found.add(m);
                }
            }
            if (current.getSuperclass() != null) queue.add(current.getSuperclass());
            queue.addAll(Arrays.asList(current.getInterfaces()));
        }
        return found;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        return type;
    }

    /** Receives the shop's postMessage events and forwards them to the game. Public for the page's JavaScript. */
    public static final class Bridge {
        public static final Bridge INSTANCE = new Bridge();

        private Bridge() {
        }

        public void message(String json) {
            report("message " + json.replace('\n', ' '));
        }

        /** Results of the shop actions (login, purchase lookups). */
        public void result(String json) {
            report("auth " + json.replace('\n', ' '));
        }

        /** The answer of a purchase call the shop page made. Only taken from the shop itself, never another site. */
        public void purchase(String json) {
            if (onShop()) report("purchase " + json.replace('\n', ' ').replace('\r', ' '));
        }

        /** The answer of an order lookup the shop page made (it polls pending orders until they are delivered). */
        public void order(String json) {
            if (onShop()) report("order " + json.replace('\n', ' ').replace('\r', ' '));
        }

        /** The "Log in" button on a delivered account in the shop page: the product as the page has it. */
        public void login(String json) {
            if (onShop()) report("login " + json.replace('\n', ' ').replace('\r', ' '));
        }

        private static boolean onShop() {
            try {
                Object location = call(engine, "getLocation");
                return location != null && location.toString().startsWith(SHOP_ORIGIN);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
