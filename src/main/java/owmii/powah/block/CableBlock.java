package owmii.powah.block;

import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.energy.CapabilityEnergy;

public final class CableBlock extends MachineBlock {
    public static final PropertyBool[] CONNECTIONS = {PropertyBool.create("down"), PropertyBool.create("up"), PropertyBool.create("north"),
        PropertyBool.create("south"), PropertyBool.create("west"), PropertyBool.create("east")};
    public CableBlock(Tier tier) { super("energy_cable", tier); }
    @Override protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ASSEMBLED, CONNECTIONS[0], CONNECTIONS[1], CONNECTIONS[2], CONNECTIONS[3], CONNECTIONS[4], CONNECTIONS[5]);
    }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new CableTile().configure(kind, tier); }
    @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        MachineTile tile = world.getTileEntity(pos) instanceof MachineTile machine ? machine : null;
        for (EnumFacing side : EnumFacing.values()) {
            TileEntity neighbor = world.getTileEntity(pos.offset(side));
            boolean connected = tile != null && tile.sides[side.ordinal()] != 3 && neighbor != null && neighbor.hasCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (neighbor instanceof CableTile cable && cable.sides[side.getOpposite().ordinal()] == 3) connected = false;
            state = state.withProperty(CONNECTIONS[side.ordinal()], connected);
        }
        return state;
    }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) { return new AxisAlignedBB(.3, .3, .3, .7, .7, .7); }
}
