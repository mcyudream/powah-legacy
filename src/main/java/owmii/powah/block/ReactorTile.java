package owmii.powah.block;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.init.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import owmii.powah.Content;

public final class ReactorTile extends MachineTile {
    public BlockPos corePos;
    public boolean assembled, dismantling, autoMode, generate = true;
    public double fuel;
    public int carbon, redstone, coolant, coolantTemperature, temperature;
    public int carbonCapacity, redstoneCapacity, coolantCapacity;
    @Override public MachineTile controller() {
        if (corePos == null || corePos.equals(pos)) return this;
        if (world != null && world.isBlockLoaded(corePos) && world.getTileEntity(corePos) instanceof ReactorTile core && core.assembled && core.corePos == null) return core;
        return null;
    }
    @Nullable private EnumFacing portFace() {
        if (corePos == null) return EnumFacing.DOWN;
        int dx = pos.getX() - corePos.getX(), dy = pos.getY() - corePos.getY(), dz = pos.getZ() - corePos.getZ();
        if (dy == 3 && dx == 0 && dz == 0) return EnumFacing.UP;
        if (dy == 0 && Math.abs(dx) + Math.abs(dz) == 1) return EnumFacing.getFacingFromVector(dx, 0, dz);
        return null;
    }
    private boolean port(@Nullable EnumFacing side) {
        EnumFacing face = portFace();
        return face != null && (side == null || side == face);
    }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) {
        if (corePos != null) {
            MachineTile core = controller();
            return core != null && (capability != CapabilityEnergy.ENERGY || port(side)) && core.hasCapability(capability, null);
        }
        return (capability != CapabilityEnergy.ENERGY || port(side)) && super.hasCapability(capability, side);
    }
    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (corePos != null) {
            MachineTile core = controller();
            if (core == null) return null;
            if (capability == CapabilityEnergy.ENERGY) return port(side) ? (T) core.energyView(portFace()) : null;
            return core.getCapability(capability, side);
        }
        return hasCapability(capability, side) ? super.getCapability(capability, side) : null;
    }
    @Override public boolean validItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return switch (slot) {
            case 0 -> super.validItem(slot, stack);
            case 1 -> stack.getItem() == Content.ITEMS.get("uraninite");
            case 2 -> TileEntityFurnace.getItemBurnTime(stack) > 0;
            case 3 -> stack.getItem() == Items.REDSTONE || stack.getItem() == net.minecraft.item.Item.getItemFromBlock(Blocks.REDSTONE_BLOCK);
            case 4 -> coolantValue(stack)[0] > 0;
            default -> false;
        };
    }
    private int[] coolantValue(ItemStack stack) {
        var item = stack.getItem();
        if (item == Content.ITEMS.get("dry_ice")) return new int[]{712, -32};
        if (item == Content.ITEMS.get("blue_ice")) return new int[]{568, -17};
        if (item == net.minecraft.item.Item.getItemFromBlock(Blocks.PACKED_ICE)) return new int[]{192, -8};
        if (item == net.minecraft.item.Item.getItemFromBlock(Blocks.ICE)) return new int[]{48, -5};
        if (item == net.minecraft.item.Item.getItemFromBlock(Blocks.SNOW)) return new int[]{48, -3};
        if (item == Items.SNOWBALL) return new int[]{12, -3};
        return new int[]{0, 0};
    }
    @Override public void update() {
        if (world == null || world.isRemote || corePos != null || !assembled) return;
        // Never force-load missing chunks, and stop immediately when a loaded part is removed.
        for (BlockPos partPos : BlockPos.getAllInBox(pos.add(-1, 0, -1), pos.add(1, 3, 1))) {
            if (!world.isBlockLoaded(partPos)) { generating = 0; active = false; return; }
            if (partPos.equals(pos)) continue;
            if (!(world.getTileEntity(partPos) instanceof ReactorTile part) || !pos.equals(part.corePos)) { demolish(); return; }
        }
        generating = 0; boolean running = enabled() && (!autoMode || generate);
        if (running) {
            ItemStack uranium = inventory.getStackInSlot(1);
            if (validItem(1, uranium) && fuel <= 900) { fuel += 100; uranium.shrink(1); markDirty(); }
            boolean burn = fuel > 0 && energy().empty() > 0;
            if (carbon == 0 && validItem(2, inventory.getStackInSlot(2))) {
                ItemStack stack = inventory.getStackInSlot(2); carbon = carbonCapacity = TileEntityFurnace.getItemBurnTime(stack); stack.shrink(1);
            }
            if (redstone == 0 && validItem(3, inventory.getStackInSlot(3))) {
                ItemStack stack = inventory.getStackInSlot(3); redstone = redstoneCapacity = stack.getItem() == Items.REDSTONE ? 18 : 162; stack.shrink(1);
            }
            if (coolant == 0 && validItem(4, inventory.getStackInSlot(4))) {
                ItemStack stack = inventory.getStackInSlot(4); int[] value = coolantValue(stack); coolant = coolantCapacity = value[0]; coolantTemperature = value[1]; stack.shrink(1);
            }
            if (coolant == 0) coolantTemperature = 0;
            double target = (fuel > 0 ? 700 : 0) + (carbon > 0 ? 180 : 0) + (redstone > 0 ? 120 : 0);
            if (tank.getFluidAmount() > 0) target /= Math.abs(coolantTemperature) + 1;
            if (temperature < target) temperature++;
            else if (world.getTotalWorldTime() % (tank.getFluidAmount() == 0 ? 5 : coolant == 0 ? 3 : 1) == 0 && temperature > target) temperature--;
            if (burn) {
                double loss = temperature / 1000.0 * .98 / 2 * (redstone > 0 ? 1.4 : 1);
                long amount = (long) ((1 - loss) * (fuel / 1000.0) * production() / (carbon > 0 ? 1 : 1.2) / (redstone > 0 ? 1 : 1.4));
                produce(amount);
                if (generating > 0) {
                    fuel = Math.max(0, fuel - (1 + tier().ordinal() * .25) * loss);
                    if (carbon > 0) carbon--;
                    if (world.getTotalWorldTime() % 40 == 0) { if (redstone > 0) redstone--; if (coolant > 0 && tank.getFluidAmount() > 0) coolant--; }
                    markDirty();
                }
            }
        }
        if (autoMode) { if (energy().empty() == 0) generate = false; else if (energy().stored() <= energy().capacity() * .7) generate = true; }
        charge(inventory.getStackInSlot(0), tier().transfer(kind()), false);
        active = generating > 0;
        // Export through the bottom, four lower side centers, and top center.
        for (EnumFacing side : EnumFacing.values()) {
            BlockPos neighborPos = side == EnumFacing.UP ? pos.up(4) : side == EnumFacing.DOWN ? pos.down() : pos.offset(side, 2);
            if (!world.isBlockLoaded(neighborPos)) continue;
            TileEntity neighbor = world.getTileEntity(neighborPos);
            if (neighbor == null) continue;
            var target = neighbor.getCapability(CapabilityEnergy.ENERGY, side.getOpposite());
            if (target != null && target.canReceive() && outputAllowed(side)) energy().remove(target.receiveEnergy((int) Math.min(tier().transfer(kind()), energy().stored()), false), false);
        }
        if (world.getTotalWorldTime() % 10 == 0) sync();
    }
    public void demolish() {
        MachineTile controller = controller();
        if (!(controller instanceof ReactorTile core) || core.dismantling) return;
        core.dismantling = true;
        // Only the controller stores resources; each structure refunds exactly 36 parts.
        for (int i = 0; i < core.slotCount(); i++) {
            ItemStack stack = core.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) Block.spawnAsEntity(world, core.pos, stack.copy());
            core.inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        ItemStack drop = ((MachineBlock) Content.BLOCKS.get("reactor_" + core.tier().id())).storedDrop(core);
        NBTTagCompound data = drop.getTagCompound().getCompoundTag("PowahData"); data.setBoolean("Assembled", false); data.removeTag("Core");
        drop.setCount(36);
        for (BlockPos position : BlockPos.getAllInBox(core.pos.add(-1, 0, -1), core.pos.add(1, 3, 1))) {
            if (world.getTileEntity(position) instanceof ReactorTile part && (part == core || core.pos.equals(part.corePos))) {
                part.dismantling = true; part.assembled = false; world.setBlockToAir(position);
            }
        }
        Block.spawnAsEntity(world, core.pos, drop);
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt); if (corePos != null) nbt.setLong("Core", corePos.toLong());
        nbt.setBoolean("Assembled", assembled); nbt.setDouble("Fuel", fuel); nbt.setInteger("Carbon", carbon); nbt.setInteger("Redstone", redstone);
        nbt.setInteger("Coolant", coolant); nbt.setInteger("CoolantTemperature", coolantTemperature); nbt.setInteger("Temperature", temperature);
        nbt.setInteger("CarbonCapacity", carbonCapacity); nbt.setInteger("RedstoneCapacity", redstoneCapacity); nbt.setInteger("CoolantCapacity", coolantCapacity);
        nbt.setBoolean("AutoMode", autoMode); nbt.setBoolean("Generate", generate); return nbt;
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt); corePos = nbt.hasKey("Core") ? BlockPos.fromLong(nbt.getLong("Core")) : null;
        assembled = nbt.getBoolean("Assembled"); fuel = Math.max(0, Math.min(1000, nbt.getDouble("Fuel")));
        carbon = Math.max(0, nbt.getInteger("Carbon")); redstone = Math.max(0, nbt.getInteger("Redstone")); coolant = Math.max(0, nbt.getInteger("Coolant"));
        carbonCapacity = Math.max(carbon, nbt.getInteger("CarbonCapacity")); redstoneCapacity = Math.max(redstone, nbt.getInteger("RedstoneCapacity")); coolantCapacity = Math.max(coolant, nbt.getInteger("CoolantCapacity"));
        coolantTemperature = Math.max(-1000, Math.min(0, nbt.getInteger("CoolantTemperature"))); temperature = Math.max(0, Math.min(1000, nbt.getInteger("Temperature")));
        autoMode = nbt.getBoolean("AutoMode"); generate = !nbt.hasKey("Generate") || nbt.getBoolean("Generate");
    }
    @Override public AxisAlignedBB getRenderBoundingBox() { return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 4, 2)); }
}
