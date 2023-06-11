package xyz.cirno.pseudodcbacklight;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import net.jcip.annotations.Immutable;

@Immutable
public final class BacklightRequest implements Parcelable {
    public final float sdrBacklightLevel;
    public final float sdrBacklightNits;
    public final float backlightLevel;
    public final float backlightNits;

    public BacklightRequest(float sdrBacklightLevel, float sdrBacklightNits, float backlightLevel, float backlightNits) {
        this.sdrBacklightLevel = sdrBacklightLevel;
        this.sdrBacklightNits = sdrBacklightNits;
        this.backlightLevel = backlightLevel;
        this.backlightNits = backlightNits;
    }

    public static final BacklightRequest INVALID = new BacklightRequest(Float.NaN, Float.NaN, Float.NaN, Float.NaN);

    // can't use AIDL source generator because we need to make it immutable (shrug
    public static final Creator<BacklightRequest> CREATOR = new Creator<>() {
        @Override
        public BacklightRequest createFromParcel(Parcel in) {
            final var pos0 = in.dataPosition();
            final var size = in.readInt();
            float _backlightLevel = 0.0f;
            float _backlightNits = 0.0f;
            float _sdrBacklightLevel = 0.0f;
            float _sdrBacklightNits = 0.0f;

            if (size < 0) return INVALID;
            try {
                _backlightLevel = in.readFloat();
                if (in.dataPosition() - pos0 >= size) return INVALID;
                _backlightNits = in.readFloat();
                if (in.dataPosition() - pos0 >= size) return INVALID;
                _sdrBacklightLevel = in.readFloat();
                if (in.dataPosition() - pos0 >= size) return INVALID;
                _sdrBacklightNits = in.readFloat();
                if (in.dataPosition() - pos0 > size) return INVALID;
            } finally {
                in.setDataPosition(pos0 + size);
            }
            return new BacklightRequest(_backlightLevel, _backlightNits, _sdrBacklightLevel, _sdrBacklightNits);
        }

        @Override
        public BacklightRequest[] newArray(int size) {
            return new BacklightRequest[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        final var pos0 = dest.dataPosition();
        dest.writeInt(0);
        dest.writeFloat(backlightLevel);
        dest.writeFloat(backlightNits);
        dest.writeFloat(sdrBacklightLevel);
        dest.writeFloat(sdrBacklightNits);
        final var pos1 = dest.dataPosition();
        dest.setDataPosition(pos0);
        dest.writeInt(pos1 - pos0);
        dest.setDataPosition(pos1);
    }
}
