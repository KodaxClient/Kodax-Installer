package me.kodingking.installer.slide.impl;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import mdlaf.resources.MaterialColors;
import me.kodingking.installer.InstallerMain;
import me.kodingking.installer.Manifest;
import me.kodingking.installer.Settings;
import me.kodingking.installer.slide.AbstractSlide;
import me.kodingking.installer.utils.Multithreading;
import me.kodingking.installer.utils.OSValidator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class InstallingSlide extends AbstractSlide {

  private JLabel infoLabel = new JLabel("Starting install...");

  @Override
  public void initialize(JFrame frame) {
    JProgressBar progressBar = new JProgressBar(1, 6 + (Settings.USE_OPTIFINE ? 2 : 0));
    progressBar.setForeground(MaterialColors.RED_400);
    progressBar.setBorderPainted(false);
    progressBar.setBorder(null);
    progressBar.setBounds(InstallerMain.width / 2 - 150 / 2, 100, 150, 20);
    progressBar.setVisible(true);

    frame.add(progressBar);

    infoLabel = new JLabel("Starting install...", JLabel.CENTER);
    infoLabel.setBounds(0, 140, InstallerMain.width, 20);
    frame.add(infoLabel);

    Multithreading.run(() -> {
      int progressValue = 0;

      infoLabel.setText("Fetching manifest...");
      progressBar.setValue(++progressValue);

      Manifest manifest = Manifest
          .fetch("https://raw.githubusercontent.com/KodaxClient/Kodax-Repo/master/installer.json");
      if (manifest == null) {
        infoLabel.setText("Error fetching manifest.");
        return;
      }
      File mcDir = getMCDir();
      if (!new File(mcDir, "versions" + File.separator + "1.8.9").exists()) {
        infoLabel.setText("Version 1.8.9 not found. Please run it once.");
        return;
      }

      File baseVersionFolder = new File(mcDir, "versions" + File.separator + "1.8.9");
      File baseVersionJar = new File(baseVersionFolder, "1.8.9.jar");

      File libraryDir = new File(mcDir,
          "libraries" + File.separator + "me" + File.separator + "kodingking" + File.separator
              + "Kodax" + File.separator + manifest.getLatestBuild());
      if (!libraryDir.exists()) {
        libraryDir.mkdirs();
      }
      File libraryFile = new File(libraryDir, "Kodax-" + manifest.getLatestBuild() + ".jar");
      infoLabel.setText("Downloading Kodax" + (Settings.USE_BETA ? " (BETA)" : "") + "...");
      progressBar.setValue(++progressValue);

      try {
        URL urlToDownload = manifest.getBuildDownload();
        if (Settings.USE_BETA) {
          StringBuilder result = new StringBuilder();
          URL url = new URL("https://api.github.com/repos/KodaxClient/Kodax/releases/latest");
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("GET");
          BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
          String line;
          while ((line = rd.readLine()) != null) {
            result.append(line);
          }
          rd.close();

          JsonObject downloadedObj = new JsonParser().parse(result.toString()).getAsJsonObject();
          if (downloadedObj.has("assets") && downloadedObj.getAsJsonArray("assets").size() > 0) {
            urlToDownload = new URL(
                downloadedObj.getAsJsonArray("assets").get(0).getAsJsonObject()
                    .get("browser_download_url")
                    .getAsString());
          }
        }

        System.out.println("Downloading from: " + urlToDownload);

        FileUtils.copyURLToFile(urlToDownload, libraryFile);
      } catch (IOException e) {
        e.printStackTrace();
        infoLabel.setText("Error downloading Kodax.");
        return;
      }

      File launchwrapperDir = new File(mcDir,
          "libraries" + File.separator + "net" + File.separator + "minecraft" + File.separator
              + "launchwrapper" + File.separator + "1.7");
      if (!launchwrapperDir.exists()) {
        launchwrapperDir.mkdirs();
      }
      File launchwrapperFile = new File(launchwrapperDir, "launchwrapper-1.7.jar");

      try {
        infoLabel.setText("Downloading launchwrapper...");
        progressBar.setValue(++progressValue);
        FileUtils.copyURLToFile(new URL(
                "https://libraries.minecraft.net/net/minecraft/launchwrapper/1.7/launchwrapper-1.7.jar"),
            launchwrapperFile);
      } catch (IOException e) {
        e.printStackTrace();
        infoLabel.setText("Error downloading launchwrapper.");
        return;
      }

      if (Settings.USE_OPTIFINE) {
        try {
          File optifineFile = File.createTempFile("Optifine", "jar");
          infoLabel.setText("Downloading Optifine...");
          progressBar.setValue(++progressValue);
          FileUtils.copyURLToFile(new URL(
                  "https://github.com/KodaxClient/Kodax-Repo/raw/master/resources/installer/OptiFine_1.8.9_HD_U_I7.jar"),
              optifineFile);

          File optifineLibDir = new File(mcDir,
              "libraries" + File.separator + "optifine" + File.separator + "OptiFine"
                  + File.separator + "1.8.9_HD_U_I7");
          if (!optifineLibDir.exists()) {
            optifineLibDir.mkdirs();
          }
          File optifineLibJar = new File(optifineLibDir, "OptiFine-1.8.9_HD_U_I7.jar");

          infoLabel.setText("Patching Optifine...");
          progressBar.setValue(++progressValue);
          URLClassLoader child = new URLClassLoader(new URL[]{optifineFile.toURL()},
              this.getClass().getClassLoader());
          Class classToLoad = Class.forName("optifine.Patcher", true, child);
          Method method = classToLoad.getDeclaredMethod("main", String[].class);
          method.invoke(null, new Object[]{
              new String[]{baseVersionJar.getAbsolutePath(), optifineFile.getAbsolutePath(),
                  optifineLibJar.getAbsolutePath()}});
        } catch (Exception e) {
          e.printStackTrace();
          infoLabel.setText("Error installing Optifine");
          return;
        }
      }

      String profileName = "Kodax " + (Settings.USE_BETA ? "BETA" : manifest.getLatestBuild());
      File versionDir = new File(mcDir,
          "versions" + File.separator + profileName);
      if (!versionDir.exists()) {
        versionDir.mkdirs();
      }

      infoLabel.setText("Creating version.json...");
      progressBar.setValue(++progressValue);

      String json = "{\"assetIndex\":{\"id\":\"1.8\",\"sha1\":\"e264980ad255aad2174cbe4d674c102474ae5202\",\"size\":94650,\"url\":\"https://launchermeta.mojang.com/mc/assets/1.8/e264980ad255aad2174cbe4d674c102474ae5202/1.8.json\",\"totalSize\":114708537},\"assets\":\"1.8\",\"downloads\":{\"client\":{\"sha1\":\"3870888a6c3d349d3771a3e9d16c9bf5e076b908\",\"size\":8461484,\"url\":\"https://launcher.mojang.com/mc/game/1.8.9/client/3870888a6c3d349d3771a3e9d16c9bf5e076b908/client.jar\"},\"server\":{\"sha1\":\"b58b2ceb36e01bcd8dbf49c8fb66c55a9f0676cd\",\"size\":8320755,\"url\":\"https://launcher.mojang.com/mc/game/1.8.9/server/b58b2ceb36e01bcd8dbf49c8fb66c55a9f0676cd/server.jar\"},\"windows_server\":{\"sha1\":\"5143618265b8a2d1d28bcadf206b7327738c2670\",\"size\":8714995,\"url\":\"https://launcher.mojang.com/mc/game/1.8.9/windows_server/5143618265b8a2d1d28bcadf206b7327738c2670/windows_server.exe\"}},\"id\":\"%FOLDER_NAME%\",\"libraries\":[{\"name\":\"com.mojang:netty:1.6\",\"downloads\":{\"artifact\":{\"size\":7877,\"sha1\":\"4b75825a06139752bd800d9e29c5fd55b8b1b1e4\",\"path\":\"com/mojang/netty/1.6/netty-1.6.jar\",\"url\":\"https://libraries.minecraft.net/com/mojang/netty/1.6/netty-1.6.jar\"}}},{\"name\":\"oshi-project:oshi-core:1.1\",\"downloads\":{\"artifact\":{\"size\":30973,\"sha1\":\"9ddf7b048a8d701be231c0f4f95fd986198fd2d8\",\"path\":\"oshi-project/oshi-core/1.1/oshi-core-1.1.jar\",\"url\":\"https://libraries.minecraft.net/oshi-project/oshi-core/1.1/oshi-core-1.1.jar\"}}},{\"name\":\"net.java.dev.jna:jna:3.4.0\",\"downloads\":{\"artifact\":{\"size\":1008730,\"sha1\":\"803ff252fedbd395baffd43b37341dc4a150a554\",\"path\":\"net/java/dev/jna/jna/3.4.0/jna-3.4.0.jar\",\"url\":\"https://libraries.minecraft.net/net/java/dev/jna/jna/3.4.0/jna-3.4.0.jar\"}}},{\"name\":\"net.java.dev.jna:platform:3.4.0\",\"downloads\":{\"artifact\":{\"size\":913436,\"sha1\":\"e3f70017be8100d3d6923f50b3d2ee17714e9c13\",\"path\":\"net/java/dev/jna/platform/3.4.0/platform-3.4.0.jar\",\"url\":\"https://libraries.minecraft.net/net/java/dev/jna/platform/3.4.0/platform-3.4.0.jar\"}}},{\"name\":\"com.ibm.icu:icu4j-core-mojang:51.2\",\"downloads\":{\"artifact\":{\"size\":1634692,\"sha1\":\"63d216a9311cca6be337c1e458e587f99d382b84\",\"path\":\"com/ibm/icu/icu4j-core-mojang/51.2/icu4j-core-mojang-51.2.jar\",\"url\":\"https://libraries.minecraft.net/com/ibm/icu/icu4j-core-mojang/51.2/icu4j-core-mojang-51.2.jar\"}}},{\"name\":\"net.sf.jopt-simple:jopt-simple:4.6\",\"downloads\":{\"artifact\":{\"size\":62477,\"sha1\":\"306816fb57cf94f108a43c95731b08934dcae15c\",\"path\":\"net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar\",\"url\":\"https://libraries.minecraft.net/net/sf/jopt-simple/jopt-simple/4.6/jopt-simple-4.6.jar\"}}},{\"name\":\"com.paulscode:codecjorbis:20101023\",\"downloads\":{\"artifact\":{\"size\":103871,\"sha1\":\"c73b5636faf089d9f00e8732a829577de25237ee\",\"path\":\"com/paulscode/codecjorbis/20101023/codecjorbis-20101023.jar\",\"url\":\"https://libraries.minecraft.net/com/paulscode/codecjorbis/20101023/codecjorbis-20101023.jar\"}}},{\"name\":\"com.paulscode:codecwav:20101023\",\"downloads\":{\"artifact\":{\"size\":5618,\"sha1\":\"12f031cfe88fef5c1dd36c563c0a3a69bd7261da\",\"path\":\"com/paulscode/codecwav/20101023/codecwav-20101023.jar\",\"url\":\"https://libraries.minecraft.net/com/paulscode/codecwav/20101023/codecwav-20101023.jar\"}}},{\"name\":\"com.paulscode:libraryjavasound:20101123\",\"downloads\":{\"artifact\":{\"size\":21679,\"sha1\":\"5c5e304366f75f9eaa2e8cca546a1fb6109348b3\",\"path\":\"com/paulscode/libraryjavasound/20101123/libraryjavasound-20101123.jar\",\"url\":\"https://libraries.minecraft.net/com/paulscode/libraryjavasound/20101123/libraryjavasound-20101123.jar\"}}},{\"name\":\"com.paulscode:librarylwjglopenal:20100824\",\"downloads\":{\"artifact\":{\"size\":18981,\"sha1\":\"73e80d0794c39665aec3f62eee88ca91676674ef\",\"path\":\"com/paulscode/librarylwjglopenal/20100824/librarylwjglopenal-20100824.jar\",\"url\":\"https://libraries.minecraft.net/com/paulscode/librarylwjglopenal/20100824/librarylwjglopenal-20100824.jar\"}}},{\"name\":\"com.paulscode:soundsystem:20120107\",\"downloads\":{\"artifact\":{\"size\":65020,\"sha1\":\"419c05fe9be71f792b2d76cfc9b67f1ed0fec7f6\",\"path\":\"com/paulscode/soundsystem/20120107/soundsystem-20120107.jar\",\"url\":\"https://libraries.minecraft.net/com/paulscode/soundsystem/20120107/soundsystem-20120107.jar\"}}},{\"name\":\"io.netty:netty-all:4.0.23.Final\",\"downloads\":{\"artifact\":{\"size\":1779991,\"sha1\":\"0294104aaf1781d6a56a07d561e792c5d0c95f45\",\"path\":\"io/netty/netty-all/4.0.23.Final/netty-all-4.0.23.Final.jar\",\"url\":\"https://libraries.minecraft.net/io/netty/netty-all/4.0.23.Final/netty-all-4.0.23.Final.jar\"}}},{\"name\":\"com.google.guava:guava:17.0\",\"downloads\":{\"artifact\":{\"size\":2243036,\"sha1\":\"9c6ef172e8de35fd8d4d8783e4821e57cdef7445\",\"path\":\"com/google/guava/guava/17.0/guava-17.0.jar\",\"url\":\"https://libraries.minecraft.net/com/google/guava/guava/17.0/guava-17.0.jar\"}}},{\"name\":\"org.apache.commons:commons-lang3:3.3.2\",\"downloads\":{\"artifact\":{\"size\":412739,\"sha1\":\"90a3822c38ec8c996e84c16a3477ef632cbc87a3\",\"path\":\"org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar\",\"url\":\"https://libraries.minecraft.net/org/apache/commons/commons-lang3/3.3.2/commons-lang3-3.3.2.jar\"}}},{\"name\":\"commons-io:commons-io:2.4\",\"downloads\":{\"artifact\":{\"size\":185140,\"sha1\":\"b1b6ea3b7e4aa4f492509a4952029cd8e48019ad\",\"path\":\"commons-io/commons-io/2.4/commons-io-2.4.jar\",\"url\":\"https://libraries.minecraft.net/commons-io/commons-io/2.4/commons-io-2.4.jar\"}}},{\"name\":\"commons-codec:commons-codec:1.9\",\"downloads\":{\"artifact\":{\"size\":263965,\"sha1\":\"9ce04e34240f674bc72680f8b843b1457383161a\",\"path\":\"commons-codec/commons-codec/1.9/commons-codec-1.9.jar\",\"url\":\"https://libraries.minecraft.net/commons-codec/commons-codec/1.9/commons-codec-1.9.jar\"}}},{\"name\":\"net.java.jinput:jinput:2.0.5\",\"downloads\":{\"artifact\":{\"size\":208338,\"sha1\":\"39c7796b469a600f72380316f6b1f11db6c2c7c4\",\"path\":\"net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar\",\"url\":\"https://libraries.minecraft.net/net/java/jinput/jinput/2.0.5/jinput-2.0.5.jar\"}}},{\"name\":\"net.java.jutils:jutils:1.0.0\",\"downloads\":{\"artifact\":{\"size\":7508,\"sha1\":\"e12fe1fda814bd348c1579329c86943d2cd3c6a6\",\"path\":\"net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar\",\"url\":\"https://libraries.minecraft.net/net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar\"}}},{\"name\":\"com.google.code.gson:gson:2.2.4\",\"downloads\":{\"artifact\":{\"size\":190432,\"sha1\":\"a60a5e993c98c864010053cb901b7eab25306568\",\"path\":\"com/google/code/gson/gson/2.2.4/gson-2.2.4.jar\",\"url\":\"https://libraries.minecraft.net/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar\"}}},{\"name\":\"com.mojang:authlib:1.5.21\",\"downloads\":{\"artifact\":{\"size\":64412,\"sha1\":\"aefba0d5b53fbcb70860bc8046ab95d5854c07a5\",\"path\":\"com/mojang/authlib/1.5.21/authlib-1.5.21.jar\",\"url\":\"https://libraries.minecraft.net/com/mojang/authlib/1.5.21/authlib-1.5.21.jar\"}}},{\"name\":\"com.mojang:realms:1.7.59\",\"downloads\":{\"artifact\":{\"size\":1198123,\"sha1\":\"9c6c59b742d8e038a15f64c1aa273a893a658424\",\"path\":\"com/mojang/realms/1.7.59/realms-1.7.59.jar\",\"url\":\"https://libraries.minecraft.net/com/mojang/realms/1.7.59/realms-1.7.59.jar\"}}},{\"name\":\"org.apache.commons:commons-compress:1.8.1\",\"downloads\":{\"artifact\":{\"size\":365552,\"sha1\":\"a698750c16740fd5b3871425f4cb3bbaa87f529d\",\"path\":\"org/apache/commons/commons-compress/1.8.1/commons-compress-1.8.1.jar\",\"url\":\"https://libraries.minecraft.net/org/apache/commons/commons-compress/1.8.1/commons-compress-1.8.1.jar\"}}},{\"name\":\"org.apache.httpcomponents:httpclient:4.3.3\",\"downloads\":{\"artifact\":{\"size\":589512,\"sha1\":\"18f4247ff4572a074444572cee34647c43e7c9c7\",\"path\":\"org/apache/httpcomponents/httpclient/4.3.3/httpclient-4.3.3.jar\",\"url\":\"https://libraries.minecraft.net/org/apache/httpcomponents/httpclient/4.3.3/httpclient-4.3.3.jar\"}}},{\"name\":\"commons-logging:commons-logging:1.1.3\",\"downloads\":{\"artifact\":{\"size\":62050,\"sha1\":\"f6f66e966c70a83ffbdb6f17a0919eaf7c8aca7f\",\"path\":\"commons-logging/commons-logging/1.1.3/commons-logging-1.1.3.jar\",\"url\":\"https://libraries.minecraft.net/commons-logging/commons-logging/1.1.3/commons-logging-1.1.3.jar\"}}},{\"name\":\"org.apache.httpcomponents:httpcore:4.3.2\",\"downloads\":{\"artifact\":{\"size\":282269,\"sha1\":\"31fbbff1ddbf98f3aa7377c94d33b0447c646b6e\",\"path\":\"org/apache/httpcomponents/httpcore/4.3.2/httpcore-4.3.2.jar\",\"url\":\"https://libraries.minecraft.net/org/apache/httpcomponents/httpcore/4.3.2/httpcore-4.3.2.jar\"}}},{\"name\":\"org.apache.logging.log4j:log4j-api:2.0-beta9\",\"downloads\":{\"artifact\":{\"size\":108161,\"sha1\":\"1dd66e68cccd907880229f9e2de1314bd13ff785\",\"path\":\"org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar\",\"url\":\"https://libraries.minecraft.net/org/apache/logging/log4j/log4j-api/2.0-beta9/log4j-api-2.0-beta9.jar\"}}},{\"name\":\"org.apache.logging.log4j:log4j-core:2.0-beta9\",\"downloads\":{\"artifact\":{\"size\":681134,\"sha1\":\"678861ba1b2e1fccb594bb0ca03114bb05da9695\",\"path\":\"org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar\",\"url\":\"https://libraries.minecraft.net/org/apache/logging/log4j/log4j-core/2.0-beta9/log4j-core-2.0-beta9.jar\"}}},{\"name\":\"org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209\",\"rules\":[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"osx\"}}],\"downloads\":{\"artifact\":{\"size\":1047168,\"sha1\":\"697517568c68e78ae0b4544145af031c81082dfe\",\"path\":\"org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl/2.9.4-nightly-20150209/lwjgl-2.9.4-nightly-20150209.jar\"}}},{\"name\":\"org.lwjgl.lwjgl:lwjgl_util:2.9.4-nightly-20150209\",\"rules\":[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"osx\"}}],\"downloads\":{\"artifact\":{\"size\":173887,\"sha1\":\"d51a7c040a721d13efdfbd34f8b257b2df882ad0\",\"path\":\"org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl_util/2.9.4-nightly-20150209/lwjgl_util-2.9.4-nightly-20150209.jar\"}}},{\"extract\":{\"exclude\":[\"META-INF/\"]},\"name\":\"org.lwjgl.lwjgl:lwjgl-platform:2.9.4-nightly-20150209\",\"natives\":{\"linux\":\"natives-linux\",\"osx\":\"natives-osx\",\"windows\":\"natives-windows\"},\"rules\":[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"osx\"}}],\"downloads\":{\"classifiers\":{\"natives-linux\":{\"size\":578680,\"sha1\":\"931074f46c795d2f7b30ed6395df5715cfd7675b\",\"path\":\"org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-linux.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-linux.jar\"},\"natives-osx\":{\"size\":426822,\"sha1\":\"bcab850f8f487c3f4c4dbabde778bb82bd1a40ed\",\"path\":\"org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-osx.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-osx.jar\"},\"natives-windows\":{\"size\":613748,\"sha1\":\"b84d5102b9dbfabfeb5e43c7e2828d98a7fc80e0\",\"path\":\"org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-windows.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209-natives-windows.jar\"}},\"artifact\":{\"size\":22,\"sha1\":\"b04f3ee8f5e43fa3b162981b50bb72fe1acabb33\",\"path\":\"org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.4-nightly-20150209/lwjgl-platform-2.9.4-nightly-20150209.jar\"}}},{\"name\":\"org.lwjgl.lwjgl:lwjgl:2.9.2-nightly-20140822\",\"rules\":[{\"action\":\"allow\",\"os\":{\"name\":\"osx\"}}],\"downloads\":{\"artifact\":{\"size\":1045632,\"sha1\":\"7707204c9ffa5d91662de95f0a224e2f721b22af\",\"path\":\"org/lwjgl/lwjgl/lwjgl/2.9.2-nightly-20140822/lwjgl-2.9.2-nightly-20140822.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl/2.9.2-nightly-20140822/lwjgl-2.9.2-nightly-20140822.jar\"}}},{\"name\":\"org.lwjgl.lwjgl:lwjgl_util:2.9.2-nightly-20140822\",\"rules\":[{\"action\":\"allow\",\"os\":{\"name\":\"osx\"}}],\"downloads\":{\"artifact\":{\"size\":173887,\"sha1\":\"f0e612c840a7639c1f77f68d72a28dae2f0c8490\",\"path\":\"org/lwjgl/lwjgl/lwjgl_util/2.9.2-nightly-20140822/lwjgl_util-2.9.2-nightly-20140822.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl_util/2.9.2-nightly-20140822/lwjgl_util-2.9.2-nightly-20140822.jar\"}}},{\"extract\":{\"exclude\":[\"META-INF/\"]},\"name\":\"org.lwjgl.lwjgl:lwjgl-platform:2.9.2-nightly-20140822\",\"natives\":{\"linux\":\"natives-linux\",\"osx\":\"natives-osx\",\"windows\":\"natives-windows\"},\"rules\":[{\"action\":\"allow\",\"os\":{\"name\":\"osx\"}}],\"downloads\":{\"classifiers\":{\"natives-linux\":{\"size\":578539,\"sha1\":\"d898a33b5d0a6ef3fed3a4ead506566dce6720a5\",\"path\":\"org/lwjgl/lwjgl/lwjgl-platform/2.9.2-nightly-20140822/lwjgl-platform-2.9.2-nightly-20140822-natives-linux.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.2-nightly-20140822/lwjgl-platform-2.9.2-nightly-20140822-natives-linux.jar\"},\"natives-osx\":{\"size\":468116,\"sha1\":\"79f5ce2fea02e77fe47a3c745219167a542121d7\",\"path\":\"org/lwjgl/lwjgl/lwjgl-platform/2.9.2-nightly-20140822/lwjgl-platform-2.9.2-nightly-20140822-natives-osx.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.2-nightly-20140822/lwjgl-platform-2.9.2-nightly-20140822-natives-osx.jar\"},\"natives-windows\":{\"size\":613680,\"sha1\":\"78b2a55ce4dc29c6b3ec4df8ca165eba05f9b341\",\"path\":\"org/lwjgl/lwjgl/lwjgl-platform/2.9.2-nightly-20140822/lwjgl-platform-2.9.2-nightly-20140822-natives-windows.jar\",\"url\":\"https://libraries.minecraft.net/org/lwjgl/lwjgl/lwjgl-platform/2.9.2-nightly-20140822/lwjgl-platform-2.9.2-nightly-20140822-natives-windows.jar\"}}}},{\"extract\":{\"exclude\":[\"META-INF/\"]},\"name\":\"net.java.jinput:jinput-platform:2.0.5\",\"natives\":{\"linux\":\"natives-linux\",\"osx\":\"natives-osx\",\"windows\":\"natives-windows\"},\"downloads\":{\"classifiers\":{\"natives-linux\":{\"size\":10362,\"sha1\":\"7ff832a6eb9ab6a767f1ade2b548092d0fa64795\",\"path\":\"net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-linux.jar\",\"url\":\"https://libraries.minecraft.net/net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-linux.jar\"},\"natives-osx\":{\"size\":12186,\"sha1\":\"53f9c919f34d2ca9de8c51fc4e1e8282029a9232\",\"path\":\"net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-osx.jar\",\"url\":\"https://libraries.minecraft.net/net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-osx.jar\"},\"natives-windows\":{\"size\":155179,\"sha1\":\"385ee093e01f587f30ee1c8a2ee7d408fd732e16\",\"path\":\"net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-windows.jar\",\"url\":\"https://libraries.minecraft.net/net/java/jinput/jinput-platform/2.0.5/jinput-platform-2.0.5-natives-windows.jar\"}}}},{\"name\":\"tv.twitch:twitch:6.5\",\"downloads\":{\"artifact\":{\"size\":55977,\"sha1\":\"320a2dfd18513a5f41b4e75729df684488cbd925\",\"path\":\"tv/twitch/twitch/6.5/twitch-6.5.jar\",\"url\":\"https://libraries.minecraft.net/tv/twitch/twitch/6.5/twitch-6.5.jar\"}}},{\"name\":\"%MAVEN_PATH%\"},{\"name\":\"net.minecraft:launchwrapper:1.7\"}%APPEND_HERE%,{\"extract\":{\"exclude\":[\"META-INF/\"]},\"name\":\"tv.twitch:twitch-platform:6.5\",\"natives\":{\"linux\":\"natives-linux\",\"osx\":\"natives-osx\",\"windows\":\"natives-windows-${arch}\"},\"rules\":[{\"action\":\"allow\"},{\"action\":\"disallow\",\"os\":{\"name\":\"linux\"}}],\"downloads\":{\"classifiers\":{\"natives-osx\":{\"size\":455359,\"sha1\":\"5f9d1ee26257b3a33f0ca06fed335ef462af659f\",\"path\":\"tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-osx.jar\",\"url\":\"https://libraries.minecraft.net/tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-osx.jar\"},\"natives-windows-32\":{\"size\":474225,\"sha1\":\"206c4ccaecdbcfd2a1631150c69a97bbc9c20c11\",\"path\":\"tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-32.jar\",\"url\":\"https://libraries.minecraft.net/tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-32.jar\"},\"natives-windows-64\":{\"size\":580098,\"sha1\":\"9fdd0fd5aed0817063dcf95b69349a171f447ebd\",\"path\":\"tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-64.jar\",\"url\":\"https://libraries.minecraft.net/tv/twitch/twitch-platform/6.5/twitch-platform-6.5-natives-windows-64.jar\"}}}},{\"extract\":{\"exclude\":[\"META-INF/\"]},\"name\":\"tv.twitch:twitch-external-platform:4.5\",\"natives\":{\"windows\":\"natives-windows-${arch}\"},\"rules\":[{\"action\":\"allow\",\"os\":{\"name\":\"windows\"}}],\"downloads\":{\"classifiers\":{\"natives-windows-32\":{\"size\":5654047,\"sha1\":\"18215140f010c05b9f86ef6f0f8871954d2ccebf\",\"path\":\"tv/twitch/twitch-external-platform/4.5/twitch-external-platform-4.5-natives-windows-32.jar\",\"url\":\"https://libraries.minecraft.net/tv/twitch/twitch-external-platform/4.5/twitch-external-platform-4.5-natives-windows-32.jar\"},\"natives-windows-64\":{\"size\":7457619,\"sha1\":\"c3cde57891b935d41b6680a9c5e1502eeab76d86\",\"path\":\"tv/twitch/twitch-external-platform/4.5/twitch-external-platform-4.5-natives-windows-64.jar\",\"url\":\"https://libraries.minecraft.net/tv/twitch/twitch-external-platform/4.5/twitch-external-platform-4.5-natives-windows-64.jar\"}}}}],\"logging\":{\"client\":{\"file\":{\"id\":\"client-1.7.xml\",\"sha1\":\"6605d632a2399010c0085d3e4da58974d62ccdfe\",\"size\":871,\"url\":\"https://launchermeta.mojang.com/mc/log_configs/client-1.7.xml/6605d632a2399010c0085d3e4da58974d62ccdfe/client-1.7.xml\"},\"argument\":\"-Dlog4j.configurationFile=${path}\",\"type\":\"log4j2-xml\"}},\"mainClass\":\"net.minecraft.launchwrapper.Launch\",\"minecraftArguments\":\"--username ${auth_player_name} --version ${version_name} --gameDir ${game_directory} --assetsDir ${assets_root} --assetIndex ${assets_index_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userProperties ${user_properties} --userType ${user_type} --tweakClass %TWEAK_CLASS%\",\"minimumLauncherVersion\":14,\"releaseTime\":\"2015-12-03T09:24:39+00:00\",\"time\":\"2016-06-01T11:45:48+00:00\",\"type\":\"release\"}";
      json = json
          .replace("%FOLDER_NAME%", profileName)
          .replace("%MAVEN_PATH%", "me.kodingking:Kodax:" + manifest.getLatestBuild())
          .replace("%TWEAK_CLASS%", "me.kodingking.kodax.injection.LaunchInjector")
          .replace("%APPEND_HERE%",
              Settings.USE_OPTIFINE ? ",{\"name\": \"optifine:OptiFine:1.8.9_HD_U_I7\"}" : "");
      try {
        FileUtils.write(new File(versionDir, profileName + ".json"), json);
      } catch (IOException e) {
        e.printStackTrace();
        infoLabel.setText("Could not write to version.json.");
        return;
      }

      try {
        infoLabel.setText("Creating launcher profile...");
        progressBar.setValue(++progressValue);

        InputStream iconInputStream = InstallerMain.class.getResourceAsStream("/icon/square_logo_with_bg.png");

        JsonObject profileToAdd = new JsonObject();
        profileToAdd.add("name", new JsonPrimitive("Kodax"));
        profileToAdd.add("type", new JsonPrimitive("custom"));
        profileToAdd.add("created", new JsonPrimitive("1970-01-01T00:00:00.000Z"));
        profileToAdd.add("lastUsed", new JsonPrimitive("1970-01-01T00:00:00.000Z"));
        profileToAdd.add("lastVersionId", new JsonPrimitive(profileName));
        profileToAdd.add("icon", new JsonPrimitive("data:image/png;base64," + new String(Base64.getEncoder().encode(IOUtils.readFully(iconInputStream, iconInputStream.available())), "UTF-8")));

        File launcherProfilesFile = new File(mcDir, "launcher_profiles.json");
        JsonObject currentProfileJson = new JsonParser().parse(new FileReader(launcherProfilesFile))
            .getAsJsonObject();
        JsonObject profilesObj = currentProfileJson.getAsJsonObject("profiles");

        final String profileUuid = "817b6478-503d-45a7-ade0-c13abe98db78".replace("-", "");

        String elementToRemove = null;
        for (Entry<String, JsonElement> jsonElement : profilesObj.entrySet()) {
          if (jsonElement.getKey().equals(profileUuid)) {
            elementToRemove = jsonElement.getKey();
            break;
          }
        }

        if (elementToRemove != null) {
          profilesObj.remove(elementToRemove);
        }

        profilesObj.add(profileUuid, profileToAdd);

        FileUtils.writeLines(launcherProfilesFile, Arrays.asList(
            new GsonBuilder().setPrettyPrinting().create().toJson(currentProfileJson).split("\n")));
      } catch (IOException e) {
        e.printStackTrace();
        infoLabel.setText("Error creating launcher profile.");
        return;
      }

      infoLabel.setText("Installed! Run the created profile to begin.");
      progressBar.setValue(++progressValue);
    });
  }

  private File getMCDir() {
    if (OSValidator.isWindows()) {
      return new File(
          System.getProperty("user.home") + File.separator + "AppData" + File.separator
              + "Roaming"
              + File.separator + ".minecraft" + File.separator);
    } else if (OSValidator.isMac()) {
      return new File(
          System.getProperty("user.home") + File.separator + "Library" + File.separator
              + "Application Support" + File.separator + "minecraft" + File.separator);
    } else if (OSValidator.isUnix()) {
      return new File(System.getProperty("user.home") + File.separator + ".minecraft");
    } else {
      return new File(
          System.getProperty("user.home") + File.separator + "AppData" + File.separator
              + "Roaming"
              + File.separator + ".minecraft" + File.separator);
    }
  }

}
