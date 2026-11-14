package geometrybook.gui.lesson.animation;

import geometrybook.gui.common.Theme;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/** 세 점, 변, 꼭짓점, 내각을 모으는 장면을 순서대로 그리는 애니메이션. */
public final class TriangleAnimation extends LessonAnimation {
    public TriangleAnimation(Runnable onUpdate) {
        super(onUpdate);
    }

    @Override public String caption() {
        if (elapsed < 3000) return "서로 일직선 위에 있지 않은 세 점을 찾아요.";
        if (elapsed < 9000) return "세 점을 곧은 선분으로 이어 삼각형을 만들어요.";
        if (elapsed < 13000) return "삼각형은 변이 3개, 꼭짓점이 3개예요.";
        if (elapsed < 18000) return "세 꼭짓점 안쪽에 있는 각을 내각이라고 해요.";
        return "세 내각을 한 점에 모으면 곧은 각, 180도가 돼요!";
    }
    @Override protected void draw(Graphics2D g) {
        double[] x = {
            300, 150, 450
        };
        double[] y = {
            45, 245, 245
        };
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(x[0], y[0]);
        triangle.lineTo(x[1], y[1]);
        triangle.lineTo(x[2], y[2]);
        triangle.closePath();
        if (elapsed >= 9000) {
            g.setColor(new Color(0xCCE7DA));
            g.fill(triangle);
        }
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.GREEN);
        for (int i = 0; i<3; i++) {
            double fraction = Math.max(0, Math.min(1, (elapsed-3000-i*2000)/2000.0));
            int next = (i+1)%3;
            g.draw(new Line2D.Double(x[i], y[i], x[i]+(x[next]-x[i])*fraction, y[i]+(y[next]-y[i])*fraction));
        }
        g.setColor(new Color(0xECA345));
        for (int i = 0; i<3; i++) g.fill(new Ellipse2D.Double(x[i]-7, y[i]-7, 14, 14));
        if (elapsed >= 13000) {
            // 밑각은 atan2(200,150)이며 화면의 y축은 아래쪽이 양수다.
            double base = Math.toDegrees(Math.atan2(200, 150)), apex = 180-2*base;
            double[] start = {
                180+base, 0, 180-base
            }, extent = {
                apex, base, base
            };
            double[] target = {
                base, 0, base+apex
            };
            Color[] colors = {
                new Color(0xA99ADD), new Color(0xEFA34E), new Color(0x70BCA4)
            };
            double f = Math.max(0, Math.min(1, (elapsed-18000)/4000.0));
            for (int i = 0; i<3; i++) {
                double cx = x[i]+(300-x[i])*f, cy = y[i]+(260-y[i])*f;
                double angle = start[i]+(target[i]-start[i])*f;
                g.setColor(colors[i]);
                g.fill(new Arc2D.Double(cx-50, cy-50, 100, 100, angle, extent[i], Arc2D.PIE));
            }
            if (f == 1) {
                g.setColor(Theme.GREEN);
                g.setStroke(new BasicStroke(2));
                g.drawLine(210, 260, 390, 260);
                g.setFont(Theme.font(Font.BOLD, 22));
                g.drawString("세 내각의 합 = 180°", 205, 310);
            }
        }
    }
}
