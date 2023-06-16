package xyz.cirno.pseudodcdimming;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.TextView;


import java.util.Locale;

@SuppressWarnings("deprecation")
public class SettingsActivity extends Activity {
    private final static String TAG = "SettingsActivity";
    private SettingsFragment fragment;
    private final Handler uiHandler = new Handler(Looper.myLooper());
    private final HandlerTimer updateTimer = new HandlerTimer(uiHandler, 100, this::updateStatus);

    private IBacklightOverrideService service;
    private IBacklightOverrideStateListener listener;
    private SharedPreferences xsp = null;
    private static final int ERROR_VERSION_MISMATCH = 0x4;
    private static final int ERROR_SERVICE_NOT_FOUND = 0x2;
    private static final int ERROR_NO_PREFS = 0x1;

    private BacklightRequest requestBacklight;
    private BacklightRequest overrideBacklight;
    private float gain;

    private long lastNotificationTime = 0;
    private long lastUpdatedNotificationTime = 0;

    @Override
    @SuppressLint("WorldReadableFiles")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        service = ServiceDiscovery.getService();
        try {
            xsp = getSharedPreferences("config", MODE_WORLD_READABLE);
        } catch (SecurityException e) {
            Log.e(TAG, "failed to get shared preferences", e);
        }
        fragment = new SettingsFragment();
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, fragment)
                .commit();
        listener = new IBacklightOverrideStateListener.Stub() {
            @Override
            public void onBacklightUpdated(BacklightRequest request, BacklightRequest override, float gain) throws RemoteException {
                lastNotificationTime = SystemClock.uptimeMillis();
                requestBacklight = request;
                overrideBacklight = override;
                SettingsActivity.this.gain = gain;
            }
        };
        checkErrors();
        syncXSharedPreferences();
    }

    private void checkErrors() {
        int errorLevel = 0;
        if (service == null) {
            errorLevel |= ERROR_SERVICE_NOT_FOUND;
        }
        if (ServiceDiscovery.isVersionMismatch()) {
            errorLevel |= ERROR_VERSION_MISMATCH;
        }
        if (xsp == null) {
            errorLevel |= ERROR_NO_PREFS;
        }

        if ((errorLevel & ERROR_VERSION_MISMATCH) != 0) {
            setError(R.string.version_mismatch_title, R.string.version_mismatch_message);
        } else if ((errorLevel & ERROR_SERVICE_NOT_FOUND) != 0) {
            setError(R.string.not_loaded_title, R.string.not_loaded_message);
        } else if ((errorLevel & ERROR_NO_PREFS) != 0) {
            setError(R.string.no_prefs_title, R.string.no_prefs_message);
        } else {
            findViewById(R.id.errorBanner).setVisibility(android.view.View.GONE);
        }
    }

    private void setError(int titleId, int messageId) {
        ((TextView)findViewById(R.id.errorTitle)).setText(titleId);
        ((TextView)findViewById(R.id.errorMessage)).setText(messageId);
        findViewById(R.id.errorBanner).setVisibility(android.view.View.VISIBLE);
    }

    private void syncXSharedPreferences() {
        if (service == null || xsp == null) return;
        BacklightOverridePreference pref;
        try {
            pref = service.getPreference();
        } catch (RemoteException e) {
            return;
        }
        boolean dirty = xsp.getBoolean("enabled", false) != pref.enabled;
        dirty = dirty || xsp.getFloat("minimum_brightness", 0.0f) != pref.minimumOverrideBacklightLevel;
        dirty = dirty || xsp.getBoolean("gain_applied_twice", false) != pref.duplicateApplicationWorkaround;

        if (dirty) {
            writeXSharedPreferences(pref);
        }
    }

    private void writeXSharedPreferences(BacklightOverridePreference pref) {
        if (xsp == null) return;
        xsp.edit()
                .putBoolean("enabled", pref.enabled)
                .putFloat("minimum_brightness", pref.minimumOverrideBacklightLevel)
                .putBoolean("gain_applied_twice", pref.duplicateApplicationWorkaround)
                .apply();
    }
    private void updateStatus() {
        if (lastUpdatedNotificationTime == lastNotificationTime) return;
        fragment.updateStatus(requestBacklight, overrideBacklight, gain);
        lastUpdatedNotificationTime = lastNotificationTime;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (service != null) {
            try {
                service.registerBacklightOverrideStateListener(listener);
                updateTimer.start();
            } catch (RemoteException e) {
                Log.e(TAG, "failed to register listener", e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (service != null) {
            try {
                updateTimer.stop();
                service.removeBacklightOverrideStateListener(listener);
            } catch (RemoteException e) {
                Log.e(TAG, "failed to unregister listener", e);
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        private IBacklightOverrideService service;
        private SwitchPreference enablePref;
        private EditTextPreference minimumBrightnessPref;
        private CheckBoxPreference gainAppliedTwicePref;

        private Preference requestBacklightPref;
        private Preference overrideBacklightPref;
        private Preference gainPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.root_preferences);

            findPreference("version").setSummary(BuildConfig.VERSION_NAME);

            final var activity = (SettingsActivity) getActivity();
            if (activity == null) return;
            service = activity.service;

            enablePref = (SwitchPreference) findPreference("enable");
            if (enablePref != null) {
                enablePref.setOnPreferenceChangeListener((p, v) -> {
                    try {
                        final var pref = service.getPreference();
                        pref.enabled = (Boolean) v;
                        service.putPreference(pref);
                        activity.writeXSharedPreferences(pref);
                        return true;
                    } catch (RemoteException e) {
                        return false;
                    }
                });
            }


            minimumBrightnessPref = (EditTextPreference) findPreference("minimum_brightness");
            if (minimumBrightnessPref != null) {
                minimumBrightnessPref.setOnPreferenceChangeListener((p, v) -> {
                    try {
                        final var fvalue = Float.parseFloat((String) v) / 100.0f;
                        if (fvalue > 1.0f || fvalue < 0.0f) return false;
                        final var pref = service.getPreference();
                        pref.minimumOverrideBacklightLevel = fvalue;
                        service.putPreference(pref);
                        updateMinimumBrightnessPreference(fvalue);
                        activity.writeXSharedPreferences(pref);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
            }

            gainAppliedTwicePref = (CheckBoxPreference) findPreference("gain_applied_twice");
            if (gainAppliedTwicePref != null) {
                gainAppliedTwicePref.setOnPreferenceChangeListener((p, v) -> {
                    try {
                        final var pref = service.getPreference();
                        pref.duplicateApplicationWorkaround = (Boolean) v;
                        service.putPreference(pref);
                        activity.writeXSharedPreferences(pref);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                });
            }

            requestBacklightPref = findPreference("request_brightness");
            overrideBacklightPref = findPreference("actual_brightness");
            gainPref = findPreference("gain");

            findPreference("project_home").setOnPreferenceClickListener(p -> {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.project_url))));
                return true;
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            final SettingsActivity activity = (SettingsActivity) getActivity();
            if (activity == null || activity.service == null) {
                findPreference("settings").setEnabled(false);
                findPreference("status").setEnabled(false);
            } else {
                try {
                    var pref = service.getPreference();
                    enablePref.setChecked(pref.enabled);
                    updateMinimumBrightnessPreference(pref.minimumOverrideBacklightLevel);
                    gainAppliedTwicePref.setChecked(pref.duplicateApplicationWorkaround);
                } catch (Exception e) {
                    // ignore
                }

            }
        }

        public void updateStatus(BacklightRequest request, BacklightRequest effective, float gain) {
            requestBacklightPref.setSummary(String.format(Locale.ROOT, "%.2f%% (%.2f cd/m²)", request.backlightLevel * 100, request.backlightNits));
            overrideBacklightPref.setSummary(String.format(Locale.ROOT, "%.2f%% (%.2f cd/m²)", effective.backlightLevel * 100, effective.backlightNits));
            gainPref.setSummary(String.format(Locale.ROOT, "%.5f", gain));
        }

        private void updateMinimumBrightnessPreference(float newValue) {
            var s2 = String.format(Locale.ROOT, "%.2f", newValue * 100);
            minimumBrightnessPref.setText(s2);
            minimumBrightnessPref.setSummary(s2 + "%");
        }

    }
}