#include <jni.h>
#include <string>

#include <jni.h>
#include <vector>
#include <string>

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_fqhll_keyboard_CustomKeyboardApp_nativeSuggest(
        JNIEnv* env,
        jobject /* this */,
        jstring prefix) {

    // convert jstring → std::string
    const char* p = env->GetStringUTFChars(prefix, nullptr);
    std::string key = p;
    env->ReleaseStringUTFChars(prefix, p);

    // your real suggestion logic here...
    std::vector<std::string> suggestions;

    if (key == "" || key == " ") {
        suggestions = {" ", " ", " "};
    } else {
        suggestions = {key + "1", key, key + "2"};
    }

    // build a Java String[] to return
    jobjectArray out = env->NewObjectArray(
            3,
            env->FindClass("java/lang/String"),
            nullptr
    );
    for (int i = 0; i < 3; ++i) {
        env->SetObjectArrayElement(
                out, i, env->NewStringUTF(suggestions[i].c_str()));
    }
    return out;
}
