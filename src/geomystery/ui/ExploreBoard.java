package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.diagram.DiagramView;
import geomystery.game.Expr;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 정리 탐구: 점을 끌어 도형을 바꿔도 정리가 늘 성립하는 것을 실시간 측정값으로 보여 준다.
 * 수수께끼의 답과 섞이지 않도록 정답을 맞힌 뒤나 도형 도감에서만 연다.
 */
public class ExploreBoard extends JComponent {
    private static final double MIN_POINT_DISTANCE = 26;
    private final List<Cmd> script;
    private final Explore spec;
    private final Map<String, Point2D.Double> overrides = new HashMap<>();
    private final Map<String, Point2D.Double> start;
    private final Widgets.TextBlock relationView;
    private String dragging, hover;

    public ExploreBoard(List<Cmd> script, Explore spec, Widgets.TextBlock relationView) {
        this.script = script;
        this.spec = spec;
        this.relationView = relationView;
        this.start = DiagramView.points(script);
        setPreferredSize(new Dimension(700, 482));
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                String h = hit(toLogical(e.getPoint()));
                if (!Objects.equals(h, hover)) { hover = h; repaint(); }
                setCursor(Cursor.getPredefinedCursor(h != null ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
            @Override public void mousePressed(MouseEvent e) {
                dragging = hit(toLogical(e.getPoint()));
                if (dragging != null) Sfx.play("select");
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragging == null) return;
                move(dragging, toLogical(e.getPoint()));
            }
            @Override public void mouseReleased(MouseEvent e) { dragging = null; repaint(); }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        setOpaque(true);
        updateRelation();
    }

    public void reset() {
        overrides.clear();
        updateRelation();
        repaint();
    }

    /** (예전의 반짝임 타이머 자리) 정리할 자원이 없지만 호출하는 쪽과의 호환을 위해 남겨 둔다. */
    public void stop() { }

    private Map<String, Point2D.Double> pts() { return DiagramView.points(script, overrides); }

    private Drag dragOf(String name) {
        for (Drag d : spec.drags) if (d.point.equals(name)) return d;
        return null;
    }

    private String hit(Point2D.Double p) {
        Map<String, Point2D.Double> pts = pts();
        String best = null;
        double bd = 20;
        for (Drag d : spec.drags) {
            Point2D.Double q = pts.get(d.point);
            if (q != null && q.distance(p) < bd) { bd = q.distance(p); best = d.point; }
        }
        return best;
    }

    /** 제약에 맞춰 점을 옮긴다. 도형이 망가지는 위치(점이 겹치거나 계산 불가)는 받아들이지 않는다. */
    private void move(String name, Point2D.Double want) {
        Drag d = dragOf(name);
        Map<String, Point2D.Double> pts = pts();
        Point2D.Double cur = pts.get(name), orig = start.get(name);
        if (d == null || cur == null) return;
        double x = want.x, y = want.y;
        switch (d.kind) {
            case "hline": y = orig.y; break;
            case "vline": x = orig.x; break;
            case "circle": case "arc": {
                Point2D.Double o = pts.get(d.center);
                double r = orig.distance(start.get(d.center));
                double ang = Math.toDegrees(Math.atan2(-(y - o.y), x - o.x));
                if ("arc".equals(d.kind) && d.args.length == 2) ang = clampArc(ang, d.args[0], d.args[1]);
                x = o.x + r * Math.cos(Math.toRadians(ang));
                y = o.y - r * Math.sin(Math.toRadians(ang));
                break;
            }
            default: break;
        }
        x = Math.max(24, Math.min(DiagramView.W - 24, x));
        y = Math.max(24, Math.min(DiagramView.H - 24, y));
        if (d.snap > 0 && !"circle".equals(d.kind) && !"arc".equals(d.kind)) {
            // 격자에 맞춘 뒤 칠판 밖으로 나가면 한 칸씩 안으로 들인다(격자는 유지)
            x = orig.x + Math.round((x - orig.x) / d.snap) * d.snap;
            y = orig.y + Math.round((y - orig.y) / d.snap) * d.snap;
            while (x < 24) x += d.snap;
            while (x > DiagramView.W - 24) x -= d.snap;
            while (y < 24) y += d.snap;
            while (y > DiagramView.H - 24) y -= d.snap;
        }
        Point2D.Double np = new Point2D.Double(x, y);
        // 접선 스크립트가 참조하는 외부 점만, 정의된 원의 바깥으로 이동을 제한한다.
        for (Cmd cmd : script) {
            if (!"tan".equals(cmd.op) || !name.equals(cmd.args[3])) continue;
            Point2D.Double o = pts.get(cmd.args[1]);
            // 기존 점 겹침 방지 거리를 여유로 두어 접점이 겹치는 것도 막는다.
            double minDistance = Double.parseDouble(cmd.args[2]) + MIN_POINT_DISTANCE;
            double distance = o.distance(np);
            if (distance >= minDistance) continue;
            if (distance == 0) {
                // 중심에서는 방향을 정할 수 없으므로 직전 위치의 방향을 사용한다.
                np.setLocation(cur);
                distance = o.distance(np);
            }
            // 중심에서 마우스를 향하는 방향은 유지하고 최소 거리까지만 접근시킨다.
            np.setLocation(o.x + (np.x - o.x) * minDistance / distance,
                    o.y + (np.y - o.y) * minDistance / distance);
        }
        Map<String, Point2D.Double> trial = new HashMap<>(overrides);
        trial.put(name, np);
        Map<String, Point2D.Double> after = DiagramView.points(script, trial);
        for (Map.Entry<String, Point2D.Double> e : after.entrySet()) {
            Point2D.Double q = e.getValue();
            if (Double.isNaN(q.x) || Double.isNaN(q.y) || Math.abs(q.x) > 4000 || Math.abs(q.y) > 4000) return;
            if (!e.getKey().equals(name) && q.distance(np) < MIN_POINT_DISTANCE) return;
        }
        overrides.put(name, np);
        updateRelation();
        repaint();
    }

    private static double clampArc(double ang, double from, double to) {
        double a = norm(ang), f = norm(from), t = norm(to);
        boolean inside = f <= t ? a >= f && a <= t : a >= f || a <= t;
        if (inside) return a;
        return angDist(a, f) < angDist(a, t) ? f : t;
    }

    private static double norm(double a) { a %= 360; return a < 0 ? a + 360 : a; }

    private static double angDist(double a, double b) {
        double d = Math.abs(norm(a) - norm(b));
        return Math.min(d, 360 - d);
    }

    // ------------------------------------------------------------------ 관계식
    private static final Pattern BLOCK = Pattern.compile("\\{([^}]*)\\}");
    private static final Pattern ANG = Pattern.compile("ang\\(\\s*(\\w+)\\s*,\\s*(\\w+)\\s*,\\s*(\\w+)\\s*\\)");
    private static final Pattern LEN = Pattern.compile("len\\(\\s*(\\w+)\\s*,\\s*(\\w+)\\s*\\)");

    private void updateRelation() {
        if (relationView != null) relationView.setTextQuick(relationText());
    }

    /** relation 문구를 지금 도형으로 계산한 글. */
    public String relationText() {
        if (spec.relation == null) return "";
        Map<String, Point2D.Double> pts = pts();
        double unit = (Double) DiagramView.unitOf(script)[0];
        StringBuilder out = new StringBuilder();
        String[] lines = spec.relation.split("\\|", -1);
        for (int li = 0; li < lines.length; li++) {
            if (li > 0) out.append('\n');
            out.append(evalLine(lines[li], pts, unit));
        }
        return out.toString();
    }

    private static final Pattern SINGLE = Pattern.compile("^(ang|len)\\([^)]*\\)$");

    /**
     * 한 줄의 {식}들을 계산한다. 같은 줄에 '항들의 합 = 합계' 꼴이 있으면 항을 따로 반올림해 합이 어긋나지 않도록
     * (예: 66.7 + 38 + 75.4 = 180.1) 최대 잉여 반올림으로 항들의 표시값을 합계에 맞춘다.
     */
    private static String evalLine(String line, Map<String, Point2D.Double> pts, double unit) {
        Matcher m = BLOCK.matcher(line);
        List<String> exprs = new ArrayList<>();
        List<int[]> spans = new ArrayList<>();
        while (m.find()) {
            exprs.add(m.group(1).trim());
            spans.add(new int[]{m.start(), m.end()});
        }
        int n = exprs.size();
        double[] val = new double[n];
        boolean[] kind = new boolean[n], single = new boolean[n], bad = new boolean[n];
        String[] shown = new String[n];
        for (int i = 0; i < n; i++) {
            String e = exprs.get(i);
            kind[i] = e.startsWith("kind:");
            if (kind[i]) e = e.substring(5);
            single[i] = !kind[i] && SINGLE.matcher(e).matches();
            try {
                val[i] = Expr.eval(substitute(e, pts, unit), 0);
            } catch (RuntimeException ex) {
                bad[i] = true;
            }
        }
        // 합계 블록: 같은 줄의 단일 항들의 합과 같은 값
        List<Integer> singles = new ArrayList<>();
        double sum = 0;
        for (int i = 0; i < n; i++) if (single[i] && !bad[i]) { singles.add(i); sum += val[i]; }
        int total = -1;
        for (int i = 0; i < n; i++) {
            if (single[i] || kind[i] || bad[i] || singles.size() < 2) continue;
            if (Math.abs(val[i] - sum) < 1e-6 * Math.max(1, Math.abs(sum))) { total = i; break; }
        }
        if (total >= 0) {
            long target = Math.round(val[total] * 10);
            long[] tenths = new long[singles.size()];
            Integer[] order = new Integer[singles.size()];
            long acc = 0;
            for (int k = 0; k < singles.size(); k++) {
                tenths[k] = (long) Math.floor(val[singles.get(k)] * 10 + 1e-9);
                acc += tenths[k];
                order[k] = k;
            }
            long need = target - acc;
            Arrays.sort(order, (x, y) -> Double.compare(frac(val[singles.get(y)]), frac(val[singles.get(x)])));
            for (int k = 0; k < need && k < order.length; k++) tenths[order[k]]++;
            if (need >= 0 && need <= order.length) {
                for (int k = 0; k < singles.size(); k++) shown[singles.get(k)] = fmt(tenths[k] / 10.0);
            }
        }
        StringBuilder sb = new StringBuilder();
        int last = 0;
        for (int i = 0; i < n; i++) {
            sb.append(line, last, spans.get(i)[0]);
            String o;
            if (bad[i]) o = "?";
            else if (kind[i]) o = kindOf(val[i]);
            else o = shown[i] != null ? shown[i] : fmt(val[i]);
            sb.append(o);
            last = spans.get(i)[1];
        }
        sb.append(line.substring(last));
        return sb.toString();
    }

    /** 지수 표기(1.0E-5) 없이 식에 넣을 수 있는 숫자. */
    private static String plain(double v) { return String.format(java.util.Locale.ROOT, "%.10f", v); }

    private static double frac(double v) {
        double t = v * 10;
        return t - Math.floor(t + 1e-9);
    }

    private static String substitute(String expr, Map<String, Point2D.Double> pts, double unit) {
        Matcher a = ANG.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (a.find()) {
            double v = DiagramView.angleAt(pts.get(a.group(1)), pts.get(a.group(2)), pts.get(a.group(3)));
            a.appendReplacement(sb, "(" + plain(v) + ")");
        }
        a.appendTail(sb);
        Matcher l = LEN.matcher(sb.toString());
        sb = new StringBuffer();
        while (l.find()) {
            double v = pts.get(l.group(1)).distance(pts.get(l.group(2))) / unit;
            l.appendReplacement(sb, "(" + plain(v) + ")");
        }
        l.appendTail(sb);
        return sb.toString();
    }

    private static String kindOf(double deg) {
        if (Math.abs(deg - 90) < 0.6) return "직각";
        if (Math.abs(deg - 180) < 0.6) return "평각";
        if (deg < 90) return "예각";
        return "둔각";
    }

    static String fmt(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.rint(r) ? String.valueOf((long) Math.rint(r)) : String.valueOf(r);
    }

    // ------------------------------------------------------------------ 그리기
    private double scale() { return Math.min((getWidth() - 20) / DiagramView.W, (getHeight() - 20) / DiagramView.H); }
    private double ox() { return (getWidth() - DiagramView.W * scale()) / 2; }
    private double oy() { return (getHeight() - DiagramView.H * scale()) / 2; }

    private Point2D.Double toLogical(Point p) {
        return new Point2D.Double((p.x - ox()) / scale(), (p.y - oy()) / scale());
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.aa(g0.create());
        int w = getWidth(), h = getHeight();
        drawBackground(g, w, h);
        g.translate(ox(), oy());
        g.scale(scale(), scale());
        DiagramView.Opts o = new DiagramView.Opts();
        o.overrides = overrides;
        try {
            DiagramView.render(g, script, o);
        } catch (RuntimeException ignored) {
            // 망가진 배치는 move()에서 거르므로 여기 오지 않는다
        }
        Map<String, Point2D.Double> pts = pts();
        for (Drag d : spec.drags) {
            Point2D.Double p = pts.get(d.point);
            if (p == null) continue;
            boolean active = d.point.equals(dragging) || d.point.equals(hover);
            double r = active ? 18 : 15;
            g.setColor(new Color(255, 214, 102, active ? 150 : 90));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
            if (active) {
                g.setColor(new Color(255, 214, 102, 50));
                g.fill(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
            }
        }
        g.dispose();
    }

    /** 변하지 않는 칠판 바탕(종이·초록 판·금테)은 한 번만 그려 두고 복사한다(끌 때마다 다시 그리면 한 프레임의 절반이 든다). */
    private java.awt.image.BufferedImage bgCache;
    private double bgScale;

    private void drawBackground(Graphics2D g, int w, int h) {
        double sc = Math.max(1, g.getTransform().getScaleX());
        if (bgCache == null || bgCache.getWidth() != (int) Math.ceil(w * sc) || bgCache.getHeight() != (int) Math.ceil(h * sc) || bgScale != sc) {
            bgScale = sc;
            bgCache = new java.awt.image.BufferedImage((int) Math.ceil(w * sc), (int) Math.ceil(h * sc), java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D b = Theme.aa(bgCache.createGraphics());
            b.scale(sc, sc);
            b.setColor(Theme.PAPER);
            b.fillRect(0, 0, w, h);
            b.setColor(new Color(0x2F5E50));
            b.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 18, 18));
            b.setColor(new Color(0xD9B456));
            b.setStroke(new BasicStroke(3f));
            b.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 3, h - 3, 18, 18));
            b.dispose();
        }
        Graphics2D c = (Graphics2D) g.create();
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        c.drawImage(bgCache, 0, 0, w, h, null);
        c.dispose();
    }

    // ------------------------------------------------------------------ 창
    /** 정리 하나의 탐구 창을 연다. 설정이 없으면 false. */
    public static boolean open(MainFrame frame, Theorem t) {
        if (t == null) return false;
        Explore spec = frame.game.content.explores.get(t.id);
        if (spec == null) return false;
        List<Cmd> script = frame.game.content.diagrams.get(spec.diagram);
        if (script == null) return false;
        Ui.Modal m = Ui.solidModal(frame, "정리 탐구 · " + t.name, 780);
        m.text(spec.caption == null || spec.caption.isEmpty() ? "반짝이는 점을 끌어 보세요." : spec.caption + "  (반짝이는 점을 끌어 보세요)");
        m.add((JComponent) Ui.vgap(10));
        Widgets.TextBlock rel = new Widgets.TextBlock("", Theme.bold(16), Theme.INK).spacing(1.6f).prefWidth(700);
        ExploreBoard board = new ExploreBoard(script, spec, rel);
        m.add(board);
        m.add((JComponent) Ui.vgap(12));
        RoundPanel rp = new RoundPanel(new BorderLayout(), Theme.c(0xFBF3E1), Theme.GOLD_DARK, 12).borderWidth(1.8f);
        rp.pad(12, 16, 12, 16);
        rp.add(rel, BorderLayout.CENTER);
        m.add(rp);
        m.add((JComponent) Ui.vgap(6));
        m.add(Ui.label("측정값은 소수 첫째 자리까지 반올림해 보여 줍니다.", Theme.serif(12), Theme.MUTED));
        Btn reset = new Btn("처음 모양으로", Btn.Style.OUTLINE, "refresh");
        reset.addActionListener(e -> board.reset());
        m.buttons.add(reset);
        m.button("닫기", Btn.Style.PRIMARY, board::stop);
        m.dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { board.stop(); }
        });
        m.show();
        return true;
    }
}
