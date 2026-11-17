package geometrybook.gui.quiz;

import geometrybook.gui.common.Theme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;

/** 라디오 버튼 대신 쓰는 큼직한 보기 카드. OX 문제는 커다란 O·X 기호로 그린다. */
final class ChoiceTile extends JButton {
    enum State { IDLE, SELECTED, CORRECT, WRONG, DIMMED }

    private static final Color BORDER = new Color(0xDCE5DF);
    private static final Color HOVER = new Color(0xF0F8F3);
    private static final Color RIGHT = new Color(0x2E9E6B);
    private static final Color RIGHT_BG = new Color(0xD6F5E3);
    private static final Color MISS = new Color(0xD9534F);
    private static final Color MISS_BG = new Color(0xFBE0DE);
    private static final Color O_BLUE = new Color(0x3A86C8);

    private final int number;
    private final boolean ox;
    private State state = State.IDLE;

    /**
     * @param number 보기 번호 (객관식 왼쪽 동그라미에 표시)
     * @param text 보기 내용 (OX 문제는 "O" 또는 "X")
     * @param ox OX 문제면 true
     */
    ChoiceTile(int number, String text, boolean ox) {
        super(text);
        this.number = number;
        this.ox = ox;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(ox ? new Dimension(200, 160) : new Dimension(320, 76));
        getAccessibleContext().setAccessibleName("보기 " + number + ": " + text);
    }

    void setState(State state) {
        this.state = state;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            double w = getWidth(), h = getHeight();
            Color background = Color.WHITE, border = BORDER;
            float stroke = 2;
            switch (state) {
                case IDLE:
                    if (getModel().isRollover() && isEnabled()) background = HOVER;
                    break;
                case SELECTED:
                    background = Theme.MINT;
                    border = Theme.GREEN;
                    stroke = 3.5f;
                    break;
                case CORRECT:
                    background = RIGHT_BG;
                    border = RIGHT;
                    stroke = 3.5f;
                    break;
                case WRONG:
                    background = MISS_BG;
                    border = MISS;
                    stroke = 3.5f;
                    break;
                default:
                    border = new Color(0xE9EEEA);
            }
            RoundRectangle2D shape = new RoundRectangle2D.Double(stroke / 2, stroke / 2, w - stroke, h - stroke, 22, 22);
            g.setColor(background);
            g.fill(shape);
            g.setColor(border);
            g.setStroke(new BasicStroke(stroke));
            g.draw(shape);

            float alpha = state == State.DIMMED ? 0.35f : 1f;
            if (ox) {
                double size = Math.min(w, h) * 0.46, cx = w / 2, cy = h / 2;
                g.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if ("O".equals(getText())) {
                    g.setColor(fade(O_BLUE, alpha));
                    g.draw(new Ellipse2D.Double(cx - size / 2, cy - size / 2, size, size));
                } else {
                    g.setColor(fade(MISS, alpha));
                    g.draw(new Line2D.Double(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2));
                    g.draw(new Line2D.Double(cx + size / 2, cy - size / 2, cx - size / 2, cy + size / 2));
                }
            } else {
                double d = 42, bx = 18, by = (h - d) / 2;
                g.setColor(fade(state == State.SELECTED ? Theme.GREEN : Theme.MINT, alpha));
                g.fill(new Ellipse2D.Double(bx, by, d, d));
                g.setFont(Theme.font(Font.BOLD, 20));
                g.setColor(fade(state == State.SELECTED ? Color.WHITE : Theme.GREEN, alpha));
                String n = String.valueOf(number);
                g.drawString(n, (float) (bx + (d - g.getFontMetrics().stringWidth(n)) / 2), (float) (by + d / 2 + 7));
                g.setFont(Theme.font(Font.BOLD, 26));
                g.setColor(fade(Theme.TEXT, alpha));
                g.drawString(getText(), (float) (bx + d + 18), (float) (h / 2 + 9));
            }
            if (state == State.CORRECT) drawMark(g, w, true);
            if (state == State.WRONG) drawMark(g, w, false);
        } finally {
            g.dispose();
        }
    }

    /** 오른쪽 위에 정답은 체크, 오답은 X 배지를 그린다. */
    private void drawMark(Graphics2D g, double w, boolean right) {
        double d = 30, x = w - d - 14, y = 14;
        g.setColor(right ? RIGHT : MISS);
        g.fill(new Ellipse2D.Double(x, y, d, d));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (right) {
            Path2D check = new Path2D.Double();
            check.moveTo(x + 8, y + 16);
            check.lineTo(x + 13, y + 21);
            check.lineTo(x + 22, y + 10);
            g.draw(check);
        } else {
            g.draw(new Line2D.Double(x + 10, y + 10, x + 20, y + 20));
            g.draw(new Line2D.Double(x + 20, y + 10, x + 10, y + 20));
        }
    }

    private static Color fade(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * alpha));
    }
}
