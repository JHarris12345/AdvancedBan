package me.leoko.advancedban.hytale;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import io.netty.channel.Channel;
import io.netty.handler.codec.quic.QuicStreamChannel;
import me.leoko.advancedban.MethodInterface;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.hytale.event.PunishmentEvent;
import me.leoko.advancedban.hytale.event.RevokePunishmentEvent;
import me.leoko.advancedban.hytale.listener.CommandReceiverHytale;
import me.leoko.advancedban.hytale.utils.ColourUtils;
import me.leoko.advancedban.hytale.utils.LuckPermsOfflineUser;
import me.leoko.advancedban.hytale.utils.Utils;
import me.leoko.advancedban.hytale.utils.config.Configuration;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import me.leoko.advancedban.utils.Permissionable;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.RecentBan;
import me.leoko.advancedban.utils.tabcompletion.TabCompleter;

import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Created by Leoko @ dev.skamps.eu on 23.07.2016.
 */
public class HytaleMethods implements MethodInterface {

    private final File configFile = new File(getDataFolder(), "config.yml");
    private final File messageFile = new File(getDataFolder(), "Messages.yml");
    private final File layoutFile = new File(getDataFolder(), "Layouts.yml");
    private final File mysqlFile = new File(getDataFolder(), "MySQL.yml");
    private Configuration config;
    private Configuration messages;
    private Configuration layouts;
    private Configuration mysql;

    private final Function<String, Permissionable> permissionableGenerator;

    public HytaleMethods() {
        //if (ProxyServer.getInstance().getPluginManager().getPlugin("LuckPerms") != null) {
            permissionableGenerator = LuckPermsOfflineUser::new;

            log("[AdvancedBan] Offline permission support through LuckPerms active");
        /*} else {
            permissionableGenerator = null;

            log("[AdvancedBan] No offline permission support through LuckPerms or CloudNet-CloudPerms");
        }*/
    }

    @Override
    public void loadFiles() {
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }

        config = getPlugin().getConfigUtils().loadResource("config.yml");
        messages = getPlugin().getConfigUtils().loadResource("Messages.yml");
        layouts = getPlugin().getConfigUtils().loadResource("Layouts.yml");
        mysql = getPlugin().getConfigUtils().loadResource("MySQL.yml");

