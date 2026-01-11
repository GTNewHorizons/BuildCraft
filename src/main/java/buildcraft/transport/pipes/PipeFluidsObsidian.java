package buildcraft.transport.pipes;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import buildcraft.BuildCraftTransport;
import buildcraft.api.core.IIconProvider;
import buildcraft.core.lib.RFBattery;
import buildcraft.core.lib.utils.BlockUtils;
import buildcraft.transport.Pipe;
import buildcraft.transport.PipeIconProvider;
import buildcraft.transport.PipeTransportFluids;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PipeFluidsObsidian extends Pipe<PipeTransportFluids> implements IEnergyHandler {

    private static final int pumpCost = 25;

    private final RFBattery battery = new RFBattery(pumpCost * 3, pumpCost * 3, 0);
    private FluidStack fluidCache;

    public PipeFluidsObsidian(Item item) {
        super(new PipeTransportFluids(), item);

        transport.initFromPipe(getClass());
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        World worldObj = container.getWorldObj();

        if (worldObj.isRemote) return;
        ForgeDirection drainingDirection = getOpenOrientation();

        if (fluidCache == null) {
            if (battery.getEnergyStored() < pumpCost) return;

            fluidCache = drainBlockFromDirection(
                    worldObj,
                    container.xCoord,
                    container.yCoord,
                    container.zCoord,
                    true,
                    drainingDirection);
            if (fluidCache != null) {
                int efficiency = battery.getEnergyStored() / pumpCost;

                fluidCache.amount = (int) (fluidCache.amount * (0.25 * efficiency));
                battery.useEnergy(0, (pumpCost * efficiency), false);
                System.out.println(
                        "Sucked " + fluidCache.getFluid().getUnlocalizedName()
                                + " | "
                                + fluidCache.amount
                                + " Efficiency =");
            }
        } else {
            // We intentionally do ".getOpposite()" for a slower slurpier transfer.
            fluidCache.amount -= this.container.fill(drainingDirection.getOpposite(), fluidCache, true);
            if (fluidCache.amount <= 0) fluidCache = null;
        }
    }

    public FluidStack drainBlockFromDirection(World world, int x, int y, int z, boolean doDrain,
            ForgeDirection direction) {
        if (direction == ForgeDirection.UNKNOWN) return null;

        int neighborX = x + direction.offsetX;
        int neighborY = y + direction.offsetY;
        int neighborZ = z + direction.offsetZ;

        return BlockUtils.drainBlock(world, neighborX, neighborY, neighborZ, doDrain);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIconProvider getIconProvider() {
        return BuildCraftTransport.instance.pipeIconProvider;
    }

    @Override
    public int getIconIndex(ForgeDirection direction) {
        return PipeIconProvider.TYPE.PipeFluidsObsidian.ordinal();
    }

    @Override
    public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
        return battery.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return battery.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return battery.getMaxEnergyStored();
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        if (fluidCache != null) {
            NBTTagCompound fluidInItemTag = new NBTTagCompound();
            fluidCache.writeToNBT(fluidInItemTag);
            nbt.setTag("fluidCache", fluidInItemTag);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        if (nbt.hasKey("fluidCache")) fluidCache = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("fluidCache"));
    }
}
