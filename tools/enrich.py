#!/usr/bin/env python3
"""
Builds assets/words.json from the raw dictionary plus hand-written content.

    python3 tools/enrich.py            # merge + validate, write assets/words.json
    python3 tools/enrich.py --check    # validate only, write nothing

Inputs
  assets/words.txt        Raw word list, comma separated (may span lines).
  tools/content/*.txt     Generated content, one word per line, pipe separated:

      word | partOfSpeech | definition | sentence | pronunciation | speakAs | alts | flags | note

    - speakAs:  optional respelling fed to text-to-speech instead of the word
                (only where engines mispronounce it). Empty = none.
    - alts:     accepted alternate pronunciations as speakable respellings,
                separated by ';'. Empty = none.
    - flags:    any of H (homophone), R (needs review), comma separated.
    - note:     free text (why it needs review, what it sounds like, ...).
    Lines starting with '#' and blank lines are ignored.

Adding words: append them to words.txt, run this script; it lists every word
that has no content yet. Add lines for them to a file in tools/content/ and
run it again.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "assets" / "words.txt"
CONTENT_DIR = ROOT / "tools" / "content"
OUT = ROOT / "assets" / "words.json"

FIELDS = ["word", "partOfSpeech", "definition", "sentence", "pronunciation",
          "speakAs", "alts", "flags", "note"]

SUFFIXES = ["ness", "ing", "ies", "ied", "ed", "es", "ly", "er", "est", "s", "y", "e"]


def read_raw():
    words, seen, dupes = [], set(), []
    for token in RAW.read_text(encoding="utf-8").replace("\n", ",").split(","):
        w = " ".join(token.split())
        if not w:
            continue
        if w.lower() in seen:
            dupes.append(w)
            continue
        seen.add(w.lower())
        words.append(w)
    return words, dupes


def read_content():
    content = {}
    for path in sorted(CONTENT_DIR.glob("*.txt")):
        for n, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            parts = [p.strip() for p in line.split("|")]
            if len(parts) != len(FIELDS):
                sys.exit(f"{path.name}:{n}: expected {len(FIELDS)} fields, got {len(parts)}")
            row = dict(zip(FIELDS, parts))
            key = row["word"].lower()
            if key in content:
                sys.exit(f"{path.name}:{n}: duplicate content for '{row['word']}'")
            content[key] = row
    return content


def to_entry(row):
    flags = {f.strip().upper() for f in row["flags"].split(",") if f.strip()}
    entry = {
        "word": row["word"],
        "partOfSpeech": row["partOfSpeech"],
        "definition": row["definition"],
        "sentence": row["sentence"],
        "pronunciation": row["pronunciation"],
        "speakAs": row["speakAs"] or None,
        "altPronunciations": [a.strip() for a in row["alts"].split(";") if a.strip()],
    }
    if "H" in flags:
        entry["homophone"] = True
    if "R" in flags:
        entry["needsReview"] = True
    if row["note"]:
        entry["note"] = row["note"]
    return entry


def stem(word):
    w = word.lower()
    for suffix in SUFFIXES:
        if w.endswith(suffix) and len(w) - len(suffix) >= 4:
            return w[: -len(suffix)]
    return w


def sentence_has_word(word, sentence):
    """Exact word (or phrase). Lowercase words may be capitalised at sentence start."""
    flags = 0 if word[0].isupper() else re.IGNORECASE
    return re.search(r"(?<![\w-])" + re.escape(word) + r"(?![\w-])", sentence, flags) is not None


def definition_leaks(word, definition):
    d = definition.lower()
    for part in word.lower().replace("-", " ").split():
        if len(part) < 3:  # "a" in "a cappella"
            continue
        s = stem(part)
        if re.search(r"\b" + re.escape(s), d) if len(s) >= 4 else re.search(r"\b" + re.escape(part) + r"\b", d):
            return True
    return False


def validate(entries):
    problems = []
    for e in entries:
        w = e["word"]
        for field in ["word", "partOfSpeech", "definition", "sentence", "pronunciation"]:
            if not e.get(field):
                problems.append(f"{w}: missing {field}")
        if e.get("sentence") and not sentence_has_word(w, e["sentence"]):
            problems.append(f"{w}: sentence does not contain the exact word")
        if e.get("definition") and definition_leaks(w, e["definition"]):
            problems.append(f"{w}: definition contains the word or a form of it")
    return problems


def main():
    check_only = "--check" in sys.argv
    words, dupes = read_raw()
    content = read_content()

    missing = [w for w in words if w.lower() not in content]
    unused = [c["word"] for k, c in content.items() if k not in {w.lower() for w in words}]
    entries = []
    for w in words:
        row = content.get(w.lower())
        if row:
            row = dict(row, word=w)  # keep the dictionary's spelling/case
            entries.append(to_entry(row))

    problems = validate(entries)

    print(f"{len(words)} words ({len(dupes)} duplicates skipped: {', '.join(dupes) or '-'})")
    if unused:
        print(f"Content for words not in words.txt (ignored): {', '.join(unused)}")
    if missing:
        print(f"\n{len(missing)} words have no content yet:\n  " + ", ".join(missing))
    if problems:
        print(f"\n{len(problems)} validation problems:\n  " + "\n  ".join(problems))
    review = [e["word"] for e in entries if e.get("needsReview")]
    if review:
        print(f"\nFlagged needsReview ({len(review)}): {', '.join(review)}")

    if missing or problems:
        sys.exit(1)
    if not check_only:
        OUT.write_text(json.dumps(entries, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"\nWrote {len(entries)} entries to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
