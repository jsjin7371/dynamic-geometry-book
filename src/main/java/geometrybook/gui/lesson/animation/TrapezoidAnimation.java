package geometrybook.gui.lesson.animation;

import geometrybook.gui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

public final class TrapezoidAnimation extends LessonAnimation {
    private static final double[] X = {190, 410, 500, 100};
    private static final double[] Y = {85, 85, 245, 245};
    private static final Color ORANGE = new Color(0xD98A30);
    private static final Color PURPLE = new Color(0x8D71C8);
    private static final Color LIGHT_PURPLE = new Color(0xDED5F3);

    public TrapezoidAnimation(Runnable onUpdate) {
        super(onUpdate);
    }

    @Override
    public String caption() {
        if (elapsed < 6000) return "네 점을 차례로 이어 사다리꼴을 만들어요.";
        if (elapsed < 11000) return "마주 보는 한 쌍의 변이 서로 평행해요.";
        if (elapsed < 16000) return "평행한 두 변 사이의 수직 거리가 높이예요.";
        if (elapsed < 20000) return "두 밑변의 길이를 더하고 높이를 곱해요.";
        return "사다리꼴의 넓이는 (윗변 + 아랫변) × 높이 ÷ 2예요!";
    }

    @Override
    protected void draw(Graphics2D g) {
        if (elapsed >= 6000) {
            g.setColor(Theme.MINT);
            g.fill(trapezoid());
        }

        paintSides(g);

        if (elapsed >= 6000) {
            paintParallelMarks(g);
        }

        if (elapsed >= 11000) {
            paintHeight(g);
        }

        if (elapsed >= 16000) {
            paintBaseLabels(g);
        }

        if (elapsed >= 20000) {
            paintFormula(g);
        }

        paintVertices(g);
    }

    private void paintSides(Graphics2D g) {
        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(5,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            double fraction = Math.max(0,
                    Math.min(1, (elapsed - i * 1500) / 1500.0));

            g.draw(new Line2D.Double(
                    X[i],
                    Y[i],
                    X[i] + (X[next] - X[i]) * fraction,
                    Y[i] + (Y[next] - Y[i]) * fraction
            ));
        }
    }

    private void paintParallelMarks(Graphics2D g) {
        g.setColor(ORANGE);
        g.setStroke(new BasicStroke(3,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

        g.draw(new Line2D.Double(285, 85, 285, 100));
        g.draw(new Line2D.Double(315, 85, 315, 100));

        g.draw(new Line2D.Double(285, 245, 285, 230));
        g.draw(new Line2D.Double(315, 245, 315, 230));

        g.setFont(Theme.font(Font.BOLD, 17));
        centered(g, "평행", 300, 65);
    }

    private void paintHeight(Graphics2D g) {
        g.setColor(PURPLE);
        g.setStroke(new BasicStroke(3,
                BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER,
                10,
                new float[]{7, 6},
                0));

        g.draw(new Line2D.Double(190, 85, 190, 245));

        g.setStroke(new BasicStroke(2.5f));
        rightAngle(g, 190, 245);

        g.setFont(Theme.font(Font.BOLD, 20));
        g.setColor(PURPLE);
        g.drawString("높이", 202, 175);
    }

    private void paintBaseLabels(Graphics2D g) {
        g.setFont(Theme.font(Font.BOLD, 18));
        g.setColor(ORANGE);

        centered(g, "윗변", 300, 125);
        centered(g, "아랫변", 300, 275);
    }

    private void paintFormula(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(105, 285, 390, 48, 16, 16);

        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(105, 285, 390, 48, 16, 16);

        g.setFont(Theme.font(Font.BOLD, 19));
        centered(g, "(윗변 + 아랫변) × 높이 ÷ 2", 300, 316);
    }

    private void paintVertices(Graphics2D g) {
        g.setColor(ORANGE);

        for (int i = 0; i < 4; i++) {
            g.fill(new Ellipse2D.Double(
                    X[i] - 7,
                    Y[i] - 7,
                    14,
                    14
            ));
        }
    }

    private static void rightAngle(Graphics2D g, double x, double y) {
        Path2D mark = new Path2D.Double();
        mark.moveTo(x + 15, y);
        mark.lineTo(x + 15, y - 15);
        mark.lineTo(x, y - 15);
        g.draw(mark);
    }

    private static Path2D trapezoid() {
        Path2D path = new Path2D.Double();
        path.moveTo(X[0], Y[0]);

        for (int i = 1; i < 4; i++) {
            path.lineTo(X[i], Y[i]);
        }

        path.closePath();
        return path;
    }
}
