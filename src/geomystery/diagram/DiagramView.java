package geomystery.diagram;

import geomystery.data.Model.Cmd;
import geomystery.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * diagrams.csv 의 도형 스크립트를 그리는 칠판.
 * 논리 좌표계는 640×440 (y 아래 방향), 각도는 수학 관례(반시계, 위쪽 양수).
 *
 * <pre>
 * pt N x y              polar N C r deg        along N A B t
 * offset N A dx dy      vec N B A C t          foot N P A B
 * apex N A B degA degB  labelcolor COLOR       unit PX SUFFIX   (길이 측정 단위: PX 픽셀 = 1 SUFFIX)
 * seg A B color w       line A B color w ext   arrow A B color w
 * circle C r color w    dial C r               hand C deg len w color   hub C
 * arc C r from to color w                      angle A O B r label color [w]
 * right A O B color [size]                     tick A B n color
 * parallel A B color    len A B text side color [offset]
 * tag N text color size text x y text color size
 * fill color alpha N1 N2 ...                   dot N dir [hollow|solid|hidden] [color]
 * </pre>
 * 너비 뒤에 'd'를 붙이면 점선(예: 2d).
 * angle 의 이름표가 '@'이면 실제로 잰 각도를, len 의 글자가 '@'이면 잰 길이(unit 기준)를 쓴다(탐구 모드용).
 * <p>
 * 연출: 보조선은 분필로 긋듯 차례로 그려지고, 정답을 맞히면 '?' 이름표 자리에 답이 써지며
 * 빨간 표시가 빛나고 시계 바늘이 한 바퀴 돈다. 칠판 위에 펜으로 낙서할 수도 있다.
 */
public class DiagramView extends JComponent {
    public static final double W = 640, H = 440;
    /** 칠판 위 색은 화면 테마와 상관없이 고정한다. */
    private static final Color INK = new Color(0x3A2819), BOARD_GREEN = new Color(0x2F5E50), CHALK = new Color(0xF4ECD8);
    private static final Color GLOW = new Color(0xFFE08A);

    /** 그리기 옵션(연출·탐구 모드). */
    public static final class Opts {
        /** 보조선 진행도 0~1. */
        public float aux = 0f;
        /** true 면 보조선을 차례로 긋고, false 면 투명도로 나타낸다. */
        public boolean drawIn = true;
        /** 정답 공개 진행도 0~1 (음수면 공개 안 함). */
        public float reveal = -1f;
        /** '?' 자리에 쓸 답. */
        public String answer;
        /** 탐구 모드: 끌어 놓은 점 위치. */
        public Map<String, Point2D.Double> overrides = Collections.emptyMap();
    }

    private List<Cmd> script = Collections.emptyList();
    private boolean showAux = false;
    private float auxProg = 0f;
    private float reveal = -1f;
    private String answer;
    private javax.swing.Timer fade, revealTimer;
    private String error;

    // ---- 풀이 다시 보기
    private List<String> steps = Collections.emptyList();
    private int stepIndex = -1;
    private long stepStart;
    private String replayAnswer;
    private javax.swing.Timer replayTimer;
    private Runnable onReplayEnd;

    /** 풀이 단계를 칠판 위에 하나씩 써 가며 보여 준다. '@aux' 단계는 보조선을 긋고 '@ans' 단계는 답을 쓴다. */
    public void playSteps(List<String> list, String answer, Runnable onEnd) {
        stopSteps();
        if (list == null || list.isEmpty()) return;
        steps = new ArrayList<>(list);
        replayAnswer = answer;
        onReplayEnd = onEnd;
        // 처음 상태로: 보조선과 써 둔 답을 지우고 시작
        if (fade != null) fade.stop();
        if (revealTimer != null) revealTimer.stop();
        showAux = false;
        auxProg = 0f;
        reveal = -1f;
        startStep(0);
        replayTimer = new javax.swing.Timer(33, e -> {
            if (stepIndex < 0) return;
            if (System.currentTimeMillis() - stepStart > stepDuration(steps.get(stepIndex))) nextStep();
            else repaintStrip();
        });
        replayTimer.start();
    }

    public boolean replaying() { return stepIndex >= 0; }

    /** 칠판 아래쪽 자막 띠 영역만 다시 그린다(칠판 전체·뒤 패널까지 다시 그리지 않도록). */
    private void repaintStrip() {
        double s = scale();
        int top = (int) ((getHeight() - H * s) / 2 + (H - 170) * s);
        repaint(0, Math.max(0, top), getWidth(), getHeight() - Math.max(0, top));
    }

    public void stopSteps() {
        if (replayTimer != null) replayTimer.stop();
        boolean was = stepIndex >= 0;
        stepIndex = -1;
        repaint();
        if (was && onReplayEnd != null) {
            Runnable r = onReplayEnd;
            onReplayEnd = null;
            r.run();
        }
    }

    private static long stepDuration(String s) { return Math.max(2300, 900 + stripDirective(s).length() * 75L); }

    private static String stripDirective(String s) {
        return s.startsWith("@aux ") || s.startsWith("@ans ") ? s.substring(5) : s;
    }

    private void startStep(int i) {
        stepIndex = i;
        stepStart = System.currentTimeMillis();
        String s = steps.get(i);
        if (s.startsWith("@aux ") && hasAux()) setAux(true);
        if (s.startsWith("@ans ") && replayAnswer != null) revealAnswer(replayAnswer);
        repaint();
    }

    /** 다음 단계로(마지막이면 마친다). 칠판을 클릭해도 넘어간다. */
    public void nextStep() {
        if (stepIndex < 0) return;
        if (stepIndex + 1 < steps.size()) startStep(stepIndex + 1);
        else {
            // 마지막 단계는 잠시 남겨 두었다가 끝낸다
            if (System.currentTimeMillis() - stepStart < stepDuration(steps.get(stepIndex)) + 1200) { repaint(); return; }
            stopSteps();
        }
    }

