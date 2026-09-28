package secret.kinetic.utils.misc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;































public final class SessionStatsExporter {

    static final String FILE_NAME = "kinetic_session.json";
    private static final long WRITE_INTERVAL_MS = 1000L;

    private static long lastWriteAt;

    private static String lastUsername = "";
    private static String lastServer = "";
    private static int lastKills;
    private static int lastDeaths;
    private static int lastWins;
    private static long lastSessionMs;

    private SessionStatsExporter() {
    }

    
    public static void publish(File gameDir, String username, String server,
                               int kills, int deaths, int wins, long sessionStart) {
        
        
        
        SessionStatsDebug.sample(gameDir, username, server,
                kills, deaths, wins, sessionStart);

        long now = System.currentTimeMillis();

        
        
        
        
        
        
        lastUsername = username == null ? "" : username;
        lastServer = server == null ? "" : server;
        lastKills = kills;
        lastDeaths = deaths;
        lastWins = wins;
        lastSessionMs = Math.max(0L, now - sessionStart);

        if (now - lastWriteAt < WRITE_INTERVAL_MS) {
            return;
        }
        lastWriteAt = now;

        write(gameDir, true);
    }

    



    public static void publishStopped(File gameDir) {
        lastWriteAt = 0L;
        SessionStatsDebug.note(gameDir, "session stopped - client shutting down,"
                + " final kills=" + lastKills + " deaths=" + lastDeaths
                + " wins=" + lastWins);
        write(gameDir, false);
    }

    private static void write(File gameDir, boolean alive) {
        if (gameDir == null) {
            return;
        }

        StringBuilder json = new StringBuilder(160);
        json.append("{\"schema\":1")
                .append(",\"alive\":").append(alive)
                .append(",\"username\":\"").append(escape(lastUsername)).append('"')
                .append(",\"server\":\"").append(escape(lastServer)).append('"')
                .append(",\"kills\":").append(lastKills)
                .append(",\"deaths\":").append(lastDeaths)
                .append(",\"wins\":").append(lastWins)
                .append(",\"sessionMs\":").append(lastSessionMs)
                .append('}');

        Writer out = null;
        try {
            if (!gameDir.isDirectory() && !gameDir.mkdirs()) {
                SessionStatsDebug.failed(gameDir, "create game dir", null);
                return;
            }
            out = new OutputStreamWriter(
                    new FileOutputStream(new File(gameDir, FILE_NAME), false), "UTF-8");
            out.write(json.toString());
            out.flush();
            SessionStatsDebug.wrote(gameDir, json.toString());
        } catch (Throwable error) {
            
            
            
            SessionStatsDebug.failed(gameDir, "write " + FILE_NAME, error);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    
    private static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c >= ' ' && c != 0x7F) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
