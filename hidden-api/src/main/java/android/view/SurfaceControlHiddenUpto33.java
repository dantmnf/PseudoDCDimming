package android.view;

import android.os.IBinder;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(SurfaceControl.class)
public final class SurfaceControlHiddenUpto33 {
    public static SurfaceControl$StaticDisplayInfo getStaticDisplayInfo(IBinder displayToken) {
        throw new RuntimeException();
    }
    public static long[] getPhysicalDisplayIds() {
        throw new RuntimeException();
    }
    public static IBinder getPhysicalDisplayToken(long physicalDisplayId) {
        throw new RuntimeException();
    }
}
