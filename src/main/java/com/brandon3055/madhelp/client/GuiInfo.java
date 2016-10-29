package com.brandon3055.madhelp.client;

import com.brandon3055.madhelp.handlers.ContentHandler.DownloadableContent;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.util.List;

/**
 * Created by brandon3055 on 19/9/2015.
 */
public class GuiInfo extends GuiScreen{
	private GuiScreen parent;
	private List<String> text;
	private GuiButton backButton;

	public GuiInfo(GuiScreen parent, List<String> text){
		this.parent = parent;
		this.text = text;
	}

	public GuiInfo(GuiScreen parent, DownloadableContent content){
		this.parent = parent;
		this.text = content.info;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initGui() {
		super.initGui();
		buttonList.clear();
		buttonList.add(backButton = new GuiButton(0, width / 2 - 50, height - 28, 100, 20, I18n.format("gui.back")));
	}

	@Override
	public void drawScreen(int x, int y, float f) {
		drawDefaultBackground();
		int yPos = 10;
		for (int i = 0; i < text.size(); i++){
			@SuppressWarnings("unchecked") List<String> list = fontRendererObj.listFormattedStringToWidth(text.get(i), width - 20);

			for (String text : list){
				if (text.length() > 3 && text.substring(0, 3).equals("[c]")){
					text = text.substring(text.indexOf("[c]") + 3);
					drawCenteredString(fontRendererObj, text, width / 2, yPos, 0xFFFFFF);
				}else {
					fontRendererObj.drawString(text, 10, yPos, 0xFFFFFF);
				}
				yPos += 10;
			}
			yPos += 5;
		}

		super.drawScreen(x, y, f);
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) mc.displayGuiScreen(parent);
	}
}
