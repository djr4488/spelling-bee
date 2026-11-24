package com.spellingbee.lwjgl3;

import com.spellingbee.speech.SpeechService;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Speaks by running the operating system's speech program on a background
 * thread: espeak-ng (or spd-say) on Linux, {@code say} on macOS and
 * System.Speech via PowerShell on Windows.
 */
public class DesktopSpeechService implements SpeechService {
    private static final long PAUSE_BETWEEN_PARTS_MS = 600;

    private final Engine engine;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "speech");
        t.setDaemon(true);
        return t;
    });
    /** Bumped on every new request; queued work for an older generation is dropped. */
    private final AtomicInteger generation = new AtomicInteger();
    private volatile Process current;
    private volatile boolean speaking;
    private volatile String voice;
    private List<String> voices;

    private DesktopSpeechService(Engine engine) {
        this.engine = engine;
    }

    /** Finds a speech engine for this OS, or returns null if there is none. */
    public static DesktopSpeechService detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Engine engine;
        if (os.contains("win")) {
            engine = new WindowsEngine();
        } else if (os.contains("mac")) {
            engine = onPath("say") ? new MacEngine() : null;
        } else if ((engine = PiperEngine.find()) != null) {
            // natural-sounding neural voice; preferred when installed
        } else if (onPath("espeak-ng")) {
            engine = new EspeakEngine("espeak-ng");
        } else if (onPath("spd-say")) {
            engine = new SpdSayEngine();
        } else if (onPath("espeak")) {
            engine = new EspeakEngine("espeak");
        } else {
            engine = null;
        }
        return engine == null ? null : new DesktopSpeechService(engine);
    }

    /** What to tell the user when {@link #detect()} finds nothing. */
    public static String missingEngineMessage() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) return "No speech: the 'say' command was not found.";
        return "No speech engine found. Install one with: sudo apt install espeak-ng";
    }

    @Override
    public void speak(String text) {
        speak(text, 1f);
    }

    @Override
    public void speak(String text, float rate) {
        speakSequence(Collections.singletonList(text), rate);
    }

    @Override
    public void speakSequence(List<String> parts, float rate) {
        final int gen = generation.incrementAndGet();
        killCurrent();
        final List<String> copy = new ArrayList<>(parts);
        final String v = voice;
        speaking = true;
        worker.submit(() -> {
            try {
                engine.cancel();
                for (int i = 0; i < copy.size(); i++) {
                    if (gen != generation.get()) return;
                    if (i > 0) Thread.sleep(PAUSE_BETWEEN_PARTS_MS);
                    if (gen != generation.get()) return;
                    run(engine.command(copy.get(i), rate, v), engine.environment(copy.get(i), rate, v));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                System.err.println("Speech failed: " + e.getMessage());
            } finally {
                if (gen == generation.get()) speaking = false;
            }
        });
    }

    private void run(List<String> command, String[][] env) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command).redirectErrorStream(true);
        for (String[] kv : env) pb.environment().put(kv[0], kv[1]);
        Process p = pb.start();
        current = p;
        p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        p.waitFor();
        current = null;
    }

    @Override
    public void stop() {
        generation.incrementAndGet();
        killCurrent();
        speaking = false;
        if (!worker.isShutdown()) worker.submit(engine::cancel);
    }

    private void killCurrent() {
        Process p = current;
        if (p != null) {
            p.descendants().forEach(ProcessHandle::destroy); // e.g. the audio player under sh
            p.destroy();
        }
    }

    @Override
    public boolean isSpeaking() {
        return speaking;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getStatus() {
        return "Speech: " + engine.name();
    }

    @Override
    public synchronized List<String> getVoices() {
        if (voices == null) {
            try {
                voices = engine.listVoices();
            } catch (IOException e) {
                voices = Collections.emptyList();
            }
        }
        return voices;
    }

    @Override
    public void setVoice(String voice) {
        this.voice = voice;
    }

    @Override
    public void dispose() {
        generation.incrementAndGet();
        killCurrent();
        engine.cancel();
        worker.shutdownNow();
        try {
            worker.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // ---- engines -------------------------------------------------------------------------

    private interface Engine {
        String name();

        List<String> command(String text, float rate, String voice);

        default String[][] environment(String text, float rate, String voice) {
            return new String[0][];
        }

        List<String> listVoices() throws IOException;

        /** Extra step to silence speech that outlives the killed process. Blocks until done. */
        default void cancel() {
        }
    }

    /**
     * Piper (https://github.com/rhasspy/piper): an offline neural voice that sounds
     * much more natural than espeak. Needs the {@code piper} command and at least
     * one voice model (.onnx + .onnx.json) in a voices folder; see README.
     */
    private static final class PiperEngine implements Engine {
        private final String exe;
        private final List<File> models;
        private final String lengthFlag;
        private final String player;
        private final File wav;

        private PiperEngine(String exe, List<File> models, String lengthFlag, String player) throws IOException {
            this.exe = exe;
            this.models = models;
            this.lengthFlag = lengthFlag;
            this.player = player;
            this.wav = File.createTempFile("spellingbee-", ".wav");
            this.wav.deleteOnExit();
        }

        static PiperEngine find() {
            String home = System.getProperty("user.home");
            String exe = onPath("piper") ? "piper"
                    : new File(home, ".local/bin/piper").canExecute() ? new File(home, ".local/bin/piper").getPath() : null;
            if (exe == null) return null;
            String player = onPath("paplay") ? "paplay" : onPath("pw-play") ? "pw-play" : onPath("aplay") ? "aplay" : null;
            if (player == null) return null;

            List<File> models = new ArrayList<>();
            String custom = System.getenv("SPELLINGBEE_PIPER_VOICES");
            List<File> dirs = new ArrayList<>();
            if (custom != null) dirs.add(new File(custom));
            dirs.add(new File(home, ".local/share/piper-voices"));
            dirs.add(new File(home, ".local/share/piper"));
            for (File dir : dirs) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".onnx") && new File(d, name + ".json").isFile());
                if (files != null) models.addAll(java.util.Arrays.asList(files));
            }
            if (models.isEmpty()) return null;
            // English (US) voices first, then by name.
            models.sort((a, b) -> {
                boolean ua = a.getName().startsWith("en_US"), ub = b.getName().startsWith("en_US");
                return ua != ub ? (ua ? -1 : 1) : a.getName().compareTo(b.getName());
            });

            // The Python and C++ builds spell the speed option differently.
            String lengthFlag = null;
            try {
                String help = String.join("\n", readLines(exe, "--help"));
                if (help.contains("--length-scale")) lengthFlag = "--length-scale";
                else if (help.contains("--length_scale")) lengthFlag = "--length_scale";
                return new PiperEngine(exe, models, lengthFlag, player);
            } catch (IOException e) {
                return null;
            }
        }

        @Override public String name() { return "Piper (" + voiceName(models.get(0)) + ")"; }

        private static String voiceName(File model) {
            return model.getName().replaceAll("\\.onnx$", "");
        }

        private File model(String voice) {
            for (File m : models) if (voiceName(m).equals(voice)) return m;
            return models.get(0);
        }

        @Override
        public List<String> command(String text, float rate, String voice) {
            // Everything arrives through environment variables, so nothing needs quoting.
            String script = "printf '%s' \"$SB_TEXT\" | \"$SB_PIPER\" -m \"$SB_MODEL\" $SB_SPEED -f \"$SB_WAV\""
                    + " >/dev/null 2>&1 && exec \"$SB_PLAYER\" \"$SB_WAV\"";
            return java.util.Arrays.asList("sh", "-c", script);
        }

        @Override
        public String[][] environment(String text, float rate, String voice) {
            // length scale > 1 is slower; Piper voices are a little fast, so 1.0 maps to 1.1.
            String speed = lengthFlag == null ? ""
                    : lengthFlag + " " + String.format(Locale.ROOT, "%.2f", 1.1f / Math.max(0.3f, rate));
            return new String[][]{
                    {"SB_TEXT", text}, {"SB_PIPER", exe}, {"SB_MODEL", model(voice).getPath()},
                    {"SB_SPEED", speed}, {"SB_WAV", wav.getPath()}, {"SB_PLAYER", player}};
        }

        @Override
        public List<String> listVoices() {
            List<String> out = new ArrayList<>();
            for (File m : models) out.add(voiceName(m));
            return out;
        }
    }

    private static final class EspeakEngine implements Engine {
        private final String exe;

        EspeakEngine(String exe) {
            this.exe = exe;
        }

        @Override public String name() { return exe; }

        @Override
        public List<String> command(String text, float rate, String voice) {
            List<String> c = new ArrayList<>();
            c.add(exe);
            c.add("-s");
            c.add(String.valueOf(Math.round(165 * rate)));
            c.add("-v");
            c.add(voice != null ? voice : "en-us");
            c.add("--");
            c.add(text);
            return c;
        }

        @Override
        public List<String> listVoices() throws IOException {
            List<String> out = new ArrayList<>();
            for (String line : readLines(exe, "--voices=en")) {
                String[] cols = line.trim().split("\\s+");
                if (cols.length >= 4 && !cols[0].equals("Pty")) out.add(cols[1]);
            }
            return out;
        }
    }

    private static final class SpdSayEngine implements Engine {
        @Override public String name() { return "spd-say (speech-dispatcher)"; }

        @Override
        public List<String> command(String text, float rate, String voice) {
            List<String> c = new ArrayList<>();
            c.add("spd-say");
            c.add("-w");
            c.add("-l");
            c.add("en-US");
            c.add("-r");
            c.add(String.valueOf(Math.max(-100, Math.min(100, Math.round((rate - 1f) * 100)))));
            if (voice != null) {
                c.add("-y");
                c.add(voice);
            }
            c.add(text.startsWith("-") ? " " + text : text);
            return c;
        }

        @Override
        public List<String> listVoices() throws IOException {
            List<String> out = new ArrayList<>();
            for (String line : readLines("spd-say", "-L")) {
                // NAME  LANGUAGE  VARIANT, where NAME may contain spaces.
                String[] cols = line.trim().split("\\s+");
                if (cols.length < 3) continue;
                String lang = cols[cols.length - 2];
                if (!lang.equalsIgnoreCase("en-US")) continue;
                String name = line.trim().replaceAll("\\s+\\S+\\s+\\S+$", "");
                out.add(name);
            }
            return out;
        }

        @Override
        public void cancel() {
            try {
                new ProcessBuilder("spd-say", "-C").start().waitFor(2, TimeUnit.SECONDS);
            } catch (IOException ignored) {
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static final class MacEngine implements Engine {
        @Override public String name() { return "macOS say"; }

        @Override
        public List<String> command(String text, float rate, String voice) {
            List<String> c = new ArrayList<>();
            c.add("say");
            c.add("-r");
            c.add(String.valueOf(Math.round(175 * rate)));
            if (voice != null) {
                c.add("-v");
                c.add(voice);
            }
            c.add("--");
            c.add(text);
            return c;
        }

        @Override
        public List<String> listVoices() throws IOException {
            List<String> out = new ArrayList<>();
            for (String line : readLines("say", "-v", "?")) {
                // "Samantha            en_US    # Hello, my name is Samantha."
                String[] halves = line.split("#", 2)[0].trim().split("\\s+");
                if (halves.length >= 2 && halves[halves.length - 1].startsWith("en")) {
                    out.add(String.join(" ", java.util.Arrays.copyOf(halves, halves.length - 1)));
                }
            }
            return out;
        }
    }

    /** Text and voice go through environment variables so nothing needs quoting. */
    private static final class WindowsEngine implements Engine {
        @Override public String name() { return "Windows System.Speech"; }

        @Override
        public List<String> command(String text, float rate, String voice) {
            String script = "Add-Type -AssemblyName System.Speech;"
                    + "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer;"
                    + "if ($env:SB_VOICE) { $s.SelectVoice($env:SB_VOICE) };"
                    + "$s.Rate = [int]$env:SB_RATE;"
                    + "$s.Speak($env:SB_TEXT)";
            List<String> c = new ArrayList<>();
            c.add("powershell");
            c.add("-NoProfile");
            c.add("-Command");
            c.add(script);
            return c;
        }

        @Override
        public String[][] environment(String text, float rate, String voice) {
            int r = Math.max(-10, Math.min(10, Math.round((rate - 1f) * 10)));
            return new String[][]{{"SB_TEXT", text}, {"SB_RATE", String.valueOf(r)}, {"SB_VOICE", voice == null ? "" : voice}};
        }

        @Override
        public List<String> listVoices() throws IOException {
            return readLines("powershell", "-NoProfile", "-Command",
                    "Add-Type -AssemblyName System.Speech;"
                            + "(New-Object System.Speech.Synthesis.SpeechSynthesizer).GetInstalledVoices()"
                            + " | ForEach-Object { $_.VoiceInfo.Name }");
        }
    }

    // ---- helpers -------------------------------------------------------------------------

    private static boolean onPath(String exe) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir, exe).canExecute()) return true;
        }
        return false;
    }

    private static List<String> readLines(String... command) throws IOException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = r.readLine()) != null) {
                if (header && (line.contains("NAME") || line.startsWith("Pty"))) {
                    header = false;
                    continue;
                }
                header = false;
                if (!line.isBlank()) lines.add(line);
            }
        }
        return lines;
    }
}
