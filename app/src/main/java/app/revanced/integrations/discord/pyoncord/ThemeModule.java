package app.revanced.integrations.discord.pyoncord;

import static app.revanced.integrations.discord.pyoncord.PyoncordPatch.context;
import static app.revanced.integrations.discord.pyoncord.Utils.readText;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

public class ThemeModule extends PyonModule {

    private JSONObject theme;
    private static HashMap<String, Integer> rawColorMap;
    private static HashMap<String, int[]> semanticColorMap;

    @Override
    public void buildJson(JSONObject jsonObject) throws JSONException {
        jsonObject.put("hasThemeSupport", true);
        jsonObject.put("storedTheme", theme);
    }

    @Override
    public void onInit() {
        theme = getTheme();
        initTheme();
    }

    private JSONObject getTheme() {
        var pyonDir = new File(context.getFilesDir(), "pyoncord");
        pyonDir.mkdirs();
        var themeFile = new File(pyonDir, "current-theme.json");

        /*
         *  Don't consider legacy themes
         */

        if (!themeFile.exists()) return null;

        try {
            var themeText = readText(themeFile);
            if (themeText.isBlank() || themeText.equals("{}") || themeText.equals("null"))
                return null;
            return new JSONObject(themeText);
        } catch (Exception e) {
            return null;
        }
    }

    private void initTheme() {
        if (theme == null) return;

        // Initialize rawColors
        try {
            rawColorMap = new HashMap<>();
            JSONObject jsonRawColors = theme.getJSONObject("data").getJSONObject("rawColors");
            for (Iterator<String> it = jsonRawColors.keys(); it.hasNext(); ) {
                String key = it.next();
                int value = hexStringToColorInt(jsonRawColors.getString(key));
                rawColorMap.put(key.toLowerCase(), value);
            }
        } catch (JSONException e) {
            rawColorMap = null;
        }

        // Initialize semanticColors
        try {
            semanticColorMap = new HashMap<>();
            JSONObject jsonSemanticColors = theme.getJSONObject("data").getJSONObject("semanticColors");
            for (Iterator<String> it = jsonSemanticColors.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONArray arr = jsonSemanticColors.getJSONArray(key);

                int[] value;
                if (arr.length() == 1) {
                    value = new int[]{hexStringToColorInt(arr.getString(0))};
                } else {
                    value = new int[]{hexStringToColorInt(arr.getString(0)), hexStringToColorInt(arr.getString(1))};
                }
                semanticColorMap.put(key, value);
            }
        } catch (JSONException e) {
            semanticColorMap = null;
        }
    }


    // Convert 0xRRGGBBAA to 0xAARRGGBB
    private int hexStringToColorInt(String hexString) {
        var parsed = Color.parseColor(hexString);
        return (hexString.length() == 7) ? parsed : parsed & 0xFFFFFF | (parsed >>> 24);
    }

    /**
     * Injection point.
     * If there's any rawColors value, hook the color getter
     */
    public static int getRawColor(Object object, int resId, int originalColor) {
        try {
            PyoncordPatch.initilizeTask.join(10000);
        } catch (InterruptedException e) {
            return originalColor;
        }
        if (rawColorMap == null) return originalColor;

        Resources resources;
        if (object instanceof Context) {
            resources = ((Context) object).getResources();
        } else {
            resources = (Resources) object;
        }
        var name = resources.getResourceEntryName(resId);
        Integer color = rawColorMap.get(name);

        if (color != null) {
            return color;
        } else {
            return originalColor;
        }
    }

    /**
     * Injection point.
     */
    public static int getSemanticColorDark(int originalColor, String colorName) {
        try {
            PyoncordPatch.initilizeTask.join(10000);
        } catch (InterruptedException e) {
            return originalColor;
        }
        if (semanticColorMap != null && semanticColorMap.get(colorName) != null) {
            return semanticColorMap.get(colorName)[0];
        } else {
            return originalColor;
        }
    }

    /**
     * Injection point.
     */
    public static int getSemanticColorLight(int originalColor, String colorName) {
        try {
            PyoncordPatch.initilizeTask.join(10000);
        } catch (InterruptedException e) {
            return originalColor;
        }
        if (semanticColorMap != null) {
            var arr = semanticColorMap.get(colorName);
            if (arr != null && arr.length >= 2) {
                return semanticColorMap.get(colorName)[1];
            }
        }
        return originalColor;
    }
}
