/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.robotics.statements;

import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.statements.StatementParameterItemStack;
import buildcraft.core.lib.utils.StringUtils;
import net.minecraft.client.renderer.texture.IIconRegister;

public class ActionStationAcceptItems extends ActionStationInputItems {

    public ActionStationAcceptItems() {
        super("buildcraft:station.accept_items");
        StatementManager.statements.put("buildcraft:station.drop_in_pipe", this);
    }

    @Override
    public String getDescription() {
        return StringUtils.localize("gate.action.station.accept_items");
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        icon = iconRegister.registerIcon("buildcraftrobotics:triggers/action_station_accept_items");
    }

    @Override
    public int maxParameters() {
        return 3;
    }

    @Override
    public IStatementParameter createParameter(int index) {
        return new StatementParameterItemStack();
    }
}
