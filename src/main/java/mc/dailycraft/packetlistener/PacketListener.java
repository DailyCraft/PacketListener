package mc.dailycraft.packetlistener;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_18_R2.CraftServer;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * The API main class.
 */
public class PacketListener implements Listener {
    private static final Map<Class<? extends Packet<?>>, List<Method>> handlers = new HashMap<>();
    private static boolean eventsRegistered = false;

    private PacketListener() {
    }

    /**
     * Register packet listeners.
     *
     * @param plugin    Your {@link JavaPlugin} instance.
     * @param listeners Classes of your listeners.
     */
    public static void registerListeners(JavaPlugin plugin, Class<?>... listeners) {
        if (!eventsRegistered) {
            Bukkit.getPluginManager().registerEvents(new PacketListener(), plugin);
            eventsRegistered = true;
        }

        for (Class<?> listener : listeners) {
            for (Method method : listener.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PacketHandler.class)) {
                    if (method.getParameters().length == 1 && method.getParameters()[0].getType() == PacketEvent.class) {
                        Type parameterType = method.getGenericParameterTypes()[0];
                        Type actualType = null;

                        if (parameterType instanceof Class<?>)
                            actualType = Packet.class;
                        else if (parameterType instanceof ParameterizedType) {
                            Type type = ((ParameterizedType) parameterType).getActualTypeArguments()[0];
                            if (type instanceof Class<?>)
                                actualType = type;
                            else if (type instanceof WildcardType) {
                                if (((WildcardType) type).getUpperBounds()[0] == null || ((WildcardType) type).getUpperBounds()[0] == Object.class)
                                    actualType = Packet.class;
                                else
                                    System.err.println("Method '" + method + "' doesn't have correct parameters");
                            } else
                                System.err.println("Method '" + method + "' doesn't have correct parameters");
                        }

                        handlers.computeIfAbsent((Class<? extends Packet<?>>) actualType, key -> new ArrayList<>()).add(method);
                    } else
                        System.err.println("Method '" + method + "' annotated with " + PacketHandler.class.getCanonicalName() + " doesn't have correct parameters");
                }
            }
        }
    }

    /**
     * Wait a specific packet with a blocking method.
     *
     * @param packet       The packet class.
     * @param player       The player where the packet is sent or received.
     * @param doWithPacket A {@link Consumer} where you can make actions (cancel for example)
     * @param timeout      A timeout (in ms) if the packet was not detect before.
     * @param <T>          The type of the packet.
     * @return {@code true} if the end of the thread's pause is not caused by the timeout.
     */
    public static <T extends Packet<?>> boolean waitPacket(Class<T> packet, Player player, Consumer<PacketEvent<T>> doWithPacket, long timeout) {
        long lastTime = System.currentTimeMillis();
        AtomicBoolean sleeping = new AtomicBoolean(true);

        String channelPipelineName = "dailycraft_packet_listener_wait_" + UUID.randomUUID();
        ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();
        pipeline.addBefore("packet_handler", channelPipelineName, new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
                if (msg.getClass() == packet) {
                    PacketEvent<T> packetEvent = new PacketEvent<>((T) msg, player, context);
                    doWithPacket.accept(packetEvent);

                    if (!packetEvent.isCancelled()) {
                        super.channelRead(context, msg);
                        sleeping.set(false);
                    }
                } else
                    super.channelRead(context, msg);
            }

            @Override
            public void write(ChannelHandlerContext context, Object msg, ChannelPromise promise) throws Exception {
                if (msg.getClass() == packet) {
                    PacketEvent<T> packetEvent = new PacketEvent<>((T) msg, player, context);
                    doWithPacket.accept(packetEvent);

                    if (!packetEvent.isCancelled()) {
                        super.write(context, msg, promise);
                        sleeping.set(false);
                    }
                } else
                    super.write(context, msg, promise);
            }
        });

        boolean b = true;
        while (sleeping.get() && (b = !(lastTime + timeout <= System.currentTimeMillis()))) ;
        pipeline.remove(channelPipelineName);
        return b;
    }

    /**
     * Same as {@link PacketListener#waitPacket(Class, Player, Consumer, long)} but without timeout.
     *
     * @param packet       The packet class.
     * @param player       The player where the packet is sent or received.
     * @param doWithPacket A {@link Consumer} where you can make actions (cancel for example)
     * @param <T>          The type of the packet.
     */
    public static <T extends Packet<?>> void waitPacket(Class<T> packet, Player player, Consumer<PacketEvent<T>> doWithPacket) {
        waitPacket(packet, player, doWithPacket, -1);
    }

    /**
     * Same as {@link PacketListener#waitPacket(Class, Player, Consumer, long)} but in async, so the method isn't blocking.
     *
     * @param plugin       Your {@link JavaPlugin} instance.
     * @param packet       The packet class.
     * @param player       The player where the packet is sent or received.
     * @param doWithPacket A {@link Consumer} where you can make actions (cancel for example)
     * @param timeout      A timeout (in ms) if the packet was not detect before.
     * @param timeoutElse  A {@link Runnable} executed if the timeout is exceeded.
     * @param <T>          The type of the packet.
     */
    public static <T extends Packet<?>> void waitPacketAsync(JavaPlugin plugin, Class<T> packet, Player player, Consumer<PacketEvent<T>> doWithPacket, long timeout, Runnable timeoutElse) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!waitPacket(packet, player, doWithPacket, timeout)) {
                timeoutElse.run();
            }
        });
    }

    /**
     * Same as {@link PacketListener#waitPacketAsync(JavaPlugin, Class, Player, Consumer, long, Runnable)} but without timeout.
     *
     * @param plugin       Your {@link JavaPlugin} instance.
     * @param packet       The packet class.
     * @param player       The player where the packet is sent or received.
     * @param doWithPacket A {@link Consumer} where you can make actions (cancel for example)
     * @param <T>          The type of the packet.
     */
    public static <T extends Packet<?>> void waitPacketAsync(JavaPlugin plugin, Class<T> packet, Player player, Consumer<PacketEvent<T>> doWithPacket) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> waitPacket(packet, player, doWithPacket));
    }

    // -------------------- //

    /**
     * A bukkit event allowing the API to operate.
     *
     * @param event The Bukkit event.
     */
    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        List<Connection> connections = ((CraftServer) Bukkit.getServer()).getServer().getConnection().getConnections();
        Connection connection = null;

        for (int i = connections.size() - 1; i >= 0; --i) {
            connection = connections.get(i).getPacketListener().getConnection();

            if (((InetSocketAddress) connection.address).getAddress().getAddress() == event.getAddress().getAddress())
                break;
        }

        final Connection finalConnection = connection;
        connection.channel.pipeline().addBefore("packet_handler", "dailycraft_packet_listener", new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext context, Object msg) throws Exception {
                if (handle(context, msg))
                    super.channelRead(context, msg);
            }

            @Override
            public void write(ChannelHandlerContext context, Object msg, ChannelPromise promise) throws Exception {
                if (handle(context, msg))
                    super.write(context, msg, promise);
            }

            private boolean handle(ChannelHandlerContext context, Object msg) {
                boolean cancel = false;

                for (Map.Entry<Class<? extends Packet<?>>, List<Method>> entry : handlers.entrySet()) {
                    if (entry.getKey().isInstance(msg)) {
                        for (Method method : entry.getValue()) {
                            try {
                                PacketEvent<?> event = new PacketEvent<>((Packet<?>) msg, finalConnection.getPacketListener() instanceof ServerGamePacketListenerImpl ? ((ServerGamePacketListenerImpl) finalConnection.getPacketListener()).getCraftPlayer() : null, context);
                                event.setCancelled(cancel);
                                method.invoke(null, event);
                                cancel = event.isCancelled();
                            } catch (IllegalAccessException | InvocationTargetException exception) {
                                exception.printStackTrace();
                            }
                        }
                    }
                }

                return !cancel;
            }
        });
    }
}