package geomystery.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** 작은 커스텀 컴포넌트 모음: 상태 알약, 진행 막대, 줄바꿈 텍스트. */
public final class Widgets {
    private Widgets() {}

    /** 상단의 연속 학습·토큰·등불·XP 표시. */
    public static class Pill extends JComponent {
        public enum Kind { LIGHT, DARK, RED }
        private String icon, text;
        private final Kind kind;
        private long flickerStart = 0;
        private javax.swing.Timer flickerTimer;

        private long pulseStart;

        /** 토큰이 들어올 때 살짝 부풀었다 돌아온다. */
        public void pulse() {
            pulseStart = System.currentTimeMillis();
            javax.swing.Timer t = new javax.swing.Timer(16, null);
            t.addActionListener(e -> {
                repaint();
                if (System.currentTimeMillis() - pulseStart > 260) t.stop();
            });
            t.start();
        }

        /** 등불이 꺼지는 연출: 아이콘이 깜빡이다 꺼지고 연기가 피어오른다. */
        public void flicker() {
            flickerStart = System.currentTimeMillis();
            if (flickerTimer != null) flickerTimer.stop();
            flickerTimer = new javax.swing.Timer(16, e -> {
                repaint();
                if (System.currentTimeMillis() - flickerStart > 1200) ((javax.swing.Timer) e.getSource()).stop();
            });
            flickerTimer.start();
        }

        public Pill(String icon, String text, Kind kind) {
            this.icon = icon;
            this.text = text;
            this.kind = kind;
        }

