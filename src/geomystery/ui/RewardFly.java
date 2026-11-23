package geomystery.ui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 받은 단서 토큰이 화면 아래쪽에서 상단 토큰 알약으로 날아 들어가는 연출.
 * 동전이 지나가는 작은 영역만 다시 그려서 아래 화면 전체를 다시 그리지 않는다.
 */
final class RewardFly {
    private RewardFly() {}

    private static final long FLIGHT = 700, STAGGER = 140;

    static void play(JFrame frame, Header header, int count, int startValue) {
        if (frame == null || header == null || count <= 0 || !header.isShowing()) return;
        JLayeredPane lp = frame.getLayeredPane();
        Point target = header.tokenCenter(lp);
        int shown = Math.min(count, 6);
        Point from = new Point(lp.getWidth() / 2, lp.getHeight() - 140);
        long t0 = System.currentTimeMillis() + 150;
        header.showTokens(startValue);
        List<Rectangle> last = new ArrayList<>();
        JComponent layer = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                long now = System.currentTimeMillis();
                for (int i = 0; i < shown; i++) {
                    double k = (now - t0 - i * STAGGER) / (double) FLIGHT;
                    if (k < 0 || k > 1) continue;
                    Point p = at(from, target, k, i);
                    double size = 30 - 10 * k;
                    Icons.paint(g, "token", p.x - size / 2, p.y - size / 2, size, Theme.GOLD);
                    // 꼬리 반짝임
                    g.setColor(new Color(242, 206, 99, (int) (120 * (1 - k))));
                    Point q = at(from, target, Math.max(0, k - 0.06), i);
                    g.fillOval(q.x - 4, q.y - 4, 8, 8);
                }
                g.dispose();
            }
        };
        layer.setBounds(0, 0, lp.getWidth(), lp.getHeight());
        lp.add(layer, JLayeredPane.POPUP_LAYER);
        boolean[] landed = new boolean[shown];
        javax.swing.Timer tm = new javax.swing.Timer(16, null);
        tm.addActionListener(e -> {
            long now = System.currentTimeMillis();
            List<Rectangle> dirty = new ArrayList<>();
            for (int i = 0; i < shown; i++) {
                double k = (now - t0 - i * STAGGER) / (double) FLIGHT;
                if (k >= 0 && k <= 1) {
                    Point p = at(from, target, k, i);
                    dirty.add(new Rectangle(p.x - 26, p.y - 26, 52, 52));
                }
                if (k > 1 && !landed[i]) {
                    landed[i] = true;
                    Sfx.play("select");
                    // 동전 수만큼 나눠서 숫자를 올린다(마지막 동전에서 정확한 값)
                    int v = i == shown - 1 ? startValue + count : startValue + (int) Math.round(count * (i + 1) / (double) shown);
                    header.showTokens(v);
                    header.pulseTokens();
                }
            }
            for (Rectangle r : last) layer.repaint(r);
            for (Rectangle r : dirty) layer.repaint(r);
            last.clear();
            last.addAll(dirty);
            if (now - t0 > FLIGHT + STAGGER * shown + 100) {
                tm.stop();
                lp.remove(layer);
                lp.repaint(target.x - 80, target.y - 40, 160, 80);
                for (Rectangle r : dirty) lp.repaint(r);
                header.showTokens(-1);
            }
        });
        tm.start();
    }

    /** 포물선 경로(동전마다 조금씩 다른 곡선). */
    private static Point at(Point a, Point b, double k, int i) {
        double e = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
        double side = (i % 2 == 0 ? 1 : -1) * (60 + i * 18);
        double cx = (a.x + b.x) / 2.0 + side, cy = Math.min(a.y, b.y) - 40;
        double u = 1 - e;
        double x = u * u * a.x + 2 * u * e * cx + e * e * b.x;
        double y = u * u * a.y + 2 * u * e * cy + e * e * b.y;
        return new Point((int) Math.round(x), (int) Math.round(y));
    }
}
