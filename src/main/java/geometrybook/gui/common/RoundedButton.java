package geometrybook.gui.common;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;

/** 둥근 배경을 직접 그리는 버튼. 비활성화 시 배경색을 밝게 표시한다. */
public final class RoundedButton extends JButton {
    /** @param text 버튼에 표시할 텍스트; 클릭 동작은 리스너로 별도 등록한다. */
    public RoundedButton(String text) {
        super(text);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
    }

    /** 둥근 배경을 그린 뒤 Swing의 버튼 텍스트 등을 그린다. */
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isEnabled() ? getBackground() : getBackground().brighter());
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        g.dispose();
        super.paintComponent(graphics);
    }
}
