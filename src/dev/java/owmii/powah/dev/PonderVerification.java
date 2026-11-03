package owmii.powah.dev;

import java.util.List;
import net.createmod.ponder.api.diagnostic.PonderDiagnosticView;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import owmii.powah.*;

public final class PonderVerification {
    public static void verify() throws Exception {
        var diagnostics = PonderIndex.getDiagnosticSnapshot(PonderDiagnosticView.EFFECTIVE);
        long registrations = diagnostics.getScenes().stream().filter(scene -> scene.getPluginId().equals(Powah.MOD_ID)).count();
        if (registrations == 0) throw new IllegalStateException("Powah Ponder plugin is not registered");
        for (var scene : diagnostics.getScenes()) if (scene.getPluginId().equals(Powah.MOD_ID) && !scene.getIssues().isEmpty())
            throw new IllegalStateException("Powah Ponder registration diagnostics: " + scene.getSceneId() + " " + scene.getIssues());
        for (String name : List.of("power", "energizing", "reactor", "ender", "charging")) {
            var structure = new PonderStructureLoader().load(Powah.id(name));
            if (!structure.getDiagnostics().isEmpty()) throw new IllegalStateException(name + " structure: " + structure.getDiagnostics());
        }
        for (String item : List.of("book", "energizing_orb", "reactor_basic", "ender_cell_basic", "player_transmitter_basic")) {
            var scenes = PonderIndex.getSceneAccess().compile(Powah.id(item));
            if (scenes.isEmpty()) throw new IllegalStateException("No Ponder scenes for " + item);
            for (var scene : scenes) {
                if (scene.getTotalTicks() < 200) throw new IllegalStateException("Incomplete Ponder scene for " + item);
                for (int i = 0; i <= scene.getTotalTicks() + 5; i++) scene.tick();
            }
        }
    }
    public static void open() { Minecraft.getMinecraft().displayGuiScreen(PonderUI.of(Content.stack("energizing_orb"))); }
    public static void openReactor() { Minecraft.getMinecraft().displayGuiScreen(PonderUI.of(Content.stack("reactor_basic"))); }
}
