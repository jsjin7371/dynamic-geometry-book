package geomystery.ui;

import geomystery.data.Model.Chapter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

/**
 * 장 표지: 장의 첫 사건을 시작할 때 배경과 함께 장 번호·제목이 떠오른다. 클릭하면 건너뛴다.
 * 표지가 지금 화면(지도) 위로 떠올라 화면을 다 덮으면 그 뒤에서 대화 화면으로 바꾸고,
 * 표지가 사라지면서 대화가 드러난다(대화 화면이 표지보다 먼저 보이지 않도록).
 */
final class ChapterCard {
    private ChapterCard() {}

    private static final long LENGTH = 3400;

    /** 표지가 화면을 완전히 덮는 시각(이때 뒤에서 화면을 바꾼다). */
    private static final long COVERED = 450;

    static void show(JFrame frame, Chapter ch, Runnable atCovered, Runnable done) {
        JRootPane root = frame.getRootPane();
        if (root == null || !frame.isShowing()) {
            if (atCovered != null) atCovered.run();
            if (done != null) done.run();
            return;
        }
        JLayeredPane lp = root.getLayeredPane();
        long t0 = System.currentTimeMillis();
        BufferedImage bg = Ui.img("bg_" + ch.scene);
        boolean[] closed = {false};
        BufferedImage[] base = {null};
        JComponent card = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                long el = System.currentTimeMillis() - t0;
                int w = getWidth(), h = getHeight();
                float a = el < COVERED ? el / (float) COVERED : el > LENGTH - 600 ? Math.max(0f, (LENGTH - el) / 600f) : 1f;
                if (base[0] == null) {
                    // 바탕(어두운 배경 그림 + 가장자리 그늘)은 한 번만 합성해 두고 투명도만 바꿔 복사한다
                    base[0] = Ui.deviceImage(g, w, h);
                    Graphics2D b = Ui.cacheGraphics(base[0], g);
                    b.setColor(new Color(0x1E140C));
                    b.fillRect(0, 0, w, h);
                    if (bg != null) {
                        double s = Math.max(w / (double) bg.getWidth(), h / (double) bg.getHeight());
                        b.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                        Ui.drawScaled(b, bg, (w - bg.getWidth() * s) / 2, (h - bg.getHeight() * s) / 2, bg.getWidth() * s, bg.getHeight() * s);
                        b.setComposite(AlphaComposite.SrcOver);
                    }
                    b.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.7f,
                            new float[]{0.3f, 1f}, new Color[]{new Color(20, 12, 6, 40), new Color(20, 12, 6, 230)}));
                    b.fillRect(0, 0, w, h);
                    b.dispose();
                }
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
                Ui.blit(g, base[0], 0, 0, w, h);
                double cy = h / 2.0;
                // 글자가 차례로 떠오른다
                float l1 = clamp((el - 300) / 500f), l2 = clamp((el - 650) / 600f), l3 = clamp((el - 1100) / 600f);
                g.setFont(Theme.tracked(Theme.bold(18), 0.45f));
                g.setColor(alpha(Theme.GOLD, l1 * a));
                Theme.drawCentered(g, ch.label, w / 2.0, cy - 92 + (1 - l1) * 10);
                double lw = 160 * l1;
                g.setStroke(new BasicStroke(1.6f));
                g.draw(new Line2D.Double(w / 2.0 - 70 - lw, cy - 92, w / 2.0 - 150, cy - 92));
                g.draw(new Line2D.Double(w / 2.0 + 150, cy - 92, w / 2.0 + 70 + lw, cy - 92));
                g.setFont(Theme.xbold(64));
                g.setColor(alpha(Theme.CREAM, l2 * a));
                Theme.drawCentered(g, ch.title, w / 2.0, cy - 10 + (1 - l2) * 16);
                g.setFont(Theme.hand(34));
                g.setColor(alpha(new Color(0xE9D8B2), l3 * a));
                Theme.drawCentered(g, ch.subtitle, w / 2.0, cy + 62);
                // 장식: 컴퍼스로 그린 듯한 원호
                g.setColor(alpha(Theme.GOLD, 0.6f * l3 * a));
                g.setStroke(new BasicStroke(1.4f));
                double r = 230;
                g.draw(new java.awt.geom.Arc2D.Double(w / 2.0 - r, cy - r + 10, r * 2, r * 2, 200, -220 * l3, java.awt.geom.Arc2D.OPEN));
                g.setFont(Theme.serif(13));
                g.setColor(alpha(new Color(0xCDB68D), 0.8f * l3 * a));
                Theme.drawCentered(g, "클릭하면 넘어갑니다", w / 2.0, h - 40);
                g.dispose();
            }
        };
        card.setBounds(0, 0, lp.getWidth(), lp.getHeight());
        lp.add(card, JLayeredPane.DRAG_LAYER);
        Sfx.play("hint");
        javax.swing.Timer tm = new javax.swing.Timer(16, null);
        boolean[] switched = {false};
        Runnable cover = () -> {
            if (switched[0]) return;
            switched[0] = true;
            if (atCovered != null) atCovered.run();
        };
        Runnable finish = () -> {
            if (closed[0]) return;
            closed[0] = true;
            cover.run(); // 일찍 눌러 건너뛰어도 화면은 바뀐다
            tm.stop();
            lp.remove(card);
            lp.repaint();
            if (done != null) done.run();
        };
        tm.addActionListener(e -> {
            long el = System.currentTimeMillis() - t0;
            if (el >= COVERED) cover.run();
            card.repaint();
            if (el > LENGTH) finish.run();
        });
        card.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { finish.run(); }
        });
        tm.start();
    }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static Color alpha(Color c, float a) { return Theme.alpha(c, Math.max(0f, Math.min(1f, a))); }
}
