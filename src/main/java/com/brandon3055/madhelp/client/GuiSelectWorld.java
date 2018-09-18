package com.brandon3055.madhelp.client;


import com.brandon3055.brandonscore.handlers.FileHandler;
import com.brandon3055.madhelp.handlers.ContentHandler;
import com.brandon3055.madhelp.handlers.ContentHandler.Pair;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TextFormatting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brandon3055 on 20/9/2015.
 */
public class GuiSelectWorld extends GuiScreen {

    private GuiDownloading parent;
    private SelectionList downloadList;
    private List<Pair<String, String>> mapList = new ArrayList<Pair<String, String>>();
    private int selected = -1;
    private GuiButton selectButton;

    public GuiSelectWorld(GuiDownloading parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        this.mapList.clear();
        for (File file : new File(FileHandler.mcDirectory, "saves").listFiles(new ContentHandler.FilterFolders()))
            this.mapList.add(new Pair<String, String>(file.getName(), file.getPath()));
        this.downloadList = new SelectionList();
        this.downloadList.registerScrollButtons(4, 5);

        buttonList.clear();
        buttonList.add(selectButton = new GuiButton(0, width / 2 - 154, height - 52, 150, 20, I18n.format("gui.mad.addToWorld.button")));
        buttonList.add(new GuiButton(1, width / 2 + 4, height - 52, 150, 20, I18n.format("gui.cancel")));

        downloadList.updateButtonStates();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.downloadList.handleMouseInput();
    }

    @Override
    public void drawScreen(int x, int y, float tick) {
        downloadList.drawScreen(x, y, tick);
        super.drawScreen(x, y, tick);
        fontRenderer.drawSplitString(TextFormatting.RED + I18n.format("gui.mad.selectWorldWarning"), 20, 8, width - 20, 0x909090);
//		fontRenderer.drawSplitString(TextFormatting.RED + "Continue?", 20, 8, width - 20, 0x909090);
    }


    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == selectButton.id && selected > 0 && selected < mapList.size()) {
            parent.installInWorld(mapList.get(selected).getKey(), mapList.get(selected).getValue());
        }
        else if (button.id == 1) {
            mc.displayGuiScreen(parent.parent);
        }
    }

    class SelectionList extends GuiSlot {
        public SelectionList() {
            super(GuiSelectWorld.this.mc, GuiSelectWorld.this.width, GuiSelectWorld.this.height, 32, GuiSelectWorld.this.height - 64, 46);
        }

        @Override
        protected int getSize() {
            return GuiSelectWorld.this.mapList.size();
        }

        /**
         * The element in the slot that was clicked, boolean for whether it was double clicked or not
         */
        @Override
        protected void elementClicked(int index, boolean doubleClicked, int p_148144_3_, int p_148144_4_) {
            GuiSelectWorld.this.selected = index;
            updateButtonStates();
        }

        public void updateButtonStates() {
            boolean flag1 = GuiSelectWorld.this.selected >= 0 && GuiSelectWorld.this.selected < this.getSize();
            GuiSelectWorld.this.selectButton.enabled = flag1;
        }

        /**
         * Returns true if the element passed in is currently selected
         */
        @Override
        protected boolean isSelected(int selection) {
            return selection == GuiSelectWorld.this.selected;
        }

        /**
         * Return the height of the content being scrolled
         */
        @Override
        protected int getContentHeight() {
            return GuiSelectWorld.this.mapList.size() * 46;
        }

        @Override
        protected void drawBackground() {
            GuiSelectWorld.this.drawDefaultBackground();
        }

        @Override
        protected void drawSlot(int index, int x, int y, int var4, int var5, int var6, float pt) {
            String name = mapList.get(index).getKey();
            String path = mapList.get(index).getValue();
            if (StringUtils.isNullOrEmpty(name) || StringUtils.isNullOrEmpty(path)) {
                name = TextFormatting.DARK_RED + "ERROR";
                path = TextFormatting.DARK_RED + "ERROR";
            }

            GuiSelectWorld.this.drawString(GuiSelectWorld.this.fontRenderer, name, x + 2, y + 1, 0xFFFFFF);
            GuiSelectWorld.this.fontRenderer.drawSplitString(path, x + 2, y + 12, width - ((x + 2) * 2), 0x808080);
        }
    }
}
