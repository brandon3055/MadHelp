package com.brandon3055.madhelp.handlers;

import com.brandon3055.brandonscore.handlers.FileHandler;
import com.brandon3055.madhelp.LogHelper;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.brandon3055.madhelp.MadHelp.SELECT;

/**
 * Created by brandon3055 on 17/9/2015.
 */
public class ContentHandler {

    public static List<DownloadableContent> compiledContentList = new ArrayList<DownloadableContent>();
    public static String status = "OK";
    public static DownloadThread downloadThread;
    public static UnzipThread unZipThread;
    public static Map<String, Integer> cachedVersions = new HashMap<String, Integer>();
    public static Map<String, Boolean> updates = new HashMap<String, Boolean>();
    public static Pair<List<String>, Integer> splashScreen = new Pair<List<String>, Integer>(new ArrayList<String>(), 0);
    public static boolean showSplashScreen = true;

    public static void init() {
        addDefaultFiles();
        File contentFile = getContentFile();

        if (!status.equals("OK")) return;
        else if (contentFile == null) {
            status = "Not sure what happened there... This may be a bug...";
            return;
        }

        File splashScreenFile = getSplashScreenFile();

        if (status.equals("OK")) {
            readSplashInfo(splashScreenFile);
        }
        else {
            splashScreen.key.add(TextFormatting.RED + "Encountered an error while trying to load the splash screen");
            splashScreen.key.add(status);
        }

        readContentFile(contentFile);
        ContentHandler.readCachedVersions();
        updateSplashRevision();
    }

    public static void reloadContentList() {
        readContentFile(getContentFile());
    }

    private static void updateSplashRevision() {
        if (ConfigHandler.persistentSplashScreen) return;
        else if (!ContentHandler.showSplashScreen) {
            showSplashScreen = false;
            return;
        }

        File splashRev = new File(ConfigHandler.configFolder, "MadHelpSplashRev.json");
        boolean show = false;
        if (!splashRev.exists()) show = true;
        else {
            try {
                int rev = -1;
                JsonReader reader = new JsonReader(new FileReader(splashRev));
                reader.setLenient(true);
                reader.beginObject();
                if (reader.nextName().equals("splashRevision")) rev = reader.nextInt();
                reader.endObject();
                reader.close();
                show = rev != splashScreen.value;
            }
            catch (FileNotFoundException e) {
                LogHelper.error("Encountered an error while reading splash screen revision json.");
                e.printStackTrace();
            }
            catch (IOException e) {
                LogHelper.error("Encountered an error while reading splash screen revision json");
                e.printStackTrace();
            }
            catch (IllegalStateException e) {
                LogHelper.error("Looks like the splash screen revision json is invalid. It has been deleted");
                e.printStackTrace();
                splashRev.delete();
            }
        }

        if (show) {
            try {
                JsonWriter writer = new JsonWriter(new FileWriter(splashRev));
                writer.beginObject();
                writer.name("splashRevision").value(splashScreen.value);
                writer.endObject();
                writer.close();
            }
            catch (IOException e) {
                LogHelper.error("Encountered a problem when updating splash screen revision cache");
                e.printStackTrace();
            }

        }
        else showSplashScreen = false;
    }

    public static void readCachedVersions() {
        updates.clear();
        File versionCash = new File(ConfigHandler.configFolder, "MadHelpCachedVersions.json");
        if (!versionCash.exists()) {
            LogHelper.info("Versions Null");
            for (DownloadableContent content : compiledContentList) {
                updates.put(content.displayName, true);
            }
            return;
        }
        cachedVersions.clear();

        try {
            JsonReader reader = new JsonReader(new FileReader(versionCash));
            reader.setLenient(true);
            reader.beginArray();

            while (reader.hasNext()) {
                reader.beginObject();
                String name = "";
                int version = 0;

                while (reader.hasNext()) {
                    String nextField = reader.nextName();
                    if (nextField.equals("contentName")) {
                        name = reader.nextString();
                    }
                    else if (nextField.equals("cachedVersion")) {
                        version = reader.nextInt();
                    }
                }

                cachedVersions.put(name, version);
                reader.endObject();
            }
            reader.endArray();
            reader.close();
        }
        catch (FileNotFoundException e) {
            LogHelper.error("Encountered an error while reading cached versions.");
            e.printStackTrace();
        }
        catch (IOException e) {
            LogHelper.error("Encountered an error while reading cached versions.");
            e.printStackTrace();
        }
        catch (IllegalStateException e) {
            LogHelper.error("Looks like the cached versions json file is invalid. It will be deleted");
            versionCash.delete();
            e.printStackTrace();
        }

        for (DownloadableContent content : compiledContentList) {
            if (!cachedVersions.containsKey(content.displayName) || cachedVersions.get(content.displayName) != content.releaseNumber)
                updates.put(content.displayName, !cachedVersions.containsKey(content.displayName));
        }
    }

