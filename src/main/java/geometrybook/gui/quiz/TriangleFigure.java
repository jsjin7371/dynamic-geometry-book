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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * 퀴즈 문제 옆에 그리는 삼각형 그림. questions.csv의 figure 칸을 읽어 그린다.
 * 형식: "angles:꼭대기,왼쪽,오른쪽" (모르는 각은 ?), 크기가 바뀌는 장면은 끝에 ";grow".
 *
 * 처음에는 변이 그려지고 모르는 각의 ?가 통통 튄다. 힌트를 누르면 학생이 직접 끌어서 확인한다.
 * - 보통 삼각형: 세 각 조각을 아래 선으로 끌어 놓으면 한 줄(180°)이 된다.
 * - grow: 오른쪽 꼭짓점의 노란 점을 끌어 크기를 바꿔도 각은 그대로다.
 * - 두 각의 합이 180° 이상: 노란 점을 끌어 올려도 두 변이 만나지 않는다.
 * 채점하면 남은 장면을 자동으로 끝내고 ?가 답으로 바뀌며, 맞히면 별이 터지고 틀리면 흔들린다.
 */
public final class TriangleFigure extends JComponent {
    private static final double W = 460, H = 340;
    private static final double GATHER_X = 230, GATHER_Y = 296;
    private static final Color[] FILL = {new Color(0xA99ADD), new Color(0xEFA34E), new Color(0x70BCA4)};
    private static final Color[] INK = {new Color(0x6B55B8), new Color(0xB86A12), new Color(0x2F8A6E)};
    private static final Color HANDLE = new Color(0xFFD54F);

    private final double[] angles = new double[3];   // 꼭대기, 왼쪽, 오른쪽 (도)
    private final boolean[] unknown = new boolean[3];
    private final boolean grow;
    private final boolean impossible;
    private final Timer timer;
    private final long born = System.currentTimeMillis();
    private long hintAt = -1;
    private long revealAt = -1;
    private long doneAt = -1;
    private boolean correct;
    private final List<double[]> stars = new ArrayList<>();

    /** 각 조각 (보통 삼각형의 끌기 장면). */
    private final Piece[] pieces = new Piece[3];
    private int dragging = -1;
    private double grabX, grabY;
    private double size = 1;      // grow: 노란 점으로 바꾸는 크기
    private double rise;          // 만나지 않는 장면: 노란 점으로 올리는 높이 (0~1)
    private boolean handleHeld;

    /** 마지막으로 그린 크기 맞춤 값. 마우스 좌표를 그림 좌표로 바꿀 때 쓴다. */
    private double scale = 1, offsetX, offsetY;

    /** figure 칸이 삼각형 그림이면 그림을, 아니면 null을 돌려준다. */
    public static TriangleFigure of(String figure) {
        if (figure == null || !figure.startsWith("angles:")) return null;
        String body = figure.substring("angles:".length());
        boolean grow = body.endsWith(";grow");
        if (grow) body = body.substring(0, body.length() - ";grow".length());
        String[] parts = body.split(",");
        return parts.length == 3 ? new TriangleFigure(parts, grow) : null;
    }

