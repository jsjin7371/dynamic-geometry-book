package geometrybook.gui.lesson.animation;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.Timer;

/** 도형 설명 애니메이션의 재생 시간과 타이머 수명을 공유한다. */
public abstract class LessonAnimation extends JComponent {
    public static final int DURATION = 24000;
    protected int elapsed;
    private long lastTick;
    private final Timer timer;
    private final Runnable onUpdate;
    protected LessonAnimation(Runnable onUpdate) {
        this.onUpdate = onUpdate;
        setPreferredSize(new Dimension(600, 340));
        timer = new Timer(25, e -> {
            long now = System.nanoTime();
            elapsed = Math.min(DURATION, elapsed + (int)((now - lastTick) / 1_000_000));
            lastTick = now;
            if (elapsed == DURATION) ((Timer)e.getSource()).stop();
            repaint();
            onUpdate.run();
        });
    }

    public void play() {
        if (elapsed == DURATION) elapsed = 0;
        lastTick = System.nanoTime();
        timer.start();
        onUpdate.run();
    }

    public void pause() {
        timer.stop();
        onUpdate.run();
    }

    public void restart() {
        elapsed = 0;
        play();
        repaint();
    }
    public boolean playing() {
        return timer.isRunning();
    }
    public int elapsed() {
        return elapsed;
    }
    public abstract String caption();

    @Override public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }
}
