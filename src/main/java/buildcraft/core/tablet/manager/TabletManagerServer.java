package buildcraft.core.tablet.manager;

import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;

import buildcraft.core.tablet.TabletServer;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class TabletManagerServer {

    public static final TabletManagerServer INSTANCE = new TabletManagerServer();

    private final HashMap<EntityPlayer, TabletThread> threads = new HashMap<>();

    public TabletServer get(EntityPlayer player) {
        if (!threads.containsKey(player)) {
            TabletServer tablet = new TabletServer(player);
            TabletThread thread = new TabletThread(tablet);
            threads.put(player, thread);
            new Thread(thread).start();
        }
        return (TabletServer) threads.get(player).getTablet();
    }

    public void onServerStopping() {
        for (TabletThread thread : threads.values()) {
            thread.stop();
        }
        threads.clear();
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        for (TabletThread thread : threads.values()) {
            thread.tick(0.05F);
        }
    }

    @SubscribeEvent
    public void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TabletThread thread = threads.get(event.player);
        if (thread != null) {
            thread.stop();
            threads.remove(event.player);
        }
    }
}
