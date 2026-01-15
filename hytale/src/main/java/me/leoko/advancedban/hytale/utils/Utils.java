package me.leoko.advancedban.hytale.utils;

import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import io.netty.channel.Channel;
import io.netty.handler.codec.quic.QuicStreamChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String getIPFromPacketHandler(PacketHandler packetHandler) {
        Channel ch = packetHandler.getChannel();
        SocketAddress remote;
        if (ch instanceof QuicStreamChannel quic) {
            remote = quic.parent().remoteSocketAddress();
        } else {
            remote = ch.remoteAddress();
        }

        if (remote instanceof InetSocketAddress inet) {
            // This is the clean numeric IP like "203.0.113.10"
            if (inet.getAddress() != null) {
                return inet.getAddress().getHostAddress();
            }
            // Fallback if address unresolved
            return inet.getHostString();
        }

        return "NULL";
    }
}
