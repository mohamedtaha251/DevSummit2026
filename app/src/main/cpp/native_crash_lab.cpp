#include <jni.h>
#include <cstdlib>

extern "C" JNIEXPORT void JNICALL
Java_com_example_devsummit2026_ui_crashlytics_NativeCrashLab_triggerGwpAsanOrMteCrash(
        JNIEnv* /* env */,
        jobject /* thiz */) {
    // Trigger Use-After-Free so GWP-ASan (gwpAsanMode="always") or ARM MTE traps with SIGSEGV.
    for (int i = 0; i < 4096; ++i) {
        auto* ptr = static_cast<volatile char*>(std::malloc(64));
        if (ptr == nullptr) {
            break;
        }
        ptr[0] = 'A';
        std::free(const_cast<char*>(ptr));
        // Intentional Use-After-Free
        ptr[0] = 'B';
        volatile char sink = ptr[0];
        (void) sink;
    }
    // Fallback if GWP-ASan/MTE is unavailable on this device/emulator
    std::abort();
}
