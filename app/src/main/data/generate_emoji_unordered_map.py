import csv

def normalize(word):
    word = word.strip().lower()
    word = "_".join(word.split())
    return word

def split_alt(s):
    if not s:
        return []
    return [x.strip() for x in s.split("|") if x.strip()]

mapping = {}
before_prefix = True

with open("Flag Emojis - Sheet1.csv", newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)

    for row in reader:
        # detect separator row
        first_col = row["Flag"].strip()
        if first_col == "NO PREFIX ONLY":
            before_prefix = False
            continue

        emoji = row["Flag"].strip()
        region = row["Region Code"].strip().lower()
        name = row["Common Country Name"]
        alt = row["Alt Country Names (Sep by |)"]
        chinese = row["Chinese Country Name"]

        words = set()

        if region:
            words.add(region)

        if name:
            words.add(normalize(name))

        for w in split_alt(alt):
            words.add(normalize(w))

        if chinese:
            words.add(normalize(chinese))

        for w in words:
            if before_prefix:
                key = f"?flag_{w}"
            else:
                key = f"?{w}"

            mapping[key] = emoji

# print C++ map
print("unordered_map<string, string> flags = {")
for k, v in sorted(mapping.items()):
    print(f'    {{"{k}", "{v}"}},')
print("};")