package owmii.powah.item;

import java.util.*;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import owmii.powah.block.*;

public final class MachineItem extends ItemBlock {
    public final MachineBlock machine;
    public MachineItem(MachineBlock block) {
        super(block); machine = block;
        setMaxStackSize(block.kind.equals("reactor") || block.kind.equals("energy_cable") ? 64 : 1);
    }
    @Override public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return ItemEnergy.capacity(stack) > 0 ? new ItemEnergy(stack) : null;
    }
    @Override public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos clicked, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!machine.kind.equals("reactor")) return super.onItemUse(player, world, clicked, hand, side, x, y, z);
        BlockPos base = world.getBlockState(clicked).getBlock().isReplaceable(world, clicked) ? clicked : clicked.offset(side);
        ItemStack held = player.getHeldItem(hand);
        int count = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (ItemStack.areItemsEqual(stack, held) && ItemStack.areItemStackTagsEqual(stack, held)) count += stack.getCount();
        }
        if (!player.isCreative() && count < 36) {
            if (!world.isRemote) player.sendStatusMessage(new TextComponentTranslation("chat.powah.not.enough.blocks", 36 - count), true);
            return EnumActionResult.FAIL;
        }
        for (BlockPos pos : BlockPos.getAllInBox(base.add(-1, 0, -1), base.add(1, 3, 1))) {
            if (pos.getY() < 0 || pos.getY() > 255 || !world.isBlockLoaded(pos) || !world.getBlockState(pos).getBlock().isReplaceable(world, pos)
                || !player.canPlayerEdit(pos, side, held) || !world.getWorldBorder().contains(pos)) return EnumActionResult.FAIL;
        }
        if (!world.checkNoEntityCollision(new AxisAlignedBB(base.add(-1, 0, -1), base.add(2, 4, 2)))) return EnumActionResult.FAIL;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack placement = held.copy();
        for (BlockPos pos : BlockPos.getAllInBox(base.add(-1, 0, -1), base.add(1, 3, 1))) {
            world.setBlockState(pos, machine.getDefaultState().withProperty(MachineBlock.ASSEMBLED, true), 3);
            ReactorTile tile = (ReactorTile) world.getTileEntity(pos);
            tile.assembled = true; tile.corePos = pos.equals(base) ? null : base; tile.owner = player.getUniqueID(); tile.markDirty();
        }
        machine.onBlockPlacedBy(world, base, world.getBlockState(base), player, placement);
        ReactorTile core = (ReactorTile) world.getTileEntity(base); core.assembled = true; core.corePos = null;
        for (BlockPos pos : BlockPos.getAllInBox(base.add(-1, 0, -1), base.add(1, 3, 1))) ((ReactorTile) world.getTileEntity(pos)).sync();
        if (!player.isCreative()) {
            int remaining = 36;
            for (int i = 0; i < player.inventory.getSizeInventory() && remaining > 0; i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (ItemStack.areItemsEqual(stack, placement) && ItemStack.areItemStackTagsEqual(stack, placement)) {
                    int taken = Math.min(remaining, stack.getCount()); stack.shrink(taken); remaining -= taken;
                }
            }
        }
        return EnumActionResult.SUCCESS;
    }
    @Override public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        long cap = ItemEnergy.capacity(stack);
        if (cap > 0) lines.add(String.format("%,d / %,d FE", ItemEnergy.stored(stack), cap));
        if (machine.kind.equals("reactor")) lines.add(net.minecraft.client.resources.I18n.format("powah.reactor.parts"));
        else lines.add(String.format("%,d FE/t", machine.tier.transfer(machine.kind)));
    }
}
