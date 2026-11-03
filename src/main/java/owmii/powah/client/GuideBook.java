package owmii.powah.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import owmii.powah.*;
import owmii.powah.block.Tier;

/** The imported GuideME document data, kept entirely on the client. */
final class GuideBook {
    record Page(String id, String title, String parent, String icon, int position,
                List<String> items, List<JsonObject> blocks) {}
    final Map<String, Page> pages = new LinkedHashMap<>();
    private static final Pattern INLINE = Pattern.compile("<(ItemLink|powah:EnergyCapacity|powah:EnergyMaxIO|powah:EnergyGeneration) id=\"([^\"]+)\"\\s*/>");

    static GuideBook load() {
        String language = Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
        String file = language.startsWith("zh") ? "zh_cn" : "en_us";
        GuideBook book = new GuideBook();
        try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(Powah.id("guide/" + file + ".json"));
             var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            for (JsonElement element : new JsonParser().parse(reader).getAsJsonArray()) {
                JsonObject object = element.getAsJsonObject();
                List<JsonObject> blocks = new ArrayList<>();
                object.getAsJsonArray("blocks").forEach(block -> blocks.add(block.getAsJsonObject()));
                List<String> items = new ArrayList<>();
                object.getAsJsonArray("items").forEach(item -> items.add(item.getAsString()));
                Page page = new Page(string(object, "id"), string(object, "title"), string(object, "parent"),
                    string(object, "icon"), object.get("position").getAsInt(), items, blocks);
                book.pages.put(page.id(), page);
            }
        } catch (IOException error) { throw new IllegalStateException("Cannot load the Powah guide", error); }
        return book;
    }

    List<Page> children(String parent) {
        return pages.values().stream().filter(page -> page.parent().equals(parent) && !page.id().equals("index.md"))
            .sorted(Comparator.comparingInt(Page::position).thenComparing(Page::id)).toList();
    }
    String pageFor(ItemStack stack) {
        if (stack.isEmpty()) return null;
        String id = stack.getItem().getRegistryName().toString();
        return pages.values().stream().filter(page -> page.items().contains(id)).map(Page::id).findFirst().orElse(null);
    }
    static String string(JsonObject object, String key) { return object.get(key).getAsString(); }
    static ItemStack stack(String id) { return Content.stack(id.replace("powah:", "")); }
    static String itemIn(String text) {
        Matcher matcher = INLINE.matcher(text);
        return matcher.find() && matcher.group(1).equals("ItemLink") ? matcher.group(2) : null;
    }
    static String text(String source) {
        Matcher matcher = INLINE.matcher(source);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String type = matcher.group(1), id = matcher.group(2).replace("powah:", "");
            String value;
            if (type.equals("ItemLink")) value = Content.stack(id).getDisplayName();
            else {
                int split = id.lastIndexOf('_');
                String kind = id.substring(0, split); Tier tier = Tier.of(id.substring(split + 1));
                long number = switch (type) {
                    case "powah:EnergyCapacity" -> tier.capacity(kind);
                    case "powah:EnergyGeneration" -> Math.max(1, Math.round(tier.production(kind) * Config.productionRatio));
                    default -> tier.transfer(kind);
                };
                value = String.format(Locale.ROOT, "%,d", number) + (type.equals("powah:EnergyCapacity") ? " FE" : " FE/t");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString().replace("**", "").replace("`", "");
    }
}
