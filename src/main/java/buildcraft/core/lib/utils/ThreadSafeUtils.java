package buildcraft.core.lib.utils;

import buildcraft.core.lib.network.ChannelHandler;
import buildcraft.core.lib.network.Packet;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class ThreadSafeUtils {

    private ThreadSafeUtils() {}

    /**
     * This function assumes that you're using BC's ChannelHandler system, which only has one channel handler. This
     * might get very messy otherwise. TODO: HACK - Can we rewrite this for BC 7.1 along with the whole network system
     * to be somewhat more sane? Please?
     *
     * @param packet
     * @param channel
     * @return
     */
    public static net.minecraft.network.Packet generatePacketFrom(Packet packet, FMLEmbeddedChannel channel) {
        ByteBuf data = Unpooled.buffer();
        for (io.netty.channel.ChannelHandler h : channel.pipeline().toMap().values()) {
            if (h instanceof ChannelHandler) {
                data.writeByte(((ChannelHandler) h).getDiscriminator(packet.getClass()));
                break;
            }
        }
        packet.writeData(data);
        return new FMLProxyPacket(data.copy(), channel.attr(NetworkRegistry.FML_CHANNEL).get());
    }
}
