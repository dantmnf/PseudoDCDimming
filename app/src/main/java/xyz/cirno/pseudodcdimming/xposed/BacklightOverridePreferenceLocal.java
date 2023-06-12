package xyz.cirno.pseudodcdimming.xposed;

import net.jcip.annotations.Immutable;

@Immutable
public final class BacklightOverridePreferenceLocal {
    public final boolean enabled;
    public final float minimumOverrideBacklightLevel;
    public final float minimumOverrideBacklightNits;
    public static final BacklightOverridePreferenceLocal DEFAULT = new BacklightOverridePreferenceLocal(false, 0.0f, 0.0f);

    public BacklightOverridePreferenceLocal(boolean enabled, float minimumOverrideBacklightLevel, float minimumOverrideBacklightNits) {
        this.enabled = enabled;
        this.minimumOverrideBacklightLevel = minimumOverrideBacklightLevel;
        this.minimumOverrideBacklightNits = minimumOverrideBacklightNits;
    }
}
