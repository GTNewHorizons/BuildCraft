package buildcraft.builders.render;

import buildcraft.BuildCraftBuilders;
import buildcraft.builders.TileBuilder;
import buildcraft.core.lib.render.RenderEntityBlock;
import buildcraft.core.render.RenderBuilder;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class RenderBuilderTile extends RenderBuilder {
    private static final float Z_OFFSET = 2049 / 2048.0F;

    @Override
    public void renderTileEntityAt(TileEntity tileentity, double x, double y, double z, float f) {
        super.renderTileEntityAt(tileentity, x, y, z, f);

        bindTexture(TextureMap.locationBlocksTexture);
        RenderEntityBlock.RenderInfo renderBox = new RenderEntityBlock.RenderInfo();

        GL11.glPushMatrix();

        GL11.glTranslatef((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        GL11.glScalef(Z_OFFSET, Z_OFFSET, Z_OFFSET);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        renderBox.setRenderSingleSide(1);
        renderBox.texture = BuildCraftBuilders.builderBlock.blockTopOn;
        renderBox.light = ((TileBuilder) tileentity).buildersInAction.size() > 0 ? 15 : 0;
        RenderEntityBlock.INSTANCE.renderBlock(renderBox);

        GL11.glPopMatrix();
    }
}
