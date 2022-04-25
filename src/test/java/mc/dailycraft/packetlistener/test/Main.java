package mc.dailycraft.packetlistener.test;

import mc.dailycraft.packetlistener.PacketListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main class of the test and example plugin.
 */
public class Main extends JavaPlugin {
    /**
     * The basic override of {@link JavaPlugin#onEnable()} for register the packet listener.
     */
    @Override
    public void onEnable() {
        PacketListener.registerListeners(this, Packets.class);
    }

    /**
     * A simple method to get the class instance.
     *
     * @return The class instance.
     */
    public static Main getInstance() {
        return getPlugin(Main.class);
    }
}