package me.leoko.advancedban.bungee.listener;

import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import me.leoko.advancedban.MethodInterface;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.bungee.BungeeMain;
import me.leoko.advancedban.bungee.utils.Utils;
import me.leoko.advancedban.manager.PunishmentManager;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.RecentBan;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Beelzebu
 */
public class PubSubMessageListener implements Listener {
    
    private static final MethodInterface mi = Universal.get().getMethods();

    @SuppressWarnings("deprecation")
	@EventHandler
    public void onMessageReceive(PubSubMessageEvent e) {
        if (e.getChannel().equals("advancedban:main")) {
            String[] msg = e.getMessage().split(" ");

            // Check for if we are excluding this proxy
            if (msg[0].startsWith("excludeProxy:")) {
                String exclude = msg[0].split(":")[1];

                // Before returning, we want to remove the "excludeProxy" bit so all the message have the same parts (as some won't have that)
                List<String> partsList = new ArrayList<>(Arrays.asList(msg));
                partsList.remove(0); // Remove the first element
                msg = partsList.toArray(new String[0]);

                if (BungeeMain.get().getRedisProxyID().equals(exclude)) return;
            }

            if (msg[0].equalsIgnoreCase("kick")) {
                if (ProxyServer.getInstance().getPlayer(msg[1]) != null) {
                    ProxyServer.getInstance().getPlayer(msg[1]).disconnect(e.getMessage().substring((msg[0] + msg[1]).length() + 2));
                }

            } else if (msg[0].startsWith("notification")) {
                for (ProxiedPlayer pp : ProxyServer.getInstance().getPlayers()) {
                    if (mi.hasPerms(pp, msg[1])) {
                        mi.sendMessage(pp, e.getMessage().substring((msg[0] + msg[1]).length() + 2));
                    }
                }

            } else if (msg[0].startsWith("message")) {
                if (ProxyServer.getInstance().getPlayer(msg[1]) != null) {
                    ProxyServer.getInstance().getPlayer(msg[1]).sendMessage(e.getMessage().substring((msg[0] + msg[1]).length() + 2));
                }
                if (msg[1].equalsIgnoreCase("CONSOLE")) {
                    ProxyServer.getInstance().getConsole().sendMessage(e.getMessage().substring((msg[0] + msg[1]).length() + 2));
                }

            } else if (msg[0].startsWith("addToPunishmentMap")) {
                StringBuilder punishmentJSON = new StringBuilder();
                for (int i=1; i<msg.length; i++) {
                    punishmentJSON.append(msg[i] + " ");
                }

                Punishment punishment = (Punishment) Universal.get().deserialiseJson(punishmentJSON.toString().trim(), Punishment.class);
                PunishmentManager.get().addToPunishmentMap(punishment, false, false);

            } else if (msg[0].startsWith("removeFromPunishmentMap")) {
                StringBuilder punishmentJSON = new StringBuilder();
                for (int i=1; i<msg.length; i++) {
                    punishmentJSON.append(msg[i] + " ");
                }

                Punishment punishment = (Punishment) Universal.get().deserialiseJson(punishmentJSON.toString().trim(), Punishment.class);
                PunishmentManager.get().removeFromPunishmentMap(punishment, false);

            } else if (msg[0].startsWith("addToHistoryMap")) {
                StringBuilder punishmentJSON = new StringBuilder();
                for (int i=1; i<msg.length; i++) {
                    punishmentJSON.append(msg[i] + " ");
                }

                Punishment punishment = (Punishment) Universal.get().deserialiseJson(punishmentJSON.toString().trim(), Punishment.class);
                PunishmentManager.get().addToHistoryMap(punishment,false);

            } else if (msg[0].equals("cachePlayer")) {

            } else if (msg[0].equalsIgnoreCase("logBan")) {
                String playerName = msg[1];
                StringBuilder punishmentJSON = new StringBuilder();

                for (int i=2; i<msg.length; i++) {
                    punishmentJSON.append(msg[i] + " ");
                }

                MethodInterface mi = Universal.get().getMethods();
                Object playerObj = mi.getPlayer(playerName);

                if (playerObj instanceof ProxiedPlayer player) {
                    String ip = mi.getIP(player);
                    Punishment punishment = (Punishment) Universal.get().deserialiseJson(punishmentJSON.toString().trim(), Punishment.class);
                    PunishmentManager.recentBans.put(ip, new RecentBan(punishment, ip, System.currentTimeMillis(), new ArrayList<>()));

                    mi.kickAllOnIP(player.getAddress().getHostName(), "&cAn account logged in with the same IP as you just got banned. Do NOT log back in");
                }

            } else if (msg[0].equalsIgnoreCase("kickallonip")) {
                String ip = msg[1];
                StringBuilder kickMessage = new StringBuilder();

                for (int i = 2; i < msg.length; i++) {
                    kickMessage.append(msg[i] + " ");
                }

                for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                    if (p.getAddress().getAddress().getHostAddress().equals(ip)) {
                        p.disconnect(Utils.colour(kickMessage.toString().trim()));
                    }
                }
            }

        } else if (e.getChannel().equals("advancedban:connection")) {
            String[] msg = e.getMessage().split(",");
            Universal.get().getIps().remove(msg[0].toLowerCase());
            Universal.get().getIps().put(msg[0].toLowerCase(), msg[1]);

        } else if (e.getChannel().equals("bungeecore:main")) {

            if (e.getMessage().startsWith("POPULATE_WARN_WORDS_")) {
                String json = e.getMessage().replace("POPULATE_WARN_WORDS_", "");
                List<String> warnWords = (List<String>) Universal.get().deserialiseJson(json, List.class);

                Universal.get().warnWords = warnWords;

            } else if (e.getMessage().startsWith("POPULATE_BAN_WORDS_")) {
                String json = e.getMessage().replace("POPULATE_BAN_WORDS_", "");
                List<String> banWords = (List<String>) Universal.get().deserialiseJson(json, List.class);

                Universal.get().immediateBanWords = banWords;
            }
        }
    }
}