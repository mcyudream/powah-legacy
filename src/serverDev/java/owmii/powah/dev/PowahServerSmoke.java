package owmii.powah.dev;

import com.google.gson.*;
import java.nio.file.Files;
import java.util.UUID;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import owmii.powah.*;
import owmii.powah.block.*;
import owmii.powah.energy.EnderNetwork;

/** Separate opt-in server fixture. Never included in the mod jar. */
@Mod(modid = "powah_server_verification", name = "Powah dedicated server verification", version = "1",
    dependencies = "required-after:powah-legacy", serverSideOnly = true, acceptableRemoteVersions = "*")
public final class PowahServerSmoke {
    private final JsonArray checks = new JsonArray();
    private MinecraftServer server;
    private WorldServer world;
    private MachineTile furnator, source, cable, sink;
    private int ticks;
    @Mod.EventHandler public void init(FMLInitializationEvent event) { MinecraftForge.EVENT_BUS.register(this); }
    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        server = FMLCommonHandler.instance().getMinecraftServerInstance(); world = server.getWorld(0);
        try {
            check(server.isDedicatedServer(), "The real dedicated server reaches its started event");
            String expectedCleanroom = System.getProperty("powah.expectedCleanroom");
            if (expectedCleanroom != null) check(expectedCleanroom.equals(Loader.instance().getIndexedModList().get("cleanroom").getVersion()), "The dedicated server runs on official Cleanroom " + expectedCleanroom);
            check(Powah.proxy.getClass() == CommonProxy.class, "Server uses the common proxy without loading client rendering or Ponder integration");
            check(Content.BLOCKS.size() == 107 && Content.ITEMS.size() == 145, "All Powah content registers on the dedicated server");
            furnator = place(0, "furnator_basic"); furnator.inventory.setStackInSlot(1, new ItemStack(Items.COAL));
            source = place(4, "energy_cable_basic"); source.energy().set(2000);
            cable = place(5, "energy_cable_basic"); sink = place(6, "energy_hopper_starter");
            source.sides[EnumFacing.EAST.ordinal()] = 1;
        } catch (Throwable error) { finish(error); }
    }
    private MachineTile place(int x, String name) {
        BlockPos pos = new BlockPos(x, 80, 0); world.getChunk(pos);
        // Repeated runs reuse this test world; replacing an identical state keeps its old tile.
        world.setBlockToAir(pos);
        world.setBlockState(pos, Content.BLOCKS.get(name).getDefaultState(), 3);
        return (MachineTile) world.getTileEntity(pos);
    }
    private void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
        checks.add(message); System.out.println("POWAH SERVER CHECK: " + message);
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null) return;
        try {
            ticks++;
            if (ticks == 20) {
                check(sink.energy().stored() == 0, "Input-only cable face blocks energy from crossing the route");
                source.sides[EnumFacing.EAST.ordinal()] = 2;
            }
            if (ticks == 50) {
                check(furnator.energy().stored() > 0 && furnator.energy().stored() + furnator.fuelBuffer == 48000,
                    "Furnator ticks on the dedicated server and conserves coal energy");
                check(sink.energy().stored() == 2000 && source.energy().stored() + cable.energy().stored() == 0,
                    "Output-only cable face transfers exactly 2,000 FE after reopening the route");
                var network = EnderNetwork.get(world);
                var channel = network.channel(UUID.fromString("127cfbd9-3938-42fb-afcc-a6b91ee7bc7e"), 0);
                channel.capacity(0); network.setDirty(false); channel.capacity(1000000);
                check(network.isDirty(), "An empty Ender capacity extension marks its world data for saving");
                finish(null);
            }
        } catch (Throwable error) { finish(error); }
    }
    private void finish(Throwable error) {
        try {
            JsonObject report = new JsonObject(); report.addProperty("result", error == null ? "PASS" : "FAIL");
            report.addProperty("withPonder", Loader.isModLoaded("ponder-legacy")); report.add("checks", checks);
            report.addProperty("cleanroom", Loader.instance().getIndexedModList().get("cleanroom").getVersion());
            if (error != null) { error.printStackTrace(); report.addProperty("error", error.toString()); }
            Files.writeString(Loader.instance().getConfigDir().toPath().getParent().resolve("powah-server-smoke.json"),
                new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { throw new IllegalStateException(failure); }
        finally { server.initiateShutdown(); server = null; }
    }
}
