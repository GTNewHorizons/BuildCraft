package buildcraft.factory;

import static buildcraft.factory.BlockTankValidator.canPlaceTankInBetween;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.event.world.BlockEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class BlockTankPlacementHandler {

    @SubscribeEvent
    public void onTankPlace(BlockEvent.PlaceEvent event) {
        TileEntity aboveTarget = event.world.getTileEntity(event.x, event.y + 1, event.z);
        TileEntity bellowTarget = event.world.getTileEntity(event.x, event.y - 1, event.z);

        if (!(canPlaceTankInBetween(aboveTarget, bellowTarget))) {
            event.player.addChatMessage(new ChatComponentTranslation("buildcraft.tanks.cannotConnectTanks"));
            event.setCanceled(true);
        }
    }
}
