package owmii.powah.block;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import owmii.powah.Content;

public final class ResourceBlock extends Block {
    private final String id;
    public ResourceBlock(String id) {
        super(Material.ROCK); this.id = id; setHardness(3); setResistance(8); setHarvestLevel("pickaxe", 1);
        if (id.endsWith("ice")) { slipperiness = .98f; setLightOpacity(3); }
    }
    @Override public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return id.contains("_ore") ? Content.ITEMS.get("uraninite_raw") : super.getItemDropped(state, random, fortune);
    }
    @Override public int quantityDropped(IBlockState state, int fortune, Random random) {
        if (!id.contains("_ore")) return 1;
        int base = id.endsWith("dense") ? 4 : id.endsWith("poor") ? 1 : 2;
        return base + (fortune > 0 ? random.nextInt(fortune + 1) : 0);
    }
}
