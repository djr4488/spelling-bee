#!/usr/bin/env python3
"""Generates the small feedback sounds in assets/sounds/ (no external assets needed)."""
import math
import struct
import wave
from pathlib import Path

RATE = 22050
OUT = Path(__file__).resolve().parent.parent / "assets" / "sounds"


def tone(freq, seconds, volume=0.4):
    n = int(RATE * seconds)
    out = []
    for i in range(n):
        t = i / RATE
        env = min(1.0, i / (RATE * 0.01)) * math.exp(-4.0 * t / seconds)  # quick attack, soft decay
        s = math.sin(2 * math.pi * freq * t) + 0.3 * math.sin(4 * math.pi * freq * t)
        out.append(volume * env * s / 1.3)
    return out


def write(name, samples):
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in samples))


write("correct.wav", tone(659, 0.12) + tone(784, 0.12) + tone(1047, 0.35))  # E5 G5 C6, cheerful
write("wrong.wav", tone(330, 0.18, 0.3) + tone(262, 0.35, 0.3))             # E4 C4, gentle "oops"
print("wrote", ", ".join(p.name for p in sorted(OUT.glob("*.wav"))))
