package buildcraft.factory;

import java.util.Objects;

import net.minecraft.tileentity.TileEntity;

public class BlockTankValidator {

    /**
     * Helper function for the tank placement first check
     * <p>
     * Check if fluid on {@link TileTank} tank is fluidName. This function gets the name of liquid from tank for a
     * comparison of strings
     * <p>
     * Special Case: The code checks if tank fluid or fluidName is null, in that case the returns is always true, this
     * happens due to in that case the tank is empty, allowing placement
     *
     * @param tank      Tank with fluid
     * @param fluidName Target Fluid
     * @return true if tank is ok for placement, false tank can't be placed here
     */
    public static boolean checkFluidForPlacement(TileEntity tank, String fluidName) {
        if (!(tank instanceof TileTank)) return true;

        TileTank bottomTank = ((TileTank) tank).getBottomTank();

        if (bottomTank.tank.getFluid() == null || fluidName.isEmpty()) return true;

        return Objects.equals(bottomTank.tank.getFluid().getFluid().getName(), fluidName);
    }

    /**
     * Helper function for the tank placement second check.
     * <p>
     * Checks whether placing a tank between two existing tanks would connect tanks containing different fluids.
     *
     * <p>
     * Placement is allowed if either tank is empty, or both tanks contain the same fluid.
     *
     * @param tankAbove the tank directly above the placement
     * @param tankBelow the tank directly below the placement position
     * @return true if the tanks can connect, false if they can't connect
     */
    public static boolean canPlaceTankInBetween(TileEntity tankAbove, TileEntity tankBelow) {
        if (!(tankAbove instanceof TileTank)) return true;
        if (!(tankBelow instanceof TileTank)) return true;

        TileTank bottomTank = ((TileTank) tankBelow).getBottomTank();
        TileTank aboveTank = ((TileTank) tankAbove).getBottomTank();

        if (bottomTank.tank.getFluid() == null || aboveTank.tank.getFluid() == null) return true;

        return Objects.equals(
                bottomTank.tank.getFluid().getFluid().getName(),
                aboveTank.tank.getFluid().getFluid().getName());
    }
}
