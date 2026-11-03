package owmii.powah.item;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.energy.*;
import owmii.powah.block.MachineBlock;
import owmii.powah.energy.EnergyStore;

public final class ItemEnergy implements ICapabilityProvider, IEnergyStorage {
    private final ItemStack stack;
    public ItemEnergy(ItemStack stack) { this.stack = stack; }
    public static boolean extendable(ItemStack stack) {
        return stack.getItem() instanceof BatteryItem || stack.getItem() instanceof MachineItem item && item.machine.kind.equals("energy_cell") && item.machine.tier != owmii.powah.block.Tier.CREATIVE;
    }
    public static long capacity(ItemStack stack) {
        if (stack.getItem() instanceof BatteryItem item) return item.tier.capacity("battery");
        if (stack.getItem() instanceof MachineItem item && !item.machine.kind.equals("reactor")) return item.machine.tier.capacity(item.machine.kind);
        return 0;
    }
    public static long stored(ItemStack stack) {
        return stack.hasTagCompound() ? Math.max(0, Math.min(capacity(stack), stack.getTagCompound().getCompoundTag("PowahData").getLong("Energy"))) : 0;
    }
    public static void set(ItemStack stack, long energy) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound data = stack.getTagCompound().getCompoundTag("PowahData");
        data.setLong("Energy", Math.max(0, Math.min(capacity(stack), energy))); stack.getTagCompound().setTag("PowahData", data);
    }
    private int rate() {
        if (stack.getItem() instanceof BatteryItem item) return item.tier.transfer("battery");
        if (stack.getItem() instanceof MachineItem item) return item.machine.tier.transfer(item.machine.kind);
        return 0;
    }
    @Override public int receiveEnergy(int amount, boolean simulate) {
        int moved = EnergyStore.bounded(Math.min(Math.max(0, amount), Math.min(rate(), capacity(stack) - stored(stack))));
        if (!simulate && moved > 0) set(stack, stored(stack) + moved); return moved;
    }
    @Override public int extractEnergy(int amount, boolean simulate) {
        int moved = EnergyStore.bounded(Math.min(Math.max(0, amount), Math.min(rate(), stored(stack))));
        if (!simulate && moved > 0) set(stack, stored(stack) - moved); return moved;
    }
    @Override public int getEnergyStored() { return EnergyStore.bounded(stored(stack)); }
    @Override public int getMaxEnergyStored() { return EnergyStore.bounded(capacity(stack)); }
    @Override public boolean canExtract() { return true; }
    @Override public boolean canReceive() { return true; }
    @Override public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side) { return capability == CapabilityEnergy.ENERGY; }
    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) { return capability == CapabilityEnergy.ENERGY ? (T) this : null; }
}
