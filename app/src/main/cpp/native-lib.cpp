#include <jni.h>
#include <string>

std::string mockSuggestion(const std::string& input) {
    return "Suggested: " + input;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_fqhll_keyboard_MainActivity_getSuggestion(
        JNIEnv* env, jobject /* this */, jstring input) {
    const char *c_input = env->GetStringUTFChars(input, nullptr);
    std::string result = mockSuggestion(c_input);
    env->ReleaseStringUTFChars(input, c_input);
    return env->NewStringUTF(result.c_str());
}