package me.leoko.advancedban.hytale.managers;

import me.leoko.advancedban.Universal;
import me.leoko.advancedban.hytale.HytaleMain;
import me.leoko.advancedban.hytale.utils.DiscordWebhook;
import me.leoko.advancedban.utils.Punishment;
import me.leoko.advancedban.utils.PunishmentType;

import java.awt.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordWebhookManager {

    public static void sendDiscordEmbed(String title, Punishment punishment) {
        Universal.get().getMethods().runAsync(() -> {
            String url = Universal.get().getMethods().getString(Universal.get().getMethods().getConfig(), "DiscordWebhook");
            DiscordWebhook webhook = new DiscordWebhook(url);
            DiscordWebhook.EmbedObject embedObject = new DiscordWebhook.EmbedObject();

            embedObject.setColor(getEmbedColour(punishment.getType()));
            embedObject.setTitle(title);

            // Using certain characters in the punishment reason will prevent the webhook from sending so we need to make it words, numbers, and punctuation only
            String reason = punishment.getReason();

            Pattern pattern = Pattern.compile("[^a-zA-Z0-9\\p{Punct}/ ]|[\"\\\\]"); // Four backslashes removes a literal backslash
            Matcher matcher = pattern.matcher(reason);

            reason = matcher.replaceAll("");

            if (punishment.getEnd() == -1) {
                embedObject.setDescription("The player `" + punishment.getName() + "` " + createPunishmentString(punishment.getType()) + " by `" + punishment.getOperator() + "` for `" + reason + "`\\n\\nPlayer UUID: `" + punishment.getNormalUUID() + "`");

            } else {
                embedObject.setDescription("The player `" + punishment.getName() + "` " + createPunishmentString(punishment.getType()) + " by `" + punishment.getOperator() + "` for `" + reason + "`. Duration: `" + punishment.getDuration(true) + "`\\n\\nPlayer UUID: `" + punishment.getNormalUUID() + "`");
            }

            webhook.addEmbed(embedObject);

            try {
                webhook.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendDiscordMessage(String message) {
        Universal.get().getMethods().runAsync(() -> {
            String url = Universal.get().getMethods().getString(Universal.get().getMethods().getConfig(), "DiscordWebhook");
            DiscordWebhook webhook = new DiscordWebhook(url);
            webhook.setContent(message);

            try {
                webhook.execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static String createPunishmentString(PunishmentType punishmentType) {
        switch (punishmentType) {
            case BAN:
                return "was banned";

            case KICK:
                return "was kicked";

            case MUTE:
                return "was muted";

            case NOTE:
                return "was noted";

            case IP_BAN:
                return "was IP banned";

            case WARNING:
                return "was warned";

            case TEMP_BAN:
                return "was temporarily banned";

            case TEMP_MUTE:
                return "was temporarily muted";

            case TEMP_IP_BAN:
                return "was temporarily IP banned";

            case TEMP_WARNING:
                return "was temporarily warned";
        }

        return "was somethinged";
    }

    private static Color getEmbedColour(PunishmentType punishmentType) {
        switch (punishmentType) {
            case BAN:
            case IP_BAN:
            case TEMP_BAN:
            case TEMP_IP_BAN:
                return Color.RED;

            case TEMP_MUTE:
            case MUTE:
                return Color.ORANGE;

            case KICK:
                return Color.CYAN;

            case WARNING:
            case TEMP_WARNING:
                return Color.YELLOW;

            case NOTE:
                return Color.GREEN;
        }

        return Color.CYAN;
    }
}
