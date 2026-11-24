package com.spellingbee.speech;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PronouncerTest {
    @Test
    void spellsOutLettersWithNamesSpeechEnginesReadCorrectly() {
        // Not "ay" (read as "eye") or "eff" (read as "E F F").
        assertEquals("see, A, tee", Pronouncer.spellOut("cat"));
        assertEquals("ef, A, en", Pronouncer.spellOut("Fan"));
    }

    @Test
    void spellsOutSpacesHyphensAndApostrophes() {
        assertEquals("ex, space, why, hyphen, zee, apostrophe, ess", Pronouncer.spellOut("x y-z's"));
    }
}
