package com.facebook.react.bridge;

public class CatalystInstanceImpl {
    public native void setGlobalVariable(String propName, String jsonValue);

    public void loadScriptFromFileOriginal(String fileName, String sourceURL, boolean loadSynchronously) {}
}
