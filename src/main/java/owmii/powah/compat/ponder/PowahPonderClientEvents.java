package owmii.powah.compat.ponder;

import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Constructed only by the optional client Ponder plugin. */
public final class PowahPonderClientEvents {
    private boolean refreshOnJoin;

    @SubscribeEvent
    public void worldLoaded(WorldEvent.Load event) {
        if (event.getWorld().isRemote) refreshOnJoin = true;
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || !refreshOnJoin
                || minecraft.world == null || minecraft.player == null) return;
        refreshOnJoin = false;
        // Disconnect diagnostics can parse structures while Forge restores its registries.
        // Re-resolve block states after the next client world has joined those registries.
        PonderStructureLoader.invalidateCaches();
        PonderIndex.reload();
    }
}
