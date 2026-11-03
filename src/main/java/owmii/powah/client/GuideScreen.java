package owmii.powah.client;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.util.*;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import owmii.powah.Powah;

/** GuideME 21.1.7's reading layout and assets, implemented with the 1.12 GUI API. */
public final class GuideScreen extends GuiScreen {
    private static final ResourceLocation BACKGROUND = Powah.id("textures/guide/background.png");
    private static final ResourceLocation BUTTONS = Powah.id("textures/guide/buttons.png");
    private static final int BODY = 0xd2d2d2, LINK = 0x00d5ff, HEADER = 25;
    private GuideBook book;
    private String pageId = "index.md";
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private final Set<String> expanded = new HashSet<>();
    private final List<Part> document = new ArrayList<>();
    private final List<NavRow> navigation = new ArrayList<>();
    private final Map<String, List<IRecipe>> recipes = new HashMap<>();
    private int left, boxWidth, documentLeft, documentWidth, scroll, contentHeight, navScroll;
    private int thumbHeldAt = -1;
    private boolean fullWidth = true, sidebarOpen, searching;
    private GuiTextField search;
    private String lastQuery = "";

    private record NavRow(GuideBook.Page page, int depth) {}
    private static final class Part {
        int type, x, y, w, h, color;
        String text, target;
        ItemStack stack = ItemStack.EMPTY;
        ResourceLocation image;
        Part(int type, int x, int y, int w, int h, int color) {
            this.type = type; this.x = x; this.y = y; this.w = w; this.h = h; this.color = color;
        }
    }

