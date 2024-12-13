package me.leoko.advancedban.utils;

import me.leoko.advancedban.manager.PunishmentManager;

import java.util.Set;

/**
 * Created by Leo on 04.08.2017.
 */
public class InterimData {

    private final String uuid, name, ip;
    private final Set<Punishment> punishments, history;

    public InterimData(String uuid, String name, String ip, Set<Punishment> punishments, Set<Punishment> history) {
        this.uuid = uuid;
        this.name = name;
        this.ip = ip;
        this.punishments = punishments;
        this.history = history;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public Set<Punishment> getPunishments() {
        return punishments;
    }

    public Set<Punishment> getHistory() {
        return history;
    }

    public Punishment getBan() {
        for (Punishment pt : punishments) {
            if (pt.getType().getBasic() == PunishmentType.BAN && !pt.isExpired()) {
                return pt;
            }
        }
        return null;
    }

    public void accept() {
        mainLoop:
        for (Punishment punishment : punishments) {
            for (Punishment loadedPunishment : PunishmentManager.get().getLoadedPunishments(false).values()) {
                if (punishment.getId() == loadedPunishment.getId()) continue mainLoop;
            }

            PunishmentManager.get().addToPunishmentMap(punishment, false, true);
        }

        mainLoop:
        for (Punishment history : history) {
            for (Punishment loadedHistory : PunishmentManager.get().getLoadedHistory().values()) {
                if (history.getId() == loadedHistory.getId()) continue mainLoop;
            }

            PunishmentManager.get().addToHistoryMap(history, true);
        }

        PunishmentManager.get().setCached(this);
    }
}