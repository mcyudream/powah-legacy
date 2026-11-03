package owmii.powah;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import owmii.powah.block.*;
import owmii.powah.item.*;

public final class Content {
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static final CreativeTabs TAB = new CreativeTabs("powah") {
        @Override public ItemStack createIcon() { return stack("energy_cell_basic"); }
    };
    public static ItemStack stack(String id) { Item item = ITEMS.get(id); return item == null ? ItemStack.EMPTY : new ItemStack(item); }
    public static JsonElement json(String path) {
        var stream = Content.class.getResourceAsStream("/assets/powah/legacy/" + path);
        if (stream == null) throw new IllegalStateException("Missing Powah resource " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) { return new JsonParser().parse(reader); }
        catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> event) {
        for (JsonElement element : json("content.json").getAsJsonObject().getAsJsonArray("blocks")) {
            JsonObject data = element.getAsJsonObject(); String id = data.get("id").getAsString(), kind = data.get("kind").getAsString();
            Tier tier = data.has("tier") ? Tier.of(data.get("tier").getAsString()) : Tier.STARTER;
            Block block = switch (kind) {
                case "solid", "ore" -> new ResourceBlock(id);
                case "energy_cable" -> new CableBlock(tier);
                default -> new MachineBlock(kind, tier);
            };
            block.setRegistryName(Powah.id(id)).setTranslationKey("powah." + id).setCreativeTab(TAB);
            BLOCKS.put(id, block); event.getRegistry().register(block);
        }
    }
    @SubscribeEvent public static void items(RegistryEvent.Register<Item> event) {
        BLOCKS.forEach((id, block) -> {
            Item item = block instanceof MachineBlock ? new MachineItem((MachineBlock) block) : new ItemBlock(block);
            item.setRegistryName(Powah.id(id)); ITEMS.put(id, item); event.getRegistry().register(item);
        });
        for (JsonElement element : json("content.json").getAsJsonObject().getAsJsonArray("items")) {
            String id = element.getAsString();
            Item item = id.startsWith("battery_") ? new BatteryItem(Tier.of(id.substring(8))) : new UtilityItem(id);
            item.setRegistryName(Powah.id(id)).setTranslationKey("powah." + id).setCreativeTab(TAB);
            ITEMS.put(id, item); event.getRegistry().register(item);
        }
        ore("coal", new ItemStack(Items.COAL, 1, OreDictionary.WILDCARD_VALUE));
        ore("netherStar", new ItemStack(Items.NETHER_STAR)); ore("rodBlaze", new ItemStack(Items.BLAZE_ROD));
        ore("iceBlue", stack("blue_ice")); ore("ingotEnergizedSteel", stack("steel_energized"));
        for (String prefix : List.of("", "deepslate_")) {
            ore("oreUraninitePoor", stack(prefix + "uraninite_ore_poor"));
            ore("oreUraninite", stack(prefix + "uraninite_ore")); ore("oreUraniniteDense", stack(prefix + "uraninite_ore_dense"));
        }
        GameRegistry.addSmelting(stack("uraninite_raw"), stack("uraninite"), .5f);
        EntityRegistry.registerModEntity(Powah.id("charged_snowball"), ChargedSnowball.class, "charged_snowball", 0, Powah.INSTANCE, 64, 10, true);
    }
    private static void ore(String name, ItemStack stack) { OreDictionary.registerOre(name, stack); }
}
