// IBacklightOverrideStateListener.aidl
package xyz.cirno.pseudodcbacklight;

parcelable BacklightRequest;

interface IBacklightOverrideStateListener {
    oneway void onBacklightUpdated(in BacklightRequest request, in BacklightRequest override, float gain);
}
