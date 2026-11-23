package geomystery.ui;

import java.awt.*;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 대화 장면의 살아 있는 연출: 장면별 배경 움직임, 눈 깜빡임, 대사 표정.
 * 배경 그림은 캐시를 그대로 쓰고, 여기서는 작은 도형 수십 개만 그려 매 프레임 비용을 작게 유지한다.
 * 좌표는 배경 굽기 좌표(1600×640)와 인물 굽기 좌표(360×480)를 화면으로 옮겨 쓴다.
 */
final class SceneFx {
    private SceneFx() {}

    private static final Color OL = new Color(0x3A2819);

    // ================================================================== 배경 움직임
    /** 배경 위·인물 뒤에 그리는 것(톱니, 촛불, 바닥 빛). x0,y0,s 는 배경 좌표 → 화면 변환. */
    static void behind(Graphics2D g, String scene, double x0, double y0, double s, double t) {
        if ("observatory".equals(scene)) {
            // 틈새 밤하늘의 반짝이는 별
            for (int i = 0; i < 18; i++) {
                double fx = 700 + hash(i, 11) * 200, fy = 10 + hash(i, 12) * 290;
                double tw = 0.5 + 0.5 * Math.sin(t * (1.5 + hash(i, 13) * 2.5) + i * 2.1);
                double r = (2.2 + 3.2 * tw) * s;
                g.setColor(new Color(1f, 0.97f, 0.82f, (float) (0.25 + 0.6 * tw)));
                double cx = x0 + fx * s, cy = y0 + fy * s;
                g.fill(new Ellipse2D.Double(cx - r / 2, cy - r / 2, r, r));
                if (tw > 0.8) {
                    g.setStroke(new BasicStroke((float) Math.max(1, s)));
                    g.draw(new Line2D.Double(cx - r * 1.6, cy, cx + r * 1.6, cy));
                    g.draw(new Line2D.Double(cx, cy - r * 1.6, cx, cy + r * 1.6));
                }
            }
            // 태양계 모형의 행성(굽기 좌표 1320,300의 기울어진 궤도)
            double[] rr = {50, 85, 120}, sp = {0.9, 0.55, 0.33}, size = {10, 14, 12};
            Color[] cs = {new Color(0xC9775A), new Color(0x6FA3C9), new Color(0xA9C27A)};
            for (int i = 0; i < 3; i++) {
                double a = t * sp[i] + i * 2.2;
                double px = 1320 + Math.cos(a) * rr[i], py = 300 + Math.sin(a) * rr[i] * 0.35;
                double d = size[i] * s;
                g.setColor(cs[i]);
                g.fill(new Ellipse2D.Double(x0 + px * s - d / 2, y0 + py * s - d / 2, d, d));
                g.setColor(OL);
                g.setStroke(new BasicStroke((float) Math.max(1, 1.6 * s)));
                g.draw(new Ellipse2D.Double(x0 + px * s - d / 2, y0 + py * s - d / 2, d, d));
            }
        } else if ("clock".equals(scene)) {
            gear(g, x0, y0, s, 360, 230, 92, 14, 22, t * 0.22, new Color(0x9A6B3C), new Color(0x7E5532), 130, 34);
            gear(g, x0, y0, s, 500, 380, 58, 11, 16, -t * 0.22 * 92 / 58.0, new Color(0x8E6236), new Color(0x5A3A22), 66, 24);
        } else if ("monastery".equals(scene)) {
            // 스테인드글라스가 바닥에 드리운 빛(천천히 일렁인다)
            Color[] glass = {new Color(0x3E6E9C), new Color(0xB5402E), new Color(0xE0B850), new Color(0x2F6E5E)};
            for (int wi = 0; wi < 2; wi++) {
                double wx = wi == 0 ? 230 : 1370;
                for (int i = 0; i < 4; i++) {
                    float a = (float) (0.10 + 0.05 * Math.sin(t * 0.8 + i + wi));
                    Color c = glass[i];
                    g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(a * 255)));
                    double fx = wx - 80 + i * 40 + (wi == 0 ? 60 : -60);
                    Path2D q = new Path2D.Double();
                    q.moveTo(x0 + fx * s, y0 + 530 * s);
                    q.lineTo(x0 + (fx + 40) * s, y0 + 530 * s);
                    q.lineTo(x0 + (fx + 40 + (wi == 0 ? 70 : -70)) * s, y0 + 630 * s);
                    q.lineTo(x0 + (fx + (wi == 0 ? 70 : -70)) * s, y0 + 630 * s);
                    q.closePath();
                    g.fill(q);
                }
            }
            // 촛불
            int k = 0;
            for (int x : new int[]{470, 1130}) {
                for (int dx : new int[]{-30, 0, 30}) {
                    double ph = k++ * 1.7;
                    double flick = 1 + 0.22 * Math.sin(t * 9 + ph) + 0.1 * Math.sin(t * 23 + ph * 2);
                    double cx = x0 + (x + dx) * s, cy = y0 + 324 * s;
                    g.setColor(new Color(255, 200, 90, (int) (55 + 30 * Math.sin(t * 6 + ph))));
                    double gr = 26 * s * flick;
                    g.fill(new Ellipse2D.Double(cx - gr, cy - gr - 2 * s, gr * 2, gr * 2));
                    double fh = 20 * s * flick, fw = 11 * s;
                    Path2D f = new Path2D.Double();
                    f.moveTo(cx, cy - fh * 0.9 + Math.sin(t * 7 + ph) * 1.5 * s);
                    f.quadTo(cx + fw * 0.7, cy - fh * 0.1, cx, cy + fh * 0.35);
                    f.quadTo(cx - fw * 0.7, cy - fh * 0.1, cx, cy - fh * 0.9 + Math.sin(t * 7 + ph) * 1.5 * s);
                    g.setColor(new Color(0xF2A23A));
                    g.fill(f);
                    g.setColor(new Color(0xFBE39A));
                    g.fill(new Ellipse2D.Double(cx - fw * 0.22, cy - fh * 0.15, fw * 0.44, fh * 0.42));
                }
            }
        }
    }

    /** 인물 앞에 떠다니는 것(꽃잎, 먼지). */
    static void front(Graphics2D g, String scene, double x0, double y0, double s, int w, int h, double t) {
        if ("observatory".equals(scene)) {
            // 7초마다 틈새를 가로지르는 별똥별(0.8초)
            double ph = t % 7.0;
            if (ph < 0.8) {
                int k = (int) (t / 7.0);
                double e = ph / 0.8;
                double sx = 890 - hash(k, 21) * 40, sy = 20 + hash(k, 22) * 80;
                double hx = sx - 180 * e, hy = sy + 120 * e;
                float a = (float) Math.sin(Math.PI * e);
                g.setPaint(new GradientPaint((float) (x0 + hx * s), (float) (y0 + hy * s), new Color(1f, 1f, 0.9f, a),
                        (float) (x0 + (hx + 60) * s), (float) (y0 + (hy - 40) * s), new Color(1f, 1f, 0.9f, 0f)));
                g.setStroke(new BasicStroke((float) Math.max(1.5, 2.5 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(x0 + hx * s, y0 + hy * s, x0 + (hx + 60) * s, y0 + (hy - 40) * s));
            }
            // 반짝이는 별가루
            for (int i = 0; i < 14; i++) {
                double fx = hash(i, 31) * 1600, fy = 120 + hash(i, 32) * 380 + Math.sin(t * 0.6 + i) * 10;
                float a = (float) (0.15 + 0.3 * Math.max(0, Math.sin(t * 1.7 + i * 1.9)));
                g.setColor(new Color(1f, 0.9f, 0.6f, a));
                double sz = (1.6 + hash(i, 33) * 2) * Math.max(1, s * 1.2);
                g.fill(new Ellipse2D.Double(x0 + fx * s - sz / 2, y0 + fy * s - sz / 2, sz, sz));
            }
        } else if ("garden".equals(scene) || scene == null) {
            Color[] cs = {new Color(242, 184, 198, 220), new Color(247, 238, 220, 210), new Color(168, 198, 134, 210)};
            for (int i = 0; i < 26; i++) {
                double sx = hash(i, 1), sy = hash(i, 2), sp = 0.6 + hash(i, 3) * 0.8;
                double x = ((sx + t * 0.035 * sp) % 1.15 - 0.075) * w + Math.sin(t * 1.3 + i) * 14;
                double y = ((sy + t * 0.05 * sp) % 1.15 - 0.075) * h;
                // 그리기 도구를 꽃잎마다 새로 만들지 않고 모양만 변환해 그린다
                double sz = 5 + hash(i, 5) * 5;
                AffineTransform at = AffineTransform.getTranslateInstance(x, y);
                at.rotate(t * (0.8 + hash(i, 4)) + i);
                at.scale(1, 0.55 + 0.45 * Math.abs(Math.sin(t * 2 + i)));
                g.setColor(cs[i % 3]);
                g.fill(at.createTransformedShape(new Ellipse2D.Double(-sz, -sz / 2, sz * 2, sz)));
            }
        } else {
            // 빛줄기 속 먼지
            double bx0, bx1, by0, by1;
            if ("clock".equals(scene)) { bx0 = 560; bx1 = 1040; by0 = 280; by1 = 520; }
            else { bx0 = 600; bx1 = 1000; by0 = 120; by1 = 520; }
            for (int i = 0; i < 34; i++) {
                double fx = bx0 + hash(i, 1) * (bx1 - bx0) + Math.sin(t * 0.7 + i) * 12;
                double fy = by1 - ((hash(i, 2) * (by1 - by0) + t * (8 + hash(i, 3) * 10)) % (by1 - by0));
                float a = (float) (0.25 + 0.25 * Math.sin(t * 2 + i * 1.3));
                g.setColor(new Color(1f, 0.95f, 0.8f, Math.max(0.05f, a)));
                double sz = (1.6 + hash(i, 4) * 2.2) * Math.max(1, s * 1.2);
                g.fill(new Ellipse2D.Double(x0 + fx * s - sz / 2, y0 + fy * s - sz / 2, sz, sz));
            }
        }
    }

    private static final Map<String, Path2D> GEARS = new HashMap<>();

    private static void gear(Graphics2D g, double x0, double y0, double s, double cx, double cy, double r, int teeth, double toothH,
                             double angle, Color fill, Color inner, double innerD, double hubD) {
        Path2D p = GEARS.computeIfAbsent(r + ":" + teeth, k -> {
            Path2D q = new Path2D.Double();
            int n = teeth * 4;
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                double rr = (i % 4 == 0 || i % 4 == 1) ? r + toothH : r;
                if (i == 0) q.moveTo(rr * Math.cos(a), rr * Math.sin(a)); else q.lineTo(rr * Math.cos(a), rr * Math.sin(a));
            }
            q.closePath();
            return q;
        });
        Graphics2D o = (Graphics2D) g.create();
        o.translate(x0 + cx * s, y0 + cy * s);
        o.scale(s, s);
        o.rotate(angle);
        o.setColor(fill);
        o.fill(p);
        o.setColor(inner);
        o.fill(new Ellipse2D.Double(-innerD / 2, -innerD / 2, innerD, innerD));
        o.setColor(fill);
        o.setStroke(new BasicStroke(innerD > 100 ? 10 : 6));
        o.draw(new Ellipse2D.Double(-innerD / 2, -innerD / 2, innerD, innerD));
        // 바퀴살(돌아가는 게 보이도록)
        o.setStroke(new BasicStroke(innerD > 100 ? 7 : 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2;
            o.draw(new Line2D.Double(0, 0, Math.cos(a) * innerD / 2, Math.sin(a) * innerD / 2));
        }
        o.setColor(new Color(0xE8C26A));
        o.fill(new Ellipse2D.Double(-hubD / 2, -hubD / 2, hubD, hubD));
        o.dispose();
    }

    private static double hash(int i, int salt) {
        long x = (i * 73856093L) ^ (salt * 19349663L);
        x = (x ^ (x >>> 13)) * 0x5bd1e995L;
        return ((x ^ (x >>> 15)) & 0xFFFF) / 65535.0;
    }

    // ================================================================== 눈 깜빡임
    /** 인물별 눈 위치(굽기 좌표 360×480): x, y, 반지름, 눈꺼풀 색. */
    private static final Map<String, double[][]> EYES = new HashMap<>();
    private static final Map<String, Color> LIDS = new HashMap<>();

    private static void eyes(String id, Color lid, double r, double... xy) {
        double[][] e = new double[xy.length / 2][];
        for (int i = 0; i < e.length; i++) e[i] = new double[]{xy[i * 2], xy[i * 2 + 1], r};
        EYES.put(id, e);
        LIDS.put(id, lid);
    }

    static {
        eyes("hazel", new Color(0xC47A3A), 9, 156, 208, 204, 208);
        eyes("gregor", new Color(0x2B2E33), 9, 150, 208, 210, 208);
        eyes("minerva", new Color(0x8B6B45), 21, 144, 177, 216, 177);
        eyes("toru", new Color(0x8FA460), 8, 158, 186, 202, 186);
        eyes("mole", new Color(0x5B4A42), 5.5, 150, 180, 210, 180);
        eyes("crow", new Color(0x2B2C36), 16, 152, 178, 200, 178);
        eyes("pigeon", new Color(0xA9AFBB), 12, 156, 178, 204, 178);
        eyes("cat", new Color(0x2B2B31), 14, 152, 184, 208, 184);
        eyes("mouse", new Color(0x9C9CA8), 8, 158, 196, 202, 196);
        eyes("fox", new Color(0xD9772E), 8, 156, 182, 204, 182);
        eyes("bat", new Color(0x6A5A78), 12, 158, 187, 202, 187);
        eyes("hedgehog", new Color(0xEBD3A6), 7.5, 160, 194, 200, 194);
        eyes("luna", new Color(0x3A3A44), 7.5, 156, 184, 204, 184);
        eyes("rabbit", new Color(0xF4EFE6), 8.5, 158, 190, 202, 190);
        eyes("otter", new Color(0x8B5E3C), 7, 156, 186, 204, 186);
        eyes("sheep", new Color(0xE8D2B0), 7, 158, 192, 202, 192);
    }

    /** 3~4.5초마다 0.14초 동안 눈을 감는다(인물마다 박자가 다르다). */
    static boolean blinking(String id, double t) {
        double period = 3.0 + (Math.abs(id.hashCode()) % 15) / 10.0;
        double ph = (Math.abs(id.hashCode()) % 97) / 97.0 * period;
        double m = (t + ph) % period;
        return m < 0.14 || (id.hashCode() % 3 == 0 && m > 0.32 && m < 0.44); // 가끔 두 번 깜빡
    }

    /** 인물 그림(x, y, dw, dh에 그려진) 위에 감은 눈을 그린다. dim 이면 말하지 않는 인물처럼 어둡게. */
    static void blink(Graphics2D g, String id, double x, double y, double dw, double dh, boolean mirror, boolean dim) {
        double[][] e = EYES.get(id);
        if (e == null) return;
        Color lid = LIDS.get(id);
        if (dim) lid = new Color((int) (lid.getRed() * 0.62), (int) (lid.getGreen() * 0.58), (int) (lid.getBlue() * 0.55));
        double k = dw / 360.0;
        for (double[] eye : e) {
            double ex = mirror ? 360 - eye[0] : eye[0];
            double cx = x + ex * k, cy = y + eye[1] * (dh / 480.0), r = eye[2] * k;
            g.setColor(lid);
            g.fill(new Ellipse2D.Double(cx - r * 1.15, cy - r * 1.2, r * 2.3, r * 2.4));
            g.setColor(dim ? OL.darker() : OL);
            g.setStroke(new BasicStroke((float) Math.max(1.2, r * 0.28), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Arc2D.Double(cx - r * 0.9, cy - r * 0.6, r * 1.8, r * 1.2, 200, 140, Arc2D.OPEN));
        }
    }

    // ================================================================== 표정
    /** 인물별 머리 중심(굽기 좌표)과 반지름. */
    static double[] head(String id) {
        switch (id) {
            case "hazel": return new double[]{180, 200, 70};
            case "gregor": return new double[]{180, 208, 74};
            case "minerva": return new double[]{180, 170, 98};
            case "toru": return new double[]{180, 186, 62};
            default: return new double[]{180, 188, 72};
        }
    }

    /** 놀람은 대사가 시작될 때 깡충 뛴다(화면 좌표 y 오프셋). */
    static double jump(String emotion, double since) {
        if ("surprised".equals(emotion) && since < 0.45) return -Math.sin(Math.PI * since / 0.45) * 16;
        if ("happy".equals(emotion) && since < 0.9) return -Math.abs(Math.sin(since * 10)) * 5 * (1 - since / 0.9);
        return 0;
    }

    /** 머리 둘레에 표정 기호를 그린다. hx,hy,r 은 화면 좌표의 머리 중심·반지름. */
    static void emotion(Graphics2D g, String emotion, double hx, double hy, double r, double since, boolean mirror) {
        if (emotion == null || emotion.isEmpty()) return;
        double side = mirror ? -1 : 1;
        float in = (float) Math.min(1, since / 0.2);
        Composite keep = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, in));
        switch (emotion) {
            case "happy": {
                for (int i = 0; i < 4; i++) {
                    double a = since * 2.2 + i * Math.PI / 2;
                    star(g, hx + Math.cos(a) * r * 1.15, hy - r * 0.55 + Math.sin(a) * r * 0.45, r * (0.09 + 0.03 * Math.sin(since * 7 + i)), new Color(0xF2C04A));
                }
                break;
            }
            case "surprised": {
                double x = hx + side * r * 0.95, y = hy - r * 0.95;
                g.setColor(new Color(0xC23B2A));
                g.setStroke(new BasicStroke((float) (r * 0.07), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = -1; i <= 1; i++) {
                    double a = Math.toRadians(-90 + i * 28 + (mirror ? -20 : 20));
                    g.draw(new Line2D.Double(x + Math.cos(a) * r * 0.18, y + Math.sin(a) * r * 0.18, x + Math.cos(a) * r * 0.42, y + Math.sin(a) * r * 0.42));
                }
                g.setFont(Theme.xbold((float) (r * 0.55)));
                Theme.drawCentered(g, "!", x, y + r * 0.25);
                break;
            }
            case "puzzled": {
                g.setFont(Theme.xbold((float) (r * 0.6)));
                g.setColor(new Color(0xA33A28));
                Theme.drawCentered(g, "?", hx + side * r * 0.95, hy - r * 0.85 + Math.sin(since * 4) * r * 0.05);
                break;
            }
            case "worried": {
                double x = hx - side * r * 0.85, y = hy - r * 0.35 + (since % 1.4) * r * 0.25;
                Path2D d = new Path2D.Double();
                double dr = r * 0.13;
                d.moveTo(x, y - dr * 1.5);
                d.quadTo(x + dr, y, x, y + dr * 0.8);
                d.quadTo(x - dr, y, x, y - dr * 1.5);
                g.setColor(new Color(0x8DC3E8));
                g.fill(d);
                g.setColor(new Color(0x3A6E9A));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(d);
                break;
            }
            case "angry": {
                double x = hx + side * r * 0.8, y = hy - r * 0.8;
                double a = r * 0.2 * (1 + 0.12 * Math.sin(since * 14));
                g.setColor(new Color(0xC23B2A));
                g.setStroke(new BasicStroke((float) (r * 0.07), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 4; i++) {
                    double ang = i * Math.PI / 2 + Math.PI / 4;
                    double px = x + Math.cos(ang) * a, py = y + Math.sin(ang) * a;
                    g.draw(new Arc2D.Double(px - a * 0.6, py - a * 0.6, a * 1.2, a * 1.2, Math.toDegrees(-ang) + 90, 180, Arc2D.OPEN));
                }
                break;
            }
            default: break;
        }
        g.setComposite(keep);
    }

    private static void star(Graphics2D g, double cx, double cy, double r, Color c) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i, rr = i % 2 == 0 ? r : r * 0.35;
            double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }
}