    public static void updateCachedVersions() {
        File versionCash = new File(ConfigHandler.configFolder, "MadHelpCachedVersions.json");
        if (compiledContentList.size() == 0) return;

        Map<String, Integer> versions = new HashMap<String, Integer>();
        for (DownloadableContent content : compiledContentList)
            versions.put(content.displayName, content.releaseNumber);

        try {
            JsonWriter writer = new JsonWriter(new FileWriter(versionCash));
            writer.setIndent("	");

            writer.beginArray();

            for (String name : versions.keySet()) {

                writer.beginObject();
                writer.name("contentName").value(name);
                writer.name("cachedVersion").value(versions.get(name));
                writer.endObject();
            }

            writer.endArray();
            writer.close();
        }
        catch (IOException e) {
            LogHelper.error("Something went wrong while updating cached versions!");
            e.printStackTrace();
        }

    }

    private static File getContentFile() {
        File contentFile;

        if (ConfigHandler.useRemoteList) {
            contentFile = new File(ConfigHandler.configFolder, "MadHelpContentList.json");
            try {
                BufferedInputStream is = new BufferedInputStream(new URL(ConfigHandler.remoteListURL).openStream());
                FileOutputStream os = new FileOutputStream(contentFile);
                IOUtils.copy(is, os);
                is.close();
                os.close();
            }
            catch (MalformedURLException e) {
                LogHelper.error("Unable to download remote content list [Invalid URL] - " + ConfigHandler.remoteListURL);
                status = "Unable to download remote content list [Invalid URL] - URL: " + ConfigHandler.remoteListURL + " Exception: " + e.getLocalizedMessage();
                e.printStackTrace();
                return null;
            }
            catch (IOException e) {
                LogHelper.error("Unable to download remote content list");
                status = "Unable to download remote content list [IOException] - " + e.getLocalizedMessage();
                e.printStackTrace();
                return null;
            }
        }
        else {
            contentFile = new File(ConfigHandler.configFolder, "MadHelpContentList.json");
            if (!contentFile.exists()) {
                LogHelper.error("Unable to load local content list [File dose not exist] - " + contentFile);
                status = "Unable to load local content list [File dose not exist] - " + contentFile;
                return null;
            }
        }
        return contentFile;
    }

    private static File getSplashScreenFile() {
        File splashScreenFile;

        if (ConfigHandler.useRemoteSplashScreenJSON) {
            splashScreenFile = new File(ConfigHandler.configFolder, "MadHelpSplashScreen.json");
            try {
                BufferedInputStream is = new BufferedInputStream(new URL(ConfigHandler.remoteSplashScreenJSONURL).openStream());
                FileOutputStream os = new FileOutputStream(splashScreenFile);
                IOUtils.copy(is, os);
                is.close();
                os.close();
            }
            catch (MalformedURLException e) {
                LogHelper.error("Unable to download remote splash screen json [Invalid URL] - " + ConfigHandler.remoteListURL);
                status = "Unable to download remote splash screen json [Invalid URL] - URL: " + ConfigHandler.remoteListURL + " Exception: " + e.getLocalizedMessage();
                e.printStackTrace();
                return null;
            }
            catch (IOException e) {
                LogHelper.error("Unable to download remote splash screen json");
                status = "Unable to download remote splash screen json [IOException] - " + e.getLocalizedMessage();
                e.printStackTrace();
                return null;
            }
        }
        else {
            splashScreenFile = new File(ConfigHandler.configFolder, "MadHelpSplashScreen.json");
            if (!splashScreenFile.exists()) {
                LogHelper.error("Unable to load local splash screen json [File dose not exist] - " + splashScreenFile);
                status = "Unable to load local splash screen json [File dose not exist] - " + splashScreenFile;
                return null;
            }
        }
        return splashScreenFile;
    }

