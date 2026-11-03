package owmii.powah.block;

import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.*;
import net.minecraft.block.state.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.*;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.items.ItemHandlerHelper;
import owmii.powah.*;

public class MachineBlock extends Block {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool ASSEMBLED = PropertyBool.create("assembled");
    public final String kind;
    public final Tier tier;
    public MachineBlock(String kind, Tier tier) {
        super(Material.IRON); this.kind = kind; this.tier = tier;
        setHardness(2); setResistance(20); setHarvestLevel("pickaxe", 0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(ASSEMBLED, false));
    }
    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, FACING, ASSEMBLED); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState().withProperty(FACING, EnumFacing.byIndex(Math.min(5, meta & 7))).withProperty(ASSEMBLED, (meta & 8) != 0); }
    @Override public int getMetaFromState(IBlockState state) { return state.getValue(FACING).getIndex() | (state.getValue(ASSEMBLED) ? 8 : 0); }
    @Override public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float x, float y, float z, int meta, EntityLivingBase placer) {
        EnumFacing facing = switch (kind) {
            case "energizing_rod", "energizing_orb", "ender_gate", "energy_hopper" -> side.getOpposite();
            default -> placer.getHorizontalFacing().getOpposite();
        };
        return getDefaultState().withProperty(FACING, facing);
    }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) {
        MachineTile tile = switch (kind) { case "energizing_orb" -> new OrbTile(); case "reactor" -> new ReactorTile(); default -> new MachineTile(); };
        return tile.configure(kind, tier);
    }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return false; }
    @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT; }
    @Override public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess access, BlockPos pos) {
        return switch (kind) {
            case "solar_panel" -> new AxisAlignedBB(0, 0, 0, 1, .22, 1);
            case "energizing_rod" -> new AxisAlignedBB(.32, .05, .32, .68, .95, .68);
            case "ender_gate" -> new AxisAlignedBB(.2, .1, .2, .8, .9, .8);
            case "energizing_orb" -> new AxisAlignedBB(.15, 0, .15, .85, .8, .85);
            default -> FULL_BLOCK_AABB;
        };
    }
    @Override public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if (!(world.getTileEntity(pos) instanceof MachineTile tile)) return;
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey("PowahData")) {
            NBTTagCompound data = stack.getTagCompound().getCompoundTag("PowahData").copy();
            data.setInteger("x", pos.getX()); data.setInteger("y", pos.getY()); data.setInteger("z", pos.getZ());
            tile.readFromNBT(data);
        }
        if (tile.owner == null) tile.owner = placer.getUniqueID();
        tile.markDirty();
    }
    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float x, float y, float z) {
        if (!(world.getTileEntity(pos) instanceof MachineTile clicked)) return false;
        MachineTile tile = clicked.controller(); if (tile == null) return false;
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == Content.ITEMS.get("wrench")) return false;
        if (!tile.canConfigure(player)) return true;
        if (FluidUtil.getFluidHandler(held) != null && tile.hasCapability(net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side)) {
            if (!world.isRemote) FluidUtil.interactWithFluidHandler(player, hand, tile.tank);
            return true;
        }
        if (kind.equals("solar_panel") && held.getItem() == Content.ITEMS.get("lens_of_ender") && !tile.lens) {
            if (!world.isRemote) { tile.lens = true; if (!player.isCreative()) held.shrink(1); tile.markDirty(); tile.sync(); }
            return true;
        }
        if (tile instanceof OrbTile orb) {
            if (!world.isRemote) orb.interact(player, hand);
            return true;
        }
        if (!world.isRemote) player.openGui(Powah.INSTANCE, kind.equals("energy_cable") ? side.ordinal() + 1 : 0, world, tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
        return true;
    }
    public ItemStack storedDrop(MachineTile tile) {
        ItemStack drop = new ItemStack(this);
        if (tile != null && !kind.equals("energy_cable")) {
            NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
            data.removeTag("Inventory"); data.removeTag("id"); data.removeTag("x"); data.removeTag("y"); data.removeTag("z");
            NBTTagCompound tag = new NBTTagCompound(); tag.setTag("PowahData", data); drop.setTagCompound(tag);
        }
        return drop;
    }
    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess access, BlockPos pos, IBlockState state, int fortune) {
        MachineTile tile = access.getTileEntity(pos) instanceof MachineTile machine ? machine : null;
        if (tile instanceof ReactorTile reactor && reactor.assembled) return;
        drops.add(storedDrop(tile));
    }
    @Override public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        return willHarvest || super.removedByPlayer(state, world, pos, player, false);
    }
    @Override public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity tile, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, tile, tool); world.setBlockToAir(pos);
    }
    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (world.getTileEntity(pos) instanceof MachineTile tile && !world.isRemote) {
            if (tile instanceof ReactorTile reactor && reactor.assembled) { reactor.demolish(); }
            else {
                for (int slot = 0; slot < tile.slotCount(); slot++) {
                    ItemStack stack = tile.inventory.getStackInSlot(slot);
                    if (!stack.isEmpty()) spawnAsEntity(world, pos, stack.copy());
                }
                // The lens and fluids are stored on the block item.
            }
        }
        super.breakBlock(world, pos, state);
    }
    @Override public ItemStack getPickBlock(IBlockState state, RayTraceResult hit, World world, BlockPos pos, EntityPlayer player) { return new ItemStack(this); }
    @Override public boolean hasComparatorInputOverride(IBlockState state) { return true; }
    @Override public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        if (!(world.getTileEntity(pos) instanceof MachineTile tile)) return 0;
        MachineTile controller = tile.controller(); if (controller == null) return 0;
        return (int) (15.0 * controller.energy().stored() / Math.max(1, controller.energy().capacity()));
    }
}
