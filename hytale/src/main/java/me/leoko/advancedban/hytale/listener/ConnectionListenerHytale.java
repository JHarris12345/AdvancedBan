package me.leoko.advancedban.hytale.listener;

import me.leoko.advancedban.Universal;
import me.leoko.advancedban.hytale.HytaleMain;
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
public class ConnectionListenerHytale implements Listener {

    @SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW)
    public void onConnection(LoginEvent event) {
        if (event.isCancelled()) return;

        UUIDManager.get().supplyInternUUID(event.getConnection().getName(), event.getConnection().getUniqueId());
        event.registerIntent((HytaleMain)Universal.get().getMethods().getPlugin());

        Universal.get().getMethods().runAsync(() -> {
            String ip = event.getConnection().getAddress().getAddress().getHostAddress();
            String result = Universal.get().callConnection(event.getConnection().getName(), ip);

            if (result != null) {
                event.setCancelled(true);
                event.setCancelReason(result);
            }

            // Catch if an alt logs in during a ban and ban them too (this is done after checking if the result is null so it only does the check if they're not already punished)
            if (result == null) {
                RecentBan recentBan = PunishmentManager.recentBans.getOrDefault(ip, null);
                if (recentBan != null) {
                    Punishment punishment = recentBan.getPunishment();

                    String playerName = event.getConnection().getName();
                    String evadingName = punishment.getName();

                    // If the logging in player is NOT the originally banned player AND they haven't recently been caught for ban evasion
                    if (!playerName.equals(evadingName) && !recentBan.getCaughtNames().contains(playerName)) {
                        if (System.currentTimeMillis() < recentBan.getBanedAtTime() + (3600000 * 24)) {
                            String args = playerName + " Ban evasion of " + evadingName;
                            boolean tempEnded = false;

                            if (punishment.getType() == PunishmentType.TEMP_BAN) {
                                long endTime = punishment.getEnd();
                                if (System.currentTimeMillis() > endTime) tempEnded = true;

                                int secondsLeft = (int) Math.ceil((endTime - System.currentTimeMillis()) / 1000d);
                                args = playerName + " " + secondsLeft + "s" + " Ban evasion of " + evadingName;
                            }

                            if (!tempEnded) {
                                new PunishmentProcessor(punishment.getType()).accept(new Command.CommandInput(ProxyServer.getInstance().getConsole(),
                                        args.split(" ")));

                                recentBan.getCaughtNames().add(playerName);
                                event.setCancelled(true);
                            }

                        } else {
                            PunishmentManager.recentBans.remove(ip);
                        }
                    }
                }
            }

            if (Universal.isRedis()) {
                HytaleMain.redis.sendChannelMessage("advancedban:connection", event.getConnection().getName() + "," + ip);
            }
            event.completeIntent((HytaleMain) Universal.get().getMethods().getPlugin());
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
}