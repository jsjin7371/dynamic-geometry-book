package geomystery.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** 24×24 그리드 기준 벡터 아이콘과 정리 카드용 미니 도형. */
public final class Icons {
    private Icons() {}

    public static Icon icon(String name, int size, Color c) {
        return new Icon() {
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                paint((Graphics2D) g, name, x, y, size, c);
            }
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
        };
    }

    private static Stroke st(float w) {
        return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    public static void paint(Graphics2D g0, String name, double x, double y, double size, Color c) {
        Graphics2D g = Theme.aa(g0.create());
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(c);
        g.setStroke(st(1.8f));
        switch (name) {
            case "map": {
                Path2D p = new Path2D.Double();
                p.moveTo(3, 6); p.lineTo(9, 4); p.lineTo(15, 6); p.lineTo(21, 4); p.lineTo(21, 18);
                p.lineTo(15, 20); p.lineTo(9, 18); p.lineTo(3, 20); p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(9, 4, 9, 18));
                g.draw(new Line2D.Double(15, 6, 15, 20));
                break;
            }
            case "box": {
                g.draw(new RoundRectangle2D.Double(3, 8, 18, 12, 3, 3));
                Path2D p = new Path2D.Double();
                p.moveTo(9, 8); p.lineTo(9, 5); p.lineTo(15, 5); p.lineTo(15, 8);
                g.draw(p);
                g.draw(new Line2D.Double(3, 13, 21, 13));
                break;
            }
            case "trophy": {
                Path2D p = new Path2D.Double();
                p.moveTo(7, 4); p.lineTo(17, 4); p.lineTo(17, 9);
                p.quadTo(17, 14, 12, 14); p.quadTo(7, 14, 7, 9); p.closePath();
                g.draw(p);
                Path2D h = new Path2D.Double();
                h.moveTo(7, 6); h.lineTo(4, 6); h.quadTo(4, 10.5, 7.5, 11);
                h.moveTo(17, 6); h.lineTo(20, 6); h.quadTo(20, 10.5, 16.5, 11);
                g.draw(h);
                g.draw(new Line2D.Double(12, 14, 12, 18));
                g.draw(new Line2D.Double(8, 20, 16, 20));
                g.draw(new Line2D.Double(9.5, 18, 14.5, 18));
                break;
            }
            case "book": {
                g.draw(new RoundRectangle2D.Double(4, 3, 16, 18, 3, 3));
                Path2D t = new Path2D.Double();
                t.moveTo(12, 8); t.lineTo(15.5, 14.5); t.lineTo(8.5, 14.5); t.closePath();
                g.draw(t);
                break;
            }
            case "person": {
                g.draw(new Ellipse2D.Double(8, 4, 8, 8));
                Path2D p = new Path2D.Double();
                p.moveTo(4, 21); p.quadTo(4, 14, 12, 14); p.quadTo(20, 14, 20, 21);
                g.draw(p);
                break;
            }
            case "flame": case "candle": {
                Path2D f = new Path2D.Double();
                f.moveTo(12, 2.5); f.curveTo(16, 7, 15.5, 10, 12, 11.5); f.curveTo(8.5, 10, 8.5, 6.5, 12, 2.5);
                g.setColor(new Color(0xE0802A));
                g.fill(f);
                g.setColor(new Color(0xF6C048));
                g.fill(new Ellipse2D.Double(10.6, 7, 2.8, 3.8));
                g.setColor(c);
                g.draw(new RoundRectangle2D.Double(8.5, 12.5, 7, 9, 1.5, 1.5));
                break;
            }
            case "token": {
                g.setColor(new Color(0xE3B84E));
                g.fill(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                g.setColor(new Color(0x9A6E22));
                g.setStroke(st(1.6f));
                g.draw(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                g.draw(new Ellipse2D.Double(5.5, 5.5, 13, 13));
                g.setFont(Theme.xbold(11f));
                g.setColor(new Color(0x5A3A12));
                Theme.drawCentered(g, "?", 12, 12.5);
                break;
            }
            case "lantern": {
                g.draw(new Line2D.Double(10, 3, 14, 3));
                g.setColor(new Color(0xF2C75A));
                g.fill(new RoundRectangle2D.Double(8, 6.5, 8, 12, 2, 2));
                g.setColor(c);
                g.draw(new RoundRectangle2D.Double(8, 6.5, 8, 12, 2, 2));
                g.draw(new Line2D.Double(7, 6, 17, 6));
                g.draw(new Line2D.Double(7, 19, 17, 19));
                g.draw(new Line2D.Double(9.5, 21, 14.5, 21));
                g.draw(new Line2D.Double(12, 3, 12, 6));
                break;
            }
            case "lantern_off": {
                g.setColor(Theme.alpha(c, 0.45f));
                g.draw(new RoundRectangle2D.Double(8, 6.5, 8, 12, 2, 2));
                g.draw(new Line2D.Double(7, 6, 17, 6));
                g.draw(new Line2D.Double(7, 19, 17, 19));
                g.draw(new Line2D.Double(12, 3, 12, 6));
                break;
            }
            case "check": {
                g.setStroke(st(2.8f));
                Path2D p = new Path2D.Double();
                p.moveTo(5.5, 12.5); p.lineTo(10, 17); p.lineTo(18.5, 7.5);
                g.draw(p);
                break;
            }
            case "lock": {
                g.draw(new RoundRectangle2D.Double(6, 11, 12, 9, 2.5, 2.5));
                g.draw(new Arc2D.Double(8, 5, 8, 11, 0, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(8, 10.5, 8, 11));
                g.draw(new Line2D.Double(16, 10.5, 16, 11));
                break;
            }
            case "star": {
                g.draw(star(12, 12.5, 9.5, 4.2));
                break;
            }
            case "star_fill": {
                g.fill(star(12, 12.5, 9.5, 4.2));
                break;
            }
            case "back": {
                g.setStroke(st(2.2f));
                Path2D p = new Path2D.Double();
                p.moveTo(14.5, 6); p.lineTo(8.5, 12); p.lineTo(14.5, 18);
                g.draw(p);
                break;
            }
            case "next": {
                g.setStroke(st(2.2f));
                Path2D p = new Path2D.Double();
                p.moveTo(9.5, 6); p.lineTo(15.5, 12); p.lineTo(9.5, 18);
                g.draw(p);
                break;
            }
            case "close": {
                g.setStroke(st(2.2f));
                g.draw(new Line2D.Double(7, 7, 17, 17));
                g.draw(new Line2D.Double(17, 7, 7, 17));
                break;
            }
            case "aux": {
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2.5f, 3f}, 0));
                g.draw(new Line2D.Double(5, 19, 19, 5));
                g.setStroke(st(2f));
                if ("aux".equals(name)) {
                    g.draw(new Line2D.Double(5, 5, 5, 10));
                    g.draw(new Line2D.Double(5, 5, 10, 5));
                }
                break;
            }
            case "search": {
                g.setStroke(st(2.4f));
                g.draw(new Ellipse2D.Double(4, 4, 12, 12));
                g.draw(new Line2D.Double(14.5, 14.5, 20, 20));
                break;
            }
            case "compass": {
                g.draw(new Ellipse2D.Double(2.5, 2.5, 19, 19));
                Path2D s = new Path2D.Double();
                s.moveTo(12, 4.5); s.lineTo(13.4, 10.6); s.lineTo(19.5, 12); s.lineTo(13.4, 13.4);
                s.lineTo(12, 19.5); s.lineTo(10.6, 13.4); s.lineTo(4.5, 12); s.lineTo(10.6, 10.6); s.closePath();
                g.draw(s);
                g.fill(new Ellipse2D.Double(11, 11, 2, 2));
                break;
            }
            case "triangle": {
                Path2D t = new Path2D.Double();
                t.moveTo(12, 5); t.lineTo(20, 19); t.lineTo(4, 19); t.closePath();
                g.setStroke(st(2.2f));
                g.draw(t);
                break;
            }
            case "clock": {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                g.draw(new Line2D.Double(12, 12, 12, 7));
                g.draw(new Line2D.Double(12, 12, 15.5, 14));
                break;
            }
            case "bulb": {
                g.draw(new Arc2D.Double(6, 3, 12, 12, -30, 240, Arc2D.OPEN));
                g.draw(new Line2D.Double(9.5, 14, 9.5, 17));
                g.draw(new Line2D.Double(14.5, 14, 14.5, 17));
                g.draw(new Line2D.Double(9.5, 17, 14.5, 17));
                g.draw(new Line2D.Double(10, 20, 14, 20));
                break;
            }
            case "refresh": {
                g.draw(new Arc2D.Double(5, 5, 14, 14, 30, 280, Arc2D.OPEN));
                Path2D p = new Path2D.Double();
                p.moveTo(18.5, 3.5); p.lineTo(18.5, 8.5); p.lineTo(13.5, 8.5);
                g.draw(p);
                break;
            }
            case "dice": {
                g.draw(new RoundRectangle2D.Double(4, 4, 16, 16, 4, 4));
                for (double[] d : new double[][]{{8.5, 8.5}, {15.5, 15.5}, {12, 12}, {15.5, 8.5}, {8.5, 15.5}}) {
                    g.fill(new Ellipse2D.Double(d[0] - 1.3, d[1] - 1.3, 2.6, 2.6));
                }
                break;
            }
            case "skip": {
                g.setStroke(st(2f));
                Path2D p = new Path2D.Double();
                p.moveTo(5, 6); p.lineTo(11, 12); p.lineTo(5, 18);
                p.moveTo(12, 6); p.lineTo(18, 12); p.lineTo(12, 18);
                g.draw(p);
                break;
            }
            case "home": {
                Path2D p = new Path2D.Double();
                p.moveTo(3.5, 11); p.lineTo(12, 4); p.lineTo(20.5, 11);
                g.draw(p);
                Path2D b = new Path2D.Double();
                b.moveTo(6, 9.5); b.lineTo(6, 20); b.lineTo(18, 20); b.lineTo(18, 9.5);
                g.draw(b);
                g.draw(new Rectangle2D.Double(10, 14, 4, 6));
                break;
            }
            case "heart": case "heart_fill": {
                Path2D h = heart(12, 12.5, 8.5);
                if ("heart_fill".equals(name)) g.fill(h); else g.draw(h);
                break;
            }
            case "keyboard": {
                g.draw(new RoundRectangle2D.Double(2.5, 6, 19, 12, 3, 3));
                for (int i = 0; i < 4; i++) g.fill(new Rectangle2D.Double(5.2 + i * 4, 9, 1.8, 1.8));
                for (int i = 0; i < 3; i++) g.fill(new Rectangle2D.Double(7.2 + i * 4, 12, 1.8, 1.8));
                g.draw(new Line2D.Double(8, 15.5, 16, 15.5));
                break;
            }
            default:
                g.draw(new Ellipse2D.Double(5, 5, 14, 14));
        }
        g.dispose();
    }

    /** 하트 모양(가운데 cx, cy, 반폭 r). */
    public static Path2D heart(double cx, double cy, double r) {
        Path2D h = new Path2D.Double();
        h.moveTo(cx, cy + r * 0.95);
        h.curveTo(cx - r * 1.25, cy + r * 0.05, cx - r * 0.95, cy - r * 1.05, cx, cy - r * 0.35);
        h.curveTo(cx + r * 0.95, cy - r * 1.05, cx + r * 1.25, cy + r * 0.05, cx, cy + r * 0.95);
        h.closePath();
        return h;
    }

    public static Shape star(double cx, double cy, double r1, double r2) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? r1 : r2;
            double a = Math.toRadians(-90 + i * 36);
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    /** 정리 카드에 들어가는 미니 도형. r 영역 안에 그린다. */
    public static void theorem(Graphics2D g0, String type, Rectangle2D r, Color line, Color accent) {
        Graphics2D g = Theme.aa(g0.create());
        double s = Math.min(r.getWidth(), r.getHeight()) / 100.0;
        g.translate(r.getCenterX() - 50 * s, r.getCenterY() - 50 * s);
        g.scale(s, s);
        g.setStroke(st(3f));
        g.setColor(line);
        if (type == null) type = "";
        switch (type) {
            case "points": {
                double[][] p = {{22, 30}, {78, 24}, {84, 76}, {18, 72}};
                g.setColor(accent);
                g.setStroke(st(2f));
                for (int i = 0; i < 4; i++) for (int j = i + 1; j < 4; j++) g.draw(new Line2D.Double(p[i][0], p[i][1], p[j][0], p[j][1]));
                g.setColor(line);
                for (double[] q : p) g.fill(new Ellipse2D.Double(q[0] - 5, q[1] - 5, 10, 10));
                break;
            }
            case "midpoint": {
                g.draw(new Line2D.Double(10, 55, 90, 55));
                g.setColor(accent);
                g.draw(new Line2D.Double(30, 47, 30, 63));
                g.draw(new Line2D.Double(70, 47, 70, 63));
                g.setColor(line);
                for (double x : new double[]{10, 50, 90}) g.fill(new Ellipse2D.Double(x - 5, 50, 10, 10));
                break;
            }
            case "perp": {
                g.draw(new Line2D.Double(10, 78, 90, 78));
                g.setColor(accent);
                g.draw(new Line2D.Double(50, 18, 50, 78));
                g.setStroke(st(2f));
                g.draw(new Line2D.Double(50, 66, 62, 66));
                g.draw(new Line2D.Double(62, 66, 62, 78));
                g.setColor(line);
                g.fill(new Ellipse2D.Double(45, 13, 10, 10));
                break;
            }
            case "angle": {
                g.draw(new Line2D.Double(18, 78, 88, 78));
                g.draw(new Line2D.Double(18, 78, 70, 22));
                g.setColor(accent);
                g.draw(new Arc2D.Double(-8, 52, 52, 52, 0, 47, Arc2D.OPEN));
                break;
            }
            case "vertical": {
                g.draw(new Line2D.Double(12, 25, 88, 75));
                g.draw(new Line2D.Double(12, 75, 88, 25));
                g.setColor(accent);
                g.draw(new Arc2D.Double(34, 34, 32, 32, 33, 114, Arc2D.OPEN));
                g.draw(new Arc2D.Double(34, 34, 32, 32, 213, 114, Arc2D.OPEN));
                break;
            }
            case "parallel": {
                g.draw(new Line2D.Double(8, 32, 92, 32));
                g.draw(new Line2D.Double(8, 70, 92, 70));
                g.draw(new Line2D.Double(30, 10, 70, 92));
                g.setColor(accent);
                g.draw(new Arc2D.Double(31, 18, 28, 28, -64, 64, Arc2D.OPEN));
                g.draw(new Arc2D.Double(46, 56, 28, 28, 116, 64, Arc2D.OPEN));
                break;
            }
            case "inscribed": case "semicircle": case "cyclic": {
                g.draw(new Ellipse2D.Double(10, 10, 80, 80));
                g.setColor(accent);
                if ("inscribed".equals(type)) {
                    Path2D p = new Path2D.Double();
                    p.moveTo(14, 62); p.lineTo(56, 11); p.lineTo(78, 78);
                    g.draw(p);
                    g.setColor(line);
                    g.draw(new Line2D.Double(14, 62, 50, 50));
                    g.draw(new Line2D.Double(50, 50, 78, 78));
                } else if ("semicircle".equals(type)) {
                    g.draw(new Line2D.Double(10, 50, 90, 50));
                    Path2D p = new Path2D.Double();
                    p.moveTo(10, 50); p.lineTo(70, 18); p.lineTo(90, 50);
                    g.draw(p);
                } else {
                    Path2D p = new Path2D.Double();
                    p.moveTo(18, 30); p.lineTo(70, 13); p.lineTo(86, 66); p.lineTo(32, 85); p.closePath();
                    g.draw(p);
                }
                break;
            }
            case "triangle": case "exterior": case "isosceles": {
                Path2D t = new Path2D.Double();
                if ("isosceles".equals(type)) { t.moveTo(50, 12); t.lineTo(80, 82); t.lineTo(20, 82); }
                else { t.moveTo(40, 18); t.lineTo(74, 80); t.lineTo(12, 80); }
                t.closePath();
                g.draw(t);
                g.setColor(accent);
                if ("exterior".equals(type)) {
                    g.draw(new Line2D.Double(74, 80, 96, 80));
                    g.draw(new Arc2D.Double(62, 68, 24, 24, 0, 119, Arc2D.OPEN));
                } else if ("isosceles".equals(type)) {
                    g.draw(new Line2D.Double(30, 45, 38, 49));
                    g.draw(new Line2D.Double(70, 45, 62, 49));
                } else {
                    g.draw(new Line2D.Double(10, 18, 80, 18));
                }
                break;
            }
            case "pythagoras": {
                Path2D t = new Path2D.Double();
                t.moveTo(30, 70); t.lineTo(70, 70); t.lineTo(30, 40); t.closePath();
                g.draw(t);
                g.setColor(accent);
                g.setStroke(st(2f));
                g.draw(new Rectangle2D.Double(30, 70, 40, 22));
                g.draw(new Rectangle2D.Double(8, 40, 22, 30));
                Path2D sq = new Path2D.Double();
                sq.moveTo(30, 40); sq.lineTo(70, 70); sq.lineTo(88, 46); sq.lineTo(48, 16); sq.closePath();
                g.draw(sq);
                break;
            }
            case "chord": {
                g.draw(new Ellipse2D.Double(10, 10, 80, 80));
                g.draw(new Line2D.Double(16, 66, 84, 66));
                g.setColor(accent);
                g.draw(new Line2D.Double(50, 50, 50, 66));
                g.setStroke(st(2f));
                g.draw(new Line2D.Double(50, 58, 58, 58));
                g.draw(new Line2D.Double(58, 58, 58, 66));
                g.draw(new Line2D.Double(32, 61, 32, 71));
                g.draw(new Line2D.Double(68, 61, 68, 71));
                g.setColor(line);
                g.fill(new Ellipse2D.Double(46, 46, 8, 8));
                break;
            }
            case "tangent": {
                g.draw(new Ellipse2D.Double(8, 22, 60, 60));
                g.setColor(accent);
                g.draw(new Line2D.Double(92, 52, 47, 23));
                g.draw(new Line2D.Double(92, 52, 47, 81));
                g.setColor(line);
                g.setStroke(st(2f));
                g.draw(new Line2D.Double(38, 52, 47, 23));
                g.draw(new Line2D.Double(38, 52, 47, 81));
                g.fill(new Ellipse2D.Double(88, 48, 8, 8));
                break;
            }
            case "tanchord": {
                g.draw(new Ellipse2D.Double(14, 6, 72, 72));
                g.draw(new Line2D.Double(6, 78, 94, 78));
                g.draw(new Line2D.Double(50, 78, 80, 26));
                g.setColor(accent);
                g.draw(new Arc2D.Double(34, 62, 32, 32, 0, 60, Arc2D.OPEN));
                g.setStroke(st(2f));
                g.draw(new Line2D.Double(22, 22, 50, 78));
                g.draw(new Line2D.Double(22, 22, 80, 26));
                break;
            }
            case "sector": {
                g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
                g.fill(new Arc2D.Double(12, 12, 76, 76, 20, 80, Arc2D.PIE));
                g.setColor(line);
                g.draw(new Ellipse2D.Double(12, 12, 76, 76));
                g.setColor(accent);
                g.draw(new Arc2D.Double(12, 12, 76, 76, 20, 80, Arc2D.PIE));
                break;
            }
            default:
                g.draw(new Ellipse2D.Double(20, 20, 60, 60));
        }
        g.dispose();
    }
}
