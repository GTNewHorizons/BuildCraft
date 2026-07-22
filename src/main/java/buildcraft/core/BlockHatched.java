package buildcraft.core;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import buildcraft.BuildCraftCore;
import buildcraft.api.transport.IPipeTile;
import buildcraft.core.lib.block.BlockBuildCraft;
import buildcraft.core.lib.utils.ResourceUtils;
import buildcraft.core.lib.utils.Utils;

public abstract class BlockHatched extends BlockBuildCraft {

    private IIcon itemHatch;

    protected BlockHatched(Material material) {
        super(material);

        setRotatable(true);
        setPassCount(2);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return 1;
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        String base = ResourceUtils.getObjectPrefix(Block.blockRegistry.getNameForObject(this));
        itemHatch = register.registerIcon(base + "/item_hatch");
    }

    @Override
    public IIcon getIconForPass(IBlockAccess access, int x, int y, int z, int side, int pass) {
        // The quarry's pipe connection method has no idea about "sides".
        if (pass == 1) {
            return Utils.isPipeConnected(access, x, y, z, ForgeDirection.getOrientation(side), IPipeTile.PipeType.ITEM)
                    ? itemHatch
                    : BuildCraftCore.transparentTexture;
        }
        return super.getIconForPass(access, x, y, z, side, pass);
    }

    @Override
    public IIcon getIconForPass(int side, int meta, int pass) {
        if (pass == 1) {
            return side == 1 ? itemHatch : null;
        }
        return super.getIconForPass(side, meta, pass);
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
}
