package me.leoko.advancedban.bungee;

import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.bungee.listener.ChatListenerBungee;
import me.leoko.advancedban.bungee.listener.ConnectionListenerBungee;
import me.leoko.advancedban.bungee.listener.InternalListener;
import me.leoko.advancedban.bungee.listener.PubSubMessageListener;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class BungeeMain extends Plugin {

    private static BungeeMain instance;
    public static BungeeMain get() {
        return instance;
    }
    public static RedisBungeeAPI redis = null;

    @Override
    public void onEnable() {
        instance = this;
        Universal.get().setup(new BungeeMethods());
        ProxyServer.getInstance().getPluginManager().registerListener(this, new ConnectionListenerBungee());
        ProxyServer.getInstance().getPluginManager().registerListener(this, new ChatListenerBungee());
        ProxyServer.getInstance().getPluginManager().registerListener(this, new InternalListener());
        ProxyServer.getInstance().registerChannel("advancedban:main");

        if (ProxyServer.getInstance().getPluginManager().getPlugin("RedisBungee") != null) {
            redis = RedisBungeeAPI.getRedisBungeeApi();
            Universal.setRedis(true);
            ProxyServer.getInstance().getPluginManager().registerListener(this, new PubSubMessageListener());
            redis.registerPubSubChannels("advancedban:main", "advancedban:connection");
            Universal.get().log("RedisBungee detected, hooking into it!");
        } else {
            Universal.get().log("RedisBungee not detected");
        }
    }

    @Override
    public void onDisable() {
        Universal.get().shutdown();
    }
}