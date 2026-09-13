package dev.pixelgba;

import android.graphics.Bitmap;

final class Core {
    static { System.loadLibrary("pixelgba"); }
    // ponytail: one core, serialized calls; multi-session emulation would need native handles.
    static synchronized native boolean open(String rom, String save);
    static synchronized native void close();
    static synchronized native int frame(Bitmap bitmap, short[] audio, int keys, float speed);
    static synchronized native boolean state(String path, boolean load);
    static synchronized native boolean flush(String path);
    static synchronized native void clearCheats();
    static synchronized native boolean cheat(String name, String code, int type);
    static synchronized native int read(int address);
}
