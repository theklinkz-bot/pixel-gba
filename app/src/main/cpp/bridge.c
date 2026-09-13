#include <jni.h>
#include <android/bitmap.h>
#include <mgba/core/core.h>
#include <mgba/core/cheats.h>
#include <mgba/core/blip_buf.h>
#include <mgba/core/serialize.h>
#include <mgba-util/vfs.h>
#include <fcntl.h>
#include <unistd.h>

static struct mCore* core;
static color_t pixels[240 * 160];
#define JNI(name) Java_dev_pixelgba_Core_##name

JNIEXPORT void JNICALL JNI(close)(JNIEnv* env, jclass cls) {
    if (core) { mCoreConfigDeinit(&core->config); core->deinit(core); core = NULL; }
}
JNIEXPORT jboolean JNICALL JNI(open)(JNIEnv* env, jclass cls, jstring rom, jstring save) {
    JNI(close)(env, cls);
    const char* path = (*env)->GetStringUTFChars(env, rom, NULL);
    core = mCoreCreate(mPLATFORM_GBA);
    bool ok = core && core->init(core);
    if (ok) {
        mCoreInitConfig(core, NULL);
        mCoreConfigSetIntValue(&core->config, "sampleRate", 32768);
        mCoreConfigSetIntValue(&core->config, "volume", 256);
        mCoreLoadConfig(core);
        core->setVideoBuffer(core, pixels, 240);
        core->setAudioBufferSize(core, 2048);
        blip_set_rates(core->getAudioChannel(core, 0), core->frequency(core), 32768);
        blip_set_rates(core->getAudioChannel(core, 1), core->frequency(core), 32768);
        ok = mCoreLoadFile(core, path);
    }
    (*env)->ReleaseStringUTFChars(env, rom, path);
    if (!ok) { JNI(close)(env, cls); return JNI_FALSE; }
    const char* sp = (*env)->GetStringUTFChars(env, save, NULL);
    struct VFile* vf = VFileOpen(sp, O_RDONLY);
    if (vf) { size_t size = vf->size(vf); void* data = malloc(size); if (data && vf->read(vf, data, size) == size) core->savedataRestore(core, data, size, false); free(data); vf->close(vf); }
    (*env)->ReleaseStringUTFChars(env, save, sp);
    core->reset(core);
    return JNI_TRUE;
}
JNIEXPORT jint JNICALL JNI(frame)(JNIEnv* env, jclass cls, jobject bitmap, jshortArray audio, jint keys, jint frames) {
    if (!core) return 0;
    int count = 0;
    short buffer[4096];
    core->setKeys(core, keys & 1023);
    for (int i = 0; i < frames && i < 5; ++i) {
        core->runFrame(core);
        count = blip_read_samples(core->getAudioChannel(core, 0), buffer, 2048, 1);
        blip_read_samples(core->getAudioChannel(core, 1), buffer + 1, 2048, 1);
    }
    AndroidBitmapInfo info;
    void* output;
    if (AndroidBitmap_getInfo(env, bitmap, &info) == 0 && info.width == 240 && info.height == 160 && AndroidBitmap_lockPixels(env, bitmap, &output) == 0) {
        for (int y = 0; y < 160; ++y) {
            uint32_t* row = (uint32_t*)((char*)output + y * info.stride);
            for (int x = 0; x < 240; ++x) row[x] = pixels[y * 240 + x] | 0xFF000000;
        }
        AndroidBitmap_unlockPixels(env, bitmap);
    }
    if (count * 2 <= (*env)->GetArrayLength(env, audio)) (*env)->SetShortArrayRegion(env, audio, 0, count * 2, buffer);
    return count * 2;
}
JNIEXPORT jboolean JNICALL JNI(state)(JNIEnv* env, jclass cls, jstring path, jboolean load) {
    if (!core) return JNI_FALSE;
    const char* p = (*env)->GetStringUTFChars(env, path, NULL);
    struct VFile* vf = VFileOpen(p, load ? O_RDONLY : O_RDWR | O_CREAT | O_TRUNC);
    bool ok = vf && (load ? mCoreLoadStateNamed(core, vf, SAVESTATE_SAVEDATA | SAVESTATE_RTC) : mCoreSaveStateNamed(core, vf, SAVESTATE_SAVEDATA | SAVESTATE_RTC));
    if (ok && !load) ok = vf->sync(vf, NULL, 0);
    if (vf) vf->close(vf);
    (*env)->ReleaseStringUTFChars(env, path, p);
    return ok;
}
JNIEXPORT jboolean JNICALL JNI(flush)(JNIEnv* env, jclass cls, jstring path) {
    if (!core) return JNI_FALSE;
    void* data = NULL;
    size_t size = core->savedataClone(core, &data);
    if (!size) { free(data); return JNI_TRUE; }
    const char* p = (*env)->GetStringUTFChars(env, path, NULL);
    int fd = open(p, O_WRONLY | O_CREAT | O_TRUNC, 0600);
    bool ok = fd >= 0 && write(fd, data, size) == (ssize_t)size && fsync(fd) == 0;
    if (fd >= 0) close(fd);
    free(data);
    (*env)->ReleaseStringUTFChars(env, path, p);
    return ok;
}
JNIEXPORT void JNICALL JNI(clearCheats)(JNIEnv* env, jclass cls) { if (core) mCheatDeviceClear(core->cheatDevice(core)); }
JNIEXPORT jboolean JNICALL JNI(cheat)(JNIEnv* env, jclass cls, jstring name, jstring code, jint type) {
    if (!core || type < 0 || type > 4) return JNI_FALSE;
    const char* n = (*env)->GetStringUTFChars(env, name, NULL);
    const char* c = (*env)->GetStringUTFChars(env, code, NULL);
    struct mCheatDevice* device = core->cheatDevice(core);
    struct mCheatSet* set = device->createSet(device, n);
    char* copy = strdup(c); char* context; char* line = strtok_r(copy, "\n\r", &context);
    bool ok = line != NULL;
    while (line) { if (!mCheatAddLine(set, line, type)) ok = false; line = strtok_r(NULL, "\n\r", &context); }
    if (ok) { set->enabled = true; mCheatAddSet(device, set); mCheatRefresh(device, set); }
    else { mCheatSetDeinit(set); }
    free(copy);
    (*env)->ReleaseStringUTFChars(env, name, n); (*env)->ReleaseStringUTFChars(env, code, c);
    return ok;
}
JNIEXPORT jint JNICALL JNI(read)(JNIEnv* env, jclass cls, jint address) { return core ? core->busRead16(core, address) : -1; }