    private TriangleFigure(String[] parts, boolean grow) {
        this.grow = grow;
        double known = 0;
        int missing = 0;
        for (int i = 0; i < 3; i++) {
            unknown[i] = parts[i].trim().equals("?");
            if (unknown[i]) missing++;
            else known += angles[i] = Double.parseDouble(parts[i].trim());
        }
        impossible = known > 180 || (missing > 0 && known >= 180);
        for (int i = 0; i < 3; i++) {
            if (unknown[i]) angles[i] = impossible ? 0 : (180 - known) / missing;
        }
        if (!impossible) {
            // 한 줄로 모일 때 오른쪽·꼭대기·왼쪽 순서로 0°~180°를 채운다.
            double[] target = {angles[2], 180 - angles[1], 0};
            Point2D.Double[] p = vertices(1);
            for (int i = 0; i < 3; i++) pieces[i] = new Piece(p[i], p[(i + 1) % 3], p[(i + 2) % 3], target[i]);
        }
        setPreferredSize(new Dimension(460, 340));
        setMinimumSize(new Dimension(300, 220));
        setOpaque(false);
        timer = new Timer(30, e -> repaint());
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { press(logical(e)); }
            @Override public void mouseDragged(MouseEvent e) { drag(logical(e)); }
            @Override public void mouseReleased(MouseEvent e) { release(); }
            @Override public void mouseMoved(MouseEvent e) {
                setCursor(Cursor.getPredefinedCursor(grabbable(logical(e)) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    /** 힌트 장면을 시작한다. 이때부터 끌 수 있다. */
    public void showHint() {
        if (hintAt < 0) hintAt = System.currentTimeMillis();
    }

    /** 남은 장면을 자동으로 끝내고 모르는 각을 답으로 바꾼다. 맞히면 별을, 틀리면 흔들림을 보여 준다. */
    public void reveal(boolean correct) {
        if (revealAt >= 0) return;
        this.correct = correct;
        revealAt = System.currentTimeMillis();
        showHint();
        dragging = -1;
        handleHeld = false;
        if (!impossible && !grow) {
            for (Piece piece : pieces) if (!piece.snapped) piece.moveTo(true, revealAt);
        }
        if (correct) {
            Random random = new Random();
            for (int i = 0; i < 36; i++) {
                double direction = random.nextDouble() * Math.PI * 2, speed = 2 + random.nextDouble() * 5;
                stars.add(new double[]{Math.cos(direction) * speed, Math.sin(direction) * speed - 3, random.nextInt(4)});
            }
        }
    }

    @Override public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    // ---- 끌기 ----

    private boolean canDrag() {
        return hintAt >= 0 && revealAt < 0;
    }

    private boolean grabbable(Point2D.Double m) {
        if (!canDrag()) return false;
        if (impossible || grow) return handle().distance(m) < 30;
        return pieceAt(m) >= 0;
    }

    private int pieceAt(Point2D.Double m) {
        long now = System.currentTimeMillis();
        for (int i = 2; i >= 0; i--) {
            if (!pieces[i].snapped && pieces[i].center(now).distance(m) < 58) return i;
        }
        return -1;
    }

    private void press(Point2D.Double m) {
        if (!canDrag()) return;
        if (impossible || grow) {
            handleHeld = handle().distance(m) < 30;
            return;
        }
        dragging = pieceAt(m);
        if (dragging >= 0) {
            Point2D.Double c = pieces[dragging].center(System.currentTimeMillis());
            grabX = m.x - c.x;
            grabY = m.y - c.y;
        }
    }

    private void drag(Point2D.Double m) {
        if (!canDrag()) return;
        if (handleHeld && grow) {
            size = Math.max(0.5, Math.min(1.2, (m.x - 230) / (vertices(1)[2].x - 230)));
            if (Math.abs(size - 1) > 0.15 && doneAt < 0) doneAt = System.currentTimeMillis();
        } else if (handleHeld) {
            rise = Math.max(0, Math.min(1, (110 - m.y) / 90));
            if (rise > 0.97 && doneAt < 0) doneAt = System.currentTimeMillis();
        } else if (dragging >= 0) {
            pieces[dragging].hold(m.x - grabX, m.y - grabY);
        }
    }

    private void release() {
        handleHeld = false;
        if (dragging < 0) return;
        Piece piece = pieces[dragging];
        long now = System.currentTimeMillis();
        boolean close = piece.center(now).distance(GATHER_X, GATHER_Y) < 80;
        piece.moveTo(close, now);
        dragging = -1;
        boolean all = true;
        for (Piece p : pieces) all &= p.snapped;
        if (all && doneAt < 0) doneAt = now + 250;
    }

    private Point2D.Double handle() {
        if (grow) return vertices(size)[2];
        return new Point2D.Double(230, 110 - 90 * rise);
    }

    private Point2D.Double logical(MouseEvent e) {
        return new Point2D.Double((e.getX() - offsetX) / scale, (e.getY() - offsetY) / scale);
    }

    // ---- 그리기 ----

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            scale = Math.min(getWidth() / W, getHeight() / H);
            offsetX = (getWidth() - W * scale) / 2;
            offsetY = (getHeight() - H * scale) / 2;
            g.translate(offsetX, offsetY);
            g.scale(scale, scale);
            long now = System.currentTimeMillis();
            if (revealAt >= 0 && !correct) {
                double p = Math.min(1, (now - revealAt) / 450.0);
                g.translate(Math.sin((now - revealAt) / 28.0) * 9 * (1 - p), 0);
            }
            if (impossible) drawNeverMeet(g, now);
            else drawTriangle(g, now);
            drawStars(g, now);
        } finally {
            g.dispose();
        }
    }

    /** 꼭대기·왼쪽·오른쪽 꼭짓점을 그림 영역에 맞춘다. factor로 크기를 바꾼다 (가운데 기준). */
    private Point2D.Double[] vertices(double factor) {
        double b = Math.toRadians(angles[1]), c = Math.toRadians(angles[2]);
        double k = Math.sin(c) / Math.sin(b + c);
        double ax = Math.cos(b) * k, ay = -Math.sin(b) * k;   // 밑변 (0,0)-(1,0) 기준
        double minX = Math.min(0, ax), maxX = Math.max(1, ax);
        double fit = Math.min(360 / (maxX - minX), 200 / -ay) * factor;
        double left = 230 - (maxX - minX) * fit / 2 - minX * fit, base = 130 + (-ay) * fit / 2;
        return new Point2D.Double[]{
                new Point2D.Double(left + ax * fit, base + ay * fit),
                new Point2D.Double(left, base),
                new Point2D.Double(left + fit, base)};
    }

    /** grow에서 끌지 않았으면 채점 후 저절로 커졌다 작아진다. */
    private double currentSize(long now) {
        if (!grow) return 1;
        if (revealAt >= 0 && doneAt < 0) return 0.8 + 0.2 * Math.cos((now - revealAt) / 600.0);
        return size;
    }

    private void drawTriangle(Graphics2D g, long now) {
        double factor = currentSize(now);
        Point2D.Double[] p = vertices(factor);
        double drawIn = Math.min(1, (now - born) / 700.0);
        Path2D shape = new Path2D.Double();
        shape.moveTo(p[0].x, p[0].y);
        shape.lineTo(p[1].x, p[1].y);
        shape.lineTo(p[2].x, p[2].y);
        shape.closePath();
        g.setColor(new Color(204, 231, 218, (int) (255 * drawIn)));
        g.fill(shape);
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.GREEN);
        for (int i = 0; i < 3; i++) {
            Point2D.Double from = p[i], to = p[(i + 1) % 3];
            g.draw(new Line2D.Double(from.x, from.y, from.x + (to.x - from.x) * drawIn, from.y + (to.y - from.y) * drawIn));
        }

        boolean gathering = hintAt >= 0 && !grow;
        if (gathering) {
            // 조각이 놓일 선. 다 모이면 진한 선이 된다.
            g.setColor(doneAt >= 0 && now >= doneAt ? Theme.GREEN : new Color(0xB9CCC2));
            g.setStroke(doneAt >= 0 && now >= doneAt ? new BasicStroke(4)
                    : new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{10, 9}, 0));
            g.draw(new Line2D.Double(120, GATHER_Y, 340, GATHER_Y));
        }
        for (int i = 0; i < 3; i++) {
            Piece piece = pieces[i];
            double r = 40 * factor;
            if (gathering) {
                // 빠진 자리는 점선으로 남겨 둔다.
                g.setColor(withAlpha(FILL[i], 140));
                g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 0));
                g.draw(new Arc2D.Double(piece.homeX - 40, piece.homeY - 40, 80, 80, piece.homeAngle, piece.extent, Arc2D.PIE));
                Point2D.Double c = piece.center(now);
                double angle = piece.angle(now);
                double wiggle = !piece.snapped && i != dragging && revealAt < 0 ? 4 * Math.sin((now - hintAt) / 180.0 + i) : 0;
                double radius = i == dragging ? 52 : piece.snapped ? 56 : 46;
                g.setColor(withAlpha(FILL[i], i == dragging ? 240 : 220));
                g.fill(new Arc2D.Double(c.x - radius, c.y - radius, radius * 2, radius * 2, angle + wiggle, piece.extent, Arc2D.PIE));
                g.setColor(INK[i]);
                g.setStroke(new BasicStroke(2));
                g.draw(new Arc2D.Double(c.x - radius, c.y - radius, radius * 2, radius * 2, angle + wiggle, piece.extent, Arc2D.PIE));
            } else {
                Point2D.Double v = p[i];
                g.setColor(withAlpha(FILL[i], 210));
                g.fill(new Arc2D.Double(v.x - r, v.y - r, r * 2, r * 2, piece.homeAngle, piece.extent, Arc2D.PIE));
            }
            drawLabel(g, i, p[i], piece.homeAngle + piece.extent / 2, 68 * factor, now);
        }

        g.setFont(Theme.font(Font.BOLD, 22));
        if (gathering && doneAt >= 0 && now >= doneAt) {
            g.setColor(Theme.GREEN);
            centered(g, label(1) + " + " + label(0) + " + " + label(2) + " = 180°", 230, 334);
        } else if (gathering && revealAt < 0) {
            g.setColor(Theme.MUTED);
            g.setFont(Theme.font(Font.BOLD, 18));
            centered(g, "색깔 조각 3개를 점선 위로 끌어 놓아요!", 230, 334);
        }
        if (grow && hintAt >= 0) {
            boolean changed = doneAt >= 0 || revealAt >= 0;
            g.setColor(changed ? Theme.GREEN : Theme.MUTED);
            g.setFont(Theme.font(Font.BOLD, changed ? 22 : 18));
            centered(g, changed ? "크기가 바뀌어도 각은 그대로!" : "노란 점을 끌어서 크기를 바꿔 봐요!", 230, 320);
            if (revealAt < 0) drawHandle(g, p[2], now, "↔");
        }
    }

    /** 두 각의 합이 180° 이상이면 두 변이 나란히 올라가 만나지 않는 장면. */
    private void drawNeverMeet(Graphics2D g, long now) {
        double drawIn = Math.min(1, (now - born) / 700.0);
        double shown = revealAt >= 0 ? Math.max(rise, ease(Math.min(1, (now - revealAt) / 900.0))) : rise;
        double top = 110 - 90 * shown;
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Theme.GREEN);
        g.draw(new Line2D.Double(110, 250, 110 + 240 * drawIn, 250));
        g.draw(new Line2D.Double(110, 250, 110, 250 - (250 - top) * drawIn));
        g.draw(new Line2D.Double(350, 250, 350, 250 - (250 - top) * drawIn));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{8, 10}, 0));
        g.draw(new Line2D.Double(110, top - 6, 110, top - 40));
        g.draw(new Line2D.Double(350, top - 6, 350, top - 40));
        for (int i = 1; i < 3; i++) {
            double x = i == 1 ? 110 : 322;
            g.setColor(withAlpha(FILL[i], 200));
            g.fill(new Rectangle2D.Double(x, 222, 28, 28));
            g.setColor(INK[i]);
            g.setFont(Theme.font(Font.BOLD, 22));
            centered(g, label(i), i == 1 ? 152 : 308, 240);
        }
        drawLabel(g, 0, new Point2D.Double(230, 170), Double.NaN, 0, now);
        if (shown >= 0.97) {
            g.setColor(new Color(0xC2453D));
            g.setFont(Theme.font(Font.BOLD, 24));
            centered(g, "두 변이 만나지 않아요!", 230, 300);
        } else if (hintAt >= 0 && revealAt < 0) {
            g.setColor(Theme.MUTED);
            g.setFont(Theme.font(Font.BOLD, 18));
            centered(g, "노란 점을 위로 끌어 올려 봐요!", 230, 300);
            drawHandle(g, new Point2D.Double(230, top), now, "↑");
        }
    }

    /** 끌 수 있는 노란 점. 살짝 숨 쉬듯 커졌다 작아진다. */
    private void drawHandle(Graphics2D g, Point2D.Double at, long now, String arrow) {
        double r = (handleHeld ? 20 : 17) + 2 * Math.sin((now - hintAt) / 200.0);
        g.setColor(withAlpha(HANDLE, 90));
        g.fill(new Ellipse2D.Double(at.x - r - 8, at.y - r - 8, (r + 8) * 2, (r + 8) * 2));
        g.setColor(HANDLE);
        g.fill(new Ellipse2D.Double(at.x - r, at.y - r, r * 2, r * 2));
        g.setColor(new Color(0xC79100));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Ellipse2D.Double(at.x - r, at.y - r, r * 2, r * 2));
        g.setColor(Theme.TEXT);
        g.setFont(Theme.font(Font.BOLD, 18));
        centered(g, arrow, at.x, at.y + 6);
    }

    /** 각 이름표. 모르는 각은 채점 전까지 통통 튀는 노란 ?, 채점 후에는 답이 튀어나온다. */
    private void drawLabel(Graphics2D g, int i, Point2D.Double vertex, double middle, double distance, long now) {
        double x = vertex.x, y = vertex.y;
        if (!Double.isNaN(middle)) {
            x += Math.cos(Math.toRadians(middle)) * distance;
            y -= Math.sin(Math.toRadians(middle)) * distance;
        }
        if (unknown[i] && revealAt < 0) {
            double pulse = 1 + 0.12 * Math.sin((now - born) / 160.0);
            AffineTransform saved = g.getTransform();
            g.translate(x, y);
            g.scale(pulse, pulse);
            g.setColor(Theme.YELLOW);
            g.fill(new Ellipse2D.Double(-19, -19, 38, 38));
            g.setColor(new Color(0xD9A400));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Ellipse2D.Double(-19, -19, 38, 38));
            g.setColor(Theme.TEXT);
            g.setFont(Theme.font(Font.BOLD, 24));
            centered(g, "?", 0, 9);
            g.setTransform(saved);
            return;
        }
        double pop = unknown[i] ? 1 + 0.5 * Math.max(0, 1 - (now - revealAt) / 400.0) : 1;
        AffineTransform saved = g.getTransform();
        g.translate(x, y);
        g.scale(pop, pop);
        g.setColor(unknown[i] ? Theme.GREEN : INK[i]);
        g.setFont(Theme.font(Font.BOLD, unknown[i] ? 28 : 23));
        centered(g, label(i), 0, 8);
        g.setTransform(saved);
    }

    private void drawStars(Graphics2D g, long now) {
        if (stars.isEmpty()) return;
        double t = (now - revealAt) / 30.0;
        if (t > 50) return;
        Color[] colors = {new Color(0xF2B705), INK[0], new Color(0xF08A24), INK[2]};
        for (double[] s : stars) {
            double x = 230 + s[0] * t, y = 150 + s[1] * t + 0.12 * t * t;
            g.setColor(withAlpha(colors[(int) s[2]], (int) (255 * Math.max(0, 1 - t / 50))));
            g.fill(star(x, y, 11));
        }
    }

    private String label(int i) {
        if (unknown[i] && revealAt < 0) return "?";
        return Math.round(angles[i]) + "°";
    }

    /**
     * 끌 수 있는 각 조각 하나. 제자리(꼭짓점)와 모이는 자리(선 위) 사이를 움직인다.
     * 놓으면 250ms 동안 미끄러지듯 자리를 찾아간다.
     */
    private static final class Piece {
        final double homeX, homeY, homeAngle, extent, targetAngle;
        double fromX, fromY, fromAngle, toX, toY, toAngle;
        long movedAt;
        boolean snapped;

        Piece(Point2D.Double vertex, Point2D.Double a, Point2D.Double b, double targetAngle) {
            double start = direction(vertex, a), extent = signedDelta(start, direction(vertex, b));
            if (extent < 0) {
                start += extent;
                extent = -extent;
            }
            homeX = vertex.x;
            homeY = vertex.y;
            homeAngle = start;
            this.extent = extent;
            this.targetAngle = start + signedDelta(start, targetAngle);
            fromX = toX = homeX;
            fromY = toY = homeY;
            fromAngle = toAngle = homeAngle;
        }

        double progress(long now) {
            return ease(Math.min(1, (now - movedAt) / 250.0));
        }

        Point2D.Double center(long now) {
            double t = progress(now);
            return new Point2D.Double(fromX + (toX - fromX) * t, fromY + (toY - fromY) * t);
        }

        double angle(long now) {
            return fromAngle + (toAngle - fromAngle) * progress(now);
        }

        /** 마우스로 잡고 있는 동안은 그대로 따라온다. */
        void hold(double x, double y) {
            fromX = toX = x;
            fromY = toY = y;
            fromAngle = toAngle = homeAngle;
        }

        /** 선 위(true)나 제자리(false)로 미끄러지듯 보낸다. */
        void moveTo(boolean gather, long now) {
            Point2D.Double c = center(now);
            fromX = c.x;
            fromY = c.y;
            fromAngle = angle(now);
            toX = gather ? GATHER_X : homeX;
            toY = gather ? GATHER_Y : homeY;
            toAngle = gather ? targetAngle : homeAngle;
            movedAt = now;
            snapped = gather;
        }
    }

    private static Path2D star(double cx, double cy, double r) {
        Path2D path = new Path2D.Double();
        for (int k = 0; k < 10; k++) {
            double radius = k % 2 == 0 ? r : r * 0.45, angle = Math.PI / 5 * k - Math.PI / 2;
            if (k == 0) path.moveTo(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
            else path.lineTo(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
        }
        path.closePath();
        return path;
    }

    /** 화면 좌표의 방향을 Arc2D 각도(동쪽 0°, 반시계 +)로 바꾼다. */
    private static double direction(Point2D.Double from, Point2D.Double to) {
        return Math.toDegrees(Math.atan2(-(to.y - from.y), to.x - from.x));
    }

    private static double signedDelta(double from, double to) {
        double d = (to - from) % 360;
        if (d > 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    private static double ease(double t) {
        t = Math.max(0, t);
        return t * t * (3 - 2 * t);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static void centered(Graphics2D g, String text, double cx, double baseline) {
        g.drawString(text, (float) (cx - g.getFontMetrics().stringWidth(text) / 2.0), (float) baseline);
    }
}