    /** 풀이 단계 자막(칠판 아래쪽 띠). */
    private void paintStep(Graphics2D g) {
        if (stepIndex < 0 || stepIndex >= steps.size()) return;
        String text = stripDirective(steps.get(stepIndex));
        long el = System.currentTimeMillis() - stepStart;
        float sz = 22f / Theme.scale();
        g.setFont(Theme.hand(sz * 1.15f));
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            String trial = cur.length() == 0 ? w : cur + " " + w;
            if (Theme.width(fm, trial) > W - 90 && cur.length() > 0) { lines.add(cur.toString()); cur.setLength(0); cur.append(w); }
            else { cur.setLength(0); cur.append(trial); }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        int lh = fm.getHeight();
        double bh = 26 + lines.size() * lh, by = H - bh - 8;
        float appear = (float) Math.min(1, el / 250.0);
        Composite keep = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, appear));
        g.setColor(new Color(20, 40, 34, 215));
        g.fill(new RoundRectangle2D.Double(18, by, W - 36, bh, 16, 16));
        g.setColor(new Color(230, 190, 85, 160));
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new RoundRectangle2D.Double(18, by, W - 36, bh, 16, 16));
        // 단계 점
        for (int i = 0; i < steps.size(); i++) {
            g.setColor(i == stepIndex ? GLOW : i < stepIndex ? new Color(230, 190, 85, 160) : new Color(255, 255, 255, 60));
            g.fill(new Ellipse2D.Double(W - 40 - (steps.size() - i) * 12, by + 9, 7, 7));
        }
        g.setFont(Theme.bold(12f / Theme.scale()));
        g.setColor(new Color(230, 190, 85));
        g.drawString("풀이 " + (stepIndex + 1) + " / " + steps.size() + "  ·  칠판을 누르면 다음", 34, (float) by + 17);
        // 분필로 써 나가듯 왼쪽부터 드러난다
        g.setFont(Theme.hand(sz * 1.15f));
        int total = 0;
        for (String l : lines) total += l.length();
        double shown = Math.min(total, el / 38.0);
        double y = by + 22 + fm.getAscent();
        for (String l : lines) {
            int n = (int) Math.min(l.length(), Math.max(0, shown));
            shown -= l.length();
            if (n > 0) {
                g.setColor(CHALK);
                Theme.drawString(g, l.substring(0, n), 36, (float) y);
            }
            y += lh;
        }
        g.setComposite(keep);
    }

    // ---- 낙서 펜
    public enum Tool { NONE, CHALK, GOLD, RED, ERASER }
    private Tool tool = Tool.NONE;
    private final List<Scribble> scribbles = new ArrayList<>();
    private Scribble drawing;

    private static final class Scribble {
        final Color color;
        final float width;
        final Path2D.Double path = new Path2D.Double();
        final List<Point2D.Double> pts = new ArrayList<>();
        Scribble(Color c, float w) { color = c; width = w; }
    }

    public DiagramView() {
        setOpaque(false);
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (tool == Tool.NONE) {
                    if (stepIndex >= 0) nextStepNow();
                    return;
                }
                Point2D.Double p = toLogical(e.getPoint());
                if (tool == Tool.ERASER) { erase(p); return; }
                Color c = tool == Tool.GOLD ? new Color(0xF2CE63) : tool == Tool.RED ? new Color(0xF08A70) : new Color(0xF7F1E3);
                drawing = new Scribble(c, 3.2f);
                drawing.path.moveTo(p.x, p.y);
                drawing.pts.add(p);
                scribbles.add(drawing);
                repaint();
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (tool == Tool.NONE) return;
                Point2D.Double p = toLogical(e.getPoint());
                if (tool == Tool.ERASER) { erase(p); return; }
                if (drawing == null) return;
                drawing.path.lineTo(p.x, p.y);
                drawing.pts.add(p);
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) { drawing = null; }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    /** 클릭으로 바로 다음 단계(마지막 단계면 끝). */
    private void nextStepNow() {
        if (stepIndex + 1 < steps.size()) startStep(stepIndex + 1);
        else stopSteps();
    }

    public void setScript(List<Cmd> cmds) {
        if (stepIndex >= 0) { onReplayEnd = null; stopSteps(); }
        script = cmds == null ? Collections.<Cmd>emptyList() : cmds;
        showAux = false;
        auxProg = 0f;
        reveal = -1f;
        answer = null;
        if (fade != null) fade.stop();
        if (revealTimer != null) revealTimer.stop();
        scribbles.clear();
        clearFx();
        repaint();
    }

    // ---- 정답·오답 분필 연출
    /** 칠판에 크게 쓰는 글자(분필), 찍히는 정리 도장, 오답이면 true. */
    private String fxText, fxStamp;
    private boolean fxWrong, fxStampSound;
    private long fxStart = -1;
    private javax.swing.Timer fxTimer;
    /** 글자 윤곽(한 번만 만든다: 분필 결을 이 모양 안에만 찍는다). */
    private Shape fxShape;
    private static final long FX_RIGHT = 2600, FX_WRONG = 1750, STAMP_AT = 760, STAMP_IN = 170;

    /** 정답: 답을 분필로 크게 쓰고 정리 이름 도장을 찍는다(도장은 다음 문제까지 남는다). */
    public void chalkAnswer(String text, String stamp) {
        fxText = text;
        fxStamp = stamp;
        fxWrong = false;
        startFx(FX_RIGHT);
    }

    /** 오답: 쓴 답을 분필로 적고 빨간 X를 그은 뒤 지우개로 지운다. */
    public void chalkWrong(String text) {
        fxText = text;
        fxStamp = null;
        fxWrong = true;
        startFx(FX_WRONG);
    }

    public void clearFx() {
        if (fxTimer != null) fxTimer.stop();
        boolean had = fxStart >= 0;
        fxStart = -1;
        fxText = null;
        fxStamp = null;
        fxShape = null;
        fxTextImg = null;
        fxStampImg = null;
        if (had) repaint();
    }

    private void startFx(long dur) {
        if (fxTimer != null) fxTimer.stop();
        fxShape = null;
        fxTextImg = null;
        fxStampImg = null;
        fxStampSound = false;
        fxStart = System.currentTimeMillis();
        boolean[] eraseSound = {false};
        fxTimer = new javax.swing.Timer(16, e -> {
            long el = System.currentTimeMillis() - fxStart;
            if (!fxWrong && fxStamp != null && !fxStampSound && el >= STAMP_AT + STAMP_IN) {
                fxStampSound = true;
                geomystery.ui.Sfx.play("stamp");
            }
            if (fxWrong && !eraseSound[0] && el >= 880) {
                eraseSound[0] = true;
                geomystery.ui.Sfx.play("erase");
            }
            repaintFx();
            if (el > dur) {
                ((javax.swing.Timer) e.getSource()).stop();
                if (fxWrong) { fxText = null; fxStart = -1; }
                else fxText = null; // 도장은 남긴다
                repaintFx();
            }
        });
        fxTimer.start();
        repaintFx();
    }

    /** 연출이 그려지는 부분(가운데 글자 띠 + 오른쪽 아래 도장)만 다시 그린다. */
    private void repaintFx() {
        double s = scale();
        double ox = (getWidth() - W * s) / 2, oy = (getHeight() - H * s) / 2;
        repaint((int) (ox + 40 * s) - 2, (int) (oy + (H / 2 - 110) * s) - 2, (int) ((W - 80) * s) + 4, (int) (220 * s) + 4);
        repaint((int) (ox + (W - 300) * s) - 2, (int) (oy + (H - 130) * s) - 2, (int) (300 * s) + 4, (int) (130 * s) + 4);
    }

    private static double ease(double k) { k = Math.max(0, Math.min(1, k)); return 1 - Math.pow(1 - k, 3); }

    /** 분필 글씨(그림자·분필 결 포함)와 도장을 미리 그려 둔 이미지. 효과가 시작될 때 한 번만 만든다. */
    private java.awt.image.BufferedImage fxTextImg, fxStampImg;
    private Rectangle2D fxTextBox, fxStampBox;
    private double fxImgScale;

    private static double devScale(Graphics2D g) {
        AffineTransform t = g.getTransform();
        return Math.max(0.5, Math.hypot(t.getScaleX(), t.getShearY()));
    }

    /** 칠판 좌표의 사각형(x, y, w, h)을 화면 픽셀 크기 그대로 담는 빈 이미지. */
    private static java.awt.image.BufferedImage fxCanvas(Rectangle2D r, double sc) {
        return new java.awt.image.BufferedImage(Math.max(1, (int) Math.ceil(r.getWidth() * sc)), Math.max(1, (int) Math.ceil(r.getHeight() * sc)),
                java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
    }

    private void buildFxText(Graphics2D like, double sc) {
        float size = (fxWrong ? 80f : 96f) / Theme.scale();
        Font f = Theme.hand(size);
        Shape raw = Theme.outline(fxText, f, like.getFontRenderContext());
        Rectangle2D b = raw.getBounds2D();
        double maxW = W - 120;
        double k = b.getWidth() > maxW ? maxW / b.getWidth() : 1;
        AffineTransform at = AffineTransform.getTranslateInstance(W / 2, H / 2);
        at.scale(k, k);
        at.translate(-b.getCenterX(), -b.getCenterY());
        fxShape = at.createTransformedShape(raw);
        Rectangle2D tb = fxShape.getBounds2D();
        fxTextBox = new Rectangle2D.Double(tb.getX() - 12, tb.getY() - 12, tb.getWidth() + 24, tb.getHeight() + 24);
        fxTextImg = fxCanvas(fxTextBox, sc);
        Graphics2D g = Theme.aa(fxTextImg.createGraphics());
        g.scale(sc, sc);
        g.translate(-fxTextBox.getX(), -fxTextBox.getY());
        g.setColor(fxWrong ? new Color(0xEDE3CC) : new Color(0xFFF6DE));
        g.fill(fxShape);
        // 분필 결: 이미 칠한 글자 위에만(SRC_ATOP) 짙은 점을 찍는다 — 모양으로 잘라내는 것보다 훨씬 빠르다
        g.setComposite(AlphaComposite.SrcAtop);
        Random r = new Random(fxText.hashCode());
        g.setColor(new Color(47, 94, 80, 150));
        for (int i = 0; i < 140; i++) {
            double px = tb.getX() + r.nextDouble() * tb.getWidth(), py = tb.getY() + r.nextDouble() * tb.getHeight();
            double d = 1.2 + r.nextDouble() * 2.2;
            g.fill(new Ellipse2D.Double(px, py, d, d * 0.7));
        }
        // 그림자는 글자 뒤에(DST_OVER)
        g.setComposite(AlphaComposite.DstOver);
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(AffineTransform.getTranslateInstance(2.5, 3).createTransformedShape(fxShape));
        g.dispose();
    }

    private void buildFxStamp(double sc) {
        Font nameF = Theme.xbold(19f / Theme.scale());
        Font topF = Theme.bold(11.5f / Theme.scale());
        java.awt.image.BufferedImage probe = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        FontMetrics fm = probe.getGraphics().getFontMetrics(nameF);
        double bw = Math.max(150, Math.min(270, fm.stringWidth(fxStamp) + 40)), bh = 62;
        double half = Math.hypot(bw, bh) / 2 + 6;
        fxStampBox = new Rectangle2D.Double(-half, -half, half * 2, half * 2);
        fxStampImg = fxCanvas(fxStampBox, sc);
        Graphics2D st = Theme.aa(fxStampImg.createGraphics());
        st.scale(sc, sc);
        st.translate(half, half);
        st.rotate(Math.toRadians(-8));
        Color red = new Color(0xE05A42);
        st.setColor(red);
        st.setStroke(new BasicStroke(3.2f));
        st.draw(new RoundRectangle2D.Double(-bw / 2, -bh / 2, bw, bh, 12, 12));
        st.setStroke(new BasicStroke(1.4f));
        st.draw(new RoundRectangle2D.Double(-bw / 2 + 5, -bh / 2 + 5, bw - 10, bh - 10, 8, 8));
        st.setFont(topF);
        FontMetrics tm = st.getFontMetrics();
        String top = "증 명 완 료";
        st.drawString(top, (float) (-tm.stringWidth(top) / 2.0), (float) (-bh / 2 + 21));
        st.setFont(nameF);
        String name = fxStamp;
        while (fm.stringWidth(name) > bw - 22 && name.length() > 2) name = name.substring(0, name.length() - 1);
        if (!name.equals(fxStamp)) name = name.substring(0, name.length() - 1) + "…";
        Theme.drawString(st, name, (float) (-Theme.width(st.getFontMetrics(), name) / 2.0), (float) (bh / 2 - 12));
        st.dispose();
    }

    /** 미리 그린 이미지를 칠판 좌표 r 에 1:1(화면 픽셀)로 복사한다. */
    private static void fxBlit(Graphics2D g, java.awt.image.BufferedImage img, Rectangle2D r) {
        geomystery.ui.Ui.blit(g, img, r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    private void paintFx(Graphics2D g) {
        if (fxStart < 0) return;
        long el = System.currentTimeMillis() - fxStart;
        double sc = devScale(g);
        if (Math.abs(sc - fxImgScale) > 0.01) { fxTextImg = null; fxStampImg = null; fxImgScale = sc; }
        if (fxText != null) {
            if (fxTextImg == null) buildFxText(g, sc);
            Rectangle2D tb = fxShape.getBounds2D();
            double write = ease(el / (fxWrong ? 380.0 : 650.0));
            float fade = fxWrong ? 1f : (float) (el < 1900 ? 1 : Math.max(0, 1 - (el - 1900) / 600.0));
            double erase = fxWrong ? ease((el - 900) / 600.0) : 0;
            if (fade > 0 && erase < 1) {
                Composite keep = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
                // 글자를 읽기 쉽게 깔아 주는 짙은 칠판 바탕
                float back = (float) (Math.min(1, el / 150.0) * 0.82 * (1 - erase));
                g.setColor(new Color(0x24, 0x4A, 0x3F, Math.round(255 * back)));
                g.fill(new RoundRectangle2D.Double(tb.getX() - 34, tb.getY() - 22, tb.getWidth() + 68, tb.getHeight() + 44, 30, 30));
                Shape oldClip = g.getClip();
                double x0 = tb.getX() - 6 + (tb.getWidth() + 12) * erase;
                g.clip(new Rectangle2D.Double(x0, tb.getY() - 30, (tb.getX() - 6 + (tb.getWidth() + 12) * write) - x0, tb.getHeight() + 60));
                fxBlit(g, fxTextImg, fxTextBox);
                g.setClip(oldClip);
                // 분필 끝 가루
                if (write < 1) {
                    double tx = tb.getX() + tb.getWidth() * write;
                    Random d = new Random(el / 40);
                    g.setColor(new Color(255, 250, 235, 180));
                    for (int i = 0; i < 6; i++) {
                        double sz = 1.5 + d.nextDouble() * 2.5;
                        g.fill(new Ellipse2D.Double(tx + d.nextGaussian() * 6, tb.getCenterY() + d.nextGaussian() * tb.getHeight() * 0.35, sz, sz));
                    }
                }
                if (fxWrong) {
                    // 빨간 X 두 획
                    g.setColor(new Color(0xE0664C));
                    g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    double pad = 18;
                    double ax = tb.getX() - pad, ay = tb.getY() - pad, bx = tb.getMaxX() + pad, by = tb.getMaxY() + pad;
                    double k1 = ease((el - 380) / 200.0), k2 = ease((el - 580) / 200.0);
                    Shape oc = g.getClip();
                    g.clip(new Rectangle2D.Double(x0, -10, W + 20, H + 20));
                    if (k1 > 0) g.draw(new Line2D.Double(ax, ay, ax + (bx - ax) * k1, ay + (by - ay) * k1));
                    if (k2 > 0) g.draw(new Line2D.Double(bx, ay, bx + (ax - bx) * k2, ay + (by - ay) * k2));
                    g.setClip(oc);
                    // 지우개
                    if (erase > 0 && erase < 1) {
                        double ex = x0 - 20;
                        g.setColor(new Color(255, 250, 235, 40));
                        g.fill(new Rectangle2D.Double(tb.getX() - 40, tb.getY() - 30, ex - tb.getX() + 40, tb.getHeight() + 60));
                        g.setColor(new Color(0x7A5232));
                        g.fill(new RoundRectangle2D.Double(ex - 26, tb.getCenterY() - 46, 52, 92, 10, 10));
                        g.setColor(new Color(0xD9CDB0));
                        g.fill(new RoundRectangle2D.Double(ex - 26, tb.getCenterY() - 46, 52, 22, 10, 10));
                    }
                }
                g.setComposite(keep);
            }
        }
        if (!fxWrong && fxStamp != null && el >= STAMP_AT) {
            if (fxStampImg == null) buildFxStamp(sc);
            double k = Math.min(1, (el - STAMP_AT) / (double) STAMP_IN);
            double z = 2.1 - 1.1 * (k * k);
            float a = (float) Math.min(1, k * 1.4);
            double cx = W - 150, cy = H - 66;
            Composite keep = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.92f));
            if (k < 1) {
                // 찍히는 0.17초 동안만 크기를 바꿔 그린다
                Graphics2D st = (Graphics2D) g.create();
                st.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                st.translate(cx, cy);
                st.scale(z, z);
                st.drawImage(fxStampImg, (int) Math.round(fxStampBox.getX()), (int) Math.round(fxStampBox.getY()),
                        (int) Math.round(fxStampBox.getWidth()), (int) Math.round(fxStampBox.getHeight()), null);
                st.dispose();
            } else {
                fxBlit(g, fxStampImg, new Rectangle2D.Double(cx + fxStampBox.getX(), cy + fxStampBox.getY(), fxStampBox.getWidth(), fxStampBox.getHeight()));
            }
            g.setComposite(keep);
            // 도장 잉크가 튄 자국(찍히는 순간)
            long since = el - STAMP_AT - STAMP_IN;
            if (since > 0 && since < 320) {
                float sa = (float) (1 - since / 320.0);
                g.setColor(new Color(224, 90, 66, Math.round(160 * sa)));
                double rx = fxStampBox.getWidth() * 0.42 + since * 0.05;
                for (int i = 0; i < 8; i++) {
                    double ang = i * Math.PI / 4 + 0.3;
                    g.fill(new Ellipse2D.Double(cx + Math.cos(ang) * rx - 2.5, cy + Math.sin(ang) * rx * 0.45 - 2.5, 5, 5));
                }
            }
        }
    }

    public boolean hasAux() {
        for (Cmd c : script) if ("aux".equals(c.layer) && isDrawing(c.op)) return true;
        return false;
    }

    public boolean isAuxShown() { return showAux; }

    /** 보조선을 긋는다(분필로 차례로) / 지운다(서서히). */
    public void setAux(boolean on) {
        showAux = on;
        if (fade != null) fade.stop();
        int n = Math.max(1, auxDrawCount());
        float step = on ? Math.min(0.05f, 1f / (n * 14f)) : 0.1f;
        fade = new javax.swing.Timer(16, e -> {
            auxProg += showAux ? step : -step;
            if (auxProg >= 1f || auxProg <= 0f) {
                auxProg = Math.max(0f, Math.min(1f, auxProg));
                ((javax.swing.Timer) e.getSource()).stop();
            }
            repaint();
        });
        fade.start();
    }

    private int auxDrawCount() {
        int n = 0;
        for (Cmd c : script) if ("aux".equals(c.layer) && isDrawing(c.op) && !"dot".equals(c.op)) n++;
        return n;
    }

    /** 정답을 칠판에 쓴다: '?' 이름표가 답으로 바뀌고 빨간 표시가 빛나며 시계 바늘이 돈다. */
    public void revealAnswer(String ans) {
        answer = ans;
        reveal = 0f;
        if (revealTimer != null) revealTimer.stop();
        revealTimer = new javax.swing.Timer(16, e -> {
            reveal += 0.012f;
            if (reveal >= 1f) { reveal = 1f; ((javax.swing.Timer) e.getSource()).stop(); }
            repaint();
        });
        revealTimer.start();
    }

    /** '?' 이름표가 있는지(정답을 칠판에 쓸 자리가 있는지). */
    public static boolean hasQuestionLabel(List<Cmd> script) {
        for (Cmd c : script) for (String a : c.args) if (a.contains("?")) return true;
        return false;
    }

    // ---- 펜
    public void setTool(Tool t) {
        tool = t == null ? Tool.NONE : t;
        setCursor(Cursor.getPredefinedCursor(tool == Tool.NONE ? Cursor.DEFAULT_CURSOR : Cursor.CROSSHAIR_CURSOR));
    }

    public Tool tool() { return tool; }

    public void clearScribbles() { scribbles.clear(); repaint(); }

    private void erase(Point2D.Double p) {
        boolean removed = scribbles.removeIf(s -> {
            for (Point2D.Double q : s.pts) if (q.distance(p) < 14) return true;
            return false;
        });
        if (removed) repaint();
    }

    private double scale() { return Math.min(getWidth() / W, getHeight() / H); }

    public Point2D.Double toLogical(Point p) {
        double s = scale();
        return new Point2D.Double((p.x - (getWidth() - W * s) / 2) / s, (p.y - (getHeight() - H * s) / 2) / s);
    }

    /** 도형 스크립트에서 쓸 수 있는 명령. */
    public static final List<String> DEFINE_OPS = Arrays.asList("pt", "polar", "along", "offset", "vec", "foot", "apex", "tan");
    public static final List<String> DRAW_OPS = Arrays.asList("labelcolor", "unit", "seg", "line", "ray", "arrow", "circle", "dial",
            "hand", "hub", "arc", "angle", "right", "tick", "parallel", "len", "tag", "text", "fill", "sector", "wedge", "dot");

    /** 명령마다 반드시 있어야 하는 인자 수. */
    private static int minArgs(String op) {
        switch (op) {
            case "pt": case "offset": return 3;
            case "polar": case "along": return 4;
            case "vec": case "apex": return 5;
            case "foot": return 4;
            case "seg": case "line": case "ray": case "arrow": case "tick": case "parallel": return 2;
            case "angle": case "right": return 3;
            case "len": return 3;
            case "text": return 3;
            case "fill": return 5;
            case "sector": return 4;
            case "wedge": return 3;
            case "tan": return 5;
            case "labelcolor": case "unit": case "circle": case "dial": case "hub": case "tag": case "dot": case "hand": case "arc": return 1;
            default: return 0;
        }
    }

    /** 명령이 참조하는 점 이름들(인자 위치). */
    private static int[] pointArgs(Cmd c) {
        switch (c.op) {
            case "polar": case "offset": return new int[]{1};
            case "along": case "apex": return new int[]{1, 2};
            case "vec": case "foot": return new int[]{1, 2, 3};
            case "tan": return new int[]{1, 3};
            case "wedge": return new int[]{0, 1, 2};
            case "seg": case "line": case "ray": case "arrow": case "tick": case "parallel": case "len": return new int[]{0, 1};
            case "circle": case "dial": case "hand": case "hub": case "arc": case "tag": case "dot": case "sector": return new int[]{0};
            case "angle": case "right": return new int[]{0, 1, 2};
            case "fill": {
                int[] r = new int[Math.max(0, c.args.length - 2)];
                for (int i = 0; i < r.length; i++) r[i] = i + 2;
                return r;
            }
            default: return new int[0];
        }
    }

    /**
     * 스크립트 문제를 찾는다: 모르는 명령, 인자 부족, 정의 전에 쓴 점, 정의되지 않은 점, 그리기 오류.
     * 각 문자열 앞에는 몇 번째 명령인지(1부터) 붙는다.
     */
    public static List<String> check(List<Cmd> script) {
        List<String> out = new ArrayList<>();
        Set<String> defined = new HashSet<>();
        for (Cmd c : script) if (DEFINE_OPS.contains(c.op) && c.args.length > 0) defined.add(c.args[0]);
        Set<String> soFar = new HashSet<>();
        for (int i = 0; i < script.size(); i++) {
            Cmd c = script.get(i);
            String at = "#" + (i + 1) + " " + c.op + ": ";
            if (!"base".equals(c.layer) && !"aux".equals(c.layer)) out.add(at + "레이어는 base 또는 aux 여야 합니다 (" + c.layer + ")");
            if (!DEFINE_OPS.contains(c.op) && !DRAW_OPS.contains(c.op)) { out.add(at + "알 수 없는 명령"); continue; }
            if (c.args.length < minArgs(c.op)) { out.add(at + "인자가 부족합니다 (최소 " + minArgs(c.op) + "개)"); continue; }
            for (int k : pointArgs(c)) {
                if (k >= c.args.length) continue;
                String n = c.args[k];
                if (DEFINE_OPS.contains(c.op)) {
                    if (!soFar.contains(n)) out.add(at + "점 " + n + "을(를) 정의하기 전에 사용했습니다");
                } else if (!defined.contains(n)) {
                    out.add(at + "정의되지 않은 점 " + n);
                }
            }
            if (DEFINE_OPS.contains(c.op)) soFar.add(c.args[0]);
        }
        if (out.isEmpty()) {
            java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(64, 44, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = im.createGraphics();
            try {
                g.scale(0.1, 0.1);
                Opts o = new Opts();
                o.aux = 1f;
                o.reveal = 1f;
                o.answer = "1";
                render(g, script, o);
            } catch (RuntimeException ex) {
                out.add("그리는 중 오류: " + ex);
            } finally {
                g.dispose();
            }
        }
        return out;
    }

    /** 스크립트가 정의하는 점들의 논리 좌표(편집기용). 오류가 있는 정의는 건너뛴다. */
    public static Map<String, Point2D.Double> points(List<Cmd> script) {
        return points(script, Collections.emptyMap());
    }

    /** 끌어 놓은 점(overrides)을 반영한 좌표. */
    public static Map<String, Point2D.Double> points(List<Cmd> script, Map<String, Point2D.Double> overrides) {
        Map<String, Point2D.Double> pts = new LinkedHashMap<>();
        for (Cmd c : script) {
            try {
                define(c, pts);
                if (DEFINE_OPS.contains(c.op) && overrides.containsKey(c.args[0])) pts.put(c.args[0], overrides.get(c.args[0]));
            } catch (RuntimeException ignored) { }
        }
        pts.values().removeIf(Objects::isNull);
        return pts;
    }

    /** ∠AOB (0~180°). */
    public static double angleAt(Point2D.Double A, Point2D.Double O, Point2D.Double B) {
        double a1 = Math.atan2(A.y - O.y, A.x - O.x), a2 = Math.atan2(B.y - O.y, B.x - O.x);
        double d = Math.abs(Math.toDegrees(a1 - a2)) % 360;
        return d > 180 ? 360 - d : d;
    }

    /** unit 명령의 (픽셀/단위, 접미사). 없으면 (40, ""). */
    public static Object[] unitOf(List<Cmd> script) {
        for (Cmd c : script) if ("unit".equals(c.op)) return new Object[]{d(c.args, 0, 40), sArg(c.args, 1, "")};
        return new Object[]{40.0, ""};
    }

    private static boolean isDrawing(String op) {
        switch (op) {
            case "pt": case "polar": case "along": case "offset": case "vec": case "foot": case "apex": case "tan": case "labelcolor": case "unit":
                return false;
            default:
                return true;
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.aa(g0.create());
        double s = scale();
        g.translate((getWidth() - W * s) / 2, (getHeight() - H * s) / 2);
        g.scale(s, s);
        try {
            Opts o = new Opts();
            o.aux = showAux ? Math.max(auxProg, 0.001f) : auxProg;
            o.drawIn = showAux;
            o.reveal = reveal;
            o.answer = answer;
            render(g, script, o);
        } catch (RuntimeException ex) {
            if (!String.valueOf(ex).equals(error)) {
                error = String.valueOf(ex);
                System.err.println("[diagram] " + ex);
            }
        }
        paintStep(g);
        paintFx(g);
        // 낙서
        for (Scribble sc : scribbles) {
            g.setColor(new Color(0, 0, 0, 40));
            g.setStroke(new BasicStroke(sc.width + 1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.translate(1, 1.5);
            g.draw(sc.path);
            g.translate(-1, -1.5);
            g.setColor(sc.color);
            g.setStroke(new BasicStroke(sc.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(sc.path);
        }
        g.dispose();
    }

    // =====================================================================
    /** 스크립트를 그린다(보조선은 투명도로). */
    public static void render(Graphics2D g, List<Cmd> script, float auxAlpha) {
        Opts o = new Opts();
        o.aux = auxAlpha;
        o.drawIn = false;
        render(g, script, o);
    }

    public static void render(Graphics2D g, List<Cmd> script, Opts o) {
        Map<String, Point2D.Double> pts = new HashMap<>();
        Ctx ctx = new Ctx(pts);
        ctx.reveal = o.reveal;
        ctx.answer = o.answer;
        Object[] unit = unitOf(script);
        ctx.unitPx = (Double) unit[0];
        ctx.unitSuffix = (String) unit[1];
        // 1) 점 정의 (탐구 모드에서 끈 점은 그 위치로 바꿔 끼운다)
        for (Cmd c : script) {
            define(c, pts);
            if (DEFINE_OPS.contains(c.op) && o.overrides.containsKey(c.args[0])) pts.put(c.args[0], o.overrides.get(c.args[0]));
        }
        // 2) 기본 레이어
        for (Cmd c : script) if (!"aux".equals(c.layer) && !"dot".equals(c.op)) draw(g, c, ctx);
        // 3) 보조선
        if (o.aux > 0f) {
            Composite old = g.getComposite();
            if (!o.drawIn) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, o.aux)));
                for (Cmd c : script) if ("aux".equals(c.layer) && !"dot".equals(c.op)) draw(g, c, ctx);
            } else {
                List<Cmd> aux = new ArrayList<>();
                for (Cmd c : script) if ("aux".equals(c.layer) && !"dot".equals(c.op)) aux.add(c);
                int n = 0;
                for (Cmd c : aux) if (isDrawing(c.op)) n++;
                int i = 0;
                for (Cmd c : aux) {
                    if (!isDrawing(c.op)) { draw(g, c, ctx); continue; }
                    double local = Math.max(0, Math.min(1, o.aux * n - i));
                    i++;
                    if (local <= 0) continue;
                    ctx.prog = local;
                    if (!isStrokeOp(c.op)) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) local));
                    draw(g, c, ctx);
                    g.setComposite(old);
                }
                ctx.prog = 1;
                // 분필 끝에 날리는 가루
                if (o.aux < 1f && ctx.tip != null) {
                    Random r = new Random((long) (o.aux * 1000));
                    g.setColor(new Color(255, 250, 235, 170));
                    for (int k = 0; k < 7; k++) {
                        double dx = r.nextGaussian() * 5, dy = r.nextGaussian() * 5;
                        double sz = 1 + r.nextDouble() * 2.2;
                        g.fill(new Ellipse2D.Double(ctx.tip.x + dx, ctx.tip.y + dy, sz, sz));
                    }
                }
            }
            g.setComposite(old);
        }
        // 4) 점과 이름
        for (Cmd c : script) if ("dot".equals(c.op) && (!"aux".equals(c.layer) || o.aux > 0.5f)) draw(g, c, ctx);
    }

    private static boolean isStrokeOp(String op) {
        switch (op) {
            case "seg": case "line": case "ray": case "arrow": case "circle": case "arc": case "angle": return true;
            default: return false;
        }
    }

    private static final class Ctx {
        final Map<String, Point2D.Double> pts;
        Color labelColor = CHALK;
        /** 지금 그리는 명령의 진행도(보조선 긋기). */
        double prog = 1;
        Point2D.Double tip;
        float reveal = -1f;
        String answer;
        double unitPx = 40;
        String unitSuffix = "";

        Ctx(Map<String, Point2D.Double> pts) { this.pts = pts; }

        Point2D.Double p(String n) {
            Point2D.Double q = pts.get(n);
            if (q == null) throw new IllegalArgumentException("정의되지 않은 점: " + n);
            return q;
        }

        boolean revealing() { return reveal >= 0 && answer != null; }

        /** 빛남 세기(정답 공개 중 잠깐 밝아졌다가 은은하게 남는다). */
        float glow() {
            if (reveal < 0) return 0;
            return (float) (0.35 + 0.65 * Math.sin(Math.min(1, reveal * 1.6) * Math.PI));
        }

        /** 진행도에 맞춰 선을 일부만 긋는다. 정답 공개 중에는 빨간 선 아래에 빛을 깐다. */
        void stroke(Graphics2D g, Shape s) {
            Shape shape = prog >= 1 ? s : partial(s, prog, this);
            Color c = g.getColor();
            if (reveal >= 0 && isRed(c)) {
                Stroke keep = g.getStroke();
                float w = keep instanceof BasicStroke ? ((BasicStroke) keep).getLineWidth() : 3f;
                g.setColor(new Color(GLOW.getRed(), GLOW.getGreen(), GLOW.getBlue(), Math.round(110 * glow())));
                g.setStroke(new BasicStroke(w + 9, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(shape);
                g.setStroke(keep);
                g.setColor(c);
            }
            g.draw(shape);
        }
    }

    private static boolean isRed(Color c) { return c.getRed() > 200 && c.getGreen() < 140 && c.getBlue() < 120; }

    /** 도형의 앞부분(전체 길이의 t)만 남긴 경로. 끝점을 ctx.tip 에 적는다. */
    private static Shape partial(Shape s, double t, Ctx ctx) {
        PathIterator it = s.getPathIterator(null, 0.5);
        List<double[]> segs = new ArrayList<>();
        double[] c = new double[6];
        double sx = 0, sy = 0, lx = 0, ly = 0, total = 0;
        while (!it.isDone()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_MOVETO) { sx = lx = c[0]; sy = ly = c[1]; segs.add(new double[]{0, c[0], c[1], 0}); }
            else if (type == PathIterator.SEG_LINETO || type == PathIterator.SEG_CLOSE) {
                double x = type == PathIterator.SEG_CLOSE ? sx : c[0], y = type == PathIterator.SEG_CLOSE ? sy : c[1];
                double len = Math.hypot(x - lx, y - ly);
                segs.add(new double[]{1, x, y, len});
                total += len;
                lx = x; ly = y;
            }
            it.next();
        }
        double want = total * t, acc = 0;
        Path2D.Double out = new Path2D.Double();
        double px = 0, py = 0;
        for (double[] sg : segs) {
            if (sg[0] == 0) { out.moveTo(sg[1], sg[2]); px = sg[1]; py = sg[2]; continue; }
            if (acc + sg[3] >= want) {
                double f = sg[3] == 0 ? 1 : (want - acc) / sg[3];
                double x = px + (sg[1] - px) * f, y = py + (sg[2] - py) * f;
                out.lineTo(x, y);
                ctx.tip = new Point2D.Double(x, y);
                return out;
            }
            out.lineTo(sg[1], sg[2]);
            acc += sg[3];
            px = sg[1]; py = sg[2];
        }
        ctx.tip = new Point2D.Double(px, py);
        return out;
    }

    private static double d(String[] a, int i, double def) {
        if (a.length <= i) return def;
        try { return Double.parseDouble(a[i]); } catch (NumberFormatException e) { return def; }
    }

    private static String sArg(String[] a, int i, String def) { return a.length > i ? a[i] : def; }

    private static Point2D.Double polar(Point2D.Double c, double r, double deg) {
        double rad = Math.toRadians(deg);
        return new Point2D.Double(c.x + r * Math.cos(rad), c.y - r * Math.sin(rad));
    }

    /** 화면 좌표의 벡터를 수학 각도(도)로. */
    private static double dirDeg(Point2D.Double from, Point2D.Double to) {
        return Math.toDegrees(Math.atan2(-(to.y - from.y), to.x - from.x));
    }

    private static void define(Cmd c, Map<String, Point2D.Double> pts) {
        String[] a = c.args;
        switch (c.op) {
            case "pt": pts.put(a[0], new Point2D.Double(d(a, 1, 0), d(a, 2, 0))); break;
            case "polar": pts.put(a[0], polar(pts.get(a[1]), d(a, 2, 0), d(a, 3, 0))); break;
            case "along": {
                Point2D.Double p = pts.get(a[1]), q = pts.get(a[2]);
                double t = d(a, 3, 0.5);
                pts.put(a[0], new Point2D.Double(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t));
                break;
            }
            case "offset": {
                Point2D.Double p = pts.get(a[1]);
                pts.put(a[0], new Point2D.Double(p.x + d(a, 2, 0), p.y + d(a, 3, 0)));
                break;
            }
            case "vec": {
                Point2D.Double b = pts.get(a[1]), p = pts.get(a[2]), q = pts.get(a[3]);
                double t = d(a, 4, 1);
                pts.put(a[0], new Point2D.Double(b.x + (q.x - p.x) * t, b.y + (q.y - p.y) * t));
                break;
            }
            case "foot": {
                Point2D.Double p = pts.get(a[1]), u = pts.get(a[2]), v = pts.get(a[3]);
                double dx = v.x - u.x, dy = v.y - u.y;
                double t = ((p.x - u.x) * dx + (p.y - u.y) * dy) / (dx * dx + dy * dy);
                pts.put(a[0], new Point2D.Double(u.x + dx * t, u.y + dy * t));
                break;
            }
            case "tan": {
                // tan 이름 O 반지름 P 방향(1|-1): 원 밖의 점 P에서 원(O, 반지름)에 그은 접선의 접점
                Point2D.Double O = pts.get(a[1]), P = pts.get(a[3]);
                double r = d(a, 2, 100), side = d(a, 4, 1);
                double dx = P.x - O.x, dy = P.y - O.y, dist = Math.max(1e-6, Math.hypot(dx, dy));
                double phi = Math.atan2(dy, dx);
                double alpha = dist <= r ? 0 : Math.acos(r / dist);
                double t = phi + (side >= 0 ? alpha : -alpha);
                pts.put(a[0], new Point2D.Double(O.x + r * Math.cos(t), O.y + r * Math.sin(t)));
                break;
            }
            case "apex": {
                Point2D.Double A = pts.get(a[1]), B = pts.get(a[2]);
                double base = dirDeg(A, B);
                double ra = Math.toRadians(base + d(a, 3, 60));
                double rb = Math.toRadians(base + 180 - d(a, 4, 60));
                // A + t(cos ra, -sin ra) = B + s(cos rb, -sin rb)
                double ax = Math.cos(ra), ay = -Math.sin(ra), bx = Math.cos(rb), by = -Math.sin(rb);
                double det = ax * (-by) - ay * (-bx);
                double t = ((B.x - A.x) * (-by) - (B.y - A.y) * (-bx)) / det;
                pts.put(a[0], new Point2D.Double(A.x + ax * t, A.y + ay * t));
                break;
            }
            default: break;
        }
    }

    static Color color(String name) {
        if (name == null) return CHALK;
        switch (name) {
            case "ink": return INK;
            case "red": return new Color(0xE0664C);
            case "gold": return new Color(0xE6BE55);
            case "chalk": return CHALK;
            case "cream": return new Color(0xF3E6C4);
            case "green": return BOARD_GREEN;
            case "tan": return new Color(0xB98E5A);
            case "blue": return new Color(0x8DB7DA);
            case "dim": return new Color(244, 236, 216, 140);
            default:
                if (name.startsWith("#")) return new Color(Integer.parseInt(name.substring(1), 16));
                return CHALK;
        }
    }

    private static Stroke stroke(String w) {
        if (w == null) w = "3";
        boolean dashed = w.endsWith("d");
        float width = Float.parseFloat(dashed ? w.substring(0, w.length() - 1) : w);
        if (dashed) {
            return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[]{width * 3f + 4, width * 2.5f + 4}, 0f);
        }
        return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    private static void text(Graphics2D g, String s, double cx, double cy, Color c, float size, boolean bold) {
        if (s == null || s.isEmpty()) return;
        float sz = size / Theme.scale(); // 칠판 글자는 글자 크기 설정과 무관하게 고정
        Font f = bold ? Theme.bold(sz) : Theme.serif(sz);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        float x = (float) (cx - Theme.width(fm, s) / 2.0);
        float y = (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0);
        g.setColor(c);
        Theme.drawString(g, s, x, y);
    }

    /**
     * 이름표 글자. 정답 공개 중이면 '?'를 답으로 바꿔 왼쪽부터 손글씨처럼 써 나간다.
     */
    private static void label(Graphics2D g, Ctx ctx, String s, double cx, double cy, Color c, float size, boolean bold) {
        if (s == null || s.isEmpty()) return;
        if (!ctx.revealing() || !s.contains("?")) {
            text(g, s, cx, cy, c, size, bold);
            return;
        }
        String shown = s.replace("?", ctx.answer);
        float sz = (size + 3) / Theme.scale();
        Font f = Theme.hand(sz * 1.25f);
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        double w = Theme.width(fm, shown);
        float x = (float) (cx - w / 2.0), y = (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0);
        // 빛나는 바탕
        float gl = ctx.glow();
        g.setColor(new Color(GLOW.getRed(), GLOW.getGreen(), GLOW.getBlue(), Math.round(70 * gl)));
        g.fill(new RoundRectangle2D.Double(x - 8, cy - fm.getAscent() / 2.0 - 6, w + 16, fm.getAscent() + 12, 14, 14));
        double write = Math.min(1, ctx.reveal * 1.8);
        Shape oldClip = g.getClip();
        g.clip(new Rectangle2D.Double(x - 4, cy - 60, (w + 8) * write, 120));
        g.setColor(GLOW);
        Theme.drawString(g, shown, x, y);
        g.setClip(oldClip);
    }

    private static String fmt(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.rint(r) ? String.valueOf((long) Math.rint(r)) : String.valueOf(r);
    }

    private static void draw(Graphics2D g, Cmd c, Ctx ctx) {
        String[] a = c.args;
        switch (c.op) {
            case "labelcolor": ctx.labelColor = color(a[0]); break;
            case "unit": break;
            case "seg": {
                Point2D.Double p = ctx.p(a[0]), q = ctx.p(a[1]);
                g.setColor(color(sArg(a, 2, "chalk")));
                g.setStroke(stroke(sArg(a, 3, "3")));
                ctx.stroke(g, new Line2D.Double(p, q));
                break;
            }
            case "line": case "ray": {
                Point2D.Double p = ctx.p(a[0]), q = ctx.p(a[1]);
                double ext = d(a, 4, 60);
                double len = Math.max(1e-6, p.distance(q)), ux = (q.x - p.x) / len, uy = (q.y - p.y) / len;
                Point2D.Double p2 = "ray".equals(c.op) ? p : new Point2D.Double(p.x - ux * ext, p.y - uy * ext);
                Point2D.Double q2 = new Point2D.Double(q.x + ux * ext, q.y + uy * ext);
                g.setColor(color(sArg(a, 2, "chalk")));
                g.setStroke(stroke(sArg(a, 3, "3")));
                ctx.stroke(g, new Line2D.Double(p2, q2));
                break;
            }
            case "arrow": {
                Point2D.Double p = ctx.p(a[0]), q = ctx.p(a[1]);
                Color col = color(sArg(a, 2, "chalk"));
                g.setColor(col);
                g.setStroke(stroke(sArg(a, 3, "3")));
                double len = Math.max(1e-6, p.distance(q)), ux = (q.x - p.x) / len, uy = (q.y - p.y) / len;
                ctx.stroke(g, new Line2D.Double(p.x, p.y, q.x - ux * 6, q.y - uy * 6));
                if (ctx.prog >= 0.95) {
                    Path2D head = new Path2D.Double();
                    head.moveTo(q.x, q.y);
                    head.lineTo(q.x - ux * 16 - uy * 8, q.y - uy * 16 + ux * 8);
                    head.lineTo(q.x - ux * 16 + uy * 8, q.y - uy * 16 - ux * 8);
                    head.closePath();
                    g.fill(head);
                }
                g.fill(new Ellipse2D.Double(p.x - 5, p.y - 5, 10, 10));
                break;
            }
            case "circle": {
                Point2D.Double o = ctx.p(a[0]);
                double r = d(a, 1, 100);
                g.setColor(color(sArg(a, 2, "chalk")));
                g.setStroke(stroke(sArg(a, 3, "3")));
                ctx.stroke(g, new Arc2D.Double(o.x - r, o.y - r, r * 2, r * 2, 90, 360, Arc2D.OPEN));
                break;
            }
            case "dial": {
                Point2D.Double o = ctx.p(a[0]);
                double r = d(a, 1, 170);
                g.setColor(new Color(0xF3E6C4));
                g.fill(new Ellipse2D.Double(o.x - r, o.y - r, r * 2, r * 2));
                g.setColor(new Color(0xD9B456));
                g.setStroke(new BasicStroke(9f));
                g.draw(new Ellipse2D.Double(o.x - r, o.y - r, r * 2, r * 2));
                g.setColor(new Color(0x8A5A34));
                g.setStroke(new BasicStroke(2f));
                double r2 = r - 13;
                g.draw(new Ellipse2D.Double(o.x - r2, o.y - r2, r2 * 2, r2 * 2));
                g.setColor(new Color(0xBE9561));
                for (int i = 0; i < 12; i++) {
                    Point2D.Double t = polar(o, r - 30, i * 30);
                    double dr = i % 3 == 0 ? 4.5 : 3.2;
                    g.fill(new Ellipse2D.Double(t.x - dr, t.y - dr, dr * 2, dr * 2));
                }
                break;
            }
            case "hand": {
                Point2D.Double o = ctx.p(a[0]);
                double deg = d(a, 1, 90);
                if (ctx.reveal >= 0 && ctx.reveal < 1) {
                    // 정답을 맞히면 바늘이 시계 방향으로 한 바퀴 돌아 제자리에 선다
                    double e = 1 - Math.pow(1 - Math.min(1, ctx.reveal * 1.3), 3);
                    deg = deg + 360 * (1 - e);
                }
                Point2D.Double t = polar(o, d(a, 2, 120), deg);
                g.setColor(color(sArg(a, 4, "ink")));
                g.setStroke(new BasicStroke((float) d(a, 3, 6), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Double(o, t));
                break;
            }
            case "hub": {
                Point2D.Double o = ctx.p(a[0]);
                g.setColor(new Color(0xA8382A));
                g.fill(new Ellipse2D.Double(o.x - 9, o.y - 9, 18, 18));
                g.setColor(INK);
                g.setStroke(new BasicStroke(2.5f));
                g.draw(new Ellipse2D.Double(o.x - 9, o.y - 9, 18, 18));
                break;
            }
            case "arc": {
                Point2D.Double o = ctx.p(a[0]);
                double r = d(a, 1, 100), from = d(a, 2, 0), to = d(a, 3, 90);
                g.setColor(color(sArg(a, 4, "chalk")));
                g.setStroke(stroke(sArg(a, 5, "3")));
                ctx.stroke(g, new Arc2D.Double(o.x - r, o.y - r, r * 2, r * 2, from, to - from, Arc2D.OPEN));
                break;
            }
            case "angle": {
                Point2D.Double A = ctx.p(a[0]), O = ctx.p(a[1]), B = ctx.p(a[2]);
                double r = d(a, 3, 40);
                String label = sArg(a, 4, "");
                Color col = color(sArg(a, 5, "gold"));
                double a1 = dirDeg(O, A), a2 = dirDeg(O, B);
                double diff = a2 - a1;
                while (diff > 180) diff -= 360;
                while (diff <= -180) diff += 360;
                g.setColor(col);
                g.setStroke(stroke(sArg(a, 6, "3")));
                ctx.stroke(g, new Arc2D.Double(O.x - r, O.y - r, r * 2, r * 2, a1, diff, Arc2D.OPEN));
                if (label.equals("@")) label = Math.round(angleAt(A, O, B)) + "°";
                if (!label.trim().isEmpty()) {
                    Point2D.Double lp = polar(O, r + 24, a1 + diff / 2);
                    label(g, ctx, label, lp.x, lp.y, col, 21f, true);
                }
                break;
            }
            case "right": {
                Point2D.Double A = ctx.p(a[0]), O = ctx.p(a[1]), B = ctx.p(a[2]);
                double sz = d(a, 4, 16);
                double la = Math.max(1e-6, O.distance(A)), lb = Math.max(1e-6, O.distance(B));
                double ux = (A.x - O.x) / la * sz, uy = (A.y - O.y) / la * sz;
                double vx = (B.x - O.x) / lb * sz, vy = (B.y - O.y) / lb * sz;
                Path2D p = new Path2D.Double();
                p.moveTo(O.x + ux, O.y + uy);
                p.lineTo(O.x + ux + vx, O.y + uy + vy);
                p.lineTo(O.x + vx, O.y + vy);
                g.setColor(color(sArg(a, 3, "chalk")));
                g.setStroke(new BasicStroke(2.2f));
                g.draw(p);
                break;
            }
            case "tick": {
                Point2D.Double p = ctx.p(a[0]), q = ctx.p(a[1]);
                int n = (int) d(a, 2, 1);
                double mx = (p.x + q.x) / 2, my = (p.y + q.y) / 2;
                double len = Math.max(1e-6, p.distance(q)), ux = (q.x - p.x) / len, uy = (q.y - p.y) / len;
                g.setColor(color(sArg(a, 3, "chalk")));
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < n; i++) {
                    double off = (i - (n - 1) / 2.0) * 7;
                    double cx = mx + ux * off, cy = my + uy * off;
                    g.draw(new Line2D.Double(cx - uy * 10, cy + ux * 10, cx + uy * 10, cy - ux * 10));
                }
                break;
            }
            case "parallel": {
                Point2D.Double p = ctx.p(a[0]), q = ctx.p(a[1]);
                int n = (int) d(a, 3, 1);
                double mx = (p.x + q.x) / 2, my = (p.y + q.y) / 2;
                double len = Math.max(1e-6, p.distance(q)), ux = (q.x - p.x) / len, uy = (q.y - p.y) / len;
                g.setColor(color(sArg(a, 2, "chalk")));
                g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < n; i++) {
                    double off = (i - (n - 1) / 2.0) * 9;
                    double tx = mx + ux * (off + 5), ty = my + uy * (off + 5);
                    Path2D ch = new Path2D.Double();
                    ch.moveTo(tx - ux * 10 - uy * 7, ty - uy * 10 + ux * 7);
                    ch.lineTo(tx, ty);
                    ch.lineTo(tx - ux * 10 + uy * 7, ty - uy * 10 - ux * 7);
                    g.draw(ch);
                }
                break;
            }
            case "len": {
                Point2D.Double p = ctx.p(a[0]), q = ctx.p(a[1]);
                String label = sArg(a, 2, "");
                double side = d(a, 3, 1);
                Color col = color(sArg(a, 4, "chalk"));
                double off = d(a, 5, 26);
                double len = Math.max(1e-6, p.distance(q));
                if (label.startsWith("@")) label = label.substring(1) + fmt(len / ctx.unitPx) + ctx.unitSuffix;
                double nx = -(q.y - p.y) / len, ny = (q.x - p.x) / len;
                double mx = (p.x + q.x) / 2 + nx * off * side, my = (p.y + q.y) / 2 + ny * off * side;
                label(g, ctx, label, mx, my, col, 21f, true);
                break;
            }
            case "tag": {
                Point2D.Double p = ctx.p(a[0]);
                label(g, ctx, sArg(a, 1, ""), p.x, p.y, color(sArg(a, 2, "chalk")), (float) d(a, 3, 20), true);
                break;
            }
            case "text": {
                label(g, ctx, sArg(a, 2, ""), d(a, 0, 0), d(a, 1, 0), color(sArg(a, 3, "chalk")), (float) d(a, 4, 18), false);
                break;
            }
            case "wedge": {
                // 부채꼴 칠하기(점으로): wedge O A B [색] [투명도] — A에서 B까지 반시계 방향
                Point2D.Double o = ctx.p(a[0]), A = ctx.p(a[1]), B = ctx.p(a[2]);
                double r = o.distance(A), from = dirDeg(o, A), to = dirDeg(o, B);
                double ext = to - from;
                while (ext < 0) ext += 360;
                while (ext >= 360) ext -= 360;
                Color col = color(sArg(a, 3, "gold"));
                float alpha = (float) d(a, 4, 0.3);
                g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), Math.round(alpha * 255)));
                g.fill(new Arc2D.Double(o.x - r, o.y - r, r * 2, r * 2, from, ext, Arc2D.PIE));
                break;
            }
            case "sector": {
                // 부채꼴 칠하기: sector O 반지름 시작° 끝° [색] [투명도]
                Point2D.Double o = ctx.p(a[0]);
                double r = d(a, 1, 100), from = d(a, 2, 0), to = d(a, 3, 90);
                Color col = color(sArg(a, 4, "gold"));
                float alpha = (float) d(a, 5, 0.3);
                g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), Math.round(alpha * 255)));
                g.fill(new Arc2D.Double(o.x - r, o.y - r, r * 2, r * 2, from, to - from, Arc2D.PIE));
                break;
            }
            case "fill": {
                Color col = color(a[0]);
                float alpha = (float) d(a, 1, 0.3);
                Path2D poly = new Path2D.Double();
                for (int i = 2; i < a.length; i++) {
                    Point2D.Double p = ctx.p(a[i]);
                    if (i == 2) poly.moveTo(p.x, p.y); else poly.lineTo(p.x, p.y);
                }
                poly.closePath();
                g.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), Math.round(alpha * 255)));
                g.fill(poly);
                break;
            }
            case "dot": {
                Point2D.Double p = ctx.p(a[0]);
                String dir = sArg(a, 1, "n");
                String style = sArg(a, 2, "hollow");
                Color lc = a.length > 3 ? color(a[3]) : ctx.labelColor;
                if ("hidden".equals(style)) break;
                if ("solid".equals(style)) {
                    g.setColor(INK);
                    g.fill(new Ellipse2D.Double(p.x - 6, p.y - 6, 12, 12));
                } else {
                    g.setColor(new Color(0xFFFBF1));
                    g.fill(new Ellipse2D.Double(p.x - 7.5, p.y - 7.5, 15, 15));
                    g.setColor(INK);
                    g.setStroke(new BasicStroke(2.6f));
                    g.draw(new Ellipse2D.Double(p.x - 7.5, p.y - 7.5, 15, 15));
                }
                if (!"none".equals(dir)) {
                    double ang;
                    switch (dir) {
                        case "e": ang = 0; break;
                        case "ne": ang = 45; break;
                        case "n": ang = 90; break;
                        case "nw": ang = 135; break;
                        case "w": ang = 180; break;
                        case "sw": ang = 225; break;
                        case "s": ang = 270; break;
                        default: ang = 315; break;
                    }
                    Point2D.Double lp = polar(p, 24, ang);
                    text(g, a[0].replaceAll("[0-9]+$", ""), lp.x, lp.y, lc, 23f, false);
                }
                break;
            }
            default: break;
        }
    }
}
