package owmii.powah;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import owmii.powah.block.MachineTile;
import owmii.powah.inventory.MachineContainer;

public class CommonProxy implements IGuiHandler {
    public void preInit() { }
    public void init() { }
    public void openBook() { }
    @Override public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id < 0 || id > 6) return null;
        return world.getTileEntity(new BlockPos(x, y, z)) instanceof MachineTile tile && tile.controller() != null
            ? new MachineContainer(player.inventory, tile.controller(), id == 0 ? EnumFacing.NORTH : EnumFacing.byIndex(id - 1)) : null;
    }
    @Override public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) { return null; }
}
