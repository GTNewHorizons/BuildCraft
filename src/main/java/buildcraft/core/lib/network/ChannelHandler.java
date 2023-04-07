/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.core.lib.network;

import java.lang.ref.WeakReference;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.INetHandler;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import buildcraft.api.core.BCLog;
import buildcraft.core.lib.network.command.PacketCommand;
import buildcraft.core.proxy.CoreProxy;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;
import gnu.trove.map.TByteIntMap;
import gnu.trove.map.hash.TByteIntHashMap;
import gnu.trove.map.hash.TByteObjectHashMap;
import gnu.trove.map.hash.TObjectByteHashMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.AttributeKey;

/**
 * Code based on FMLIndexedMessageToMessageCodec, but since some of its fields are private, I needed a custom version.
 */
@io.netty.channel.ChannelHandler.Sharable
public final class ChannelHandler extends MessageToMessageCodec<FMLProxyPacket, Packet> {

    public static final int CLIENT_ONLY = 1;
    public static final int SERVER_ONLY = 2;
    public static final int BOTH_SIDES = SERVER_ONLY | CLIENT_ONLY;

    public static final Marker SUSPICIOUS_PACKETS = MarkerManager.getMarker("SuspiciousPackets");
    public static final AttributeKey<ThreadLocal<WeakReference<FMLProxyPacket>>> INBOUNDPACKETTRACKER = new AttributeKey<ThreadLocal<WeakReference<FMLProxyPacket>>>(
            "bc:inboundpacket");
    private TByteObjectHashMap<Class<? extends Packet>> discriminators = new TByteObjectHashMap<Class<? extends Packet>>();
    private TObjectByteHashMap<Class<? extends Packet>> types = new TObjectByteHashMap<Class<? extends Packet>>();
    private TByteIntMap sides = new TByteIntHashMap();
    private int maxDiscriminator;

    public ChannelHandler() {
        // Packets common to buildcraft.core.network
        addDiscriminator(0, PacketTileUpdate.class, CLIENT_ONLY);
        addDiscriminator(1, PacketTileState.class, CLIENT_ONLY);
        addDiscriminator(2, PacketNBT.class);
        addDiscriminator(3, PacketSlotChange.class);
        addDiscriminator(4, PacketGuiReturn.class);
        addDiscriminator(5, PacketGuiWidget.class);
        // this won't work whatsoever. this is a freaking abstract class
        // addDiscriminator(6, PacketUpdate.class);
        addDiscriminator(7, PacketCommand.class);
        addDiscriminator(8, PacketEntityUpdate.class, CLIENT_ONLY);
        maxDiscriminator = 9;
    }

    protected void logForgedPackets(EntityPlayer player, String packetType, ByteBuf rawPacket) {
        BCLog.logger.info(
                SUSPICIOUS_PACKETS,
                "Player {} tried to send packet of type {} to invalid side {}. This could be a false warning due to custom mods/addon, or it could be an legit attempt at hacking!",
                player.getGameProfile(),
                player instanceof EntityPlayerMP ? Side.SERVER : Side.CLIENT,
                packetType);
    }

    public byte getDiscriminator(Class<? extends Packet> clazz) {
        return types.get(clazz);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        ctx.attr(INBOUNDPACKETTRACKER).set(new ThreadLocal<WeakReference<FMLProxyPacket>>());
    }

    public ChannelHandler addDiscriminator(int discriminator, Class<? extends Packet> type) {
        return addDiscriminator(discriminator, type, BOTH_SIDES);
    }

    public ChannelHandler addDiscriminator(int discriminator, Class<? extends Packet> type, int handlingSide) {
        discriminators.put((byte) discriminator, type);
        types.put(type, (byte) discriminator);
        sides.put((byte) discriminator, handlingSide);
        return this;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, List<Object> out) throws Exception {
        ByteBuf buffer = Unpooled.buffer();
        Class<? extends Packet> clazz = msg.getClass();
        byte discriminator = types.get(clazz);
        buffer.writeByte(discriminator);
        msg.writeData(buffer);
        FMLProxyPacket proxy = new FMLProxyPacket(buffer.copy(), ctx.channel().attr(NetworkRegistry.FML_CHANNEL).get());
        WeakReference<FMLProxyPacket> ref = ctx.attr(INBOUNDPACKETTRACKER).get().get();
        FMLProxyPacket old = ref == null ? null : ref.get();
        if (old != null) {
            proxy.setDispatcher(old.getDispatcher());
        }
        out.add(proxy);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
        testMessageValidity(msg);
        ByteBuf payload = msg.payload();
        byte discriminator = payload.readByte();
        Class<? extends Packet> clazz = discriminators.get(discriminator);
        if (clazz == null) {
            throw new NullPointerException(
                    "Undefined message for discriminator " + discriminator + " in channel " + msg.channel());
        }
        int expectedSide = ctx.channel().attr(NetworkRegistry.CHANNEL_SOURCE).get() == Side.CLIENT ? CLIENT_ONLY
                : SERVER_ONLY;
        if ((expectedSide & sides.get(discriminator)) != expectedSide) {
            INetHandler handler = ctx.channel().attr(NetworkRegistry.NET_HANDLER).get();
            EntityPlayer player = CoreProxy.proxy.getPlayerFromNetHandler(handler);
            logForgedPackets(player, clazz.getSimpleName(), payload.slice());
        } else {
            Packet newMsg = clazz.newInstance();
            ctx.attr(INBOUNDPACKETTRACKER).get().set(new WeakReference<>(msg));
            newMsg.readData(payload.slice());
            out.add(newMsg);
        }
    }

    /**
     * Called to verify the message received. This can be used to hard disconnect in case of an unexpected packet, say
     * due to a weird protocol mismatch. Use with caution.
     * 
     * @param msg
     */
    protected void testMessageValidity(FMLProxyPacket msg) {}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        FMLLog.log(Level.ERROR, cause, "BC ChannelHandler exception caught");
        super.exceptionCaught(ctx, cause);
    }

    public void registerPacketType(Class<? extends Packet> packetType) {
        addDiscriminator(maxDiscriminator++, packetType);
    }

    public void registerPacketType(Class<? extends Packet> packetType, int handlingSide) {
        addDiscriminator(maxDiscriminator++, packetType);
    }
}
