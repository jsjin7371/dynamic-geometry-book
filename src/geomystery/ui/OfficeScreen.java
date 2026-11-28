package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * 탐정 사무소: 헤이즐의 방. 모은 것들이 방 안에 진열된다.
 * <ul>
 *   <li>창밖 — 지금 수사 중인 장의 풍경</li>
 *   <li>훈장 진열장 — 얻은 훈장은 금빛, 아직 못 얻은 것은 먼지 쌓인 빈자리</li>
 *   <li>증거품 유리 돔 — 범인을 밝힌 사건의 증거품(누르면 자백 만화 다시 보기)</li>
 *   <li>정리 카드 액자 — 모은 정리 카드(누르면 크게 보기)</li>
 *   <li>인물 수첩 — 만난 인물과 호감도 하트, 가득 차면 후일담(누르면 대화)</li>
 * </ul>
 * 방 전체는 백그라운드 스레드에서 한 장으로 그려 두고(캐시), 화면에는 그 그림을 복사만 한다.
 * 살아 있는 부분(헤이즐 눈 깜빡임, 마우스를 올린 곳 강조)만 그 영역을 다시 그린다.
 */
public class OfficeScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private final Header header;
    private final Room room;

    public OfficeScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        header = new Header(game);
        header.setTitle("DETECTIVE OFFICE", "탐정 사무소");
        add(header, BorderLayout.NORTH);
        room = new Room();
        JScrollPane sp = Ui.scroll(room);
        sp.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(sp, BorderLayout.CENTER);
    }

    public void refresh() {
        header.refresh();
        room.revalidate();
        room.repaint();
    }

    /** 시작 직후 미리 그려 둔다. */
    public void prewarm() {
        if (room.getWidth() <= 0) return;
        double sc = Ui.screenScale(this);
        room.request(room.getWidth(), room.getHeight(), sc);
    }

    /** 방의 상태(진행·테마·옷차림)가 바뀌었는지 알아보는 표식. */
    private String signature() {
        StringBuilder sb = new StringBuilder(Theme.isDark() ? "d" : "l");
        sb.append(game.lessonsDone()).append(game.save.solvedFinales).append(game.save.epiloguesSeen).append(game.save.outfit);
        for (Badge b : game.content.badges) sb.append(game.hasBadge(b) ? '1' : '0');
        sb.append(System.identityHashCode(Ui.img("hazel")));
        return sb.toString();
    }

    private static final java.util.concurrent.ExecutorService BUILDER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "geomystery-office");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    // ================================================================== 방
    private static final int ROOM_H = 470, PORTRAIT_TOP = ROOM_H + 30;

    /** 누를 수 있는 곳. */
    private static final class Spot {
        final Shape shape;
        final String kind, id, tip;

        Spot(Shape shape, String kind, String id, String tip) {
            this.shape = shape;
            this.kind = kind;
            this.id = id;
            this.tip = tip;
        }
    }

    private final class Room extends JComponent implements Scrollable {
        private BufferedImage cache;
        private String cacheKey, pendingKey;
        private int cacheW, cacheH;
        private List<Spot> spots = new ArrayList<>();
        private List<Spot> bSpots = new ArrayList<>();
        private Spot hover;
        /** 헤이즐 그림의 위치(눈 깜빡임용). */
        private Rectangle2D hazelRect, bHazelRect;
        private boolean blinkShown;
        private final javax.swing.Timer blinkTimer;
        private final long t0 = System.currentTimeMillis();

        Room() {
            setToolTipText("");
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    Spot s = hit(e.getPoint());
                    if (s != hover) {
                        if (hover != null) repaintSpot(hover);
                        hover = s;
                        if (hover != null) repaintSpot(hover);
                    }
                    setCursor(Cursor.getPredefinedCursor(s != null && clickable(s) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (hover != null) { Spot h = hover; hover = null; repaintSpot(h); }
                }
                @Override public void mouseClicked(MouseEvent e) {
                    Spot s = hit(e.getPoint());
                    if (s != null) open(s);
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            // 헤이즐 눈 깜빡임: 상태가 바뀔 때만 얼굴 부분을 다시 그린다
            blinkTimer = new javax.swing.Timer(60, e -> {
                if (hazelRect == null || !isShowing()) return;
                boolean b = SceneFx.blinking("hazel", (System.currentTimeMillis() - t0) / 1000.0);
                if (b != blinkShown) {
                    blinkShown = b;
                    Rectangle2D r = hazelRect;
                    repaint((int) r.getX(), (int) r.getY(), (int) r.getWidth() + 1, (int) (r.getHeight() * 0.6));
                }
            });
            addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) blinkTimer.start(); else blinkTimer.stop();
                }
            });
        }

        private boolean clickable(Spot s) {
            switch (s.kind) {
                case "npc": return game.epilogueOpen(s.id);
                case "dome": return game.save.solvedFinales.contains(s.id);
                case "card": return game.content.theorems.containsKey(s.id) && game.cards().contains(game.content.theorems.get(s.id));
                default: return false;
            }
        }

        private void open(Spot s) {
            switch (s.kind) {
                case "npc": {
                    Npc n = game.content.npc(s.id);
                    if (n == null) return;
                    if (game.epilogueOpen(s.id)) frame.startEpilogue(n);
                    else frame.toast(n.name + " · 호감도 " + game.hearts(s.id) + " / " + Game.HEARTS + " — 그 장의 사건을 모두 풀고 범인을 밝히면 후일담이 열려요");
                    break;
                }
                case "dome": {
                    Lesson l = game.content.lessonById.get(s.id);
                    if (l != null && game.save.solvedFinales.contains(l.id)) ConfessionComic.play(frame, game, l, null);
                    else frame.toast("아직 범인을 밝히지 못한 사건이에요");
                    break;
                }
                case "card": {
                    Theorem t = game.content.theorems.get(s.id);
                    if (t != null && clickable(s)) CardZoom.open(frame, t);
                    break;
                }
                default: break;
            }
        }

        private Spot hit(Point p) {
            for (int i = spots.size() - 1; i >= 0; i--) if (spots.get(i).shape.contains(p)) return spots.get(i);
            return null;
        }

        private void repaintSpot(Spot s) {
            Rectangle b = s.shape.getBounds();
            repaint(b.x - 8, b.y - 8, b.width + 16, b.height + 16);
        }

        @Override public String getToolTipText(MouseEvent e) {
            Spot s = hit(e.getPoint());
            return s == null ? null : s.tip;
        }

        @Override public Dimension getPreferredSize() {
            int groups = 0;
            for (Chapter ch : game.content.chapters) if (!game.content.npcsOf(ch).isEmpty()) groups++;
            return new Dimension(1000, PORTRAIT_TOP + 60 + (int) Math.ceil(groups / 2.0) * 200 + 20);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 60; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }

        void request(int w, int h, double sc) {
            if (w <= 0 || h <= 0) return;
            String k = w + "x" + h + "@" + sc + "|" + signature();
            if (k.equals(cacheKey) || k.equals(pendingKey)) return;
            pendingKey = k;
            BUILDER.submit(() -> {
                BufferedImage img = new BufferedImage((int) Math.ceil(w * sc), (int) Math.ceil(h * sc), BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D g = Theme.aa(img.createGraphics());
                g.scale(sc, sc);
                bSpots = new ArrayList<>();
                try {
                    paintRoom(g, w, h);
                } catch (RuntimeException ex) {
                    System.err.println("[office] " + ex);
                } finally {
                    g.dispose();
                }
                List<Spot> ss = bSpots;
                Rectangle2D hr = bHazelRect;
                SwingUtilities.invokeLater(() -> {
                    if (!k.equals(pendingKey)) return;
                    cache = img;
                    cacheKey = k;
                    cacheW = w;
                    cacheH = h;
                    spots = ss;
                    hazelRect = hr;
                    pendingKey = null;
                    repaint();
                });
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            if (!Ui.offscreen(g)) request(w, h, Ui.deviceScale(g));
            if (cache != null) {
                Ui.blit(g, cache, 0, 0, cacheW, cacheH);
            } else {
                g.setColor(new Color(0x6B4A2E));
                g.fill(new RoundRectangle2D.Float(0, 0, w, Math.min(h, ROOM_H), 16, 16));
                g.setFont(Theme.hand(26));
                g.setColor(Theme.CREAM);
                Theme.drawCentered(g, "사무소를 정리하는 중…", w / 2.0, 120);
            }
            // 헤이즐 눈 깜빡임
            if (hazelRect != null && SceneFx.blinking("hazel", (System.currentTimeMillis() - t0) / 1000.0)) {
                SceneFx.blink(g, "hazel", hazelRect.getX(), hazelRect.getY(), hazelRect.getWidth(), hazelRect.getHeight(), false, false);
            }
            // 마우스를 올린 곳
            if (hover != null) {
                g.setColor(clickable(hover) ? new Color(255, 214, 102, 220) : new Color(255, 255, 255, 120));
                g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(hover.shape);
            }
            g.dispose();
        }

        // ------------------------------------------------------------ 방 그리기(백그라운드)
        private void paintRoom(Graphics2D g, int w, int h) {
            boolean dark = Theme.isDark();
            // ---- 벽지와 나무 벽판
            Shape roomShape = new RoundRectangle2D.Double(0, 0, w, ROOM_H, 18, 18);
            Graphics2D r = (Graphics2D) g.create();
            r.clip(roomShape);
            Color wall = dark ? new Color(0x3E3328) : new Color(0xC9A97A), stripe = dark ? new Color(0x463A2E) : new Color(0xD3B587);
            r.setColor(wall);
            r.fillRect(0, 0, w, ROOM_H);
            r.setColor(stripe);
            for (int x = 0; x < w; x += 44) r.fillRect(x, 0, 18, ROOM_H);
            r.setColor(dark ? new Color(0x2E2219) : new Color(0x7A5232));
            r.fillRect(0, 330, w, ROOM_H - 330);
            r.setColor(dark ? new Color(0x3A2B1E) : new Color(0x8C6340));
            for (int x = 20; x < w; x += 150) r.fill(new RoundRectangle2D.Double(x, 346, 126, 70, 8, 8));
            r.setColor(dark ? new Color(0x5A4228) : new Color(0xB8903A));
            r.fillRect(0, 326, w, 8);
            r.setColor(dark ? new Color(0x1E150D) : new Color(0x4A3020));
            r.fillRect(0, 432, w, ROOM_H - 432);
            // 천장의 따뜻한 등불빛
            r.setPaint(new RadialGradientPaint(new Point2D.Double(w * 0.55, 0), w * 0.6f, new float[]{0f, 1f},
                    new Color[]{new Color(255, 220, 150, dark ? 60 : 70), new Color(255, 220, 150, 0)}));
            r.fillRect(0, 0, w, ROOM_H);
            r.dispose();

            double leftW = 230, rightW = Math.min(300, w * 0.26);
            window(g, 24, 24, leftW - 24, 190);
            cardFrame(g, 24, 232, leftW - 24, 196);
            double cx0 = leftW + 24, cx1 = w - rightW - 16;
            shelf(g, cx0, 24, cx1 - cx0, 200);
            domes(g, cx0, 240, cx1 - cx0, 190);
            hazelDesk(g, w - rightW, 0, rightW, ROOM_H);

            g.setColor(new Color(0x3A2819));
            g.setStroke(new BasicStroke(4f));
            g.draw(roomShape);

            portraits(g, w, PORTRAIT_TOP);
        }

        private void window(Graphics2D g, double x, double y, double w, double h) {
            Chapter ch = game.currentChapter();
            BufferedImage bg = ch == null ? null : Ui.img("bg_" + ch.scene);
            g.setColor(new Color(0x2A1D12));
            g.fill(new RoundRectangle2D.Double(x - 8, y - 8, w + 16, h + 16, 12, 12));
            Graphics2D c = (Graphics2D) g.create();
            c.clip(new Rectangle2D.Double(x, y, w, h));
            if (bg != null) {
                double s = h / (double) bg.getHeight() * 1.25;
                double iw = bg.getWidth() * s;
                Ui.drawScaled(c, bg, x + w / 2 - iw / 2, y - h * 0.1, iw, bg.getHeight() * s);
            }
            c.setColor(new Color(255, 255, 255, 30));
            c.fill(new Rectangle2D.Double(x, y, w, h));
            c.dispose();
            // 창틀과 창살
            g.setColor(new Color(0x8C6A3A));
            g.setStroke(new BasicStroke(7f));
            g.draw(new Rectangle2D.Double(x, y, w, h));
            g.setStroke(new BasicStroke(5f));
            g.draw(new Line2D.Double(x + w / 2, y, x + w / 2, y + h));
            g.draw(new Line2D.Double(x, y + h / 2, x + w, y + h / 2));
            // 커튼
            g.setColor(new Color(0x8A3A3A));
            Path2D cl = new Path2D.Double();
            cl.moveTo(x - 10, y - 10); cl.lineTo(x + 30, y - 10); cl.quadTo(x + 10, y + h * 0.5, x + 26, y + h + 6); cl.lineTo(x - 10, y + h + 6); cl.closePath();
            g.fill(cl);
            Path2D cr = new Path2D.Double();
            cr.moveTo(x + w + 10, y - 10); cr.lineTo(x + w - 30, y - 10); cr.quadTo(x + w - 10, y + h * 0.5, x + w - 26, y + h + 6); cr.lineTo(x + w + 10, y + h + 6); cr.closePath();
            g.fill(cr);
            g.setColor(new Color(0x6B4A2E));
            g.fill(new RoundRectangle2D.Double(x - 16, y - 16, w + 32, 10, 6, 6));
            label(g, ch == null ? "" : "창밖: " + ch.title, x + w / 2, y + h + 4);
        }

        private void label(Graphics2D g, String s, double cx, double cy) {
            if (s == null || s.isEmpty()) return;
            g.setFont(Theme.bold(12.5f));
            FontMetrics fm = g.getFontMetrics();
            double tw = Theme.width(fm, s) + 18;
            g.setColor(new Color(0xF4E8CC));
            g.fill(new RoundRectangle2D.Double(cx - tw / 2, cy - 11, tw, 22, 8, 8));
            g.setColor(new Color(0x8C6A3A));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new RoundRectangle2D.Double(cx - tw / 2, cy - 11, tw, 22, 8, 8));
            g.setColor(new Color(0x3A2819));
            Theme.drawCentered(g, s, cx, cy);
        }

        private void cardFrame(Graphics2D g, double x, double y, double w, double h) {
            g.setColor(new Color(0x6B4A2E));
            g.fill(new RoundRectangle2D.Double(x - 6, y - 6, w + 12, h + 12, 8, 8));
            g.setColor(new Color(0xEFE3C6));
            g.fill(new Rectangle2D.Double(x, y, w, h));
            g.setColor(Theme.GOLD);
            g.setStroke(new BasicStroke(2f));
            g.draw(new Rectangle2D.Double(x + 4, y + 4, w - 8, h - 8));
            List<Theorem> all = new ArrayList<>(game.content.theorems.values());
            List<Theorem> owned = game.cards();
            int cols = 5, rows = (int) Math.ceil(all.size() / (double) cols);
            double cw = (w - 24) / cols, chh = (h - 40) / Math.max(1, rows);
            double cardW = Math.min(cw - 6, chh * 0.78), cardH = cardW / 0.78;
            for (int i = 0; i < all.size(); i++) {
                Theorem t = all.get(i);
                double ccx = x + 12 + (i % cols) * cw + cw / 2, ccy = y + 14 + (i / cols) * chh + chh / 2;
                Rectangle2D rr = new Rectangle2D.Double(ccx - cardW / 2, ccy - cardH / 2, cardW, cardH);
                boolean have = owned.contains(t);
                g.setColor(have ? new Color(0xF4E8CC) : new Color(0xDCD1B8));
                g.fill(rr);
                g.setColor(have ? Theme.GOLD : new Color(0xBFB298));
                g.setStroke(new BasicStroke(1.5f));
                g.draw(rr);
                if (have) Icons.theorem(g, t.icon, new Rectangle2D.Double(rr.getX() + 2, rr.getY() + 3, cardW - 4, cardW - 4), new Color(0x3A2819), new Color(0xA33A28));
                else { g.setFont(Theme.bold(11)); g.setColor(new Color(0xA89C80)); Theme.drawCentered(g, "?", rr.getCenterX(), rr.getCenterY()); }
                bSpots.add(new Spot(rr, "card", t.id, have ? t.name + " — 눌러서 크게 보기" : "아직 얻지 못한 정리 카드"));
            }
            g.setFont(Theme.hand(18));
            g.setColor(new Color(0x3A2819));
            Theme.drawCentered(g, "정리 카드 " + owned.size() + " / " + all.size(), x + w / 2, y + h - 14);
        }

        private void shelf(Graphics2D g, double x, double y, double w, double h) {
            // 진열장 몸체
            g.setColor(new Color(0x5A3A22));
            g.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
            g.setColor(new Color(0x3A2416));
            g.fill(new Rectangle2D.Double(x + 10, y + 10, w - 20, h - 20));
            List<Badge> bs = game.content.badges;
            int perRow = (int) Math.ceil(bs.size() / 2.0);
            double rowH = (h - 20) / 2.0;
            for (int row = 0; row < 2; row++) {
                double by = y + 10 + rowH * (row + 1) - 10;
                g.setColor(new Color(0x8C6340));
                g.fill(new Rectangle2D.Double(x + 6, by, w - 12, 10));
            }
            double cell = (w - 30) / Math.max(1, perRow);
            double r = Math.min(cell * 0.32, rowH * 0.3);
            for (int i = 0; i < bs.size(); i++) {
                Badge b = bs.get(i);
                int row = i / perRow, col = i % perRow;
                double cx = x + 15 + col * cell + cell / 2, cy = y + 10 + rowH * row + rowH * 0.46;
                boolean have = game.hasBadge(b);
                if (have) {
                    // 리본과 메달
                    g.setColor(new Color(0xA33A28));
                    g.fill(poly(cx - r * 0.55, cy - r * 1.6, cx - r * 0.05, cy - r * 1.6, cx - r * 0.25, cy - r * 0.6, cx - r * 0.75, cy - r * 0.6));
                    g.setColor(new Color(0x2F5E4F));
                    g.fill(poly(cx + r * 0.05, cy - r * 1.6, cx + r * 0.55, cy - r * 1.6, cx + r * 0.75, cy - r * 0.6, cx + r * 0.25, cy - r * 0.6));
                    g.setColor(Theme.GOLD);
                    g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                    g.setColor(new Color(0xF2D27A));
                    g.fill(new Ellipse2D.Double(cx - r * 0.78, cy - r * 0.78, r * 1.56, r * 1.56));
                    g.setColor(new Color(0x8C6A3A));
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                    Icons.paint(g, b.icon, cx - r * 0.55, cy - r * 0.55, r * 1.1, new Color(0x5A3A22));
                } else {
                    g.setColor(new Color(255, 255, 255, 22));
                    g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                    g.setColor(new Color(255, 255, 255, 60));
                    g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{4, 4}, 0));
                    g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
                }
                g.setFont(Theme.serif(11));
                g.setColor(have ? new Color(0xF2DFAE) : new Color(0x9C8466));
                Theme.drawCentered(g, b.name, cx, cy + r + 14);
                bSpots.add(new Spot(new Ellipse2D.Double(cx - r - 4, cy - r - 4, r * 2 + 8, r * 2 + 8), "badge", b.id,
                        (have ? "훈장 · " : "아직 얻지 못한 훈장 · ") + b.name + " — " + b.description));
            }
            g.setColor(new Color(0x2A1D12));
            g.setStroke(new BasicStroke(3f));
            g.draw(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
            label(g, "훈장 진열장", x + w / 2, y + h + 2);
        }

        private void domes(Graphics2D g, double x, double y, double w, double h) {
            List<Lesson> bosses = new ArrayList<>();
            for (Lesson l : game.content.lessons) if (game.content.hasFinale(l)) bosses.add(l);
            // 진열 탁자
            double ty = y + h - 40;
            g.setColor(new Color(0x6B4A2E));
            g.fill(new RoundRectangle2D.Double(x, ty, w, 22, 8, 8));
            g.setColor(new Color(0x4A3020));
            g.fill(new Rectangle2D.Double(x + 20, ty + 22, 14, 30));
            g.fill(new Rectangle2D.Double(x + w - 34, ty + 22, 14, 30));
            int n = Math.max(1, bosses.size());
            double cell = w / n, dr = Math.min(cell * 0.34, h * 0.38);
            for (int i = 0; i < bosses.size(); i++) {
                Lesson l = bosses.get(i);
                boolean solved = game.save.solvedFinales.contains(l.id);
                double cx = x + cell * i + cell / 2, base = ty;
                // 받침
                g.setColor(new Color(0x3A2416));
                g.fill(new RoundRectangle2D.Double(cx - dr * 0.9, base - 12, dr * 1.8, 14, 6, 6));
                String prop = "";
                List<Confession> cs = game.content.confessions.get(l.id);
                if (cs != null) for (Confession c : cs) if (Confession.MOTIVE.equals(c.kind)) prop = c.prop;
                if (solved) ConfessionComic.prop(g, prop, cx, base - 12 - dr * 0.6, dr * 0.62);
                else {
                    g.setFont(Theme.xbold((float) (dr * 0.7)));
                    g.setColor(new Color(255, 255, 255, 70));
                    Theme.drawCentered(g, "?", cx, base - 12 - dr * 0.6);
                }
                // 유리 돔
                Shape dome = new RoundRectangle2D.Double(cx - dr * 0.8, base - 12 - dr * 1.45, dr * 1.6, dr * 1.45, dr * 1.4, dr * 1.4);
                g.setColor(new Color(200, 230, 255, 40));
                g.fill(dome);
                g.setColor(new Color(230, 245, 255, 150));
                g.setStroke(new BasicStroke(2f));
                g.draw(dome);
                g.setColor(new Color(255, 255, 255, 110));
                g.fill(new Ellipse2D.Double(cx - dr * 0.55, base - 12 - dr * 1.2, dr * 0.18, dr * 0.5));
                Chapter ch = game.content.chapterOf(l);
                String culprit = game.content.culpritOf(l);
                Npc cn = culprit == null ? null : game.content.npc(culprit);
                g.setFont(Theme.serif(11.5f));
                g.setColor(new Color(0xF2DFAE));
                Theme.drawCentered(g, solved ? (cn == null ? l.caseName : cn.name.substring(cn.name.lastIndexOf(' ') + 1) + "의 증거품") : ch.label, cx, ty + 38);
                bSpots.add(new Spot(dome, "dome", l.id, solved ? l.caseName + " — 눌러서 자백 만화 다시 보기" : "아직 범인을 밝히지 못한 사건 · " + l.caseName));
            }
            label(g, "증거품 진열대", x + w / 2, ty + 11);
        }

        private void hazelDesk(Graphics2D g, double x, double y, double w, double h) {
            // 책상
            double dy = 330;
            BufferedImage hz = Ui.img("hazel");
            if (hz != null) {
                double hh = 300, hw = hh * hz.getWidth() / hz.getHeight();
                double hx = x + w * 0.5 - hw / 2, hy = dy + 46 - hh;
                Ui.drawScaled(g, hz, hx, hy, hw, hh);
                bHazelRect = new Rectangle2D.Double(hx, hy, hw, hh);
            }
            g.setColor(new Color(0x6B4A2E));
            g.fill(new RoundRectangle2D.Double(x + 10, dy, w - 30, 26, 8, 8));
            g.setColor(new Color(0x5A3A22));
            g.fill(new Rectangle2D.Double(x + 20, dy + 26, w - 50, 70));
            g.setColor(new Color(0x8C6340));
            g.fill(new RoundRectangle2D.Double(x + 34, dy + 40, (w - 78) / 2, 22, 4, 4));
            g.fill(new RoundRectangle2D.Double(x + 44 + (w - 78) / 2, dy + 40, (w - 78) / 2, 22, 4, 4));
            // 책상 위 소품: 돋보기와 책 더미
            g.setColor(new Color(0xA33A28));
            g.fill(new Rectangle2D.Double(x + 30, dy - 18, 60, 12));
            g.setColor(new Color(0x2F5E4F));
            g.fill(new Rectangle2D.Double(x + 34, dy - 30, 52, 12));
            g.setColor(new Color(0xC9A456));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(x + w - 90, dy - 26, 26, 26));
            g.draw(new Line2D.Double(x + w - 68, dy - 4, x + w - 52, dy + 6));
            // 말풍선
            String say = hazelLine();
            g.setFont(Theme.serif(13.5f));
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = Widgets.TextBlock.wrap(say, fm, (int) w - 60);
            double bh = lines.size() * 20 + 20, bw = w - 30;
            RoundRectangle2D bub = new RoundRectangle2D.Double(x + 8, 18, bw, bh, 14, 14);
            g.setColor(new Color(0xFFFBF2));
            g.fill(bub);
            g.setColor(Theme.GOLD);
            g.setStroke(new BasicStroke(2f));
            g.draw(bub);
            g.setColor(new Color(0x3A2819));
            int ly = 18 + 14 + fm.getAscent() - 2;
            for (String s : lines) { Theme.drawString(g, s, (float) (x + 22), ly); ly += 20; }
        }

        private String hazelLine() {
            int fresh = game.newEpilogues();
            if (fresh > 0) return "새 후일담이 " + fresh + "개 열렸어! 아래 인물 수첩에서 반짝이는 초상화를 눌러 봐.";
            int solved = game.save.solvedFinales.size();
            if (solved == 0) return "여기가 우리 사무소야. 사건을 풀수록 진열장이 채워질 거야!";
            if (game.allCleared()) return "범인 " + solved + "명을 모두 잡았어. 진열대의 돔을 누르면 그날의 자백을 다시 볼 수 있어.";
            return "범인 " + solved + "명을 잡았어. 증거품 돔을 누르면 자백 만화를, 카드를 누르면 정리를 다시 볼 수 있어.";
        }

        // ------------------------------------------------------------ 인물 수첩
        private void portraits(Graphics2D g, int w, double top) {
            g.setFont(Theme.xbold(20));
            g.setColor(Theme.INK);
            g.drawString("인물 수첩", 4, (float) top + 6);
            g.setFont(Theme.serif(13));
            g.setColor(Theme.INK_SOFT);
            String sub = "사건을 풀고 범인을 밝힐수록 하트가 찬다 · 하트 다섯 개면 후일담이 열린다";
            g.drawString(sub, 110, (float) top + 5);
            List<Chapter> chs = new ArrayList<>();
            for (Chapter ch : game.content.chapters) if (!game.content.npcsOf(ch).isEmpty()) chs.add(ch);
            double gw = (w - 20) / 2.0, gh = 186;
            for (int i = 0; i < chs.size(); i++) {
                Chapter ch = chs.get(i);
                double gx = (i % 2) * (gw + 20), gy = top + 24 + (i / 2) * (gh + 14);
                RoundRectangle2D box = new RoundRectangle2D.Double(gx, gy, gw, gh, 14, 14);
                g.setColor(Theme.PAPER);
                g.fill(box);
                g.setColor(Theme.BORDER);
                g.setStroke(new BasicStroke(2f));
                g.draw(box);
                boolean open = game.isChapterUnlocked(ch);
                g.setFont(Theme.tracked(Theme.serif(12), 0.16f));
                g.setColor(Theme.INK_SOFT);
                g.drawString(ch.label, (float) gx + 18, (float) gy + 26);
                g.setFont(Theme.xbold(16));
                g.setColor(Theme.INK);
                g.drawString(open ? ch.title : "???", (float) gx + 18 + g.getFontMetrics(Theme.tracked(Theme.serif(12), 0.16f)).stringWidth(ch.label) + 12, (float) gy + 27);
                List<Npc> ns = game.content.npcsOf(ch);
                double cell = (gw - 24) / Math.max(4, ns.size());
                double fs = Math.min(76, cell - 18);
                for (int k = 0; k < ns.size(); k++) {
                    Npc n = ns.get(k);
                    double cx = gx + 12 + cell * k + cell / 2, fy = gy + 42;
                    portrait(g, n, cx, fy, fs, open);
                }
            }
        }

        private void portrait(Graphics2D g, Npc n, double cx, double y, double fs, boolean open) {
            int hearts = game.hearts(n.id);
            boolean epi = game.epilogueOpen(n.id), fresh = epi && !game.save.epiloguesSeen.contains(n.id);
            Rectangle2D frameR = new Rectangle2D.Double(cx - fs / 2 - 6, y - 6, fs + 12, fs + 12);
            g.setColor(epi ? Theme.GOLD : new Color(0xB59C78));
            g.fill(new RoundRectangle2D.Double(frameR.getX(), frameR.getY(), frameR.getWidth(), frameR.getHeight(), 10, 10));
            g.setColor(new Color(0xE9D8B2));
            g.fill(new Ellipse2D.Double(cx - fs / 2, y, fs, fs));
            BufferedImage face = Ui.img(n.image + "_face");
            if (face != null) {
                if (open) Ui.drawScaled(g, face, cx - fs / 2, y, fs, fs);
                else {
                    // 아직 못 만난 인물: 그림자
                    Composite keep = g.getComposite();
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
                    Ui.drawScaled(g, face, cx - fs / 2, y, fs, fs);
                    g.setComposite(keep);
                }
            }
            g.setFont(Theme.bold(12));
            g.setColor(Theme.INK);
            String nm = open ? n.name : "???";
            String shortName = nm.contains(" ") ? nm.substring(nm.lastIndexOf(' ') + 1) : nm;
            Theme.drawCentered(g, shortName, cx, y + fs + 20);
            // 하트
            double hw = 12, hx0 = cx - (Game.HEARTS * hw) / 2 + hw / 2;
            for (int i = 0; i < Game.HEARTS; i++) {
                Path2D hp = Icons.heart(hx0 + i * hw, y + fs + 40, 5.2);
                if (open && i < hearts) { g.setColor(new Color(0xD9534A)); g.fill(hp); }
                else { g.setColor(new Color(0xC9B48A)); g.setStroke(new BasicStroke(1.2f)); g.draw(hp); }
            }
            if (epi) {
                // 후일담 리본
                g.setColor(new Color(0x2F5E4F));
                g.fill(new RoundRectangle2D.Double(cx - 30, y + fs + 50, 60, 18, 9, 9));
                g.setFont(Theme.bold(11));
                g.setColor(Theme.CREAM);
                Theme.drawCentered(g, "후일담", cx, y + fs + 59);
            }
            if (fresh) {
                g.setColor(new Color(0xC23B2A));
                g.fill(new RoundRectangle2D.Double(cx + fs / 2 - 26, y - 12, 34, 18, 9, 9));
                g.setFont(Theme.bold(10.5f));
                g.setColor(Color.WHITE);
                Theme.drawCentered(g, "NEW", cx + fs / 2 - 9, y - 3);
            }
            String tip = !open ? "아직 만나지 못한 인물" : n.name + " · " + n.role + " · 호감도 " + hearts + "/" + Game.HEARTS
                    + (epi ? " — 눌러서 후일담 보기" : " — 그 장의 사건을 모두 풀고 범인을 밝히면 후일담이 열려요");
            bSpots.add(new Spot(new Ellipse2D.Double(cx - fs / 2 - 2, y - 2, fs + 4, fs + 4), "npc", n.id, tip));
        }

        private Path2D poly(double... xy) {
            Path2D p = new Path2D.Double();
            p.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
            p.closePath();
            return p;
        }
    }
}
