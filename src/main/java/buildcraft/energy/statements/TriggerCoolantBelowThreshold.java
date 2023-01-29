/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.energy.statements;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import buildcraft.api.statements.IStatementContainer;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.ITriggerExternal;
import buildcraft.core.lib.utils.StringUtils;
import buildcraft.core.statements.BCStatement;
import buildcraft.energy.TileEngineIron;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TriggerCoolantBelowThreshold extends BCStatement implements ITriggerExternal {

    private float threshold;

    public TriggerCoolantBelowThreshold(float threshold) {
        super("buildcraft:trigger.coolantLevelBelow." + (int) (threshold * 100));
        this.threshold = threshold;
    }

    @Override
    public String getDescription() {
        return String.format(StringUtils.localize("gate.trigger.coolantLevelBelow"), (int) (threshold * 100));
    }

    @Override
    public boolean isTriggerActive(TileEntity target, ForgeDirection side, IStatementContainer source,
            IStatementParameter[] parameters) {
        if (!(target instanceof TileEngineIron)) {
            return false;
        }

        return ((TileEngineIron) target).hasCoolantBelowThreshold(threshold);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        icon = iconRegister.registerIcon("buildcraftenergy:triggers/trigger_coolant_below_threshold");
    }
}
