package com.spellingbee.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lines up what was typed with the correct spelling (fewest edits) and marks
 * each letter as matching or not, so the differences can be highlighted.
 */
public final class LetterDiff {
    /** One displayed character and whether it matches the other spelling. */
    public static final class Mark {
        public final char ch;
        public final boolean ok;

        Mark(char ch, boolean ok) {
            this.ch = ch;
            this.ok = ok;
        }

        @Override
        public String toString() {
            return ok ? String.valueOf(ch) : "[" + ch + "]";
        }
    }

    public final List<Mark> typed;
    public final List<Mark> correct;

    private LetterDiff(List<Mark> typed, List<Mark> correct) {
        this.typed = Collections.unmodifiableList(typed);
        this.correct = Collections.unmodifiableList(correct);
    }

    public static LetterDiff of(String typedText, String correctText) {
        String a = typedText == null ? "" : typedText.trim();
        String b = correctText;
        String na = AnswerChecker.normalize(a);
        String nb = AnswerChecker.normalize(b);
        // Normalizing can change length (accents); fall back to raw text then.
        String ka = na.length() == a.length() ? na : a.toLowerCase();
        String kb = nb.length() == b.length() ? nb : b.toLowerCase();

        int n = a.length(), m = b.length();
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) d[i][0] = i;
        for (int j = 0; j <= m; j++) d[0][j] = j;
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int sub = d[i - 1][j - 1] + (ka.charAt(i - 1) == kb.charAt(j - 1) ? 0 : 1);
                d[i][j] = Math.min(sub, Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1));
            }
        }

        List<Mark> ta = new ArrayList<>(), tb = new ArrayList<>();
        int i = n, j = m;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && ka.charAt(i - 1) == kb.charAt(j - 1) && d[i][j] == d[i - 1][j - 1]) {
                ta.add(new Mark(a.charAt(i - 1), true));
                tb.add(new Mark(b.charAt(j - 1), true));
                i--;
                j--;
            } else if (i > 0 && j > 0 && d[i][j] == d[i - 1][j - 1] + 1) {
                ta.add(new Mark(a.charAt(i - 1), false)); // wrong letter
                tb.add(new Mark(b.charAt(j - 1), false));
                i--;
                j--;
            } else if (i > 0 && d[i][j] == d[i - 1][j] + 1) {
                ta.add(new Mark(a.charAt(i - 1), false)); // extra letter typed
                i--;
            } else {
                tb.add(new Mark(b.charAt(j - 1), false)); // letter left out
                j--;
            }
        }
        Collections.reverse(ta);
        Collections.reverse(tb);
        return new LetterDiff(ta, tb);
    }

    /** Compact text form for tests and logs, e.g. "ne[s]essary". */
    public static String describe(List<Mark> marks) {
        StringBuilder sb = new StringBuilder();
        for (Mark m : marks) sb.append(m);
        return sb.toString();
    }
}
