package geomystery.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** 둥근 모서리·테두리·그림자를 가진 패널. */
public class RoundPanel extends JPanel {
    private Color fill, border;
    private int arc;
    private float borderWidth = 1.5f;
    private int shadow = 0;
    private Color shadowColor = Theme.c(0xD6C29A);
    private boolean doubleBorder = false;
    private Color innerBorder;

    public RoundPanel(Color fill, Color border, int arc) {
        this.fill = fill;
        this.border = border;
        this.arc = arc;
        setOpaque(false);
    }

    public RoundPanel(LayoutManager lm, Color fill, Color border, int arc) {
        this(fill, border, arc);
        setLayout(lm);
    }

    public RoundPanel shadow(int px, Color c) { shadow = px; shadowColor = c; return this; }
    public RoundPanel borderWidth(float w) { borderWidth = w; return this; }
    public RoundPanel doubleBorder(Color inner) { doubleBorder = true; innerBorder = inner; return this; }
    public RoundPanel pad(int t, int l, int b, int r) { setBorder(BorderFactory.createEmptyBorder(t, l, b + shadow, r + shadow)); return this; }

    /**
     * 안쪽 여백은 적어도 테두리 두께만큼 둔다. 그래야 안에 넣은 표·스크롤 영역(불투명)이
     * 테두리 위를 덮어 그리지 않는다(스크롤할 때 그 영역만 다시 그려져 테두리가 지워지던 문제).
     */
    @Override public Insets getInsets() {
        Insets in = super.getInsets();
        if (border == null) return in;
        int m = (int) Math.ceil(borderWidth);
        return new Insets(Math.max(in.top, m), Math.max(in.left, m), Math.max(in.bottom, m), Math.max(in.right, m));
    }

    @Override public Insets getInsets(Insets in) {
        Insets r = getInsets();
        if (in == null) return r;
        in.set(r.top, r.left, r.bottom, r.right);
        return in;
    }

    public void setFill(Color c) { fill = c; repaint(); }
    public Color fill() { return fill; }
    public void setBorderColor(Color c) { border = c; repaint(); }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.aa(g0.create());
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        float w = getWidth() - shadow, h = getHeight() - shadow;
        float bw = border == null ? 0 : borderWidth;
        if (shadow > 0) {
            g.setColor(shadowColor);
            g.fill(new RoundRectangle2D.Float(shadow, shadow, w - 1, h - 1, arc, arc));
        }
        Shape body = new RoundRectangle2D.Float(bw / 2, bw / 2, w - bw - 0.5f, h - bw - 0.5f, arc, arc);
        if (fill != null) {
            g.setColor(fill);
            g.fill(body);
        }
        if (border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke(bw));
            g.draw(body);
        }
        if (doubleBorder) {
            float in = 6 + bw;
            g.setColor(innerBorder);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new RoundRectangle2D.Float(in, in, w - in * 2, h - in * 2, Math.max(2, arc - 6), Math.max(2, arc - 6)));
        }
        g.dispose();
    }
}
