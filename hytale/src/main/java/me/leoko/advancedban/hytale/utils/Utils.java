package me.leoko.advancedban.hytale.utils;

import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    /**
     * Resolves the remote IP of a connection without a compile-time dependency on
     * {@code com.hypixel.hytale.protocol.io.ChannelConnection}. That class exists on the
     * live server but is not exposed by the published Server maven artifact we build
     * against, so naming it (e.g. as the return type of {@link PacketHandler#getChannel()})
     * fails the build with "cannot access ChannelConnection". We instead reflect over
     * getChannel()/remoteAddress() - both the legacy io.netty Channel and the current
     * ChannelConnection expose remoteAddress(), so this stays correct against either build.
     */
    public static String getIPFromPacketHandler(PacketHandler packetHandler) {
        try {
            Object channel = PacketHandler.class.getMethod("getChannel").invoke(packetHandler);
            if (channel == null) {
                return "NULL";
            }

            Object remote = channel.getClass().getMethod("remoteAddress").invoke(channel);

            if (remote instanceof InetSocketAddress inet) {
                // This is the clean numeric IP like "203.0.113.10"
                if (inet.getAddress() != null) {
                    return inet.getAddress().getHostAddress();
                }
                // Fallback if address unresolved
                return inet.getHostString();
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to resolve remote IP from packet handler", e);
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
