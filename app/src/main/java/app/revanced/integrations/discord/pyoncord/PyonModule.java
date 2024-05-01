package app.revanced.integrations.discord.pyoncord;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class PyonModule {
    public abstract void buildJson(JSONObject jsonObject) throws JSONException;
    public abstract void onInit();
}
