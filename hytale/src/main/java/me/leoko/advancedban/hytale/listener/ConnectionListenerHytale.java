package me.leoko.advancedban.hytale.listener;

import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import me.leoko.advancedban.MethodInterface;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.hytale.HytaleMain;
import me.leoko.advancedban.hytale.utils.ColourUtils;
import me.leoko.advancedban.hytale.utils.Utils;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.manager.UUIDManager;
import me.leoko.advancedban.utils.Command;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.PunishmentType;
import me.leoko.advancedban.utils.RecentBan;
import me.leoko.advancedban.utils.commands.PunishmentProcessor;

/**
 * Created by Leoko @ dev.skamps.eu on 24.07.2016.
 */
public class ConnectionListenerHytale {

    public static void onConnection(PlayerSetupConnectEvent event) {
        if (event.isCancelled()) return;

        UUIDManager.get().supplyInternUUID(event.getUsername(), event.getUuid());
        //event.registerIntent((HytaleMain)Universal.get().getMethods().getPlugin()); // Tf does this do?

        MethodInterface mi = Universal.get().getMethods();

        //mi.runAsync(() -> {
            String ip = Utils.getIPFromPacketHandler(event.getPacketHandler());
            String result = Universal.get().callConnection(event.getUsername(), ip);

            if (result != null) {
                String rawMessage = ColourUtils.stripColour(ColourUtils.colour(result.replace("§", "&")));

                event.setCancelled(true);
                event.setReason(rawMessage);
            }

            // Catch if an alt logs in during a ban and ban them too (this is done after checking if the result is null so it only does the check if they're not already punished)
            if (result == null) {
                RecentBan recentBan = PunishmentManager.recentBans.getOrDefault(ip, null);
                if (recentBan != null) {
                    Punishment punishment = recentBan.getPunishment();

                    String playerName = event.getUsername();
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
                                new PunishmentProcessor(punishment.getType()).accept(new Command.CommandInput(ConsoleSender.INSTANCE,
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
                HytaleMain.redis.sendChannelMessage("advancedban:connection", event.getUsername() + "," + ip);
            }
        //});
    }

    public static void onDisconnect(PlayerDisconnectEvent event) {
        Universal.get().getMethods().runAsync(() -> {
            PunishmentManager.get().discard(event.getPlayerRef().getUsername());
        });
    }
}