package geomystery.ui;

import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 효과음. 음원 파일 없이 짧은 파형을 직접 합성해 javax.sound.sampled 로 재생한다.
 * 오디오 장치가 없으면 조용히 아무것도 하지 않는다.
 *
 * <pre>
 * click  버튼      type   대사 타자기     correct 정답 차임   wrong 오답 버저
 * hint   쪽지 넘김  aux    분필 긋기       stamp  해결 도장    fanfare 최종 추리 성공
 * select 카드 고르기
 * </pre>
 */
public final class Sfx {
    private Sfx() {}

    private static final float RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);
    private static final float[] LEVEL_GAIN = {0f, 0.3f, 0.65f, 1f};

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "geomystery-sfx");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, Clip> clips = new HashMap<>();
    private static volatile int level = 2;
    private static volatile boolean broken = false;

    /** 음량 단계(0 끔 ~ 3 크게). 바뀌면 다음 재생 때 파형을 다시 만든다. */
    public static void setLevel(int lv) {
        int v = Math.max(0, Math.min(3, lv));
        if (v == level) return;
        level = v;
        EXEC.submit(() -> {
            for (Clip c : clips.values()) c.close();
            clips.clear();
        });
    }

    public static void play(String name) {
        if (level == 0 || broken) return;
        EXEC.submit(() -> {
            try {
                Clip c = clips.get(name);
                if (c == null) {
                    byte[] pcm = render(name, LEVEL_GAIN[level]);
                    if (pcm == null) return;
                    c = AudioSystem.getClip();
                    c.open(FORMAT, pcm, 0, pcm.length);
                    clips.put(name, c);
                }
                c.stop();
                c.setFramePosition(0);
                c.start();
            } catch (Exception | LinkageError e) {
                broken = true; // 오디오 장치 없음: 이후로는 시도하지 않는다
            }
        });
    }

    // ------------------------------------------------------------------ 합성
    private static byte[] render(String name, float gain) {
        float[] s;
        switch (name) {
            case "click": s = click(); break;
            case "select": s = select(); break;
            case "type": s = type(); break;
            case "correct": s = correct(); break;
            case "wrong": s = wrong(); break;
            case "hint": s = rustle(0.16, 0.22); break;
            case "aux": s = chalk(); break;
            case "stamp": s = stamp(); break;
            case "fanfare": s = fanfare(); break;
            case "shutter": s = shutter(); break;
            case "erase": s = rustle(0.42, 0.20); break;
            case "heart": s = heart(); break;
            case "page": s = rustle(0.22, 0.14); break;
            default: return null;
        }
        byte[] out = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            int v = Math.round(Math.max(-1f, Math.min(1f, s[i] * gain)) * 32000);
            out[i * 2] = (byte) v;
            out[i * 2 + 1] = (byte) (v >> 8);
        }
        return out;
    }

    private static float[] buf(double seconds) { return new float[(int) (RATE * seconds)]; }

    /** 종소리 같은 음 하나를 더한다. */
    private static void bell(float[] s, double start, double dur, double freq, double amp, double decay) {
        int a = (int) (start * RATE), n = (int) (dur * RATE);
        for (int i = 0; i < n && a + i < s.length; i++) {
            double t = i / RATE;
            double env = Math.min(1, t / 0.004) * Math.exp(-t * decay);
            double v = Math.sin(2 * Math.PI * freq * t) + 0.35 * Math.sin(2 * Math.PI * freq * 2.01 * t)
                    + 0.12 * Math.sin(2 * Math.PI * freq * 3.02 * t);
            s[a + i] += (float) (v * env * amp / 1.47);
        }
    }

    private static float[] click() {
        float[] s = buf(0.03);
        Random r = new Random(1);
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double env = Math.exp(-t * 260);
            s[i] = (float) ((Math.sin(2 * Math.PI * 1700 * t) * 0.6 + (r.nextDouble() * 2 - 1) * 0.4) * env * 0.22);
        }
        return s;
    }

    private static float[] select() {
        float[] s = buf(0.08);
        bell(s, 0, 0.08, 1320, 0.18, 55);
        return s;
    }

    private static float[] type() {
        float[] s = buf(0.018);
        Random r = new Random(7);
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            s[i] = (float) ((r.nextDouble() * 2 - 1) * Math.exp(-t * 420) * 0.10 + Math.sin(2 * Math.PI * 2600 * t) * Math.exp(-t * 500) * 0.05);
        }
        return s;
    }

    private static float[] correct() {
        float[] s = buf(0.75);
        bell(s, 0.00, 0.6, 783.99, 0.32, 7);
        bell(s, 0.09, 0.6, 1046.5, 0.32, 7);
        bell(s, 0.18, 0.55, 1318.5, 0.30, 6);
        return s;
    }

    private static float[] wrong() {
        float[] s = buf(0.34);
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double env = Math.min(1, t / 0.01) * Math.exp(-t * 7);
            double f = 165 - t * 60;
            double sq = Math.signum(Math.sin(2 * Math.PI * f * t)) * 0.35 + Math.sin(2 * Math.PI * f * 1.01 * t) * 0.5;
            s[i] = (float) (sq * env * 0.28);
        }
        return s;
    }

    private static float[] rustle(double dur, double amp) {
        float[] s = buf(dur);
        Random r = new Random(3);
        double lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double env = Math.sin(Math.PI * t / dur) * (0.6 + 0.4 * Math.sin(t * 90));
            double n = r.nextDouble() * 2 - 1;
            lp += (n - lp) * 0.35;
            s[i] = (float) ((n - lp) * env * amp);
        }
        return s;
    }

    private static float[] chalk() {
        float[] s = buf(0.32);
        Random r = new Random(5);
        double lp = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double env = Math.min(1, t / 0.03) * Math.min(1, (0.32 - t) / 0.06);
            double n = r.nextDouble() * 2 - 1;
            lp += (n - lp) * 0.5;
            double grit = 0.55 + 0.45 * Math.sin(2 * Math.PI * 38 * t);
            s[i] = (float) (lp * env * grit * 0.16);
        }
        return s;
    }

    private static float[] stamp() {
        float[] s = buf(0.32);
        Random r = new Random(9);
        for (int i = 0; i < s.length; i++) {
            double t = i / RATE;
            double thud = Math.sin(2 * Math.PI * (95 - t * 120) * t) * Math.exp(-t * 18);
            double hit = (r.nextDouble() * 2 - 1) * Math.exp(-t * 160);
            s[i] = (float) (thud * 0.55 + hit * 0.25);
        }
        return s;
    }

    /** 카메라 셔터: 찰칵(두 번의 짧은 금속음) + 필름 감는 소리. */
    private static float[] shutter() {
        float[] s = buf(0.42);
        Random r = new Random(13);
        for (int k = 0; k < 2; k++) {
            int a = (int) ((k == 0 ? 0 : 0.07) * RATE);
            for (int i = 0; i < 0.03 * RATE && a + i < s.length; i++) {
                double t = i / RATE;
                s[a + i] += (float) (((r.nextDouble() * 2 - 1) * 0.6 + Math.sin(2 * Math.PI * 3100 * t) * 0.4) * Math.exp(-t * 220) * 0.5);
            }
        }
        for (int i = (int) (0.14 * RATE); i < s.length; i++) {
            double t = i / RATE - 0.14;
            double env = Math.min(1, t / 0.02) * Math.max(0, 1 - t / 0.28);
            double v = (r.nextDouble() * 2 - 1) * (0.5 + 0.5 * Math.signum(Math.sin(2 * Math.PI * 70 * t)));
            s[i] += (float) (v * env * 0.05);
        }
        return s;
    }

    /** 하트: 맑은 두 음. */
    private static float[] heart() {
        float[] s = buf(0.6);
        bell(s, 0.00, 0.5, 1174.7, 0.22, 8);
        bell(s, 0.10, 0.5, 1568.0, 0.20, 8);
        return s;
    }

    private static float[] fanfare() {
        float[] s = buf(1.4);
        double[] notes = {523.25, 659.25, 783.99, 1046.5};
        for (int i = 0; i < notes.length; i++) bell(s, i * 0.12, 0.5, notes[i], 0.24, 6);
        bell(s, 0.5, 0.9, 523.25, 0.18, 3);
        bell(s, 0.5, 0.9, 659.25, 0.18, 3);
        bell(s, 0.5, 0.9, 783.99, 0.18, 3);
        bell(s, 0.5, 0.9, 1046.5, 0.2, 3);
        return s;
    }
}
