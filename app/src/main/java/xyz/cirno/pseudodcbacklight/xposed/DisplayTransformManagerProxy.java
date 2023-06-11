package xyz.cirno.pseudodcbacklight.xposed;

import java.lang.reflect.Method;

public class DisplayTransformManagerProxy {
    private final Object obj;
    private Method setColorMatrixMethod;
    private Method needsLinearColorMatrixMethod;

    public DisplayTransformManagerProxy(Object obj) {
        this.obj = obj;
        try {
            setColorMatrixMethod = obj.getClass().getMethod("setColorMatrix", int.class, float[].class);
            needsLinearColorMatrixMethod = obj.getClass().getMethod("needsLinearColorMatrix");
        } catch (NoSuchMethodException ignore) {}
    }
    public void setColorMatrix(int level, float[] value) {
        if (setColorMatrixMethod == null) return;
        try {
            setColorMatrixMethod.invoke(obj, level, value);
        } catch (Exception ignored) {}
    }
    public boolean needsLinearColorMatrix() {
        if (setColorMatrixMethod == null) return true;
        try {
            return (boolean)needsLinearColorMatrixMethod.invoke(obj);
        } catch (Exception e) {
            return false;
        }
    }
}
