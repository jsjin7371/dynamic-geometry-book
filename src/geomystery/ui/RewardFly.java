package geomystery.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * 받은 단서 토큰이 화면 아래쪽에서 상단 토큰 알약으로 날아 들어가는 연출.
 * <p>매끄럽게 움직이도록:
 * <ul>
 *   <li>동전 그림은 크기별로 한 번만 그려 두고(화면 배율) 매 프레임 복사만 한다 — 동전 속 '?' 글자를
 *       매번 다른 크기로 새로 그리면 프레임마다 글자 모양을 다시 만들어야 해서 느리다.</li>
 *   <li>동전마다 지나간 자리와 새 자리만 따로 바로 그린다. repaint() 로 맡기면 Swing 이 여러 동전의
 *       영역을 하나의 큰 사각형으로 합쳐서 그 아래 화면 전체를 다시 그리게 된다.</li>
 *   <li>위치는 소수점까지 계산해서(레티나에서는 반 픽셀 단위로) 1픽셀씩 튀지 않게 한다.</li>
 * </ul>
 */
final class RewardFly {
    private RewardFly() {}

    private static final long FLIGHT = 700, STAGGER = 140;
    private static final int MIN_SIZE = 20, MAX_SIZE = 30;
    /** 크기(20~30)별 동전 그림. 화면 배율이 바뀌면 다시 만든다. */
    private static BufferedImage[] sprites;
    private static double spriteScale;

    static void play(JFrame frame, Header header, int count, int startValue) {
        if (frame == null || header == null || count <= 0 || !header.isShowing()) return;
        JLayeredPane lp = frame.getLayeredPane();
        Point target = header.tokenCenter(lp);
        int shown = Math.min(count, 6);
        Point from = new Point(lp.getWidth() / 2, lp.getHeight() - 140);
        long t0 = System.currentTimeMillis() + 150;
        header.showTokens(startValue);
        double sc = 1;
        try { sc = Math.max(1, frame.getGraphicsConfiguration().getDefaultTransform().getScaleX()); } catch (Exception ignored) { }
        BufferedImage[] spr = sprites(sc);
        JComponent layer = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                Rectangle clip = g.getClipBounds();
                long now = System.currentTimeMillis();
                for (int i = 0; i < shown; i++) {
                    double k = (now - t0 - i * STAGGER) / (double) FLIGHT;
                    if (k < 0 || k > 1) continue;
                    Point2D.Double p = at(from, target, k, i);
                    if (clip != null && !clip.intersects(p.x - 26, p.y - 26, 52, 52)) continue;
                    // 꼬리 반짝임
                    g.setColor(new Color(242, 206, 99, (int) (120 * (1 - k))));
                    Point2D.Double q = at(from, target, Math.max(0, k - 0.06), i);
                    g.fill(new java.awt.geom.Ellipse2D.Double(q.x - 4, q.y - 4, 8, 8));
                    int size = (int) Math.round(MAX_SIZE - (MAX_SIZE - MIN_SIZE) * k);
                    Ui.blit(g, spr[size - MIN_SIZE], p.x - size / 2.0, p.y - size / 2.0, size, size);
                }
                g.dispose();
            }
        };
        layer.setBounds(0, 0, lp.getWidth(), lp.getHeight());
        lp.add(layer, JLayeredPane.POPUP_LAYER);
        boolean[] landed = new boolean[shown];
        Rectangle[] last = new Rectangle[shown];
        javax.swing.Timer tm = new javax.swing.Timer(15, null);
        tm.addActionListener(e -> {
            long now = System.currentTimeMillis();
            Rectangle all = null;
            for (int i = 0; i < shown; i++) {
                double k = (now - t0 - i * STAGGER) / (double) FLIGHT;
                Rectangle r = null;
                if (k >= 0 && k <= 1) {
                    Point2D.Double p = at(from, target, k, i);
                    Point2D.Double q = at(from, target, Math.max(0, k - 0.06), i);
                    r = new Rectangle((int) Math.floor(Math.min(p.x - 17, q.x - 6)), (int) Math.floor(Math.min(p.y - 17, q.y - 6)), 0, 0);
                    r.add(Math.ceil(Math.max(p.x + 17, q.x + 6)), Math.ceil(Math.max(p.y + 17, q.y + 6)));
                }
                Rectangle d = last[i] == null ? r : (r == null ? last[i] : r.union(last[i]));
                if (d != null) all = all == null ? d : all.union(d);
                last[i] = r;
                if (k > 1 && !landed[i]) {
                    landed[i] = true;
                    Sfx.play("select");
                    // 동전 수만큼 나눠서 숫자를 올린다(마지막 동전에서 정확한 값)
                    int v = i == shown - 1 ? startValue + count : startValue + (int) Math.round(count * (i + 1) / (double) shown);
                    header.showTokens(v);
                    header.pulseTokens();
                }
            }
            // 한 번에 한 영역만 다시 그린다(동전마다 따로 그리면 그때마다 화면으로 내보내느라 더 느리다)
            if (all != null) layer.repaint(all);
            if (now - t0 > FLIGHT + STAGGER * shown + 100) {
                tm.stop();
                lp.remove(layer);
                lp.repaint(target.x - 80, target.y - 40, 160, 80);
                header.showTokens(-1);
            }
        });
        tm.start();
    }

    /** 시작할 때(백그라운드) 동전 그림을 미리 만들어 둔다. 첫 보상 때 화면이 멈칫하지 않도록. */
    static void prewarm(double sc) {
        Thread t = new Thread(() -> sprites(sc), "geomystery-coins");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
    }

    private static synchronized BufferedImage[] sprites(double sc) {
        if (sprites != null && spriteScale == sc) return sprites;
        BufferedImage[] out = new BufferedImage[MAX_SIZE - MIN_SIZE + 1];
        for (int s = MIN_SIZE; s <= MAX_SIZE; s++) {
            int px = (int) Math.ceil(s * sc);
            BufferedImage im = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = Theme.aa(im.createGraphics());
            g.scale(sc, sc);
            Icons.paint(g, "token", 0, 0, s, Theme.GOLD);
            g.dispose();
            out[s - MIN_SIZE] = im;
        }
        sprites = out;
        spriteScale = sc;
        return out;
    }

    /** 포물선 경로(동전마다 조금씩 다른 곡선). */
    private static Point2D.Double at(Point a, Point b, double k, int i) {
        double e = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
        double side = (i % 2 == 0 ? 1 : -1) * (60 + i * 18);
        double cx = (a.x + b.x) / 2.0 + side, cy = Math.min(a.y, b.y) - 40;
        double u = 1 - e;
        return new Point2D.Double(u * u * a.x + 2 * u * e * cx + e * e * b.x, u * u * a.y + 2 * u * e * cy + e * e * b.y);
    }
}
