package com.brandon3055.madhelp.client;

import com.brandon3055.brandonscore.utils.DataUtils;
import com.brandon3055.brandonscore.utils.Utils;
import com.brandon3055.madhelp.LogHelper;
import com.brandon3055.madhelp.MadHelp;
import com.brandon3055.madhelp.handlers.ContentHandler;
import com.brandon3055.madhelp.handlers.ContentHandler.DownloadableContent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by brandon3055 on 17/9/2015.
 */
public class GuiDownloading extends GuiScreen implements GuiYesNoCallback {

    public GuiScreen parent;
    private DownloadableContent content;
    private long fileSize;
    private long downloaded;
    public String message = "";
    /**
     * What stage of the installation are we at?
     * 0 = Not Started
     * 1 = Downloading
     * 2 = Unzipping and installing
     * 3 = Finished
     * 4 = Failed
     */
    public int installStage;
    private GuiButton buttonCancel;
    private GuiButton buttonFinish;
    private boolean hasUnzipped = false;
    private int guiTick = 0;
    private static ResourceLocation guiElements = new ResourceLocation(MadHelp.MODID.toLowerCase() + ":textures/gui/downloadElements.png");

    public GuiDownloading(GuiScreen parent, DownloadableContent content, int installStage) {
        this.parent = parent;
        this.content = content;
        this.installStage = installStage;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();
        buttonList.add(buttonCancel = new GuiButton(0, width / 2 - 121, height / 2 + 20, 242, 20, I18n.format("gui.cancel")));
        buttonList.add(buttonFinish = new GuiButton(1, width / 2 - 121, height / 2 + 100, 242, 20, I18n.format("gui.mad.finish.button")));
        buttonFinish.visible = false;
    }

    @Override
    public void updateScreen() {
        guiTick++;
        super.updateScreen();
        //Start the download
        if (installStage == 0) {
            if (ContentHandler.downloadThread != null) {
                installStage = 4;
                message = "Was unable to start because the download thread was not null. This is most likely a bug please restart minecraft and try again. If you continue to see this message please report it to the mod developer";
                return;
            }
            ContentHandler.downloadThread = new ContentHandler.DownloadThread(content);
            ContentHandler.downloadThread.start();
            installStage = 1;
        }
        //Monitor the download
        else if (installStage == 1) {
            if (ContentHandler.downloadThread != null) {
                fileSize = ContentHandler.downloadThread.getDownloadSize();
                downloaded = ContentHandler.downloadThread.getDownloaded();
                if (ContentHandler.downloadThread.isFinished()) {
                    if (ContentHandler.downloadThread.wasSuccessful()) {
                        installStage = 2;
                        guiTick = 0;
                    }
                    else {
                        installStage = 4;
                        message = "Download Failed";
                    }
                }
            }
            else {
                installStage = 4;
                message = "The download thread was null... Hmm... That has to be a bug.";
            }
            if (fileSize > 0) buttonCancel.enabled = true;
        }
        //Unzip and install the world
        else if (installStage == 2) {
            buttonCancel.visible = false;
            if (guiTick < 2) return;

            //Unzip the file
            try {
                ContentHandler.unZipFile(content);
            }
            catch (IOException e) {
                LogHelper.error("Unzip Failed");
                message = "Failed to unzip the map - " + e.getLocalizedMessage();
                installStage = 4;
                e.printStackTrace();
                return;
            }

            //Find the world folder inside the temp folder (Will assume that the first folder found is the world folder)
            String error = ContentHandler.sortTheFolders();
            if (error != null) {
                message = error;
                installStage = 4;
                return;
            }

            if (!content.isRegionDownload) {
                File[] worlds = ContentHandler.saveFolder.listFiles(new ContentHandler.FilterFolders());
                boolean foundConflict = false;

                for (File world : worlds) {
                    if (world.getName().equals(ContentHandler.worldFolder.getName())) {
                        foundConflict = true;
                        break;
                    }
                }

                if (foundConflict) {
                    mc.displayGuiScreen(new GuiGetNewName(this, ContentHandler.worldFolder.getName(), true));
                    return;
                }
                else {
                    if (!ContentHandler.worldFolder.renameTo(new File(ContentHandler.saveFolder, ContentHandler.worldFolder.getName())))
                        message = "Was unable to transfer world from temporary folder to saves folder...";
                    try {FileUtils.deleteDirectory(ContentHandler.tempFolder); }
                    catch (IOException e) { e.printStackTrace(); }
                }

                if (StringUtils.isNullOrEmpty(message)) installStage = 3;
                else installStage = 4;
            }
            else {
                mc.displayGuiScreen(new GuiSelectWorld(this));
            }
        }
        //Download finished
        else if (installStage == 3) {
            buttonCancel.visible = false;
            buttonFinish.visible = true;
        }
        //Download faild
        else if (installStage == 4) {
            buttonCancel.visible = false;
            buttonFinish.visible = true;
        }
    }

