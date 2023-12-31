package xyz.cirno.pseudodcdimming.xposed;

import android.annotation.SuppressLint;
import android.os.RemoteException;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import xyz.cirno.pseudodcdimming.BacklightOverridePreference;
import xyz.cirno.pseudodcdimming.BacklightRequest;
import xyz.cirno.pseudodcdimming.BuildConfig;
import xyz.cirno.pseudodcdimming.IBacklightOverrideService;
import xyz.cirno.pseudodcdimming.IBacklightOverrideStateListener;
import xyz.cirno.pseudodcdimming.util.PerceptualQuantizer;


public class BacklightOverrideService {
    private static final String TAG = "BacklightOverrideService";
    private final ClassLoader systemServerClassLoader;
    public final IBacklightOverrideService binderService = new BinderService();
    private volatile BacklightRequest lastBacklightRequest = BacklightRequest.INVALID;
    private volatile BacklightOverrideState lastBacklightOverride = BacklightOverrideState.INVALID;
    private volatile BacklightOverridePreferenceLocal preference = BacklightOverridePreferenceLocal.DEFAULT;
    public float deviceMinimumBacklightNits = Float.NaN;
    public BacklightAdapterProxy backlightAdapter;
    public DisplayDeviceConfigProxy displayDeviceConfig;
    private ExecutorService callbackExecutor;
    private final List<IBacklightOverrideStateListener> callbacks = new ArrayList<>();
    private DisplayTransformManagerProxy dtm;
    private final int gainLayer;

    public BacklightOverrideService(ClassLoader classLoader) {
        systemServerClassLoader = classLoader;
        gainLayer = resolveGainLayer(classLoader);
    }

    public void lateInitialize(DisplayDeviceConfigProxy deviceConfig) {
        callbackExecutor = Executors.newSingleThreadExecutor();
        displayDeviceConfig = deviceConfig;
        deviceMinimumBacklightNits = deviceConfig.getNitsFromBacklight(0.0f);
        Log.d(TAG, String.format(Locale.ROOT, "deviceMinimumBacklightNits = %f", deviceMinimumBacklightNits));
        setPreference(readPersistentPreference());
    }

    private BacklightOverridePreference readPersistentPreference() {
        var pref = new BacklightOverridePreference();
        try {
            var xsp = new XSharedPreferences(BuildConfig.APPLICATION_ID, "config");
            pref.enabled = xsp.getBoolean("enabled", false);
            pref.minimumOverrideBacklightLevel = xsp.getFloat("minimum_brightness", 0.0f);
            pref.duplicateApplicationWorkaround = xsp.getBoolean("gain_applied_twice", false);
        } catch (Exception e) {
            Log.e(TAG, "failed to read persistent preference", e);
            pref.enabled = false;
            pref.minimumOverrideBacklightLevel = 0.0f;
            pref.duplicateApplicationWorkaround = false;
        }
        return pref;
    }

    public DisplayTransformManagerProxy getTransformManager() {
        if (dtm == null) {
            try {
                var localServiceClass = XposedHelpers.findClass("com.android.server.LocalServices", systemServerClassLoader);
                var displayTransformManagerClass = XposedHelpers.findClass("com.android.server.display.color.DisplayTransformManager", systemServerClassLoader);
                var dtmobj = XposedHelpers.callStaticMethod(localServiceClass, "getService", displayTransformManagerClass);
                dtm = new DisplayTransformManagerProxy(dtmobj);
            } catch (Exception e) {
                Log.e(TAG, "failed to obtain DisplayTransformManager", e);
            }
        }
        return dtm;
    }

    @SuppressLint("PrivateApi")
    private static int resolveGainLayer(ClassLoader classLoader) {
        try {
             var displayTransformManagerClass = classLoader.loadClass("com.android.server.display.color.DisplayTransformManager");
            final var invertColorLayerField = displayTransformManagerClass.getDeclaredField("LEVEL_COLOR_MATRIX_INVERT_COLOR");
            return invertColorLayerField.getInt(null);
        } catch (ReflectiveOperationException e) {
            return 299;
        }
    }

    public void notifyAllListeners() {
        final var lastRequest = getLastBacklightRequest();
        final var lastOverrideState = getLastBacklightOverrideState();
        final var lastOverride = lastOverrideState.overrideRequest;
        final var pref = getPreference();
        Log.d(TAG, String.format(Locale.ROOT, "enable=%s, backlightLevel=%f->%f, backlightNits=%f->%f, transformGain=%f",
                pref.enabled,
                lastRequest.backlightLevel,
                lastOverride.backlightLevel,
                lastRequest.backlightNits,
                lastOverride.backlightNits,
                lastOverrideState.gain));
        callbackExecutor.execute(() -> {
            synchronized (callbacks) {
                final var itr = callbacks.listIterator();
                while(itr.hasNext()){
                    final var it = itr.next();
                    try {
                        it.onBacklightUpdated(lastRequest, lastOverride, lastOverrideState.gain);
                    } catch (RemoteException e) {
                        // remove invalid remote
                        itr.remove();
                    }
                }
            }
        });
    }

    public void addListener(IBacklightOverrideStateListener listener) {
        synchronized (callbacks) {
            callbacks.add(listener);
            Log.d(TAG, String.format(Locale.ROOT, "addListener: %d listeners registered", callbacks.size()));
        }
        notifyAllListeners();
    }

