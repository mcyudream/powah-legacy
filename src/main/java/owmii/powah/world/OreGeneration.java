package owmii.powah.world;

import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;
import owmii.powah.*;

public final class OreGeneration implements IWorldGenerator {
    @Override public void generate(Random random, int x, int z, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!Config.worldgen || world.provider.getDimension() != 0) return;
        veins(random, world, x, z, "uraninite_ore_poor", 8, 5, 4, 64);
        veins(random, world, x, z, "uraninite_ore", 6, 4, 4, 24);
        veins(random, world, x, z, "uraninite_ore_dense", 3, 3, 4, 12);
        if (world.getBiome(new BlockPos(x * 16 + 8, 64, z * 16 + 8)).getDefaultTemperature() < .2f) veins(random, world, x, z, "dry_ice", 4, 17, 4, 48);
    }
    private void veins(Random random, World world, int x, int z, String id, int count, int size, int min, int max) {
        WorldGenMinable feature = new WorldGenMinable(Content.BLOCKS.get(id).getDefaultState(), size);
        for (int i = 0; i < count; i++) feature.generate(world, random, new BlockPos(x * 16 + random.nextInt(16), min + random.nextInt(max - min), z * 16 + random.nextInt(16)));
    }
}
