# Packet Listener

This library for Bukkit allows you to have greater control over the packets sent and received on the server. You can modify
packet, execute actions and cancel the packet.

It allows packets to be retrieved in an event system similar to that of Bukkit. You can also pause the program while
waiting for a packet to be sent or received.

Note: This library is currently only compatible with Minecraft 1.18.2.

## How to use it ?

### Listener

First, you need to tell the api which classes contain handlers. This action is usually in the [`JavaPlugin#onEnable()`](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/plugin/java/JavaPlugin.html#onEnable()).

```java
PacketListener.registerListeners(this, ServerboundPackets.class, ClientboundPackets.class, ...);
```

Afterwards, to create a handler you must create a method in `public static void` with the annotation [`@PacketHandler`](src/main/java/mc/dailycraft/packetlistener/PacketHandler.java)
and in parameter [`PacketEvent<?>`](src/main/java/mc/dailycraft/packetlistener/PacketEvent.java) by replacing the `?` by the desired packet, or leave it if you want your method to
handle all packets.

```java
@PacketHandler
public static void inChat(PacketEvent<ServerboundChatPacket> event) {
    // do something
    // you can prevent the packet from being sent or received with
    event.setCancelled(true);
}
```

### Waiting a packet

This API also allows you to wait for a packet through a blocking method

```java
PacketListener.waitPacket(ServerboundSignUpdatePacket.class /* The packet class */,
        Bukkit.getPlayer("DailyCraft") /* The player */,
        event -> {} /* A Consumer with a PacketEvent for make actions with the packet */,
        1000 /* A timeout in ms. Note: this arg is optional */);
```

And there's also `PacketListener.waitPacketAsync(...)` so the method isn't blocking because running in an async thread.
Note: The arguments are the same except that you must add the instance of your JavaPlugin class as the first argument.

### Example

You can find an example [here](src/test/java/mc/dailycraft/packetlistener/test)