package owmii.powah;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class Config {
    public static int energizingRange = 4, fuelEnergy = 30;
    public static double energizingRatio = 1, productionRatio = 1;
    public static boolean worldgen = true;
    public static void load(File file) {
        Configuration config = new Configuration(file);
        config.load();
        energizingRange = config.getInt("energizingRange", "general", 4, 1, 32, "Maximum rod-to-orb distance in blocks.");
        fuelEnergy = config.getInt("energyPerFuelTick", "general", 30, 1, 1000000, "Furnator FE per vanilla fuel tick.");
        energizingRatio = config.getFloat("energizingEnergyRatio", "general", 1, .001f, 1000, "Multiplier for energizing recipe costs.");
        productionRatio = config.getFloat("productionRatio", "general", 1, .001f, 1000, "Generator production multiplier.");
        worldgen = config.getBoolean("worldGeneration", "general", true, "Generate uraninite and dry ice in new overworld chunks.");
        if (config.hasChanged()) config.save();
    }
}
