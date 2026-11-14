package geometrybook.gui.lesson.animation;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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

    /** 600×340 좌표계에 현재 elapsed 시점의 장면을 그린다. 크기 맞춤과 정리는 부모가 한다. */
    protected abstract void draw(Graphics2D g);

    /** 컴포넌트 크기에 맞춰 600×340 좌표계를 비율 유지로 가운데에 놓고 draw를 부른다. */
    @Override protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = Math.min(getWidth() / 600.0, getHeight() / 340.0);
            g.translate((getWidth() - 600 * scale) / 2, (getHeight() - 340 * scale) / 2);
            g.scale(scale, scale);
            draw(g);
        } finally {
            g.dispose();
        }
    }

    /** (cx, y)를 가운데 기준선으로 글자를 그린다. */
    protected static void centered(Graphics2D g, String text, double cx, double y) {
        g.drawString(text, (float) (cx - g.getFontMetrics().stringWidth(text) / 2.0), (float) y);
    }

    @Override public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }
}
