package geometrybook.gui.quiz;

import geometrybook.gui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * 퀴즈 문제 옆에 그리는 삼각형 그림. questions.csv의 figure 칸을 읽어 그린다.
 * 형식: "angles:꼭대기,왼쪽,오른쪽" (모르는 각은 ?), 크기가 바뀌는 장면은 끝에 ";grow".
 *
 * 처음에는 변이 그려지고 모르는 각의 ?가 통통 튄다. 힌트를 누르면 세 각이 한 줄로 모여 180°를
 * 보여 주고(grow는 크기 변화, 두 각의 합이 180° 이상이면 두 변이 만나지 않는 장면),
 * 채점하면 ?가 답으로 바뀌며 맞히면 별이 터지고 틀리면 흔들린다.
 */
public final class TriangleFigure extends JComponent {
    private static final double W = 460, H = 340;
    private static final Color[] FILL = {new Color(0xA99ADD), new Color(0xEFA34E), new Color(0x70BCA4)};
    private static final Color[] INK = {new Color(0x6B55B8), new Color(0xB86A12), new Color(0x2F8A6E)};

    private final double[] angles = new double[3];   // 꼭대기, 왼쪽, 오른쪽 (도)
    private final boolean[] unknown = new boolean[3];
    private final boolean grow;
    private final boolean impossible;
    private final Timer timer;
    private final long born = System.currentTimeMillis();
    private long hintAt = -1;
    private long revealAt = -1;
    private boolean correct;
    private final List<double[]> stars = new ArrayList<>();

    /** figure 칸이 삼각형 그림이면 그림을, 아니면 null을 돌려준다. */
    public static TriangleFigure of(String figure) {
        if (figure == null || !figure.startsWith("angles:")) return null;
        String body = figure.substring("angles:".length());
        boolean grow = body.endsWith(";grow");
        if (grow) body = body.substring(0, body.length() - ";grow".length());
        String[] parts = body.split(",");
        return parts.length == 3 ? new TriangleFigure(parts, grow) : null;
    }

    private TriangleFigure(String[] parts, boolean grow) {
        this.grow = grow;
        double known = 0;
        int missing = 0;
        for (int i = 0; i < 3; i++) {
            unknown[i] = parts[i].trim().equals("?");
            if (unknown[i]) missing++;
            else known += angles[i] = Double.parseDouble(parts[i].trim());
        }
        impossible = known > 180 || (missing > 0 && known >= 180);
        for (int i = 0; i < 3; i++) {
            if (unknown[i]) angles[i] = impossible ? 0 : (180 - known) / missing;
        }
        setPreferredSize(new Dimension(460, 340));
        setMinimumSize(new Dimension(300, 220));
        setOpaque(false);
        timer = new Timer(30, e -> repaint());
    }

    /** 힌트 장면을 시작한다. */
    public void showHint() {
        if (hintAt < 0) hintAt = System.currentTimeMillis();
    }

