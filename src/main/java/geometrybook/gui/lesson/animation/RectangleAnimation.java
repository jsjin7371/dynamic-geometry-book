package geometrybook.gui.lesson.animation;

import geometrybook.gui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;

public final class RectangleAnimation extends LessonAnimation {
    private static final int LEFT = 150;
    private static final int TOP = 75;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 180;
    private static final Color MARK = new Color(0xD98A30);
    private static final Color ANGLE = new Color(0x8D71C8);

    public RectangleAnimation(Runnable onUpdate) {
        super(onUpdate);
    }

    @Override public String caption() {
        if (elapsed < 6000) return "네 점을 차례로 이어 직사각형을 만들어요.";
        if (elapsed < 12000) return "직사각형의 네 각은 모두 직각이에요.";
        if (elapsed < 18000) return "마주 보는 두 변의 길이는 서로 같아요.";
        return "가로 5cm, 세로 3cm이면 넓이는 15cm²예요.";
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = Math.min(getWidth() / 600.0, getHeight() / 340.0);
            g.translate((getWidth() - 600 * scale) / 2, (getHeight() - 340 * scale) / 2);
            g.scale(scale, scale);

            int[] x = {LEFT, LEFT + WIDTH, LEFT + WIDTH, LEFT};
            int[] y = {TOP, TOP, TOP + HEIGHT, TOP + HEIGHT};

            if (elapsed >= 6000) {
                g.setColor(Theme.MINT);
                g.fillRect(LEFT, TOP, WIDTH, HEIGHT);
            }

            g.setColor(Theme.GREEN);
            g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            for (int i = 0; i < 4; i++) {
                int next = (i + 1) % 4;
                double fraction = Math.max(0, Math.min(1, (elapsed - i * 1500) / 1500.0));
                g.draw(new Line2D.Double(x[i], y[i],
                        x[i] + (x[next] - x[i]) * fraction,
                        y[i] + (y[next] - y[i]) * fraction));
            }

            if (elapsed >= 6000) drawVertices(g, x, y);
            if (elapsed >= 12000) drawRightAngles(g);
            if (elapsed >= 18000) drawLengths(g);
        } finally {
            g.dispose();
        }
    }

    private void drawVertices(Graphics2D g, int[] x, int[] y) {
        g.setColor(MARK);
        for (int i = 0; i < 4; i++) {
            g.fillOval(x[i] - 6, y[i] - 6, 12, 12);
        }
    }

    private void drawRightAngles(Graphics2D g) {
        g.setColor(ANGLE);
        g.setStroke(new BasicStroke(3));

        g.drawLine(LEFT + 22, TOP, LEFT + 22, TOP + 22);
        g.drawLine(LEFT + 22, TOP + 22, LEFT, TOP + 22);

        g.drawLine(LEFT + WIDTH - 22, TOP, LEFT + WIDTH - 22, TOP + 22);
        g.drawLine(LEFT + WIDTH - 22, TOP + 22, LEFT + WIDTH, TOP + 22);

        g.drawLine(LEFT + WIDTH - 22, TOP + HEIGHT, LEFT + WIDTH - 22, TOP + HEIGHT - 22);
        g.drawLine(LEFT + WIDTH - 22, TOP + HEIGHT - 22, LEFT + WIDTH, TOP + HEIGHT - 22);

        g.drawLine(LEFT + 22, TOP + HEIGHT, LEFT + 22, TOP + HEIGHT - 22);
        g.drawLine(LEFT + 22, TOP + HEIGHT - 22, LEFT, TOP + HEIGHT - 22);

        g.setFont(Theme.font(Font.BOLD, 18));
        g.drawString("90°", LEFT + 30, TOP + 42);
    }

    private void drawLengths(Graphics2D g) {
        g.setColor(MARK);
        g.setStroke(new BasicStroke(3));

        g.drawLine(LEFT + WIDTH / 2, TOP - 9, LEFT + WIDTH / 2, TOP + 9);
        g.drawLine(LEFT + WIDTH / 2, TOP + HEIGHT - 9, LEFT + WIDTH / 2, TOP + HEIGHT + 9);

        g.drawLine(LEFT - 9, TOP + HEIGHT / 2, LEFT + 9, TOP + HEIGHT / 2);
        g.drawLine(LEFT + WIDTH - 9, TOP + HEIGHT / 2, LEFT + WIDTH + 9, TOP + HEIGHT / 2);

        g.setFont(Theme.font(Font.BOLD, 18));
        g.setColor(Theme.TEXT);

        g.drawString("5cm", LEFT + WIDTH / 2 - 22, TOP - 18);
        g.drawString("5cm", LEFT + WIDTH / 2 - 22, TOP + HEIGHT + 28);
        g.drawString("3cm", LEFT - 48, TOP + HEIGHT / 2 + 6);
        g.drawString("3cm", LEFT + WIDTH + 12, TOP + HEIGHT / 2 + 6);

        String formula = "넓이 = 가로 × 세로 = 5 × 3 = 15cm²";
        g.setFont(Theme.font(Font.BOLD, 20));
        g.drawString(formula, (600 - g.getFontMetrics().stringWidth(formula)) / 2, 320);
    }
}
