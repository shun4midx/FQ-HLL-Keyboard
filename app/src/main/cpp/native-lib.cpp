#include <FQ-HLL/FQ-HLL.h>
#include <string>
#include <unordered_map>
#include <vector>
#include <utility>
#include <jni.h>
#include <fstream>

using std::string;
using std::vector;
using std::pair;
using std::unordered_map;

static std::unique_ptr<Autocorrector> g_ac;


std::vector<std::string> getWords(const std::string& path) {
    std::vector<std::string> words;

    std::ifstream file(path);
    if (!file.is_open()) {
        return words;
    }

    std::string line;
    while (std::getline(file, line)) {
        // Remove trailing \r or \n
        line.erase(line.find_last_not_of("\r\n") + 1);
        if (!line.empty()) {
            words.push_back(line);
        }
    }

    return words;
}

bool isLower(char c) {
    return c >= 'a' && c <= 'z';
}

char toUpper(char c) {
    if (c >= 'a' && c <= 'z') {
        return c - 'a' + 'A';
    } else {
        return c;
    }
}

int getCaseState(const std::string& word) {
    for (int i = 0; i < word.length(); ++i) {
        if (isLower(word[i])) {
            return i == 0 ? 0 : 1;
        }
    }

    return 2;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_fqhll_keyboard_CustomKeyboardApp_nativeInitAutocorrector(JNIEnv* env, jobject /* this */, jstring jpath) {
    const char* c_path = env->GetStringUTFChars(jpath, nullptr);
    std::string path(c_path);
    env->ReleaseStringUTFChars(jpath, c_path);

    AutocorrectorCfg cfg;
    cfg.dictionary_list = getWords(path);
    g_ac = std::make_unique<Autocorrector>(cfg);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_fqhll_keyboard_CustomKeyboardApp_nativeSuggest(
        JNIEnv* env,
        jobject /* this */,
        jstring prefix) {

    if (!g_ac) return nullptr;

    // Convert jstring -> std::string
    const char *p = env->GetStringUTFChars(prefix, nullptr);
    std::string key(p);
    env->ReleaseStringUTFChars(prefix, p);

    // Real logic: fill a vector of {text, confidence}
    pair<vector<string>, vector<double>> results;

    unordered_map<string, string> autoreplace = {{"i", "I"}, {"im", "I'm"}, {"Im", "I'm"}, {"id", "I'd"}, {"Id", "I'd"}, {"youd", "you'd"}, {"Youd", "You'd"}, {"youll", "you'll"}, {"Youll", "You'll"}, {"isnt", "isn't"}, {"Isnt", "Isn't"},
                                                 {"wasnt", "wasn't"}, {"Wasnt", "Wasn't"}, {"arent", "aren't"}, {"Arent", "Aren't"}, {"ill", "I'll"}, {"Ill", "I'll"}, {"doesnt", "doesn't"}, {"Doesnt", "Doesn't"}, {"dont", "don't"}, {"Dont", "Don't"},
                                                 {"wont", "won't"}, {"Wont", "Won't"}, {"hes", "he's"}, {"Hes", "He's"}, {"shes", "she's"}, {"Shes", "She's"}, {"its", "it's"}, {"Its",  "It's"}, {"lets", "let's"}, {"Lets", "Let's"},
                                                 {"hed", "he'd"}, {"Hed", "He'd"}, {"aint", "ain't"}, {"Aint", "Ain't"}, {"cant", "can't"}, {"Cant", "Can't"}, {"shouldnt", "shouldn't"}, {"Shouldnt", "Shouldn't"},
                                                 {"couldnt", "couldn't"}, {"Couldnt", "Couldn't"}, {"wouldnt", "wouldn't"}, {"Wouldnt", "Wouldn't"}, {"didnt", "didn't"}, {"Didnt", "Didn't"}, {"yall", "y'all"}, {"Yall", "Y'all"}, {"theyre", "they're"}, {"Theyre", "They're"},
                                                 {"havent", "haven't"}, {"Havent", "Haven't"}, {"theres", "there's"}, {"Theres", "There's"}, {"thats", "that's"}, {"Thats", "That's"}, {"hasnt", "hasn't"}, {"Hasnt", "Hasn't"}, {"ive", "I've"}, {"Ive", "I've"}};

    if (key.empty() || key == " ") {
        results = {{" ", " ", " "},
                   {0.0, 0.0, 0.0}};
    } else if (autoreplace.find(key) != autoreplace.end()) {
        results = {{key, autoreplace[key], " "},
                   {0.5, 0.8, 0.0}};
    } else {
        Results result = g_ac->top3(key);
        vector<string> suggestions = result.suggestions[key];
        vector<double> confidences = result.scores[key];

        int case_state = getCaseState(key);

        if (case_state == 1) {
            for (int i = 0; i < 3; ++i) {
                if (suggestions[i].length() > 0) {
                    suggestions[i][0] = toUpper(suggestions[i][0]);
                }
            }
        } else if (case_state == 2) {
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < suggestions[i].length(); ++j) {
                    suggestions[i][j] = toUpper(suggestions[i][j]);
                }
            }
        }

        // Reorder suggestions: {second, first, third}
        vector<string> reordered = {suggestions[1], suggestions[0], suggestions[2]};
        vector<double> reordered_scores = {confidences[1], confidences[0], confidences[2]};

        results = {reordered, reordered_scores};
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