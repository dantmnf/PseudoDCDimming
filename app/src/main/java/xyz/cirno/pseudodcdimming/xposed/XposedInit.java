package xyz.cirno.pseudodcdimming.xposed;

import android.content.pm.IPackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.ServiceManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.cirno.pseudodcdimming.BacklightRequest;
import xyz.cirno.pseudodcdimming.BuildConfig;
import xyz.cirno.pseudodcdimming.IBacklightOverrideService;
import xyz.cirno.pseudodcdimming.IPCUtil;
import xyz.cirno.pseudodcdimming.ServiceDiscoveryResult;

public class XposedInit implements IXposedHookLoadPackage {
    private static final String TAG = "PseudoDcBacklight.Xposed";
    private static final String ATTACHED_IS_INTERNAL = "xyz.cirno.pseudodcbacklight.isInternal";
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("android".equals(lpparam.packageName)) {
            handleLoadSystemServer(lpparam);
        }
    }

    private static int resolveGainLayer(ClassLoader classLoader) {
        var displayTransformManagerClass = XposedHelpers.findClass("com.android.server.display.color.DisplayTransformManager", classLoader);

        final var invertColorLayerField = XposedHelpers.findFieldIfExists(
                displayTransformManagerClass,
                "LEVEL_COLOR_MATRIX_INVERT_COLOR"
        );


        int gainLayer = 299;
        try {
            if (invertColorLayerField != null) {
                gainLayer = invertColorLayerField.getInt(null);
            }
        } catch (IllegalAccessException e) {
            // ignore
        }
        return gainLayer;
    }

    private void handleLoadSystemServer(XC_LoadPackage.LoadPackageParam lpparam) {
        final var classLoader = lpparam.classLoader;

        final var overrideService = new BacklightOverrideService(classLoader);

        final var localDisplayDevice = XposedHelpers.findClass("com.android.server.display.LocalDisplayAdapter$LocalDisplayDevice", classLoader);

        final var backlightAdapter = XposedHelpers.findClass("com.android.server.display.LocalDisplayAdapter$BacklightAdapter", classLoader);

        final var localDisplayAdapter = XposedHelpers.findClass( "com.android.server.display.LocalDisplayAdapter", classLoader);

        final var backlightAdapter_setBacklight = XposedHelpers.findMethodExact(
            backlightAdapter,
            "setBacklight",
            float.class,  // sdrBacklight
            float.class,  // sdrNits
            float.class,  // backlight
            float.class   // nits
        );

        final var gainLayer = resolveGainLayer(classLoader);


        XposedHelpers.findAndHookConstructor(localDisplayDevice,
            localDisplayAdapter,     // [surrounding this]
            IBinder.class,     // displayToken
            long.class,        // physicalDisplayId
            "android.view.SurfaceControl$StaticDisplayInfo",       // staticDisplayInfo
            "android.view.SurfaceControl$DynamicDisplayInfo",      // dynamicInfo
            "android.view.SurfaceControl$DesiredDisplayModeSpecs", // modeSpecs
            boolean.class,     // isDefaultDisplay
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.hasThrowable()) {
                        return;
                    }
                    final var displayDeviceConfig = new DisplayDeviceConfigProxy(XposedHelpers.callMethod(param.thisObject,"getDisplayDeviceConfig"));
                    overrideService.lateInitialize(displayDeviceConfig);
                }
            }
        );
        XposedHelpers.findAndHookConstructor(backlightAdapter,
                IBinder.class,
                boolean.class,
                XposedHelpers.findClass("com.android.server.display.LocalDisplayAdapter$SurfaceControlProxy", classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.hasThrowable()) {
                            return;
                        }
                        final var token = (IBinder)param.args[0];
                        final var staticInfo = android.view.SurfaceControlHidden.getStaticDisplayInfo(token);
                        XposedHelpers.setAdditionalInstanceField(
                                param.thisObject,
                                ATTACHED_IS_INTERNAL,
                                staticInfo.isInternal
                        );
                        if (staticInfo.isInternal) {
                            overrideService.backlightAdapter = new BacklightAdapterProxy(param.thisObject, backlightAdapter_setBacklight);
                        }
                    }
                });

        XposedBridge.hookMethod(backlightAdapter_setBacklight, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // void setBacklight(float sdrBacklight, float sdrNits, float backlight, float nits)
                final var isInternal = XposedHelpers.getAdditionalInstanceField(param.thisObject, ATTACHED_IS_INTERNAL);
                if (isInternal == null || !((boolean)isInternal)) return;
                final var requestSdrBacklight = (float)param.args[0];
                final var requestSdrNits = (float)param.args[1];
                final var requestBacklight = (float)param.args[2];
                final var requestNits = (float)param.args[3];
                // Log.d(TAG, String.format(Locale.ROOT, "setBacklight(%f, %f, %f, %f)", requestSdrBacklight, requestSdrNits, requestBacklight, requestNits));

                var request = new BacklightRequest(requestSdrBacklight, requestSdrNits, requestBacklight, requestNits);

                var prevOverride = overrideService.getLastBacklightOverrideState();
                var overrideState = overrideService.getOverrideBacklightAndGain(request);

                var overrideBacklight = overrideState.overrideRequest;
                param.args[0] = overrideBacklight.sdrBacklightLevel;
                param.args[1] = overrideBacklight.sdrBacklightNits;
                param.args[2] = overrideBacklight.backlightLevel;
                param.args[3] = overrideBacklight.backlightNits;

                if (overrideBacklight.backlightLevel > prevOverride.overrideRequest.backlightLevel) {
                    // increased hardware brightness:
                    // set gain -> darker
                    // set brightness -> brighter
                    param.getExtra().putFloat("setGainAfterBrightness", -1.0f);
                    overrideService.setTransformGain(overrideState.gain);
                } else {
                    // decreased hardware brightness:
                    // set brightness -> darker
                    // set gain -> brighter
                    // a dark spike is more acceptable than a bright spike
                    param.getExtra().putFloat("setGainAfterBrightness", overrideState.gain);
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.hasThrowable()) {
                    return;
                }
                var lateGain = param.getExtra().getFloat("setGainAfterBrightness", -1.0f);
                if (lateGain > 0.0f) {
                    overrideService.setTransformGain(lateGain);
                }
                overrideService.notifyAllListeners();
            }
        });

        final var binderServiceClass = XposedHelpers.findClass("com.android.server.display.DisplayManagerService$BinderService", classLoader);
        XposedHelpers.findAndHookMethod("android.hardware.display.IDisplayManager$Stub", classLoader,
            "onTransact",
            int.class, Parcel.class, Parcel.class, int.class,
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!param.thisObject.getClass().equals(binderServiceClass)) return;

                    final int code = (int)param.args[0];

                    if (code != IPCUtil.TRANSACTION_SERVICE_DISCOVERY) return;

                    final Parcel data = (Parcel)param.args[1];
                    final Parcel reply = (Parcel)param.args[2];
                    final int flags = (int)param.args[3];


                    final var pmb = ServiceManager.getService("package");
                    // PMS not initialized yet, skip hook.
                    if (pmb == null) return;

                    final int uid = Binder.getCallingUid();
                    final var pm = IPackageManager.Stub.asInterface(pmb);
                    final var callingpackage = pm.getNameForUid(uid);

                    if (!BuildConfig.APPLICATION_ID.equals(callingpackage)) return;

                    if (code == IPCUtil.TRANSACTION_SERVICE_DISCOVERY) {
                        if (reply == null) {
                            param.setResult(false);
                            return;
                        }
                        final var response = new ServiceDiscoveryResult();
                        response.version = IBacklightOverrideService.VERSION;
                        response.service = overrideService.binderService.asBinder();
                        response.writeToParcel(reply, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
                        param.setResult(true);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                }
            });

    }
}