    public GuideScreen() { expanded.add("index.md"); }
    @Override public void initGui() {
        if (book == null) book = GuideBook.load();
        if (historyIndex < 0) { history.add(pageId); historyIndex = 0; }
        boxWidth = fullWidth ? width : Math.min(570, width); left = (width - boxWidth) / 2;
        documentLeft = left + (pinned() ? 150 : 15);
        documentWidth = left + boxWidth - documentLeft - 8;
        buttonList.clear();
        int start = left + boxWidth - 105;
        for (int id = 0; id < 5; id++) buttonList.add(new IconButton(id, start + id * 21, 4));
        search = new GuiTextField(0, fontRenderer, documentLeft + 7, HEADER + 8, Math.max(40, documentWidth - 20), 18);
        search.setMaxStringLength(100); search.setText(lastQuery); search.setFocused(searching);
        rebuildNavigation(); rebuildDocument(); updateButtons();
    }
    private boolean pinned() { return width >= 570; }
    private GuideBook.Page page() { return book.pages.get(pageId); }
    public String getCurrentPageId() { return pageId; }
    public void navigateTo(String target) {
        if (!book.pages.containsKey(target)) return;
        if (historyIndex + 1 < history.size()) history.subList(historyIndex + 1, history.size()).clear();
        history.add(target); historyIndex = history.size() - 1;
        loadPage(target);
    }
    private void loadPage(String target) {
        pageId = target; scroll = 0; searching = false; search.setFocused(false);
        String parent = page().parent();
        while (!parent.isEmpty()) { expanded.add(parent); parent = book.pages.get(parent).parent(); }
        rebuildNavigation(); rebuildDocument(); updateButtons();
    }
    private void updateButtons() {
        buttonList.get(1).enabled = historyIndex > 0;
        buttonList.get(2).enabled = historyIndex + 1 < history.size();
    }
    @Override protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0 -> { searching = !searching; search.setFocused(searching); scroll = 0; rebuildDocument(); }
            case 1 -> { if (historyIndex > 0) loadPage(history.get(--historyIndex)); }
            case 2 -> { if (historyIndex + 1 < history.size()) loadPage(history.get(++historyIndex)); }
            case 3 -> { fullWidth = !fullWidth; initGui(); }
            case 4 -> mc.displayGuiScreen(null);
        }
    }
    private void rebuildNavigation() { navigation.clear(); addNavigation(book.pages.get("index.md"), 0); }
    private void addNavigation(GuideBook.Page page, int depth) {
        navigation.add(new NavRow(page, depth));
        if (expanded.contains(page.id())) for (GuideBook.Page child : book.children(page.id())) addNavigation(child, depth + 1);
    }
    private int text(String value, int x, int y, int space, int color, String target) {
        for (String line : fontRenderer.listFormattedStringToWidth(GuideBook.text(value), Math.max(12, space))) {
            Part part = new Part(0, x, y, fontRenderer.getStringWidth(line), 12, color);
            part.text = line; part.target = target; document.add(part); y += 12;
        }
        return y;
    }
    private void rectangle(int x, int y, int w, int h, int color) { document.add(new Part(3, x, y, w, h, color)); }
    private void item(ItemStack stack, int x, int y) {
        Part part = new Part(2, x, y, 16, 16, 0); part.stack = stack;
        part.target = book.pageFor(stack); document.add(part);
    }
    private int link(GuideBook.Page target, int y) {
        item(GuideBook.stack(target.icon()), 8, y);
        return text(target.title(), 30, y + 4, documentWidth - 40, LINK, target.id()) + 9;
    }
    private void rebuildDocument() {
        document.clear(); int y = searching ? 38 : 10;
        if (searching) {
            String query = search.getText().strip().toLowerCase(Locale.ROOT);
            for (GuideBook.Page candidate : book.pages.values()) {
                if (!query.isEmpty() && (candidate.title() + " " + candidate.blocks()).toLowerCase(Locale.ROOT).contains(query)) y = link(candidate, y);
            }
            if (y == 38) y = text(I18n.format(query.isEmpty() ? "powah.guide.search_hint" : "powah.guide.no_results"), 8, y, documentWidth - 20, BODY, null);
        } else {
            if (Loader.isModLoaded("ponder-legacy")) y = text(I18n.format("powah.guide.ponder"), 8, y, documentWidth - 20, LINK, "@ponder") + 9;
            for (JsonObject block : page().blocks()) {
                switch (GuideBook.string(block, "type")) {
                    case "text" -> y = text(GuideBook.string(block, "text"), 8, y, documentWidth - 20, BODY, null) + 9;
                    case "heading" -> {
                        y += 5; y = text(GuideBook.string(block, "text"), 8, y, documentWidth - 20, 0xffffff, null) + 3;
                        rectangle(8, y, documentWidth - 20, 1, 0x7fd2d2d2); y += 9;
                    }
                    case "image" -> {
                        int w = Math.min(documentWidth - 20, block.get("width").getAsInt());
                        int h = Math.max(1, w * block.get("height").getAsInt() / block.get("width").getAsInt());
                        Part part = new Part(1, 8, y, w, h, 0);
                        part.image = Powah.id("textures/guide/" + GuideBook.string(block, "image")); document.add(part); y += h + 10;
                    }
                    case "link" -> y = text(GuideBook.string(block, "text"), 8, y, documentWidth - 20, LINK, GuideBook.string(block, "target")) + 9;
                    case "children" -> { for (GuideBook.Page child : book.children(pageId)) y = link(child, y); }
                    case "table" -> y = table(block.getAsJsonArray("rows"), y);
                    case "recipes" -> y = recipeCards(block.getAsJsonArray("items"), y);
                }
            }
        }
        contentHeight = y + 8; scroll = Math.min(scroll, maxScroll());
    }
    private int table(JsonArray rows, int y) {
        int columns = rows.get(0).getAsJsonArray().size(), total = documentWidth - 20;
        int first = columns == 1 ? total : Math.max(total / columns, total * 2 / 5);
        int rest = columns < 2 ? 0 : (total - first) / (columns - 1);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            JsonArray row = rows.get(rowIndex).getAsJsonArray(); int x = 8, bottom = y + 22;
            for (int col = 0; col < columns; col++) {
                int w = col == 0 ? first : col == columns - 1 ? total - first - rest * (columns - 2) : rest;
                String raw = row.get(col).getAsString(), id = GuideBook.itemIn(raw); int inset = id == null ? 4 : 22;
                if (id != null) item(GuideBook.stack(id), x + 3, y + 3);
                bottom = Math.max(bottom, text(raw, x + inset, y + 6, w - inset - 3, rowIndex == 0 ? 0xffffff : BODY, id == null ? null : book.pageFor(GuideBook.stack(id))) + 4);
                x += w;
            }
            rectangle(8, y, total, 1, 0xff7c7c7c); x = 8;
            for (int col = 0; col <= columns; col++) {
                rectangle(x, y, 1, bottom - y, 0xff7c7c7c);
                x = col == 0 ? 8 + first : col == columns - 1 ? 8 + total : x + rest;
            }
            y = bottom;
        }
        rectangle(8, y, total, 1, 0xff7c7c7c); return y + 12;
    }
    private int recipeCards(JsonArray ids, int y) {
        y = text(I18n.format("powah.guide.recipes"), 8, y + 4, documentWidth - 20, 0xffffff, null) + 7;
        for (JsonElement element : ids) {
            String id = element.getAsString(); ItemStack output = GuideBook.stack(id);
            List<IRecipe> matching = recipes.computeIfAbsent(id, key -> {
                List<IRecipe> result = new ArrayList<>();
                for (IRecipe recipe : CraftingManager.REGISTRY) if (ItemStack.areItemsEqual(recipe.getRecipeOutput(), output)) result.add(recipe);
                return result;
            });
            if (matching.isEmpty()) {
                item(output, 8, y); y = text(output.getDisplayName() + ": " + I18n.format("powah.guide.interaction_recipe"), 30, y + 3, documentWidth - 40, BODY, null) + 10;
                continue;
            }
            for (IRecipe recipe : matching) {
                y = text(output.getDisplayName(), 8, y, documentWidth - 20, BODY, book.pageFor(output)) + 3;
                int rowWidth = recipe instanceof IShapedRecipe shaped ? shaped.getRecipeWidth() : 3;
                NonNullList<Ingredient> ingredients = recipe.getIngredients();
                for (int slot = 0; slot < 9; slot++) {
                    int col = slot % 3, row = slot / 3, input = row * rowWidth + col;
                    rectangle(8 + col * 18, y + row * 18, 17, 17, 0xff252525);
                    if (col < rowWidth && input < ingredients.size()) {
                        ItemStack[] choices = ingredients.get(input).getMatchingStacks();
                        if (choices.length > 0) item(choices[0], 9 + col * 18, y + row * 18 + 1);
                    }
                }
                text("→", 72, y + 23, 20, BODY, null);
                rectangle(96, y + 18, 19, 19, 0xff252525); item(recipe.getRecipeOutput(), 98, y + 20); y += 65;
            }
        }
        return y + 5;
    }
    private int maxScroll() { return Math.max(0, contentHeight - (height - HEADER)); }
    private int thumbHeight() { return Math.min(height - HEADER, Math.max(32, (height - HEADER) * (height - HEADER) / Math.max(1, contentHeight))); }
    private int thumbY() { return HEADER + (maxScroll() == 0 ? 0 : scroll * (height - HEADER - thumbHeight()) / maxScroll()); }
    private void clip(int x, int y, int w, int h) {
        int factor = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST); GL11.glScissor(x * factor, mc.displayHeight - (y + h) * factor, w * factor, h * factor);
    }
    @Override public void drawScreen(int mouseX, int mouseY, float partial) {
        drawDefaultBackground(); GlStateManager.enableBlend(); GlStateManager.color(63 / 255f, 63 / 255f, 63 / 255f, 229 / 255f);
        mc.getTextureManager().bindTexture(BACKGROUND); drawModalRectWithCustomSizedTexture(left, 0, 0, 0, boxWidth, height, 16, 16);
        GlStateManager.color(1, 1, 1, 1); drawRect(documentLeft, HEADER, left + boxWidth, height, 0x40333333);
        int titleSpace = Math.max(30, boxWidth - 125 - (pinned() ? 0 : 15));
        GlStateManager.pushMatrix(); GlStateManager.translate(left + (pinned() ? 5 : 20), 6, 0); GlStateManager.scale(1.3f, 1.3f, 1);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(searching ? I18n.format("powah.guide.search") : page().title(), (int) (titleSpace / 1.3f)), 0, 0, 0xffffff);
        GlStateManager.popMatrix(); drawRect(left, HEADER - 1, left + boxWidth, HEADER, 0x7fffffff);
        clip(documentLeft, HEADER, documentWidth, height - HEADER);
        Part hovered = null;
        for (Part part : document) {
            int x = documentLeft + part.x, y = HEADER + part.y - scroll;
            if (y + part.h < HEADER || y >= height) continue;
            switch (part.type) {
                case 0 -> fontRenderer.drawString(part.text, x, y, part.color);
                case 1 -> {
                    GlStateManager.color(1, 1, 1, 1); mc.getTextureManager().bindTexture(part.image);
                    drawScaledCustomSizeModalRect(x, y, 0, 0, part.w, part.h, part.w, part.h, part.w, part.h);
                }
                case 2 -> {
                    RenderHelper.enableGUIStandardItemLighting(); itemRender.renderItemAndEffectIntoGUI(part.stack, x, y);
                    itemRender.renderItemOverlayIntoGUI(fontRenderer, part.stack, x, y, null); RenderHelper.disableStandardItemLighting();
                }
                case 3 -> drawRect(x, y, x + part.w, y + part.h, part.color);
            }
            if (mouseX >= x && mouseX < x + part.w && mouseY >= Math.max(HEADER, y) && mouseY < y + part.h) hovered = part;
        }
        if (searching) search.drawTextBox();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (maxScroll() > 0) {
            int x = left + boxWidth - 8, y = thumbY(); drawRect(x, y, x + 8, y + thumbHeight(), 0xff808080); drawRect(x, y, x + 7, y + thumbHeight() - 1, 0xffc0c0c0);
        }
        drawNavigation(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partial);
        if (hovered != null && (!sidebarOpen || pinned() || mouseX >= left + 150)) {
            if (!hovered.stack.isEmpty()) renderToolTip(hovered.stack, mouseX, mouseY);
            else if (hovered.target != null) drawRect(documentLeft + hovered.x, HEADER + hovered.y - scroll + 9, documentLeft + hovered.x + hovered.w, HEADER + hovered.y - scroll + 10, 0xff00d5ff);
        }
        for (GuiButton button : buttonList) if (button.isMouseOver()) drawHoveringText(I18n.format("powah.guide.toolbar." + button.id), mouseX, mouseY);
    }
    private void drawNavigation(int mouseX, int mouseY) {
        if (!pinned()) sidebarOpen = mouseX >= left && mouseX < left + (sidebarOpen ? 150 : 15);
        if (!pinned() && !sidebarOpen) {
            drawGradientRect(left, HEADER, left + 15, height, 0xff000000, 0x7f000000);
            fontRenderer.drawString(">", left + 4, height / 2, BODY); return;
        }
        if (!pinned()) drawGradientRect(left, HEADER, left + 150, height, 0xff000000, 0xe0000000);
        clip(left, HEADER, 150, height - HEADER);
        for (int i = 0; i < navigation.size(); i++) {
            NavRow row = navigation.get(i); int y = HEADER + 8 + i * 18 - navScroll, x = left + row.depth() * 10 + 5;
            if (y + 17 < HEADER || y > height) continue;
            boolean hover = mouseX >= left && mouseX < left + 150 && mouseY >= y && mouseY < y + 18;
            if (hover) drawRect(left, y, left + 150, y + 18, 0xff212121);
            if (!book.children(row.page().id()).isEmpty()) fontRenderer.drawString(expanded.contains(row.page().id()) ? "v" : ">", x, y + 4, BODY);
            RenderHelper.enableGUIStandardItemLighting(); itemRender.renderItemAndEffectIntoGUI(GuideBook.stack(row.page().icon()), x + 9, y); RenderHelper.disableStandardItemLighting();
            fontRenderer.drawString(fontRenderer.trimStringToWidth(row.page().title(), 150 - row.depth() * 10 - 31), x + 28, y + 4, row.page().id().equals(pageId) || hover ? LINK : BODY);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
    @Override protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (mouseY < HEADER) { super.mouseClicked(mouseX, mouseY, button); return; }
        if (mouseX >= left && mouseX < left + 150 && (pinned() || sidebarOpen) && button == 0) {
            int index = (mouseY - HEADER - 8 + navScroll) / 18;
            if (mouseY - HEADER - 8 + navScroll >= 0 && index < navigation.size()) {
                GuideBook.Page target = navigation.get(index).page();
                if (!expanded.add(target.id())) expanded.remove(target.id());
                navigateTo(target.id());
            }
            return;
        }
        if (searching) search.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && mouseX >= left + boxWidth - 8 && mouseX < left + boxWidth && maxScroll() > 0) { thumbHeldAt = mouseY - thumbY(); return; }
        if (button == 3 || button == 4) { actionPerformed(buttonList.get(button == 3 ? 1 : 2)); return; }
        if (button != 0 || mouseX < documentLeft || mouseX >= documentLeft + documentWidth) return;
        for (Part part : document) {
            int x = documentLeft + part.x, y = HEADER + part.y - scroll;
            if (part.target != null && mouseX >= x && mouseX < x + part.w && mouseY >= y && mouseY < y + part.h) {
                if (part.target.equals("@ponder")) owmii.powah.compat.ponder.PowahPonderPlugin.open(GuideBook.stack(page().icon()));
                else navigateTo(part.target);
                return;
            }
        }
    }
    @Override protected void mouseClickMove(int x, int y, int button, long elapsed) {
        if (thumbHeldAt >= 0 && height - HEADER > thumbHeight()) scroll = Math.max(0, Math.min(maxScroll(), (y - HEADER - thumbHeldAt) * maxScroll() / (height - HEADER - thumbHeight())));
    }
    @Override protected void mouseReleased(int x, int y, int button) { thumbHeldAt = -1; super.mouseReleased(x, y, button); }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int delta = Mouse.getEventDWheel();
        if (delta == 0) return;
        int x = Mouse.getEventX() * width / mc.displayWidth;
        if (x < left + 150 && (pinned() || sidebarOpen)) navScroll = Math.max(0, Math.min(Math.max(0, navigation.size() * 18 + 8 - height + HEADER), navScroll - Integer.signum(delta) * 36));
        else scroll = Math.max(0, Math.min(maxScroll(), scroll - Integer.signum(delta) * 30));
    }
    @Override protected void keyTyped(char character, int key) throws IOException {
        if (searching && search.textboxKeyTyped(character, key)) {
            lastQuery = search.getText(); scroll = 0; rebuildDocument(); return;
        }
        if (key == Keyboard.KEY_F && GuiScreen.isCtrlKeyDown()) { actionPerformed(buttonList.get(0)); return; }
        if (key == Keyboard.KEY_ESCAPE && searching) { actionPerformed(buttonList.get(0)); return; }
        if (key == Keyboard.KEY_NEXT || key == Keyboard.KEY_DOWN) scroll = Math.min(maxScroll(), scroll + (key == Keyboard.KEY_NEXT ? height - HEADER - 20 : 12));
        else if (key == Keyboard.KEY_PRIOR || key == Keyboard.KEY_UP) scroll = Math.max(0, scroll - (key == Keyboard.KEY_PRIOR ? height - HEADER - 20 : 12));
        else if (key == Keyboard.KEY_HOME) scroll = 0;
        else if (key == Keyboard.KEY_END) scroll = maxScroll();
        else super.keyTyped(character, key);
    }
    @Override public void updateScreen() { search.updateCursorCounter(); }
    @Override public boolean doesGuiPauseGame() { return false; }

    private final class IconButton extends GuiButton {
        IconButton(int id, int x, int y) { super(id, x, y, 16, 16, ""); }
        @Override public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partial) {
            hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
            int color = !enabled ? 64 : hovered ? 255 : 200;
            GlStateManager.color(hovered && enabled ? 0 : color / 255f, hovered && enabled ? 213 / 255f : color / 255f, color / 255f, 1);
            GlStateManager.enableBlend(); minecraft.getTextureManager().bindTexture(BUTTONS);
            int u = switch (id) { case 0 -> 48; case 1 -> 0; case 2 -> 16; case 3 -> fullWidth ? 32 : 16; default -> 32; };
            drawModalRectWithCustomSizedTexture(x, y, u, id == 3 ? 32 : 0, 16, 16, 64, 64); GlStateManager.color(1, 1, 1, 1);
        }
    }
}
