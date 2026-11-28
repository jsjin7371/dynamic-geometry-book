package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * 사건 보드 — 코르크판에 해결한 사건의 사진, 얻은 정리 카드, 용의자와 범인을 붙이고 빨간 실로 잇는다.
 * 진행할수록 보드가 채워진다. 사진을 누르면 사건 기록을 볼 수 있다.
 */
public class CaseBoardScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private final Header header;
    private final Board board;
    private String lastSig = "";

    /** 보드에 보이는 상태(해결·열림·범인·테마·새 사진)가 바뀌었는지 알아보는 표식. */
    private String signature() {
        StringBuilder sb = new StringBuilder(Theme.isDark() ? "d" : "l");
        for (Lesson l : game.content.lessons) sb.append(game.isDone(l) ? 'D' : game.isUnlocked(l) ? 'U' : 'L');
        sb.append(game.save.solvedFinales);
        fresh = freshPhotos();
        sb.append("|new").append(fresh);
        return sb.toString();
    }

    /** 아직 보드에 꽂지 않은 새 증거 사진(해결했지만 꽂히는 연출을 보지 않은 사건). */
    private Set<String> fresh = Collections.emptySet();

    /** 한 번에 꽂는 새 사진은 최근 것 4장까지. 그보다 오래된 것은 이미 꽂아 둔 것으로 친다. */
    private Set<String> freshPhotos() {
        List<Lesson> news = new ArrayList<>();
        for (Lesson l : game.content.lessons) if (game.isDone(l) && !game.save.photosPinned.contains(l.id)) news.add(l);
        if (news.size() > 4) {
            for (Lesson l : news.subList(0, news.size() - 4)) game.save.photosPinned.add(l.id);
            news = news.subList(news.size() - 4, news.size());
        }
        Set<String> out = new LinkedHashSet<>();
        for (Lesson l : news) out.add(l.id);
        return Collections.unmodifiableSet(out);
    }

    public CaseBoardScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        header = new Header(game);
        header.setTitle("CASE BOARD", "사건 보드");
        add(header, BorderLayout.NORTH);
        board = new Board();
        JScrollPane sp = Ui.scroll(board);
        sp.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        // 스크롤바가 생겼다 없어졌다 하면 보드 폭이 바뀌어 미리 그린 그림을 다시 그려야 한다 → 항상 보인다
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(sp, BorderLayout.CENTER);
    }

    public void refresh() {
        header.refresh();
        lastSig = signature();
        board.revalidate();
        board.repaint();
    }

    /** 코르크 무늬 한 장(화면 배율별). 확대·축소 없이 바둑판처럼 이어 붙인다. */
    private static final Map<Integer, BufferedImage> CORK = Collections.synchronizedMap(new HashMap<>());

    private static BufferedImage corkTile(int scale) {
        return CORK.computeIfAbsent(scale, s -> {
            int n = 220 * s;
            BufferedImage im = new BufferedImage(n, n, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = im.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xB98A57));
            g.fillRect(0, 0, n, n);
            Random r = new Random(5);
            for (int i = 0; i < 2600; i++) {
                int v = r.nextInt(3);
                g.setColor(v == 0 ? new Color(0x9C6F40) : v == 1 ? new Color(0xCFA26C) : new Color(0xA77A49));
                int sz = (1 + r.nextInt(3)) * s;
                g.fillOval(r.nextInt(n), r.nextInt(n), sz, sz);
            }
            g.dispose();
            return im;
        });
    }

    /** 사진용으로 잘라 줄인 배경(사건·크기별). 보드를 다시 그릴 때마다 줄이지 않도록 보관한다. */
    private static final Map<String, BufferedImage> PHOTOS = Collections.synchronizedMap(new HashMap<>());

    /** 보드 그림은 이 스레드에서 그린다(첫 그리기가 1초 가까이 걸려 화면이 멈추지 않도록). */
    private static final java.util.concurrent.ExecutorService BUILDER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "geomystery-board");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    /** 시작 직후 미리 보드 그림을 준비해 둔다(처음 열 때 기다리지 않도록). */
    public void prewarm() {
        lastSig = signature();
        if (board.getWidth() <= 0 || board.getHeight() <= 0) return;
        double sc = Ui.screenScale(this);
        board.request(board.getWidth(), board.getHeight(), sc);
    }

    private final class Board extends JComponent implements Scrollable {
        /** 화면(EDT)에서 쓰는 클릭 영역·윤곽. 새 그림이 완성될 때 통째로 바뀐다. */
        private Map<Rectangle2D, Lesson> hits = new LinkedHashMap<>();
        private Map<Lesson, Shape> outlines = new HashMap<>();
        /** 그리는 스레드가 채우는 쪽. */
        private Map<Rectangle2D, Lesson> bHits = new LinkedHashMap<>();
        private Map<Lesson, Shape> bOutlines = new HashMap<>();
        private Lesson hover;
        /** 보드 전체를 한 번 그려 둔 이미지(스크롤할 때는 이것만 복사). */
        private java.awt.image.BufferedImage cache;
        private String cacheKey, pendingKey;
        private int cacheW, cacheH;
        /** 새 증거 사진: 보드 그림에는 빼 두고 따로 떨어뜨려 핀에 꽂는다. */
        private Map<Lesson, Sprite> sprites = new LinkedHashMap<>();
        private Map<Lesson, Sprite> bSprites = new LinkedHashMap<>();
        private Set<String> bFresh = Collections.emptySet();
        private long pinStart;
        private javax.swing.Timer pinTimer;
        private static final long PIN_GAP = 380, PIN_DROP = 380, PIN_SWING = 1500;

        /** 이 크기·상태의 그림이 없으면 백그라운드에서 그리도록 맡긴다. */
        void request(int w, int h, double sc) {
            if (w <= 0 || h <= 0) return;
            String k = w + "x" + h + "@" + sc + "|" + lastSig;
            if (k.equals(cacheKey) || k.equals(pendingKey)) return;
            pendingKey = k;
            Set<String> fr = fresh;
            BUILDER.submit(() -> {
                bFresh = fr;
                bSprites = new LinkedHashMap<>();
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage((int) Math.ceil(w * sc), (int) Math.ceil(h * sc), java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D cg = Theme.aa(img.createGraphics());
                cg.scale(sc, sc);
                try {
                    paintBoard(cg, w, h);
                } finally {
                    cg.dispose();
                }
                Map<Rectangle2D, Lesson> hh = bHits;
                Map<Lesson, Shape> oo = bOutlines;
                Map<Lesson, Sprite> ss = bSprites;
                SwingUtilities.invokeLater(() -> {
                    if (!k.equals(pendingKey)) return; // 그새 더 새로운 요청이 있었다
                    cache = img;
                    cacheKey = k;
                    cacheW = w;
                    cacheH = h;
                    hits = hh;
                    outlines = oo;
                    if (pinTimer != null) pinTimer.stop();
                    sprites = ss;
                    pinStart = 0;
                    pinDone = false;
                    // 이미 꽂는 연출을 본 사진이면(창 크기만 바뀐 경우 등) 다시 떨어뜨리지 않는다
                    boolean seen = !ss.isEmpty();
                    for (Lesson l : ss.keySet()) if (!game.save.photosPinned.contains(l.id)) seen = false;
                    if (seen) { pinDone = true; pinStart = 1; }
                    pendingKey = null;
                    repaint();
                });
            });
        }

        Board() {
            setToolTipText("");
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    Lesson l = hit(e.getPoint());
                    if (l != hover) {
                        Shape before = hover == null ? null : outlines.get(hover);
                        hover = l;
                        Shape after = hover == null ? null : outlines.get(hover);
                        if (before != null) repaint(before.getBounds().x - 6, before.getBounds().y - 6, before.getBounds().width + 12, before.getBounds().height + 12);
                        if (after != null) repaint(after.getBounds().x - 6, after.getBounds().y - 6, after.getBounds().width + 12, after.getBounds().height + 12);
                    }
                    setCursor(Cursor.getPredefinedCursor(l != null && game.isUnlocked(l) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
                @Override public void mouseClicked(MouseEvent e) {
                    Lesson l = hit(e.getPoint());
                    if (l != null && game.isUnlocked(l)) openRecord(l);
                }
                @Override public void mouseExited(MouseEvent e) {
                    if (hover != null && outlines.get(hover) != null) {
                        Rectangle b = outlines.get(hover).getBounds();
                        hover = null;
                        repaint(b.x - 6, b.y - 6, b.width + 12, b.height + 12);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private Lesson hit(Point p) {
            for (Map.Entry<Rectangle2D, Lesson> e : hits.entrySet()) if (e.getKey().contains(p)) return e.getValue();
            return null;
        }

        @Override public String getToolTipText(MouseEvent e) {
            Lesson l = hit(e.getPoint());
            if (l == null) return null;
            if (!game.isUnlocked(l)) return "아직 열리지 않은 사건";
            return (game.isDone(l) ? "해결 · " : "수사 중 · ") + l.caseName + " — 눌러서 사건 기록 보기";
        }

        @Override public Dimension getPreferredSize() {
            int maxL = 0;
            for (Chapter ch : game.content.chapters) maxL = Math.max(maxL, game.content.lessonsOf(ch).size());
            return new Dimension(1000, 150 + maxL * 96 + 300);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 60; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            if (!Ui.offscreen(g)) request(w, h, Ui.deviceScale(g));
            if (cache != null) {
                // 새 그림이 준비될 때까지는 이전 그림을 보여 준다
                Ui.blit(g, cache, 0, 0, cacheW, cacheH);
            } else {
                g.setColor(new Color(0x5A3A22));
                g.fill(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));
                g.setColor(new Color(0xB98A57));
                g.fill(new RoundRectangle2D.Float(12, 12, w - 24, h - 24, 8, 8));
                g.setFont(Theme.hand(26));
                g.setColor(new Color(0x3A2819));
                Theme.drawCentered(g, "사건 보드를 정리하는 중…", w / 2.0, 120);
            }
            paintSprites(g);
            Shape hs = hover == null ? null : outlines.get(hover);
            if (hs != null && game.isUnlocked(hover)) {
                g.setColor(new Color(255, 214, 102, 200));
                g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(hs);
            }
            g.dispose();
        }

        // ---------------------------------------------------------- 새 사진 꽂기
        private boolean pinDone;

        private void startPin() {
            pinStart = System.currentTimeMillis();
            pinDone = false;
            Set<Lesson> sounded = new HashSet<>();
            long total = (sprites.size() - 1) * PIN_GAP + PIN_DROP + PIN_SWING;
            // 흔들리는 동안은 초당 30번이면 충분하다(회전 그리기는 비싸다)
            pinTimer = new javax.swing.Timer(33, e -> {
                long el = System.currentTimeMillis() - pinStart;
                for (Sprite sp : sprites.values()) {
                    repaint(sp.bounds());
                    if (el - sp.order * PIN_GAP >= PIN_DROP && sounded.add(sp.lesson)) Sfx.play("click");
                }
                if (el > total) {
                    ((javax.swing.Timer) e.getSource()).stop();
                    pinDone = true;
                    for (Sprite sp : sprites.values()) { game.save.photosPinned.add(sp.lesson.id); repaint(sp.bounds()); }
                    game.save.save();
                }
            });
            pinTimer.start();
            Sfx.play("page");
        }

        private void paintSprites(Graphics2D g) {
            if (sprites.isEmpty()) return;
            if (pinStart == 0 && isShowing()) startPin();
            long el = pinStart == 0 ? 0 : System.currentTimeMillis() - pinStart;
            for (Sprite sp : sprites.values()) {
                long ti = el - sp.order * PIN_GAP;
                if (ti < 0) continue;
                if (pinDone) {
                    // 멈춘 뒤에는 한 번 회전해 둔 이미지를 그대로 복사한다
                    double sc = Ui.deviceScale(g);
                    if (sp.rest == null || Math.abs(sp.restScale - sc) > 0.01) sp.buildRest(sc);
                    Ui.blit(g, sp.rest, sp.pinX - sp.restHalf, sp.pinY - sp.restHalf, sp.restHalf * 2, sp.restHalf * 2);
                    continue;
                }
                double off = 0, ang = sp.rot;
                if (ti < PIN_DROP) {
                    double k = ti / (double) PIN_DROP;
                    off = -110 * Math.pow(1 - k, 3);
                    ang = sp.rot + 18 * (1 - k);
                } else {
                    double ts = (ti - PIN_DROP) / 1000.0;
                    ang = sp.rot + 15 * Math.exp(-ts * 3.0) * Math.sin(ts * 10.5);
                }
                Graphics2D s = (Graphics2D) g.create();
                s.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                s.translate(sp.pinX, sp.pinY + off);
                s.rotate(Math.toRadians(ang));
                s.drawImage(sp.img, (int) Math.round(-sp.offX), (int) Math.round(-sp.offY), (int) Math.round(sp.lw), (int) Math.round(sp.lh), null);
                s.dispose();
            }
        }

        private void paintBoard(Graphics2D g, int w, int h) {
            // 나무 액자 + 코르크
            g.setColor(new Color(0x5A3A22));
            g.fill(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));
            {
                // 코르크: 화면 픽셀 크기의 무늬를 1:1로 이어 붙인다(패턴 채우기를 배율째 하면 아주 느리다)
                int s = (int) Math.round(Ui.deviceScale(g));
                BufferedImage tile = corkTile(Math.max(1, s));
                Graphics2D c = (Graphics2D) g.create();
                c.clip(new RoundRectangle2D.Float(12, 12, w - 24, h - 24, 8, 8));
                java.awt.geom.AffineTransform t = c.getTransform();
                c.setTransform(new java.awt.geom.AffineTransform(1, 0, 0, 1, t.getTranslateX(), t.getTranslateY()));
                for (int y = 0; y < h * s; y += tile.getHeight()) for (int x = 0; x < w * s; x += tile.getWidth()) c.drawImage(tile, x, y, null);
                c.dispose();
            }
            g.setColor(new Color(0, 0, 0, 50));
            g.setStroke(new BasicStroke(4f));
            g.draw(new RoundRectangle2D.Float(14, 14, w - 28, h - 28, 8, 8));
            bHits = new LinkedHashMap<>();
            bOutlines = new HashMap<>();
            List<Chapter> chs = game.content.chapters;
            double colW = (w - 40) / (double) chs.size();
            for (int c = 0; c < chs.size(); c++) paintChapter(g, chs.get(c), 20 + c * colW, colW);
        }

        private void paintChapter(Graphics2D g, Chapter ch, double x0, double colW) {
            List<Lesson> ls = game.content.lessonsOf(ch);
            boolean open = game.isChapterUnlocked(ch);
            // 장 메모
            Graphics2D n = (Graphics2D) g.create();
            n.translate(x0 + colW / 2, 62);
            n.rotate(Math.toRadians((ch.order % 2 == 0 ? 2 : -2)));
            RoundRectangle2D note = new RoundRectangle2D.Double(-120, -36, 240, 76, 4, 4);
            n.setColor(new Color(0, 0, 0, 50));
            n.translate(3, 4);
            n.fill(note);
            n.translate(-3, -4);
            n.setColor(open ? new Color(0xF6E9C4) : new Color(0xD9CDB0));
            n.fill(note);
            n.setFont(Theme.tracked(Theme.serif(12), 0.2f));
            n.setColor(new Color(0x8C6A48));
            Theme.drawCentered(n, ch.label, 0, -16);
            n.setFont(Theme.hand(28));
            n.setColor(new Color(0x3A2819));
            Theme.drawCentered(n, open ? ch.title : "???", 0, 10);
            n.setFont(Theme.serif(12));
            n.setColor(new Color(0xA33A28));
            Theme.drawCentered(n, game.doneCount(ch) + " / " + ls.size() + " 해결", 0, 30);
            n.dispose();
            pin(g, x0 + colW / 2, 30, new Color(0x3E5F8A));

            // 사건 사진들(지그재그)
            List<Point2D.Double> pins = new ArrayList<>();
            double pw = Math.min(150, colW * (game.content.chapters.size() > 3 ? 0.47 : 0.52)), ph = pw * 0.86;
            for (int i = 0; i < ls.size(); i++) {
                Lesson l = ls.get(i);
                double px = x0 + (i % 2 == 0 ? colW * 0.07 : colW * 0.93 - pw);
                double py = 128 + i * 96;
                pins.add(new Point2D.Double(px + pw / 2, py + 6));
            }
            // 실 (사진 아래에 깔리도록 먼저)
            for (int i = 0; i + 1 < ls.size(); i++) {
                if (!game.isDone(ls.get(i))) continue;
                string(g, pins.get(i), pins.get(i + 1));
            }
            for (int i = 0; i < ls.size(); i++) {
                Lesson l = ls.get(i);
                Point2D.Double pn = pins.get(i);
                double px = pn.x - pw / 2, py = pn.y - 6;
                double rot = ((l.id.hashCode() % 7) - 3) * 1.2;
                boolean isNew = bFresh.contains(l.id);
                polaroid(g, l, px, py, pw, ph, rot, i % 2 == 0, !isNew);
                bHits.put(new Rectangle2D.Double(px, py, pw, ph), l);
                if (isNew) bSprites.put(l, sprite(g, l, pn, pw, ph, rot, bSprites.size()));
                else pin(g, pn.x, pn.y, new Color(0xC23B2A));
            }
            // 최종 사건의 용의자들
            Lesson boss = null;
            for (Lesson l : ls) if (l.boss) boss = l;
            if (boss != null && game.isUnlocked(boss)) suspects(g, boss, x0, colW, 128 + ls.size() * 96 + ph - 40, pins.get(ls.indexOf(boss)));
        }

        /** 새 사진 한 장을 회전 없이 따로 그려 둔다(핀 위치 기준으로 떨어뜨리고 흔든다). */
        private Sprite sprite(Graphics2D like, Lesson l, Point2D.Double pn, double pw, double ph, double rot, int order) {
            double m = 16, lw = pw + m * 2, lh = ph + m * 2;
            double sc = Ui.deviceScale(like);
            BufferedImage im = new BufferedImage((int) Math.ceil(lw * sc), (int) Math.ceil(lh * sc), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = Theme.aa(im.createGraphics());
            g.scale(sc, sc);
            photoBody(g, l, m, m, pw, ph);
            pin(g, m + pw / 2, m + 6, new Color(0xC23B2A));
            g.dispose();
            Sprite sp = new Sprite();
            sp.lesson = l;
            sp.img = im;
            sp.lw = lw;
            sp.lh = lh;
            sp.offX = m + pw / 2;
            sp.offY = m + 6;
            sp.pinX = pn.x;
            sp.pinY = pn.y;
            sp.rot = rot;
            sp.order = order;
            return sp;
        }

        private void polaroid(Graphics2D g0, Lesson l, double x, double y, double w, double h, double rot, boolean left, boolean drawPhoto) {
            boolean done = game.isDone(l);
            bOutlines.put(l, java.awt.geom.AffineTransform.getRotateInstance(Math.toRadians(rot), x + w / 2, y + h / 2)
                    .createTransformedShape(new Rectangle2D.Double(x - 2, y - 2, w + 4, h + 4)));
            if (drawPhoto) {
                Graphics2D g = (Graphics2D) g0.create();
                g.rotate(Math.toRadians(rot), x + w / 2, y + h / 2);
                photoBody(g, l, x, y, w, h);
                g.dispose();
            }
            cardFor(g0, l, x, y, w, done, left);
        }

        /** 폴라로이드 본체(종이, 사진, 제목, 해결 도장). 해결한 사건은 칠판 증거 사진을 붙인다. */
        private void photoBody(Graphics2D g, Lesson l, double x, double y, double w, double h) {
            boolean done = game.isDone(l), unlocked = game.isUnlocked(l);
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new Rectangle2D.Double(x + 4, y + 6, w, h));
            g.setColor(unlocked ? new Color(0xFBF7EE) : new Color(0xD8D2C4));
            g.fill(new Rectangle2D.Double(x, y, w, h));
            double ix = x + 8, iy = y + 8, iw = w - 16, ih = h * 0.6;
            BufferedImage bg = Ui.img("bg_" + l.scene);
            BufferedImage evidence = done ? EvidencePhoto.of(game.content, l, (int) Math.round(iw * Ui.deviceScale(g)), (int) Math.round(ih * Ui.deviceScale(g))) : null;
            if (evidence != null) {
                Ui.drawScaled(g, evidence, ix, iy, iw, ih);
            } else if (unlocked && bg != null) {
                int sw = 520, sh = (int) (sw * ih / iw);
                int sx = Math.abs(l.id.hashCode() * 37) % Math.max(1, bg.getWidth() - sw);
                int sy = Math.max(0, Math.min(bg.getHeight() - sh, 60 + Math.abs(l.id.hashCode()) % 120));
                int dw = (int) Math.round(iw * Ui.deviceScale(g)), dh = (int) Math.round(ih * Ui.deviceScale(g));
                String key = l.id + ":" + dw + "x" + dh;
                BufferedImage photo = PHOTOS.get(key);
                if (photo == null) {
                    photo = Ui.resize(bg.getSubimage(sx, sy, sw, Math.min(sh, bg.getHeight() - sy)), dw, dh, false);
                    PHOTOS.put(key, photo);
                }
                Ui.drawScaled(g, photo, ix, iy, iw, ih);
                if (!done) {
                    g.setColor(new Color(30, 20, 10, 140));
                    g.fill(new Rectangle2D.Double(ix, iy, iw, ih));
                    g.setFont(Theme.xbold(34));
                    g.setColor(new Color(0xF2DFAE));
                    Theme.drawCentered(g, "?", ix + iw / 2, iy + ih / 2);
                }
            } else {
                g.setColor(new Color(0x9E968A));
                g.fill(new Rectangle2D.Double(ix, iy, iw, ih));
                Icons.paint(g, "lock", ix + iw / 2 - 13, iy + ih / 2 - 13, 26, new Color(0xE6E0D2));
            }
            g.setFont(Theme.hand(Math.min(22, (float) w / 7.5f)));
            g.setColor(new Color(0x3A2819));
            String cap = unlocked ? l.caseName : "잠긴 사건";
            Theme.drawCentered(g, cap, x + w / 2, iy + ih + (h - ih - 8) / 2);
            if (done) {
                // 해결 도장
                Graphics2D s = (Graphics2D) g.create();
                s.translate(x + w - 28, iy + ih - 10);
                s.rotate(Math.toRadians(-14));
                s.setColor(new Color(194, 59, 42, 210));
                s.setStroke(new BasicStroke(2.2f));
                s.draw(new Ellipse2D.Double(-22, -22, 44, 44));
                s.setFont(Theme.xbold(14));
                Theme.drawCentered(s, "해결", 0, 0);
                s.dispose();
            }
        }

        /** 정리 카드(해결한 사건만): 사진 옆에 비스듬히. */
        private void cardFor(Graphics2D g0, Lesson l, double x, double y, double w, boolean done, boolean left) {
            Theorem t = game.content.theorems.get(l.theoremId);
            if (done && t != null) {
                Graphics2D c = (Graphics2D) g0.create();
                double cx = left ? x + w + 4 : x - 58;
                c.translate(cx, y + 18);
                c.rotate(Math.toRadians(left ? 7 : -7));
                RoundRectangle2D card = new RoundRectangle2D.Double(0, 0, 54, 70, 6, 6);
                c.setColor(new Color(0, 0, 0, 60));
                c.translate(2, 3);
                c.fill(card);
                c.translate(-2, -3);
                c.setColor(new Color(0xF4E8CC));
                c.fill(card);
                c.setColor(Theme.GOLD);
                c.setStroke(new BasicStroke(2.2f));
                c.draw(card);
                Icons.theorem(c, t.icon, new Rectangle2D.Double(9, 8, 36, 36), new Color(0x3A2819), new Color(0xA33A28));
                c.setFont(Theme.bold(8.5f));
                c.setColor(new Color(0x3A2819));
                int ly = 54;
                for (String ln : Widgets.TextBlock.wrap(t.name, c.getFontMetrics(), 48)) {
                    if (ly > 68) break;
                    Theme.drawCentered(c, ln, 27, ly);
                    ly += 10;
                }
                c.dispose();
                pin(g0, cx + 27, y + 20, Theme.GOLD);
            }
        }

        private void suspects(Graphics2D g, Lesson boss, double x0, double colW, double y, Point2D.Double bossPin) {
            List<String> who = new ArrayList<>();
            String culprit = null;
            for (FinaleLine f : game.content.finaleOf(boss)) {
                if (!f.isTestimony() || who.contains(f.speaker)) continue;
                who.add(f.speaker);
                if (FinaleLine.LIE.equals(f.kind)) culprit = f.speaker;
            }
            if (who.isEmpty()) return;
            boolean solved = game.save.solvedFinales.contains(boss.id);
            double fs = Math.min(64, (colW - 20) / who.size() - 8);
            double gap = (colW - who.size() * fs) / (who.size() + 1);
            g.setFont(Theme.hand(22));
            g.setColor(new Color(0x3A2819));
            g.drawString(solved ? "범인을 찾았다!" : "용의자", (float) (x0 + gap), (float) y);
            for (int i = 0; i < who.size(); i++) {
                String sp = who.get(i);
                double fx = x0 + gap + i * (fs + gap), fy = y + 14;
                boolean isCulprit = solved && sp.equals(culprit);
                if (isCulprit) string(g, bossPin, new Point2D.Double(fx + fs / 2, fy + 4));
                g.setColor(new Color(0, 0, 0, 60));
                g.fill(new Rectangle2D.Double(fx + 3, fy + 4, fs, fs + 18));
                g.setColor(new Color(0xFBF7EE));
                g.fill(new Rectangle2D.Double(fx, fy, fs, fs + 18));
                Npc n = game.content.npc(sp);
                BufferedImage im = n == null ? null : Ui.img(n.image + "_face");
                Composite keep = g.getComposite();
                if (solved && !isCulprit) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                g.setColor(new Color(0xE9D8B2));
                g.fill(new Rectangle2D.Double(fx + 4, fy + 4, fs - 8, fs - 8));
                if (im != null) Ui.drawScaled(g, im, fx + 4, fy + 4, fs - 8, fs - 8);
                g.setComposite(keep);
                g.setFont(Theme.serif(10.5f));
                g.setColor(new Color(0x3A2819));
                String name = n != null ? n.name : sp;
                String shortName = name.contains(" ") ? name.substring(name.lastIndexOf(' ') + 1) : name;
                Theme.drawCentered(g, shortName, fx + fs / 2, fy + fs + 8);
                if (isCulprit) {
                    g.setColor(new Color(0xC23B2A));
                    g.setStroke(new BasicStroke(3.5f));
                    g.draw(new Ellipse2D.Double(fx - 6, fy - 6, fs + 12, fs + 30));
                    Graphics2D s = (Graphics2D) g.create();
                    s.translate(fx + fs / 2, fy + fs + 26);
                    s.rotate(Math.toRadians(-6));
                    s.setFont(Theme.xbold(15));
                    Theme.drawCentered(s, "범인!", 0, 0);
                    s.dispose();
                }
                pin(g, fx + fs / 2, fy + 3, isCulprit ? new Color(0xC23B2A) : new Color(0x2F6B55));
            }
        }

        /** 처진 빨간 실. */
        private void string(Graphics2D g, Point2D.Double a, Point2D.Double b) {
            double sag = 18 + a.distance(b) * 0.08;
            QuadCurve2D q = new QuadCurve2D.Double(a.x, a.y, (a.x + b.x) / 2, (a.y + b.y) / 2 + sag, b.x, b.y);
            g.setColor(new Color(0, 0, 0, 50));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.translate(1.5, 2.5);
            g.draw(q);
            g.translate(-1.5, -2.5);
            g.setColor(new Color(0xB8302A));
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(q);
        }

        private void pin(Graphics2D g, double x, double y, Color c) {
            g.setColor(new Color(0, 0, 0, 70));
            g.fill(new Ellipse2D.Double(x - 5, y - 2, 12, 12));
            g.setColor(c);
            g.fill(new Ellipse2D.Double(x - 7, y - 7, 14, 14));
            g.setColor(new Color(255, 255, 255, 150));
            g.fill(new Ellipse2D.Double(x - 4, y - 5, 5, 4));
        }
    }

    /** 새로 꽂는 증거 사진 한 장. */
    private static final class Sprite {
        Lesson lesson;
        BufferedImage img;
        double lw, lh, offX, offY, pinX, pinY, rot;
        int order;
        /** 멈춘 자세(rot)로 미리 회전한 이미지. 핀을 가운데로 한 정사각형. */
        BufferedImage rest;
        double restScale, restHalf;

        void buildRest(double sc) {
            restHalf = Math.hypot(Math.max(offX, lw - offX), Math.max(offY, lh - offY)) + 4;
            rest = new BufferedImage((int) Math.ceil(restHalf * 2 * sc), (int) Math.ceil(restHalf * 2 * sc), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = rest.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(sc, sc);
            g.translate(restHalf, restHalf);
            g.rotate(Math.toRadians(rot));
            g.drawImage(img, (int) Math.round(-offX), (int) Math.round(-offY), (int) Math.round(lw), (int) Math.round(lh), null);
            g.dispose();
            restScale = sc;
        }

        Rectangle bounds() {
            double r = Math.hypot(lw, lh);
            return new Rectangle((int) (pinX - r) - 4, (int) (pinY - 120 - 4), (int) (r * 2) + 8, (int) (r + 130) + 8);
        }
    }

    private void openRecord(Lesson l) {
        boolean done = game.isDone(l);
        Chapter ch = game.content.chapterOf(l);
        Ui.Modal m = Ui.modal(frame, (l.boss ? "최종 사건 · " : "사건 " + l.order + " · ") + l.caseName, 540);
        m.add(Ui.label(ch.label + "  ·  " + l.title + "  ·  " + l.location, Theme.serif(14), Theme.INK_SOFT));
        m.add(Ui.vgap(12));
        JPanel memo = Ui.vbox();
        for (String s : l.memo) {
            JLabel b = Ui.label("•  " + s, Theme.serif(15), Theme.INK);
            b.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            memo.add(b);
        }
        m.add(memo);
        m.add(Ui.vgap(10));
        if (done) {
            m.text("사건 결과 — " + l.resolution);
            if (l.boss && game.save.solvedFinales.contains(l.id)) m.text("최종 추리로 범인을 밝혀냈다.");
        } else {
            m.text("아직 수사 중인 사건입니다.");
        }
        m.button("닫기", Btn.Style.OUTLINE, null);
        Theorem t = game.content.theorems.get(l.theoremId);
        if (done && t != null && game.content.explores.containsKey(t.id)) {
            m.button("정리 탐구", Btn.Style.OUTLINE, () -> ExploreBoard.open(frame, t));
        }
        m.button(done ? "다시 풀기" : "사건 시작", Btn.Style.PRIMARY, () -> frame.startLesson(l, false));
        m.show();
    }
}
