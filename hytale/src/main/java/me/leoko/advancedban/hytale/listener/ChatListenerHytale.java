package me.leoko.advancedban.hytale.listener;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.leoko.advancedban.Universal;
import me.leoko.advancedban.hytale.HytaleMain;
import me.leoko.advancedban.hytale.managers.DiscordWebhookManager;
import me.leoko.advancedban.hytale.utils.ColourUtils;
import me.leoko.advancedban.hytale.utils.Utils;
import me.leoko.advancedban.hytale.utils.WarnWordsLog;
import me.leoko.advancedban.utils.Command;
import me.leoko.advancedban.utils.PunishmentType;
import me.leoko.advancedban.utils.commands.PunishmentProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Leoko @ dev.skamps.eu on 24.07.2016.
 */
public class ChatListenerHytale {

    public void onChat(PlayerChatEvent event) {
        PlayerRef player = event.getSender();

        // There's no command events right now so we just check chat and not commands
        /*if (!event.isCommand()) {
            // If they're muted
            if (Universal.get().getMethods().callChat(event.getSender())) {
                event.setCancelled(true);
            }

        } else {
            // if they try send a blocked command whilst being muted
            if (Universal.get().getMethods().callCMD(event.getSender(), event.getMessage())) {
                event.setCancelled(true);
            }
        }*/

        // If they're muted
        if (Universal.get().getMethods().callChat(event.getSender())) {
            event.setCancelled(true);
        }

        // Check for filtered words. If the player is staff, don't do anything for commands (so we can still delete inappropriate names etc)
        //if (event.isCommand() && player.hasPermission("group.trialmod")) return;

        // If it's a chat, don't do anything if the event was cancelled
        //if (!event.isCommand() && event.isCancelled()) return;

        List<String> filteredWords = new ArrayList<>(Universal.get().immediateBanWords);
        filteredWords.addAll(Universal.get().warnWords);

        // We want to ensure things like &nig (underlined ig) don't get caught. So translate colours first
        // Unsure how to strip colours just yet saw getRawText returns null
        //String colouredMessage = Universal.get().getMethods().clearFormatting(ColourUtils.colour(event.getContent()));
        String colouredMessage = event.getContent();

        for (String filteredWord : filteredWords) {
            // If the message doesn't contain any signs of the word, continue
            if (!colouredMessage.toLowerCase().contains(filteredWord.toLowerCase())) continue;

            // This pattern below matches any filtered word that has no letters before or after it.
            // It WILL catch non-letters before or after (like _penis, penis&, etc)
            String pattern = "(?<![\\p{L}])" + filteredWord + "(?![\\p{L}])";
            Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(colouredMessage);

            // It contains a filtered word
            if (matcher.find()) {
                //String server = player.getServer().getInfo().getName();

                // If it's a warn word and their first offense, warn them (then log them so we know they've been warned)
                if (Universal.get().warnWords.contains(filteredWord)) {
                    List<String> caughtWords = Universal.get().caughtWarnWords.getOrDefault(player.getUuid(), new ArrayList<>());

                    if (!caughtWords.contains(filteredWord)) {
                        event.setCancelled(true);
                        player.sendMessage(ColourUtils.colour("&cYou tried to use the phrase '" + filteredWord + "'" +
                                " which results in an immediate permanent ban. As this is your first time, we have" +
                                " prevented you from using it. Please do not use it again if you want to remain on the server"));

                        caughtWords.add(filteredWord);
                        Universal.get().caughtWarnWords.put(player.getUuid(), caughtWords);

                        Universal.get().log("Warned " + player.getUsername() + " for use of the phrase '" + filteredWord + "'");
                        WarnWordsLog.logToFile("Warned " + player.getUsername() + " (" + player.getUuid() + ") for use of the phrase '" + filteredWord + "'. Full message: '" + event.getContent() + "'");
                    }

                    // Else it is an auto-ban word so insta-ban and post the proof
                } else {
                    event.setCancelled(true);
                    String args = player.getUsername() + " Use of an illegal phrase: " + filteredWord;

                    new PunishmentProcessor(PunishmentType.BAN).accept(new Command.CommandInput(ConsoleSender.INSTANCE,
                            args.split(" ")));

                    // Again not 100% sure how to schedule
                    /*ProxyServer.getInstance().getScheduler().schedule(HytaleMain.get(), () -> {
                        DiscordWebhookManager.sendDiscordMessage("`" + player.getName() + "` said `" + event.getMessage() + "`" +
                                " on " + server);
                        }, 1, TimeUnit.SECONDS);*/

                    DiscordWebhookManager.sendDiscordMessage("`" + player.getUsername() + "` said `" + event.getContent() + "`");
                }

                break;
            }
        }

        filteredWords.clear();
    }
}
