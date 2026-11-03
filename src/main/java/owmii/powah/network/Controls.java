package owmii.powah.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.*;
import net.minecraftforge.fml.relauncher.Side;
import owmii.powah.block.*;
import owmii.powah.inventory.MachineContainer;

public final class Controls implements IMessage {
    public static final int ALL_SIDES = 9;
    public static final int TANK = 10, TANK_DRAIN = 11;
    public static final int CHANNEL_BASE = 16;
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("powah_controls");
    private long position;
    private int action;
    public Controls() { }
    public Controls(BlockPos pos, int action) { this.position = pos.toLong(); this.action = action; }
    public static void register() { CHANNEL.registerMessage(Handler.class, Controls.class, 0, Side.SERVER); }
    @Override public void fromBytes(ByteBuf buffer) { position = buffer.readLong(); action = buffer.readInt(); }
    @Override public void toBytes(ByteBuf buffer) { buffer.writeLong(position); buffer.writeInt(action); }
    public static final class Handler implements IMessageHandler<Controls, IMessage> {
        @Override public IMessage onMessage(Controls message, MessageContext context) {
            var player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                BlockPos pos = BlockPos.fromLong(message.position);
                if (player.getDistanceSq(pos) > 64 || !player.world.isBlockLoaded(pos)) return;
                if (!(player.openContainer instanceof MachineContainer container) || !container.tile.getPos().equals(pos) || !container.canInteractWith(player)) return;
                MachineTile tile = container.tile; int action = message.action;
                if (action >= 0 && action < 6) tile.sides[action] = (byte) ((tile.sides[action] + 1) % 4);
                else if (action == 6) tile.redstoneMode = (tile.redstoneMode + 1) % 3;
                else if (action == 7 && tile.isEnder()) tile.channel = (tile.channel + 1) % tile.tier().channels;
                else if (action == 8 && tile instanceof ReactorTile reactor) reactor.autoMode = !reactor.autoMode;
                else if (action == ALL_SIDES) {
                    byte mode = (byte) ((tile.sides[1] + 1) % 4);
                    java.util.Arrays.fill(tile.sides, mode);
                }
                else if ((action == TANK || action == TANK_DRAIN)
                    && tile.hasCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null)) {
                    var carried = player.inventory.getItemStack();
                    if (carried.isEmpty()) return;
                    var result = action == TANK_DRAIN
                        ? net.minecraftforge.fluids.FluidUtil.tryFillContainer(carried, tile.tank, tile.tank.getCapacity(), player, true)
                        : net.minecraftforge.fluids.FluidUtil.tryEmptyContainer(carried, tile.tank, tile.tank.getCapacity(), player, true);
                    if (!result.isSuccess() && action == TANK && net.minecraftforge.fluids.FluidUtil.getFluidContained(carried) == null)
                        result = net.minecraftforge.fluids.FluidUtil.tryFillContainer(carried, tile.tank, tile.tank.getCapacity(), player, true);
                    if (!result.isSuccess()) return;
                    if (carried.getCount() == 1) player.inventory.setItemStack(result.getResult());
                    else {
                        carried.shrink(1);
                        net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(player, result.getResult());
                    }
                    player.connection.sendPacket(new net.minecraft.network.play.server.SPacketSetSlot(-1, -1, player.inventory.getItemStack()));
                    container.detectAndSendChanges();
                }
                else if (tile.isEnder() && action >= CHANNEL_BASE && action < CHANNEL_BASE + tile.tier().channels) tile.channel = action - CHANNEL_BASE;
                else return;
                tile.markDirty(); tile.sync();
            });
            return null;
        }
    }
}
