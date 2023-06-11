package xyz.cirno.pseudodcbacklight.xposed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import de.robv.android.xposed.XposedHelpers;

public class DisplayDeviceConfigProxy {
    private final Object obj;
    private final Method getNitsFromBacklightMethod;

    public DisplayDeviceConfigProxy(Object obj) {
        this.obj = obj;
        getNitsFromBacklightMethod = XposedHelpers.findMethodExact(obj.getClass(), "getNitsFromBacklight", float.class);
    }

    float getNitsFromBacklight(float backlight) {
        try {
            var value = getNitsFromBacklightMethod.invoke(obj, backlight);
            if (value != null) {
                return (float)value;
            }
        } catch (Exception e) {
            // ignore
        }
        return -1;
    }
}
