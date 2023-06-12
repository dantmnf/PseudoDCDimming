// IBacklightOverrideStateListener.aidl
package xyz.cirno.pseudodcdimming;

parcelable BacklightRequest;

interface IBacklightOverrideStateListener {
    oneway void onBacklightUpdated(in BacklightRequest request, in BacklightRequest override, float gain);
}
