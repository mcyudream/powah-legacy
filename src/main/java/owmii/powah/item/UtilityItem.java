package owmii.powah.item;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.items.ItemHandlerHelper;
import owmii.powah.*;
import owmii.powah.block.*;

public final class UtilityItem extends Item {
    public final String id;
    public UtilityItem(String id) { this.id = id; if (id.equals("wrench") || id.equals("book") || isBindingCard()) setMaxStackSize(1); if (id.equals("charged_snowball")) setMaxStackSize(16); }
    public boolean isBindingCard() { return id.equals("binding_card") || id.equals("binding_card_dim"); }
    @Override public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (id.equals("book") && world.isRemote) Powah.proxy.openBook();
        if (isBindingCard() && !world.isRemote) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            if (!stack.getTagCompound().hasUniqueId("BoundPlayer")) {
                stack.getTagCompound().setUniqueId("BoundPlayer", player.getUniqueID()); stack.getTagCompound().setString("BoundName", player.getName());
            }
        }
        if (id.equals("wrench") && !world.isRemote) {
            if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
            int mode = (stack.getTagCompound().getInteger("Mode") + 1) % 3; stack.getTagCompound().setInteger("Mode", mode);
            player.sendStatusMessage(new TextComponentTranslation("powah.wrench.mode." + mode), true);
        }
        if (id.equals("charged_snowball")) {
            if (!world.isRemote) { ChargedSnowball snowball = new ChargedSnowball(world, player); snowball.shoot(player, player.rotationPitch, player.rotationYaw, 0, 1.5f, 1); world.spawnEntity(snowball); }
            if (!player.isCreative()) stack.shrink(1);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    @Override public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!id.equals("wrench") || !(world.getTileEntity(pos) instanceof MachineTile clicked)) return EnumActionResult.PASS;
        MachineTile tile = clicked.controller(); if (tile == null || !tile.canConfigure(player)) return EnumActionResult.FAIL;
        if (world.isRemote) return EnumActionResult.SUCCESS;
        ItemStack stack = player.getHeldItem(hand); if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        int mode = stack.getTagCompound().getInteger("Mode");
        if (player.isSneaking()) {
            if (clicked instanceof ReactorTile reactor && reactor.assembled) reactor.demolish();
            else { ItemHandlerHelper.giveItemToPlayer(player, ((MachineBlock) world.getBlockState(pos).getBlock()).storedDrop(clicked)); world.setBlockToAir(pos); }
        } else if (mode == 1) {
            tile.sides[side.ordinal()] = (byte) ((tile.sides[side.ordinal()] + 1) % 4); tile.markDirty(); tile.sync();
            player.sendStatusMessage(new TextComponentTranslation("powah.side." + tile.sides[side.ordinal()]), true);
        } else if (mode == 2) {
            if (tile instanceof OrbTile) { stack.getTagCompound().setLong("LinkOrb", pos.toLong()); stack.getTagCompound().setInteger("LinkDimension", player.dimension); }
            else if (tile.kind().equals("energizing_rod") && stack.getTagCompound().hasKey("LinkOrb") && player.dimension == stack.getTagCompound().getInteger("LinkDimension")) {
                BlockPos orb = BlockPos.fromLong(stack.getTagCompound().getLong("LinkOrb"));
                if (orb.distanceSq(pos) <= Config.energizingRange * Config.energizingRange && world.getTileEntity(orb) instanceof OrbTile) { tile.linkedOrb = orb; tile.markDirty(); tile.sync(); }
                else player.sendStatusMessage(new TextComponentTranslation("chat.powah.wrench.link.fail"), true);
            }
        } else world.setBlockState(pos, world.getBlockState(pos).withProperty(MachineBlock.FACING, EnumFacing.values()[(clicked.facing().ordinal() + 1) % 6]), 3);
        return EnumActionResult.SUCCESS;
    }
    @Override public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
        String result = null;
        if (target instanceof EntityEnderman || target instanceof EntityEndermite) {
            if (id.equals("binding_card")) result = "binding_card_dim";
            if (id.equals("photoelectric_pane")) result = "lens_of_ender";
        }
        if (id.equals("aerial_pearl") && target instanceof EntityZombie) result = "player_aerial_pearl";
        if (result == null) return false;
        if (!player.world.isRemote) {
            ItemStack output = Content.stack(result); if (stack.hasTagCompound()) output.setTagCompound(stack.getTagCompound().copy());
            if (!player.isCreative()) stack.shrink(1); ItemHandlerHelper.giveItemToPlayer(player, output); target.setDead();
        }
        return true;
    }
    @Override public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
        if (isBindingCard()) lines.add(stack.hasTagCompound() && stack.getTagCompound().hasUniqueId("BoundPlayer") ? stack.getTagCompound().getString("BoundName") : net.minecraft.client.resources.I18n.format("info.powah.click.to.bind"));
        if (id.equals("wrench")) { lines.add(net.minecraft.client.resources.I18n.format("powah.wrench.mode." + (stack.hasTagCompound() ? stack.getTagCompound().getInteger("Mode") : 0))); lines.add(net.minecraft.client.resources.I18n.format("powah.wrench.help")); }
    }
}
