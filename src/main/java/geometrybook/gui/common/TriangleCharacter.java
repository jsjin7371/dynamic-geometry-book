package geometrybook.gui.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JComponent;

/** 로그인과 나의 탐험 화면에서 공유하는 벡터 삼각형 캐릭터. */
public final class TriangleCharacter extends JComponent {
    /** 기본 크기의 투명 컴포넌트를 만든다. 레이아웃에 맞춰 비율을 유지하며 확대·축소된다. */
    public TriangleCharacter() {
        setPreferredSize(new Dimension(150, 130));
        setOpaque(false);
    }

    /** 컴포넌트 영역 중앙에 캐릭터를 비율을 유지해 그린다. */
    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double scale = Math.min(getWidth() / 132.0, getHeight() / 115.0);
        g.translate((getWidth() - 132 * scale) / 2, (getHeight() - 115 * scale) / 2);
        g.scale(scale, scale);
        draw(g);
        g.dispose();
    }

    /**
     * 기존 그래픽에 캐릭터를 그린다. 원점 기준 132×115 좌표계를 사용하며
     * 위치와 크기 변환은 호출자가 적용한다. 전달된 그래픽 상태는 변경하지 않는다.
     * @param g 캐릭터를 그릴 그래픽
     */
    public static void draw(Graphics2D g) {
        Graphics2D copy = (Graphics2D) g.create();
        copy.setStroke(new BasicStroke(2));
        Path2D triangle = new Path2D.Double();
        triangle.moveTo(66, 1); triangle.lineTo(2, 112); triangle.lineTo(130, 112); triangle.closePath();
        copy.setColor(new Color(0xCBE8D6)); copy.fill(triangle);
        copy.setColor(Theme.GREEN); copy.draw(triangle);
        drawFace(copy, 66, 73);
        copy.dispose();
    }

    /**
     * 다른 도형에도 같은 표정을 그린다. 호출 후 그래픽 색상은 변경된 상태로 남는다.
     * @param g 얼굴을 그릴 그래픽
     * @param x 얼굴의 가로 중심 좌표
     * @param y 얼굴 기준 세로 좌표
     */
    public static void drawFace(Graphics2D g, int x, int y) {
        g.setColor(Theme.TEXT); g.fillOval(x - 17, y - 8, 6, 9); g.fillOval(x + 11, y - 8, 6, 9);
        g.drawArc(x - 9, y - 2, 18, 14, 200, 140);
        g.setColor(new Color(0xF5B9A7)); g.fillOval(x - 30, y + 4, 14, 7); g.fillOval(x + 16, y + 4, 14, 7);
    }
}
