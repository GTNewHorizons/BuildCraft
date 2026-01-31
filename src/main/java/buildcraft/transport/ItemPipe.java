/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.transport;

import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.Level;

import com.google.common.collect.ImmutableMap;

import buildcraft.BuildCraftTransport;
import buildcraft.api.core.BCLog;
import buildcraft.api.core.IIconProvider;
import buildcraft.api.transport.IItemPipe;
import buildcraft.core.BCCreativeTab;
import buildcraft.core.lib.items.ItemBuildCraft;
import buildcraft.core.lib.utils.ColorUtils;
import buildcraft.core.lib.utils.StringUtils;
import buildcraft.transport.pipes.PipePowerCobblestone;
import buildcraft.transport.pipes.PipePowerDiamond;
import buildcraft.transport.pipes.PipePowerEmerald;
import buildcraft.transport.pipes.PipePowerGold;
import buildcraft.transport.pipes.PipePowerIron;
import buildcraft.transport.pipes.PipePowerQuartz;
import buildcraft.transport.pipes.PipePowerSandstone;
import buildcraft.transport.pipes.PipePowerStone;
import buildcraft.transport.pipes.PipePowerWood;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemPipe extends ItemBuildCraft implements IItemPipe {

    @SideOnly(Side.CLIENT)
    private IIconProvider iconProvider;

    private int pipeIconIndex;

    protected ItemPipe(BCCreativeTab creativeTab) {
        super(creativeTab);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
    }

    @Override
    public boolean onItemUse(ItemStack itemstack, EntityPlayer entityplayer, World world, int x, int y, int z,
            int sideI, float par8, float par9, float par10) {
        int side = sideI;
        Block block = BuildCraftTransport.genericPipeBlock;

        int i = x;
        int j = y;
        int k = z;

        Block worldBlock = world.getBlock(i, j, k);

        if (worldBlock == Blocks.snow) {
            side = 1;
        } else if (worldBlock != Blocks.vine && worldBlock != Blocks.tallgrass
                && worldBlock != Blocks.deadbush
                && (worldBlock == null || !worldBlock.isReplaceable(world, i, j, k))) {
                    if (side == 0) {
                        j--;
                    }
                    if (side == 1) {
                        j++;
                    }
                    if (side == 2) {
                        k--;
                    }
                    if (side == 3) {
                        k++;
                    }
                    if (side == 4) {
                        i--;
                    }
                    if (side == 5) {
                        i++;
                    }
                }

        if (itemstack.stackSize == 0) {
            return false;
        }

        if (world.canPlaceEntityOnSide(block, i, j, k, false, side, entityplayer, itemstack)) {
            Pipe<?> pipe = BlockGenericPipe.createPipe(this);

            if (pipe == null) {
                BCLog.logger.log(Level.WARN, "Pipe failed to create during placement at {0},{1},{2}", i, j, k);
                return false;
            }

            if (BlockGenericPipe
                    .placePipe(pipe, world, i, j, k, block, 0, entityplayer, ForgeDirection.getOrientation(sideI))) {
                block.onBlockPlacedBy(world, i, j, k, entityplayer, itemstack);

                if (!world.isRemote) {
                    TileEntity tile = world.getTileEntity(i, j, k);
                    ((TileGenericPipe) tile).initializeFromItemMetadata(itemstack.getItemDamage());
                }

                world.playSoundEffect(
                        i + 0.5F,
                        j + 0.5F,
                        k + 0.5F,
                        block.stepSound.func_150496_b(),
                        (block.stepSound.getVolume() + 1.0F) / 2.0F,
                        block.stepSound.getPitch() * 0.8F);

                itemstack.stackSize--;

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    public void setPipesIcons(IIconProvider iconProvider) {
        this.iconProvider = iconProvider;
    }

    public void setPipeIconIndex(int index) {
        this.pipeIconIndex = index;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int par1) {
        if (iconProvider != null) { // invalid pipes won't have this set
            return iconProvider.getIcon(pipeIconIndex);
        } else {
            return null;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
        // NOOP
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        if (stack.getItemDamage() >= 1) {
            int color = (stack.getItemDamage() - 1) & 15;
            tooltip.add(
                    ColorUtils.getFormattingTooltip(color) + EnumChatFormatting.ITALIC
                            + StringUtils.localize("color." + ColorUtils.getName(color)));
        }
        Class<? extends Pipe<?>> pipe = BlockGenericPipe.pipes.get(this);
        List<String> toolTip = PipeToolTipManager.getToolTip(pipe, advanced);
        if (!toolTip.isEmpty()) {
            for (int i = 0; i < toolTip.size() - 1; i++) {
                tooltip.add(toolTip.get(i));
            }
            EnumChatFormatting pipeColor = getPipeColor(pipe);
            tooltip.add(pipeColor + toolTip.get(toolTip.size() - 1));
        }
    }

    private static final Map<Class<? extends Pipe<?>>, EnumChatFormatting> PIPE_COLORS = ImmutableMap
            .<Class<? extends Pipe<?>>, EnumChatFormatting>builder()
            .put(PipePowerDiamond.class, EnumChatFormatting.DARK_AQUA)
            .put(PipePowerEmerald.class, EnumChatFormatting.DARK_GREEN)
            .put(PipePowerGold.class, EnumChatFormatting.GOLD).put(PipePowerIron.class, EnumChatFormatting.BLUE)
            .put(PipePowerQuartz.class, EnumChatFormatting.DARK_GRAY)
            .put(PipePowerWood.class, EnumChatFormatting.DARK_GREEN)
            .put(PipePowerSandstone.class, EnumChatFormatting.DARK_PURPLE)
            .put(PipePowerStone.class, EnumChatFormatting.AQUA)
            .put(PipePowerCobblestone.class, EnumChatFormatting.DARK_RED).build();

    private EnumChatFormatting getPipeColor(Class<? extends Pipe<?>> pipe) {
        return PIPE_COLORS.getOrDefault(pipe, EnumChatFormatting.GRAY);
    }
}
