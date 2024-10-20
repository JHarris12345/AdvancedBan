package me.leoko.advancedban.bungee.listener;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.bungee.BungeeMain;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import me.leoko.advancedban.utils.Command;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.PunishmentType;
import me.leoko.advancedban.utils.RecentBan;
import me.leoko.advancedban.utils.commands.PunishmentProcessor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

/**
 * Created by Leoko @ dev.skamps.eu on 24.07.2016.
 */
public class ConnectionListenerBungee implements Listener {

    @SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL) // Priority used to be LOW. Changed to NORMAL (can change back if needs be)
    public void onConnection(LoginEvent event) {
        if(event.isCancelled())
            return;

        UUIDManager.get().supplyInternUUID(event.getConnection().getName(), event.getConnection().getUniqueId());
        event.registerIntent((BungeeMain)Universal.get().getMethods().getPlugin());
        Universal.get().getMethods().runAsync(() -> {
            String ip = event.getConnection().getAddress().getAddress().getHostAddress();

            // Catch if an alt logs in during a ban and ban them too
            RecentBan recentBan = PunishmentManager.recentBans.getOrDefault(ip, null);
            if (recentBan != null) {
                Punishment punishment = recentBan.getPunishment();

               String playerName = event.getConnection().getName();
               String evadingName = punishment.getName();

               if (!playerName.equals(evadingName) && !recentBan.getCaughtNames().contains(playerName)) {
                   if (System.currentTimeMillis() < recentBan.getBanedAtTime() + 3600000) {
                       String args = playerName + " Ban evasion of " + evadingName;

                       if (punishment.getType() == PunishmentType.TEMP_BAN) {
                           long endTime = punishment.getEnd();
                           int secondsLeft = (int) Math.ceil((endTime - System.currentTimeMillis()) / 1000d);

                           args = playerName + " " + secondsLeft + "s" + " Ban evasion of " + evadingName;
                       }

                       new PunishmentProcessor(punishment.getType()).accept(new Command.CommandInput(ProxyServer.getInstance().getConsole(),
                               args.split(" ")));

                       recentBan.getCaughtNames().add(playerName);

                   } else {
                       PunishmentManager.recentBans.remove(ip);
                   }
               }
            }

            String result = Universal.get().callConnection(event.getConnection().getName(), ip);

            if (result != null) {
                if(BungeeMain.getCloudSupport() != null){
                    BungeeMain.getCloudSupport().kick(event.getConnection().getUniqueId(), result);
                }else {
                    event.setCancelled(true);
                    event.setCancelReason(result);
                }
            }

            if (Universal.isRedis()) {
                RedisBungee.getApi().sendChannelMessage("advancedban:connection", event.getConnection().getName() + "," + ip);
            }
            event.completeIntent((BungeeMain) Universal.get().getMethods().getPlugin());
        });
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        Universal.get().getMethods().runAsync(() -> {
            if (event.getPlayer() != null) {
                PunishmentManager.get().discard(event.getPlayer().getName());
            }
        });
    }

    @SuppressWarnings("deprecation")
	@EventHandler
    public void onLogin(final PostLoginEvent event) {
        Universal.get().getMethods().scheduleAsync(() -> {
            if (event.getPlayer().getName().equalsIgnoreCase("Leoko")) {
                if (Universal.get().broadcastLeoko()) {
                    ProxyServer.getInstance().broadcast("");
                    ProxyServer.getInstance().broadcast("§c§lAdvancedBan §8§l» §7My creator §c§oLeoko §7just joined the game ^^");
                    ProxyServer.getInstance().broadcast("");
                } else {
                    event.getPlayer().sendMessage("§c§lAdvancedBan v2 §8§l» §cHey Leoko we are using your Plugin (NO-BC)");
                }
            }
        }, 20);
    }
}