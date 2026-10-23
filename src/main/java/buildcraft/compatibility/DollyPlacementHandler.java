package buildcraft.compatibility;

import static buildcraft.factory.BlockTankValidator.canPlaceTankInBetween;
import static buildcraft.factory.BlockTankValidator.checkFluidForPlacement;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import mcp.mobius.betterbarrels.api.events.DollyPlaceEvent;

/**
 * Handles JABBA Dolly placement compatibility with Tanks
 * <p>
 * Prevents placing a tank containing a different fluid directly above or bellow an existing tank. Placement is allowed
 * if either tank is empty or both contain the same fluid.
 *
 * <p>
 * Registered on {@link MinecraftForge#EVENT_BUS} when JABBA is loaded.
 */
public class DollyPlacementHandler {

    @SubscribeEvent
    public void onDollyPlace(DollyPlaceEvent event) {
        TileEntity aboveTarget = event.world.getTileEntity(event.targX, event.targY + 1, event.targZ);
        TileEntity bellowTarget = event.world.getTileEntity(event.targX, event.targY - 1, event.targZ);

        String fluidOnDolly = event.tileNBT.getCompoundTag("tank").getString("FluidName");

        if (!(checkFluidForPlacement(bellowTarget, fluidOnDolly)
                && checkFluidForPlacement(aboveTarget, fluidOnDolly))) {
            event.player.addChatMessage(new ChatComponentTranslation("buildcraft.tanks.differentFluid"));
            event.setCanceled(true);
            return;
        }

        if (!(canPlaceTankInBetween(aboveTarget, bellowTarget))) {
            event.player.addChatMessage(new ChatComponentTranslation("buildcraft.tanks.cannotConnectTanks"));
            event.setCanceled(true);
        }
    }
}
