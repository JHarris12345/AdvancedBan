package me.leoko.advancedban.hytale.utils;

import com.hypixel.hytale.protocol.io.ChannelConnection;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String getIPFromPacketHandler(PacketHandler packetHandler) {
        ChannelConnection channel = packetHandler.getChannel();
        if (channel == null) {
            return "NULL";
        }

        SocketAddress remote = channel.remoteAddress();

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

    // This BLOCKS that calling thread until this code is completed
    public static <T> T runOnWorldThreadBlocking(World world, Callable<T> task) {
        CompletableFuture<T> cf = new CompletableFuture<>();
        world.execute(() -> {
            try {
                cf.complete(task.call());
            } catch (Throwable t) {
                cf.completeExceptionally(t);
            }
        });
        return cf.join();
    }
}
