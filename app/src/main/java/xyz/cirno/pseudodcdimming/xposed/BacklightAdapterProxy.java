package xyz.cirno.pseudodcdimming.xposed;

import java.lang.reflect.Method;

public class BacklightAdapterProxy {
    private final Object adapter;
    private final Method setBacklightMethod;

    public BacklightAdapterProxy(Object adapter, Method setBacklightMethod) {
        this.adapter = adapter;
        this.setBacklightMethod = setBacklightMethod;
    }

    public void setBacklight(float sdrBacklight, float sdrNits, float backlight, float nits) {
        try {
            setBacklightMethod.invoke(adapter, sdrBacklight, sdrNits, backlight, nits);
        } catch (Exception e) {
            // ignore
        }
    }
}
