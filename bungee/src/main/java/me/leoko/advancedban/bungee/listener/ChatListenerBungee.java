package me.leoko.advancedban.bungee.listener;

import me.leoko.advancedban.Universal;
import me.leoko.advancedban.bungee.BungeeMain;
import me.leoko.advancedban.bungee.managers.DiscordWebhookManager;
import me.leoko.advancedban.bungee.utils.Utils;
import me.leoko.advancedban.bungee.utils.WarnWordsLog;
import me.leoko.advancedban.utils.Command;
import me.leoko.advancedban.utils.PunishmentType;
import me.leoko.advancedban.utils.commands.PunishmentProcessor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Leoko @ dev.skamps.eu on 24.07.2016.
 */
public class ChatListenerBungee implements Listener {

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) return;
        ProxiedPlayer player = (ProxiedPlayer) event.getSender();

        if (!event.isCommand()) {
            // If they're muted
            if (Universal.get().getMethods().callChat(event.getSender())) {
                event.setCancelled(true);
            }

        } else {
            // if they try send a blocked command whilst being muted
            if (Universal.get().getMethods().callCMD(event.getSender(), event.getMessage())) {
                event.setCancelled(true);
            }
        }

        // Check for filtered words. If the player is staff, don't do anything for commands (so we can still delete inappropriate names etc)
        if (event.isCommand() && player.hasPermission("group.trialmod")) return;

        // If it's a chat, don't do anything if the event was cancelled
        if (!event.isCommand() && event.isCancelled()) return;

        List<String> filteredWords = new ArrayList<>(Universal.get().immediateBanWords);
        filteredWords.addAll(Universal.get().warnWords);

        // We want to ensure things like &nig (underlined ig) don't get caught. So translate colours first
        String colouredMessage = ChatColor.stripColor(Utils.colour(event.getMessage()));

        for (String filteredWord : filteredWords) {
            // If the message doesn't contain any signs of the word, continue
            if (!colouredMessage.toLowerCase().contains(filteredWord.toLowerCase())) continue;

            // This pattern below matches any filtered word that has no letters before or after it.
            // It WILL catch non-letters before or after (like _penis, penis&, etc)
            String pattern = "(?<![\\p{L}])" + filteredWord + "(?![\\p{L}])";
            Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(colouredMessage);

            // It contains a filtered word
            if (matcher.find()) {
                String server = player.getServer().getInfo().getName();

                // If it's a warn word and their first offense, warn them (then log them so we know they've been warned)
                if (Universal.get().warnWords.contains(filteredWord)) {
                    List<String> caughtWords = Universal.get().caughtWarnWords.getOrDefault(player.getUniqueId(), new ArrayList<>());

                    if (!caughtWords.contains(filteredWord)) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "You tried to use the phrase '" + filteredWord + "'" +
                                " which results in an immediate permanent ban. As this is your first time, we have" +
                                " prevented you from using it. Please do not use it again if you want to remain on the server");

                        caughtWords.add(filteredWord);
                        Universal.get().caughtWarnWords.put(player.getUniqueId(), caughtWords);

                        Universal.get().log("Warned " + player.getName() + " for use of the phrase '" + filteredWord + "'");
                        WarnWordsLog.logToFile("Warned " + player.getName() + " (" + player.getUniqueId() + ") for use of the phrase '" + filteredWord + "' on " + server + ". Full message: '" + event.getMessage() + "'");
                    }

                    // Else it is an auto-ban word so insta-ban and post the proof
                } else {
                    event.setCancelled(true);
                    String args = player.getName() + " Use of an illegal phrase: " + filteredWord;

                    new PunishmentProcessor(PunishmentType.BAN).accept(new Command.CommandInput(ProxyServer.getInstance().getConsole(),
                            args.split(" ")));

                    ProxyServer.getInstance().getScheduler().schedule(BungeeMain.get(), () -> {
                        DiscordWebhookManager.sendDiscordMessage("`" + player.getName() + "` said `" + event.getMessage() + "`" +
                                " on " + server);
                        }, 1, TimeUnit.SECONDS);
                }

                break;
            }
        }

        filteredWords.clear();
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        final String commandName = event.getCursor().split(" ")[0];
        if (commandName.length() > 1 && event.getCursor().length() > commandName.length()) {
            final Command command = Command.getByName(commandName.substring(1));
            if (command != null && event.getSender() instanceof ProxiedPlayer) {
                if (command.getPermission() == null || Universal.get().getMethods().hasPerms(event.getSender(), command.getPermission())) {
                    final String[] args = event.getCursor().substring(commandName.length() + 1).split(" ", -1);
                    event.getSuggestions().addAll(command.getTabCompleter().onTabComplete(event.getSender(), args));
                }
            }
        }
    }
}
