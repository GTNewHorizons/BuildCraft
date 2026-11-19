package buildcraft.robotics.map;

public final class MapUtils {

    private MapUtils() {}

    public static long getIDFromCoords(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int getXFromID(long id) {
        return (int) (id >> 32);
    }

    public static int getZFromID(long id) {
        return (int) id;
    }
}
