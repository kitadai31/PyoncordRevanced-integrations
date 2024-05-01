package app.revanced.integrations.discord.pyoncord;

import static app.revanced.integrations.discord.pyoncord.Utils.readText;
import static app.revanced.integrations.discord.pyoncord.Utils.writeText;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.CatalystInstanceImpl;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PyoncordPatch {
    public static Context context;
    public static final String TAG = "PyoncordRevanced";
    public static Thread initilizeTask;
    private static final PyonModule[] pyonModules = {
            new ThemeModule()
            // TODO: Implement SysColorsModule() and FontsModule()
    };
    private static File bundle;

    private static String buildLoaderJsonString() {
        try {
            JSONObject json = new JSONObject();
            json.put("loaderName", "PyoncordRevanced");
            json.put("loaderVersion", "1.0.0");

            for (PyonModule module : pyonModules) {
                module.buildJson(json);
            }
            return json.toString();
        } catch (JSONException e) {
            Log.e(TAG, "should never happen", e);
        }
        return null;
    }

    /**
     * Injection point.
     * Equivalent to handleLoadPackage in PyonXposed
     */
    public static void onCreateApplication(Application application) {
        Log.i(TAG, "onCreateApplication が呼ばれました。");
        context = application.getApplicationContext();
        initilizeTask = new Thread(() -> {
            for (PyonModule module : pyonModules) module.onInit();

            File cacheDir = new File(context.getFilesDir(), "pyoncord");
            cacheDir.mkdirs();
            File filesDir = new File(context.getFilesDir(), "pyoncord");
            filesDir.mkdirs();

            File preloadDir = new File(filesDir, "preloads");
            preloadDir.mkdirs();
            bundle = new File(cacheDir, "bundle.js");
            File etag = new File(cacheDir, "etag.txt");

            try {
                //TODO: Implement URL config
                String url = "https://raw.githubusercontent.com/pyoncord/detta-builds/main/bunny.js";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.addRequestProperty("User-Agent", "PyoncordRevanced " + System.getProperty("http.agent"));
                if (etag.exists() && bundle.exists()) {
                    conn.setRequestProperty("If-None-Match", readText(etag));
                }

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        //sb.append(System.lineSeparator());
                    }
                    br.close();
                    writeText(bundle, sb.toString());
                    var header = conn.getHeaderField("Etag");
                    if (header != null) writeText(etag, header);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to download pyoncord", e);
            }
        });
        initilizeTask.start();
    }

    /**
     * Injection point
     */
    public static void beforeLoadScript(CatalystInstanceImpl catalystInstance) {
        try {
            initilizeTask.join(10000);
        } catch (InterruptedException ignored) {
        }

        catalystInstance.setGlobalVariable("__PYON_LOADER__", buildLoaderJsonString());
        catalystInstance.loadScriptFromFileOriginal(bundle.getAbsolutePath(), bundle.getAbsolutePath(), false);
        //TODO: Is the third parameter really false?
    }
}
