package geometrybook.gui.lesson.animation;

import geometrybook.gui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public final class ParallelogramAnimation extends LessonAnimation {
    private static final double[] X = {200, 500, 400, 100};
    private static final double[] Y = {40, 40, 220, 220};
    private static final double FOOT_X = 200;
    private static final double SHIFT = 300;
    private static final Color ORANGE = new Color(0xD98A30);
    private static final Color PURPLE = new Color(0x8D71C8);
    private static final Color TRIANGLE_FILL = new Color(0xDED5F3);

    public ParallelogramAnimation(Runnable onUpdate) {
        super(onUpdate);
    }

    @Override public String caption() {
        if (elapsed < 6000) return "네 점을 차례로 이어 평행사변형을 만들어요.";
        if (elapsed < 10000) return "마주 보는 두 쌍의 변은 서로 평행해요.";
        if (elapsed < 14000) return "마주 보는 두 각의 크기는 서로 같아요.";
        if (elapsed < 18000) return "밑변에 수직인 선분의 길이를 높이라고 해요.";
        if (elapsed < 22000) return "삼각형을 옮기면 직사각형이 돼요. 넓이는 그대로예요!";
        return "평행사변형의 넓이는 밑변 × 높이예요!";
    }

    @Override protected void draw(Graphics2D g) {
        double move = Math.max(0, Math.min(1, (elapsed - 18000) / 4000.0));
        if (elapsed < 18000) {
            paintWhole(g);
        } else {
            paintCut(g, move);
        }
        paintLabels(g, move);
    }

    private void paintWhole(Graphics2D g) {
        if (elapsed >= 6000) {
            g.setColor(Theme.MINT);
            g.fill(parallelogram());
        }
        if (elapsed >= 14000) {
            g.setColor(TRIANGLE_FILL);
            g.fill(triangle());
        }
        if (elapsed >= 10000 && elapsed < 14000) {
            paintAngles(g);
        }
        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            double fraction = Math.max(0, Math.min(1, (elapsed - i * 1500) / 1500.0));
            g.draw(new Line2D.Double(X[i], Y[i], X[i] + (X[next] - X[i]) * fraction,
                    Y[i] + (Y[next] - Y[i]) * fraction));
        }
        if (elapsed >= 6000 && elapsed < 14000) {
            paintParallelMarks(g);
        }
        if (elapsed >= 14000) {
            paintHeight(g);
        }
        g.setColor(new Color(0xECA345));
        for (int i = 0; i < 4; i++) {
            g.fill(new Ellipse2D.Double(X[i] - 7, Y[i] - 7, 14, 14));
        }
    }

    private void paintCut(Graphics2D g, double move) {
        g.setColor(Theme.MINT);
        g.fill(trapezoid());
        AffineTransform shift = AffineTransform.getTranslateInstance(SHIFT * move, 0);
        g.setColor(TRIANGLE_FILL);
        g.fill(shift.createTransformedShape(triangle()));
        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(trapezoid());
        g.draw(shift.createTransformedShape(triangle()));
        if (move == 0) {
            paintHeight(g);
        }
        if (move < 1) {
            return;
        }
        g.setColor(PURPLE);
        g.setStroke(new BasicStroke(2));
        rightAngle(g, FOOT_X, 40, 1, 1);
        rightAngle(g, 500, 40, -1, 1);
        rightAngle(g, 500, 220, -1, -1);
        rightAngle(g, FOOT_X, 220, 1, -1);
        g.setColor(new Color(0xECA345));
        double[] cx = {FOOT_X, 500, 500, FOOT_X};
        double[] cy = {40, 40, 220, 220};
        for (int i = 0; i < 4; i++) {
            g.fill(new Ellipse2D.Double(cx[i] - 7, cy[i] - 7, 14, 14));
        }
    }

    private void paintLabels(Graphics2D g, double move) {
        if (elapsed >= 14000) {
            g.setFont(Theme.font(Font.BOLD, 20));
            label(g, "밑변", 250 + 100 * move, 254, ORANGE);
            label(g, "높이", FOOT_X + 24 + SHIFT * move, 138, PURPLE);
        }
        if (move >= 1) {
            g.setFont(Theme.font(Font.BOLD, 24));
            g.setColor(Theme.GREEN);
            centered(g, "넓이 = 밑변 × 높이", 300, 300);
        }
    }

    private void paintParallelMarks(Graphics2D g) {
        g.setColor(ORANGE);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        chevron(g, 350, 40, 1, 0);
        chevron(g, 250, 220, 1, 0);
        double length = Math.hypot(X[3] - X[0], Y[3] - Y[0]);
        double ux = (X[3] - X[0]) / length;
        double uy = (Y[3] - Y[0]) / length;
        double[][] mids = {{(X[0] + X[3]) / 2, (Y[0] + Y[3]) / 2}, {(X[1] + X[2]) / 2, (Y[1] + Y[2]) / 2}};
        for (double[] mid : mids) {
            chevron(g, mid[0] - ux * 5, mid[1] - uy * 5, ux, uy);
            chevron(g, mid[0] + ux * 5, mid[1] + uy * 5, ux, uy);
        }
    }

    private void paintAngles(Graphics2D g) {
        int[] acute = {1, 3};
        int[] obtuse = {0, 2};
        for (int i : acute) {
            angleMark(g, i, PURPLE, new int[]{42});
        }
        for (int i : obtuse) {
            angleMark(g, i, ORANGE, new int[]{30, 40});
        }
    }

    private void angleMark(Graphics2D g, int vertex, Color color, int[] radii) {
        int previous = (vertex + 3) % 4;
        int next = (vertex + 1) % 4;
        double start = Math.toDegrees(Math.atan2(-(Y[previous] - Y[vertex]), X[previous] - X[vertex]));
        double end = Math.toDegrees(Math.atan2(-(Y[next] - Y[vertex]), X[next] - X[vertex]));
        double extent = end - start;
        while (extent > 180) extent -= 360;
        while (extent <= -180) extent += 360;
        int outer = radii[radii.length - 1];
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
        g.fill(new Arc2D.Double(X[vertex] - outer, Y[vertex] - outer, outer * 2, outer * 2, start, extent, Arc2D.PIE));
        g.setColor(color);
        g.setStroke(new BasicStroke(3));
        for (int radius : radii) {
            g.draw(new Arc2D.Double(X[vertex] - radius, Y[vertex] - radius, radius * 2, radius * 2, start, extent, Arc2D.OPEN));
        }
    }

    private void paintHeight(Graphics2D g) {
        g.setColor(PURPLE);
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{7, 6}, 0));
        g.draw(new Line2D.Double(FOOT_X, 40, FOOT_X, 220));
        g.setStroke(new BasicStroke(2));
        rightAngle(g, FOOT_X, 220, -1, -1);
    }

    private static void chevron(Graphics2D g, double cx, double cy, double ux, double uy) {
        double nx = -uy;
        double ny = ux;
        double tipX = cx + ux * 4;
        double tipY = cy + uy * 4;
        double tailX = cx - ux * 4;
        double tailY = cy - uy * 4;
        g.draw(new Line2D.Double(tailX + nx * 7, tailY + ny * 7, tipX, tipY));
        g.draw(new Line2D.Double(tailX - nx * 7, tailY - ny * 7, tipX, tipY));
    }

    private static void rightAngle(Graphics2D g, double x, double y, int dx, int dy) {
        Path2D mark = new Path2D.Double();
        mark.moveTo(x + dx * 14, y);
        mark.lineTo(x + dx * 14, y + dy * 14);
        mark.lineTo(x, y + dy * 14);
        g.draw(mark);
    }

    private static void label(Graphics2D g, String text, double cx, double y, Color color) {
        FontMetrics metrics = g.getFontMetrics();
        GlyphVector glyphs = g.getFont().createGlyphVector(g.getFontRenderContext(), text);
        Shape outline = glyphs.getOutline((float) (cx - metrics.stringWidth(text) / 2.0), (float) y);
        g.setColor(new Color(255, 255, 255, 235));
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outline);
        g.setColor(color);
        g.fill(outline);
    }

    private static Path2D parallelogram() {
        Path2D path = new Path2D.Double();
        path.moveTo(X[0], Y[0]);
        for (int i = 1; i < 4; i++) path.lineTo(X[i], Y[i]);
        path.closePath();
        return path;
    }

    private static Path2D triangle() {
        Path2D path = new Path2D.Double();
        path.moveTo(X[3], Y[3]);
        path.lineTo(X[0], Y[0]);
        path.lineTo(FOOT_X, Y[3]);
        path.closePath();
        return path;
    }

    private static Path2D trapezoid() {
        Path2D path = new Path2D.Double();
        path.moveTo(FOOT_X, Y[3]);
        path.lineTo(X[2], Y[2]);
        path.lineTo(X[1], Y[1]);
        path.lineTo(X[0], Y[0]);
        path.closePath();
        return path;
    }
}
