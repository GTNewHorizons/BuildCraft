package buildcraft.builders;

import buildcraft.builders.gui.ContainerBuilder;
import buildcraft.core.blueprints.RequirementItemStack;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.List;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

/**
 * Created by asie on 10/6/15.
 */
public class BuilderTooltipHandler {
    @SubscribeEvent
    public void itemTooltipEvent(ItemTooltipEvent event) {
        if (event.itemStack != null
                && event.entityPlayer != null
                && event.entityPlayer.openContainer != null
                && event.entityPlayer.openContainer instanceof ContainerBuilder) {
            ContainerBuilder containerBuilder = (ContainerBuilder) event.entityPlayer.openContainer;
            TileBuilder builder = containerBuilder.getBuilder();
            if (builder != null) {
                List<RequirementItemStack> needs = builder.getNeededItems();
                if (needs != null) {
                    for (RequirementItemStack ris : needs) {
                        if (ris.stack == event.itemStack) {
                            event.toolTip.add(
                                    EnumChatFormatting.GRAY + "" + EnumChatFormatting.ITALIC + "Needed: " + ris.size);
                        }
                    }
                }
            }
        }
    }
}
