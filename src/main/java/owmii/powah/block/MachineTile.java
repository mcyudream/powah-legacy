package owmii.powah.block;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.*;
import owmii.powah.*;
import owmii.powah.energy.*;
import owmii.powah.item.*;

public class MachineTile extends TileEntity implements ITickable {
    private String savedKind = "energy_cell";
    private Tier savedTier = Tier.STARTER;
    protected EnergyStore local;
    public UUID owner;
    public int channel, redstoneMode;
    public byte[] sides = new byte[6]; // 0 both, 1 input, 2 output, 3 disabled
    public long fuelBuffer, fuelCapacity, generating;
    public boolean lens, active;
    public BlockPos linkedOrb;
    protected boolean reading;
    public MachineTile configure(String kind, Tier tier) { savedKind = kind; savedTier = tier; return this; }
    public final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override public int getSlots() { return slotCount(); }
        @Override public int getSlotLimit(int slot) { return MachineTile.this.slotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return validItem(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return validItem(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
        }
        @Override protected void onContentsChanged(int slot) { if (!reading) { inventoryChanged(slot); markDirty(); } }
    };
    public final FluidTank tank = new FluidTank(4000) {
        @Override public boolean canFillFluidType(FluidStack fluid) {
            return fluid != null && fluid.getFluid() == (kind().equals("magmator") ? FluidRegistry.LAVA : FluidRegistry.WATER);
        }
        @Override protected void onContentsChanged() { markDirty(); }
    };
    public String kind() {
        if (world != null && world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() instanceof MachineBlock block) savedKind = block.kind;
        return savedKind;
    }
    public Tier tier() {
        if (world != null && world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() instanceof MachineBlock block) savedTier = block.tier;
        return savedTier;
    }
    public MachineTile controller() { return this; }
    public boolean isEnder() { return kind().equals("ender_cell") || kind().equals("ender_gate"); }
    public EnergyStore energy() {
        if (isEnder() && owner != null && world != null && !world.isRemote) return EnderNetwork.get(world).channel(owner, channel);
        if (local == null) local = new EnergyStore(tier().capacity(kind()), tier().transfer(kind()), this::markDirty);
        return local;
    }
    public int slotCount() {
        return switch (kind()) {
            case "energy_cable", "energizing_rod", "energy_hopper", "solar_panel" -> 0;
            case "energizing_orb", "energy_discharger" -> 7;
            case "reactor" -> 5;
            case "ender_cell", "ender_gate" -> 3;
            case "furnator", "energy_cell" -> 2;
            default -> 1;
        };
    }
    public int slotLimit(int slot) {
        if (kind().equals("furnator") && slot == 1 || kind().equals("reactor") && slot != 0) return 64;
        return 1;
    }
    protected void inventoryChanged(int slot) { }
    public boolean validItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= slotCount() || stack.isEmpty()) return false;
        String kind = kind();
        if (kind.equals("furnator") && slot == 1) return TileEntityFurnace.getItemBurnTime(stack) > 0;
        if (kind.equals("player_transmitter")) return stack.getItem() instanceof UtilityItem item && item.isBindingCard() && stack.hasTagCompound() && stack.getTagCompound().hasUniqueId("BoundPlayer");
        if (isEnder() && slot == 0) return kind.equals("ender_cell") && ItemEnergy.capacity(stack) > 0 && ItemEnergy.extendable(stack);
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        return storage != null && (kind.equals("energy_discharger") ? storage.canExtract() : storage.canReceive());
    }
    public boolean enabled() {
        if (world == null) return true;
        return redstoneMode == 0 || world.isBlockPowered(pos) == (redstoneMode == 1);
    }
    public boolean canConfigure(EntityPlayer player) {
        return !isEnder() || owner == null || owner.equals(player.getUniqueID()) || player.canUseCommand(2, "powah");
    }
    public EnumFacing facing() { return world == null ? EnumFacing.DOWN : world.getBlockState(pos).getValue(MachineBlock.FACING); }
    public boolean energySide(@Nullable EnumFacing side) {
        if (side == null) return true;
        return switch (kind()) {
            case "energizing_orb" -> false;
            case "solar_panel" -> side == EnumFacing.DOWN;
            case "energizing_rod", "ender_gate" -> side == facing();
            default -> true;
        };
    }
    public boolean inputAllowed(@Nullable EnumFacing side) {
        if (!enabled() || !energySide(side) || side != null && (sides[side.ordinal()] == 2 || sides[side.ordinal()] == 3)) return false;
        return !Set.of("furnator", "magmator", "thermo_generator", "solar_panel", "reactor", "energy_discharger").contains(kind());
    }
    public boolean outputAllowed(@Nullable EnumFacing side) {
        if (!enabled() || !energySide(side) || side != null && (sides[side.ordinal()] == 1 || sides[side.ordinal()] == 3)) return false;
        return !Set.of("energizing_orb", "energizing_rod", "energy_hopper", "player_transmitter").contains(kind());
    }
    public IEnergyStorage energyView(@Nullable EnumFacing side) {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int amount, boolean simulate) {
                return canReceive() ? EnergyStore.bounded(energy().insert(Math.min(tier().transfer(kind()), amount), simulate)) : 0;
            }
            @Override public int extractEnergy(int amount, boolean simulate) {
                return canExtract() ? EnergyStore.bounded(energy().remove(Math.min(tier().transfer(kind()), amount), simulate)) : 0;
            }
            @Override public int getEnergyStored() { return tier() == Tier.CREATIVE ? getMaxEnergyStored() : energy().getEnergyStored(); }
            @Override public int getMaxEnergyStored() { return energy().getMaxEnergyStored(); }
            @Override public boolean canExtract() { return outputAllowed(side); }
            @Override public boolean canReceive() { return inputAllowed(side); }
        };
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        if (capability == CapabilityEnergy.ENERGY) return !kind().equals("energizing_orb") && energySide(side);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return slotCount() > 0;
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) return Set.of("magmator", "thermo_generator", "reactor").contains(kind());
        return super.hasCapability(capability, side);
    }
    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (capability == CapabilityEnergy.ENERGY && hasCapability(capability, side)) return (T) energyView(side);
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && slotCount() > 0) return (T) inventory;
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && hasCapability(capability, side)) return (T) tank;
        return super.getCapability(capability, side);
    }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        generating = 0;
        if (tier() == Tier.CREATIVE) energy().set(energy().capacity());
        if (enabled()) {
            switch (kind()) {
                case "furnator" -> furnator();
                case "magmator" -> magmator();
                case "solar_panel" -> solar();
                case "thermo_generator" -> thermo();
                case "energizing_rod" -> rod();
                case "energy_discharger" -> discharge();
                case "energy_hopper" -> hopper();
                case "player_transmitter" -> transmitter();
                case "ender_cell" -> extendEnder();
            }
            if (!kind().equals("energy_discharger") && !kind().equals("player_transmitter") && !kind().equals("energizing_orb")) {
                int start = isEnder() ? 1 : 0, end = isEnder() ? 3 : kind().equals("energy_cell") ? 2 : Math.min(1, slotCount());
                long budget = tier().transfer(kind());
                for (int slot = start; slot < end; slot++) budget -= charge(inventory.getStackInSlot(slot), budget, false);
            }
        }
        pushEnergy();
        active = generating > 0;
        if (world.getTotalWorldTime() % 10 == 0) sync();
    }
    protected int production() { return EnergyStore.bounded(Math.round(tier().production(kind()) * Config.productionRatio)); }
    protected void produce(long amount) { generating = energy().insert(amount, false); }
    private void furnator() {
        if (energy().empty() == 0) return;
        if (fuelBuffer == 0) {
            ItemStack fuel = inventory.getStackInSlot(1);
            int burn = TileEntityFurnace.getItemBurnTime(fuel);
            if (burn > 0) {
                ItemStack container = fuel.getItem().getContainerItem(fuel);
                fuel.shrink(1); if (fuel.isEmpty()) inventory.setStackInSlot(1, container);
                fuelBuffer = fuelCapacity = (long) burn * Config.fuelEnergy; markDirty();
            }
        }
        produce(Math.min(production(), fuelBuffer)); fuelBuffer -= generating;
    }
    private void magmator() {
        if (energy().empty() == 0) return;
        if (fuelBuffer == 0 && tank.getFluidAmount() > 0) {
            int amount = Math.min(100, tank.getFluidAmount()); tank.drain(amount, true); fuelBuffer = fuelCapacity = amount * 100L;
        }
        produce(Math.min(production(), fuelBuffer)); fuelBuffer -= generating;
    }
    private void solar() {
        if (world.provider.hasSkyLight() && world.isDaytime() && !world.isRaining() && (lens || world.canSeeSky(pos.up()))) produce(production());
    }
    private void thermo() {
        if (tank.getFluidAmount() == 0) return;
        var block = world.getBlockState(pos.down()).getBlock();
        int heat = block == Blocks.LAVA || block == Blocks.FLOWING_LAVA ? 1000 : block == Blocks.MAGMA ? 800 : block == Content.BLOCKS.get("blazing_crystal_block") ? 2800 : 0;
        if (heat > 0) {
            produce((long) production() * heat / 1000);
            if (generating > 0 && world.getTotalWorldTime() % 40 == 0) tank.drain(1, true);
        }
    }
    private void rod() {
        OrbTile orb = null;
        if (linkedOrb != null && world.isBlockLoaded(linkedOrb) && pos.distanceSq(linkedOrb) <= Config.energizingRange * Config.energizingRange
            && world.getTileEntity(linkedOrb) instanceof OrbTile tile) orb = tile;
        if (orb == null && world.getTotalWorldTime() % 20 == 0) {
            double nearest = Double.MAX_VALUE;
            for (TileEntity tile : world.loadedTileEntityList) if (tile instanceof OrbTile candidate) {
                double distance = pos.distanceSq(tile.getPos());
                if (distance <= Config.energizingRange * Config.energizingRange && distance < nearest) { orb = candidate; nearest = distance; }
            }
            if (orb != null) { linkedOrb = orb.pos; markDirty(); }
        }
        if (orb != null) {
            long moved = orb.fill(Math.min(energy().stored(), tier().transfer(kind())));
            energy().remove(moved, false); generating = moved;
        }
    }
    public long charge(ItemStack stack, long limit, boolean discharge) {
        if (stack.isEmpty() || limit <= 0) return 0;
        IEnergyStorage target = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (target == null) return 0;
        int amount = EnergyStore.bounded(Math.min(limit, discharge ? energy().empty() : energy().stored()));
        if (amount <= 0) return 0;
        int moved = discharge ? target.extractEnergy(amount, false) : target.receiveEnergy(amount, false);
        if (discharge) energy().insert(moved, false); else energy().remove(moved, false);
        if (moved > 0) markDirty();
        return moved;
    }
    private void discharge() {
        long budget = tier().transfer(kind());
        for (int i = 0; i < slotCount(); i++) budget -= charge(inventory.getStackInSlot(i), budget, true);
    }
    private void hopper() {
        BlockPos targetPos = pos.offset(facing()); if (!world.isBlockLoaded(targetPos)) return;
        TileEntity target = world.getTileEntity(targetPos);
        IItemHandler handler = target == null ? null : target.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing().getOpposite());
        if (!(handler instanceof IItemHandlerModifiable writable)) return;
        long budget = 500L * tier().scale;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i).copy();
            long moved = charge(stack, budget, false); budget -= moved;
            if (moved > 0) { writable.setStackInSlot(i, stack); target.markDirty(); }
        }
    }
    private void transmitter() {
        ItemStack card = inventory.getStackInSlot(0);
        if (!card.hasTagCompound() || !card.getTagCompound().hasUniqueId("BoundPlayer")) return;
        var player = world.getMinecraftServer().getPlayerList().getPlayerByUUID(card.getTagCompound().getUniqueId("BoundPlayer"));
        if (player == null || card.getItem() != Content.ITEMS.get("binding_card_dim") && player.dimension != world.provider.getDimension()) return;
        long budget = 500L * tier().scale;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) budget -= charge(player.inventory.getStackInSlot(i), budget, false);
        player.inventory.markDirty();
    }
    private void extendEnder() {
        ItemStack extender = inventory.getStackInSlot(0); long capacity = ItemEnergy.capacity(extender);
        if (owner == null || capacity <= 0 || !ItemEnergy.extendable(extender) || capacity > EnergyStore.MAX - energy().capacity()) return;
        EnergyStore store = energy(); store.capacity(store.capacity() + capacity); store.insert(ItemEnergy.stored(extender), false);
        extender.shrink(1); markDirty();
    }
    protected void pushEnergy() {
        if (energy().stored() == 0) return;
        long remaining = tier().transfer(kind());
        int offset = (int) (world.getTotalWorldTime() % 6);
        for (int i = 0; i < 6 && remaining > 0; i++) {
            EnumFacing side = EnumFacing.values()[(i + offset) % 6];
            if (!outputAllowed(side)) continue;
            BlockPos neighborPos = pos.offset(side); if (!world.isBlockLoaded(neighborPos)) continue;
            TileEntity neighbor = world.getTileEntity(neighborPos);
            if (neighbor == null || neighbor instanceof MachineTile machine && machine.isEnder() && isEnder() && Objects.equals(owner, machine.owner) && channel == machine.channel) continue;
            IEnergyStorage target = neighbor.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (target == null || !target.canReceive()) continue;
            if (neighbor instanceof MachineTile machine && machine.kind().equals("energy_cell") && kind().equals("energy_cell")
                && (double) energy().stored() / Math.max(1, energy().capacity()) <= (double) machine.energy().stored() / Math.max(1, machine.energy().capacity())) continue;
            int moved = target.receiveEnergy(EnergyStore.bounded(Math.min(energy().stored(), remaining)), false);
            energy().remove(moved, false); remaining -= moved;
        }
    }
    public void sync() {
        if (world == null) return;
        IBlockState state = world.getBlockState(pos); world.notifyBlockUpdate(pos, state, state, 3);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt); nbt.setString("Kind", kind()); nbt.setString("Tier", tier().id());
        nbt.setLong("Energy", energy().stored()); nbt.setLong("Capacity", energy().capacity());
        nbt.setTag("Inventory", inventory.serializeNBT()); nbt.setTag("Tank", tank.writeToNBT(new NBTTagCompound()));
        nbt.setLong("FuelBuffer", fuelBuffer); nbt.setLong("FuelCapacity", fuelCapacity); nbt.setLong("Generating", generating);
        nbt.setInteger("Channel", channel); nbt.setInteger("RedstoneMode", redstoneMode); nbt.setByteArray("Sides", sides);
        nbt.setBoolean("Lens", lens); nbt.setBoolean("Active", active);
        if (owner != null) nbt.setUniqueId("Owner", owner);
        if (linkedOrb != null) nbt.setLong("Orb", linkedOrb.toLong());
        return nbt;
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt); savedKind = nbt.getString("Kind"); if (savedKind.isEmpty()) savedKind = "energy_cell";
        try { savedTier = Tier.of(nbt.getString("Tier")); } catch (IllegalArgumentException error) { savedTier = Tier.STARTER; }
        owner = nbt.hasUniqueId("Owner") ? nbt.getUniqueId("Owner") : null;
        channel = Math.max(0, Math.min(tier().channels - 1, nbt.getInteger("Channel")));
        local = new EnergyStore(isEnder() ? nbt.getLong("Capacity") : tier().capacity(kind()), tier().transfer(kind()), this::markDirty);
        local.set(nbt.getLong("Energy"));
        reading = true;
        if (nbt.hasKey("Inventory")) inventory.deserializeNBT(nbt.getCompoundTag("Inventory"));
        tank.readFromNBT(nbt.getCompoundTag("Tank")); reading = false;
        fuelBuffer = Math.max(0, nbt.getLong("FuelBuffer")); generating = nbt.getLong("Generating");
        fuelCapacity = Math.max(fuelBuffer, nbt.getLong("FuelCapacity"));
        redstoneMode = Math.max(0, Math.min(2, nbt.getInteger("RedstoneMode")));
        sides = nbt.getByteArray("Sides"); if (sides.length != 6) sides = new byte[6];
        for (int i = 0; i < 6; i++) sides[i] = (byte) Math.max(0, Math.min(3, sides[i]));
        lens = nbt.getBoolean("Lens"); active = nbt.getBoolean("Active"); linkedOrb = nbt.hasKey("Orb") ? BlockPos.fromLong(nbt.getLong("Orb")) : null;
    }
    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }
    @Override public void onDataPacket(NetworkManager manager, SPacketUpdateTileEntity packet) { readFromNBT(packet.getNbtCompound()); }
    @Override public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos, IBlockState oldState, IBlockState newState) { return oldState.getBlock() != newState.getBlock(); }
    @Override public AxisAlignedBB getRenderBoundingBox() { return new AxisAlignedBB(pos).grow(.5); }
}
