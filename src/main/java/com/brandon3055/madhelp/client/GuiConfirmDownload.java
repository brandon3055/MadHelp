package com.brandon3055.madhelp.client;

import com.brandon3055.brandonscore.client.utils.GuiHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

/**
 * Created by brandon3055 on 17/9/2015.
 */
public class GuiConfirmDownload extends GuiYesNo{

	private String useExistingButtonText;
	private boolean regional;

	public GuiConfirmDownload(GuiYesNoCallback parent, int id, boolean regional) {
		super(parent, "", "", id);
		this.regional = regional;
		if (!regional)
		{
			this.confirmButtonText = I18n.format("gui.mad.reDownload.button");
			this.useExistingButtonText = I18n.format("gui.mad.useExisting.button");
			this.cancelButtonText = I18n.format("gui.cancel");
		}else {
			this.confirmButtonText = I18n.format("gui.yes");
			this.cancelButtonText = I18n.format("gui.no");
		}
	}

	@Override
	public void initGui() {
		super.initGui();
		buttonList.clear();

		this.buttonList.add(new GuiButton(0, this.width / 2 - 155, this.height / 6 + 96, 100, 20, this.confirmButtonText));
		if (!regional) this.buttonList.add(new GuiButton(1, this.width / 2 - 50, this.height / 6 + 96, 100, 20, I18n.format("gui.mad.useExisting.button")));
		this.buttonList.add(new GuiButton(2, this.width / 2 + 55, this.height / 6 + 96, 100, 20, this.cancelButtonText));
	}


	@Override
	public void drawScreen(int x, int y, float p_73863_3_) {
		super.drawScreen(x, y, p_73863_3_);

		if (!regional)
		{
			fontRendererObj.drawSplitString(I18n.format("gui.mad.reDownload1.info"), 20, 60, width - 40, 16777215);
			fontRendererObj.drawSplitString(TextFormatting.GREEN + I18n.format("gui.mad.reDownload2.info"), 20, 90, width - 40, 16777215);
		}
		else {
			fontRendererObj.drawSplitString(I18n.format("gui.mad.regionWarning1.info"), 20, 60, width - 40, 16777215);
			GuiHelper.drawCenteredSplitString(fontRendererObj, I18n.format("Do you want to continue?"), width/2, 130, width - 40, 16777215, false);
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0 || button.id == 1) {
			this.parentScreen.confirmClicked(button.id == 0, this.parentButtonClickedId);
		}
		else mc.displayGuiScreen((GuiScreen)parentScreen);
	}

}
