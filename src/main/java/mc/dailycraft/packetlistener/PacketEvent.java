package mc.dailycraft.packetlistener;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * The class to manage the event
 *
 * @param <T> The packet managed by the class
 */
public class PacketEvent<T extends Packet<?>> {
    private final T packet;
    private final Player player;
    private final ChannelHandlerContext handlerContext;
    private boolean cancelled;

    /**
     * The main constructor.
     *
     * @param packet         The Packet.
     * @param player         The player.
     * @param handlerContext The handler context for in-death actions.
     */
    public PacketEvent(T packet, Player player, ChannelHandlerContext handlerContext) {
        this.packet = packet;
        this.player = player;
        this.handlerContext = handlerContext;
    }

    /**
     * A getter to get and modify the packet.
     *
     * @return The current packet.
     */
    public T getPacket() {
        return packet;
    }

    /**
     * The player where the packet is sent or received.
     *
     * @return The player. Can be return null if the packet is a login packet otherwise always returns the player.
     */
    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * Allows to retrieve the package handler context to do more in-depth actions.
     *
     * @return The handler context.
     */
    public ChannelHandlerContext getHandlerContext() {
        return handlerContext;
    }

    /**
     * @return {@code true} if the packet is cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Allows you to change the cancellation state.
     *
     * @param cancelled The new value.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}