        public void set(String icon, String text) {
            this.icon = icon;
            this.text = text;
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(Theme.bold(16));
            int w = fm.stringWidth(text) + 40 + (icon != null ? 28 : 0);
            return new Dimension(w, 46);
        }

        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            long pe = System.currentTimeMillis() - pulseStart;
            if (pe < 260) {
                double sc = 1 + 0.08 * Math.sin(Math.PI * pe / 260.0);
                g.translate(w / 2.0, h / 2.0);
                g.scale(sc, sc);
                g.translate(-w / 2.0, -h / 2.0);
            }
            Shape s = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, h - 2, h - 2);
            Color tc;
            switch (kind) {
                case RED:
                    g.setColor(Theme.RED); g.fill(s); tc = Theme.CREAM; break;
                case DARK:
                    g.setColor(Theme.alpha(Theme.GOLD, 0.6f)); g.setStroke(new BasicStroke(1.4f)); g.draw(s); tc = Theme.CREAM; break;
                default:
                    g.setColor(Theme.PAPER); g.fill(s);
                    g.setColor(Theme.BORDER); g.setStroke(new BasicStroke(1.6f)); g.draw(s); tc = Theme.INK;
            }
            g.setFont(Theme.bold(16));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(text) + (icon != null ? 28 : 0);
            double x = (w - tw) / 2.0;
            if (icon != null) {
                long el = System.currentTimeMillis() - flickerStart;
                Color ic = kind == Kind.LIGHT ? Theme.INK : Theme.CREAM;
                if (el < 1200) {
                    // 깜빡깜빡 → 꺼짐(어두워짐) → 연기
                    float on = el < 600 ? (Math.sin(el / 35.0) > 0 ? 1f : 0.25f) : Math.max(0.35f, 1f - (el - 600) / 600f);
                    Graphics2D gi = (Graphics2D) g.create();
                    gi.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, on));
                    Icons.paint(gi, el < 600 ? icon : "lantern_off", x, h / 2.0 - 11, 22, ic);
                    if (el < 600 && on > 0.5f) {
                        gi.setColor(new Color(255, 190, 80, 90));
                        gi.fill(new java.awt.geom.Ellipse2D.Double(x - 4, h / 2.0 - 15, 30, 30));
                    }
                    gi.dispose();
                    if (el > 450) {
                        double t = (el - 450) / 750.0;
                        for (int k = 0; k < 3; k++) {
                            double tt = Math.max(0, t - k * 0.15);
                            if (tt <= 0) continue;
                            double r = 4 + tt * 9;
                            g.setColor(new Color(200, 190, 175, (int) Math.max(0, 140 * (1 - tt))));
                            g.fill(new java.awt.geom.Ellipse2D.Double(x + 11 - r + Math.sin(tt * 6 + k) * 4, h / 2.0 - 12 - tt * 26 - r, r * 2, r * 2));
                        }
                    }
                } else {
                    Icons.paint(g, icon, x, h / 2.0 - 11, 22, ic);
                }
                x += 28;
            }
            g.setColor(tc);
            g.drawString(text, (float) x, (float) (h / 2.0 + (fm.getAscent() - fm.getDescent()) / 2.0));
            g.dispose();
        }
    }

    /** 둥근 진행 막대. */
    public static class Bar extends JComponent {
        private float value;
        private Color track, fill;
        private final int thickness;

        public Bar(Color track, Color fill, int thickness) {
            this.track = track;
            this.fill = fill;
            this.thickness = thickness;
            setOpaque(false);
        }

        public void setValue(float v) { value = Math.max(0, Math.min(1, v)); repaint(); }

        private long shineStart;
        private javax.swing.Timer shineTimer;

        /** 완료 축하: 빛줄기가 막대를 훑고 끝에서 반짝인다(막대 자신만 다시 그림). */
        public void celebrate(long delayMs) {
            shineStart = System.currentTimeMillis() + delayMs;
            if (shineTimer != null) shineTimer.stop();
            shineTimer = new javax.swing.Timer(16, e -> {
                repaint();
                if (System.currentTimeMillis() - shineStart > 1500) ((javax.swing.Timer) e.getSource()).stop();
            });
            shineTimer.start();
        }
        public void setFill(Color c) { fill = c; repaint(); }

        @Override public Dimension getPreferredSize() { return new Dimension(100, thickness); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, thickness); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = thickness, y = (getHeight() - h) / 2;
            g.setColor(track);
            g.fill(new RoundRectangle2D.Float(0, y, w, h, h, h));
            if (value > 0) {
                g.setColor(fill);
                RoundRectangle2D.Float body = new RoundRectangle2D.Float(0, y, Math.max(h, w * value), h, h, h);
                g.fill(body);
                long el = System.currentTimeMillis() - shineStart;
                if (shineStart > 0 && el >= 0 && el < 1500) {
                    double k = el / 1100.0;
                    double cx = -60 + (w + 120) * Math.min(1, k);
                    Graphics2D sg = (Graphics2D) g.create();
                    sg.clip(body);
                    sg.setPaint(new LinearGradientPaint((float) cx - 50, 0, (float) cx + 50, 0, new float[]{0f, 0.5f, 1f},
                            new Color[]{new Color(255, 255, 255, 0), new Color(255, 250, 220, 210), new Color(255, 255, 255, 0)}));
                    sg.fill(body);
                    sg.dispose();
                    if (k > 0.85) {
                        double q = Math.min(1, (k - 0.85) / 0.5);
                        int a = (int) (230 * (1 - q));
                        g.setColor(new Color(242, 192, 74, Math.max(0, a)));
                        double ex = w * value - h / 2.0, ey = y + h / 2.0;
                        for (int i = 0; i < 6; i++) {
                            double ang = i * Math.PI / 3, rr = 3 + q * 9;
                            g.fill(new java.awt.geom.Ellipse2D.Double(ex + Math.cos(ang) * rr - 2, ey + Math.sin(ang) * rr - 2, 4, 4));
                        }
                    }
                }
            }
            g.dispose();
        }
    }

    /** 단어 단위로 자동 줄바꿈되는 텍스트 블록(줄 간격 조절 가능). */
    public static class TextBlock extends JComponent {
        private String text;
        private Color color;
        private float spacing = 1.55f;
        private int prefWidth = 320;
        private boolean center;
        private int lastWidth = -1;

        public TextBlock(String text, Font font, Color color) {
            this.text = text == null ? "" : text;
            this.color = color;
            setFont(font);
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
        }

        public TextBlock spacing(float s) { spacing = s; return this; }
        public TextBlock prefWidth(int w) { prefWidth = w; return this; }
        public TextBlock center(boolean c) { center = c; return this; }

        public void setText(String t) {
            text = t == null ? "" : t;
            revalidate();
            repaint();
        }

        /**
         * 글자만 바꾸고, 줄 수(높이)가 그대로면 레이아웃을 다시 계산하지 않는다.
         * 드래그처럼 자주 바뀌는 글에 쓴다(창 전체 재배치를 피한다).
         */
        public void setTextQuick(String t) {
            String nt = t == null ? "" : t;
            if (nt.equals(text)) return;
            int before = getPreferredSize().height;
            text = nt;
            if (getPreferredSize().height != before) revalidate();
            repaint();
        }

        public String getText() { return text; }

        public void setColor(Color c) { color = c; repaint(); }

        public static List<String> wrap(String text, FontMetrics fm, int width) {
            List<String> lines = new ArrayList<>();
            if (width <= 10) width = 10;
            for (String para : text.split("\n", -1)) {
                if (para.isEmpty()) { lines.add(""); continue; }
                StringBuilder line = new StringBuilder();
                for (String word : para.split(" ")) {
                    String trial = line.length() == 0 ? word : line + " " + word;
                    if (Theme.width(fm, trial) <= width) {
                        line.setLength(0);
                        line.append(trial);
                        continue;
                    }
                    if (line.length() > 0) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    // 단어 자체가 너무 길면 글자 단위로 자른다
                    String w = word;
                    while (fm.stringWidth(w) > width && w.length() > 1) {
                        int cut = w.length() - 1;
                        while (cut > 1 && fm.stringWidth(w.substring(0, cut)) > width) cut--;
                        lines.add(w.substring(0, cut));
                        w = w.substring(cut);
                    }
                    line.append(w);
                }
                lines.add(line.toString());
            }
            return lines;
        }

        private int lineHeight(FontMetrics fm) { return Math.round(fm.getHeight() * spacing); }

        @Override
        public Dimension getPreferredSize() {
            Insets in = getInsets();
            int w = getWidth() > 0 ? getWidth() : prefWidth;
            FontMetrics fm = getFontMetrics(getFont());
            float n = 0;
            for (String s : wrap(text, fm, w - in.left - in.right)) n += s.isEmpty() ? 0.45f : 1f;
            int h = Math.round(n * lineHeight(fm)) - (lineHeight(fm) - fm.getHeight()) + in.top + in.bottom;
            return new Dimension(prefWidth, Math.max(h, fm.getHeight()));
        }

        @Override public Dimension getMinimumSize() { return new Dimension(40, getPreferredSize().height); }
        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }

        @Override
        public void setBounds(int x, int y, int w, int h) {
            super.setBounds(x, y, w, h);
            if (w != lastWidth) {
                lastWidth = w;
                SwingUtilities.invokeLater(this::revalidate);
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            Insets in = getInsets();
            g.setFont(getFont());
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            int w = getWidth() - in.left - in.right;
            int y = in.top + fm.getAscent();
            for (String line : wrap(text, fm, w)) {
                if (line.isEmpty()) { y += Math.round(lineHeight(fm) * 0.45f); continue; }
                int x = in.left + (center ? (w - Theme.width(fm, line)) / 2 : 0);
                Theme.drawString(g, line, x, y);
                y += lineHeight(fm);
            }
            g.dispose();
        }
    }

    /**
     * 표정이 있는 얼굴. 구운 얼굴 이미지 위에 표정 연출(움직임·땀·반짝임·물음표 등)을 덧그린다.
     * 그래서 인물마다 표정 그림을 따로 굽지 않아도 된다.
     */
    public static class FaceView extends JComponent {
        public enum Mood { NONE, HAPPY, PUZZLED, SHOCK, CONFESS }

        private String image;
        private final int size;
        private boolean round;
        private Mood mood = Mood.NONE;
        private long moodStart;
        private long holdMs;
        private javax.swing.Timer anim;
        private Color fallback;
        private String initial;

        public FaceView(String image, int size) {
            this.image = image;
            this.size = size;
            setOpaque(false);
        }

        /** 원형 배경을 깐다(증언 카드 등). */
        public FaceView round(boolean r) { round = r; return this; }

        /** 얼굴 이미지가 없을 때 쓸 첫 글자와 색. */
        public FaceView fallback(String initial, Color c) { this.initial = initial; fallback = c; return this; }

        public void setImage(String img) { image = img; repaint(); }

        /** 표정을 짓는다. holdMs 가 0이면 계속 유지. */
        public void mood(Mood m, long holdMs) {
            mood = m;
            moodStart = System.currentTimeMillis();
            this.holdMs = holdMs;
            if (anim == null) {
                anim = new javax.swing.Timer(30, e -> {
                    long el = System.currentTimeMillis() - moodStart;
                    if (this.holdMs > 0 && el > this.holdMs) { mood = Mood.NONE; ((javax.swing.Timer) e.getSource()).stop(); }
                    if (!isShowing() && el > 3000) ((javax.swing.Timer) e.getSource()).stop();
                    repaint();
                });
            }
            anim.restart();
        }

        public Mood mood() { return mood; }

        @Override public Dimension getPreferredSize() { return new Dimension(size, size); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            double t = (System.currentTimeMillis() - moodStart) / 1000.0;
            int s = size;
            double ox = 0, oy = Math.max(0, getHeight() - s), rot = 0, sc = 1;
            switch (mood) {
                case HAPPY: oy -= Math.abs(Math.sin(t * 9)) * s * 0.07 * Math.max(0, 1 - t / 1.6); break;
                case PUZZLED: rot = Math.toRadians(-9) * Math.min(1, t * 5); break;
                case SHOCK: ox = Math.sin(t * 60) * s * 0.035 * Math.max(0.25, 1 - t / 0.8); sc = 1 + 0.06 * Math.max(0, 1 - t * 3); break;
                case CONFESS: oy += s * 0.04 * Math.min(1, t * 2); rot = Math.toRadians(6) * Math.min(1, t * 2); break;
                default: break;
            }
            if (round) {
                g.setColor(mood == Mood.SHOCK || mood == Mood.CONFESS ? new Color(0xD7DCE6) : new Color(0xE9D8B2));
                g.fill(new java.awt.geom.Ellipse2D.Double(0, oy, s, s));
            }
            Graphics2D f = (Graphics2D) g.create();
            f.translate(ox + s / 2.0, oy + s / 2.0);
            f.rotate(rot);
            f.scale(sc, sc);
            f.translate(-s / 2.0, -s / 2.0);
            if (round) f.clip(new java.awt.geom.Ellipse2D.Double(-s * 0.1, -s * 0.2, s * 1.2, s * 1.2));
            java.awt.image.BufferedImage im = image == null ? null : Ui.img(image);
            if (im != null) {
                Ui.drawScaled(f, im, round ? 2 : 0, round ? 2 : 0, round ? s - 4 : s, round ? s - 4 : s);
            } else if (initial != null) {
                f.setColor(fallback != null ? fallback : Theme.GREEN);
                f.fill(new java.awt.geom.Ellipse2D.Double(0, 0, s, s));
                f.setFont(Theme.xbold(s * 0.42f));
                f.setColor(Theme.CREAM);
                Theme.drawCentered(f, initial, s / 2.0, s / 2.0);
            }
            if (mood == Mood.SHOCK || mood == Mood.CONFESS) {
                // 핼쑥한 이마 그늘
                f.setPaint(new GradientPaint(0, s * 0.12f, new Color(70, 90, 160, 110), 0, s * 0.42f, new Color(70, 90, 160, 0)));
                f.fill(new java.awt.geom.Ellipse2D.Double(s * 0.22, s * 0.1, s * 0.56, s * 0.36));
            }
            f.dispose();
            // 덧그림
            switch (mood) {
                case HAPPY: {
                    for (int k = 0; k < 4; k++) {
                        double a = t * 2.4 + k * Math.PI / 2;
                        double px = s / 2.0 + Math.cos(a) * s * 0.5, py = oy + s * 0.35 + Math.sin(a) * s * 0.36;
                        star(g, px, py, s * (0.06 + 0.02 * Math.sin(t * 8 + k)), new Color(0xF2C04A));
                    }
                    break;
                }
                case PUZZLED: {
                    g.setFont(Theme.xbold(s * 0.34f));
                    g.setColor(new Color(0xA33A28));
                    double bob = Math.sin(t * 4) * s * 0.03;
                    g.drawString("?", (float) (s * 0.78), (float) (oy + s * 0.3 + bob));
                    break;
                }
                case SHOCK: {
                    g.setFont(Theme.xbold(s * 0.3f));
                    g.setColor(new Color(0xC23B2A));
                    g.drawString("!!", (float) (s * 0.74), (float) (oy + s * 0.28));
                    drop(g, s * 0.16 + ox, oy + s * 0.22 + (t % 1.2) * s * 0.15, s * 0.11);
                    drop(g, s * 0.82 + ox, oy + s * 0.46 + ((t + 0.5) % 1.2) * s * 0.12, s * 0.09);
                    break;
                }
                case CONFESS: {
                    drop(g, s * 0.2, oy + s * 0.25 + (t % 1.6) * s * 0.12, s * 0.1);
                    g.setColor(new Color(90, 110, 170, 150));
                    g.setStroke(new BasicStroke(Math.max(1.5f, s / 40f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    for (int k = 0; k < 3; k++) {
                        double x = s * (0.3 + k * 0.2);
                        g.draw(new java.awt.geom.Line2D.Double(x, oy + s * 0.04, x, oy + s * 0.16));
                    }
                    break;
                }
                default: break;
            }
            g.dispose();
        }

        private static void star(Graphics2D g, double cx, double cy, double r, Color c) {
            java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 4 * i, rr = i % 2 == 0 ? r : r * 0.35;
                double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            g.setColor(c);
            g.fill(p);
        }

        private static void drop(Graphics2D g, double x, double y, double r) {
            java.awt.geom.Path2D d = new java.awt.geom.Path2D.Double();
            d.moveTo(x, y - r * 1.4);
            d.quadTo(x + r, y, x, y + r * 0.8);
            d.quadTo(x - r, y, x, y - r * 1.4);
            g.setColor(new Color(0x8DC3E8));
            g.fill(d);
            g.setColor(new Color(0x3A6E9A));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(d);
        }
    }

    /**
     * 숫자가 0부터 목표값까지 빠르게 올라가는 글자(예: "+225", "100%", "1:23", "3일").
     * 최종 글자 크기로 자리를 미리 잡아 두어, 올라가는 동안 레이아웃을 다시 계산하지 않고 자기 자신만 다시 그린다.
     */
    public static class CountLabel extends JComponent {
        private static final java.util.regex.Pattern NUM = java.util.regex.Pattern.compile("^([^0-9]*)(\\d+)(?::(\\d{2}))?(.*)$");
        private final Color color;
        private String target = "", prefix = "", suffix = "";
        private int goal;
        private boolean time;
        private long start;
        private final javax.swing.Timer timer;
        private static final long DURATION = 900;

        public CountLabel(Font font, Color color) {
            this.color = color;
            setFont(font);
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            timer = new javax.swing.Timer(16, e -> {
                repaint();
                if (System.currentTimeMillis() - start > DURATION) ((javax.swing.Timer) e.getSource()).stop();
            });
        }

        /** delayMs 뒤부터 0 → 목표값으로 올라간다. 숫자가 없는 글은 그대로 보인다. */
        public void countTo(String text, long delayMs) {
            target = text == null ? "" : text;
            java.util.regex.Matcher m = NUM.matcher(target);
            if (m.matches()) {
                prefix = m.group(1);
                time = m.group(3) != null;
                goal = time ? Integer.parseInt(m.group(2)) * 60 + Integer.parseInt(m.group(3)) : Integer.parseInt(m.group(2));
                suffix = m.group(4);
            } else {
                goal = -1;
            }
            start = System.currentTimeMillis() + delayMs;
            revalidate();
            repaint();
            if (goal > 0) timer.restart();
        }

        private String shown() {
            if (goal < 0) return target;
            double p = Math.max(0, Math.min(1, (System.currentTimeMillis() - start) / (double) DURATION));
            double e = 1 - Math.pow(1 - p, 3);
            int v = (int) Math.round(goal * e);
            return prefix + (time ? (v / 60) + ":" + String.format("%02d", v % 60) : String.valueOf(v)) + suffix;
        }

        @Override public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(target) + 8, fm.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            g.setFont(getFont());
            g.setColor(color);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(shown(), 0, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
        }
    }
}
