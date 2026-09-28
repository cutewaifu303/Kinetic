package secret.kinetic.managers.impl;

import secret.kinetic.api.events.annotations.EventHook;
import secret.kinetic.api.events.impl.client.ClientTickEvent;
import secret.kinetic.api.events.impl.client.GameStoppingEvent;
import secret.kinetic.api.events.impl.client.PacketReceivedEvent;
import secret.kinetic.api.events.impl.player.KillEvent;
import secret.kinetic.api.events.impl.player.PlayerDeathEvent;
import secret.kinetic.utils.misc.SessionStatsDebug;
import secret.kinetic.utils.misc.SessionStatsExporter;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S42PacketCombatEvent;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static secret.kinetic.utils.misc.IMinecraft.mc;


























































public class SessionStatsManager {

    





    private static final String[] SELF_WIN_PHRASES = {
            "VICTORY", "YOU WIN", "YOU WON", "YOU ARE THE WINNER",
            "WON THE GAME", "GAME WON", "1ST PLACE", "FIRST PLACE",
            
            
            "VITORIA", "VOCE GANHOU", "VOCE VENCEU", "GANHOU O JOGO",
            "PRIMEIRO LUGAR"
    };

    




    private static final String[] NAMED_WIN_PHRASES = {
            "WINNER", "WINS", "WON", "VENCEDOR", "VENCEU", "GANHOU"
    };

    




    private static final String[] GAME_OVER_PHRASES = {
            "DEFEAT", "GAME OVER", "BETTER LUCK", "TRY AGAIN",
            "DERROTA", "VOCE PERDEU"
    };

    
    private static final String[] LOSS_PHRASES = {
            "DEFEAT", "YOU LOST", "YOU DIED", "GAME OVER", "ELIMINATED",
            "BETTER LUCK", "TRY AGAIN",
            "DERROTA", "VOCE PERDEU", "VOCE MORREU", "CAMA DESTRUIDA"
    };

    





    private static final String[] SELF_DEATH_PHRASES = {
            "YOU DIED", "YOU ARE DEAD", "YOU HAVE DIED", "YOU WERE KILLED",
            "YOU WERE SLAIN", "YOU DROWNED", "YOU BURNED", "YOU STARVED",
            "YOU FELL", "YOU BLEW UP",
            "VOCE MORREU", "VOCE FOI MORTO", "VOCE FOI MORTA", "VOCE CAIU",
            "VOCE SE AFOGOU", "VOCE QUEIMOU",
            "HAS MUERTO"
    };

    




    private static final String[] DEATH_CUES = {
            "DIED", "DEAD", "KILLED", "SLAIN", "SHOT", "FELL", "THREW",
            "THROWN", "DROWNED", "BURNED", "BURNT", "SUFFOCATED", "STARVED",
            "BLEW UP", "WITHERED", "SQUASHED", "GROUND TOO HARD",
            "HIGH PLACE", "INTO THE VOID", "FINAL KILL",
            
            
            
            "MORREU", "MORTO", "MORTA", "MORTE", "MATOU", "JOGADO", "JOGADA",
            "ARREMESSADO", "CAIU", "AFOGOU", "QUEIMOU", "EXPLODIU"
    };

    




    private static final String[] DEATH_VETO = {
            "BED", "CAMA", "DESTROYED", "DESTRUIDA", "DESTRUIU", "BROKE"
    };

    



    private static final String[] AGENT_MARKERS = { "BY", "POR", "PELO", "PELA" };

    



    private static final String[] ACTIVE_KILL_VERBS = {
            "KILLED", "SLAIN", "SHOT", "MATOU", "ASSASSINOU", "ELIMINOU"
    };

    
    private static final String[] VOID_CUES = { "VOID", "ABISMO", "VAZIO" };
    private static final String[] FALL_CUES = {
            "FELL", "FALL", "GROUND TOO HARD", "HIGH PLACE", "CAIU", "QUEDA"
    };
    private static final String[] FIRE_CUES = {
            "FIRE", "LAVA", "BURNED", "BURNT", "QUEIMOU", "FOGO"
    };
    private static final String[] WATER_CUES = {
            "DROWNED", "AFOGOU", "AFOGADO", "WATER"
    };

    




    private static final long WIN_COOLDOWN_MS = 20000L;

    






    private static final long DEATH_COOLDOWN_MS = 5000L;

    




    private static final long DEATH_TEXT_REPEAT_MS = 15000L;

    





    private static final int MAX_TEXTS_LOGGED = 240;
    private static final int MAX_PENDING_NOTES = 64;

    




    private static final int MAX_NOTED_ONCE = 120;

    



    private static final float LOW_HEALTH_NOTE = 2.0F;

    



    private static final int MAX_PACKET_NOTES = 200;

    



