package geometrybook.gui.common;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JProgressBar;

/** 배경과 진행 영역을 둥글게 그리는 가로 진행 바. 진행 값은 JProgressBar 방식으로 설정한다. */
public final class RoundedProgressBar extends JProgressBar {
    private static final int TRACK_HEIGHT = 14;

    /**
     * @param min 진행 값의 최솟값
     * @param max 진행 값의 최댓값
     */
    public RoundedProgressBar(int min, int max) {
        super(min, max);
        setOpaque(false);
        setBorderPainted(false);
        setBorder(null);
        setForeground(Theme.GREEN);
        setBackground(new Color(0xDCE6DF));
        setPreferredSize(new Dimension(160, TRACK_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, TRACK_HEIGHT));
    }

    /** 남는 세로 공간의 중앙에 둥근 트랙과 현재 비율만큼의 진행 영역을 그린다. */
    @Override
    protected void paintComponent(Graphics graphics) {
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int availableHeight = getHeight() - insets.top - insets.bottom;
        int height = Math.min(TRACK_HEIGHT, availableHeight);
        if (width <= 0 || height <= 0) return;

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int x = insets.left;
        int y = insets.top + (availableHeight - height) / 2;
        g.setColor(getBackground());
        g.fill(new RoundRectangle2D.Double(x, y, width, height, height, height));

        double range = (double) getMaximum() - getMinimum();
        double fraction = range > 0 ? (getValue() - (double) getMinimum()) / range : 0;
        double filledWidth = width * Math.max(0, Math.min(1, fraction));
        if (filledWidth > 0) {
            double start = getComponentOrientation().isLeftToRight() ? x : x + width - filledWidth;
            g.setColor(getForeground());
            g.fill(new RoundRectangle2D.Double(start, y, filledWidth, height, height, height));
        }
        g.dispose();
    }
}
