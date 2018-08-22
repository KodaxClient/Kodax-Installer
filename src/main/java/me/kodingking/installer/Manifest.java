package me.kodingking.installer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Manifest {

    private String latestBuild;
    private URL buildDownload, latestBetaDownload;

    public Manifest(String latestBuild, URL buildDownload, URL latestBetaDownload) {
        this.latestBuild = latestBuild;
        this.buildDownload = buildDownload;
        this.latestBetaDownload = latestBetaDownload;
    }

    public String getLatestBuild() {
        return latestBuild;
    }

    public URL getBuildDownload() {
        return buildDownload;
    }

    public URL getLatestBetaDownload() {
        return latestBetaDownload;
    }

    public static Manifest fetch(String urlToRead) {
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(urlToRead);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            JsonObject object = new JsonParser().parse(result.toString()).getAsJsonObject();
            JsonObject metaObj = object.getAsJsonObject("meta");
            return new Manifest(metaObj.get("latestBuild").getAsString(), new URL(metaObj.get("latestDownload").getAsString()), new URL(metaObj.get("latestBetaDownload").getAsString()));
        } catch (Exception e) {
            System.out.println("Error reading manifest...");
            return null;
        }
    }
}