    public void removeListener(IBacklightOverrideStateListener listener) {
        final var targetBinder = listener.asBinder();
        if (targetBinder == null) return;
        synchronized (callbacks) {
            callbacks.removeIf(it -> {
                final var itBinder = it.asBinder();
                if (itBinder != null) {
                    return targetBinder.equals(itBinder);
                }
                return Objects.equals(listener, it);
            });
            Log.d(TAG, String.format(Locale.ROOT, "removeListener: %d listeners registered", callbacks.size()));
        }
    }

    private boolean validatePreference(BacklightOverridePreference pref) {
        if (pref.minimumOverrideBacklightLevel < 0.0f || pref.minimumOverrideBacklightLevel > 1.0f)
            return false;
        return true;
    }

    private void setPreference(BacklightOverridePreference pref) {
        if (!validatePreference(pref)) return;
        float newMinimumNits = 2.0f;
        if (displayDeviceConfig != null) {
            newMinimumNits = displayDeviceConfig.getNitsFromBacklight(pref.minimumOverrideBacklightLevel);
        }

        Log.d(TAG, String.format(Locale.ROOT, "setPreference: enabled=%s, minimumOverrideBacklightLevel=%f, minimumOverrideBacklightNits=%f",
                pref.enabled, pref.minimumOverrideBacklightLevel, newMinimumNits));

        preference = new BacklightOverridePreferenceLocal(pref.enabled, pref.minimumOverrideBacklightLevel, newMinimumNits, pref.duplicateApplicationWorkaround);

        final var lastRequest = getLastBacklightRequest();
        // skip refresh if setBacklight is not called yet
        if (Objects.equals(lastRequest, BacklightRequest.INVALID)) return;

        if (backlightAdapter != null) {
            try {
                backlightAdapter.setBacklight(
                        lastRequest.sdrBacklightLevel,
                        lastRequest.sdrBacklightNits,
                        lastRequest.backlightLevel,
                        lastRequest.backlightNits
                );
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public BacklightOverridePreferenceLocal getPreference() {
        return preference;
    }

    private BacklightOverridePreference getPreferenceForBinder() {
        var pref = getPreference();
        var result = new BacklightOverridePreference();
        result.enabled = pref.enabled;
        result.minimumOverrideBacklightLevel = pref.minimumOverrideBacklightLevel;
        result.duplicateApplicationWorkaround = pref.duplicateApplicationWorkaround;
        return result;
    }

    public BacklightRequest getLastBacklightRequest() {
        return lastBacklightRequest;
    }

    private void setLastBacklightRequest(BacklightRequest request) {
        lastBacklightRequest = request;
    }

    public void setLastBacklightOverride(BacklightOverrideState state) {
        lastBacklightOverride = state;
    }

    public BacklightOverrideState getLastBacklightOverrideState() {
        return lastBacklightOverride;
    }

    public BacklightOverrideState getOverrideBacklightAndGain(BacklightRequest request) {
        float overrideBacklight = request.backlightLevel;
        float overrideNits = request.backlightNits;
        float overrideSdrBacklight = request.sdrBacklightLevel;
        float overrideSdrNits = request.sdrBacklightNits;
        float gain = 1.0f;

        setLastBacklightRequest(request);

        var pref = getPreference();

        if (pref.enabled) {
            final var minOverrideBacklight = pref.minimumOverrideBacklightLevel;
            final var minOverrideNits = pref.minimumOverrideBacklightNits;
            if (request.backlightLevel < minOverrideBacklight) {
                overrideBacklight = minOverrideBacklight;
                overrideNits = minOverrideNits;
                gain = (request.backlightNits / minOverrideNits);
                gain = Math.max(gain, deviceMinimumBacklightNits / minOverrideNits);
                //gain = Math.max(gain, overrideService.minimumGain);
                //gain = Math.max(gain, 0.05f);
            }

            // also override sdr brightness if they don't differ much perceptually
            if (Math.abs(PerceptualQuantizer.NitsToSignal(request.sdrBacklightNits) - PerceptualQuantizer.NitsToSignal(request.backlightNits)) < 1.0/512.0) {
                overrideSdrBacklight = overrideBacklight;
                overrideSdrNits = overrideNits;
            }
        }
        var override = new BacklightRequest(
                overrideSdrBacklight,
                overrideSdrNits,
                overrideBacklight,
                overrideNits
        );
        var result = new BacklightOverrideState(override, gain, pref);
        setLastBacklightOverride(result);
        return result;
    }

    public void setTransformGain(float gain) {
        if (getPreference().duplicateApplicationWorkaround) {
            gain = (float)Math.sqrt(gain);
        }
        final var dtm = getTransformManager();
        if (dtm != null) {
            // if (!dtm.needsLinearColorMatrix()) {
            //     TODO: gamma gain
            // }
            final float[] mtx = {
                    gain, 0f, 0f, 0f,
                    0f, gain, 0f, 0f,
                    0f, 0f, gain, 0f,
                    0f, 0f, 0f, 1f
            };
            dtm.setColorMatrix(gainLayer, mtx);
        }
    }

    private final class BinderService extends IBacklightOverrideService.Stub {
        @Override
        public BacklightOverridePreference getPreference() throws RemoteException {
            return getPreferenceForBinder();
        }

        @Override
        public void putPreference(BacklightOverridePreference pref) throws RemoteException {
            setPreference(pref);
        }

        @Override
        public void registerBacklightOverrideStateListener(IBacklightOverrideStateListener listener) throws RemoteException {
            addListener(listener);
        }

        @Override
        public void removeBacklightOverrideStateListener(IBacklightOverrideStateListener listener) throws RemoteException {
            removeListener(listener);
        }
    }
}
