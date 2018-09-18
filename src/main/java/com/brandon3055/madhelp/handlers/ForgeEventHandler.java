package com.brandon3055.madhelp.handlers;

import com.brandon3055.madhelp.client.GuiDownloadList;
import com.brandon3055.madhelp.client.GuiInfo;
import net.minecraft.client.gui.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

/**
 * Created by brandon3055 on 17/9/2015.
 */
public class ForgeEventHandler {

    @SuppressWarnings("unchecked")
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void guiOpen(GuiScreenEvent.InitGuiEvent event) {
        if (event.getGui() instanceof GuiMainMenu) {
            for (Object button : event.getButtonList()) if (((GuiButton) button).id == 426) return;

            String bText = ConfigHandler.buttonText;
            if (ContentHandler.updates.size() > 0)
                bText += TextFormatting.RED + " " + I18n.format("gui.mad.new.button");

            event.getButtonList().add(new GuiButton(426, event.getGui().width / 2 + ConfigHandler.buttonPos[0], event.getGui().height / 4 + 48 + ConfigHandler.buttonPos[1], ConfigHandler.buttonPos[2], ConfigHandler.buttonPos[3], bText));

            if (ContentHandler.showSplashScreen) {
                ContentHandler.showSplashScreen = false;
                event.getGui().mc.displayGuiScreen(new GuiInfo(event.getGui(), ContentHandler.splashScreen.getKey()));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void buttonPressed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getGui() instanceof GuiMainMenu && event.getButton().id == 426) {
            event.getGui().mc.displayGuiScreen(new GuiDownloadList(event.getGui()));
            ContentHandler.reloadContentList();
            ContentHandler.updateCachedVersions();
        }
    }

    @SubscribeEvent
    public void guiOpen(GuiOpenEvent event) {
        if (event.getGui() instanceof GuiShareToLan && ConfigHandler.disableLaneCheatMode) {
            GuiShareToLan gui = (GuiShareToLan) event.getGui();

            try {
                Field parentGui = ReflectionHelper.findField(GuiShareToLan.class, "field_146598_a", "lastScreen");
                parentGui.setAccessible(true);

                Object parent = parentGui.get(gui);
                event.setGui(new GuiLane((GuiIngameMenu) parent));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class GuiLane extends GuiShareToLan {

        public GuiLane(GuiScreen p_i1055_1_) {
            super(p_i1055_1_);
        }

        @Override
        public void initGui() {
            super.initGui();
            if (buttonList.size() >= 4) {
                buttonList.remove(3);
                buttonList.remove(2);
            }
        }

        @Override
        public void drawScreen(int p_73863_1_, int p_73863_2_, float p_73863_3_) {
            this.drawDefaultBackground();
            this.drawCenteredString(this.fontRenderer, I18n.format("lanServer.title", new Object[0]), this.width / 2, 50, 16777215);

            int yPos = 82;
            for (String s : ConfigHandler.disableCheatMessage) {
                this.drawCenteredString(this.fontRenderer, s, this.width / 2, yPos, 16777215);
                yPos += 10;
            }

            int k;

            for (k = 0; k < this.buttonList.size(); ++k) {
                ((GuiButton) this.buttonList.get(k)).drawButton(this.mc, p_73863_1_, p_73863_2_, p_73863_3_);
            }

            for (k = 0; k < this.labelList.size(); ++k) {
                ((GuiLabel) this.labelList.get(k)).drawLabel(this.mc, p_73863_1_, p_73863_2_);
            }
        }
    }
}
