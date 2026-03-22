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
using std::unordered_set;

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

char toLower(char c) {
    if (c >= 'A' && c <= 'Z') {
        return c - 'A' + 'a';
    } else {
        return c;
    }
}

std::string strToLower(const std::string& word) {
    std::string ans = "";

    for (int i = 0; i < word.length(); ++i) {
        ans += toLower(word[i]);
    }

    return ans;
}

int getCaseState(const std::string& word) { // Change it to mean 1 = Standard, 2 = STANDARD, 3 = Everything else
    if (word.length() < 2) {
        if (isLower(word[0])) {
            return 0;
        } else {
            return 2;
        }
    } else {
        if (isLower(word[0])) {
            return 0;
        }

        bool is_lower = false;
        bool found_second = false;
        for (int i = 1; i < word.length(); ++i) {
            char c = word[i];
            if (!isLower(c) && !(c >= 'A' && c <= 'Z')) continue; // skip non-letters
            is_lower = isLower(c);
            found_second = true;
            break;
        }
        if (!found_second) return 2; // only one letter

        for (int i = 1; i < word.length(); ++i) {
            char c = word[i];
            if (!isLower(c) && !(c >= 'A' && c <= 'Z')) continue; // skip non-letters
            if (isLower(c) != is_lower) return 0;
        }

        return is_lower ? 1 : 2;
    }
}

std::string normalizeForAc(const std::string& raw) {
    std::string out;
    for (char c : raw) {
        if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
            out.push_back(toLower(c));
        }
    }
    return out;
}

std::unordered_map<std::string, std::string> loadContractions(const std::string& path) {
    std::unordered_map<std::string, std::string> map;
    std::ifstream file(path);
    if (!file.is_open()) return map;
    std::string line;
    while (std::getline(file, line)) {
        line.erase(line.find_last_not_of("\r\n") + 1);
        size_t eq = line.find('=');
        if (eq != std::string::npos) {
            map[line.substr(0, eq)] = line.substr(eq + 1);
        }
    }
    return map;
}

