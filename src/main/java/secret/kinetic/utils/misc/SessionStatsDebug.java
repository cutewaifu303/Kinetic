package secret.kinetic.utils.misc;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;



























public final class SessionStatsDebug {

    
    public static boolean chat = true;

    
    public static boolean file = true;

    public static final String FILE_NAME = "kinetic_session_debug.log";

    
    private static final long HEARTBEAT_MS = 5000L;

    
    private static final long MAX_BYTES = 256L * 1024L;

    






    private static final char COLOUR = (char) 0x00A7;

    private static long trackedSessionStart = Long.MIN_VALUE;
    private static int lastKills;
    private static int lastDeaths;
    private static int lastWins;
    private static long lastHeartbeatAt;
    private static long publishCalls;
    private static String lastJson;
    private static String lastFailure;

    private SessionStatsDebug() {
    }

    




    public static void sample(File gameDir, String username, String server,
                              int kills, int deaths, int wins, long sessionStart) {
        try {
            publishCalls++;

            
            
            
            if (sessionStart != trackedSessionStart) {
                trackedSessionStart = sessionStart;
                lastKills = kills;
                lastDeaths = deaths;
                lastWins = wins;
                publishCalls = 1L;
                lastHeartbeatAt = 0L;
                lastJson = null;
                lastFailure = null;
                truncate(gameDir);
                write(gameDir, "session start"
                        + " | user=" + username
                        + " | server=" + server
                        + " | kills=" + kills + " deaths=" + deaths + " wins=" + wins);
                write(gameDir, "writing stats to "
                        + path(gameDir, SessionStatsExporter.FILE_NAME));
                write(gameDir, "logging to " + path(gameDir, FILE_NAME));
                say("Session stats logging started - " + FILE_NAME);
            }

            if (kills != lastKills) {
                change(gameDir, kills > lastKills ? "KILL" : "kills reset",
                        "kills", lastKills, kills);
                lastKills = kills;
            }
            if (deaths != lastDeaths) {
                change(gameDir, deaths > lastDeaths ? "DEATH" : "deaths reset",
                        "deaths", lastDeaths, deaths);
                lastDeaths = deaths;
            }
            if (wins != lastWins) {
                change(gameDir, wins > lastWins ? "WIN" : "wins reset",
                        "wins", lastWins, wins);
                lastWins = wins;
            }

            long now = System.currentTimeMillis();
            if (now - lastHeartbeatAt >= HEARTBEAT_MS) {
                lastHeartbeatAt = now;
                write(gameDir, "alive | publish calls=" + publishCalls
                        + " | kills=" + kills + " deaths=" + deaths + " wins=" + wins
                        + " | server=" + server + " | " + health());
            }
        } catch (Throwable ignored) {
            
        }
    }

    




    public static void wrote(File gameDir, String json) {
        if (json == null || json.equals(lastJson)) {
            return;
        }
        lastJson = json;
        write(gameDir, "wrote " + json);
    }

    
    public static void failed(File gameDir, String what, Throwable error) {
        String detail = error == null ? "no exception"
                : error.getClass().getName() + ": " + error.getMessage();
        write(gameDir, "FAILED " + what + " | " + detail);
        
        String key = what + "|" + detail;
        if (!key.equals(lastFailure)) {
            lastFailure = key;
            say(COLOUR + "cSession stats: " + what + " failed - " + detail);
        }
    }

    
    public static void note(File gameDir, String message) {
        write(gameDir, message);
    }

    private static void change(File gameDir, String label, String field,
                               int from, int to) {
        write(gameDir, label + " | " + field + " " + from + " -> " + to
                + " | " + health());
        say(label + " - " + field + " now " + to);
    }

    



    private static String health() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return "no player";
            }
            return "health=" + mc.thePlayer.getHealth()
                    + " isDead=" + mc.thePlayer.isDead;
        } catch (Throwable ignored) {
            return "health unavailable";
        }
    }

    private static void say(String message) {
        if (!chat) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null) {
                return;
            }


        } catch (Throwable ignored) {
            
        }
    }

    private static String path(File gameDir, String name) {
        try {
            return new File(gameDir, name).getAbsolutePath();
        } catch (Throwable ignored) {
            return name;
        }
    }

    private static void truncate(File gameDir) {
        try {
            File target = new File(gameDir, FILE_NAME);
            if (target.isFile() && !target.delete()) {
                
                
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    

    private static void write(File gameDir, String message) {
        if (!file || gameDir == null) {
            return;
        }
        Writer out = null;
        try {
            if (!gameDir.isDirectory() && !gameDir.mkdirs()) {
                return;
            }
            File target = new File(gameDir, FILE_NAME);
            boolean append = target.isFile() && target.length() < MAX_BYTES;
            out = new OutputStreamWriter(new FileOutputStream(target, append), "UTF-8");
            out.write(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()));
            out.write("  ");
            out.write(message == null ? "null" : message);
            out.write("\r\n");
            out.flush();
        } catch (Throwable ignored) {
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
