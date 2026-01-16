package me.leoko.advancedban.hytale.event;

import com.hypixel.hytale.event.IEvent;
import me.leoko.advancedban.hytale.managers.DiscordWebhookManager;
import me.leoko.advancedban.utils.Punishment;

/**
 * Event fired when a punishment is created
 */
public class PunishmentEvent implements IEvent<Void> {
    private final Punishment punishment;

    public PunishmentEvent(Punishment punishment) {
        this.punishment = punishment;
    }

    public Punishment getPunishment() {
        return punishment;
    }
}