    public void installInWorld(String name, String path) {
        mc.displayGuiScreen(this);
        File sourceFolder = new File(ContentHandler.worldFolder, content.regionInputPath);
        File destinationFolder = new File(path, content.regionOutputPath);

        try {
            FileUtils.copyDirectory(sourceFolder, destinationFolder);
        }
        catch (IOException e) {
            LogHelper.error("Unable to copy content");
            e.printStackTrace();
            installStage = 4;
            message = "Caught an exception when copying content to world - " + e.getLocalizedMessage();
            return;
        }

        installStage = 3;
    }

    public void tryNewName(String newName) {
        File[] worlds = ContentHandler.saveFolder.listFiles(new ContentHandler.FilterFolders());
        boolean foundConflict = false;

        for (File world : worlds) {
            if (world.getName().equals(newName)) {
                foundConflict = true;
                break;
            }
        }

        if (foundConflict) {
            mc.displayGuiScreen(new GuiGetNewName(this, newName, false));
            return;
        }
        else {
            if (!ContentHandler.worldFolder.renameTo(new File(ContentHandler.saveFolder, newName)))
                message = "Was unable to transfer world from temporary folder to saves folder...";
            try {FileUtils.deleteDirectory(ContentHandler.tempFolder); }
            catch (IOException e) { e.printStackTrace(); }
        }

        if (StringUtils.isNullOrEmpty(message)) installStage = 3;
        else installStage = 4;
        mc.displayGuiScreen(this);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float tick) {
        drawDefaultBackground();

        if (installStage == 1) {

            mc.renderEngine.bindTexture(guiElements);
            drawTexturedModalRect(width / 2 - 121, height / 2, 0, 0, 242, 18);

            if (fileSize > 0) {
                double progress = (double) downloaded / (double) fileSize;
                drawTexturedModalRect(width / 2 - 120, height / 2 + 1, 1, 18, (int) (progress * 240D), 16);

//				renderFalling(tick);
            }

            String progressText;
            if (fileSize == 0) progressText = I18n.format("gui.mad.starting.info");
            else {
                double progress = (double) downloaded / (double) fileSize;
                if (progress < 0.0001) progress = 0;
                progressText = I18n.format("gui.mad.downloading.info");
                progressText += " " + Utils.round(progress * 100D, 100) + "% - " + DataUtils.formatFileSize(downloaded) + " / " + DataUtils.formatFileSize(fileSize);
            }
            int w = fontRendererObj.getStringWidth(progressText);
            int x = width / 2 - w / 2;
            int y = height / 2 + 5;
            drawRect(x - 2, y - 2, x + w + 4, y + 10, 0x90000000);
            fontRendererObj.drawStringWithShadow(progressText, x, y, 0x00FF00);
        }
        else if (installStage == 2) {
            drawString(fontRendererObj, I18n.format("gui.mad.unzipping.info"), 50, 50, 0xFFFFFF);
        }
        else if (installStage == 3) {
            drawString(fontRendererObj, I18n.format("gui.mad.successful.info"), 50, 50, 0x00FF00);
        }
        else if (installStage == 4) {
            drawString(fontRendererObj, I18n.format("gui.mad.failed.info"), 50, 50, 0xFF0000);
            fontRendererObj.drawSplitString(I18n.format("gui.mad.why.info"), 50, 70, width - 100, 0xFFFF00);
            fontRendererObj.drawSplitString(message, 50, 80, width - 100, 0x909090);

            if (ContentHandler.downloadThread != null && !StringUtils.isNullOrEmpty(ContentHandler.downloadThread.getFaliureMessage())) {
                fontRendererObj.drawSplitString("Why:", 50, 110, width - 100, 0xFFFF00);
                fontRendererObj.drawSplitString(ContentHandler.downloadThread.getFaliureMessage(), 50, 120, width - 100, 0x909090);
            }


            if (message.equals("Download Failed")) {
                fontRendererObj.drawSplitString("This downloader is not as sophisticated as most. If you are having trouble downloading the map try " + "downloading the map via the direct link. Then place the downloaded zip in the saves folder and run the installation process again. " + "(Or you could just unzip it your self)", 50, 200, width - 100, 0xFFFFFF);
            }
        }


        super.drawScreen(mouseX, mouseY, tick);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 && ContentHandler.downloadThread != null) {
            ContentHandler.downloadThread.cancelDownload();
        }
        if (button.id == 1) {
            if (ContentHandler.downloadThread != null) {
                if (!ContentHandler.downloadThread.isFinished()) ContentHandler.downloadThread.cancelDownload();
                ContentHandler.downloadThread = null;
            }
            mc.displayGuiScreen(parent);
        }
    }

}
