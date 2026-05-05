package buildcraft.core.lib.gui;

import java.util.ArrayList;

import net.minecraft.util.IIcon;

import buildcraft.api.statements.IStatement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Created by asie on 1/24/15.
 */
public abstract class StatementSlot extends AdvancedSlot {

    public int slot;
    public ArrayList<StatementParameterSlot> parameters = new ArrayList<>();

    public StatementSlot(GuiAdvancedInterface gui, int x, int y, int slot) {
        super(gui, x, y);

        this.slot = slot;
    }

    @Override
    public String getDescription() {
        IStatement stmt = getStatement();

        if (stmt != null) {
            return stmt.getDescription();
        } else {
            return "";
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon() {
        IStatement stmt = getStatement();

        if (stmt != null) {
            return stmt.getIcon();
        } else {
            return null;
        }
    }

    @Override
    public boolean isDefined() {
        return getStatement() != null;
    }

    public abstract IStatement getStatement();

    @Override
    public boolean shouldDrawHighlight() {
        return false;
    }
}
