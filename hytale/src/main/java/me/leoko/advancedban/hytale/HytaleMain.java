package me.leoko.advancedban.hytale;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.hytale.listener.ChatListenerHytale;
import me.leoko.advancedban.hytale.listener.ConnectionListenerHytale;
import me.leoko.advancedban.hytale.listener.PubSubMessageListener;
import me.leoko.advancedban.hytale.utils.config.ConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HytaleMain extends JavaPlugin {

    private static HytaleMain instance;

    public HytaleMain(@NotNull JavaPluginInit init) {
        super(init);
    }

    public static HytaleMain get() {
        return instance;
    }
    public static RedisBungeeAPI redis = null;

    private ConfigUtils configUtils;

    @Override
    protected void setup() {
        instance = this;
        configUtils = new ConfigUtils(this,"VerseCore");
        Universal.get().setup(new HytaleMethods());


        getEventRegistry().registerGlobal(PlayerChatEvent.class, ChatListenerHytale::onChat);
        getEventRegistry().registerGlobal(PlayerSetupConnectEvent.class, ConnectionListenerHytale::onConnection);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, ConnectionListenerHytale::onDisconnect);

        //ProxyServer.getInstance().getPluginManager().registerListener(this, new InternalListener());
        //ProxyServer.getInstance().registerChannel("advancedban:main");

        // Redis doesn't exist yet
        /*if (ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null) {
            redis = RedisBungeeAPI.getRedisBungeeApi();
            Universal.setRedis(true);
            ProxyServer.getInstance().getPluginManager().registerListener(this, new PubSubMessageListener());
            redis.registerPubSubChannels("advancedban:main", "advancedban:connection", "bungeecore:main");
            Universal.get().log("RedisBungee detected, hooking into it!");
        } else {
            Universal.get().log("RedisBungee not detected");
        }*/

        // Load the warn words and immediate ban words (must be done AFTER registering the redis channels)
        /*RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("bungeecore:main", "REQUEST_WARN_WORDS");
        RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("bungeecore:main", "REQUEST_BAN_WORDS");*/
    }

    @Override
    protected void shutdown() {
        Universal.get().shutdown();
    }

    public String getRedisProxyID() {
        return redis.getProxyId().replace(" ", "-");
    }

    public ConfigUtils getConfigUtils() {
        return configUtils;
    }
}