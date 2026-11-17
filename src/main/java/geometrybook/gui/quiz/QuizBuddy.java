package geometrybook.gui.quiz;

import geometrybook.gui.common.Theme;
import geometrybook.gui.common.TriangleCharacter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * 퀴즈 결과 칸 옆에서 반응하는 삼각형 캐릭터. 로그인 화면의 삼각형 캐릭터와 같은 모양이다.
 * 고르면 깡충, 힌트를 보면 갸우뚱, 맞히면 점프하며 웃고, 틀리면 도리도리하며 시무룩해진다.
 */
final class QuizBuddy extends JComponent {
    enum Mood { IDLE, HOP, THINK, HAPPY, SAD }

    private static final Color BODY = new Color(0xCBE8D6);
    private Mood mood = Mood.IDLE;
    private long since = System.currentTimeMillis();
    private final Timer timer = new Timer(30, e -> repaint());

    QuizBuddy() {
        setPreferredSize(new Dimension(112, 124));
        setMinimumSize(new Dimension(112, 124));
        setOpaque(false);
    }

    void react(Mood mood) {
        this.mood = mood;
        since = System.currentTimeMillis();
    }

    @Override public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            long t = System.currentTimeMillis() - since;
            if (mood == Mood.HOP && t > 450) mood = Mood.IDLE;

            double dx = 0, dy = 3 * Math.sin(System.currentTimeMillis() / 400.0), tilt = 0;
            switch (mood) {
                case HOP:
                    dy = -14 * Math.sin(Math.PI * Math.min(1, t / 450.0));
                    break;
                case THINK:
                    tilt = Math.toRadians(8 * Math.sin(t / 500.0));
                    break;
                case HAPPY:
                    dy = t < 1500 ? -26 * Math.abs(Math.sin(t / 190.0)) : -4 * Math.abs(Math.sin(t / 300.0));
                    break;
                case SAD:
                    dx = t < 700 ? 7 * Math.sin(t / 45.0) : 0;
                    dy = 4;
                    break;
                default:
            }
            double scale = Math.min(getWidth() / 140.0, getHeight() / 130.0);
            g.translate(getWidth() / 2.0 + dx, getHeight() - 8 + dy);
            g.scale(scale, scale);
            g.rotate(tilt);
            g.translate(-66, -112);   // 132×115 캐릭터의 발밑 가운데를 기준점으로

            drawBody(g);
            switch (mood) {
                case HAPPY: drawHappyFace(g, t); break;
                case SAD: drawSadFace(g, t); break;
                case THINK: drawThinkFace(g, t); break;
                default: TriangleCharacter.drawFace(g, 66, 73);
            }
        } finally {
            g.dispose();
        }
    }

    private void drawBody(Graphics2D g) {
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(66, 1);
        triangle.lineTo(2, 112);
        triangle.lineTo(130, 112);
        triangle.closePath();
        g.setColor(BODY);
        g.fill(triangle);
        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(3));
        g.draw(triangle);
    }

    /** 눈을 ^^로 감고 입을 크게 벌린 채 두 팔을 번쩍 든다. 주위에 반짝이. */
    private void drawHappyFace(Graphics2D g, long t) {
        g.setColor(Theme.TEXT);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(46, 70, 52, 63));
        g.draw(new Line2D.Double(52, 63, 58, 70));
        g.draw(new Line2D.Double(74, 70, 80, 63));
        g.draw(new Line2D.Double(80, 63, 86, 70));
        Path2D mouth = new Path2D.Double();
        mouth.moveTo(54, 78);
        mouth.quadTo(66, 100, 78, 78);
        mouth.closePath();
        g.setColor(new Color(0xC2453D));
        g.fill(mouth);
        g.setColor(new Color(0xF5B9A7));
        g.fillOval(30, 77, 14, 7);
        g.fillOval(88, 77, 14, 7);
        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double wave = 6 * Math.sin(t / 120.0);
        g.draw(new Line2D.Double(26, 70, 8, 44 + wave));
        g.draw(new Line2D.Double(106, 70, 124, 44 - wave));
        g.setColor(new Color(0xF2B705));
        for (int k = 0; k < 3; k++) {
            double a = t / 300.0 + k * 2.1, r = 8 + 2 * Math.sin(t / 150.0 + k);
            sparkle(g, 66 + Math.cos(a) * 70, 30 + Math.sin(a) * 26, r);
        }
    }

    /** 처진 눈과 시무룩한 입, 머리 위 땀방울. */
    private void drawSadFace(Graphics2D g, long t) {
        g.setColor(Theme.TEXT);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(46, 64, 57, 69));
        g.draw(new Line2D.Double(86, 64, 75, 69));
        g.drawArc(56, 80, 20, 14, 20, 140);
        Path2D drop = new Path2D.Double();
        double y = 34 + Math.min(10, t / 80.0);
        drop.moveTo(98, y);
        drop.quadTo(92, y + 12, 98, y + 15);
        drop.quadTo(104, y + 12, 98, y);
        g.setColor(new Color(0x7CB8E8));
        g.fill(drop);
    }

    /** 위를 보는 눈과 머리 위에 떠오르는 물음표. */
    private void drawThinkFace(Graphics2D g, long t) {
        g.setColor(Theme.TEXT);
        g.fill(new Ellipse2D.Double(49, 61, 7, 10));
        g.fill(new Ellipse2D.Double(77, 61, 7, 10));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(58, 86, 74, 84));
        g.setColor(new Color(0xF2B705));
        g.setFont(Theme.font(Font.BOLD, 30));
        g.drawString("?", 104, (float) (16 + 4 * Math.sin(t / 250.0)));
    }

    private static void sparkle(Graphics2D g, double cx, double cy, double r) {
        Path2D s = new Path2D.Double();
        s.moveTo(cx, cy - r);
        s.quadTo(cx, cy, cx + r, cy);
        s.quadTo(cx, cy, cx, cy + r);
        s.quadTo(cx, cy, cx - r, cy);
        s.quadTo(cx, cy, cx, cy - r);
        g.fill(s);
    }
}
