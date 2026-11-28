package geomystery.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 배경음악. 효과음과 마찬가지로 음원 파일 없이 곡을 직접 합성한다.
 * <pre>
 * garden     선의 정원      — C장조, 하프 아르페지오와 오카리나 같은 선율 (84 BPM)
 * clock      각도의 시계탑   — D단조, 오르골과 째깍거리는 초침 (92 BPM)
 * monastery  삼각형의 수도원 — D 도리아 선법, 오르간 화음과 낮은 종 (60 BPM)
 * puzzle     수수께끼 풀이   — A단조, 생각을 방해하지 않는 성긴 하프 (72 BPM)
 * finale     최종 추리      — E단조, 고동치는 베이스와 북 (104 BPM)
 * </pre>
 * 곡은 처음 필요할 때 한 번 합성해 끝과 처음이 이어지는 루프로 만들고,
 * 전용 스레드가 SourceDataLine 으로 흘려 보내며 곡이 바뀌면 1.5초 동안 교차 페이드한다.
 */
public final class Music {
    private Music() {}

    private static final float RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
    /** 음량 단계별 이득. 효과음보다 한참 작게 깔린다. */
    private static final float[] LEVEL_GAIN = {0f, 0.13f, 0.26f, 0.42f};
    private static final int CHUNK = 1024;

    private static final Object LOCK = new Object();
    private static final Map<String, float[]> cache = new HashMap<>();
    private static volatile String requested;
    private static volatile int level = 2;
    private static volatile boolean broken;
    private static Thread thread;

    /** 장면 이름(chapters.csv 의 scene) → 곡 이름. */
    public static String forScene(String scene) {
        if ("clock".equals(scene) || "monastery".equals(scene) || "observatory".equals(scene)) return scene;
        return "garden";
    }

    /** 곡을 바꾼다(같은 곡이면 그대로 이어서 재생). null 이면 서서히 멈춘다. */
    public static void play(String track) {
        requested = track;
        wake();
    }

    public static void setLevel(int lv) {
        level = Math.max(0, Math.min(3, lv));
        preload();
        wake();
    }

    private static volatile boolean preloading;

