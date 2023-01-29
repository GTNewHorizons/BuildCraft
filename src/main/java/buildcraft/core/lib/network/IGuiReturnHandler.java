/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.core.lib.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import io.netty.buffer.ByteBuf;

public interface IGuiReturnHandler {

    World getWorld();

    void writeGuiData(ByteBuf data);

    void readGuiData(ByteBuf data, EntityPlayer player);
}
