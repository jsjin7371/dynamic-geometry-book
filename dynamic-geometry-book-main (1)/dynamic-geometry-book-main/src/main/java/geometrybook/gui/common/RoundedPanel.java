package geometrybook.gui.common;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** 둥근 배경을 직접 그리는 패널. 자식의 배치와 안쪽 여백은 호출자가 설정한다. */
public class RoundedPanel extends JPanel {
    private final int radius;

    /**
     * @param layout 자식 컴포넌트를 배치할 레이아웃
     * @param color 배경색
     * @param radius 둥근 모서리를 그릴 때 사용할 호의 너비와 높이
     */
    public RoundedPanel(LayoutManager layout, Color color, int radius) {
        super(layout);
        this.radius = radius;
        setBackground(color);
        setOpaque(false);
    }

    /** 둥근 배경을 그린다. Swing이 호출하므로 직접 호출하지 않는다. */
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(getBackground());
        g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        g.dispose();
        super.paintComponent(graphics);
    }
}
