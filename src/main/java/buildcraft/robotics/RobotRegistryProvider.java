package buildcraft.robotics;

import java.util.HashMap;

import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import buildcraft.api.robots.DockingStation;
import buildcraft.api.robots.IRobotRegistryProvider;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class RobotRegistryProvider implements IRobotRegistryProvider {

    private static final String WORLD_DATA_KEY = "robotRegistry";

    private final HashMap<Integer, RobotRegistry> registries = new HashMap<>();

    public RobotRegistryProvider() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public synchronized RobotRegistry getRegistry(World world) {
        final RobotRegistry registry = registries.get(world.provider.dimensionId);
        if (registry == null || registry.world != world) {

            RobotRegistry newRegistry = (RobotRegistry) world.perWorldStorage
                    .loadData(RobotRegistry.class, WORLD_DATA_KEY);

            if (newRegistry == null) {
                newRegistry = new RobotRegistry(WORLD_DATA_KEY);
                world.perWorldStorage.setData(WORLD_DATA_KEY, newRegistry);
            }

            newRegistry.world = world;

            for (DockingStation d : newRegistry.stations.values()) {
                d.world = world;
            }

            MinecraftForge.EVENT_BUS.register(newRegistry);

            registries.put(world.provider.dimensionId, newRegistry);

            return newRegistry;
        }

        return registry;
    }

    @SubscribeEvent
    public synchronized void onWorldUnload(WorldEvent.Unload event) {
        final RobotRegistry registry = registries.get(event.world.provider.dimensionId);
        if (registry != null && registry.world == event.world) {
            MinecraftForge.EVENT_BUS.unregister(registry);
            registries.remove(event.world.provider.dimensionId);
        }
    }
}
