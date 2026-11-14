package geometrybook.gui.lesson.animation;

import geometrybook.gui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/** 중심에서 같은 거리로 원을 그리고 반지름과 지름의 관계를 설명한다. */
public final class CircleAnimation extends LessonAnimation {
    private static final int CX = 300;
    private static final int CY = 155;
    private static final int RADIUS = 105;
    private static final Color RADIUS_COLOR = new Color(0xB77924);
    private static final Color SECOND_RADIUS_COLOR = new Color(0x8D71C8);

    public CircleAnimation(Runnable onUpdate) {
        super(onUpdate);
    }

    @Override public String caption() {
        if (elapsed < 6000) return "중심에서 같은 거리를 유지하며 원을 그려요.";
        if (elapsed < 12000) return "중심에서 원 위의 한 점까지 이은 선분이 반지름이에요.";
        if (elapsed < 18000) return "방향이 달라도 한 원의 반지름은 모두 길이가 같아요.";
        return "중심을 지나는 지름은 반지름의 두 배예요. 3cm의 두 배는 6cm!";
    }

    @Override protected void draw(Graphics2D g) {
        if (elapsed >= 6000) {
            g.setColor(Theme.MINT);
            g.fill(new Ellipse2D.Double(CX - RADIUS, CY - RADIUS, RADIUS * 2, RADIUS * 2));
        }
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.GREEN);
        double sweep = Math.min(1, elapsed / 6000.0) * 360;
        g.draw(new Arc2D.Double(CX - RADIUS, CY - RADIUS, RADIUS * 2, RADIUS * 2,
                0, sweep, Arc2D.OPEN));
        g.setFont(Theme.font(Font.BOLD, 19));
        if (elapsed < 6000) {
            drawRadius(g, Math.toRadians(sweep), RADIUS_COLOR);
        } else if (elapsed < 12000) {
            drawRadius(g, 0, RADIUS_COLOR);
            g.drawString("반지름", CX + 28, CY - 15);
            g.setColor(Theme.TEXT);
            g.drawString("원 위의 점", CX + RADIUS + 12, CY + 6);
        } else if (elapsed < 18000) {
            drawRadius(g, 0, RADIUS_COLOR);
            double rotation = (elapsed - 12000) / 6000.0 * Math.PI * 2;
            drawRadius(g, rotation, SECOND_RADIUS_COLOR);
            g.setColor(Theme.TEXT);
            centeredText(g, "두 선분의 길이를 비교해 보세요", 304);
        } else {
            drawDiameter(g);
        }
        g.setColor(Theme.GREEN);
        g.fill(new Ellipse2D.Double(CX - 5, CY - 5, 10, 10));
        g.setFont(Theme.font(Font.BOLD, 17));
        g.drawString("중심", CX - 16, CY + 28);
    }

    private void drawRadius(Graphics2D g, double angle, Color color) {
        double x = CX + RADIUS * Math.cos(angle);
        double y = CY - RADIUS * Math.sin(angle);
        g.setColor(color);
        g.draw(new Line2D.Double(CX, CY, x, y));
        g.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
    }

    private void drawDiameter(Graphics2D g) {
        drawRadius(g, 0, RADIUS_COLOR);
        g.drawString("3cm", CX + 35, CY - 14);
        double fraction = Math.min(1, (elapsed - 18000) / 2000.0);
        g.setColor(SECOND_RADIUS_COLOR);
        g.draw(new Line2D.Double(CX, CY, CX - RADIUS * fraction, CY));
        if (fraction < 1) return;
        g.fillOval(CX - RADIUS - 5, CY - 5, 10, 10);
        g.drawString("3cm", CX - 76, CY - 14);
        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(2));
        g.drawLine(CX - RADIUS, 280, CX + RADIUS, 280);
        g.drawLine(CX - RADIUS, 274, CX - RADIUS, 286);
        g.drawLine(CX + RADIUS, 274, CX + RADIUS, 286);
        centeredText(g, "지름 = 3 + 3 = 6cm", 309);
    }

    private static void centeredText(Graphics2D g, String text, int y) {
        g.drawString(text, (600 - g.getFontMetrics().stringWidth(text)) / 2, y);
    }
}
