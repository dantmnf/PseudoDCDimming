package xyz.cirno.pseudodcdimming;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ServiceManager;
import android.util.Log;

public class ServiceDiscovery {
    public static final int TRANSACTION_SERVICE_DISCOVERY = 0x67000000 | 114514;
    private static boolean _isVersionMismatch = false;
    private static IBinder displayManager;

    private static IBinder getDisplayManager() {
        if (displayManager == null) {
            try {
                displayManager = ServiceManager.getService(Context.DISPLAY_SERVICE);
            } catch (Exception e) {
                Log.e("IPCUtil", "failed to call ServiceManager.getService, module not enabled?");
            }
        }
        return displayManager;
    }

    public static boolean isVersionMismatch() {
        if (displayManager == null) {
            getDisplayManager();
        }
        return _isVersionMismatch;
    }

    public static IBacklightOverrideService getService() {
        final var ds = getDisplayManager();
        if (ds == null) {
            return null;
        }

        final var req = Parcel.obtain();
        final var resp = Parcel.obtain();
        try {
            var status = ds.transact(TRANSACTION_SERVICE_DISCOVERY, req, resp, 0);
            if (status) {
                var result2 = ServiceDiscoveryResult.CREATOR.createFromParcel(resp);
                if (result2.version == 0) {
                    return null;
                }
                if (result2.version != IBacklightOverrideService.VERSION) {
                    _isVersionMismatch = true;
                    return null;
                }
                return IBacklightOverrideService.Stub.asInterface(result2.service);
            }
        } catch (Exception e) {
            // ignore
        } finally {
            req.recycle();
            resp.recycle();
        }
        return null;
    }

}
