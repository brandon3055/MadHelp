package com.brandon3055.madhelp.client;

import com.brandon3055.madhelp.handlers.ContentHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.StringUtils;
import org.apache.commons.io.FileUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Created by brandon3055 on 18/9/2015.
 */
public class GuiGetNewName extends GuiScreen {
    private GuiDownloading parent;
    private String invalidname;
    private GuiTextField textField;
    private String headingText;
    private GuiButton enterButton;
    private GuiButton cancelButton;

    public GuiGetNewName(GuiDownloading parent, String invalidname, boolean isFirstAttempt) {
        this.parent = parent;
        this.invalidname = invalidname;
        if (isFirstAttempt) headingText = I18n.format("gui.mad.selectNewName1.info");
        else headingText = I18n.format("gui.mad.selectNewName2.info");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        textField = new GuiTextField(0, fontRendererObj, width / 2 - 100, height / 2 - 10, 200, 20);
        textField.setTextColor(-1);
        textField.setDisabledTextColour(-1);
        textField.setEnableBackgroundDrawing(true);
        textField.setMaxStringLength(40);
        textField.setVisible(true);
        textField.setText(invalidname);
        buttonList.clear();
        buttonList.add(enterButton = new GuiButton(0, width / 2 - 101, height / 2 + 15, 98, 20, I18n.format("gui.mad.ok.button")));
        buttonList.add(cancelButton = new GuiButton(1, width / 2 + 3, height / 2 + 15, 98, 20, I18n.format("gui.cancel")));
        enterButton.enabled = false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        enterButton.enabled = !StringUtils.isNullOrEmpty(textField.getText()) && !textField.getText().equals(invalidname);
    }

    @Override
    public void drawScreen(int x, int t, float tick) {
        drawDefaultBackground();
        textField.drawTextBox();
        drawCenteredString(fontRendererObj, headingText, width / 2, height / 2 - 60, 0xFF0000);
        drawCenteredString(fontRendererObj, I18n.format("gui.mad.selectNewName3.info"), width / 2, height / 2 - 30, 0xFFFFFF);
        super.drawScreen(x, t, tick);
    }

    @Override
    protected void keyTyped(char key_ch, int key_i) throws IOException {
        textField.textboxKeyTyped(key_ch, key_i);
        if (key_i == Keyboard.KEY_RETURN && !StringUtils.isNullOrEmpty(textField.getText()) && !textField.getText().equals(invalidname))
            parent.tryNewName(textField.getText());
        super.keyTyped(key_ch, key_i);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        textField.mouseClicked(x, y, button);
        super.mouseClicked(x, y, button);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 && !StringUtils.isNullOrEmpty(textField.getText())) {
            parent.tryNewName(textField.getText());
        }
        else if (button.id == 1) {
            parent.message = I18n.format("gui.mad.cancelByUser.info");
            parent.installStage = 4;
            try {FileUtils.deleteDirectory(ContentHandler.tempFolder); }
            catch (IOException e) { e.printStackTrace(); }
            mc.displayGuiScreen(parent);
        }
    }
}
