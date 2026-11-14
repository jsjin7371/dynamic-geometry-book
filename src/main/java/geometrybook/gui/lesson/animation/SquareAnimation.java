package geometrybook.gui.lesson.animation;

import geometrybook.gui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;

/** 같은 길이의 네 변과 네 직각을 강조하는 정사각형 애니메이션. */
public final class SquareAnimation extends LessonAnimation {
    public SquareAnimation(Runnable onUpdate) {
        super(onUpdate);
    }

    @Override public String caption() {
        if (elapsed < 8000) return "네 점을 차례로 이어 정사각형을 만들어요.";
        if (elapsed < 13000) return "같은 표시가 있는 네 변의 길이는 모두 같아요.";
        if (elapsed < 18000) return "네 각은 모두 직각이에요. 직각은 90도예요.";
        return "돌려도 네 변의 길이와 네 직각은 그대로예요!";
    }

    @Override protected void draw(Graphics2D g) {
        g.translate(300, 170);   // 화면 가운데를 원점으로 그린다
        double rotation = Math.max(0, Math.min(1, (elapsed - 18000) / 5000.0));
        g.rotate(rotation * Math.PI / 4);
        int[] x = {-100, 100, 100, -100};
        int[] y = {-100, -100, 100, 100};
        if (elapsed >= 8000) {
            g.setColor(Theme.MINT);
            g.fillRect(-100, -100, 200, 200);
        }
        g.setColor(Theme.GREEN);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            double fraction = Math.max(0, Math.min(1, (elapsed - i * 2000) / 2000.0));
            g.draw(new Line2D.Double(x[i], y[i], x[i] + (x[next] - x[i]) * fraction,
                    y[i] + (y[next] - y[i]) * fraction));
        }
        if (elapsed >= 8000) {
            g.setColor(new Color(0xD98A30));
            g.setStroke(new BasicStroke(3));
            g.drawLine(-8, -110, 8, -90);
            g.drawLine(-8, 90, 8, 110);
            g.drawLine(-110, -8, -90, 8);
            g.drawLine(90, -8, 110, 8);
        }
        if (elapsed >= 13000) {
            g.setColor(new Color(0x8D71C8));
            g.setStroke(new BasicStroke(2));
            for (int i = 0; i < 4; i++) {
                int dx = -Integer.signum(x[i]) * 20;
                int dy = -Integer.signum(y[i]) * 20;
                g.drawLine(x[i] + dx, y[i], x[i] + dx, y[i] + dy);
                g.drawLine(x[i] + dx, y[i] + dy, x[i], y[i] + dy);
            }
            g.setFont(Theme.font(Font.BOLD, 20));
            g.drawString("90°", -20, 8);
        }
        g.setColor(new Color(0xECA345));
        for (int i = 0; i < 4; i++) {
            g.fillOval(x[i] - 6, y[i] - 6, 12, 12);
        }
    }
}
