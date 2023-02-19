/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.core;

import java.util.List;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import buildcraft.BuildCraftCore;
import buildcraft.api.blocks.IColorRemovable;
import buildcraft.api.core.EnumColor;
import buildcraft.core.lib.items.ItemBuildCraft;
import buildcraft.core.lib.utils.NBTUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemPaintbrush extends ItemBuildCraft {

    public ItemPaintbrush() {
        super();

        setFull3D();
        setMaxStackSize(1);
        setMaxDamage(63);
    }

    private int getColor(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return -1;
        }
        NBTTagCompound compound = NBTUtils.getItemData(stack);
        return compound.hasKey("color") ? compound.getByte("color") : -1;
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (damage > getMaxDamage()) {
            stack.setTagCompound(null);
            super.setDamage(stack, 0);
        } else {
            super.setDamage(stack, damage);
        }
    }

    @Override
    public String[] getIconNames() {
        String[] names = new String[17];
        names[0] = "paintbrush/clean";
        for (int i = 0; i < 16; i++) {
            names[1 + i] = "paintbrush/" + EnumColor.fromId(i).name().toLowerCase(Locale.ENGLISH);
        }
        return names;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
        super.registerIcons(par1IconRegister);

        IIcon[] brushColors = new IIcon[16];
        System.arraycopy(icons, 1, brushColors, 0, 16);
        EnumColor.setIconArray(brushColors);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        this.itemIcon = icons[(getColor(stack) + 1) % icons.length];
        return itemIcon;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String base = super.getItemStackDisplayName(stack);
        int dye = getColor(stack);
        if (dye >= 0) {
            return base + " (" + EnumColor.fromId(dye).getLocalizedName() + ")";
        } else {
            return base;
        }
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        int dye = getColor(stack);

        Block block = world.getBlock(x, y, z);

        if (block == null) {
            return false;
        }

        if (dye >= 0) {
            int painted = 0;
            int maxPainted = BuildCraftCore.maxPaintedBlocks;
            // Direction player is looking at
            ForgeDirection lookSide;
            Vec3 look = player.getLookVec();
            double absX = Math.abs(look.xCoord);
            double absY = Math.abs(look.yCoord);
            double absZ = Math.abs(look.zCoord);

            if (absX > absY && absX > absZ) {
                lookSide = look.xCoord > 0 ? ForgeDirection.EAST : ForgeDirection.WEST;
            } else if (absY > absX && absY > absZ) {
                lookSide = look.yCoord > 0 ? ForgeDirection.UP : ForgeDirection.DOWN;
            } else {
                lookSide = look.zCoord > 0 ? ForgeDirection.SOUTH : ForgeDirection.NORTH;
            }

            while (block.recolourBlock(world, x, y, z, ForgeDirection.getOrientation(side), 15 - dye)) {
                player.swingItem();
                setDamage(stack, getDamage(stack) + 1);
                dye = getColor(stack);

                painted++;
                if (painted >= maxPainted && maxPainted != -1) return !world.isRemote;

                if (!player.isSneaking() || dye <= 0) return !world.isRemote;
                switch (lookSide) {
                    case UP:
                        y += 1;
                        break;
                    case DOWN:
                        y -= 1;
                        break;
                    case NORTH:
                        z -= 1;
                        break;
                    case SOUTH:
                        z += 1;
                        break;
                    case WEST:
                        x -= 1;
                        break;
                    case EAST:
                        x += 1;
                        break;

                }
                if (y <= 0 || y > 256) {
                    return !world.isRemote;
                }
                block = world.getBlock(x, y, z);
                if (block == null) {
                    return !world.isRemote;
                }
            }

        } else {
            // NOTE: Clean paintbrushes never damage.
            if (block instanceof IColorRemovable) {
                if (((IColorRemovable) block)
                        .removeColorFromBlock(world, x, y, z, ForgeDirection.getOrientation(side))) {
                    player.swingItem();
                    return !world.isRemote;
                }
            }
        }

        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList) {
        itemList.add(new ItemStack(this));
        for (int i = 0; i < 16; i++) {
            ItemStack stack = new ItemStack(this);
            NBTUtils.getItemData(stack).setByte("color", (byte) i);
            itemList.add(stack);
        }
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }
}
