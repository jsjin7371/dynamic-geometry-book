package geometrybook.gui.lesson.animation;

import geometrybook.gui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;

/** 네 변의 길이, 마주 보는 각, 둘레를 24초 동안 설명한다. */
public final class RhombusAnimation extends LessonAnimation {
    private static final int[] X = {300, 480, 300, 120};
    private static final int[] Y = {40, 160, 280, 160};
    private static final Color MARK = new Color(0xB77924);
    private static final Color ANGLE_A = new Color(0xA99ADD);
    private static final Color ANGLE_B = new Color(0x70BCA4);

    public RhombusAnimation(Runnable onUpdate) {
        super(onUpdate);
    }

    @Override public String caption() {
        if (elapsed < 6000) return "네 점을 차례로 이어 마름모를 만들어요.";
        if (elapsed < 12000) return "같은 표시가 있는 네 변의 길이는 모두 같아요.";
        if (elapsed < 18000) return "같은 색으로 표시한 마주 보는 두 각의 크기는 같아요.";
        return "한 변이 4cm이면, 네 변을 더한 둘레는 16cm예요.";
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = Math.min(getWidth() / 600.0, getHeight() / 340.0);
            g.translate((getWidth() - 600 * scale) / 2, (getHeight() - 340 * scale) / 2);
            g.scale(scale, scale);
            if (elapsed >= 6000) {
                g.setColor(Theme.MINT);
                g.fill(new Polygon(X, Y, 4));
            }
            if (elapsed >= 12000 && elapsed < 18000) drawAngles(g);
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;
                double fraction = Math.max(0, Math.min(1, (elapsed - i * 1500) / 1500.0));
                g.setColor(Theme.GREEN);
                g.draw(new Line2D.Double(X[i], Y[i], X[i] + (X[next] - X[i]) * fraction,
                        Y[i] + (Y[next] - Y[i]) * fraction));
            }
            if (elapsed >= 6000) drawEqualSides(g);
            if (elapsed >= 18000) drawPerimeter(g);
            g.setColor(MARK);
            for (int i = 0; i < 4; i++) g.fillOval(X[i] - 5, Y[i] - 5, 10, 10);
        } finally {
            g.dispose();
        }
    }

    private void drawEqualSides(Graphics2D g) {
        g.setStroke(new BasicStroke(3));
        g.setColor(MARK);
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            double mx = (X[i] + X[next]) / 2.0;
            double my = (Y[i] + Y[next]) / 2.0;
            double dx = (Y[next] - Y[i]) * 0.04;
            double dy = (X[i] - X[next]) * 0.04;
            g.draw(new Line2D.Double(mx - dx, my - dy, mx + dx, my + dy));
        }
    }

    private void drawAngles(Graphics2D g) {
        // Arc2D의 각도는 화면 y축과 반대 방향으로 잰다.
        double slope = Math.toDegrees(Math.atan2(120, 180));
        double[] starts = {180 + slope, 180 - slope, slope, -slope};
        double[] extents = {180 - 2 * slope, 2 * slope, 180 - 2 * slope, 2 * slope};
        for (int i = 0; i < 4; i++) {
            g.setColor(i % 2 == 0 ? ANGLE_A : ANGLE_B);
            g.fill(new Arc2D.Double(X[i] - 38, Y[i] - 38, 76, 76,
                    starts[i], extents[i], Arc2D.PIE));
        }
    }

    private void drawPerimeter(Graphics2D g) {
        g.setFont(Theme.font(Font.BOLD, 19));
        g.setColor(Theme.TEXT);
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            int mx = (X[i] + X[next]) / 2;
            int my = (Y[i] + Y[next]) / 2;
            int offsetX = mx > 300 ? 14 : -54;
            int offsetY = my < 160 ? -12 : 25;
            g.drawString("4cm", mx + offsetX, my + offsetY);
        }
        String formula = elapsed < 21000 ? "둘레 = 네 변의 길이의 합" : "4 + 4 + 4 + 4 = 16cm";
        g.setFont(Theme.font(Font.BOLD, 22));
        g.drawString(formula, (600 - g.getFontMetrics().stringWidth(formula)) / 2, 326);
    }
}