    /**
     * 모든 곡을 미리 합성해 둔다(곡마다 0.3~1초). 곡이 처음 바뀔 때 소리가 끊기지 않게
     * 시작할 때 한 번 백그라운드에서 부른다. 음악을 꺼 두었으면 아무것도 하지 않는다.
     */
    public static void preload() {
        if (preloading || level == 0) return;
        preloading = true;
        Thread t = new Thread(() -> {
            for (String n : new String[]{"garden", "puzzle", "clock", "monastery", "observatory", "finale"}) track(n);
        }, "geomystery-music-preload");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private static void wake() {
        if (broken) return;
        synchronized (LOCK) {
            if (thread == null && level > 0 && requested != null) {
                thread = new Thread(Music::run, "geomystery-music");
                thread.setDaemon(true);
                thread.setPriority(Thread.NORM_PRIORITY + 1);
                thread.start();
            }
            LOCK.notifyAll();
        }
    }

    // ------------------------------------------------------------------ 재생 스레드
    private static void run() {
        SourceDataLine line = null;
        float[] cur = null, old = null;
        String curName = null;
        int pc = 0, po = 0;
        float fade = 1f, gain = 0f;
        final float fadeStep = 1f / (RATE * 1.5f);
        byte[] out = new byte[CHUNK * 2];
        try {
            while (true) {
                String want = requested;
                float target = LEVEL_GAIN[level];
                if (want == null ? curName != null : !want.equals(curName)) {
                    old = fade > 0.5f ? cur : old; // 페이드 중 또 바뀌면 더 큰 쪽을 남긴다
                    po = old == cur ? pc : po;
                    cur = want == null ? null : track(want);
                    curName = want;
                    pc = 0;
                    fade = 0f;
                }
                boolean idle = (cur == null && (old == null || fade >= 1f)) || (target == 0f && gain < 0.0005f);
                if (idle) {
                    if (line != null) { line.stop(); line.flush(); line.close(); line = null; }
                    gain = 0f;
                    synchronized (LOCK) { LOCK.wait(1000); }
                    continue;
                }
                if (line == null) {
                    line = AudioSystem.getSourceDataLine(FORMAT);
                    line.open(FORMAT, CHUNK * 2 * 6);
                    line.start();
                }
                for (int i = 0; i < CHUNK; i++) {
                    if (fade < 1f) fade = Math.min(1f, fade + fadeStep);
                    float a = 0f, b = 0f;
                    if (cur != null) { a = cur[pc]; pc = pc + 1 == cur.length ? 0 : pc + 1; }
                    if (old != null && fade < 1f) { b = old[po]; po = po + 1 == old.length ? 0 : po + 1; }
                    gain += (target - gain) * 0.0004f;
                    float v = (a * fade + b * (1f - fade)) * gain;
                    int s = Math.round(Math.max(-1f, Math.min(1f, v)) * 32000);
                    out[i * 2] = (byte) s;
                    out[i * 2 + 1] = (byte) (s >> 8);
                }
                if (fade >= 1f) old = null;
                line.write(out, 0, out.length);
            }
        } catch (InterruptedException ignored) {
            // 종료
        } catch (Exception | LinkageError e) {
            broken = true; // 오디오 장치 없음: 조용히 포기
        } finally {
            if (line != null) line.close();
            synchronized (LOCK) { thread = null; }
        }
    }

    private static float[] track(String name) {
        synchronized (cache) {
            return cache.computeIfAbsent(name, Music::compose);
        }
    }

    // ------------------------------------------------------------------ 작곡
    /** 곡 하나를 합성한다(외부 미리 듣기·검사용으로도 공개). 값 범위는 대략 -0.9 ~ 0.9. */
    private static float[] compose(String name) {
        switch (name) {
            case "clock": return clock();
            case "monastery": return monastery();
            case "observatory": return observatory();
            case "puzzle": return puzzle();
            case "finale": return finale();
            default: return garden();
        }
    }

    /** 루프 버퍼에 쓰는 도우미. 끝을 넘친 소리는 처음으로 감아 넣어 이음매가 없다. */
    private static final class Song {
        final float[] buf;
        final double beat;
        final Random rnd;

        Song(double bpm, int beats, long seed) {
            beat = 60.0 / bpm;
            buf = new float[(int) Math.round(beats * beat * RATE)];
            rnd = new Random(seed);
        }

        void add(int i, double v) {
            int k = i % buf.length;
            buf[k] += (float) v;
        }

        static double hz(int midi) { return 440.0 * Math.pow(2, (midi - 69) / 12.0); }

        /** 부드러운 화음 패드(살짝 어긋난 두 음으로 합창 효과). */
        void pad(double at, double dur, int[] notes, double amp, boolean organ) {
            int s = (int) (at * beat * RATE), n = (int) ((dur * beat + 1.2) * RATE);
            double att = 0.5 * RATE, rel = 1.2 * RATE, holdEnd = dur * beat * RATE;
            for (int note : notes) {
                double f = hz(note), ph2 = rnd.nextDouble() * 6;
                for (int i = 0; i < n; i++) {
                    double t = i / RATE;
                    double env = Math.min(1, i / att) * (i < holdEnd ? 1 : Math.max(0, 1 - (i - holdEnd) / rel));
                    double v = Math.sin(2 * Math.PI * f * t) + Math.sin(2 * Math.PI * f * 1.004 * t + ph2) * 0.7;
                    if (organ) v += Math.sin(2 * Math.PI * f * 2 * t) * 0.45 + Math.sin(2 * Math.PI * f * 3 * t) * 0.2;
                    else v += Math.sin(2 * Math.PI * f * 2 * t) * 0.12;
                    add(s + i, v * env * amp / notes.length);
                }
            }
        }

        /** 하프·피아노 같은 퉁김: 높은 배음일수록 빨리 사라진다. */
        void pluck(double at, int note, double amp, double decay) {
            double f = hz(note);
            int s = (int) (at * beat * RATE), n = (int) (Math.min(4.0, 5.0 / decay) * RATE);
            for (int i = 0; i < n; i++) {
                double t = i / RATE;
                double v = 0;
                for (int h = 1; h <= 4; h++) v += Math.sin(2 * Math.PI * f * h * t) * Math.exp(-t * decay * Math.pow(h, 0.8)) / Math.pow(h, 1.3);
                add(s + i, v * Math.min(1, t / 0.004) * amp);
            }
        }

        /** 오르골: 맑고 짧은 금속음. */
        void musicBox(double at, int note, double amp) {
            double f = hz(note);
            int s = (int) (at * beat * RATE), n = (int) (1.6 * RATE);
            for (int i = 0; i < n; i++) {
                double t = i / RATE;
                double v = Math.sin(2 * Math.PI * f * t) * Math.exp(-t * 3.2) + Math.sin(2 * Math.PI * f * 4.02 * t) * Math.exp(-t * 11) * 0.3;
                add(s + i, v * Math.min(1, t / 0.002) * amp);
            }
        }

        /** 종: 배음이 어긋난 긴 울림. */
        void bell(double at, int note, double amp) {
            double f = hz(note);
            double[] partial = {1, 2.0, 2.76, 4.07, 5.4};
            double[] gains = {1, 0.5, 0.45, 0.25, 0.18};
            int s = (int) (at * beat * RATE), n = (int) (5 * RATE);
            for (int i = 0; i < n; i++) {
                double t = i / RATE, v = 0;
                for (int k = 0; k < partial.length; k++) v += Math.sin(2 * Math.PI * f * partial[k] * t) * gains[k] * Math.exp(-t * (0.7 + k * 0.6));
                add(s + i, v * Math.min(1, t / 0.003) * amp / 2.4);
            }
        }

        /** 둥근 베이스. */
        void bass(double at, double dur, int note, double amp) {
            double f = hz(note);
            int s = (int) (at * beat * RATE), n = (int) ((dur * beat + 0.15) * RATE);
            double end = dur * beat * RATE;
            for (int i = 0; i < n; i++) {
                double t = i / RATE;
                double env = Math.min(1, i / (0.02 * RATE)) * Math.exp(-t * 1.2) * (i < end ? 1 : Math.max(0, 1 - (i - end) / (0.15 * RATE)));
                add(s + i, (Math.sin(2 * Math.PI * f * t) + Math.sin(2 * Math.PI * f * 2 * t) * 0.18) * env * amp);
            }
        }

        /** 시계 초침 소리(째깍). */
        void tick(double at, double amp, double pitch) {
            int s = (int) (at * beat * RATE), n = (int) (0.05 * RATE);
            for (int i = 0; i < n; i++) {
                double t = i / RATE;
                double v = Math.sin(2 * Math.PI * pitch * t) * Math.exp(-t * 160) + (rnd.nextDouble() * 2 - 1) * Math.exp(-t * 300) * 0.4;
                add(s + i, v * amp);
            }
        }

        /** 북. */
        void drum(double at, double amp) {
            int s = (int) (at * beat * RATE), n = (int) (0.5 * RATE);
            double ph = 0;
            for (int i = 0; i < n; i++) {
                double t = i / RATE;
                ph += 2 * Math.PI * (55 + 70 * Math.exp(-t * 30)) / RATE;
                add(s + i, Math.sin(ph) * Math.exp(-t * 7) * amp);
            }
        }

        float[] finish() {
            float peak = 1e-6f;
            for (float v : buf) peak = Math.max(peak, Math.abs(v));
            float k = 0.85f / peak;
            for (int i = 0; i < buf.length; i++) buf[i] *= k;
            return buf;
        }
    }

    private static int[] chord(int root, int... intervals) {
        int[] r = new int[intervals.length];
        for (int i = 0; i < intervals.length; i++) r[i] = root + intervals[i];
        return r;
    }

    /** 선의 정원: C - Am - F - G, 하프 아르페지오와 오카리나 선율. */
    private static float[] garden() {
        Song s = new Song(84, 32, 11);
        int[] roots = {48, 45, 41, 43};
        int[][] chords = {chord(60, 0, 4, 7), chord(57, 0, 3, 7), chord(53, 0, 4, 7), chord(55, 0, 4, 7)};
        int[] penta = {72, 74, 76, 79, 81, 84};
        for (int c = 0; c < 4; c++) {
            double at = c * 8;
            s.pad(at, 8, chords[c], 0.22, false);
            s.bass(at, 3.5, roots[c] - 12, 0.30);
            s.bass(at + 4, 3.5, roots[c] - 12, 0.24);
            int[] arp = {chords[c][0], chords[c][1], chords[c][2], chords[c][0] + 12, chords[c][2], chords[c][1]};
            for (int e = 0; e < 16; e++) {
                if (e % 8 == 7 && s.rnd.nextBoolean()) continue;
                s.pluck(at + e * 0.5, arp[e % arp.length], e % 4 == 0 ? 0.16 : 0.11, 2.6);
            }
            for (int q = 0; q < 8; q++) {
                if (s.rnd.nextDouble() < 0.42) {
                    int note = penta[s.rnd.nextInt(penta.length)];
                    s.pad(at + q, 0.8, new int[]{note}, 0.10, false);
                }
            }
        }
        return s.finish();
    }

    /** 각도의 시계탑: Dm - B♭ - F - A, 오르골과 초침. */
    private static float[] clock() {
        Song s = new Song(92, 32, 23);
        int[][] chords = {chord(62, 0, 3, 7), chord(58, 0, 4, 7), chord(53, 0, 4, 7), chord(57, 0, 4, 7)};
        int[] roots = {38, 34, 41, 33};
        for (int c = 0; c < 4; c++) {
            double at = c * 8;
            s.pad(at, 8, chords[c], 0.16, false);
            s.bass(at, 1.5, roots[c], 0.32);
            s.bass(at + 4, 1.5, roots[c], 0.26);
            int[] tones = {chords[c][0] + 12, chords[c][1] + 12, chords[c][2] + 12, chords[c][0] + 24};
            for (int e = 0; e < 16; e++) {
                if (s.rnd.nextDouble() < 0.3) continue;
                int note = tones[(e * 3 + c) % tones.length];
                s.musicBox(at + e * 0.5, note, e % 4 == 0 ? 0.20 : 0.13);
            }
        }
        for (int b = 0; b < 32; b++) s.tick(b, b % 2 == 0 ? 0.16 : 0.11, b % 2 == 0 ? 2400 : 1900);
        return s.finish();
    }

    /** 삼각형의 수도원: Dm - C - G - Dm (도리아), 오르간과 종. */
    private static float[] monastery() {
        Song s = new Song(60, 32, 37);
        int[][] chords = {chord(50, 0, 7, 12, 15), chord(48, 0, 7, 12, 16), chord(43, 0, 7, 12, 16), chord(50, 0, 7, 12, 15)};
        int[] chant = {62, 64, 65, 67, 69, 72};
        for (int c = 0; c < 4; c++) {
            double at = c * 8;
            s.pad(at, 8, chords[c], 0.30, true);
            s.bell(at, chords[c][0] + 12, 0.30);
            if (c % 2 == 1) s.bell(at + 4, chords[c][0] + 19, 0.16);
            for (int q = 0; q < 8; q += 2) {
                if (s.rnd.nextDouble() < 0.6) s.pluck(at + q + 1, chant[s.rnd.nextInt(chant.length)], 0.09, 1.4);
            }
        }
        return s.finish();
    }

    /** 원의 천문대: Fmaj7 - Em7 - Dm7 - Cmaj7, 반짝이는 오르골과 긴 패드(별빛). */
    private static float[] observatory() {
        Song s = new Song(66, 32, 97);
        int[][] chords = {chord(53, 0, 4, 7, 11), chord(52, 0, 3, 7, 10), chord(50, 0, 3, 7, 10), chord(48, 0, 4, 7, 11)};
        int[] roots = {41, 40, 38, 36};
        for (int c = 0; c < 4; c++) {
            double at = c * 8;
            s.pad(at, 8, chords[c], 0.24, false);
            s.bass(at, 6, roots[c], 0.22);
            int[] tones = {chords[c][0] + 24, chords[c][1] + 24, chords[c][2] + 24, chords[c][3] + 12};
            for (int e = 0; e < 8; e++) {
                if (s.rnd.nextDouble() < 0.35) continue;
                s.musicBox(at + e + (s.rnd.nextBoolean() ? 0.5 : 0), tones[(e + c * 2) % tones.length], e % 4 == 0 ? 0.16 : 0.10);
            }
            if (c % 2 == 0) s.bell(at + 6, chords[c][2] + 24, 0.10);
        }
        return s.finish();
    }

    /** 수수께끼 풀이: Am - F - C - G, 성긴 하프만. */
    private static float[] puzzle() {
        Song s = new Song(72, 32, 53);
        int[][] chords = {chord(57, 0, 3, 7, 12), chord(53, 0, 4, 7, 11), chord(48, 0, 4, 7, 14), chord(55, 0, 4, 7, 9)};
        for (int c = 0; c < 4; c++) {
            double at = c * 8;
            s.pad(at, 8, new int[]{chords[c][0], chords[c][2]}, 0.13, false);
            s.bass(at, 6, chords[c][0] - 12, 0.16);
            for (int q = 0; q < 8; q++) {
                if (q % 4 == 3 || s.rnd.nextDouble() < 0.2) continue;
                s.pluck(at + q, chords[c][(q * 2 + c) % chords[c].length] + 12, 0.13, 1.8);
            }
        }
        return s.finish();
    }

    /** 최종 추리: Em - C - D - B, 고동치는 베이스와 북. */
    private static float[] finale() {
        Song s = new Song(104, 32, 71);
        int[][] chords = {chord(52, 0, 3, 7), chord(48, 0, 4, 7), chord(50, 0, 4, 7), chord(47, 0, 4, 7)};
        for (int c = 0; c < 4; c++) {
            double at = c * 8;
            s.pad(at, 8, new int[]{chords[c][0] + 12, chords[c][1] + 12, chords[c][2] + 12}, 0.20, false);
            for (int e = 0; e < 16; e++) s.bass(at + e * 0.5, 0.42, chords[c][0] - 12, e % 2 == 0 ? 0.30 : 0.20);
            for (int b = 0; b < 8; b += 2) s.drum(at + b, b == 0 ? 0.55 : 0.38);
            for (int e = 0; e < 16; e++) s.tick(at + e * 0.5 + 0.25, 0.05, 3200);
            int[] ost = {chords[c][0] + 24, chords[c][2] + 12, chords[c][1] + 12, chords[c][2] + 12};
            for (int e = 0; e < 16; e++) s.pluck(at + e * 0.5, ost[e % 4], 0.08, 4.5);
        }
        return s.finish();
    }
}