    private static void readContentFile(File contentFile) {
        compiledContentList.clear();
        try {
            JsonReader reader = new JsonReader(new FileReader(contentFile));
            reader.setLenient(true);
            reader.beginArray();

            while (reader.hasNext()) {
                DownloadableContent content = new DownloadableContent();
                reader.beginObject();

                while (reader.hasNext()) {
                    String nextField = reader.nextName();
                    if (nextField.equals("displayName")) {
                        content.displayName = reader.nextString();
                    }
                    else if (nextField.equals("fileName")) {
                        content.fileName = reader.nextString();
                    }
                    else if (nextField.equals("description")) {
                        content.description = reader.nextString();
                    }
                    else if (nextField.equals("info")) {
                        reader.beginArray();

                        while (reader.hasNext()) content.info.add(reader.nextString());

                        reader.endArray();
                    }
                    else if (nextField.equals("releaseNumber")) {
                        content.releaseNumber = reader.nextInt();
                    }
                    else if (nextField.equals("downloadURL")) {
                        content.downloadUrl = reader.nextString();
                    }
                    else if (nextField.equals("isRegional")) {
                        content.isRegionDownload = reader.nextBoolean();
                    }
                    else if (nextField.equals("regionInputPath")) {
                        content.regionInputPath = reader.nextString();
                    }
                    else if (nextField.equals("regionOutputPath")) {
                        content.regionOutputPath = reader.nextString();
                    }
                }

                reader.endObject();

                if (content.isValid()) compiledContentList.add(content);
                else {
                    status = "One ore more map entries were invalid and can not be displayed!";
                }
            }
            reader.endArray();
            reader.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            status = "File not found exception? That's odd... That should have been detected earlier... Oh well - " + e.getLocalizedMessage();
        }
        catch (IOException e) {
            e.printStackTrace();
            status = "IO Exception... Hmm... Nope i have absolutely no clue what happened there... - " + e.getLocalizedMessage();
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
            status = "There seems to be something wrong with MadHelpContentList.json. One or more entries were not readable - " + e.getLocalizedMessage();
        }
    }

    private static void readSplashInfo(File splashScreenFile) {
        try {
            List<String> list = new ArrayList<String>();
            int rev = 0;

            JsonReader reader = new JsonReader(new FileReader(splashScreenFile));
            reader.setLenient(true);
            reader.beginObject();

            while (reader.hasNext()) {
                String nextName = reader.nextName();
                if (nextName.equals("revision")) rev = reader.nextInt();
                else if (nextName.equals("infoText")) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        list.add(reader.nextString());
                    }
                    reader.endArray();
                }
            }

