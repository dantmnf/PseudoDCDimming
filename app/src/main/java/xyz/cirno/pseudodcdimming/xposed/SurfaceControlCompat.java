package xyz.cirno.pseudodcdimming.xposed;

import android.os.Build;
import android.os.IBinder;
import android.view.SurfaceControl$StaticDisplayInfo;
import android.view.SurfaceControlHidden34;
import android.view.SurfaceControlHiddenUpto33;

public class SurfaceControlCompat {
    public static SurfaceControl$StaticDisplayInfo getStaticDisplayInfo(IBinder displayToken) {
        if (Build.VERSION.SDK_INT >= 34) {
            var dc = DisplayControlProxy.getInstance();
            for (var physicalDisplayId : dc.getPhysicalDisplayIds()) {
                if (displayToken.equals(dc.getPhysicalDisplayToken(physicalDisplayId))) {
                    return SurfaceControlHidden34.getStaticDisplayInfo(physicalDisplayId);
                }
            }
            throw new RuntimeException("Display token not found: " + displayToken);
        } else {
            return SurfaceControlHiddenUpto33.getStaticDisplayInfo(displayToken);
        }
    }
}
