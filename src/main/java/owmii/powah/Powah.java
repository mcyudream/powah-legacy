package owmii.powah;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import owmii.powah.block.*;
import owmii.powah.network.Controls;

@Mod(modid = Powah.LEGACY_MOD_ID, name = "Powah Legacy", version = "5.1.0-cleanroom-alpha.2", acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:cleanroom@[0.4.0,);after:ponder-legacy")
public final class Powah {
    /** Loader identity; resource and saved registry namespaces remain unchanged. */
    public static final String LEGACY_MOD_ID = "powah-legacy";
    public static final String MOD_ID = "powah";
    @Mod.Instance(Powah.LEGACY_MOD_ID) public static Powah INSTANCE;
    @SidedProxy(clientSide = "owmii.powah.client.ClientProxy", serverSide = "owmii.powah.CommonProxy")
    public static CommonProxy proxy;
    public static ResourceLocation id(String path) { return new ResourceLocation(MOD_ID, path); }
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        Config.load(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(Content.class);
        GameRegistry.registerTileEntity(MachineTile.class, id("machine"));
        GameRegistry.registerTileEntity(CableTile.class, id("cable"));
        GameRegistry.registerTileEntity(OrbTile.class, id("energizing_orb"));
        GameRegistry.registerTileEntity(ReactorTile.class, id("reactor"));
        Controls.register();
        proxy.preInit();
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        GameRegistry.registerWorldGenerator(new owmii.powah.world.OreGeneration(), 0);
        owmii.powah.recipe.EnergizingRecipes.load();
        proxy.init();
    }
}
