package android.view;

import android.hardware.display.DeviceProductInfo;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(SurfaceControl.class)
public final class SurfaceControlHidden {
    public static SurfaceControl$StaticDisplayInfo getStaticDisplayInfo(IBinder displayToken) {
        throw new RuntimeException();
    }
}