            reader.endObject();
            reader.close();
            splashScreen = new Pair<List<String>, Integer>(list, rev);
        }
        catch (FileNotFoundException e) {
            LogHelper.error("Encountered an error while reading splash screen json.");
            e.printStackTrace();
        }
        catch (IOException e) {
            LogHelper.error("Encountered an error while reading splash screen json");
            e.printStackTrace();
        }
        catch (IllegalStateException e) {
            LogHelper.error("Looks like the splash screen json is invalid - ");
            e.printStackTrace();
        }

    }

    private static File zipFile;
    public static File tempFolder;

    public static void unZipFile(DownloadableContent content) throws IOException {
        zipFile = new File(FileHandler.mcDirectory, "saves/" + content.fileName);
        tempFolder = new File(FileHandler.mcDirectory, "saves/.download-temp");
        if (tempFolder.exists()) FileUtils.deleteDirectory(tempFolder);

        unZipThread = new UnzipThread(zipFile, tempFolder);
        unZipThread.start();
    }

    public static File worldFolder;
    public static File saveFolder;

    public static String sortTheFolders() {
        File[] files = ContentHandler.tempFolder.listFiles(new FilterFolders());
        if (files == null || files.length == 0) return "Could not find a world folder in the unzipped file!";
        else if (files.length > 1)
            return "Found more then one folder in the unzipped file! @Mod Pack Maker: There should only be one folder in the zip file and that folder should be the world save folder";

        worldFolder = files[0];
        saveFolder = new File(FileHandler.mcDirectory, "saves");
        return null;
    }

    private static void addDefaultFiles() {
        File contentFile = new File(ConfigHandler.configFolder, "MadHelpContentList.json");
        File splashScreenFile = new File(ConfigHandler.configFolder, "MadHelpSplashScreen.json");

        if (!contentFile.exists()) {
            try {
                JsonWriter writer = new JsonWriter(new FileWriter(contentFile));
                writer.setIndent("	");
                writer.beginArray();

                writer.beginObject();
                writer.name("displayName").value("Example World");
                writer.name("fileName").value("ExampleWorld.zip");
                writer.name("description").value("This is an example of a world download entry");
                writer.name("info").beginArray().value("[c]This is an example map info page").value("Ok so this is just a page where the pack maker can dump a LOT of information about the map").value("Each individual string you add to the json file is turned into a new paragraph like this!. You can add " + SELECT + "0c" + SELECT + "1o" + SELECT + "2l" + SELECT + "3o" + SELECT + "4u" + SELECT + "5r" + SELECT + "6s" + SELECT + "7." + SELECT + "8." + SELECT + "9." + SELECT + "a." + SELECT + "b." + SELECT + "c." + SELECT + "d." + SELECT + "e." + SELECT + "f." + SELECT + "r And " + SELECT + "l" + SELECT + "n" + SELECT + "oFormatting!" + SELECT + "r " + SELECT + "k\\\"Secret Text!\\\"\"").value("To add formatting you simply add the selector symbol \"" + SELECT + "\" then any of the standard minecraft formatting codes").value("[c]You can also center a paragraph by adding \"[c]\" to the start of it (before any formatting)");
                writer.endArray();
                writer.name("releaseNumber").value("0");
                writer.name("downloadURL").value("www.somedirectdownload.com/map.zip");
                writer.name("isRegional").value(false);
                writer.name("regionInputPath").value("");
                writer.name("regionOutputPath").value("");
                writer.endObject();

                writer.beginObject();
                writer.name("displayName").value("Another Example World");
                writer.name("fileName").value("ExampleWorld.zip");
                writer.name("description").value("This is an example of a world download entry");
                writer.name("info").beginArray().value("[c]Example info");
                writer.endArray();
                writer.name("releaseNumber").value("0");
                writer.name("downloadURL").value("www.somedirectdownload.com/map.zip");
                writer.name("isRegional").value(true);
                writer.name("regionInputPath").value("region");
                writer.name("regionOutputPath").value("region");
                writer.endObject();

                writer.endArray();
                writer.close();
            }
            catch (IOException e) {
                LogHelper.error("Something went wrong while creating default content file JSON");
                e.printStackTrace();
            }
        }

        if (!splashScreenFile.exists() && ConfigHandler.enableSplashScreen) {
            try {
                JsonWriter writer = new JsonWriter(new FileWriter(splashScreenFile));
                writer.setIndent("	");
                writer.beginObject();
                writer.name("revision").value(0);
                writer.name("infoText").beginArray().value("[c]This is an example splash screen").value("If you are seeing this then ether MadHelp has just been loaded for the first time or the splash screen has not been properly configured by the pack creator.").value("This screen uses the same formatting options as the map info screens.");
                writer.endArray();
                writer.endObject();
                writer.close();
            }
            catch (IOException e) {
                LogHelper.error("Something went wrong while creating default content file JSON");
                e.printStackTrace();
            }
        }

    }

    public static class DownloadableContent {
        public String displayName;
        public String fileName;
        public String description;
        public List<String> info = new ArrayList<String>();
        public int releaseNumber;
        public String downloadUrl;
        public String websiteUrl;
        public boolean isRegionDownload = false;
        public String regionInputPath;
        public String regionOutputPath;

        public boolean isValid() {
            if (StringUtils.isNullOrEmpty(displayName) || StringUtils.isNullOrEmpty(fileName) || description == null || StringUtils.isNullOrEmpty(downloadUrl) || (isRegionDownload && StringUtils.isNullOrEmpty(regionInputPath) && StringUtils.isNullOrEmpty(regionOutputPath)))
                return false;
            return true;
        }

    }

    public static class DownloadThread extends Thread {

        private boolean finished = false;
        private boolean wasSuccessful = false;
        private String faliureMessage;
        private DownloadableContent content;
        private File downloadedFile;
        private long downloadSize = 0;
        private InputStream downloadIS;
        private boolean wasCanceled = false;

        public DownloadThread(DownloadableContent content) {
            super("MapDownload");
            this.content = content;
        }

        @Override
        public void run() {
            downloadedFile = new File(FileHandler.mcDirectory, "saves/" + content.fileName + ".temp");
            if (downloadedFile.exists()) downloadedFile.delete();
            File finalFile = new File(FileHandler.mcDirectory, "saves/" + content.fileName);
            if (finalFile.exists() && !finalFile.delete()) {
                faliureMessage = "Failed to delete existing file! " + finalFile;
                return;
            }


            try {
                URL downloadURL = new URL(content.downloadUrl);
                URLConnection connection = downloadURL.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                downloadSize = connection.getContentLengthLong();
                connection.setConnectTimeout(120000);
                connection.setReadTimeout(120000);
                downloadIS = connection.getInputStream();

                FileUtils.copyInputStreamToFile(downloadIS, downloadedFile);
                IOUtils.closeQuietly(downloadIS);

                if (!downloadedFile.renameTo(finalFile)) {
                    FileUtils.moveFile(downloadedFile, finalFile);
                }
            }
            catch (Exception e) {
                if (wasCanceled) {
                    faliureMessage = "Download was canceled by user.";
                }
                else {
                    LogHelper.info("An error occurred while downloading the map");
                    e.printStackTrace();
                    faliureMessage = "Download failed with the following exception: " + e.getLocalizedMessage();
                }
                finished = true;
                downloadedFile.delete();
                return;
            }

            IOUtils.closeQuietly(downloadIS);

            wasSuccessful = true;
            finished = true;

        }

        public boolean isFinished() {
            return finished;
        }

        public boolean wasSuccessful() {
            return wasSuccessful;
        }

        public long getDownloadSize() {
            return downloadSize;
        }

        public long getDownloaded() {
            return downloadedFile.length();
        }

        public DownloadableContent getContent() {
            return content;
        }

        public void cancelDownload() {
            if (downloadIS != null && !wasCanceled) try {
                downloadIS.close();
                wasCanceled = true;
            }
            catch (IOException e) {
                LogHelper.error("Error canceling download");
                e.printStackTrace();
            }
        }

        public String getFaliureMessage() {
            return faliureMessage;
        }
    }

    public static class UnzipThread extends Thread {
        private final File zipFile;
        private final File destination;
        public String error = null;
        public volatile boolean finished = false;
        public volatile int size = 0;
        public volatile int processed = 0;

        public UnzipThread(File zipFile, File destination) {
            super("MadHelp-World-Un-zipper");
            this.zipFile = zipFile;
            this.destination = destination;
        }

        @Override
        public void run() {
            BufferedInputStream is = null;
            FileOutputStream fos = null;
            BufferedOutputStream dest = null;
            try {

                ZipFile zip = new ZipFile(zipFile);

                destination.mkdir();
                Enumeration zipFileEntries = zip.entries();
                size = zip.size();

                while (zipFileEntries.hasMoreElements()) {
                    ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                    String currentEntry = entry.getName();
                    File destFile = new File(destination, currentEntry);
                    File destinationParent = destFile.getParentFile();
                    destinationParent.mkdirs();

                    if (!entry.isDirectory()) {
                        is = new BufferedInputStream(zip.getInputStream(entry));
                        int currentByte;
                        byte data[] = new byte[2048];
                        fos = new FileOutputStream(destFile);
                        dest = new BufferedOutputStream(fos, 2048);
                        while ((currentByte = is.read(data, 0, 2048)) != -1) {
                            dest.write(data, 0, currentByte);
                        }
                        dest.flush();
                        dest.close();
                        is.close();
                    }
                    processed = processed + 1;
                }
                finished = true;

            }
            catch (Throwable e) {
                if (is != null) IOUtils.closeQuietly(is);
                if (fos != null) IOUtils.closeQuietly(fos);
                if (dest != null) IOUtils.closeQuietly(dest);

                finished = true;
                error = e.getMessage();
                e.printStackTrace();
            }
        }
    }

    public static class FilterFolders implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    }

    public static class Pair<k, v> {
        public k key;
        public v value;

        public Pair(k key, v value) {
            this.key = key;
            this.value = value;
        }

        public k getKey() {
            return key;
        }

        public v getValue() {
            return value;
        }
    }
}
