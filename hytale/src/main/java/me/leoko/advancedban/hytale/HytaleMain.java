package me.leoko.advancedban.hytale;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.hytale.listener.ChatListenerBungee;
import me.leoko.advancedban.hytale.listener.ConnectionListenerBungee;
import me.leoko.advancedban.hytale.listener.PubSubMessageListener;
import me.leoko.advancedban.hytale.utils.config.ConfigUtils;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

public class HytaleMain extends JavaPlugin {

    private static HytaleMain instance;
    public static HytaleMain get() {
        return instance;
    }
    //public static RedisBungeeAPI redis = null;
    public static Object redis = null;

    private ConfigUtils configUtils;

    @Override
    public void onEnable() {
        instance = this;
        configUtils = new ConfigUtils(this,"VerseCore");
        Universal.get().setup(new HytaleMethods());
        ProxyServer.getInstance().getPluginManager().registerListener(this, new ConnectionListenerBungee());
        ProxyServer.getInstance().getPluginManager().registerListener(this, new ChatListenerBungee());
        //ProxyServer.getInstance().getPluginManager().registerListener(this, new InternalListener());
        //ProxyServer.getInstance().registerChannel("advancedban:main");

        if (ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null) {
            redis = RedisBungeeAPI.getRedisBungeeApi();
            Universal.setRedis(true);
            ProxyServer.getInstance().getPluginManager().registerListener(this, new PubSubMessageListener());
            redis.registerPubSubChannels("advancedban:main", "advancedban:connection", "bungeecore:main");
            Universal.get().log("RedisBungee detected, hooking into it!");
        } else {
            Universal.get().log("RedisBungee not detected");
        }

        // Load the warn words and immediate ban words (msut be done AFTER registering the redis channels)
        RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("bungeecore:main", "REQUEST_WARN_WORDS");
        RedisBungeeAPI.getRedisBungeeApi().sendChannelMessage("bungeecore:main", "REQUEST_BAN_WORDS");
    }

    @Override
    public void onDisable() {
        Universal.get().shutdown();
    }

    public String getRedisProxyID() {
        return redis.getProxyId().replace(" ", "-");
    }

    public ConfigUtils getConfigUtils() {
        return configUtils;
    }
}