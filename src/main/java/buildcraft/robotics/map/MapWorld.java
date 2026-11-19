package buildcraft.robotics.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.IThreadedFileIO;
import net.minecraft.world.storage.ThreadedFileIOBase;

import buildcraft.api.core.BCLog;
import buildcraft.core.lib.utils.NBTUtils;
import gnu.trove.map.hash.TLongLongHashMap;
import gnu.trove.set.hash.TLongHashSet;

public class MapWorld {

    private final LongHashMap regionMap;
    private final HashMap<Chunk, Integer> timeToUpdate = new HashMap<>();
    private final TLongLongHashMap regionUpdateTime;
    private final TLongHashSet updatedRegions;
    private final File location;
    /**
     * Region snapshots waiting to be written, newest per region. One writer drains it at a time: Minecraft's IO thread
     * interleaves queued tasks, so separate batches could write an older snapshot after a newer one.
     */
    private final LinkedHashMap<Long, PendingSave> pendingSaves = new LinkedHashMap<>();
    private boolean writerQueued;

    public MapWorld(World world, File location) {
        regionMap = new LongHashMap();
        regionUpdateTime = new TLongLongHashMap();
        updatedRegions = new TLongHashSet();

        String saveFolder = world.provider.getSaveFolder();
        if (saveFolder == null) {
            saveFolder = "world";
        }
        this.location = new File(location, saveFolder);
        try {
            this.location.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MapRegion getRegion(int x, int z) {
        long id = MapUtils.getIDFromCoords(x, z);
        MapRegion region = (MapRegion) regionMap.getValueByKey(id);
        if (region == null) {
            region = new MapRegion(x, z);

            // Check in the location first
            File target = new File(location, "r" + x + "," + z + ".nbt");
            if (target.exists()) {
                try {
                    FileInputStream f = new FileInputStream(target);
                    byte[] data = new byte[(int) target.length()];
                    f.read(data);
                    f.close();

                    NBTTagCompound nbt = NBTUtils.load(data);
                    if (nbt != null) {
                        region.readFromNBT(nbt);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            regionMap.add(id, region);
        }
        return region;
    }

    private MapChunk getChunk(int x, int z) {
        MapRegion region = getRegion(x >> 4, z >> 4);
        return region.getChunk(x & 15, z & 15);
    }

    public boolean hasChunk(int x, int z) {
        MapRegion region = getRegion(x >> 4, z >> 4);
        return region.hasChunk(x & 15, z & 15);
    }

    /**
     * Copies the changed regions and queues them on Minecraft's file IO thread, so the disk writes stay off the server
     * thread and are flushed together with the world's own saves.
     */
    public void save() {
        long[] regionList;
        synchronized (updatedRegions) {
            regionList = updatedRegions.toArray();
            updatedRegions.clear();
        }

        boolean queueWriter = false;
        for (long id : regionList) {
            MapRegion region = (MapRegion) regionMap.getValueByKey(id);
            if (region == null) {
                continue;
            }

            NBTTagCompound output = new NBTTagCompound();
            region.writeToNBT(output);
            File file = new File(location, "r" + region.getX() + "," + region.getZ() + ".nbt");
            // Copied, as the chunks keep updating the arrays they wrote into the tag.
            PendingSave save = new PendingSave(id, file, (NBTTagCompound) output.copy());
            synchronized (pendingSaves) {
                pendingSaves.put(id, save);
                if (!writerQueued) {
                    writerQueued = true;
                    queueWriter = true;
                }
            }
        }

        if (queueWriter) {
            // A new instance each time: the IO thread skips a task it still holds, even one that just finished.
            ThreadedFileIOBase.threadedIOInstance.queueIO(new SaveWriter());
        }
    }

    private void write(PendingSave save) throws IOException {
        File temp = new File(save.file.getParentFile(), save.file.getName() + ".tmp");
        try (FileOutputStream f = new FileOutputStream(temp)) {
            f.write(NBTUtils.save(save.data));
        }
        try {
            Files.move(
                    temp.toPath(),
                    save.file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp.toPath(), save.file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static final class PendingSave {

        private final long id;
        private final File file;
        private final NBTTagCompound data;

        private PendingSave(long id, File file, NBTTagCompound data) {
            this.id = id;
            this.file = file;
            this.data = data;
        }
    }

    private final class SaveWriter implements IThreadedFileIO {

        @Override
        public boolean writeNextIO() {
            PendingSave save;
            synchronized (pendingSaves) {
                Iterator<PendingSave> it = pendingSaves.values().iterator();
                if (!it.hasNext()) {
                    writerQueued = false;
                    return false;
                }
                save = it.next();
                it.remove();
            }
            try {
                write(save);
            } catch (IOException | RuntimeException e) {
                // Never let a failed map write kill the shared file IO thread; try again on the next save.
                BCLog.logger.warn("Could not save zone map region " + save.file, e);
                synchronized (updatedRegions) {
                    updatedRegions.add(save.id);
                }
            }
            return true;
        }
    }

    public int getColor(int x, int z) {
        MapChunk chunk = getChunk(x >> 4, z >> 4);
        return chunk.getColor(x & 15, z & 15);
    }

    public void tick() {
        if (timeToUpdate.size() > 0) {
            synchronized (timeToUpdate) {
                Set<Chunk> chunks = new HashSet<>();
                chunks.addAll(timeToUpdate.keySet());
                for (Chunk c : chunks) {
                    int v = timeToUpdate.get(c);
                    if (v > 1) {
                        timeToUpdate.put(c, v - 1);
                    } else {
                        try {
                            updateChunk(c);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void updateChunk(Chunk rchunk) {
        long id = MapUtils.getIDFromCoords(rchunk.xPosition, rchunk.zPosition);
        MapChunk chunk = getChunk(rchunk.xPosition, rchunk.zPosition);
        chunk.update(rchunk);
        synchronized (updatedRegions) {
            updatedRegions.add(MapUtils.getIDFromCoords(rchunk.xPosition >> 4, rchunk.zPosition >> 4));
        }
        synchronized (timeToUpdate) {
            timeToUpdate.remove(rchunk);
        }
        regionUpdateTime.put(id, (new Date()).getTime());
    }

    public long getUpdateTime(int x, int z) {
        return regionUpdateTime.get(MapUtils.getIDFromCoords(x, z));
    }

    public void updateChunkDelayed(Chunk chunk, byte time) {
        synchronized (timeToUpdate) {
            timeToUpdate.put(chunk, (int) time);
        }
    }
}
