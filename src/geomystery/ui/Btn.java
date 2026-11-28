package geomystery.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** 시안 스타일로 직접 그리는 버튼. */
public class Btn extends JButton {
    public enum Style { PRIMARY, OUTLINE, CREAM, DARK_OUTLINE, KEY, KEY_ALT, GREEN, CHOICE, TAB, CIRCLE_DARK, LINK, GHOST }

    private Style style;
    private String iconName;
    private float fontSize = 15f;
    private boolean selected;
    private int arc = 14;
    private Color customFill;
    private int halign = SwingConstants.CENTER;

    public Btn(String text, Style style) {
        this(text, style, null);
    }

    public Btn(String text, Style style, String iconName) {
        super(text);
        this.style = style;
        this.iconName = iconName;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFocusable(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (style == Style.KEY || style == Style.KEY_ALT) fontSize = 21f;
        if (style == Style.LINK) fontSize = 14f;
        addActionListener(e -> Sfx.play("click"));
    }

    public Btn font(float size) { fontSize = size; revalidate(); repaint(); return this; }
    public Btn arc(int a) { arc = a; return this; }
    public Btn fill(Color c) { customFill = c; repaint(); return this; }
    /** 글자 정렬(SwingConstants.LEFT 또는 CENTER). 칸보다 긴 글자는 …로 줄인다. */
    public Btn align(int a) { halign = a; return this; }
    public void setStyle(Style s) { style = s; repaint(); }

    @Override public void setSelected(boolean b) { selected = b; repaint(); }
    @Override public boolean isSelected() { return selected; }

    private Font font() {
        if (style == Style.KEY || style == Style.KEY_ALT) return Theme.serif(fontSize);
        return Theme.bold(fontSize);
    }

    private int depth() {
        switch (style) {
            case PRIMARY: case KEY: case KEY_ALT: case CREAM: case GREEN: case CHOICE: return 4;
            default: return 0;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (style == Style.CIRCLE_DARK) return new Dimension(48, 48);
        FontMetrics fm = getFontMetrics(font());
        String t = getText() == null ? "" : getText();
        int w = fm.stringWidth(t) + (style == Style.LINK ? 4 : 40);
        if (iconName != null) w += 26;
        int h = style == Style.LINK ? fm.getHeight() + 4 : Math.max(44, fm.getHeight() + 22) + depth();
        return new Dimension(w, h);
    }

    /** 세로 BoxLayout 안에서 글자가 잘리지 않도록 최대 크기를 선호 크기 이상으로. */
    @Override
    public Dimension getMaximumSize() {
        Dimension m = super.getMaximumSize(), p = getPreferredSize();
        return new Dimension(Math.max(m.width, p.width), Math.max(m.height, p.height));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.aa(g0.create());
        int w = getWidth(), h = getHeight();
        ButtonModel m = getModel();
        boolean hover = m.isRollover() && isEnabled();
        boolean press = m.isArmed() && m.isPressed();
        int depth = depth();
        int dy = press ? Math.min(depth, 3) : 0;

        Color fill = null, border = null, text = Theme.INK, shadow = null;
        switch (style) {
            case PRIMARY:
                fill = hover ? new Color(0xB4442F) : Theme.RED; shadow = Theme.RED_DARK; text = Theme.CREAM; break;
            case GREEN:
                fill = hover ? new Color(0x377063) : Theme.GREEN; shadow = Theme.GREEN_DARK; text = Theme.CREAM; break;
            case OUTLINE:
                fill = hover ? Theme.c(0xFBF4E6) : Theme.PAPER; border = Theme.BORDER_DARK; break;
            case CREAM:
                fill = hover ? Theme.c(0xFFF8EA) : Theme.PAPER; shadow = Theme.c(0xB8A27A); border = Theme.c(0xD9C397); break;
            case DARK_OUTLINE:
                fill = hover ? new Color(255, 255, 255, 18) : null; border = Theme.alpha(Theme.GOLD, 0.75f); text = Theme.CREAM; break;
            case CIRCLE_DARK:
                fill = hover ? new Color(255, 255, 255, 22) : null; border = Theme.alpha(Theme.GOLD, 0.8f); text = Theme.CREAM; break;
            case KEY:
                fill = hover ? Theme.c(0xFFFDF7) : Theme.c(0xFFFAEE); border = Theme.c(0xD3B989); shadow = Theme.c(0xD9C59C); break;
            case KEY_ALT:
                fill = hover ? Theme.c(0xF0E1C0) : Theme.c(0xE8D6AF); border = Theme.c(0xCDB17E); shadow = Theme.c(0xC9B086); break;
            case CHOICE:
                fill = selected ? Theme.c(0xF8E3C8) : hover ? Theme.c(0xFFFDF7) : Theme.c(0xFFFAEE);
                border = selected ? Theme.RED : Theme.c(0xD3B989);
                shadow = selected ? Theme.c(0xD7A792) : Theme.c(0xD9C59C);
                break;
            case TAB:
                fill = selected ? Theme.DARK : hover ? Theme.c(0xE9D9B5) : null; text = selected ? Theme.CREAM : Theme.INK; break;
            case LINK:
                text = hover ? Theme.ORANGE : Theme.GOLD; break;
            case GHOST:
                fill = hover ? new Color(0, 0, 0, 14) : null; break;
        }
        if (customFill != null) fill = customFill;

        if (style == Style.CIRCLE_DARK) {
            double d = Math.min(w, h) - 2;
            Shape c = new Ellipse2D.Double((w - d) / 2, (h - d) / 2, d, d);
            if (fill != null) { g.setColor(fill); g.fill(c); }
            g.setColor(border);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(c);
            if (iconName != null) Icons.paint(g, iconName, w / 2.0 - 11, h / 2.0 - 11, 22, Theme.CREAM);
            g.dispose();
            return;
        }

        float bw = border != null ? (style == Style.CHOICE && selected ? 2.4f : 1.5f) : 0;
        if (depth > 0 && shadow != null) {
            g.setColor(shadow);
            g.fill(new RoundRectangle2D.Float(bw / 2, depth + bw / 2, w - bw - 1, h - depth - bw - 1, arc, arc));
        }
        Shape body = new RoundRectangle2D.Float(bw / 2, dy + bw / 2, w - bw - 1, h - depth - bw - 1, arc, arc);
        if (fill != null) { g.setColor(fill); g.fill(body); }
        if (border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke(bw));
            g.draw(body);
        }

        Font f = font();
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        String t = getText() == null ? "" : getText();
        int iconW = iconName != null ? (t.isEmpty() ? 20 : 26) : 0;
        int room = w - 28 - iconW;
        if (style != Style.LINK && fm.stringWidth(t) > room && t.length() > 2) {
            while (t.length() > 1 && fm.stringWidth(t + "…") > room) t = t.substring(0, t.length() - 1);
            t = t + "…";
        }
        int tw = Theme.width(fm, t) + iconW;
        double cx = w / 2.0, cy = dy + (h - depth) / 2.0;
        double x = halign == SwingConstants.LEFT ? 16 : cx - tw / 2.0;
        Color tc = isEnabled() ? text : Theme.alpha(text, 0.4f);
        if (iconName != null) {
            Icons.paint(g, iconName, x, cy - 10, 20, tc);
            x += iconW;
        }
        g.setColor(tc);
        float baseline = (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0);
        Theme.drawString(g, t, (float) x, baseline);
        if (style == Style.LINK) {
            g.drawLine((int) x, (int) baseline + 3, (int) (x + fm.stringWidth(t)), (int) baseline + 3);
        }
        if (!isEnabled() && fill != null) {
            g.setColor(new Color(239, 227, 199, 110));
            g.fill(body);
        }
        g.dispose();
    }
}