    /** 모르는 각을 답으로 바꾸고, 맞히면 별을, 틀리면 흔들림을 보여 준다. */
    public void reveal(boolean correct) {
        if (revealAt >= 0) return;
        this.correct = correct;
        revealAt = System.currentTimeMillis();
        if (correct) {
            Random random = new Random();
            for (int i = 0; i < 36; i++) {
                double direction = random.nextDouble() * Math.PI * 2, speed = 2 + random.nextDouble() * 5;
                stars.add(new double[]{Math.cos(direction) * speed, Math.sin(direction) * speed - 3, random.nextInt(4)});
            }
        }
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
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            double scale = Math.min(getWidth() / W, getHeight() / H);
            g.translate((getWidth() - W * scale) / 2, (getHeight() - H * scale) / 2);
            g.scale(scale, scale);
            long now = System.currentTimeMillis();
            if (revealAt >= 0 && !correct) {
                double p = Math.min(1, (now - revealAt) / 450.0);
                g.translate(Math.sin((now - revealAt) / 28.0) * 9 * (1 - p), 0);
            }
            if (impossible) drawNeverMeet(g, now);
            else drawTriangle(g, now);
            drawStars(g, now);
        } finally {
            g.dispose();
        }
    }

    /** 꼭대기·왼쪽·오른쪽 꼭짓점을 그림 영역에 맞춘다. grow면 크기를 천천히 바꾼다. */
    private Point2D.Double[] vertices(long now) {
        double b = Math.toRadians(angles[1]), c = Math.toRadians(angles[2]);
        double k = Math.sin(c) / Math.sin(b + c);
        double ax = Math.cos(b) * k, ay = -Math.sin(b) * k;   // 밑변 (0,0)-(1,0) 기준
        double minX = Math.min(0, ax), maxX = Math.max(1, ax);
        double fit = Math.min(360 / (maxX - minX), 200 / -ay);
        fit *= growFactor(now);
        double left = 230 - (maxX - minX) * fit / 2 - minX * fit, base = 130 + (-ay) * fit / 2;
        return new Point2D.Double[]{
                new Point2D.Double(left + ax * fit, base + ay * fit),
                new Point2D.Double(left, base),
                new Point2D.Double(left + fit, base)};
    }

    /** grow 힌트 중이면 0.6~1.0 사이로 천천히 커졌다 작아진다. */
    private double growFactor(long now) {
        return grow && hintAt >= 0 ? 0.8 + 0.2 * Math.cos((now - hintAt) / 600.0) : 1;
    }

    private void drawTriangle(Graphics2D g, long now) {
        Point2D.Double[] p = vertices(now);
        double size = growFactor(now);
        double drawIn = Math.min(1, (now - born) / 700.0);
        Path2D shape = new Path2D.Double();
        shape.moveTo(p[0].x, p[0].y);
        shape.lineTo(p[1].x, p[1].y);
        shape.lineTo(p[2].x, p[2].y);
        shape.closePath();
        g.setColor(new Color(204, 231, 218, (int) (255 * drawIn)));
        g.fill(shape);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.GREEN);
        for (int i = 0; i < 3; i++) {
            Point2D.Double from = p[i], to = p[(i + 1) % 3];
            g.draw(new Line2D.Double(from.x, from.y, from.x + (to.x - from.x) * drawIn, from.y + (to.y - from.y) * drawIn));
        }

        double gather = hintAt < 0 || grow ? 0 : ease(Math.min(1, (now - hintAt - 300) / 1600.0));
        // 한 줄로 모일 때 오른쪽·꼭대기·왼쪽 순서로 0°~180°를 채운다.
        double[] target = {angles[2], 180 - angles[1], 0};
        for (int i = 0; i < 3; i++) {
            Point2D.Double v = p[i], a = p[(i + 1) % 3], b = p[(i + 2) % 3];
            double start = direction(v, a), extent = signedDelta(start, direction(v, b));
            if (extent < 0) {
                start += extent;
                extent = -extent;
            }
            double cx = v.x + (230 - v.x) * gather, cy = v.y + (300 - v.y) * gather;
            double angle = start + signedDelta(start, target[i]) * gather;
            double r = (40 + 20 * gather) * size;
            g.setColor(withAlpha(FILL[i], 210));
            g.fill(new Arc2D.Double(cx - r, cy - r, r * 2, r * 2, angle, extent, Arc2D.PIE));
            if (gather == 0) drawLabel(g, i, v, start + extent / 2, 66 * size, now);
        }
        if (gather >= 1) {
            g.setColor(Theme.GREEN);
            g.setStroke(new BasicStroke(3));
            g.draw(new Line2D.Double(130, 300, 330, 300));
            g.setFont(Theme.font(Font.BOLD, 22));
            centered(g, label(1) + " + " + label(0) + " + " + label(2) + " = 180°", 230, 334);
        }
        if (grow && hintAt >= 0) {
            g.setColor(Theme.GREEN);
            g.setFont(Theme.font(Font.BOLD, 22));
            centered(g, "크기가 바뀌어도 각은 그대로!", 230, 320);
        }
    }

    /** 두 각의 합이 180° 이상이면 두 변이 나란히 올라가 만나지 않는 장면. */
    private void drawNeverMeet(Graphics2D g, long now) {
        double drawIn = Math.min(1, (now - born) / 700.0);
        double rise = hintAt < 0 ? 0 : ease(Math.min(1, (now - hintAt) / 1400.0));
        double top = 110 - 90 * rise;
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.GREEN);
        g.draw(new Line2D.Double(110, 250, 110 + 240 * drawIn, 250));
        g.draw(new Line2D.Double(110, 250, 110, 250 - (250 - top) * drawIn));
        g.draw(new Line2D.Double(350, 250, 350, 250 - (250 - top) * drawIn));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{8, 10}, 0));
        g.draw(new Line2D.Double(110, top - 6, 110, top - 40));
        g.draw(new Line2D.Double(350, top - 6, 350, top - 40));
        for (int i = 1; i < 3; i++) {
            double x = i == 1 ? 110 : 322;
            g.setColor(withAlpha(FILL[i], 200));
            g.fill(new java.awt.geom.Rectangle2D.Double(x, 222, 28, 28));
            g.setColor(INK[i]);
            g.setFont(Theme.font(Font.BOLD, 22));
            centered(g, label(i), i == 1 ? 152 : 308, 240);
        }
        drawLabel(g, 0, new Point2D.Double(230, 150), Double.NaN, 0, now);
        if (rise >= 1) {
            g.setColor(new Color(0xC2453D));
            g.setFont(Theme.font(Font.BOLD, 24));
            centered(g, "두 변이 만나지 않아요!", 230, 300);
        }
    }

    /** 각 이름표. 모르는 각은 채점 전까지 통통 튀는 노란 ?, 채점 후에는 답이 튀어나온다. */
    private void drawLabel(Graphics2D g, int i, Point2D.Double vertex, double middle, double distance, long now) {
        double x = vertex.x, y = vertex.y;
        if (!Double.isNaN(middle)) {
            x += Math.cos(Math.toRadians(middle)) * distance;
            y -= Math.sin(Math.toRadians(middle)) * distance;
        }
        if (unknown[i] && revealAt < 0) {
            double pulse = 1 + 0.12 * Math.sin((now - born) / 160.0);
            AffineTransform saved = g.getTransform();
            g.translate(x, y);
            g.scale(pulse, pulse);
            g.setColor(Theme.YELLOW);
            g.fill(new Ellipse2D.Double(-19, -19, 38, 38));
            g.setColor(new Color(0xD9A400));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Ellipse2D.Double(-19, -19, 38, 38));
            g.setColor(Theme.TEXT);
            g.setFont(Theme.font(Font.BOLD, 24));
            centered(g, "?", 0, 9);
            g.setTransform(saved);
            return;
        }
        double pop = unknown[i] ? 1 + 0.5 * Math.max(0, 1 - (now - revealAt) / 400.0) : 1;
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        g.scale(pop, pop);
        g.setColor(unknown[i] ? Theme.GREEN : INK[i]);
        g.setFont(Theme.font(Font.BOLD, unknown[i] ? 28 : 23));
        centered(g, label(i), 0, 8);
        g.setTransform(saved);
    }

    private void drawStars(Graphics2D g, long now) {
        if (stars.isEmpty()) return;
        double t = (now - revealAt) / 30.0;
        if (t > 50) return;
        Color[] colors = {new Color(0xF2B705), INK[0], new Color(0xF08A24), INK[2]};
        for (double[] s : stars) {
            double x = 230 + s[0] * t, y = 150 + s[1] * t + 0.12 * t * t;
            g.setColor(withAlpha(colors[(int) s[2]], (int) (255 * Math.max(0, 1 - t / 50))));
            g.fill(star(x, y, 11));
        }
    }

    private String label(int i) {
        if (unknown[i] && revealAt < 0) return "?";
        return Math.round(angles[i]) + "°";
    }

    private static Path2D star(double cx, double cy, double r) {
        Path2D path = new Path2D.Double();
        for (int k = 0; k < 10; k++) {
            double radius = k % 2 == 0 ? r : r * 0.45, angle = Math.PI / 5 * k - Math.PI / 2;
            if (k == 0) path.moveTo(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
            else path.lineTo(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
        }
        path.closePath();
        return path;
    }

    /** 화면 좌표의 방향을 Arc2D 각도(동쪽 0°, 반시계 +)로 바꾼다. */
    private static double direction(Point2D.Double from, Point2D.Double to) {
        return Math.toDegrees(Math.atan2(-(to.y - from.y), to.x - from.x));
    }

    private static double signedDelta(double from, double to) {
        double d = (to - from) % 360;
        if (d > 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    private static double ease(double t) {
        t = Math.max(0, t);
        return t * t * (3 - 2 * t);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static void centered(Graphics2D g, String text, double cx, double baseline) {
        g.drawString(text, (float) (cx - g.getFontMetrics().stringWidth(text) / 2.0), (float) baseline);
    }
}
