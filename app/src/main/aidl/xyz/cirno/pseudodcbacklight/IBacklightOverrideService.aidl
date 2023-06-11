// IBacklightOverrideService.aidl
package xyz.cirno.pseudodcbacklight;

import xyz.cirno.pseudodcbacklight.IBacklightOverrideStateListener;
import xyz.cirno.pseudodcbacklight.BacklightOverridePreference;

interface IBacklightOverrideService {
    const int VERSION = 2;
    BacklightOverridePreference getPreference();
    void putPreference(in BacklightOverridePreference pref);

    void registerBacklightOverrideStateListener(IBacklightOverrideStateListener listener);
    void removeBacklightOverrideStateListener(IBacklightOverrideStateListener listener);
}
