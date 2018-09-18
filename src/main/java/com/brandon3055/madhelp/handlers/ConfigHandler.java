package com.brandon3055.madhelp.handlers;

import com.brandon3055.brandonscore.handlers.FileHandler;
import com.brandon3055.madhelp.LogHelper;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

/**
 * Created by Brandon on 7/04/2015.
 */
public class ConfigHandler {
    public static Configuration config;

    public static int[] buttonPos;
    public static String buttonText;
    public static boolean useRemoteList;
    public static String remoteListURL;
    public static boolean enableSplashScreen;
    public static boolean persistentSplashScreen;
    public static boolean useRemoteSplashScreenJSON;
    public static String remoteSplashScreenJSONURL;
    public static boolean disableLaneCheatMode;
    public static String[] disableCheatMessage;
    public static File configFolder;

    public static void init(FMLPreInitializationEvent event) {
        configFolder = new File(FileHandler.brandon3055Folder, "madhelp");
        config = new Configuration(new File(configFolder, "MadHelp.cfg"));

        try {
            buttonPos = config.get(Configuration.CATEGORY_GENERAL, "buttonPos&Size", new int[]{-100, 108, 200, 20}, "Set the posX, posY, width, height of the download button in the main menu (A position of 0,0 will put the top left corner of the button in the middle of the screen on the x axis ans level with the \"SinglePlayer\" button on the y axis) height should be left as 20 or the button will not render correctly", -1000, 1000, true, 4).getIntList();
            buttonText = config.getString("buttonText", Configuration.CATEGORY_GENERAL, "Map Downloads", "The text that shows on the download button");
            useRemoteList = config.getBoolean("useRemoteList", Configuration.CATEGORY_GENERAL, false, "If true list of downloadable maps will be read from an online JSON file at the given URL");
            remoteListURL = config.getString("remoteListURL", Configuration.CATEGORY_GENERAL, "", "If using a remote download list this should be a direct link to the remote JSON file");

            enableSplashScreen = config.getBoolean("enableSplashScreen", Configuration.CATEGORY_GENERAL, true, "This enables an information screen that will show up before the main menu loads. You can use this screen to give the player information about your pack");
            persistentSplashScreen = config.getBoolean("persistentSplashScreen", Configuration.CATEGORY_GENERAL, false, "if true the splash screen will show every time the game loads as opposed to only showing on first load or when you make a change to its content.");
            useRemoteSplashScreenJSON = config.getBoolean("useRemoteSplashScreenJSON", Configuration.CATEGORY_GENERAL, false, "If true the splash screen info will be downloaded from a specified link. Allowing you to make changes without having to update your pack");
            remoteSplashScreenJSONURL = config.getString("remoteSplashScreenJSONURL", Configuration.CATEGORY_GENERAL, "", "This is where you put the direct link to your online splash screen json if useRemoteSplashScreenJSON is set to true");

            disableLaneCheatMode = config.getBoolean("disableLaneCheatMode", Configuration.CATEGORY_GENERAL, true, "If set to true the \"Game Mode\" and \"Allow Cheats\" buttons will be removed from the \"LAN World\" gui. If using NEI in your pack you can do something similar in the MEI config to prevent players from changing using NEI cheats. Players can obviously change these settings if they really want to cheat but this should at least help discourage cheating.");
            String[] def = new String[]{"Nothing to see here......", "This gui has not been altered in any way!", "If you were expecting other buttons here you must have just imagined them."};
            disableCheatMessage = config.getStringList("disableCheatMessage", Configuration.CATEGORY_GENERAL, def, "This text will be shown in the LAN World gui where the disabled buttons used to be if disableLaneCheatMode is enabled");

        }
        catch (Exception e) {
            LogHelper.error("Error loading config file");
            e.printStackTrace();
        }
        finally {
            if (config.hasChanged()) config.save();
        }
    }
}
