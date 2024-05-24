package me.leoko.advancedban.utils;

import java.util.List;

public class RecentBan {

    private final Punishment punishment;
    private final String ip;
    private final long banedAtTime;
    private final List<String> caughtNames;

    public RecentBan(Punishment punishment, String ip, long banedAtTime, List<String> caughtNames) {
        this.punishment = punishment;
        this.ip = ip;
        this.banedAtTime = banedAtTime;
        this.caughtNames = caughtNames;
    }

    public Punishment getPunishment() {
        return punishment;
    }

    public String getIp() {
        return ip;
    }

    public long getBanedAtTime() {
        return banedAtTime;
    }

    public List<String> getCaughtNames() {
        return caughtNames;
    }
}
