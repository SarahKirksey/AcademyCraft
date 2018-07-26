package cn.academy.support.rf;

import cn.academy.core.block.TileReceiverBase;
import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import static cn.academy.support.rf.RFSupport.if2rf;
import static cn.academy.support.rf.RFSupport.rf2if;

@RegTileEntity
@RegWithName("rf_output")
public class TileRFOutput extends TileReceiverBase implements IEnergyProvider {

    public TileRFOutput() {
        super("ac_rf_output", 0, 2000, 100);
    }
    
    @Override
    public void update() {
        super.update();
        World world = getWorld();
        if(!world.isRemote) {
            for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                int x = x + dir.offsetX,
                    y = y + dir.offsetY,
                    z = z + dir.offsetZ;
                TileEntity te = world.getTileEntity(x, y, z);
                if(te instanceof IEnergyReceiver && energy > 0) {
                    IEnergyReceiver receiver = (IEnergyReceiver) te;
                    ForgeDirection rev = dir.getOpposite();
                    if(receiver.canConnectEnergy(rev)) {
                        int req = receiver.getMaxEnergyStored(rev) - receiver.getEnergyStored(rev);
                        req = Math.min(if2rf(energy), req);
                        energy -= rf2if(receiver.receiveEnergy(rev, req, false));
                    }
                }
            }
        }
    }

    @Override
    public boolean canConnectEnergy(ForgeDirection from) {
        return true;
    }

    @Override
    public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
        int e = (int) energy;
        if(!simulate) {
            energy -= rf2if(maxExtract);
            if(energy < 0) energy = 0;
        }
        return Math.min(if2rf(e), maxExtract);
    }

    @Override
    public int getEnergyStored(ForgeDirection from) {
        return if2rf(energy);
    }

    @Override
    public int getMaxEnergyStored(ForgeDirection from) {
        return if2rf(2000);
    }

}