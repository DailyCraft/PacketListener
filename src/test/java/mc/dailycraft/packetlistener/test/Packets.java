package mc.dailycraft.packetlistener.test;

import mc.dailycraft.packetlistener.PacketEvent;
import mc.dailycraft.packetlistener.PacketHandler;
import mc.dailycraft.packetlistener.PacketListener;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;

import java.util.Arrays;

/**
 * The packet listener class of the test and example plugin.
 */
public class Packets {
    /**
     * A method handled each time a client sends a chat packet to the server.
     *
     * @param event The packet event.
     */
    @PacketHandler
    public static void inChat(PacketEvent<ServerboundChatPacket> event) {
        if (event.getPacket().getMessage().equalsIgnoreCase("gg")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("'gg' cannot be sent as it may create spam");

            PacketListener.waitPacketAsync(Main.getInstance(), ServerboundSignUpdatePacket.class, event.getPlayer(),
                    event1 -> Arrays.asList(event1.getPacket().getLines()).forEach(event.getPlayer()::sendMessage), 10000,
                    () -> event.getPlayer().sendMessage("Ho... You don't have updated a sign..."));
        }
    }

    /**
     * A method called whenever the server sends an update to the client about the entity's movement.
     *
     * @param event The packet event.
     */
    @PacketHandler
    public static void outMoveEntity(PacketEvent<ClientboundMoveEntityPacket> event) {
        System.out.println(event.getPacket().getClass());
    }

    /**
     * A method called each time a packet is sent or received.
     *
     * @param event The packet event.
     */
    @PacketHandler
    public static void all(PacketEvent<?> event) {
        System.out.println(event.getPacket().getClass());
    }

    /**
     * Same as {@link Packets#all(PacketEvent)} but the parameter {@code event} is written differently.
     *
     * @param event The packet event.
     */
    @PacketHandler
    public static void all_(PacketEvent event) {
        System.out.println(event.getPacket().getClass());
    }
}