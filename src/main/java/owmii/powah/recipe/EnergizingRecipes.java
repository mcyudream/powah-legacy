package owmii.powah.recipe;

import com.google.gson.*;
import java.util.*;
import net.minecraft.item.*;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreIngredient;
import owmii.powah.*;

public final class EnergizingRecipes {
    public record Recipe(String id, long energy, List<Ingredient> ingredients, ItemStack result) {
        public boolean matches(List<ItemStack> inputs) {
            if (inputs.size() != ingredients.size()) return false;
            return assign(inputs, 0, new boolean[ingredients.size()]);
        }
        private boolean assign(List<ItemStack> inputs, int index, boolean[] used) {
            if (index == inputs.size()) return true;
            for (int i = 0; i < used.length; i++) if (!used[i] && ingredients.get(i).apply(inputs.get(index))) {
                used[i] = true; if (assign(inputs, index + 1, used)) return true; used[i] = false;
            }
            return false;
        }
    }
    public static final List<Recipe> RECIPES = new ArrayList<>();
    public static void load() {
        RECIPES.clear();
        for (JsonElement element : Content.json("energizing.json").getAsJsonArray()) {
            JsonObject data = element.getAsJsonObject(); List<Ingredient> ingredients = new ArrayList<>();
            for (JsonElement value : data.getAsJsonArray("ingredients")) ingredients.add(ingredient(value.getAsJsonObject()));
            JsonObject output = data.getAsJsonObject("result"); Item item = Item.REGISTRY.getObject(new ResourceLocation(output.get("item").getAsString()));
            if (item == null) throw new IllegalStateException("Unknown Powah recipe output " + output);
            RECIPES.add(new Recipe(data.get("id").getAsString(), Math.max(1, Math.round(data.get("energy").getAsLong() * Config.energizingRatio)),
                ingredients, new ItemStack(item, output.has("count") ? output.get("count").getAsInt() : 1)));
        }
    }
    private static Ingredient ingredient(JsonObject data) {
        if (data.has("ore")) return new OreIngredient(data.get("ore").getAsString());
        Item item = Item.REGISTRY.getObject(new ResourceLocation(data.get("item").getAsString()));
        if (item == null) throw new IllegalStateException("Unknown Powah ingredient " + data);
        return Ingredient.fromStacks(new ItemStack(item, 1, data.has("data") ? data.get("data").getAsInt() : 0));
    }
    public static Recipe find(List<ItemStack> inputs) { for (Recipe recipe : RECIPES) if (recipe.matches(inputs)) return recipe; return null; }
}
