package buildcraft.robotics.map;

import java.io.File;
import java.util.Date;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.google.common.collect.HashBiMap;

import buildcraft.core.lib.utils.Utils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

public class MapManager {

    private static final int UPDATE_DELAY = 60000;
    private final HashBiMap<World, MapWorld> worldMap = HashBiMap.create();
    private final File location;

    public MapManager(File location) {
        this.location = location;
    }

    public void stop() {
        saveAllWorlds();
        try {
            // Nothing flushes the file IO thread after this point, so wait for the writes just queued.
            ThreadedFileIOBase.threadedIOInstance.waitForFinish();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        synchronized (worldMap) {
            worldMap.clear();
        }
    }

    public MapWorld getWorld(World world) {
        if (world == null || world.isRemote) {
            return null;
        }

        if (!worldMap.containsKey(world)) {
            synchronized (worldMap) {
                worldMap.put(world, new MapWorld(world, location));
            }
        }
        return worldMap.get(world);
    }

    private boolean doUpdate(MapWorld world, Chunk chunk) {
        int x = chunk.xPosition;
        int z = chunk.zPosition;
        long updateTime = (new Date()).getTime() - UPDATE_DELAY;
        return world.getUpdateTime(x, z) < updateTime || !world.hasChunk(x, z);
    }

    private void updateChunk(World rworld, Chunk chunk, boolean force) {
        MapWorld world = getWorld(rworld);
        if (world != null && (force || doUpdate(world, chunk))) {
            world.updateChunk(chunk);
        }
    }

    private void updateChunkDelayed(World rworld, Chunk chunk, boolean force, byte time) {
        MapWorld world = getWorld(rworld);
        if (world != null && (force || doUpdate(world, chunk))) {
            world.updateChunkDelayed(chunk, time);
        }
    }

    @SubscribeEvent
    public void tickDelayedWorlds(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == Side.SERVER) {
            MapWorld w = worldMap.get(event.world);
            if (w != null) {
                w.tick();
            }
        }
    }

    // Saved with the world rather than on a timer, so the files only change when the rest of the save does.
    @SubscribeEvent
    public void worldSaved(WorldEvent.Save event) {
        MapWorld w = worldMap.get(event.world);
        if (w != null) {
            w.save();
        }
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (worldMap.containsKey(event.world)) {
            worldMap.get(event.world).save();
            synchronized (worldMap) {
                worldMap.remove(event.world);
            }
        }
    }

    @SubscribeEvent
    public void chunkLoaded(ChunkEvent.Load event) {
        updateChunkDelayed(event.world, event.getChunk(), false, (byte) (40 + Utils.RANDOM.nextInt(20)));
    }

    @SubscribeEvent
    public void chunkUnloaded(ChunkEvent.Unload event) {
        updateChunk(event.world, event.getChunk(), false);
    }

    @SubscribeEvent
    public void blockPlaced(BlockEvent.PlaceEvent placeEvent) {
        Chunk chunk = placeEvent.world.getChunkFromBlockCoords(placeEvent.x, placeEvent.z);
        MapWorld world = getWorld(placeEvent.world);
        if (world != null && doUpdate(world, chunk)) {
            int hv = placeEvent.world.getHeightValue(placeEvent.x, placeEvent.z);
            if (placeEvent.y >= (hv - 3)) {
                world.updateChunk(chunk);
            }
        }
    }

    @SubscribeEvent
    public void blockBroken(BlockEvent.BreakEvent placeEvent) {
        Chunk chunk = placeEvent.world.getChunkFromBlockCoords(placeEvent.x, placeEvent.z);
        MapWorld world = getWorld(placeEvent.world);
        if (world != null && doUpdate(world, chunk)) {
            int hv = placeEvent.world.getHeightValue(placeEvent.x, placeEvent.z);
            if (placeEvent.y >= (hv - 3)) {
                world.updateChunk(chunk);
            }
        }
    }

    public void saveAllWorlds() {
        synchronized (worldMap) {
            for (MapWorld world : worldMap.values()) {
                world.save();
            }
        }
    }

    public void initialize() {
        for (WorldServer ws : DimensionManager.getWorlds()) {
            MapWorld mw = getWorld(ws);
            IChunkProvider provider = ws.getChunkProvider();
            if (provider instanceof ChunkProviderServer) {
                for (Object o : ((ChunkProviderServer) provider).func_152380_a()) {
                    if (o instanceof Chunk) {
                        Chunk c = (Chunk) o;
                        if (!mw.hasChunk(c.xPosition, c.zPosition)) {
                            mw.updateChunkDelayed(c, (byte) (40 + Utils.RANDOM.nextInt(20)));
                        }
                    }
                }
            }
        }
    }
}
