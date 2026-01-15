package me.leoko.advancedban.hytale.event;

import com.hypixel.hytale.event.IEvent;
import me.leoko.advancedban.utils.Punishment;

/**
 * Event fired when a punishment is revoked
 */

public class RevokePunishmentEvent implements IEvent<Void> {
    private final Punishment punishment;
    private final boolean massClear;

    public RevokePunishmentEvent(Punishment punishment, boolean massClear) {
        this.punishment = punishment;
        this.massClear = massClear;
    }

    public Punishment getPunishment() {
        return punishment;
    }

    public boolean isMassClear() {
        return massClear;
    }
}