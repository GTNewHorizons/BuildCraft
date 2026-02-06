/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 * <p/>
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents
 * of the license located in http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.silicon.gui;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import buildcraft.core.lib.utils.StringUtils;
import buildcraft.silicon.TileAdvancedCraftingTable;

public class GuiAdvancedCraftingTable extends GuiLaserTable {

    public static final ResourceLocation TEXTURE = new ResourceLocation(
            "buildcraftsilicon:textures/gui/assembly_advancedworkbench.png");
    private final TileAdvancedCraftingTable workbench;

    public GuiAdvancedCraftingTable(InventoryPlayer playerInventory, TileAdvancedCraftingTable advancedWorkbench) {
        super(
                playerInventory,
                new ContainerAdvancedCraftingTable(playerInventory, advancedWorkbench),
                advancedWorkbench,
                TEXTURE);
        this.workbench = advancedWorkbench;
        xSize = 176;
        ySize = 235;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int par1, int par2) {
        drawLedgers(par1, par2);
        String title = StringUtils.localize("gui.tile.assemblyWorkbenchBlock.name.title");
        fontRendererObj.drawString(title, getCenteredOffset(title), 6, 0x404040);
        fontRendererObj.drawString(StringUtils.localize("gui.inventory"), 8, ySize - 93, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(f, mouseX, mouseY);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        mc.renderEngine.bindTexture(TEXTURE);
        if (workbench.getEnergy() > 0) {
            int progress = workbench.getProgressScaled(24);
            drawTexturedModalRect(guiLeft + 93, guiTop + 32, 176, 0, progress + 1, 18);
        }
    }
}
