// IBacklightOverrideService.aidl
package xyz.cirno.pseudodcdimming;

import xyz.cirno.pseudodcdimming.IBacklightOverrideStateListener;
import xyz.cirno.pseudodcdimming.BacklightOverridePreference;

interface IBacklightOverrideService {
    const int VERSION = 2;
    BacklightOverridePreference getPreference();
    void putPreference(in BacklightOverridePreference pref);

    void registerBacklightOverrideStateListener(IBacklightOverrideStateListener listener);
    void removeBacklightOverrideStateListener(IBacklightOverrideStateListener listener);
}
