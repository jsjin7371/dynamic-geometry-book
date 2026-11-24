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

    /**
     * 둥근 패널 바탕(그림자·채움·테두리)을 그린다.
     * <p>큰 둥근 사각형을 안티에일리어싱으로 매번 그리면 (화면 그리기 방식상) 가장자리 조각을 GPU로 계속 올리느라
     * 느리다 — 다른 애니메이션이 위를 지나가며 매 프레임 다시 그릴 때 특히. 그래서 작은 견본 패널을 한 번 그려 두고
     * 네 모서리는 그대로, 변은 1픽셀 줄을 늘려, 가운데는 채움색으로 칠해 같은 모양을 만든다(9조각).
     */
    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.aa(g0.create());
        if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (!paintNine(g)) paintBody(g, getWidth(), getHeight());
        g.dispose();
    }

    private java.awt.image.BufferedImage nine;
    private String nineKey;
    /** 견본 패널에서 모서리 한 칸의 크기(논리 픽셀). */
    private int nineC;

    private boolean paintNine(Graphics2D g) {
        java.awt.geom.AffineTransform at = g.getTransform();
        double sc = at.getScaleX();
        int s = (int) Math.round(sc);
        // 배율이 정수가 아니거나 돌아간 좌표계(전환 사진 등)면 그냥 그린다
        if (at.getShearX() != 0 || at.getShearY() != 0 || s < 1 || Math.abs(sc - s) > 1e-6 || Math.abs(at.getScaleY() - sc) > 1e-6
                || Ui.offscreen(g)) return false;
        int w = getWidth(), h = getHeight();
        int c = arc + shadow + (int) Math.ceil(borderWidth) + 8;
        if (doubleBorder) c += 8;
        if (w < c * 2 + 2 || h < c * 2 + 2) return false; // 작은 패널은 그냥 그린다
        String key = s + "|" + c + "|" + arc + "|" + shadow + "|" + borderWidth + "|" + rgb(fill) + "|" + rgb(border) + "|" + rgb(shadowColor)
                + "|" + doubleBorder + "|" + rgb(innerBorder);
        if (nine == null || !key.equals(nineKey)) {
            int m = c * 2 + 1;
            nine = new java.awt.image.BufferedImage(m * s, m * s, java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D ng = Theme.aa(nine.createGraphics());
            ng.scale(s, s);
            paintBody(ng, m, m);
            ng.dispose();
            nineKey = key;
            nineC = c;
        }
        // 화면 픽셀 좌표에서 1:1로 복사한다
        int ox = (int) Math.round(at.getTranslateX()), oy = (int) Math.round(at.getTranslateY());
        Graphics2D d = (Graphics2D) g.create();
        d.setTransform(new java.awt.geom.AffineTransform());
        d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        int cp = c * s, W = w * s, H = h * s, mid = cp; // 견본의 가운데 1픽셀 줄
        java.awt.image.BufferedImage im = nine;
        int M = im.getWidth();
        // 네 모서리
        d.drawImage(im, ox, oy, ox + cp, oy + cp, 0, 0, cp, cp, null);
        d.drawImage(im, ox + W - cp, oy, ox + W, oy + cp, M - cp, 0, M, cp, null);
        d.drawImage(im, ox, oy + H - cp, ox + cp, oy + H, 0, M - cp, cp, M, null);
        d.drawImage(im, ox + W - cp, oy + H - cp, ox + W, oy + H, M - cp, M - cp, M, M, null);
        // 네 변(1픽셀 줄을 늘린다)
        d.drawImage(im, ox + cp, oy, ox + W - cp, oy + cp, mid, 0, mid + 1, cp, null);
        d.drawImage(im, ox + cp, oy + H - cp, ox + W - cp, oy + H, mid, M - cp, mid + 1, M, null);
        d.drawImage(im, ox, oy + cp, ox + cp, oy + H - cp, 0, mid, cp, mid + 1, null);
        d.drawImage(im, ox + W - cp, oy + cp, ox + W, oy + H - cp, M - cp, mid, M, mid + 1, null);
        // 가운데
        d.drawImage(im, ox + cp, oy + cp, ox + W - cp, oy + H - cp, mid, mid, mid + 1, mid + 1, null);
        d.dispose();
        return true;
    }

    private static String rgb(Color c) { return c == null ? "-" : Integer.toHexString(c.getRGB()); }

    private void paintBody(Graphics2D g, int iw, int ih) {
        float w = iw - shadow, h = ih - shadow;
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
    }
}
