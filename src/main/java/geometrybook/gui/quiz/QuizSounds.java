package geometrybook.gui.quiz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * 퀴즈 효과음. 소리 파일 없이 코드로 음을 만들어 들려준다.
 * 화면이 멈추지 않게 별도 스레드 하나에서 차례로 재생하고, 소리 장치가 없으면 조용히 넘어간다.
 */
final class QuizSounds {
    enum Sound { TICK, PICK, SNAP, DONE, CORRECT, WRONG }

    private static final float RATE = 44100f;
    private static final ExecutorService PLAYER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "quiz-sound");
        thread.setDaemon(true);
        return thread;
    });

    private QuizSounds() { }

    static void play(Sound sound) {
        PLAYER.execute(() -> {
            try {
                write(render(sound));
            } catch (Exception | LinkageError ignored) {
                // 소리 장치가 없거나 사용 중이면 효과음 없이 계속한다.
            }
        });
    }

    /** 음 높이(Hz)·길이(ms)를 이어 붙여 16비트 소리 데이터를 만든다. */
    private static byte[] render(Sound sound) {
        switch (sound) {
            case TICK: return tones(0.32, new double[][]{{1500, 30}});
            case PICK: return tones(0.36, new double[][]{{700, 40}, {950, 45}});
            case SNAP: return tones(0.45, new double[][]{{880, 60}, {1320, 70}});
            case DONE: return tones(0.40, new double[][]{{1047, 70}, {1319, 70}, {1568, 70}, {2093, 160}});
            case CORRECT: return tones(0.45, new double[][]{{523, 90}, {659, 90}, {784, 90}, {1047, 260}});
            default: return buzz();
        }
    }

    private static byte[] tones(double volume, double[][] notes) {
        int total = 0;
        for (double[] note : notes) total += (int) (RATE * note[1] / 1000);
        byte[] data = new byte[total * 2];
        int index = 0;
        for (double[] note : notes) {
            int length = (int) (RATE * note[1] / 1000);
            for (int i = 0; i < length; i++) {
                double t = i / RATE;
                double envelope = Math.min(1, i / (RATE * 0.005)) * Math.exp(-3.0 * i / length);
                double sample = Math.sin(2 * Math.PI * note[0] * t) + 0.3 * Math.sin(4 * Math.PI * note[0] * t);
                index = put(data, index, sample * envelope * volume / 1.3);
            }
        }
        return data;
    }

    /** 틀렸을 때 낮게 내려가는 "뿌우" 소리. */
    private static byte[] buzz() {
        int length = (int) (RATE * 0.38);
        byte[] data = new byte[length * 2];
        double phase = 0;
        int index = 0;
        for (int i = 0; i < length; i++) {
            double progress = (double) i / length;
            double frequency = 230 - 90 * progress + 6 * Math.sin(2 * Math.PI * 18 * i / RATE);
            phase += 2 * Math.PI * frequency / RATE;
            double square = Math.tanh(3 * Math.sin(phase));
            double envelope = Math.min(1, i / (RATE * 0.01)) * (1 - progress * 0.7);
            index = put(data, index, square * envelope * 0.35);
        }
        return data;
    }

    private static int put(byte[] data, int index, double value) {
        short s = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * Short.MAX_VALUE));
        data[index] = (byte) s;
        data[index + 1] = (byte) (s >> 8);
        return index + 2;
    }

    private static void write(byte[] data) throws Exception {
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        }
    }
}