    private static long sessionStart = System.currentTimeMillis();
    private static int kills;
    private static int deaths;
    private static int wins;

    
    private static int rounds;

    private static long lastWinAt;

    private static long lastRoundAt;

    private static long lastDeathAt;

    
    private static String lastDeathText = "";

    





    private static boolean deathEpisode;

    
    private static int packetNotes;

    


    private static final Set<String> loggedTexts = new HashSet<String>();

    
    private static final Set<String> notedOnce = new HashSet<String>();

    




    private static final List<String> pendingNotes = new ArrayList<String>();

    public SessionStatsManager() {
        reset();

        
        
        
        
        publish();
    }

    public static int getKills() {
        return kills;
    }

    public static int getDeaths() {
        return deaths;
    }

    public static int getWins() {
        return wins;
    }

    public static int getRounds() {
        return rounds;
    }

    public static long getSessionStart() {
        return sessionStart;
    }

    public static long getSessionMs() {
        return Math.max(0L, System.currentTimeMillis() - sessionStart);
    }

    
    public static long getPlaytimeMillis() {
        return getSessionMs();
    }

    
    public static float getKd() {
        return kills / (float) Math.max(1, deaths);
    }

    
    public static float getWinRate() {
        return wins / (float) Math.max(1, rounds) * 100f;
    }

    
    public static synchronized void reset() {
        sessionStart = System.currentTimeMillis();
        kills = 0;
        deaths = 0;
        wins = 0;
        rounds = 0;
        lastWinAt = 0L;
        lastRoundAt = 0L;
        lastDeathAt = 0L;
        lastDeathText = "";
        deathEpisode = false;
        packetNotes = 0;
        synchronized (loggedTexts) {
            loggedTexts.clear();
        }
        synchronized (notedOnce) {
            notedOnce.clear();
        }
    }

    
    @EventHook
    public void onKill(KillEvent event) {
        countKill();
    }

    




