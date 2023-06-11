package android.content.pm;

public interface IPackageManager extends android.os.IInterface {
    String getNameForUid(int uid);
    public static abstract class Stub extends android.os.Binder implements IPackageManager {
        public static IPackageManager asInterface(android.os.IBinder obj) {
            throw new RuntimeException();
        }
    }
}
