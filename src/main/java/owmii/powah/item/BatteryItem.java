package owmii.powah.item;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.energy.CapabilityEnergy;
import owmii.powah.block.Tier;

public final class BatteryItem extends Item {
    public final Tier tier;
    public BatteryItem(Tier tier) { this.tier = tier; setMaxStackSize(1); }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) { return new ItemEnergy(stack); }
    @Override public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isRemote || !(entity instanceof EntityPlayer player) || !charging(stack)) return;
        int budget = tier.transfer("battery");
        for (int i = 0; i < player.inventory.getSizeInventory() && budget > 0; i++) {
            ItemStack target = player.inventory.getStackInSlot(i);
            if (target.isEmpty() || target.getItem() instanceof BatteryItem) continue;
            var storage = target.getCapability(CapabilityEnergy.ENERGY, null);
            if (storage == null || !storage.canReceive()) continue;
            int moved = storage.receiveEnergy((int) Math.min(budget, ItemEnergy.stored(stack)), false);
            ItemEnergy.set(stack, ItemEnergy.stored(stack) - moved); budget -= moved;
        }
        player.inventory.markDirty();
    }
    private boolean charging(ItemStack stack) { return stack.hasTagCompound() && stack.getTagCompound().getBoolean("Charging"); }
    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSneaking() && !world.isRemote) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound()); stack.getTagCompound().setBoolean("Charging", !charging(stack));
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    @Override public boolean hasEffect(ItemStack stack) { return charging(stack); }
    @Override public boolean showDurabilityBar(ItemStack stack) { return true; }
    @Override public double getDurabilityForDisplay(ItemStack stack) { return 1 - (double) ItemEnergy.stored(stack) / Math.max(1, ItemEnergy.capacity(stack)); }
    @Override public int getRGBDurabilityForDisplay(ItemStack stack) { return 0x3CCBE4; }
    @Override public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        lines.add(String.format("%,d / %,d FE", ItemEnergy.stored(stack), ItemEnergy.capacity(stack)));
        lines.add(net.minecraft.client.resources.I18n.format(charging(stack) ? "powah.battery.on" : "powah.battery.off"));
        lines.add(net.minecraft.client.resources.I18n.format("powah.battery.toggle"));
    }
}
