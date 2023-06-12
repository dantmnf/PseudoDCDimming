package xyz.cirno.pseudodcdimming.xposed;

import net.jcip.annotations.Immutable;

import xyz.cirno.pseudodcdimming.BacklightRequest;

@Immutable
public final class BacklightOverrideState {
    public final BacklightRequest overrideRequest;
    public final float gain;
    public final BacklightOverridePreferenceLocal effectivePreference;
    public BacklightOverrideState(BacklightRequest overrideRequest, float gain, BacklightOverridePreferenceLocal effectivePreference) {
        this.overrideRequest = overrideRequest;
        this.gain = gain;
        this.effectivePreference = effectivePreference;
    }
    public static final BacklightOverrideState INVALID = new BacklightOverrideState(BacklightRequest.INVALID, 1.0f, BacklightOverridePreferenceLocal.DEFAULT);
}
