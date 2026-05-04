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

bool isUpper(char c) {
    return c >= 'A' && c <= 'Z';
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

int getCaseState(const std::string& word) { // Change it to mean 1 = CapitalTHeNWhAtEvEr, 2 = STANDARD, 3 = Everything else
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
            if (isLower(c)) return 1;
        }

        return 2;
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

std::string normalizeShortcut(const std::string& raw) {
    std::string out;
    for (char c : raw) {
        out.push_back(toLower(c));
    }
    return out;
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
                                                 {"itd", "it'd"}, {"Itd", "It'd"}, {"todays", "today's"}, {"Todays", "Today's"}, {"helll", "he'll"}, {"Helll", "He'll"}, {"shelll", "she'll"}, {"Shelll", "She'll"}, {"weree", "we're"}, {"Weree", "We're"}, {"ll", "//"}, {"Ll", "//"}};

    unordered_set<string> cap_uppercase = {"i", "i'm", "i'd", "i'll", "i've"};

    // Testing purposes only
    unordered_map<string, string> flags = {
            {"?black_flag", "🏴"},
            {"?checkered_flag", "🏁"},
            {"?crossed_flag", "🎌"},
            {"?crossed_flags", "🎌"},
            {"?england", "🏴󠁧󠁢󠁥󠁮󠁧󠁿"},
            {"?finish_flag", "🏁"},
            {"?flag_ac", "🇦🇨"},
            {"?flag_ad", "🇦🇩"},
            {"?flag_ae", "🇦🇪"},
            {"?flag_af", "🇦🇫"},
            {"?flag_afghan", "🇦🇫"},
            {"?flag_afghanistan", "🇦🇫"},
            {"?flag_ag", "🇦🇬"},
            {"?flag_ai", "🇦🇮"},
            {"?flag_al", "🇦🇱"},
            {"?flag_aland", "🇦🇽"},
            {"?flag_alandislands", "🇦🇽"},
            {"?flag_albania", "🇦🇱"},
            {"?flag_algeria", "🇩🇿"},
            {"?flag_am", "🇦🇲"},
            {"?flag_americansamoa", "🇦🇸"},
            {"?flag_andorra", "🇦🇩"},
            {"?flag_angola", "🇦🇴"},
            {"?flag_anguilla", "🇦🇮"},
            {"?flag_antarctica", "🇦🇶"},
            {"?flag_antigua", "🇦🇬"},
            {"?flag_antiguabarbdua", "🇦🇬"},
            {"?flag_ao", "🇦🇴"},
            {"?flag_aq", "🇦🇶"},
            {"?flag_ar", "🇦🇷"},
            {"?flag_argentina", "🇦🇷"},
            {"?flag_armenia", "🇦🇲"},
            {"?flag_aruba", "🇦🇼"},
            {"?flag_as", "🇦🇸"},
            {"?flag_ascension", "🇦🇨"},
            {"?flag_ascensionisland", "🇦🇨"},
            {"?flag_at", "🇦🇹"},
            {"?flag_au", "🇦🇺"},
            {"?flag_aus", "🇦🇺"},
            {"?flag_australia", "🇦🇺"},
            {"?flag_austria", "🇦🇹"},
            {"?flag_aw", "🇦🇼"},
            {"?flag_ax", "🇦🇽"},
            {"?flag_az", "🇦🇿"},
            {"?flag_azerbaijan", "🇦🇿"},
            {"?flag_ba", "🇧🇦"},
            {"?flag_bahamas", "🇧🇸"},
            {"?flag_bahrain", "🇧🇭"},
            {"?flag_bangladesh", "🇧🇩"},
            {"?flag_barbados", "🇧🇧"},
            {"?flag_bb", "🇧🇧"},
            {"?flag_bd", "🇧🇩"},
            {"?flag_be", "🇧🇪"},
            {"?flag_belarus", "🇧🇾"},
            {"?flag_belgium", "🇧🇪"},
            {"?flag_belize", "🇧🇿"},
            {"?flag_benin", "🇧🇯"},
            {"?flag_bermuda", "🇧🇲"},
            {"?flag_bf", "🇧🇫"},
            {"?flag_bg", "🇧🇬"},
            {"?flag_bh", "🇧🇭"},
            {"?flag_bhutan", "🇧🇹"},
            {"?flag_bi", "🇧🇮"},
            {"?flag_bih", "🇧🇦"},
            {"?flag_bj", "🇧🇯"},
            {"?flag_bl", "🇧🇱"},
            {"?flag_black", "🏴"},
            {"?flag_bm", "🇧🇲"},
            {"?flag_bn", "🇧🇳"},
            {"?flag_bo", "🇧🇴"},
            {"?flag_bolivia", "🇧🇴"},
            {"?flag_bosnia", "🇧🇦"},
            {"?flag_bosniaherzegovina", "🇧🇦"},
            {"?flag_botswana", "🇧🇼"},
            {"?flag_bouvet", "🇧🇻"},
            {"?flag_bouvetisland", "🇧🇻"},
            {"?flag_bq", "🇧🇶"},
            {"?flag_br", "🇧🇷"},
            {"?flag_brazil", "🇧🇷"},
            {"?flag_britishindianoceanterritory", "🇮🇴"},
            {"?flag_britishvirginislands", "🇻🇬"},
            {"?flag_brunei", "🇧🇳"},
            {"?flag_bs", "🇧🇸"},
            {"?flag_bt", "🇧🇹"},
            {"?flag_bulgaria", "🇧🇬"},
            {"?flag_burkinafaso", "🇧🇫"},
            {"?flag_burma", "🇲🇲"},
            {"?flag_burundi", "🇧🇮"},
            {"?flag_bv", "🇧🇻"},
            {"?flag_bw", "🇧🇼"},
            {"?flag_by", "🇧🇾"},
            {"?flag_bz", "🇧🇿"},
            {"?flag_ca", "🇨🇦"},
            {"?flag_caboverde", "🇨🇻"},
            {"?flag_cambodia", "🇰🇭"},
            {"?flag_cameroon", "🇨🇲"},
            {"?flag_canada", "🇨🇦"},
            {"?flag_canary", "🇮🇨"},
            {"?flag_canaryislands", "🇮🇨"},
            {"?flag_capeverde", "🇨🇻"},
            {"?flag_car", "🇨🇫"},
            {"?flag_caribbeannetherlands", "🇧🇶"},
            {"?flag_cayman", "🇰🇾"},
            {"?flag_caymanislands", "🇰🇾"},
            {"?flag_cc", "🇨🇨"},
            {"?flag_cd", "🇨🇩"},
            {"?flag_centralafricanrepublic", "🇨🇫"},
            {"?flag_ceutamelilla", "🇪🇦"},
            {"?flag_cf", "🇨🇫"},
            {"?flag_cg", "🇨🇬"},
            {"?flag_ch", "🇨🇭"},
            {"?flag_chad", "🇹🇩"},
            {"?flag_chile", "🇨🇱"},
            {"?flag_china", "🇨🇳"},
            {"?flag_christmasisland", "🇨🇽"},
            {"?flag_ci", "🇨🇮"},
            {"?flag_ck", "🇨🇰"},
            {"?flag_cl", "🇨🇱"},
            {"?flag_clipperton", "🇨🇵"},
            {"?flag_clippertonisland", "🇨🇵"},
            {"?flag_cm", "🇨🇲"},
            {"?flag_cn", "🇨🇳"},
            {"?flag_co", "🇨🇴"},
            {"?flag_cocos", "🇨🇨"},
            {"?flag_cocosislands", "🇨🇨"},
            {"?flag_colombia", "🇨🇴"},
            {"?flag_comors", "🇰🇲"},
            {"?flag_cookislands", "🇨🇰"},
            {"?flag_costarica", "🇨🇷"},
            {"?flag_cotedivoire", "🇨🇮"},
            {"?flag_cp", "🇨🇵"},
            {"?flag_cr", "🇨🇷"},
            {"?flag_croatia", "🇭🇷"},
            {"?flag_cu", "🇨🇺"},
            {"?flag_cuba", "🇨🇺"},
            {"?flag_curacao", "🇨🇼"},
            {"?flag_cv", "🇨🇻"},
            {"?flag_cw", "🇨🇼"},
            {"?flag_cx", "🇨🇽"},
            {"?flag_cy", "🇨🇾"},
            {"?flag_cyprus", "🇨🇾"},
            {"?flag_cz", "🇨🇿"},
            {"?flag_czechia", "🇨🇿"},
            {"?flag_czechrepublic", "🇨🇿"},
            {"?flag_de", "🇩🇪"},
            {"?flag_demrepcongo", "🇨🇩"},
            {"?flag_denmark", "🇩🇰"},
            {"?flag_dg", "🇩🇬"},
            {"?flag_diegogarcia", "🇩🇬"},
            {"?flag_dj", "🇩🇯"},
            {"?flag_djibouti", "🇩🇯"},
            {"?flag_dk", "🇩🇰"},
            {"?flag_dm", "🇩🇲"},
            {"?flag_do", "🇩🇴"},
            {"?flag_dominica", "🇩🇲"},
            {"?flag_dominicanrepublic", "🇩🇴"},
            {"?flag_dprk", "🇰🇵"},
            {"?flag_dr", "🇩🇴"},
            {"?flag_drc", "🇨🇩"},
            {"?flag_dz", "🇩🇿"},
            {"?flag_ea", "🇪🇦"},
            {"?flag_easttimor", "🇹🇱"},
            {"?flag_ec", "🇪🇨"},
            {"?flag_ecuador", "🇪🇨"},
            {"?flag_ee", "🇪🇪"},
            {"?flag_eg", "🇪🇬"},
            {"?flag_egypt", "🇪🇬"},
            {"?flag_eh", "🇪🇭"},
            {"?flag_elsalvador", "🇸🇻"},
            {"?flag_england", "🏴󠁧󠁢󠁥󠁮󠁧󠁿"},
            {"?flag_equatorialguinea", "🇬🇶"},
            {"?flag_er", "🇪🇷"},
            {"?flag_eritrea", "🇪🇷"},
            {"?flag_es", "🇪🇸"},
            {"?flag_estonia", "🇪🇪"},
            {"?flag_eswatini", "🇸🇿"},
            {"?flag_et", "🇪🇹"},
            {"?flag_ethiopia", "🇪🇹"},
            {"?flag_eu", "🇪🇺"},
            {"?flag_europeanunion", "🇪🇺"},
            {"?flag_falkland", "🇫🇰"},
            {"?flag_falklandislands", "🇫🇰"},
            {"?flag_faroe", "🇫🇴"},
            {"?flag_faroeislands", "🇫🇴"},
            {"?flag_fi", "🇫🇮"},
            {"?flag_fiji", "🇫🇯"},
            {"?flag_finland", "🇫🇮"},
            {"?flag_fj", "🇫🇯"},
            {"?flag_fk", "🇫🇰"},
            {"?flag_fm", "🇫🇲"},
            {"?flag_fo", "🇫🇴"},
            {"?flag_fr", "🇫🇷"},
            {"?flag_france", "🇫🇷"},
            {"?flag_frenchguiana", "🇬🇫"},
            {"?flag_frenchpolynesia", "🇵🇫"},
            {"?flag_frenchsouthernterritories", "🇹🇫"},
            {"?flag_ga", "🇬🇦"},
            {"?flag_gabon", "🇬🇦"},
            {"?flag_gambia", "🇬🇲"},
            {"?flag_gb", "🇬🇧"},
            {"?flag_gd", "🇬🇩"},
            {"?flag_ge", "🇬🇪"},
            {"?flag_georgia", "🇬🇪"},
            {"?flag_germany", "🇩🇪"},
            {"?flag_gf", "🇬🇫"},
            {"?flag_gg", "🇬🇬"},
            {"?flag_gh", "🇬🇭"},
            {"?flag_ghana", "🇬🇭"},
            {"?flag_gi", "🇬🇮"},
            {"?flag_gibraltar", "🇬🇮"},
            {"?flag_gl", "🇬🇱"},
            {"?flag_gm", "🇬🇲"},
            {"?flag_gn", "🇬🇳"},
            {"?flag_gp", "🇬🇵"},
            {"?flag_gq", "🇬🇶"},
            {"?flag_gr", "🇬🇷"},
            {"?flag_greece", "🇬🇷"},
            {"?flag_greenland", "🇬🇱"},
            {"?flag_grenada", "🇬🇩"},
            {"?flag_gs", "🇬🇸"},
            {"?flag_gt", "🇬🇹"},
            {"?flag_gu", "🇬🇺"},
            {"?flag_guadeloupe", "🇬🇵"},
            {"?flag_guam", "🇬🇺"},
            {"?flag_guatemala", "🇬🇹"},
            {"?flag_guernsey", "🇬🇬"},
            {"?flag_guinea", "🇬🇳"},
            {"?flag_guineabissau", "🇬🇼"},
            {"?flag_guyana", "🇬🇾"},
            {"?flag_gw", "🇬🇼"},
            {"?flag_gy", "🇬🇾"},
            {"?flag_haiti", "🇭🇹"},
            {"?flag_heardmcd", "🇭🇲"},
            {"?flag_heardmcdonaldislands", "🇭🇲"},
            {"?flag_hk", "🇭🇰"},
            {"?flag_hm", "🇭🇲"},
            {"?flag_hn", "🇭🇳"},
            {"?flag_honduras", "🇭🇳"},
            {"?flag_hongkong", "🇭🇰"},
            {"?flag_hr", "🇭🇷"},
            {"?flag_ht", "🇭🇹"},
            {"?flag_hu", "🇭🇺"},
            {"?flag_hungary", "🇭🇺"},
            {"?flag_ic", "🇮🇨"},
            {"?flag_iceland", "🇮🇸"},
            {"?flag_id", "🇮🇩"},
            {"?flag_ie", "🇮🇪"},
            {"?flag_il", "🇮🇱"},
            {"?flag_im", "🇮🇲"},
            {"?flag_in", "🇮🇳"},
            {"?flag_india", "🇮🇳"},
            {"?flag_indo", "🇮🇩"},
            {"?flag_indonesia", "🇮🇩"},
            {"?flag_io", "🇮🇴"},
            {"?flag_iq", "🇮🇶"},
            {"?flag_ir", "🇮🇷"},
            {"?flag_iran", "🇮🇷"},
            {"?flag_iraq", "🇮🇶"},
            {"?flag_ireland", "🇮🇪"},
            {"?flag_is", "🇮🇸"},
            {"?flag_isleman", "🇮🇲"},
            {"?flag_isleofman", "🇮🇲"},
            {"?flag_israel", "🇮🇱"},
            {"?flag_it", "🇮🇹"},
            {"?flag_italy", "🇮🇹"},
            {"?flag_ivorycoast", "🇨🇮"},
            {"?flag_jamaica", "🇯🇲"},
            {"?flag_japan", "🇯🇵"},
            {"?flag_je", "🇯🇪"},
            {"?flag_jersey", "🇯🇪"},
            {"?flag_jm", "🇯🇲"},
            {"?flag_jo", "🇯🇴"},
            {"?flag_jordan", "🇯🇴"},
            {"?flag_jp", "🇯🇵"},
            {"?flag_kazakhstan", "🇰🇿"},
            {"?flag_ke", "🇰🇪"},
            {"?flag_keeling", "🇨🇨"},
            {"?flag_keelingislands", "🇨🇨"},
            {"?flag_kenya", "🇰🇪"},
            {"?flag_kg", "🇰🇬"},
            {"?flag_kh", "🇰🇭"},
            {"?flag_ki", "🇰🇮"},
            {"?flag_kiribati", "🇰🇮"},
            {"?flag_km", "🇰🇲"},
            {"?flag_kn", "🇰🇳"},
            {"?flag_kosovo", "🇽🇰"},
            {"?flag_kp", "🇰🇵"},
            {"?flag_kr", "🇰🇷"},
            {"?flag_kuwait", "🇰🇼"},
            {"?flag_kw", "🇰🇼"},
            {"?flag_ky", "🇰🇾"},
            {"?flag_kyrgyzstan", "🇰🇬"},
            {"?flag_kz", "🇰🇿"},
            {"?flag_la", "🇱🇦"},
            {"?flag_laos", "🇱🇦"},
            {"?flag_latvia", "🇱🇻"},
            {"?flag_lb", "🇱🇧"},
            {"?flag_lc", "🇱🇨"},
            {"?flag_lebanon", "🇱🇧"},
            {"?flag_lesohto", "🇱🇸"},
            {"?flag_li", "🇱🇮"},
            {"?flag_liberia", "🇱🇷"},
            {"?flag_libya", "🇱🇾"},
            {"?flag_liechtenstein", "🇱🇮"},
            {"?flag_lithuania", "🇱🇹"},
            {"?flag_lk", "🇱🇰"},
            {"?flag_lr", "🇱🇷"},
            {"?flag_ls", "🇱🇸"},
            {"?flag_lt", "🇱🇹"},
            {"?flag_lu", "🇱🇺"},
            {"?flag_luxembourg", "🇱🇺"},
            {"?flag_lv", "🇱🇻"},
            {"?flag_ly", "🇱🇾"},
            {"?flag_ma", "🇲🇦"},
            {"?flag_macao", "🇲🇴"},
            {"?flag_macau", "🇲🇴"},
            {"?flag_madagascar", "🇲🇬"},
            {"?flag_malawi", "🇲🇼"},
            {"?flag_malaysia", "🇲🇾"},
            {"?flag_maldives", "🇲🇻"},
            {"?flag_mali", "🇲🇱"},
            {"?flag_malta", "🇲🇹"},
            {"?flag_marshall", "🇲🇭"},
            {"?flag_marshallislands", "🇲🇭"},
            {"?flag_martinique", "🇲🇶"},
            {"?flag_mauritania", "🇲🇷"},
            {"?flag_mauritius", "🇲🇺"},
            {"?flag_mayotte", "🇾🇹"},
            {"?flag_mc", "🇲🇨"},
            {"?flag_md", "🇲🇩"},
            {"?flag_me", "🇲🇪"},
            {"?flag_mexico", "🇲🇽"},
            {"?flag_mf", "🇲🇫"},
            {"?flag_mg", "🇲🇬"},
            {"?flag_mh", "🇲🇭"},
            {"?flag_micronesia", "🇫🇲"},
            {"?flag_mk", "🇲🇰"},
            {"?flag_ml", "🇲🇱"},
            {"?flag_mm", "🇲🇲"},
            {"?flag_mn", "🇲🇳"},
            {"?flag_mo", "🇲🇴"},
            {"?flag_moldova", "🇲🇩"},
            {"?flag_monaco", "🇲🇨"},
            {"?flag_mongolia", "🇲🇳"},
            {"?flag_montenegro", "🇲🇪"},
            {"?flag_montserrat", "🇲🇸"},
            {"?flag_morocco", "🇲🇦"},
            {"?flag_mozambique", "🇲🇿"},
            {"?flag_mp", "🇲🇵"},
            {"?flag_mq", "🇲🇶"},
            {"?flag_mr", "🇲🇷"},
            {"?flag_ms", "🇲🇸"},
            {"?flag_mt", "🇲🇹"},
            {"?flag_mu", "🇲🇺"},
            {"?flag_mv", "🇲🇻"},
            {"?flag_mw", "🇲🇼"},
            {"?flag_mx", "🇲🇽"},
            {"?flag_my", "🇲🇾"},
            {"?flag_myanmar", "🇲🇲"},
            {"?flag_mz", "🇲🇿"},
            {"?flag_na", "🇳🇦"},
            {"?flag_namibia", "🇳🇦"},
            {"?flag_nauru", "🇳🇷"},
            {"?flag_nc", "🇳🇨"},
            {"?flag_ne", "🇳🇪"},
            {"?flag_nepal", "🇳🇵"},
            {"?flag_netherlands", "🇳🇱"},
            {"?flag_newcaledonia", "🇳🇨"},
            {"?flag_newzealand", "🇳🇿"},
            {"?flag_nf", "🇳🇫"},
            {"?flag_ng", "🇳🇬"},
            {"?flag_ni", "🇳🇮"},
            {"?flag_nicaragua", "🇳🇮"},
            {"?flag_niger", "🇳🇪"},
            {"?flag_nigeria", "🇳🇬"},
            {"?flag_niue", "🇳🇺"},
            {"?flag_nl", "🇳🇱"},
            {"?flag_no", "🇳🇴"},
            {"?flag_norfolk", "🇳🇫"},
            {"?flag_norfolkisland", "🇳🇫"},
            {"?flag_northernmariana", "🇲🇵"},
            {"?flag_northernmarianaislands", "🇲🇵"},
            {"?flag_northkorea", "🇰🇵"},
            {"?flag_northmacedonia", "🇲🇰"},
            {"?flag_norway", "🇳🇴"},
            {"?flag_np", "🇳🇵"},
            {"?flag_nr", "🇳🇷"},
            {"?flag_nu", "🇳🇺"},
            {"?flag_nz", "🇳🇿"},
            {"?flag_om", "🇴🇲"},
            {"?flag_oman", "🇴🇲"},
            {"?flag_pa", "🇵🇦"},
            {"?flag_pakistan", "🇵🇰"},
            {"?flag_palau", "🇵🇼"},
            {"?flag_palestine", "🇵🇸"},
            {"?flag_palestinianterritories", "🇵🇸"},
            {"?flag_panama", "🇵🇦"},
            {"?flag_papuanewguinea", "🇵🇬"},
            {"?flag_paraguay", "🇵🇾"},
            {"?flag_pe", "🇵🇪"},
            {"?flag_peru", "🇵🇪"},
            {"?flag_pf", "🇵🇫"},
            {"?flag_pg", "🇵🇬"},
            {"?flag_ph", "🇵🇭"},
            {"?flag_philippines", "🇵🇭"},
            {"?flag_pitcairn", "🇵🇳"},
            {"?flag_pitcairnislands", "🇵🇳"},
            {"?flag_pk", "🇵🇰"},
            {"?flag_pl", "🇵🇱"},
            {"?flag_pm", "🇵🇲"},
            {"?flag_pn", "🇵🇳"},
            {"?flag_png", "🇵🇬"},
            {"?flag_poland", "🇵🇱"},
            {"?flag_portugal", "🇵🇹"},
            {"?flag_pr", "🇵🇷"},
            {"?flag_prc", "🇨🇳"},
            {"?flag_ps", "🇵🇸"},
            {"?flag_pt", "🇵🇹"},
            {"?flag_puertorico", "🇵🇷"},
            {"?flag_pw", "🇵🇼"},
            {"?flag_py", "🇵🇾"},
            {"?flag_qa", "🇶🇦"},
            {"?flag_qatar", "🇶🇦"},
            {"?flag_re", "🇷🇪"},
            {"?flag_repcongo", "🇨🇬"},
            {"?flag_reunion", "🇷🇪"},
            {"?flag_reunionisland", "🇷🇪"},
            {"?flag_ro", "🇷🇴"},
            {"?flag_roc", "🇹🇼"},
            {"?flag_rok", "🇰🇷"},
            {"?flag_romania", "🇷🇴"},
            {"?flag_rs", "🇷🇸"},
            {"?flag_ru", "🇷🇺"},
            {"?flag_russia", "🇷🇺"},
            {"?flag_rw", "🇷🇼"},
            {"?flag_rwanda", "🇷🇼"},
            {"?flag_sa", "🇸🇦"},
            {"?flag_samoa", "🇼🇸"},
            {"?flag_sanmarino", "🇸🇲"},
            {"?flag_saotome", "🇸🇹"},
            {"?flag_saotomeprincipe", "🇸🇹"},
            {"?flag_saudiarabia", "🇸🇦"},
            {"?flag_sb", "🇸🇧"},
            {"?flag_sc", "🇸🇨"},
            {"?flag_scotland", "🏴󠁧󠁢󠁳󠁣󠁴󠁿"},
            {"?flag_sd", "🇸🇩"},
            {"?flag_se", "🇸🇪"},
            {"?flag_senegal", "🇸🇳"},
            {"?flag_serbia", "🇷🇸"},
            {"?flag_seychelles", "🇸🇨"},
            {"?flag_sg", "🇸🇬"},
            {"?flag_sh", "🇸🇭"},
            {"?flag_si", "🇸🇮"},
            {"?flag_sierraleone", "🇸🇱"},
            {"?flag_singapore", "🇸🇬"},
            {"?flag_sintmaarten", "🇸🇽"},
            {"?flag_sj", "🇸🇯"},
            {"?flag_sk", "🇸🇰"},
            {"?flag_sl", "🇸🇱"},
            {"?flag_slovakia", "🇸🇰"},
            {"?flag_slovenia", "🇸🇮"},
            {"?flag_sm", "🇸🇲"},
            {"?flag_sn", "🇸🇳"},
            {"?flag_so", "🇸🇴"},
            {"?flag_solomon", "🇸🇧"},
            {"?flag_solomonislands", "🇸🇧"},
            {"?flag_somalia", "🇸🇴"},
            {"?flag_southafrica", "🇿🇦"},
            {"?flag_southgeorgia", "🇬🇸"},
            {"?flag_southgeorgiasouthsandwichislands", "🇬🇸"},
            {"?flag_southkorea", "🇰🇷"},
            {"?flag_southsudan", "🇸🇸"},
            {"?flag_spain", "🇪🇸"},
            {"?flag_sr", "🇸🇷"},
            {"?flag_srilanka", "🇱🇰"},
            {"?flag_ss", "🇸🇸"},
            {"?flag_st", "🇸🇹"},
            {"?flag_stbarth", "🇧🇱"},
            {"?flag_stbarthelemy", "🇧🇱"},
            {"?flag_sthelena", "🇸🇭"},
            {"?flag_stkitts", "🇰🇳"},
            {"?flag_stkittsnevis", "🇰🇳"},
            {"?flag_stlucia", "🇱🇨"},
            {"?flag_stmartin", "🇲🇫"},
            {"?flag_stpierremiquelon", "🇵🇲"},
            {"?flag_stvincent", "🇻🇨"},
            {"?flag_stvincentgrenadines", "🇻🇨"},
            {"?flag_sudan", "🇸🇩"},
            {"?flag_suriname", "🇸🇷"},
            {"?flag_sv", "🇸🇻"},
            {"?flag_svalbardjanmayen", "🇸🇯"},
            {"?flag_swaziland", "🇸🇿"},
            {"?flag_sweden", "🇸🇪"},
            {"?flag_switzerland", "🇨🇭"},
            {"?flag_sx", "🇸🇽"},
            {"?flag_sy", "🇸🇾"},
            {"?flag_syria", "🇸🇾"},
            {"?flag_sz", "🇸🇿"},
            {"?flag_ta", "🇹🇦"},
            {"?flag_taiwan", "🇹🇼"},
            {"?flag_tajik", "🇹🇯"},
            {"?flag_tajikistan", "🇹🇯"},
            {"?flag_tanzania", "🇹🇿"},
            {"?flag_tc", "🇹🇨"},
            {"?flag_td", "🇹🇩"},
            {"?flag_tf", "🇹🇫"},
            {"?flag_tg", "🇹🇬"},
            {"?flag_th", "🇹🇭"},
            {"?flag_thailand", "🇹🇭"},
            {"?flag_timorleste", "🇹🇱"},
            {"?flag_tj", "🇹🇯"},
            {"?flag_tk", "🇹🇰"},
            {"?flag_tl", "🇹🇱"},
            {"?flag_tm", "🇹🇲"},
            {"?flag_tn", "🇹🇳"},
            {"?flag_to", "🇹🇴"},
            {"?flag_togo", "🇹🇬"},
            {"?flag_tokelau", "🇹🇰"},
            {"?flag_tonga", "🇹🇴"},
            {"?flag_tr", "🇹🇷"},
            {"?flag_trinidad", "🇹🇹"},
            {"?flag_trinidadtobago", "🇹🇹"},
            {"?flag_tristandacuhna", "🇹🇦"},
            {"?flag_tt", "🇹🇹"},
            {"?flag_tunisia", "🇹🇳"},
            {"?flag_turkiye", "🇹🇷"},
            {"?flag_turkmen", "🇹🇲"},
            {"?flag_turkmenistan", "🇹🇲"},
            {"?flag_turkscaicos", "🇹🇨"},
            {"?flag_turkscaicosislands", "🇹🇨"},
            {"?flag_tuvalu", "🇹🇻"},
            {"?flag_tv", "🇹🇻"},
            {"?flag_tw", "🇹🇼"},
            {"?flag_tz", "🇹🇿"},
            {"?flag_ua", "🇺🇦"},
            {"?flag_uae", "🇦🇪"},
            {"?flag_ug", "🇺🇬"},
            {"?flag_uganda", "🇺🇬"},
            {"?flag_uk", "🇬🇧"},
            {"?flag_ukraine", "🇺🇦"},
            {"?flag_um", "🇺🇲"},
            {"?flag_un", "🇺🇳"},
            {"?flag_unitedarabemirates", "🇦🇪"},
            {"?flag_unitedkingdom", "🇬🇧"},
            {"?flag_unitednations", "🇺🇳"},
            {"?flag_unitedstates", "🇺🇸"},
            {"?flag_uruguay", "🇺🇾"},
            {"?flag_us", "🇺🇸"},
            {"?flag_usa", "🇺🇸"},
            {"?flag_usoutlyingislands", "🇺🇲"},
            {"?flag_usvirginislands", "🇻🇮"},
            {"?flag_uy", "🇺🇾"},
            {"?flag_uz", "🇺🇿"},
            {"?flag_uzbek", "🇺🇿"},
            {"?flag_uzbekistan", "🇺🇿"},
            {"?flag_va", "🇻🇦"},
            {"?flag_vanuatu", "🇻🇺"},
            {"?flag_vatican", "🇻🇦"},
            {"?flag_vaticancity", "🇻🇦"},
            {"?flag_vc", "🇻🇨"},
            {"?flag_ve", "🇻🇪"},
            {"?flag_venezuela", "🇻🇪"},
            {"?flag_vg", "🇻🇬"},
            {"?flag_vi", "🇻🇮"},
            {"?flag_vietnam", "🇻🇳"},
            {"?flag_vn", "🇻🇳"},
            {"?flag_vu", "🇻🇺"},
            {"?flag_wales", "🏴󠁧󠁢󠁷󠁬󠁳󠁿"},
            {"?flag_wallisfutuna", "🇼🇫"},
            {"?flag_westernsahara", "🇪🇭"},
            {"?flag_wf", "🇼🇫"},
            {"?flag_white", "🏳️"},
            {"?flag_ws", "🇼🇸"},
            {"?flag_xk", "🇽🇰"},
            {"?flag_xmas", "🇨🇽"},
            {"?flag_ye", "🇾🇪"},
            {"?flag_yemen", "🇾🇪"},
            {"?flag_yt", "🇾🇹"},
            {"?flag_za", "🇿🇦"},
            {"?flag_zambia", "🇿🇲"},
            {"?flag_zimbabwe", "🇿🇼"},
            {"?flag_zm", "🇿🇲"},
            {"?flag_zuidafrika", "🇿🇦"},
            {"?flag_zw", "🇿🇼"},
            {"?gay_flag", "🏳️‍🌈"},
            {"?lgbt_flag", "🏳️‍🌈"},
            {"?lgbtq_flag", "🏳️‍🌈"},
            {"?pirate_flag", "🏴‍☠️"},
            {"?rainbow_flag", "🏳️‍🌈"},
            {"?red_flag", "🚩"},
            {"?scotland", "🏴󠁧󠁢󠁳󠁣󠁴󠁿"},
            {"?trans_flag", "🏳️‍⚧️"},
            {"?transgender_flag", "🏳️‍⚧️"},
            {"?triangular_flag_on_post", "🚩"},
            {"?wales", "🏴󠁧󠁢󠁷󠁬󠁳󠁿"},
            {"?white_flag", "🏳️"},
    };

    // Load user contractions and merge into autoreplace
    auto userContractions = loadContractions(cpath);

    autoreplace.insert(userContractions.begin(), userContractions.end());

    if (key.empty() || key == " ") {
        results = {{" ", prefixSymbols + " ", " "},
                   {0.0, 0.0, 0.0}};
    } else {
        std::string full_key = prefixSymbols + key;
        std::string lower_full = normalizeShortcut(full_key);

        size_t shortcut_start = lower_full.rfind('?');
        bool found_flag = false;

        if (shortcut_start != std::string::npos) {
            std::string before_shortcut = full_key.substr(0, shortcut_start);
            std::string shortcut = lower_full.substr(shortcut_start);

            auto it = flags.find(shortcut);

            if (it != flags.end()) {
                results = {{before_shortcut + it->first, before_shortcut + it->second, " "},
                           {0.5, 1.0, 0.0}};
                found_flag = true;
            }
        }

        if (!found_flag) {
            if (autoreplace.find(key) != autoreplace.end()) {
                string autoreplaced = autoreplace[key];

                if (autocap && (cap_uppercase.find(autoreplaced) != cap_uppercase.end())) {
                    autoreplaced[0] = toUpper(autoreplaced[0]);
                }

                results = {{(key == "i" && !autocap ? " " : prefixSymbols + key),
                                   prefixSymbols + autoreplaced, " "},
                           {0.5,   0.8,                          0.0}};
            } else if (autoreplace.find(normalizeForAc(key)) != autoreplace.end()) {
                string autoreplaced = autoreplace[normalizeForAc(key)]; // always "rubik's"
                int case_state = getCaseState(key);
                if (case_state == 1) {
                    // Capitalize first letter: "Rubik's"
                    autoreplaced[0] = toUpper(autoreplaced[0]);
                } else if (case_state == 2) {
                    for (char &c: autoreplaced) c = toUpper(c);
                } else if (autocap && cap_uppercase.find(autoreplaced) != cap_uppercase.end()) {
                    autoreplaced[0] = toUpper(autoreplaced[0]);
                }
                results = {{prefixSymbols + key, prefixSymbols + autoreplaced, " "},
                           {0.5,                 0.8,                          0.0}};
            } else {
                Results result = g_ac->top3(key);
                vector<string> suggestions = result.suggestions[key];
                vector<double> confidences = result.scores[key];

                int case_state = getCaseState(key);

                if (case_state == 1) { // Capital beginning
                    for (int i = 0; i < 3; ++i) {
                        if (strToLower(suggestions[i]) ==
                            strToLower(key)) { // Don't autocorrect correct spelling
                            suggestions[i] = key;
                        } else if (!suggestions[i].empty()) {
                            suggestions[i][0] = toUpper(suggestions[i][0]);
                        }
                    }
                } else if (case_state == 2) { // All caps
                    for (int i = 0; i < 3; ++i) {
                        for (int j = 0; j < suggestions[i].length(); ++j) {
                            suggestions[i][j] = toUpper(suggestions[i][j]);
                        }
                    }
                } else if (case_state == 0) { // Any other/lower
                    for (int i = 0; i < 3; ++i) {
                        if (strToLower(suggestions[i]) ==
                            strToLower(key)) { // Don't autocorrect correct spelling
                            suggestions[i] = key;
                        } else {
                            for (int j = 0; j < suggestions[i].length(); ++j) {
                                suggestions[i][j] = toLower(suggestions[i][j]);
                            }
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
                            for (char &c: autoreplaced) c = toUpper(c);
                        } else if (case_state == 1 && !autoreplaced.empty()) {
                            autoreplaced[0] = toUpper(autoreplaced[0]);
                        } else if (autocap &&
                                   cap_uppercase.find(autoreplaced) != cap_uppercase.end()) {
                            autoreplaced[0] = toUpper(autoreplaced[0]);
                        } else if (case_state == 0 && !autoreplaced.empty()) {
                            bool pureAlpha = true;
                            for (char c: autoreplaced) {
                                if (!std::isalpha(static_cast<unsigned char>(c))) {
                                    pureAlpha = false;
                                    break;
                                }
                            }
                            if (pureAlpha) {
                                for (char &c: autoreplaced) c = toLower(c);
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
                    reordered = {prefixSymbols + key, prefixSymbols + suggestions[0],
                                 prefixSymbols + suggestions[1]};
                    reordered_scores = {0, confidences[0], confidences[1]};
                } else {
                    reordered = {prefixSymbols + suggestions[1], prefixSymbols + suggestions[0],
                                 prefixSymbols + suggestions[2]};
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
        }
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