        Universal.get().immediateBanWords = config.getStringList("ImmediateBanWords");
        Universal.get().warnWords = config.getStringList("WarnWords");
    }

    @Override
    public String getFromUrlJson(String url, String key) {
        try {
            HttpURLConnection request = (HttpURLConnection) new URL(url).openConnection();
            request.connect();

            JsonParser jp = new JsonParser();
            JsonObject json = (JsonObject) jp.parse(new InputStreamReader(request.getInputStream()));

            String[] keys = key.split("\\|");
            for (int i = 0; i < keys.length - 1; i++) {
                json = json.getAsJsonObject(keys[i]);
            }

            return json.get(keys[keys.length - 1]).toString().replaceAll("\"", "");

        } catch (Exception exc) {
            return null;
        }
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String[] getKeys(Object file, String path) {
        return ((Configuration) file).getSection(path).getKeys(false).toArray(new String[0]);
    }

    @Override
    public Configuration getConfig() {
        return config;
    }

    @Override
    public Configuration getMessages() {
        return messages;
    }

    @Override
    public Configuration getLayouts() {
        return layouts;
    }

    @Override
    public boolean isBungee() {
        return true;
    }

    @Override
    public String clearFormatting(String text) {
        return text; // For it to be a String, we would have already had to strip the formatting
    }

    @Override
    public HytaleMain getPlugin() {
        return HytaleMain.get();
    }

    @Override
    public File getDataFolder() {
        return getPlugin().getConfigUtils().pluginDirectory.toFile();
    }

    @Override
    public void setCommandExecutor(String cmd, String permission, TabCompleter tabCompleter) {
        getPlugin().getCommandRegistry().registerCommand(new CommandReceiverHytale(cmd, "Moderation commands", permission));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(Object player, String msg) {
        Message message = ColourUtils.colour(msg.replace("§", "&"));

        if (player instanceof PlayerRef ref) {
            ref.sendMessage(message);
            return;
        }

        ((CommandSender) player).sendMessage(message);
    }

    @Override
    public void sendMessage(String name, String msg) {
        if (Universal.isRedis()) {
            RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("advancedban:main", "message " + name + " " + msg);

        } else {
            if (name.equals("CONSOLE")) {
                getPlugin().getLogger().at(Level.INFO).log(msg);

            } else {
                PlayerRef player = Universe.get().getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE);
                if (player != null) {
                    player.sendMessage(ColourUtils.colour(msg.replace("§", "&")));
                }
            }
        }
    }

    @Override
    public boolean hasPerms(Object player, String perms) {
        if (player == null) return false;
        if (player instanceof ConsoleSender) return true;

        // If it's already a Player component, no need to hop threads.
        if (player instanceof Player p) {
            return p.hasPermission(perms);
        }

        if (player instanceof PlayerRef playerRef) {
            World world = Universe.get().getWorld(playerRef.getWorldUuid());

            return Utils.runOnWorldThreadBlocking(world, () -> {
                Player p = playerRef.getComponent(Player.getComponentType()); // getComponent HAS to be done on the world thread so this is now safe
                return p != null && p.hasPermission(perms);
            });
        }

        return false;
    }

    @Override
    public Permissionable getOfflinePermissionPlayer(String name) {
        if (permissionableGenerator != null) {
            return permissionableGenerator.apply(name);
        }

        return permission -> false;
    }

    @Override
    public boolean isOnline(String name, boolean checkRedis) {
        try {
            if (Universal.isRedis() && checkRedis) {
                UUID uuid = RedisBungeeAPI.getRedisBungeeApi().getUuidFromName(name);
                Set<UUID> onlinePlayers = RedisBungeeAPI.getRedisBungeeApi().getPlayersOnline();
                for (UUID onlineID : onlinePlayers) {
                    if (uuid.equals(onlineID)) {
                        return RedisBungeeAPI.getRedisBungeeApi().getPlayerIp(onlineID) != null;
                    }
                }
            }
            return getPlayer(name) != null;
        } catch (NullPointerException exc) {
            return false;
        }
    }

    @Override
    public PlayerRef getPlayer(String name) {
        return Universe.get().getPlayerByUsername(name, NameMatching.EXACT_IGNORE_CASE);
    }


    @Override
    public void kickPlayer(String player, String reason) {
        if (Universal.isRedis()) {
            HytaleMain.redis.sendChannelMessage("advancedban:main", "kick " + player + " " + reason);
        } else {
            getPlayer(player).getPacketHandler().disconnect(reason);
        }
    }

    @Override
    public void logBan(String name, Punishment punishment) {
        if (Universal.isRedis()) {
            HytaleMain.redis.sendChannelMessage("advancedban:main", "logBan " + name + " " + Universal.get().serialiseObject(punishment));

        } else {
            PlayerRef player = getPlayer(name);

            PunishmentManager.recentBans.put(getIP(player), new RecentBan(punishment, getIP(player), System.currentTimeMillis(), new ArrayList<>()));
            kickAllOnIP(getIP(player), "&cAn account logged in with the same IP as you just got banned. Do NOT log back in");
        }
    }

    @Override
    public PlayerRef[] getOnlinePlayers() {
        return Universe.get().getPlayers().toArray(new PlayerRef[]{});
    }

    @Override
    public List<String> getOnlinePlayerNames(boolean redis) {
        List<String> names = new ArrayList<>();

        if (Universal.isRedis()) {
            names = HytaleMain.redis.getHumanPlayersOnline()
                    .stream()
                    .filter(Objects::nonNull).collect(Collectors.toList());

        } else {
            for (PlayerRef player : getOnlinePlayers()) {
                names.add(player.getUsername());
            }
        }

        return names;
    }

    @Override
    public void scheduleAsyncRep(Runnable rn, long l1, long l2) {
        // Not sure how to make schedulers yet and it appears this code doesn't run at all anyway
        //ProxyServer.getInstance().getScheduler().schedule(getPlugin(), rn, l1 * 50, l2 * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void scheduleAsync(Runnable rn, long l1) {
        // Not sure how to make schedulers yet and it appears this code doesn't run at all anyway
        //ProxyServer.getInstance().getScheduler().schedule(getPlugin(), rn, l1 * 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runAsync(Runnable rn) {
        //ProxyServer.getInstance().getScheduler().runAsync(getPlugin(), rn);
        CompletableFuture.runAsync(rn)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    @Override
    public void runSync(Runnable rn) {
        rn.run(); //TODO WARNING not Sync to Main-Thread
    }

    @Override
    public void executeCommand(String cmd) {
        HytaleServer.get().getCommandManager().handleCommand(ConsoleSender.INSTANCE, cmd);
    }

    @Override
    public String getName(Object player) {
        if (player instanceof PlayerRef ref) return ref.getUsername();

        return ((CommandSender) player).getDisplayName();
    }

    @Override
    public String getName(String uuid) {
        PlayerRef playerRef = Universe.get().getPlayer(UUID.fromString(uuid));
        return (playerRef == null) ? "NULL" : playerRef.getUsername();
    }

    @Override
    public String getIP(Object playerRef) {
        Channel ch = ((PlayerRef) playerRef).getPacketHandler().getChannel();
        SocketAddress remote;
        if (ch instanceof QuicStreamChannel quic) {
            remote = quic.parent().remoteSocketAddress();
        } else {
            remote = ch.remoteAddress();
        }

        if (remote instanceof InetSocketAddress inet) {
            // This is the clean numeric IP like "203.0.113.10"
            if (inet.getAddress() != null) {
                return inet.getAddress().getHostAddress();
            }
            // Fallback if address unresolved
            return inet.getHostString();
        }

        return "NULL";
    }

    @Override
    public String getInternUUID(Object player) {
        return player instanceof CommandSender sender ? ((PlayerRef) sender).getUuid().toString().replaceAll("-", "") : "none";
    }

    @Override
    public String getInternUUID(String player) {
        PlayerRef proxiedPlayer = getPlayer(player);
        if (proxiedPlayer == null) {
            return null;
        }
        UUID uniqueId = proxiedPlayer.getUuid();
        return uniqueId == null ? null : uniqueId.toString().replaceAll("-", "");
    }

    @Override
    public boolean callChat(Object player) {
        Punishment pnt = PunishmentManager.get().getMute(UUIDManager.get().getUUID(getName(player)));
        if (pnt != null) {
            for (String str : pnt.getLayout()) {
                sendMessage(player, str);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean callCMD(Object player, String cmd) {
        Punishment pnt;
        if (Universal.get().isMuteCommand(cmd.substring(1))
                && (pnt = PunishmentManager.get().getMute(UUIDManager.get().getUUID(getName(player)))) != null) {
            for (String str : pnt.getLayout()) {
                sendMessage(player, str);
            }
            return true;
        }
        return false;
    }

    @Override
    public Object getMySQLFile() {
        return mysql;
    }

    @Override
    public String parseJSON(InputStreamReader json, String key) {
        JsonElement element = new JsonParser().parse(json);
        if (element instanceof JsonNull) {
            return null;
        }
        JsonElement obj = ((JsonObject) element).get(key);
        return obj != null ? obj.toString().replaceAll("\"", "") : null;
    }

    @Override
    public String parseJSON(String json, String key) {
        JsonElement element = new JsonParser().parse(json);
        if (element instanceof JsonNull) {
            return null;
        }
        JsonElement obj = ((JsonObject) element).get(key);
        return obj != null ? obj.toString().replaceAll("\"", "") : null;
    }

    @Override
    public Boolean getBoolean(Object file, String path) {
        return ((Configuration) file).getBoolean(path);
    }

    @Override
    public String getString(Object file, String path) {
        return ((Configuration) file).getString(path);
    }

    @Override
    public Long getLong(Object file, String path) {
        return ((Configuration) file).getLong(path);
    }

    @Override
    public Integer getInteger(Object file, String path) {
        return ((Configuration) file).getInt(path);
    }

    @Override
    public List<String> getStringList(Object file, String path) {
        return ((Configuration) file).getStringList(path);
    }

    @Override
    public boolean getBoolean(Object file, String path, boolean def) {
        return ((Configuration) file).getBoolean(path, def);
    }

    @Override
    public String getString(Object file, String path, String def) {
        return ((Configuration) file).getString(path, def);
    }

    @Override
    public long getLong(Object file, String path, long def) {
        return ((Configuration) file).getLong(path, def);
    }

    @Override
    public int getInteger(Object file, String path, int def) {
        return ((Configuration) file).getInt(path, def);
    }

    @Override
    public boolean contains(Object file, String path) {
        return ((Configuration) file).get(path) != null;
    }

    @Override
    public String getFileName(Object file) {
        return "[Only available on Bukkit-Version!]";
    }

    @Override
    public void callPunishmentEvent(Punishment punishment) {
        HytaleServer.get().getEventBus().dispatch(PunishmentEvent.class);
    }

    @Override
    public void callRevokePunishmentEvent(Punishment punishment, boolean massClear) {
        HytaleServer.get().getEventBus().dispatch(RevokePunishmentEvent.class);
    }

    @Override
    public boolean isOnlineMode() {
        return true;
        //return ProxyServer.getInstance().getConfig().isOnlineMode();
    }

    @Override
    public void notify(String perm, List<String> notification) {
        if (Universal.isRedis()) {
            notification.forEach((str) -> HytaleMain.redis.sendChannelMessage("advancedban:main", "notification " + perm + " " + str));
        } else {
            for (PlayerRef playerRef : getOnlinePlayers()) {

                if (hasPerms(playerRef, perm)) {
                    notification.forEach(msg -> playerRef.sendMessage(ColourUtils.colour(msg.replace('§', '&'))));
                }
            }
        }
    }

    @Override
    public void log(String msg) {
        getPlugin().getLogger().at(Level.INFO).log(msg);}

    @Override
    public boolean isUnitTesting() {
        return false;
    }

    @Override
    public void sendRedisMessage(String channel, String message) {
        HytaleMain.redis.sendChannelMessage(channel, message);
    }

    @Override
    public String getRedisProxyID() {
        return HytaleMain.get().getRedisProxyID();
    }

    @Override
    public void loadWarnBanWords() {
        RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("bungeecore:main", "REQUEST_WARN_WORDS");
        RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("bungeecore:main", "REQUEST_BAN_WORDS");
    }

    @Override
    public void kickAllOnIP(String ip, String kickMessage) {
        if (Universal.isRedis()) {
            HytaleMain.redis.sendChannelMessage("advancedban:main", "kickallonip " + ip + " " + kickMessage);

        } else {
            for (PlayerRef p : getOnlinePlayers()) {
                if (getIP(p).equals(ip)) {
                    p.getPacketHandler().disconnect(kickMessage);
                }
            }
        }
    }
}