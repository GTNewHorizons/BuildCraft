package buildcraft.factory;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import buildcraft.core.lib.items.ItemBlockBuildCraft;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemBlockTank extends ItemBlockBuildCraft {

    public ItemBlockTank(Block b) {
        super(b);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        String text = StatCollector.translateToLocal("tile.tankBlock.tooltip");
        for (String line : text.split("\\\\n")) {
            tooltip.add(line);
        }
    }
}
