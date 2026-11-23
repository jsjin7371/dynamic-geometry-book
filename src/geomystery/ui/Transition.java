package geomystery.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 화면 전환 연출. 바뀌기 전·후 화면을 1배 해상도로 한 장씩 찍어 두고, 그 두 장만 겹쳐 움직인다
 * (FADE: 이전 화면이 사라짐, BOOK: 이전 화면이 양쪽으로 펼쳐짐). 전환하는 동안 실제 화면은
 * 다시 그리지 않으므로 매끄럽고, 1배로 찍어서 준비 시간도 짧다. 끝나면 선명한 실제 화면이 나타난다.
 * <ul>
 *   <li>BOOK — 수첩이 양쪽으로 펼쳐지듯 이전 화면이 갈라져 열린다(사건 들어갈 때)</li>
 *   <li>FADE — 이전 화면이 서서히 사라진다</li>
 * </ul>
 */
final class Transition {
    enum Kind { FADE, BOOK }

    private Transition() {}

    private static Runnable running;

    static void play(JFrame frame, Kind kind, Runnable switcher, Runnable onDone) {
        if (running != null) running.run(); // 진행 중인 전환은 바로 끝낸다
        JRootPane root = frame.getRootPane();
        Container content = frame.getContentPane();
        if (!frame.isShowing() || content.getWidth() <= 0) {
            switcher.run();
            if (onDone != null) onDone.run();
            return;
        }
        double sc = 1;
        try { sc = Math.max(1, frame.getGraphicsConfiguration().getDefaultTransform().getScaleX()); } catch (Exception ignored) { }
        int w = content.getWidth(), h = content.getHeight();
        // 이전 화면 사진은 움직이는 0.3~0.5초 동안만 보이므로 1배 해상도로 찍는다(레티나 2배보다 훨씬 빠르다)
        BufferedImage before = snapshot(content, w, h, 1);
        switcher.run();
        content.validate();
        // 새 화면도 1배로 한 장 찍어 둔다. 전환 동안에는 이 두 장만 그려서(불투명) 아래 실제 화면을
        // 매 프레임 다시 그리지 않는다 — 그래야 0.3~0.5초 동안 매끄럽게 움직인다.
        BufferedImage after = snapshot(content, w, h, 1);
        JLayeredPane lp = root.getLayeredPane();
        Point at = SwingUtilities.convertPoint(content, 0, 0, lp);
        long dur = kind == Kind.BOOK ? 560 : 300;
        long t0 = System.currentTimeMillis();
        JComponent overlay = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                double k = Math.min(1, (System.currentTimeMillis() - t0) / (double) dur);
                double e = 1 - Math.pow(1 - k, 3);
                g.drawImage(after, 0, 0, w, h, null);
                if (kind == Kind.FADE) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (1 - e)));
                    g.drawImage(before, 0, 0, w, h, null);
                } else {
                    // 가운데에서 양쪽으로 접히는 표지 두 장
                    int half = w / 2, bw = before.getWidth(), bhh = before.getHeight();
                    int lw = (int) Math.round(half * (1 - e));
                    if (lw > 1) {
                        g.drawImage(before, 0, 0, lw, h, 0, 0, bw / 2, bhh, null);
                        g.drawImage(before, w - lw, 0, w, h, bw / 2, 0, bw, bhh, null);
                        // 접히면서 어두워지는 그늘과 책등
                        float shade = (float) (0.55 * e);
                        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 0), lw, 0, new Color(0f, 0f, 0f, shade)));
                        g.fillRect(0, 0, lw, h);
                        g.setPaint(new GradientPaint(w - lw, 0, new Color(0f, 0f, 0f, shade), w, 0, new Color(0, 0, 0, 0)));
                        g.fillRect(w - lw, 0, lw, h);
                        g.setColor(new Color(58, 40, 25, (int) (200 * (1 - e))));
                        g.fillRect(lw - 3, 0, 3, h);
                        g.fillRect(w - lw, 0, 3, h);
                    }
                    // 펼쳐지는 안쪽 가장자리 그림자
                    g.setPaint(new GradientPaint(lw, 0, new Color(0f, 0f, 0f, (float) (0.35 * (1 - e))), lw + 40, 0, new Color(0, 0, 0, 0)));
                    g.fillRect(lw, 0, 40, h);
                    g.setPaint(new GradientPaint(w - lw - 40, 0, new Color(0, 0, 0, 0), w - lw, 0, new Color(0f, 0f, 0f, (float) (0.35 * (1 - e)))));
                    g.fillRect(w - lw - 40, 0, 40, h);
                }
                g.dispose();
            }
        };
        overlay.setOpaque(true);
        overlay.setBounds(at.x, at.y, w, h);
        lp.add(overlay, Integer.valueOf(JLayeredPane.DRAG_LAYER + 50));
        overlay.paintImmediately(0, 0, w, h);
        javax.swing.Timer tm = new javax.swing.Timer(16, null);
        boolean[] done = {false};
        Runnable finish = () -> {
            if (done[0]) return;
            done[0] = true;
            tm.stop();
            lp.remove(overlay);
            lp.repaint(at.x, at.y, w, h);
            running = null;
            if (onDone != null) onDone.run();
        };
        running = finish;
        tm.addActionListener(e -> {
            if (System.currentTimeMillis() - t0 >= dur) finish.run();
            else overlay.repaint();
        });
        tm.start();
    }

    private static BufferedImage snapshot(Container c, int w, int h, double sc) {
        BufferedImage im = new BufferedImage((int) Math.ceil(w * sc), (int) Math.ceil(h * sc), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = im.createGraphics();
        g.scale(sc, sc);
        g.setColor(Theme.BG);
        g.fillRect(0, 0, w, h);
        c.paint(g);
        g.dispose();
        return im;
    }
}
