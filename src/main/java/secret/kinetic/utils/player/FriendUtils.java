package secret.kinetic.utils.player;

import secret.kinetic.Kinetic;
import secret.kinetic.modules.impl.misc.FriendsModule;
import secret.kinetic.utils.misc.IMinecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("all")
public class FriendUtils implements IMinecraft {
    private static final Set<String> friendsList = new HashSet<>();
    private static final File friendsFile = new File(mc.mcDataDir +
            "/" + Kinetic.NAME, "/friends.txt");

    public static void add(String name) {
        if (name != null && !name.isEmpty()) {
            String lower = name.toLowerCase();
            friendsList.add(lower);
            saveFriends();
        }
    }

    public static void remove(String name) {
        if (name != null && !name.isEmpty()) {
            String lower = name.toLowerCase();
            friendsList.remove(lower);
            saveFriends();
        }
    }

    public static FriendsModule getFriendsModule() {
        FriendsModule friendsModule = Kinetic.INSTANCE.getModuleManager().getModule(FriendsModule.class);
        if (friendsModule != null) {
            return friendsModule;
        }
        return null;
    }

    public static boolean isFriend(String name) {
        if (!getFriendsModule().isEnabled()) return false;

        return name != null && friendsList.contains(name.toLowerCase());
    }

    public static boolean isFriend(EntityPlayer player) {
        if (!getFriendsModule().isEnabled()) return false;

        return player != null && isFriend(player.getGameProfile().getName());
    }

    public static Set<String> getFriends() {
        return friendsList;
    }

    public static void clear() {
        friendsList.clear();
        saveFriends();
    }

    public static void saveFriends() {
        try {
            if (!friendsFile.getParentFile().exists()) {
                friendsFile.getParentFile().mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(friendsFile))) {
                for (String friend : friendsList) {
                    writer.write(friend);
                    writer.newLine();
                }
            }
        } catch (IOException e) {

        }
    }

    public static void loadFriends() {
        friendsList.clear();
        if (friendsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(friendsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        friendsList.add(line.trim().toLowerCase());
                    }
                }
            } catch (IOException err) {
            }
        }
    }

    public static void init() {
        loadFriends();
    }
}
