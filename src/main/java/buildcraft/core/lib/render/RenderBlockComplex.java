package buildcraft.core.lib.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;

import buildcraft.BuildCraftCore;
import buildcraft.core.lib.block.BlockBuildCraft;
import buildcraft.core.render.BCSimpleBlockRenderingHandler;

@ThreadSafeISBRH(perThread = true)
public class RenderBlockComplex extends BCSimpleBlockRenderingHandler {

    private static final int[] Y_ROTATE = { 3, 0, 1, 2 };

    private final FakeBlock fakeBlock = new FakeBlock();

    @Override
    public void renderInventoryBlock(Block block, int meta, int modelId, RenderBlocks renderer) {
        GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        BlockBuildCraft bcBlock = (BlockBuildCraft) block;
        for (int pass = 0; pass < bcBlock.getPassCount(); pass++) {
            renderPassInventory(pass, bcBlock, meta, renderer);
        }
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    private void renderPassInventory(int pass, BlockBuildCraft block, int meta, RenderBlocks renderer) {
        if (block.isRotatable()) {
            renderer.uvRotateTop = Y_ROTATE[block.getFrontSide(meta) - 2];
            renderer.uvRotateBottom = Y_ROTATE[block.getFrontSide(meta) - 2];
        }

        Tessellator tess = Tessellator.instance;
        RenderUtils.drawBlockItem(renderer, tess, block, meta, pass);

        renderer.uvRotateTop = 0;
        renderer.uvRotateBottom = 0;
    }

    private void renderPassWorld(int pass, BlockBuildCraft block, int meta, RenderBlocks renderer, IBlockAccess world,
            int x, int y, int z) {
        if (block.isRotatable()) {
            renderer.uvRotateTop = Y_ROTATE[block.getFrontSide(meta) - 2];
            renderer.uvRotateBottom = Y_ROTATE[block.getFrontSide(meta) - 2];
        }

        double pDouble = (pass > 0 ? 1 : 0) / 512.0;
        renderer.setRenderBounds(
                block.getBlockBoundsMinX() - pDouble,
                block.getBlockBoundsMinY() - pDouble,
                block.getBlockBoundsMinZ() - pDouble,
                block.getBlockBoundsMaxX() + pDouble,
                block.getBlockBoundsMaxY() + pDouble,
                block.getBlockBoundsMaxZ() + pDouble);

        if (pass == 0) {
            renderer.renderStandardBlock(block, x, y, z);
        } else {
            renderOverlayPass(pass, block, renderer, world, x, y, z);
        }

        renderer.uvRotateTop = 0;
        renderer.uvRotateBottom = 0;
    }

    private void renderOverlayPass(int pass, BlockBuildCraft block, RenderBlocks renderer, IBlockAccess world, int x,
            int y, int z) {
        IIcon[] icons = fakeBlock.getTextureState().popArray();
        int mask = 0;
        for (int side = 0; side < 6; side++) {
            IIcon icon = block.getIconForPass(world, x, y, z, side, pass);
            icons[side] = icon;
            ForgeDirection dir = ForgeDirection.getOrientation(side);
            if (icon != null
                    && block.shouldSideBeRendered(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, side)) {
                mask |= 1 << side;
            }
        }

        if (mask != 0) {
            fakeBlock.setRenderMask(mask);
            renderer.renderStandardBlock(fakeBlock, x, y, z);
        }

        fakeBlock.getTextureState().pushArray();
        fakeBlock.setRenderAllSides();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
            RenderBlocks renderer) {
        BlockBuildCraft bcBlock = (BlockBuildCraft) block;
        int meta = world.getBlockMetadata(x, y, z);

        int startPass = Math.max(0, ForgeHooksClient.getWorldRenderPass());
        for (int pass = startPass; pass < bcBlock.getPassCount(); pass++) {
            renderPassWorld(pass, bcBlock, meta, renderer, world, x, y, z);
        }
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return BuildCraftCore.complexBlockModel;
    }
}
