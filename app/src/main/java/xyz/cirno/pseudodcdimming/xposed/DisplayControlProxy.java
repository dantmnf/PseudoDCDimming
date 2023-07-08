package xyz.cirno.pseudodcdimming.xposed;

import android.annotation.SuppressLint;
import android.os.IBinder;

import java.lang.reflect.Method;

@SuppressLint("PrivateApi")
public class DisplayControlProxy {
    private static DisplayControlProxy instance;
    private Method getPhysicalDisplayIdsMethod;
    private Method getPhysicalDisplayTokenMethod;

    public static void initialize(ClassLoader systemServerClassLoader) {
        if (instance == null) {
            instance = new DisplayControlProxy(systemServerClassLoader);
        }
    }

    public static DisplayControlProxy getInstance() {
        return instance;
    }

    private DisplayControlProxy(ClassLoader systemServerClassLoader) {
        try {
            final var clazz = systemServerClassLoader.loadClass("com.android.server.display.DisplayControl");
            getPhysicalDisplayIdsMethod = clazz.getDeclaredMethod("getPhysicalDisplayIds");
            getPhysicalDisplayTokenMethod = clazz.getDeclaredMethod("getPhysicalDisplayToken", long.class);
        } catch (Exception e) {
            // ignore
        }
    }

    public long[] getPhysicalDisplayIds() {
        try {
            return (long[])getPhysicalDisplayIdsMethod.invoke(null);
        } catch (Exception e) {
            return new long[0];
        }
    }

    public IBinder getPhysicalDisplayToken(long physicalDisplayId) {
        try {
            return (IBinder)getPhysicalDisplayTokenMethod.invoke(null, physicalDisplayId);
        } catch (Exception e) {
            return null;
        }
    }
}
