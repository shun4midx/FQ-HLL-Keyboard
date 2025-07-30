#include <string>
#include <unordered_map>
#include <vector>
#include <utility>
#include <jni.h>

using std::string;
using std::vector;
using std::pair;
using std::unordered_map;

extern "C"
JNIEXPORT jobject JNICALL
Java_com_fqhll_keyboard_CustomKeyboardApp_nativeSuggest(
        JNIEnv* env,
        jobject /* this */,
        jstring prefix) {

    // Convert jstring -> std::string
    const char *p = env->GetStringUTFChars(prefix, nullptr);
    std::string key(p);
    env->ReleaseStringUTFChars(prefix, p);

    // Real logic: fill a vector of {text, confidence}
    pair<vector<string>, vector<double>> results;

    unordered_map<string, string> autoreplace = {{"i", "I"},
                                                 {"im", "I'm"},
                                                 {"Im", "I'm"},
                                                 {"id", "I'd"},
                                                 {"Id", "I'd"},
                                                 {"isnt", "isn't"},
                                                 {"Isnt", "Isn't"},
                                                 {"arent", "aren't"},
                                                 {"Arent", "Aren't"},
                                                 {"ill", "I'll"},
                                                 {"Ill", "I'll"},
                                                 {"doesnt", "doesn't"},
                                                 {"Doesnt", "Doesn't"},
                                                 {"dont", "don't"},
                                                 {"Dont", "Don't"},
                                                 {"wont", "won't"},
                                                 {"Wont", "Won't"},
                                                 {"hes", "he's"},
                                                 {"Hes", "He's"},
                                                 {"shes", "she's"},
                                                 {"Shes", "She's"},
                                                 {"its", "it's"},
                                                 {"Its", "It's"},
                                                 {"hed", "he'd"},
                                                 {"Hed", "He'd"},
                                                 {"aint", "ain't"},
                                                 {"Aint", "Ain't"},
                                                 {"cant", "can't"},
                                                 {"Cant", "Can't"},
                                                 {"shouldnt", "shouldn't"},
                                                 {"Shouldnt", "Shouldn't"},
                                                 {"couldnt", "couldn't"},
                                                 {"Couldnt", "Couldn't"},
                                                 {"wouldnt", "wouldn't"},
                                                 {"Wouldnt", "Wouldn't"},
                                                 {"didnt", "didn't"},
                                                 {"Didnt", "Didn't"}};

    if (key.empty() || key == " ") {
        results = {{" ", " ", " "},
                   {0.0, 0.0, 0.0}};
    } else if (autoreplace.find(key) != autoreplace.end()) {
        results = {{key, autoreplace[key], " "},
                   {0.5, 0.8, 0.0}};
    } else {
        results = {{key + "1", key, key + "2"},
                   {0.5, 0.55, 0.4}}; // Dummy confidences for demo
    }

    // Turn vector<string> -> Java String[]
    jclass strCls = env->FindClass("java/lang/String");
    jobjectArray jWords = env->NewObjectArray(
            results.first.size(),
            strCls,
            nullptr
    );
    for (size_t i = 0; i < results.first.size(); ++i) {
        env->SetObjectArrayElement(
                jWords, i,
                env->NewStringUTF(results.first[i].c_str())
        );
    }

    // Turn vector<double> -> Java double[]
    jdoubleArray jScores = env->NewDoubleArray(results.second.size());
    vector<jdouble> tmp(results.second.begin(), results.second.end());
    env->SetDoubleArrayRegion(
            jScores, 0, tmp.size(), tmp.data()
    );

    // Find Suggestion class + ctor
    jclass suggCls = env->FindClass("com/fqhll/keyboard/Suggestion");
    // signature: ( [Ljava/lang/String; [D )V
    jmethodID ctor = env->GetMethodID(
            suggCls,
            "<init>",
            "([Ljava/lang/String;[D)V"
    );

    // Create and return your Suggestion
    return env->NewObject(suggCls, ctor, jWords, jScores);
}