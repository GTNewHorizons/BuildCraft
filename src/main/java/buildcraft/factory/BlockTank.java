/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.factory;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidContainerItem;

import buildcraft.BuildCraftCore;
import buildcraft.core.BCCreativeTab;
import buildcraft.core.lib.block.BlockBuildCraft;
import buildcraft.core.lib.inventory.InvUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockTank extends BlockBuildCraft {

    private static final boolean DEBUG_MODE = false;
    private IIcon textureStackedSide;

    public BlockTank() {
        super(Material.glass);
        setBlockBounds(0.125F, 0F, 0.125F, 0.875F, 1F, 0.875F);
        setHardness(0.5F);
        setCreativeTab(BCCreativeTab.get("main"));
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int par6) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile instanceof TileTank) {
            TileTank tank = (TileTank) tile;
            tank.onBlockBreak();
        }

        TileEntity tileAbove = world.getTileEntity(x, y + 1, z);
        TileEntity tileBelow = world.getTileEntity(x, y - 1, z);

        super.breakBlock(world, x, y, z, block, par6);

        if (tileAbove instanceof TileTank) {
            ((TileTank) tileAbove).updateComparators();
        }

        if (tileBelow instanceof TileTank) {
            ((TileTank) tileBelow).updateComparators();
        }
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileTank();
    }

    @SuppressWarnings({ "all" })
    @Override
    public IIcon getIconAbsolute(IBlockAccess iblockaccess, int i, int j, int k, int side, int metadata) {
        if (side >= 2 && iblockaccess.getBlock(i, j - 1, k) instanceof BlockTank) {
            return textureStackedSide;
        } else {
            return super.getIconAbsolute(side, metadata);
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
            float hitY, float hitZ) {
        if (super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)) return true;

        ItemStack held = player.inventory.getCurrentItem();
        if (held == null) return false;

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileTank)) return false;
        TileTank clicked = (TileTank) te;

        final boolean creative = player.capabilities.isCreativeMode;

        // Regular Fluid Container (FC) Contingencies (IC2 / GT / Flask / Bucket)
        if (FluidContainerRegistry.isContainer(held)) {
            FluidStack fromItem = FluidContainerRegistry.getFluidForFilledItem(held);

            // container -) tank logic
            if (fromItem != null) {
                int moved = clicked.fill(ForgeDirection.UNKNOWN, fromItem, true);

                if (moved > 0 && !BuildCraftCore.debugWorldgen && !creative) {
                    if (held.stackSize > 1) {
                        ItemStack emptied = FluidContainerRegistry.drainFluidContainer(held);
                        addOrDrop(player, emptied);
                        player.inventory
                                .setInventorySlotContents(player.inventory.currentItem, InvUtils.consumeItem(held));
                    } else {
                        player.inventory.setInventorySlotContents(
                                player.inventory.currentItem,
                                FluidContainerRegistry.drainFluidContainer(held));
                    }
                }
                syncPlayerAndTanks(world, x, y, z, player, clicked);
                return true;
            }

            // tank -) container logic
            FluidStack available = clicked.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;
            if (available != null) {
                ItemStack preview = FluidContainerRegistry.fillFluidContainer(available, held);
                FluidStack willContain = FluidContainerRegistry.getFluidForFilledItem(preview);
                if (willContain != null) {
                    if (!BuildCraftCore.debugWorldgen && !creative) {
                        if (held.stackSize > 1) {
                            if (!player.inventory.addItemStackToInventory(preview)) return false;
                            player.inventory
                                    .setInventorySlotContents(player.inventory.currentItem, InvUtils.consumeItem(held));
                        } else {
                            player.inventory
                                    .setInventorySlotContents(player.inventory.currentItem, InvUtils.consumeItem(held));
                            player.inventory.setInventorySlotContents(player.inventory.currentItem, preview);
                        }
                    }
                    clicked.drain(ForgeDirection.UNKNOWN, willContain.amount, true);
                    syncPlayerAndTanks(world, x, y, z, player, clicked);
                    return true;
                }
            }
            return false;
        }

        // IFluidContainerItem (IFCI) Logic (Large Fluid Cells)
        if (!(held.getItem() instanceof IFluidContainerItem)) return false;
        if (world.isRemote) return true; // server does transfers

        final IFluidContainerItem cont = (IFluidContainerItem) held.getItem();
        final boolean sneaking = player.isSneaking();
        final int INF_MB = Integer.MAX_VALUE / 4;

        // Support for stacked cells
        ItemStack work = held.copy();
        work.stackSize = 1;

        // prioritizes the bottom for tank stacks
        TileTank bottom = getBottomTank(world, x, y, z);
        if (bottom == null) return true;

        int moved = 0;

        // Push from cell -) tank
        if (!sneaking) {
            FluidStack inCell = cont.getFluid(work);
            if (inCell != null && inCell.amount > 0) {
                int canInto = bottom.fill(ForgeDirection.UNKNOWN, inCell, false);
                if (canInto > 0) {
                    FluidStack simDrain = cont.drain(work, canInto, false);
                    if (simDrain != null && simDrain.amount > 0) {
                        if (creative) {
                            moved = bottom.fill(ForgeDirection.UNKNOWN, simDrain, true); // tank changes, item stays
                        } else {
                            FluidStack drained = cont.drain(work, simDrain.amount, true);
                            if (drained != null && drained.amount > 0) {
                                moved = bottom.fill(ForgeDirection.UNKNOWN, drained, true);
                            }
                        }
                    }
                }
            }
        }

        // Pull from cells -) tank (stack friendly)
        if (sneaking) {
            FluidStack inCell = cont.getFluid(work);
            FluidStack stackFluid = bottom.getTankInfo(ForgeDirection.UNKNOWN)[0].fluid;
            net.minecraftforge.fluids.Fluid target = (inCell != null && inCell.amount > 0) ? inCell.getFluid()
                    : (stackFluid != null ? stackFluid.getFluid() : null);

            if (target != null) {
                int accept = cont.fill(work, new FluidStack(target, INF_MB), false);
                if (accept > 0) {
                    // tank stack protection consistency
                    FluidStack drained = bottom.drain(ForgeDirection.UNKNOWN, new FluidStack(target, accept), true);
                    if (drained != null && drained.amount > 0) {
                        if (creative) {
                            moved = drained.amount; // tank changed, item remains
                        } else {
                            int filled = cont.fill(work, drained, true);
                            moved = filled;

                            // remainder logic
                            if (filled < drained.amount) {
                                FluidStack rem = drained.copy();
                                rem.amount = drained.amount - filled;
                                bottom.fill(ForgeDirection.UNKNOWN, rem, true);
                            }
                        }
                    }
                }
            }
        }

        if (moved > 0) {
            final int slot = player.inventory.currentItem;

            if (creative) {
                player.inventory.setInventorySlotContents(slot, held);
                syncPlayerAndTanks(world, x, y, z, player, clicked, bottom);
                return true;
            }

            if (held.stackSize > 1) {
                held.stackSize -= 1;
                player.inventory.setInventorySlotContents(slot, held);
                addOrDrop(player, work);
            } else {
                player.inventory.setInventorySlotContents(slot, work);
            }

            syncPlayerAndTanks(world, x, y, z, player, clicked, bottom);
            return true;
        }
        return true;
    }

    private static TileTank getBottomTank(World world, int x, int y, int z) {
        int boty = y;
        while (true) {
            TileEntity below = world.getTileEntity(x, boty - 1, z);
            if (below instanceof TileTank) boty--;
            else break;
        }
        TileEntity te = world.getTileEntity(x, boty, z);
        return (te instanceof TileTank) ? (TileTank) te : null;
    }

    private static void addOrDrop(EntityPlayer player, ItemStack stack) {
        if (stack == null) return;
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropPlayerItemWithRandomChoice(stack, false);
        }
    }

    private static void syncPlayerAndTanks(World world, int x, int y, int z, EntityPlayer player, TileTank... tanks) {
        player.inventory.markDirty();
        if (player.openContainer != null) player.openContainer.detectAndSendChanges();
        if (player.inventoryContainer != null) player.inventoryContainer.detectAndSendChanges();
        for (TileTank tt : tanks) {
            if (tt == null) continue;
            tt.markDirty();
            world.markBlockForUpdate(tt.xCoord, tt.yCoord, tt.zCoord);
        }
        world.markBlockForUpdate(x, y, z);
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        if (side <= 1) {
            return !(world.getBlock(x, y, z) instanceof BlockTank);
        } else {
            return super.shouldSideBeRendered(world, x, y, z, side);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        super.registerBlockIcons(par1IconRegister);
        textureStackedSide = par1IconRegister.registerIcon("buildcraftfactory:tankBlock/side_stacked");
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);

        if (tile instanceof TileTank) {
            TileTank tank = (TileTank) tile;
            return tank.getFluidLightLevel();
        }

        return super.getLightValue(world, x, y, z);
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntity tile = world.getTileEntity(x, y, z);

        if (tile instanceof TileTank) {
            TileTank tank = (TileTank) tile;
            return tank.getComparatorInputOverride();
        }

        return 0;
    }
}
