package owmii.powah.inventory;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.items.SlotItemHandler;
import owmii.powah.block.*;

public final class MachineContainer extends Container {
    public final MachineTile tile;
    public final int machineSlots;
    public final MachineLayout layout;
    public final EnumFacing cableSide;
    public MachineContainer(InventoryPlayer player, MachineTile tile) {
        this(player, tile, EnumFacing.NORTH);
    }
    public MachineContainer(InventoryPlayer player, MachineTile tile, EnumFacing cableSide) {
        this.tile = tile; this.cableSide = cableSide; machineSlots = tile.slotCount();
        layout = MachineLayout.forKind(tile.kind());
        for (int i = 0; i < machineSlots; i++) {
            final int slot = i;
            addSlotToContainer(new SlotItemHandler(tile.inventory, i, layout.slotX(i), layout.slotY(i)) {
                @Override public boolean isItemValid(ItemStack stack) { return tile.validItem(slot, stack); }
                @Override public int getSlotStackLimit() { return tile.slotLimit(slot); }
                // Upstream Ender capacity expansion accepts shift-clicks through a hidden slot.
                @Override public boolean isEnabled() { return !(tile.isEnder() && slot == 0); }
            });
        }
        if (layout.playerY >= 0) {
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col + row * 9 + 9, 8 + col * 18, layout.playerY + row * 18));
            for (int col = 0; col < 9; col++) addSlotToContainer(new Slot(player, col, 8 + col * 18, layout.playerY + 58));
        }
    }
    @Override public boolean canInteractWith(EntityPlayer player) {
        return tile != null && !tile.isInvalid() && tile.getWorld().getTileEntity(tile.getPos()) == tile && tile.canConfigure(player) && player.getDistanceSq(tile.getPos()) <= 64;
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (index < 0 || index >= inventorySlots.size()) return ItemStack.EMPTY;
        Slot source = inventorySlots.get(index); if (!source.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = source.getStack(), original = stack.copy();
        if (index < machineSlots) { if (!mergeItemStack(stack, machineSlots, inventorySlots.size(), true)) return ItemStack.EMPTY; }
        else if (!mergeItemStack(stack, 0, machineSlots, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) source.putStack(ItemStack.EMPTY); else source.onSlotChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        source.onTake(player, stack); return original;
    }
}