void saveContraction(const std::string& path, const std::string& key, const std::string& value) {
    std::ofstream file(path, std::ios::app);
    if (file.is_open()) {
        file << key << "=" << value << "\n";
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_fqhll_keyboard_CustomKeyboardApp_nativeAddWord(JNIEnv* env, jclass, jstring jword, jstring jpath, jstring jcontractionpath) {
    if (!g_ac) return;

    const char* c_word = env->GetStringUTFChars(jword, nullptr);
    std::string raw(c_word);
    env->ReleaseStringUTFChars(jword, c_word);

    const char* c_cpath = env->GetStringUTFChars(jcontractionpath, nullptr);
    std::string cpath(c_cpath);
    env->ReleaseStringUTFChars(jcontractionpath, c_cpath);

    std::string word = normalizeForAc(raw);
    if (word.empty()) {
        return;
    }
    bool hasNonLetters = (word.length() < raw.length()) && !raw.empty();
    if (hasNonLetters) {
        std::string key = normalizeForAc(raw); // "shun4midx" or "rubiks"

        // Strip LEADING non-alpha chars from raw to get display value
        std::string displayRaw = raw;
        size_t firstAlpha = 0;
        while (firstAlpha < displayRaw.size() &&
               !(('a' <= displayRaw[firstAlpha] && displayRaw[firstAlpha] <= 'z') ||
                 ('A' <= displayRaw[firstAlpha] && displayRaw[firstAlpha] <= 'Z'))) {
            firstAlpha++;
        }
        displayRaw = displayRaw.substr(firstAlpha); // "shun4midx" stays, "4shun4midx" becomes "shun4midx"

        // Lowercase it
        std::string lowerDisplay = displayRaw;
        for (char& c : lowerDisplay) c = toLower(c);

        std::ofstream create(cpath, std::ios::app);
        create.close();
        saveContraction(cpath, key, lowerDisplay);
    }

    g_ac->add_dictionary(word);
    g_ac->save_dictionary();

    // Append to dictionary file
    const char* c_path = env->GetStringUTFChars(jpath, nullptr);
    std::string path(c_path);
    env->ReleaseStringUTFChars(jpath, c_path);

    std::ofstream file(path, std::ios::app);
    if (file.is_open()) {
        file << word << "\n";
    }
}

void removeContraction(const std::string& path, const std::string& key) {
    std::ifstream in(path);
    if (!in.is_open()) return;
    std::vector<std::string> lines;
    std::string line;
    while (std::getline(in, line)) {
        line.erase(line.find_last_not_of("\r\n") + 1);
        // keep lines that don't start with "key="
        if (line.substr(0, key.size() + 1) != key + "=") {
            lines.push_back(line);
        }
    }
    in.close();
    std::ofstream out(path);
    for (const auto& l : lines) out << l << "\n";
}

extern "C"
JNIEXPORT void JNICALL
Java_com_fqhll_keyboard_CustomKeyboardApp_nativeRemoveWord(JNIEnv* env, jclass, jstring jword, jstring jpath, jstring jcontractionpath) {
    if (!g_ac) return;

    const char* c_word = env->GetStringUTFChars(jword, nullptr);
    std::string raw(c_word);
    env->ReleaseStringUTFChars(jword, c_word);

    // convert to valid form
    std::string word = normalizeForAc(raw);
    if (word.empty()) {
        return;
    }

    g_ac->remove_dictionary(word);
    g_ac->save_dictionary();

    // Rewrite the file without this word
    const char* c_path = env->GetStringUTFChars(jpath, nullptr);
    std::string path(c_path);
    env->ReleaseStringUTFChars(jpath, c_path);

    std::ifstream in(path);
    std::vector<std::string> lines;
    std::string line;

    while (std::getline(in, line)) {
        if (line != word && !line.empty()) {
            lines.push_back(line);
        }
    }
    in.close();

    std::ofstream out(path);
    for (const auto& l : lines) {
        out << l << "\n";
    }

    // Remove from contraction path too
    const char* c_cpath = env->GetStringUTFChars(jcontractionpath, nullptr);
    std::string cpath(c_cpath);
    env->ReleaseStringUTFChars(jcontractionpath, c_cpath);

    // Remove from contractions file regardless
    removeContraction(cpath, word);
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
        jstring prefix,
        jboolean jautocap,
        jstring jcontractionpath) {

    bool autocap = (jautocap == JNI_TRUE);

    if (!g_ac) return nullptr;

    // Convert jstring -> std::string
    const char *p = env->GetStringUTFChars(prefix, nullptr);
    std::string key(p);
    env->ReleaseStringUTFChars(prefix, p);

    const char* c_cpath = env->GetStringUTFChars(jcontractionpath, nullptr);
    std::string cpath(c_cpath);
    env->ReleaseStringUTFChars(jcontractionpath, c_cpath);

    // Extract symbol prefix (non-letters at start)
    std::string prefixSymbols;
    while (!key.empty() && !std::isalpha(static_cast<unsigned char>(key[0]))) {
        prefixSymbols.push_back(key[0]);
        key.erase(0, 1);
    }

    // Real logic: fill a vector of {text, confidence}
    pair<vector<string>, vector<double>> results;

    unordered_map<string, string> autoreplace = {{"i", "i"}, {"im", "i'm"}, {"Im", "I'm"}, {"id", "i'd"}, {"Id", "I'd"}, {"youd", "you'd"}, {"Youd", "You'd"}, {"youll", "you'll"}, {"Youll", "You'll"}, {"isnt", "isn't"}, {"Isnt", "Isn't"},
                                                 {"wasnt", "wasn't"}, {"Wasnt", "Wasn't"}, {"arent", "aren't"}, {"Arent", "Aren't"}, {"illl", "i'll"}, {"Illl", "I'll"}, {"doesnt", "doesn't"}, {"Doesnt", "Doesn't"}, {"dont", "don't"}, {"Dont", "Don't"},
                                                 {"wont", "won't"}, {"Wont", "Won't"}, {"hes", "he's"}, {"Hes", "He's"}, {"shes", "she's"}, {"Shes", "She's"}, {"its", "it's"}, {"Its",  "It's"}, {"itss", "its"}, {"Itss", "Its"}, {"letss", "let's"}, {"Letss", "Let's"},
                                                 {"hed", "he'd"}, {"Hed", "He'd"}, {"shedd", "she'd"}, {"Shedd", "She'd"}, {"aint", "ain't"}, {"Aint", "Ain't"}, {"cant", "can't"}, {"Cant", "Can't"}, {"shouldnt", "shouldn't"}, {"Shouldnt", "Shouldn't"},
                                                 {"couldnt", "couldn't"}, {"Couldnt", "Couldn't"}, {"wouldnt", "wouldn't"}, {"Wouldnt", "Wouldn't"}, {"didnt", "didn't"}, {"Didnt", "Didn't"}, {"yall", "y'all"}, {"Yall", "Y'all"}, {"theyre", "they're"}, {"Theyre", "They're"},
                                                 {"havent", "haven't"}, {"Havent", "Haven't"}, {"theres", "there's"}, {"Theres", "There's"}, {"thats", "that's"}, {"Thats", "That's"}, {"hasnt", "hasn't"}, {"Hasnt", "Hasn't"}, {"ive", "i've"}, {"Ive", "I've"},
                                                 {"youre", "you're"}, {"Youre", "You're"}, {"youve", "you've"}, {"Youve", "You've"}, {"whats", "what's"}, {"Whats", "What's"}, {"theyll", "they'll"}, {"Theyll", "They'll"}, {"welll", "we'll"}, {"Welll", "We'll"},
                                                 {"shouldve", "should've"}, {"Shouldve", "Should've"}, {"wouldve", "would've"}, {"Wouldve", "Would've"}, {"hows", "how's"}, {"Hows", "How's"}, {"theyd", "they'd"}, {"Theyd", "They'd"}, {"thatll", "that'll"}, {"Thatll", "That'll"}, {"werent", "weren't"}, {"Werent", "Weren't"}, {"whys", "why's"}, {"Whys", "Why's"}, {"theyve", "they've"}, {"Theyve", "They've"},
                                                 {"itll", "it'll"}, {"Itll", "It'll"}, {"howd", "how'd"}, {"Howd", "How'd"}, {"whod", "who'd"}, {"Whod", "Who'd"}, {"whos", "who's"}, {"Whos", "Who's"}, {"heres", "here's"}, {"Heres", "Here's"}, {"wheres", "where's"}, {"Wheres", "Where's"}, {"whens", "when's"}, {"Whens", "When's"}, {"hadnt", "hadn't"}, {"Hadnt", "Hadn't"},
                                                 {"itd", "it'd"}, {"Itd", "It'd"}, {"todays", "today's"}, {"Todays", "Today's"}, {"helll", "he'll"}, {"Helll", "He'll"}, {"shelll", "she'll"}, {"Shelll", "She'll"}, {"ll", "//"}, {"Ll", "//"}};

    unordered_set<string> cap_uppercase = {"i", "i'm", "i'd", "i'll", "i've"};

    // Load user contractions and merge into autoreplace
    auto userContractions = loadContractions(cpath);

    autoreplace.insert(userContractions.begin(), userContractions.end());

    if (key.empty() || key == " ") {
        results = {{" ", prefixSymbols + " ", " "},
                   {0.0, 0.0, 0.0}};
    } else if (autoreplace.find(key) != autoreplace.end()) {
        string autoreplaced = autoreplace[key];

        if (autocap && (cap_uppercase.find(autoreplaced) != cap_uppercase.end())) {
            autoreplaced[0] = toUpper(autoreplaced[0]);
        }

        results = {{(key == "i" && !autocap ? " " : prefixSymbols + key), prefixSymbols + autoreplaced, " "},
                   {0.5, 0.8, 0.0}};
    } else if (autoreplace.find(normalizeForAc(key)) != autoreplace.end()) {
        string autoreplaced = autoreplace[normalizeForAc(key)]; // always "rubik's"
        int case_state = getCaseState(key);
        if (case_state == 1) {
            // Capitalize first letter: "Rubik's"
            autoreplaced[0] = toUpper(autoreplaced[0]);
        } else if (case_state == 2) {
            for (char& c : autoreplaced) c = toUpper(c);
        } else if (autocap && cap_uppercase.find(autoreplaced) != cap_uppercase.end()) {
            autoreplaced[0] = toUpper(autoreplaced[0]);
        }
        results = {{prefixSymbols + key, prefixSymbols + autoreplaced, " "},
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
        } else if (case_state == 0) { // Any other/lower
            for (int i = 0; i < 3; ++i) {
                if (suggestions[i] == strToLower(key)) { // Don't autocorrect correct spelling
                    suggestions[i] = key;
                }
            }
        }

        // Replace autocorrect words with their suggestions
        for (int i = 0; i < 3; ++i) {
            string normalized = normalizeForAc(suggestions[i]);
            string autoreplaced;
            bool found = false;

            if (autoreplace.find(suggestions[i]) != autoreplace.end()) {
                autoreplaced = autoreplace[suggestions[i]];
                found = true;
            } else if (autoreplace.find(normalized) != autoreplace.end()) {
                autoreplaced = autoreplace[normalized];
                found = true;
            }

            if (found) {
                if (case_state == 2 && !autoreplaced.empty()) {
                    for (char& c : autoreplaced) c = toUpper(c);
                } else if (case_state == 1 && !autoreplaced.empty()) {
                    autoreplaced[0] = toUpper(autoreplaced[0]);
                } else if (autocap && cap_uppercase.find(autoreplaced) != cap_uppercase.end()) {
                    autoreplaced[0] = toUpper(autoreplaced[0]);
                } else if (case_state == 0 && !autoreplaced.empty()) {
                    bool pureAlpha = true;
                    for (char c : autoreplaced) {
                        if (!std::isalpha(static_cast<unsigned char>(c))) { pureAlpha = false; break; }
                    }
                    if (pureAlpha) {
                        for (char& c : autoreplaced) c = toLower(c);
                    }
                }
                suggestions[i] = autoreplaced;
            }
            // else leave suggestions[i] unchanged
        }

        // Reorder suggestions: {second, first, third}, reorder again for high accuracy added response
        vector<string> reordered;
        vector<double> reordered_scores;

        if (confidences[0] >= 0.6 && suggestions[0] != key) {
            reordered = {prefixSymbols + key, prefixSymbols + suggestions[0], prefixSymbols + suggestions[1]};
            reordered_scores = {0, confidences[0], confidences[1]};
        } else {
            reordered = {prefixSymbols + suggestions[1], prefixSymbols + suggestions[0], prefixSymbols + suggestions[2]};
            reordered_scores = {confidences[1], confidences[0], confidences[2]};
        }

//        if (reordered[0] == "" && reordered[2] != "") {
//            reordered[0] = reordered[2];
//            reordered_scores[0] = reordered_scores[2];
//
//            reordered[2] = "";
//            reordered_scores[2] = 0;
//        }

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

extern "C"
JNIEXPORT void JNICALL
Java_com_fqhll_keyboard_CustomKeyboardApp_nativeSetLayout(JNIEnv* env, jclass, jstring jlayout, jstring jpath) {
    if (!g_ac) return;

    const char* c_path = env->GetStringUTFChars(jpath, nullptr);
    std::string path(c_path);
    env->ReleaseStringUTFChars(jpath, c_path);

    const char* c_layout = env->GetStringUTFChars(jlayout, nullptr);
    std::string layout(c_layout);
    env->ReleaseStringUTFChars(jlayout, c_layout);

    AutocorrectorCfg cfg;
    cfg.dictionary_list = getWords(path);
    cfg.keyboard = layout;
    g_ac = std::make_unique<Autocorrector>(cfg);
}
