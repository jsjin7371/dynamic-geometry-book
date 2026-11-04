package geometrybook.gui.lesson;

import geometrybook.gui.common.Theme;
import java.awt.geom.Line2D;
import java.awt.*;
import javax.swing.*;

/** 마름모의 네 변과 두 대각선을 차례로 그린다. */
final class RhombusAnimation extends JComponent {
    static final int DURATION = 24000;
    private int elapsed;
    private long lastTick;
    private final Timer timer;
    private final Runnable onUpdate;
    private static final int[] X = {300, 485, 300, 115};
    private static final int[] Y = {42, 156, 270, 156};
    private static final Polygon OUTLINE = new Polygon(X, Y, 4);

    RhombusAnimation(Runnable onUpdate) {
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

    void play() {
        if (elapsed == DURATION) elapsed = 0;
        lastTick = System.nanoTime();
        timer.start();
        onUpdate.run();
    }

    void pause() {
        timer.stop();
        onUpdate.run();
    }

    void restart() {
        elapsed = 0;
        play();
        repaint();
    }
    boolean playing() {
        return timer.isRunning();
    }
    int elapsed() {
        return elapsed;
    }
    String caption() {
        int time = elapsed();
        if (time < 6000) return "네 꼭짓점을 곧은 선분으로 차례차례 이어요.";
        if (time < 13000) return "네 변에 같은 표시가 있어요. 길이가 모두 같다는 뜻이에요.";
        if (time < 19000) return "마주 보는 꼭짓점끼리 이으면 두 대각선이 생겨요.";
        return "작은 네모를 보세요. 두 대각선이 직각으로 만나요!";
    }

    void drawScene(Graphics2D g, int elapsed) {
        if (elapsed >= 6000) {
            g.setColor(new Color(0xCCE7DA));
            g.fill(OUTLINE);
        }
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            g.setColor(Theme.GREEN);
            drawSegment(g, X[i], Y[i], X[next], Y[next], elapsed - i * 1500, 1500);
            if (elapsed >= 6000 + i * 650) {
                // 변의 중점에서 변에 수직으로 짧은 같은 길이 표시를 그린다.
                double mx = (X[i] + X[next]) / 2.0;
                double my = (Y[i] + Y[next]) / 2.0;
                double dx = (Y[next] - Y[i]) * 0.045;
                double dy = (X[i] - X[next]) * 0.045;
                g.setColor(new Color(0x956619));
                g.draw(new Line2D.Double(mx - dx, my - dy, mx + dx, my + dy));
            }
        }
        if (elapsed >= 13000) {
            g.setColor(new Color(0x7260A3));
            g.setStroke(new BasicStroke(3));
            drawSegment(g, X[0], Y[0], X[2], Y[2], elapsed - 13000, 2000);
            drawSegment(g, X[3], Y[3], X[1], Y[1], elapsed - 15500, 2000);
        }
        if (elapsed >= 19000) {
            int cx = X[0], cy = Y[1];
            g.setColor(Theme.TEXT);
            g.drawLine(cx + 17, cy, cx + 17, cy - 17);
            g.drawLine(cx + 17, cy - 17, cx, cy - 17);
            g.setFont(Theme.font(Font.BOLD, 20));
            g.drawString("90°", cx + 23, cy - 12);
        }
        g.setColor(new Color(0xECA345));
        for (int i = 0; i < 4; i++) g.fillOval(X[i] - 6, Y[i] - 6, 12, 12);
    }

    private static void drawSegment(Graphics2D g, int x1, int y1, int x2, int y2,
                                    int time, int duration) {
        if (time <= 0) return;
        double fraction = Math.min(1.0, (double) time / duration);
        g.draw(new Line2D.Double(x1, y1, x1 + fraction * (x2 - x1), y1 + fraction * (y2 - y1)));
    }

    @Override public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = Math.min(getWidth()/600.0, getHeight()/340.0);
        g.translate((getWidth()-600*scale)/2, (getHeight()-340*scale)/2);
        g.scale(scale, scale);
        drawScene(g, elapsed);
        g.dispose();
    }
}
