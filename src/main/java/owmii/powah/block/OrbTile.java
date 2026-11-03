package owmii.powah.block;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.*;
import owmii.powah.recipe.EnergizingRecipes;

public final class OrbTile extends MachineTile {
    public long progress, required;
    public String recipeId = "";
    private EnergizingRecipes.Recipe recipe;
    private final IItemHandler automation = new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return inventory.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    @SuppressWarnings("unchecked")
    @Override public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) automation;
        return super.getCapability(capability, side);
    }
    @Override public int slotLimit(int slot) { return slot == 0 ? 64 : 1; }
    @Override public boolean validItem(int slot, ItemStack stack) {
        return slot > 0 && slot < 7 && !stack.isEmpty() && inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(slot).isEmpty();
    }
    @Override protected void inventoryChanged(int slot) { progress = 0; recipe = null; required = 0; recipeId = ""; }
    private void refresh() {
        List<ItemStack> input = new ArrayList<>();
        if (inventory.getStackInSlot(0).isEmpty()) for (int i = 1; i < 7; i++) if (!inventory.getStackInSlot(i).isEmpty()) input.add(inventory.getStackInSlot(i));
        EnergizingRecipes.Recipe found = EnergizingRecipes.find(input);
        if (found == null || !found.id().equals(recipeId)) progress = 0;
        recipe = found; recipeId = found == null ? "" : found.id(); required = found == null ? 0 : found.energy();
    }
    public long fill(long amount) {
        refresh(); if (recipe == null || !enabled()) return 0;
        long filled = Math.max(0, Math.min(amount, required - progress)); progress += filled;
        if (progress >= required) {
            ItemStack result = recipe.result().copy(); reading = true;
            for (int i = 1; i < 7; i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
            inventory.setStackInSlot(0, result); reading = false; progress = 0; required = 0; recipe = null; recipeId = "";
        }
        if (filled > 0) { markDirty(); if (world != null && world.getTotalWorldTime() % 5 == 0) sync(); }
        return filled;
    }
    @Override public void update() {
        if (world == null || world.isRemote) return;
        refresh(); if (world.getTotalWorldTime() % 10 == 0) sync();
    }
    public void interact(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) {
            for (int i = 0; i < 7; i++) {
                ItemStack stack = inventory.extractItem(i, 64, false);
                if (!stack.isEmpty()) { ItemHandlerHelper.giveItemToPlayer(player, stack); if (!player.isSneaking()) break; }
            }
        } else for (int i = 1; i < 7; i++) {
            if (validItem(i, held)) {
                ItemStack one = held.copy(); one.setCount(1); inventory.setStackInSlot(i, one);
                if (!player.isCreative()) held.shrink(1); break;
            }
        }
        sync();
    }
    @Override public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt); nbt.setLong("Progress", progress); nbt.setString("Recipe", recipeId); nbt.setLong("Required", required); return nbt;
    }
    @Override public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt); progress = Math.max(0, nbt.getLong("Progress")); recipeId = nbt.getString("Recipe"); required = Math.max(0, nbt.getLong("Required")); recipe = null;
    }
}
