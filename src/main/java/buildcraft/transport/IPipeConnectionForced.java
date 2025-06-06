/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.transport;

import net.minecraftforge.common.util.ForgeDirection;

public interface IPipeConnectionForced {

    /**
     * Allows you to block connection overrides.
     *
     * @param with
     * @return TRUE to block an override. FALSE to allow overrides.
     */
    boolean ignoreConnectionOverrides(ForgeDirection with);
}