    @EventHook
    public void onDeath(PlayerDeathEvent event) {
        countDeath("death screen");
    }

    




    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        try {
            Object packet = event.getPacket();
            if (packet instanceof S45PacketTitle) {
                onTitle((S45PacketTitle) packet);
            } else if (packet instanceof S02PacketChat) {
                onChat((S02PacketChat) packet);
            } else if (packet instanceof S06PacketUpdateHealth) {
                onHealthPacket((S06PacketUpdateHealth) packet);
            } else if (packet instanceof S42PacketCombatEvent) {
                onCombatPacket((S42PacketCombatEvent) packet);
            } else if (packet instanceof S07PacketRespawn) {
                onRespawnPacket((S07PacketRespawn) packet);
            }
        } catch (Throwable ignored) {
            
        }
    }

    




    @EventHook
    public void onTick(ClientTickEvent event) {
        closeEpisodeIfAlive();
        publish();
        
        
        flushNotes();
    }

    
    @EventHook
    public void onGameStopping(GameStoppingEvent event) {
        try {
            SessionStatsExporter.publishStopped(mc.mcDataDir);
            flushNotes();
        } catch (Throwable ignored) {
            
        }
    }

    

    private static synchronized void countKill() {
        kills++;
        note("KILL | kills now " + kills);
    }

    private static synchronized void countDeath(String source) {
        countDeath(source, null);
    }

    














    private static synchronized void countDeath(String source, String text) {
        long now = System.currentTimeMillis();
        String said = text == null || text.length() == 0 ? "" : " | " + text;
        if (deathEpisode) {
            noteOnce("death-episode|" + source + said,
                    "DEATH ignored, same episode | " + source + said);
            return;
        }
        if (said.length() > 0 && text.equals(lastDeathText)
                && now - lastDeathAt < DEATH_TEXT_REPEAT_MS) {
            noteOnce("death-repeat|" + source + said,
                    "DEATH ignored, same wording | " + source + said);
            return;
        }
        if (lastDeathAt > 0L && now - lastDeathAt < DEATH_COOLDOWN_MS) {
            noteOnce("death-cooldown|" + source + said,
                    "DEATH ignored, within cooldown | " + source + said);
            return;
        }
        deathEpisode = true;
        lastDeathAt = now;
        lastDeathText = text == null ? "" : text;
        deaths++;
        note("DEATH from " + source + " | " + causeOf(text)
                + " | deaths now " + deaths + said);
    }

    




    private static synchronized void closeEpisodeIfAlive() {
        if (!deathEpisode) {
            return;
        }
        try {
            if (mc.thePlayer != null && mc.thePlayer.getHealth() > 0.0F) {
                deathEpisode = false;
                note("death episode closed | alive again");
            }
        } catch (Throwable ignored) {
            
        }
    }

    private static synchronized void countWin(String source, String text) {
        long now = System.currentTimeMillis();
        if (now - lastWinAt < WIN_COOLDOWN_MS) {
            noteOnce("win-cooldown|" + source + "|" + text,
                    "WIN ignored, within cooldown | " + source + " | " + text);
            return;
        }
        lastWinAt = now;
        wins++;
        note("WIN from " + source + " | " + text + " | wins now " + wins);
        countRound(source, text, true);
    }

    



    private static synchronized void countRound(String source, String text, boolean won) {
        long now = System.currentTimeMillis();
        if (now - lastRoundAt < WIN_COOLDOWN_MS) {
            noteOnce("round-cooldown|" + source + "|" + text,
                    "ROUND ignored, within cooldown | " + source + " | " + text);
            return;
        }
        lastRoundAt = now;
        rounds++;
        note("ROUND " + (won ? "won" : "lost") + " from " + source + " | " + text
                + " | rounds now " + rounds);
    }

    

    





    private void onHealthPacket(S06PacketUpdateHealth packet) {
        float health = packet.getHealth();
        if (health <= 0.0F) {
            packetNote("health packet | 0 health");
            countDeath("health packet");
        } else if (health <= LOW_HEALTH_NOTE) {
            packetNote("health packet | low health " + health);
        }
    }

    





    private void onCombatPacket(S42PacketCombatEvent packet) {
        if (packet.eventType != S42PacketCombatEvent.Event.ENTITY_DIED) {
            return;
        }
        boolean mine = false;
        try {
            mine = mc.thePlayer != null
                    && packet.field_179774_b == mc.thePlayer.getEntityId();
        } catch (Throwable ignored) {
            
        }
        String said = normalise(packet.deathMessage);
        packetNote("combat ENTITY_DIED | mine=" + mine + " | entity "
                + packet.field_179774_b + " | " + packet.deathMessage);
        if (mine) {
            countDeath("combat packet", said);
        }
    }

    





    private void onRespawnPacket(S07PacketRespawn packet) {
        packetNote("respawn packet | dimension " + packet.getDimensionID()
                + " | gametype " + packet.getGameType());
        synchronized (SessionStatsManager.class) {
            if (deathEpisode) {
                deathEpisode = false;
                note("death episode closed | respawn packet");
            }
        }
    }

    






    private void onTitle(S45PacketTitle packet) {
        if (packet.getType() != S45PacketTitle.Type.TITLE
                && packet.getType() != S45PacketTitle.Type.SUBTITLE) {
            return;
        }
        if (packet.getMessage() == null) {
            return;
        }
        String text = normalise(packet.getMessage().getUnformattedText());
        if (text.length() == 0) {
            return;
        }
        String source = packet.getType() == S45PacketTitle.Type.TITLE
                ? "title" : "subtitle";
        logText(source, text);
        considerDeath(source, text);
        considerWin(source, text);
    }

    





    private void onChat(S02PacketChat packet) {
        if (packet.getChatComponent() == null) {
            return;
        }
        String text = normalise(packet.getChatComponent().getUnformattedText());
        if (text.length() == 0) {
            return;
        }
        considerDeath("chat", text);
        String name = normalise(username());
        if (name.length() == 0 || !containsWord(text, name)) {
            return;
        }
        logText("chat", text);
        considerWin("chat", text);
    }

    

    









    private static void considerDeath(String source, String text) {
        
        
        
        if (text.indexOf(':') >= 0) {
            return;
        }
        
        
        if (containsAny(text, DEATH_VETO)) {
            return;
        }
        if (containsAny(text, SELF_DEATH_PHRASES)) {
            countDeath(source + ", addressed to you", text);
            return;
        }
        if (!containsAny(text, DEATH_CUES)) {
            return;
        }
        String name = normalise(username());
        if (name.length() == 0 || !containsWord(text, name)) {
            return;
        }
        if (iAmTheVictim(words(text), name)) {
            countDeath(source + ", named", text);
        }
    }

    













    private static boolean iAmTheVictim(String[] words, String me) {
        int name = indexOfWord(words, me);
        if (name < 0) {
            return false;
        }
        int marker = indexOfAnyWord(words, AGENT_MARKERS);
        if (marker >= 0) {
            return name < marker;
        }
        if (name <= 1) {
            return !(name + 1 < words.length
                    && isAnyWord(words[name + 1], ACTIVE_KILL_VERBS));
        }
        for (int i = 0; i < name; i++) {
            if (isAnyWord(words[i], ACTIVE_KILL_VERBS)) {
                return true;
            }
        }
        return false;
    }

    




    private static String causeOf(String text) {
        if (text == null || text.length() == 0) {
            return "unknown cause";
        }
        StringBuilder out = new StringBuilder();
        if (containsAny(text, VOID_CUES)) {
            out.append("void");
        } else if (containsAny(text, FALL_CUES)) {
            out.append("fall");
        } else if (containsAny(text, FIRE_CUES)) {
            out.append("fire");
        } else if (containsAny(text, WATER_CUES)) {
            out.append("water");
        }
        if (containsAny(text, AGENT_MARKERS)
                || containsAny(text, ACTIVE_KILL_VERBS)) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append("by a player");
        }
        return out.length() == 0 ? "unknown cause" : out.toString();
    }

    




    private static String[] words(String text) {
        List<String> out = new ArrayList<String>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                word.append(c);
            } else if (word.length() > 0) {
                out.add(word.toString());
                word.setLength(0);
            }
        }
        if (word.length() > 0) {
            out.add(word.toString());
        }
        return out.toArray(new String[out.size()]);
    }

    private static int indexOfWord(String[] words, String needle) {
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfAnyWord(String[] words, String[] needles) {
        for (int i = 0; i < words.length; i++) {
            if (isAnyWord(words[i], needles)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isAnyWord(String word, String[] needles) {
        for (int i = 0; i < needles.length; i++) {
            if (word.equals(needles[i])) {
                return true;
            }
        }
        return false;
    }

    private static void considerWin(String source, String text) {
        
        
        if (containsAny(text, LOSS_PHRASES)) {
            if (containsAny(text, GAME_OVER_PHRASES)) {
                countRound(source, text, false);
            }
            return;
        }
        if (containsAny(text, SELF_WIN_PHRASES)) {
            countWin(source, text);
            return;
        }
        if (containsAny(text, NAMED_WIN_PHRASES)) {
            String name = normalise(username());
            if (name.length() > 0 && containsWord(text, name)) {
                countWin(source, text);
            }
        }
    }

    













    private static String normalise(String text) {
        if (text == null) {
            return "";
        }
        
        
        String plain = Normalizer.normalize(
                StringUtils.stripControlCodes(text), Normalizer.Form.NFD)
                .toUpperCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(plain.length());
        boolean pendingSpace = false;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c <= ' ') {
                pendingSpace = out.length() > 0;
                continue;
            }
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;   
            }
            if (pendingSpace) {
                out.append(' ');
                pendingSpace = false;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean containsAny(String text, String[] phrases) {
        for (int i = 0; i < phrases.length; i++) {
            if (containsWord(text, phrases[i])) {
                return true;
            }
        }
        return false;
    }

    




    private static boolean containsWord(String haystack, String needle) {
        if (haystack == null || needle == null || needle.length() == 0) {
            return false;
        }
        int from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                return false;
            }
            int end = at + needle.length();
            boolean startsClean = at == 0
                    || !Character.isLetterOrDigit(haystack.charAt(at - 1));
            boolean endsClean = end >= haystack.length()
                    || !Character.isLetterOrDigit(haystack.charAt(end));
            if (startsClean && endsClean) {
                return true;
            }
            from = at + 1;
        }
    }

    

    



    private static void logText(String source, String text) {
        synchronized (loggedTexts) {
            if (loggedTexts.contains(text)
                    || loggedTexts.size() >= MAX_TEXTS_LOGGED) {
                return;
            }
            loggedTexts.add(text);
        }
        note(source + " seen | " + text);
    }

    





    private static void noteOnce(String key, String line) {
        synchronized (notedOnce) {
            if (notedOnce.contains(key) || notedOnce.size() >= MAX_NOTED_ONCE) {
                return;
            }
            notedOnce.add(key);
        }
        note(line);
    }

    private static void note(String line) {
        synchronized (pendingNotes) {
            if (pendingNotes.size() >= MAX_PENDING_NOTES) {
                return;
            }
            pendingNotes.add(line);
        }
    }

    





    private static void packetNote(String line) {
        synchronized (SessionStatsManager.class) {
            if (packetNotes >= MAX_PACKET_NOTES) {
                return;
            }
            packetNotes++;
        }
        note(line);
    }

    
    private static void flushNotes() {
        List<String> lines;
        synchronized (pendingNotes) {
            if (pendingNotes.isEmpty()) {
                return;
            }
            lines = new ArrayList<String>(pendingNotes);
            pendingNotes.clear();
        }
        for (int i = 0; i < lines.size(); i++) {
            try {
                SessionStatsDebug.note(mc.mcDataDir, lines.get(i));
            } catch (Throwable ignored) {
            }
        }
    }

    

    private void publish() {
        try {
            SessionStatsExporter.publish(mc.mcDataDir, username(), getServerName(),
                    kills, deaths, wins, sessionStart);
        } catch (Throwable ignored) {
            
            
        }
    }

    



    private static String username() {
        try {
            return mc.getSession() == null ? "" : mc.getSession().getUsername();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String getServerName() {
        return mc.getCurrentServerData() != null
                ? mc.getCurrentServerData().serverIP : "Singleplayer";
    }
}
