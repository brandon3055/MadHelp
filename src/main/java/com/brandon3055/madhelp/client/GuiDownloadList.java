package com.brandon3055.madhelp.client;

import com.brandon3055.brandonscore.handlers.FileHandler;
import com.brandon3055.madhelp.LogHelper;
import com.brandon3055.madhelp.handlers.ContentHandler;
import com.brandon3055.madhelp.handlers.ContentHandler.DownloadableContent;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TextFormatting;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Created by brandon3055 on 17/9/2015.
 */
public class GuiDownloadList extends GuiScreen implements GuiYesNoCallback {

    private GuiScreen parent;
    private SelectionList downloadList;
    private List<DownloadableContent> mapList;
    private int selected = -1;
    private GuiButton downloadButton;
    private GuiButton openLinkButton;
    private GuiButton infoButton;
    private GuiButton websiteButton;

    public GuiDownloadList(GuiScreen parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        this.mapList = ContentHandler.compiledContentList;
        this.downloadList = new SelectionList();
        downloadList.registerScrollButtons(5, 6);

        buttonList.clear();
        buttonList.add(downloadButton = new GuiButton(0, width / 2 - 154, height - 52, 150, 20, I18n.format("gui.mad.download.button")));
        buttonList.add(openLinkButton = new GuiButton(1, width / 2 + 4, height - 52, 150, 20, I18n.format("gui.mad.openLink.button")));
        buttonList.add(infoButton = new GuiButton(2, width / 2 - 154, height - 28, 100, 20, I18n.format("gui.mad.info.button")));
        buttonList.add(websiteButton = new GuiButton(3, width / 2 - 50, height - 28, 100, 20, I18n.format("gui.mad.website.button")));
        buttonList.add(new GuiButton(4, width / 2 + 54, height - 28, 100, 20, I18n.format("gui.cancel")));

        downloadList.updateButtonStates();
    }

    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        this.downloadList.handleMouseInput();
    }

    @Override
    public void drawScreen(int x, int y, float tick) {
        downloadList.drawScreen(x, y, tick);
        super.drawScreen(x, y, tick);
        if (!ContentHandler.status.equals("OK")) {
            drawString(fontRendererObj, "Something went wrong!", 20, 10, 0xFF0000);
            fontRendererObj.drawSplitString(ContentHandler.status, 20, 24, width - 40, 0x909090);
        }
    }


    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == downloadButton.id) {
            tryStartDownload(false);
        }
        else if (button.id == openLinkButton.id) {
            mc.displayGuiScreen(new GuiConfirmOpenLink(this, mapList.get(selected).downloadUrl, 0, true));
        }
        else if (button.id == infoButton.id) {
            mc.displayGuiScreen(new GuiInfo(this, mapList.get(selected)));
        }
        else if (button.id == websiteButton.id) {
            mc.displayGuiScreen(new GuiConfirmOpenLink(this, mapList.get(selected).websiteUrl, 1, true));
        }
        else if (button.id == 4) {
            mc.displayGuiScreen(parent);
        }
    }

    private void tryStartDownload(boolean confirmed) {
        DownloadableContent content = mapList.get(selected);
        if (content == null) return;

        if (ContentHandler.downloadThread != null) {
            mc.displayGuiScreen(new GuiYesNo(this, I18n.format("gui.mad.checkInProgress1.info"), I18n.format("gui.mad.checkInProgress2.info"), I18n.format("gui.yes"), I18n.format("gui.no"), 2));
            return;
        }
        if (content.isRegionDownload && !confirmed) {
            mc.displayGuiScreen(new GuiConfirmDownload(this, 4, true));
            return;
        }
        if (new File(FileHandler.mcDirectory, "saves/" + content.fileName).exists()) {
            mc.displayGuiScreen(new GuiConfirmDownload(this, 3, false));
            return;
        }
        mc.displayGuiScreen(new GuiDownloading(this, content, 0));
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (id == 0 || id == 1) {
            mc.displayGuiScreen(this);
            if (result && id == 0) openLink(URI.create(mapList.get(selected).downloadUrl));
            if (result && id == 1) openLink(URI.create(mapList.get(selected).websiteUrl));
        }
        else if (id == 2) {
            if (result && ContentHandler.downloadThread != null) mc.displayGuiScreen(new GuiDownloading(this, ContentHandler.downloadThread.getContent(), 1));
            else mc.displayGuiScreen(this);
        }
        else if (id == 3) {
            if (result) mc.displayGuiScreen(new GuiDownloading(this, mapList.get(selected), 0));
            else mc.displayGuiScreen(new GuiDownloading(this, mapList.get(selected), 2));
        }
        else if (id == 4 && result) {
            tryStartDownload(true);
        }
    }

    private void openLink(URI uri) {
        try {
            Class oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop", new Class[0]).invoke(null, new Object[0]);
            oclass.getMethod("browse", new Class[]{URI.class}).invoke(object, new Object[]{uri});
        }
        catch (Throwable throwable) {
            LogHelper.error("Couldn\'t open link");
            throwable.printStackTrace();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    class SelectionList extends GuiSlot {
        public SelectionList() {
            super(GuiDownloadList.this.mc, GuiDownloadList.this.width, GuiDownloadList.this.height, 32, GuiDownloadList.this.height - 64, 36);
        }

        protected int getSize() {
            return GuiDownloadList.this.mapList.size();
        }

        /**
         * The element in the slot that was clicked, boolean for whether it was double clicked or not
         */
        protected void elementClicked(int index, boolean doubleClicked, int p_148144_3_, int p_148144_4_) {
            GuiDownloadList.this.selected = index;
            boolean flag1 = index >= 0 && index < this.getSize();
            updateButtonStates();

            if (doubleClicked && flag1) {
                GuiDownloadList.this.tryStartDownload(false);
            }
        }

        public void updateButtonStates() {
            boolean flag1 = GuiDownloadList.this.selected >= 0 && GuiDownloadList.this.selected < this.getSize();
            GuiDownloadList.this.downloadButton.enabled = flag1;
            GuiDownloadList.this.openLinkButton.enabled = flag1;
            GuiDownloadList.this.infoButton.enabled = flag1 && GuiDownloadList.this.mapList.get(selected).info.size() > 0;
            GuiDownloadList.this.websiteButton.enabled = flag1 && !StringUtils.isNullOrEmpty(GuiDownloadList.this.mapList.get(selected).websiteUrl);
        }

        /**
         * Returns true if the element passed in is currently selected
         */
        protected boolean isSelected(int selection) {
            return selection == GuiDownloadList.this.selected;
        }

        /**
         * Return the height of the content being scrolled
         */
        protected int getContentHeight() {
            return GuiDownloadList.this.mapList.size() * 36;
        }

        protected void drawBackground() {
            GuiDownloadList.this.drawDefaultBackground();
        }

        @Override
        protected void drawSlot(int entryID, int insideLeft, int yPos, int insideSlotHeight, int mouseXIn, int mouseYIn) {

            DownloadableContent map = mapList.get(entryID);
            String s = map.displayName;

            if (s == null || StringUtils.isNullOrEmpty(s)) {
                s = "Map " + (entryID + 1);
            }

            if (ContentHandler.updates.containsKey(s)) {
                String us;
                if (ContentHandler.updates.get(s)) {
                    us = TextFormatting.RED + " " + I18n.format("gui.mad.newMap.info");
                }
                else {
                    us = TextFormatting.RED + " " + I18n.format("gui.mad.newVersion.info");
                }
                GuiDownloadList.this.drawString(GuiDownloadList.this.fontRendererObj, us, 10, yPos + 1, 0xFFFFFF);
            }

            String description = map.description;
            String type = map.isRegionDownload ? TextFormatting.DARK_RED + I18n.format("gui.mad.regionalDownload.info") : TextFormatting.GREEN + I18n.format("gui.mad.mapDownload.info");

            GuiDownloadList.this.drawString(GuiDownloadList.this.fontRendererObj, s, insideLeft + 2, yPos + 1, 0xFFFFFF);
            GuiDownloadList.this.drawString(GuiDownloadList.this.fontRendererObj, description, insideLeft + 2, yPos + 12, 0x808080);
            GuiDownloadList.this.drawString(GuiDownloadList.this.fontRendererObj, type, insideLeft + 2, yPos + 12 + 10, 0xFFFFFF);
        }
    }
}
