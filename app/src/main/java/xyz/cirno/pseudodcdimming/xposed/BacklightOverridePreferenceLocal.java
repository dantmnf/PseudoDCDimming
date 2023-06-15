package xyz.cirno.pseudodcdimming.xposed;

import net.jcip.annotations.Immutable;

@Immutable
public final class BacklightOverridePreferenceLocal {
    public final boolean enabled;
    public final float minimumOverrideBacklightLevel;
    public final float minimumOverrideBacklightNits;
    public final boolean duplicateApplicationWorkaround;
    public static final BacklightOverridePreferenceLocal DEFAULT = new BacklightOverridePreferenceLocal(false, 0.0f, 0.0f, false);

    public BacklightOverridePreferenceLocal(boolean enabled, float minimumOverrideBacklightLevel, float minimumOverrideBacklightNits, boolean duplicateApplicationWorkaround) {
        this.enabled = enabled;
        this.minimumOverrideBacklightLevel = minimumOverrideBacklightLevel;
        this.minimumOverrideBacklightNits = minimumOverrideBacklightNits;
        this.duplicateApplicationWorkaround = duplicateApplicationWorkaround;
    }
}
