package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * 도감 카드 크게 보기. 카드가 마우스를 따라 살짝 기울어지고 빛이 반사된다.
 * 앞면은 한 번만 그려 두고(캐시), 마우스가 움직일 때만 다시 그린다(계속 도는 타이머 없음).
 */
final class CardZoom {
    private CardZoom() {}

    static void open(MainFrame frame, Theorem t) {
        Game game = frame.game;
        Ui.Modal m = Ui.solidModal(frame, "정리 카드 · " + t.name, 520);
        Tilt card = new Tilt(game, t);
        JPanel wrap = Ui.clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.add(card);
        m.add(wrap);
        m.add((JComponent) Ui.vgap(4));
        m.add(Ui.label("카드 위에서 마우스를 움직여 보세요", Theme.serif(12.5f), Theme.MUTED));
        if (game.content.explores.containsKey(t.id)) {
            Btn ex = new Btn("정리 탐구 — 직접 움직여 보기", Btn.Style.OUTLINE, "compass").font(14);
            ex.addActionListener(e -> {
                m.close();
                SwingUtilities.invokeLater(() -> ExploreBoard.open(frame, t));
            });
            m.buttons.add(ex);
        }
        m.button("닫기", Btn.Style.PRIMARY, null);
        Sfx.play("select");
        m.show();
    }

    private static final class Tilt extends JComponent {
        private static final int CW = 340, CH = 452, PAD = 34;
        private final Game game;
        private final Theorem t;
        private final boolean foil;
        private BufferedImage face;
        private double nx, ny, tx, ty;   // 지금 기울기와 목표 기울기(-1~1)
        private Point2D.Double glare;
        private javax.swing.Timer settle;

        Tilt(Game game, Theorem t) {
            this.game = game;
            this.t = t;
            Lesson l = game.lessonForTheorem(t.id);
            Chapter ch = l == null ? null : game.content.chapterOf(l);
            foil = ch != null && game.doneCount(ch) == game.content.lessonsOf(ch).size();
            setOpaque(false);
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    tx = Math.max(-1, Math.min(1, (e.getX() - getWidth() / 2.0) / (CW / 2.0)));
                    ty = Math.max(-1, Math.min(1, (e.getY() - getHeight() / 2.0) / (CH / 2.0)));
                    nx = tx;
                    ny = ty;
                    glare = new Point2D.Double(e.getX(), e.getY());
                    repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    // 놓으면 천천히 제자리로(잠깐만 도는 타이머)
                    tx = ty = 0;
                    glare = null;
                    if (settle != null) settle.stop();
                    settle = new javax.swing.Timer(16, ev -> {
                        nx += (tx - nx) * 0.2;
                        ny += (ty - ny) * 0.2;
                        repaint();
                        if (Math.abs(nx) < 0.01 && Math.abs(ny) < 0.01) { nx = ny = 0; ((javax.swing.Timer) ev.getSource()).stop(); repaint(); }
                    });
                    settle.start();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        @Override public Dimension getPreferredSize() { return new Dimension(CW + PAD * 2, CH + PAD * 2); }

        private BufferedImage face(Graphics2D like) {
            double sc = Ui.deviceScale(like);
            if (face == null || face.getWidth() != (int) Math.ceil(CW * sc)) {
                face = Ui.deviceImage(like, CW, CH);
                Graphics2D g = Ui.cacheGraphics(face, like);
                paintFace(g);
                g.dispose();
            }
            return face;
        }

        private void paintFace(Graphics2D g) {
            RoundRectangle2D shape = new RoundRectangle2D.Double(2, 2, CW - 4, CH - 4, 20, 20);
            g.setColor(new Color(0xF7EEDC));
            g.fill(shape);
            g.setColor(foil ? new Color(0xD9B456) : new Color(0xC9A456));
            g.setStroke(new BasicStroke(foil ? 5f : 4f));
            g.draw(shape);
            g.setStroke(new BasicStroke(1.5f));
            g.setColor(new Color(0xB8903A));
            g.draw(new RoundRectangle2D.Double(14, 14, CW - 28, CH - 28, 12, 12));
            g.setFont(Theme.tracked(Theme.serif(13), 0.25f));
            g.setColor(new Color(0x6E5641));
            g.drawString("THEOREM No. " + t.id.substring(1), 30, 44);
            if (foil) Icons.paint(g, "star_fill", CW - 54, 26, 24, new Color(0xD9B456));
            // 큰 그림
            g.setColor(new Color(0xEFE3C7));
            g.fill(new Ellipse2D.Double(CW / 2.0 - 100, 62, 200, 200));
            Icons.theorem(g, t.icon, new Rectangle2D.Double(CW / 2.0 - 80, 82, 160, 160), new Color(0x3A2819), new Color(0xA33A28));
            g.setFont(Theme.xbold(26));
            g.setColor(new Color(0x3A2819));
            Theme.drawCentered(g, t.name, CW / 2.0, 296);
            g.setColor(new Color(0xB8903A));
            g.fill(new Rectangle2D.Double(CW / 2.0 - 40, 318, 80, 2));
            g.setFont(Theme.serif(15));
            g.setColor(new Color(0x5A4632));
            int y = 348;
            for (String line : Widgets.TextBlock.wrap(t.description, g.getFontMetrics(), CW - 60)) {
                Theme.drawCentered(g, line, CW / 2.0, y);
                y += 24;
            }
            Lesson l = game.lessonForTheorem(t.id);
            if (l != null) {
                Chapter ch = game.content.chapterOf(l);
                g.setFont(Theme.hand(20));
                g.setColor(new Color(0xA33A28));
                Theme.drawCentered(g, ch.label + " · '" + l.caseName + "' 사건에서 획득", CW / 2.0, CH - 34);
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
            // 그림자(기울기 반대쪽)
            g.setColor(new Color(0, 0, 0, 45));
            g.fill(new RoundRectangle2D.Double(cx - CW / 2.0 - nx * 10 + 4, cy - CH / 2.0 - ny * 10 + 12, CW, CH, 20, 20));
            AffineTransform keep = g.getTransform();
            g.translate(cx, cy);
            g.rotate(Math.toRadians(nx * 2.5));
            g.shear(nx * 0.06, ny * 0.04);
            g.scale(1 - Math.abs(nx) * 0.05, 1 - Math.abs(ny) * 0.04);
            g.translate(-CW / 2.0, -CH / 2.0);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(face(g), 0, 0, CW, CH, null);
            // 빛 반사
            RoundRectangle2D shape = new RoundRectangle2D.Double(2, 2, CW - 4, CH - 4, 20, 20);
            double gx = CW / 2.0 + nx * CW * 0.5, gy = CH / 2.0 + ny * CH * 0.5;
            if (glare != null || Math.abs(nx) + Math.abs(ny) > 0.02) {
                Graphics2D s = (Graphics2D) g.create();
                s.clip(shape);
                s.setPaint(new RadialGradientPaint(new Point2D.Double(gx, gy), CW * 0.7f, new float[]{0f, 1f},
                        new Color[]{new Color(255, 255, 255, foil ? 120 : 80), new Color(255, 255, 255, 0)}));
                s.fill(shape);
                if (foil) {
                    s.setPaint(new LinearGradientPaint((float) (gx - 90), 0, (float) (gx + 90), CH, new float[]{0f, 0.5f, 1f},
                            new Color[]{new Color(255, 230, 150, 0), new Color(255, 230, 150, 90), new Color(255, 230, 150, 0)}));
                    s.fill(shape);
                }
                s.dispose();
            }
            g.setTransform(keep);
            g.dispose();
        }
    }
}
