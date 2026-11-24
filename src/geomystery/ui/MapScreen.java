package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 01 탐험 지도 — 홈 · 학습 경로. */
public class MapScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private final Header header;
    private Chapter viewing;

    private final JLabel bannerSmall, bannerTitle, bannerPct;
    private final Widgets.Bar bannerBar;
    private final Btn bannerBtn;
    private final PathMap path;
    private final TipCard tip;
    private final JPanel questRows;
    private final JLabel questTime;
    private final JPanel leagueRows;
    private final JLabel leagueDays, leagueTitle;
    private final SideArrow prevCh, nextCh;
    /** 이미 축하한 완료 의뢰. 처음 지도를 열 때 이미 끝나 있던 의뢰는 축하하지 않는다. */
    private final java.util.Set<String> seenDone = new java.util.HashSet<>();

    public MapScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        bindKeys();
        setBackground(Theme.BG);

        for (Quest q : game.todayQuests()) if (game.questDone(q)) seenDone.add(q.id + "@" + game.today());
        header = new Header(game);
        prevCh = new SideArrow(-1, () -> moveChapter(-1));
        nextCh = new SideArrow(1, () -> moveChapter(1));
        add(header, BorderLayout.NORTH);

        JPanel body = Ui.clear(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        GridBagConstraints c = new GridBagConstraints();

        // ---------- 왼쪽: 현재 사건 배너 + 경로 지도
        JPanel left = Ui.clear(new BorderLayout(0, 18));
        RoundPanel banner = new RoundPanel(new BorderLayout(24, 0), Theme.GREEN, Theme.GOLD_DARK, 14).borderWidth(2f);
        banner.pad(16, 24, 16, 20);
        JPanel bt = Ui.vbox();
        bannerSmall = Ui.label("", Theme.tracked(Theme.serif(13), 0.12f), new Color(0xCFE0D6));
        bannerTitle = Ui.label("", Theme.xbold(22), Theme.CREAM);
        bt.add(bannerSmall);
        bt.add(Ui.vgap(6));
        bt.add(bannerTitle);
        banner.add(bt, BorderLayout.WEST);
        JPanel bm = Ui.vbox();
        bm.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bannerBar = new Widgets.Bar(new Color(0x1E4037), Theme.GOLD, 10);
        bannerBar.setAlignmentX(LEFT_ALIGNMENT);
        bannerPct = Ui.label("", Theme.serif(13), new Color(0xCFE0D6));
        bm.add(bannerBar);
        bm.add(Ui.vgap(8));
        bm.add(bannerPct);
        banner.add(bm, BorderLayout.CENTER);
        bannerBtn = new Btn("이어서 풀기", Btn.Style.CREAM).font(17);
        bannerBtn.addActionListener(e -> continueCase());
        JPanel bbWrap = Ui.clear(new GridBagLayout());
        bbWrap.add(bannerBtn);
        banner.add(bbWrap, BorderLayout.EAST);
        left.add(banner, BorderLayout.NORTH);

        path = new PathMap();
        // 장 넘기기: 지도 양옆 가운데에 큰 화살표 하나씩
        path.setLayout(null);
        path.add(prevCh);
        path.add(nextCh);
        left.add(path, BorderLayout.CENTER);

        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 28);
        body.add(left, c);

        // ---------- 오른쪽 열
        JPanel right = Ui.vbox();
        right.setPreferredSize(new Dimension(300, 600));
        right.setMinimumSize(new Dimension(300, 300));
        tip = new TipCard();
        right.add(tip);
        right.add(Ui.vgap(16));

        RoundPanel quest = new RoundPanel(new BorderLayout(0, 14), Theme.PAPER, Theme.BORDER, 14).borderWidth(2f);
        quest.pad(18, 20, 20, 20);
        quest.setAlignmentX(LEFT_ALIGNMENT);
        JPanel qh = Ui.clear(new BorderLayout(8, 0));
        qh.add(Ui.label("오늘의 의뢰", Theme.xbold(18), Theme.INK), BorderLayout.WEST);
        questTime = Ui.label("", Theme.serif(12.5f), Theme.RED);
        qh.add(questTime, BorderLayout.EAST);
        quest.add(qh, BorderLayout.NORTH);
        questRows = Ui.vbox();
        quest.add(questRows, BorderLayout.CENTER);
        quest.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        right.add(quest);
        right.add(Ui.vgap(20));

        RoundPanel league = new RoundPanel(new BorderLayout(0, 12), Theme.DARK, Theme.GOLD_DARK, 14).borderWidth(2f);
        league.pad(18, 20, 16, 20);
        league.setAlignmentX(LEFT_ALIGNMENT);
        JPanel lh = Ui.clear(new BorderLayout(8, 0));
        leagueTitle = Ui.label("", Theme.xbold(17), Theme.CREAM);
        lh.add(leagueTitle, BorderLayout.WEST);
        leagueDays = Ui.label("", Theme.serif(12.5f), new Color(0xCDB68D));
        lh.add(leagueDays, BorderLayout.EAST);
        league.add(lh, BorderLayout.NORTH);
        leagueRows = Ui.vbox();
        league.add(leagueRows, BorderLayout.CENTER);
        Btn all = new Btn("협회 순위 전체 보기", Btn.Style.LINK);
        all.addActionListener(e -> frame.showAssocTab("league"));
        JPanel allWrap = Ui.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        allWrap.add(all);
        league.add(allWrap, BorderLayout.SOUTH);
        league.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        right.add(league);
        right.add(Box.createVerticalGlue());

        c.gridx = 1; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0);
        body.add(right, c);
        add(body, BorderLayout.CENTER);
    }

    private void moveChapter(int d) {
        List<Chapter> chs = game.content.chapters;
        int i = chs.indexOf(viewing) + d;
        if (i < 0 || i >= chs.size()) return;
        path.beginFlip(d);
        viewing = chs.get(i);
        Music.play(Music.forScene(viewing.scene)); // 지도에서 넘겨 보는 장의 곡
        Sfx.play("hint");
        refresh(false);
    }

    /** 지도에서 Enter: 지금 사건 이어서 하기. */
    private void bindKeys() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "mapContinue");
        getActionMap().put("mapContinue", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (isShowing()) continueCase(); }
        });
        // ← → 로 장 넘기기(화살표 버튼과 같다). 지도가 화면에 있을 때만.
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("LEFT"), "mapPrevChapter");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("RIGHT"), "mapNextChapter");
        getActionMap().put("mapPrevChapter", chapterKey(-1));
        getActionMap().put("mapNextChapter", chapterKey(1));
    }

    private Action chapterKey(int d) {
        return new AbstractAction() {
            @Override public boolean isEnabled() { return isShowing(); } // 다른 화면의 ← → 를 가로채지 않게
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                SideArrow b = d < 0 ? prevCh : nextCh;
                if (isShowing() && b.isEnabled()) moveChapter(d);
            }
        };
    }

    private void continueCase() {
        Lesson cur = game.currentLesson();
        if (cur == null) {
            frame.showTab("box");
            return;
        }
        frame.startLesson(cur, false);
    }

    public void refresh() { refresh(true); }

    private void refresh(boolean resetChapter) {
        if (resetChapter || viewing == null) viewing = game.currentChapter();
        header.setTitle(viewing.label, viewing.title);
        header.refresh();
        int idx = game.content.chapters.indexOf(viewing);
        prevCh.setEnabled(idx > 0);
        nextCh.setEnabled(idx < game.content.chapters.size() - 1);

        Lesson cur = game.currentLesson();
        if (cur != null) {
            Chapter cc = game.content.chapterOf(cur);
            List<Lesson> ls = game.content.lessonsOf(cc);
            bannerSmall.setText("현재 사건 · " + cur.order + " / " + ls.size() + " 단계");
            bannerTitle.setText(cur.boss ? "최종 사건 — " + cur.caseName : cur.title + " — " + cur.caseName);
            float pct = game.doneCount(cc) / (float) ls.size();
            bannerBar.setValue(pct);
            bannerPct.setText("챕터 진행도 " + Math.round(pct * 100) + "%");
            bannerBtn.setText(game.save.isDone(cur.id) ? "다시 풀기" : cur.order == 1 && game.doneCount(cc) == 0 ? "사건 시작" : "이어서 풀기");
        } else {
            bannerSmall.setText("모든 사건 해결");
            bannerTitle.setText("명탐정의 기록이 완성되었다!");
            bannerBar.setValue(1f);
            bannerPct.setText("전체 진행도 100%");
            bannerBtn.setText("복습하기");
        }

        path.setChapter(viewing);
        if (resetChapter) {
            Lesson jc = game.takeJustCleared();
            if (jc != null) {
                Chapter cc = game.content.chapterOf(jc);
                if (cc == viewing) path.walkFrom(jc);
                else if (jc.boss && cur != null && game.content.chapterOf(cur) == viewing) {
                    javax.swing.Timer t = new javax.swing.Timer(500, e -> {
                        frame.toast("새 장이 열렸다 — " + viewing.label + " · " + viewing.title);
                        Sfx.play("fanfare");
                    });
                    t.setRepeats(false);
                    t.start();
                }
            }
        }
        tip.setText(cur != null ? cur.tip : "모든 사건을 해결했다네! 수수께끼 상자에서 오답을 복습해 보는 건 어떤가?");

        questTime.setText("자정까지 " + game.hoursToMidnight() + "시간");
        questRows.removeAll();
        for (Quest q : game.todayQuests()) {
            int p = game.questProgress(q);
            JPanel row = Ui.clear(new BorderLayout(0, 8));
            row.setAlignmentX(LEFT_ALIGNMENT);
            JPanel tr = Ui.clear(new BorderLayout());
            tr.add(Ui.label(q.text, Theme.serif(14.5f), Theme.INK), BorderLayout.WEST);
            tr.add(Ui.label(p + " / " + q.target, Theme.bold(15), Theme.INK), BorderLayout.EAST);
            row.add(tr, BorderLayout.NORTH);
            Widgets.Bar b = new Widgets.Bar(Theme.c(0xE6D6B4), game.questDone(q) ? Theme.GOLD_DARK : Theme.GREEN, 9);
            // 새로 완료된 의뢰는 지도에 돌아왔을 때 막대가 반짝인다(한 번만)
            if (game.questDone(q) && seenDone.add(q.id + "@" + game.today())) b.celebrate(450);
            b.setValue(p / (float) q.target);
            row.add(b, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            questRows.add(row);
            questRows.add(Ui.vgap(10));
        }

        leagueTitle.setText(game.tierName());
        leagueDays.setText(game.leagueDaysLeft() + "일 남음");
        leagueRows.removeAll();
        List<Game.LeagueRow> rows = game.league();
        List<Integer> show = new ArrayList<>();
        int me = 0;
        for (int i = 0; i < rows.size(); i++) if (rows.get(i).me) me = i;
        if (me < 3) { show.add(0); show.add(1); show.add(2); }
        else { show.add(0); show.add(1); show.add(me); }
        for (int i : show) leagueRows.add(new LeagueLine(i + 1, rows.get(i)));
        revalidate();
        repaint();
    }

    private static class LeagueLine extends JComponent {
        final int rank;
        final Game.LeagueRow row;

        LeagueLine(int rank, Game.LeagueRow row) {
            this.rank = rank;
            this.row = row;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(240, 38));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            Color fg = Theme.CREAM;
            if (row.me) {
                g.setColor(Theme.c(0xF6EBD2));
                g.fill(new RoundRectangle2D.Float(-8, 2, w + 16, h - 4, 8, 8));
                fg = Theme.INK;
            }
            g.setFont(Theme.bold(15));
            g.setColor(row.me ? Theme.INK : Theme.GOLD);
            FontMetrics fm = g.getFontMetrics();
            int by = (h + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString("" + rank, 2, by);
            String xp = Ui.num(row.xp) + " XP";
            g.setFont(Theme.serif(13.5f));
            int xw = g.getFontMetrics().stringWidth(xp);
            g.setColor(fg);
            g.drawString(xp, w - xw, by);
            g.setFont(row.me ? Theme.bold(13.5f) : Theme.serif(13.5f));
            String name = row.name;
            FontMetrics nf = g.getFontMetrics();
            while (nf.stringWidth(name) > w - xw - 36 - 10 && name.length() > 2) name = name.substring(0, name.length() - 2) + "…";
            g.drawString(name, 30, by);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ 헤이즐의 한마디
    private class TipCard extends JComponent {
        private String text = "";

        TipCard() {
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(300, 214));
            setMinimumSize(new Dimension(260, 214));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 214));
        }

        void setText(String t) { text = t == null ? "" : t; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            g.setColor(Theme.PAPER);
            g.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));
            g.setColor(Theme.BORDER);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));
            BufferedImage face = Ui.img("hazel_face");
            float bx = 14, by = 14, bw = w - 28, bh = h - 92;
            // 말풍선
            g.setColor(Theme.c(0xFFFBF2));
            Path2D tail = new Path2D.Float();
            tail.moveTo(56, by + bh - 2); tail.lineTo(64, by + bh + 16); tail.lineTo(80, by + bh - 2); tail.closePath();
            g.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 12, 12));
            g.fill(tail);
            g.setColor(Theme.INK);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(bx, by, bw, bh, 12, 12));
            g.setColor(Theme.c(0xFFFBF2));
            g.fill(new Rectangle2D.Float(57, by + bh - 3, 22, 4));
            g.setColor(Theme.INK);
            g.draw(new Line2D.Float(56, by + bh, 64, by + bh + 16));
            g.draw(new Line2D.Float(64, by + bh + 16, 80, by + bh));
            g.setFont(Theme.serif(13.5f));
            FontMetrics fm = g.getFontMetrics();
            int y = (int) by + 25;
            for (String line : Widgets.TextBlock.wrap(text, fm, (int) bw - 28)) {
                if (y > by + bh - 6) break;
                g.drawString(line, bx + 14, y);
                y += 21;
            }
            if (face != null) Ui.drawScaled(g, face, 18, h - 84, 78, 78);
            g.setFont(Theme.hand(22));
            g.setColor(Theme.INK_SOFT);
            g.drawString("— 헤이즐 탐정", 108, h - 30);
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ 경로 지도
    private class PathMap extends JComponent {
        private Chapter chapter;
        private List<Lesson> lessons = new ArrayList<>();
        private List<Point2D.Double> nodes = new ArrayList<>();
        private int hover = -1;
        private final double[] xs = {0.48, 0.24, 0.66, 0.34, 0.66, 0.46, 0.3, 0.6};
        private int walkIndex = -1;
        private long walkStart;
        /** 걷기를 맡았지만 아직 시작하지 않음: 화면 전환이 끝나고 지도 그림이 준비된 뒤 첫 화면에서 시작한다. */
        private boolean walkPending;
        /** 헤이즐이 서 있는 자리(다시 그릴 범위). 걷는 중이 아니면 이 작은 영역만 다시 그린다. */
        private Rectangle marker;
        /** 걷는 길(시작·도착 사건과 그 사이 곡선)을 감싸는 영역. 걷는 동안에는 이 부분만 다시 그린다. */
        private Rectangle walkArea;
        private final javax.swing.Timer bob = new javax.swing.Timer(40, e -> {
            if (!Ui.inFront(this)) return;
            boolean walking = walkIndex >= 0 && (walkPending || System.currentTimeMillis() - walkStart < WALK_MS + 100);
            javax.swing.Timer tm = (javax.swing.Timer) e.getSource();
            int want = walking ? 15 : 40; // 걷는 동안은 약 60fps, 제자리 깡충은 25fps면 충분
            if (tm.getDelay() != want) tm.setDelay(want);
            if (walking && walkPending) {
                if (marker != null) repaint(marker); // 출발 전: 헤이즐 자리만(전환이 끝났는지 보려고)
                else repaint();
            } else if (walking) {
                if (walkArea != null) repaint(walkArea);
                else repaint();
            } else if (marker != null) repaint(marker); // 헤이즐이 없으면(모든 사건 해결) 아무것도 다시 그리지 않는다
        });
        private static final long WALK_MS = 1900;
        /** 장 풍경 바탕 그림(장·크기·배율·테마별). */
        private final Map<String, BufferedImage> BACKDROPS = new java.util.concurrent.ConcurrentHashMap<>();
        /** 지도 그림은 이 스레드에서 그린다(레티나에서 한 장에 0.03~0.25초 — 화면 스레드에서 그리면 걷기가 끊긴다). */
        private final java.util.concurrent.ExecutorService MAP_BUILDER = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "geomystery-map");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });

        // ---- 장 넘기기: 이전 지도를 한 장 찍어 두고 종이처럼 넘긴다
        private java.awt.image.BufferedImage flipOld;
        private int flipDir;
        private long flipStart;
        private static final long FLIP_MS = 520;
        private final javax.swing.Timer flipTimer = new javax.swing.Timer(16, e -> {
            repaint();
            if (System.currentTimeMillis() - flipStart > FLIP_MS) {
                flipOld = null;
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });

        void beginFlip(int dir) {
            if (getWidth() <= 0 || !isShowing()) return;
            double sc = 1;
            try { sc = Math.max(1, getGraphicsConfiguration().getDefaultTransform().getScaleX()); } catch (Exception ignored) { }
            java.awt.image.BufferedImage im = new java.awt.image.BufferedImage((int) Math.ceil(getWidth() * sc), (int) Math.ceil(getHeight() * sc), java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = im.createGraphics();
            g.scale(sc, sc);
            flipOld = null;
            paint(g);
            g.dispose();
            flipOld = im;
            flipDir = dir;
            flipStart = System.currentTimeMillis();
            flipTimer.restart();
        }

        /** 이전 장의 지도가 오른쪽(다음 장) 또는 왼쪽(이전 장)부터 말려 넘어간다. */
        private void paintFlip(Graphics2D g, int w, int h) {
            if (flipOld == null) return;
            double k = Math.min(1, (System.currentTimeMillis() - flipStart) / (double) FLIP_MS);
            double e = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
            double edge = flipDir > 0 ? w * (1 - e) : w * e;          // 아직 남은 이전 장의 경계
            double fold = Math.min(70, Math.max(0, Math.sin(Math.PI * e) * 90)); // 넘어가는 종이 뒷면 폭
            Graphics2D o = (Graphics2D) g.create();
            o.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int iw = flipOld.getWidth(), ih = flipOld.getHeight();
            double sx = iw / (double) w;
            if (flipDir > 0) {
                int ex = (int) edge;
                if (ex > 0) o.drawImage(flipOld, 0, 0, ex, h, 0, 0, (int) (ex * sx), ih, null);
                // 새 장 위에 드리운 그림자
                o.setPaint(new GradientPaint((float) edge, 0, new Color(0f, 0f, 0f, 0.28f), (float) edge + 50, 0, new Color(0, 0, 0, 0)));
                o.fillRect((int) edge, 0, 50, h);
                // 말려 올라간 종이 뒷면
                o.setPaint(new GradientPaint((float) (edge - fold), 0, new Color(0xD9C69A), (float) edge, 0, new Color(0xF7EEDC)));
                o.fillRect((int) (edge - fold), 0, (int) Math.ceil(fold), h);
            } else {
                int ex = (int) edge;
                if (ex < w) o.drawImage(flipOld, ex, 0, w, h, (int) (ex * sx), 0, iw, ih, null);
                o.setPaint(new GradientPaint((float) edge - 50, 0, new Color(0, 0, 0, 0), (float) edge, 0, new Color(0f, 0f, 0f, 0.28f)));
                o.fillRect((int) edge - 50, 0, 50, h);
                o.setPaint(new GradientPaint((float) edge, 0, new Color(0xF7EEDC), (float) (edge + fold), 0, new Color(0xD9C69A)));
                o.fillRect((int) edge, 0, (int) Math.ceil(fold), h);
            }
            o.dispose();
        }

        /** 방금 해결한 사건에서 다음 사건으로 헤이즐이 걸어간다. */
        void walkFrom(Lesson done) {
            walkIndex = lessons.indexOf(done);
            walkStart = Long.MAX_VALUE / 4; // 시작 전: 출발 사건 위에 서 있는다
            walkPending = true;
            walkArea = null;
            if (walkIndex < 0 || walkIndex + 1 >= lessons.size()) { walkIndex = -1; walkPending = false; }
            bob.setDelay(15);
            bob.restart();
        }

        @Override
        public void doLayout() {
            // 장 넘기기 화살표: 지도 양옆 가운데(사건은 지도 안쪽 24~66% 자리에 있어 겹치지 않는다)
            int bw = SideArrow.W, bh = SideArrow.H, y = (getHeight() - bh) / 2;
            prevCh.setBounds(12, y, bw, bh);
            nextCh.setBounds(getWidth() - 12 - bw, y, bw, bh);
        }

        PathMap() {
            setPreferredSize(new Dimension(600, 560));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int h = hit(e.getPoint());
                    if (h != hover) { hover = h; repaint(); }
                    setCursor(Cursor.getPredefinedCursor(h >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
                @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
                @Override public void mouseClicked(MouseEvent e) {
                    int h = hit(e.getPoint());
                    if (h >= 0) openLesson(lessons.get(h));
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            bob.start();
        }

        void setChapter(Chapter ch) {
            if (ch != chapter) { walkIndex = -1; walkPending = false; walkArea = null; }
            chapter = ch;
            lessons = game.content.lessonsOf(ch);
            prepare();
            repaint();
        }

        /** 지금 상태의 지도 그림을 미리(백그라운드에서) 그려 두게 한다. 화면 전환이 도는 동안 준비된다. */
        private void prepare() {
            int w = getWidth(), h = getHeight();
            if (chapter == null || w <= 0 || h <= 0) return;
            double sc = 1;
            try { sc = Math.max(1, getGraphicsConfiguration().getDefaultTransform().getScaleX()); } catch (Exception ignored) { }
            request(w, h, sc);
        }

        private int hit(Point p) {
            for (int i = 0; i < nodes.size(); i++) if (nodes.get(i).distance(p) < 44) return i;
            return -1;
        }

        private void layoutNodes() {
            nodes = layout(lessons, getWidth(), getHeight());
        }

        /** 사건 자리(백그라운드 그리기에서도 쓰므로 필드를 건드리지 않는다). */
        private List<Point2D.Double> layout(List<Lesson> ls, double w, double h) {
            List<Point2D.Double> out = new ArrayList<>();
            int n = ls.size();
            for (int i = 0; i < n; i++) {
                double t = n == 1 ? 0.5 : i / (double) (n - 1);
                double y = h * (0.87 - 0.72 * t);
                double x = ls.get(i).boss ? 0.52 : xs[i % xs.length];
                out.add(new Point2D.Double(w * x, y));
            }
            return out;
        }

        /** 헤이즐을 뺀 지도(길·사건 표시·장식)를 한 장으로 그려 둔 것. 헤이즐이 깡충거릴 때는 이것을 복사만 한다. */
        private java.awt.image.BufferedImage mapCache;
        private String mapKey;

        /** 지도 그림 이름: 앞부분(크기·배율·장·테마)이 같으면 진행 상태만 다른 것이라 옛 그림을 잠깐 보여 줘도 된다. */
        private String baseKey(int w, int h, double sc) {
            return w + "x" + h + "@" + sc + "|" + (chapter == null ? "-" : chapter.id) + "|" + Theme.isDark();
        }

        private String mapKey(int w, int h, double sc) {
            Lesson cur = game.currentLesson();
            return baseKey(w, h, sc) + "|" + hover + "|" + game.lessonsDone() + "|" + (cur == null ? "-" : cur.id);
        }

        private String pendingKey;

        /** 이 상태의 지도 그림을 백그라운드에서 그리게 한다(이미 있거나 그리는 중이면 아무것도 안 한다). */
        private void request(int w, int h, double sc) {
            String k = mapKey(w, h, sc);
            if (k.equals(mapKey) || k.equals(pendingKey)) return;
            pendingKey = k;
            Chapter ch = chapter;
            List<Lesson> ls = lessons;
            int hov = hover;
            MAP_BUILDER.submit(() -> {
                java.awt.image.BufferedImage img = new java.awt.image.BufferedImage((int) Math.ceil(w * sc), (int) Math.ceil(h * sc),
                        java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
                Graphics2D cg = Theme.aa(img.createGraphics());
                cg.scale(sc, sc);
                try {
                    paintStatic(cg, w, h, ch, ls, hov);
                } catch (RuntimeException ignored) {
                    // 그리는 도중 상태가 바뀌었다: 화면 스레드가 다시 맡긴다
                } finally {
                    cg.dispose();
                }
                SwingUtilities.invokeLater(() -> {
                    if (!k.equals(pendingKey)) return; // 그새 더 새로운 요청이 있었다
                    pendingKey = null;
                    mapCache = img;
                    mapKey = k;
                    repaint();
                });
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            if (chapter == null) { paintStatic(g, w, h, null, lessons, hover); g.dispose(); return; }
            Lesson cur = game.currentLesson();
            double sc = Ui.deviceScale(g);
            String key = mapKey(w, h, sc);
            boolean offscreen = Ui.offscreen(g);
            boolean fresh = true;
            if (offscreen) {
                paintStatic(g, w, h, chapter, lessons, hover); // 전환 사진용: 캐시를 바꾸지 않는다
            } else {
                if (!key.equals(mapKey) || mapCache == null) {
                    if (mapCache != null && mapKey != null && mapKey.startsWith(baseKey(w, h, sc) + "|")) {
                        // 진행 상태만 바뀌었다: 새 그림은 백그라운드에서 그리고, 그동안 옛 그림을 보여 준다(화면이 멈추지 않게)
                        request(w, h, sc);
                        fresh = false;
                    } else {
                        // 크기·장·테마가 바뀌었다: 옛 그림을 쓸 수 없으니 바로 그린다
                        pendingKey = null;
                        mapCache = Ui.deviceImage(g, w, h);
                        Graphics2D cg = Ui.cacheGraphics(mapCache, g);
                        paintStatic(cg, w, h, chapter, lessons, hover);
                        cg.dispose();
                        mapKey = key;
                    }
                }
                Ui.blit(g, mapCache, 0, 0, w, h);
            }
            layoutNodes();
            // 맡아 둔 걷기는 화면 전환이 끝나고 새 지도 그림이 보일 때 시작한다(전환에 가려 앞부분을 놓치지 않게)
            if (walkPending && !offscreen && fresh && !Transition.isRunning()) {
                walkPending = false;
                walkStart = System.currentTimeMillis() + 250;
            }
            drawHazel(g, cur);
            paintFlip(g, w, h);
            g.dispose();
        }

        /**
         * 장 풍경 바탕(화면 배율 크기, 둥근 지도 모양으로 잘림). 장·크기·테마별로 한 번만 만든다.
         * 지도 그림을 그리는 백그라운드 스레드에서도 부른다.
         */
        private BufferedImage backdrop(Chapter ch, int w, int h, double sc) {
            String key = ch.scene + "|" + w + "x" + h + "@" + sc + "|" + Theme.isDark();
            BufferedImage hit = BACKDROPS.get(key);
            if (hit != null) return hit;
            BufferedImage bg = Ui.img("bg_" + ch.scene);
            if (bg == null || w <= 4 || h <= 4) return null;
            int pw = (int) Math.ceil(w * sc), ph = (int) Math.ceil(h * sc);
            BufferedImage out = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = Theme.aa(out.createGraphics());
            g.scale(sc, sc);
            // 지도 모양(둥근 사각형)을 먼저 칠하고, 그 안에만 풍경을 얹는다(SRC_ATOP: 가장자리가 부드럽다)
            g.setColor(Color.WHITE);
            g.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 15, 15));
            g.setComposite(AlphaComposite.SrcAtop);
            // 지도를 가득 채우도록 맞추고(남는 쪽은 잘라 냄) 가운데 둔다
            double k = Math.max(w / (double) bg.getWidth(), h / (double) bg.getHeight());
            int iw = (int) Math.ceil(bg.getWidth() * k * sc), ih = (int) Math.ceil(bg.getHeight() * k * sc);
            BufferedImage scaled = Ui.resize(bg, iw, ih, false);
            Graphics2D d = (Graphics2D) g.create();
            d.setTransform(new java.awt.geom.AffineTransform());
            d.drawImage(scaled, (pw - iw) / 2, (ph - ih) / 2, null);
            d.dispose();
            // 양피지 색으로 덮기: 가운데는 풍경이 비치고, 가장자리·제목 자리는 더 짙게
            Color paper = Theme.c(0xEADBB8);
            g.setColor(new Color(paper.getRed(), paper.getGreen(), paper.getBlue(), 150));
            g.fillRect(0, 0, w, h);
            g.setPaint(new RadialGradientPaint(new java.awt.geom.Point2D.Float(w / 2f, h / 2f), Math.max(w, h) * 0.7f,
                    new float[]{0.45f, 1f},
                    new Color[]{new Color(paper.getRed(), paper.getGreen(), paper.getBlue(), 0), new Color(paper.getRed(), paper.getGreen(), paper.getBlue(), 170)}));
            g.fillRect(0, 0, w, h);
            g.setPaint(new GradientPaint(0, 0, new Color(paper.getRed(), paper.getGreen(), paper.getBlue(), 150), 0, 90,
                    new Color(paper.getRed(), paper.getGreen(), paper.getBlue(), 0)));
            g.fillRect(0, 0, w, 90);
            g.dispose();
            if (BACKDROPS.size() > 12) BACKDROPS.clear(); // 창 크기를 여러 번 바꿔도 쌓이지 않게
            BACKDROPS.put(key, out);
            return out;
        }

        private void paintStatic(Graphics2D g, int w, int h, Chapter chapter, List<Lesson> lessons, int hover) {
            g.setColor(Theme.c(0xEADBB8));
            g.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 16, 16));
            // 그 장의 풍경을 옅게 깐다(양피지 색으로 덮어 길·사건 표시가 잘 보이게)
            if (chapter != null) {
                BufferedImage bd = backdrop(chapter, w, h, Ui.deviceScale(g));
                if (bd != null) Ui.blit(g, bd, 0, 0, w, h);
            }
            g.setColor(Theme.c(0xC8A979));
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 16, 16));
            if (chapter == null) return;

            g.setFont(Theme.hand(28));
            g.setColor(Theme.INK);
            g.drawString(chapter.title + " 가는 길", 26, 46);
            // 장식: 물결선과 나침반
            g.setColor(Theme.c(0xBFA073));
            g.setStroke(new BasicStroke(2f));
            squiggle(g, 50, h * 0.22, 130);
            squiggle(g, w - 190, h * 0.78, 130);
            compass(g, w - 66, 64, 34);
            if (!game.isChapterUnlocked(chapter)) {
                g.setFont(Theme.serif(14));
                g.setColor(Theme.INK_SOFT);
                g.drawString("이전 장의 최종 사건을 해결하면 열립니다.", 28, 74);
            }

            List<Point2D.Double> nodes = layout(lessons, w, h);
            // 점선 경로
            g.setColor(Theme.c(0x8A6440));
            g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{0.5f, 8f}, 0));
            for (int i = 0; i + 1 < nodes.size(); i++) {
                Point2D.Double a = nodes.get(i), b = nodes.get(i + 1);
                double my = (a.y + b.y) / 2;
                g.draw(new CubicCurve2D.Double(a.x, a.y, a.x + (b.x - a.x) * 0.1, my, b.x - (b.x - a.x) * 0.1, my, b.x, b.y));
            }
            Lesson cur = game.currentLesson();
            for (int i = 0; i < nodes.size(); i++) {
                Lesson l = lessons.get(i);
                Point2D.Double p = nodes.get(i);
                boolean done = game.isDone(l), unlocked = game.isUnlocked(l);
                boolean isCur = l == cur;
                boolean hov = i == hover;
                if (l.boss) drawBoss(g, p, l, done, unlocked, hov);
                else if (isCur) drawCurrent(g, p, l, hov);
                else drawNode(g, p, l, done, unlocked, hov);
            }
        }

        /** 지금 사건 위에 서 있는 헤이즐(방금 사건을 풀었다면 길을 따라 깡충깡충 걸어간다). */
        private void drawHazel(Graphics2D g, Lesson cur) {
            marker = null;
            BufferedImage face = Ui.img("hazel_face");
            int ci = cur == null ? -1 : lessons.indexOf(cur);
            if (face == null || nodes.isEmpty()) return;
            long now = System.currentTimeMillis();
            double t = now / 1000.0;
            Point2D.Double at;
            double lift;
            long el = now - walkStart;
            if (walkIndex >= 0 && el < WALK_MS && walkIndex + 1 < nodes.size()) {
                double k = Math.max(0, el) / (double) WALK_MS, e = k * k * (3 - 2 * k);
                Point2D.Double a = nodes.get(walkIndex), b = nodes.get(walkIndex + 1);
                double my = (a.y + b.y) / 2;
                Point2D.Double c1 = new Point2D.Double(a.x + (b.x - a.x) * 0.1, my), c2 = new Point2D.Double(b.x - (b.x - a.x) * 0.1, my);
                at = cubic(a, c1, c2, b, e);
                lift = Math.abs(Math.sin(k * Math.PI * 7)) * 9; // 깡충깡충
                // 다시 그릴 범위: 두 사건과 그 사이 길 + 헤이즐 머리 위 여유
                Rectangle2D box = new Line2D.Double(a, b).getBounds2D();
                walkArea = new Rectangle((int) box.getX() - 48, (int) box.getY() - 130, (int) box.getWidth() + 96, (int) box.getHeight() + 150);
                // 발자국
                g.setColor(new Color(138, 100, 64, 120));
                for (int i = 1; i <= 6; i++) {
                    double kk = e - i * 0.06;
                    if (kk <= 0) break;
                    Point2D.Double f = cubic(a, c1, c2, b, kk);
                    g.fill(new Ellipse2D.Double(f.x - 3 + (i % 2 == 0 ? 4 : -4), f.y - 2, 6, 4));
                }
            } else {
                if (walkIndex >= 0 && el >= WALK_MS) { walkIndex = -1; walkArea = null; }
                if (ci < 0 || ci >= nodes.size()) return;
                at = nodes.get(ci);
                lift = Math.abs(Math.sin(t * 2.2)) * 4;
            }
            double size = 50, y = at.y - 50 - size - lift;
            marker = new Rectangle((int) (at.x - size / 2) - 8, (int) (at.y - 50 - size - 14) - 8, (int) size + 16, (int) size + 40);
            // 그림자: 높이 뛸수록 작고 옅어진다
            double sh = 1 - lift / 30;
            g.setColor(new Color(0, 0, 0, (int) (50 * sh)));
            g.fill(new Ellipse2D.Double(at.x - 16 * sh, at.y - 54 + 4 + (1 - sh) * 4, 32 * sh, 8 * sh));
            // 얼굴 메달은 화면 해상도로 한 번만 그려 두고, 매 프레임 픽셀 그대로 복사만 한다
            BufferedImage tok = token(g, face, size);
            double pad = TOKEN_PAD;
            Ui.blit(g, tok, at.x - size / 2 - pad, y - pad, size + pad * 2, size + pad * 2);
        }

        private static final double TOKEN_PAD = 5;
        private BufferedImage tokenImg;
        private double tokenScale;
        private boolean tokenDark;

        /** 흰 테두리·금색 테두리·얼굴을 합친 헤이즐 메달(화면 배율 크기). */
        private BufferedImage token(Graphics2D like, BufferedImage face, double size) {
            double sc = Ui.deviceScale(like);
            if (tokenImg != null && tokenScale == sc && tokenDark == Theme.isDark()) return tokenImg;
            double pad = TOKEN_PAD;
            BufferedImage im = Ui.deviceImage(like, (int) Math.ceil(size + pad * 2), (int) Math.ceil(size + pad * 2));
            Graphics2D g = Ui.cacheGraphics(im, like);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double x = pad, y = pad;
            Ellipse2D ring = new Ellipse2D.Double(x - 3, y - 3, size + 6, size + 6);
            g.setColor(new Color(0xFFFAF0));
            g.fill(ring);
            // 얼굴은 원 안쪽만 보이게 자른다(꼬리가 원 밖으로 튀어나오지 않게). 가장자리를 부드럽게 하려고
            // 모양 자르기 대신 원(바탕색)을 먼저 그리고 그 위에 SRC_ATOP 으로 얼굴을 얹는다 — 원 밖은 투명해진다.
            BufferedImage faceLayer = new BufferedImage(im.getWidth(), im.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D fg = Theme.aa(faceLayer.createGraphics());
            fg.scale(sc, sc);
            fg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            fg.setColor(new Color(0xFFFAF0));
            fg.fill(ring);
            fg.setComposite(AlphaComposite.SrcAtop);
            Ui.drawScaled(fg, face, x, y, size, size);
            fg.dispose();
            Graphics2D d = (Graphics2D) g.create();
            d.setTransform(new java.awt.geom.AffineTransform());
            d.drawImage(faceLayer, 0, 0, null);
            d.dispose();
            // 금테는 얼굴 위에 둘러서 잘린 가장자리를 덮는다
            g.setColor(Theme.GOLD);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(ring);
            g.dispose();
            tokenImg = im;
            tokenScale = sc;
            tokenDark = Theme.isDark();
            return im;
        }

        private Point2D.Double cubic(Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, Point2D.Double p3, double t) {
            double u = 1 - t;
            return new Point2D.Double(
                    u * u * u * p0.x + 3 * u * u * t * p1.x + 3 * u * t * t * p2.x + t * t * t * p3.x,
                    u * u * u * p0.y + 3 * u * u * t * p1.y + 3 * u * t * t * p2.y + t * t * t * p3.y);
        }

        private void squiggle(Graphics2D g, double x, double y, double len) {
            Path2D p = new Path2D.Double();
            p.moveTo(x, y);
            for (double t = 0; t <= len; t += 4) p.lineTo(x + t, y + Math.sin(t / len * Math.PI * 2) * 10);
            g.draw(p);
        }

        private void compass(Graphics2D g, double cx, double cy, double r) {
            g.setColor(Theme.c(0xA0805A));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            g.draw(new Ellipse2D.Double(cx - r + 5, cy - r + 5, r * 2 - 10, r * 2 - 10));
            Path2D s = new Path2D.Double();
            s.moveTo(cx, cy - r + 4); s.lineTo(cx + 5, cy); s.lineTo(cx, cy + r - 4); s.lineTo(cx - 5, cy); s.closePath();
            g.setColor(new Color(0xA33A28));
            g.fill(s);
            Path2D s2 = new Path2D.Double();
            s2.moveTo(cx - r + 4, cy); s2.lineTo(cx, cy + 4); s2.lineTo(cx + r - 4, cy); s2.lineTo(cx, cy - 4); s2.closePath();
            g.setColor(Theme.c(0x8A6440));
            g.fill(s2);
            g.setFont(Theme.serif(9));
            Theme.drawCentered(g, "N", cx, cy - r - 7);
        }

        private void tag(Graphics2D g, double x, double y, String text, boolean dark, boolean alignRight) {
            g.setFont(dark ? Theme.bold(14) : Theme.serif(14.5f));
            FontMetrics fm = g.getFontMetrics();
            double tw = fm.stringWidth(text) + 22, th = 32;
            double tx = alignRight ? x - tw : x;
            Shape s = new RoundRectangle2D.Double(tx, y - th / 2, tw, th, 8, 8);
            g.setColor(dark ? new Color(0x3F2B1B) : Theme.c(0xF8EFDC));
            g.fill(s);
            if (!dark) {
                g.setColor(Theme.c(0xCDB48A));
                g.setStroke(new BasicStroke(1.4f));
                g.draw(s);
            }
            g.setColor(dark ? Theme.CREAM : Theme.INK);
            g.drawString(text, (float) (tx + 11), (float) (y + (fm.getAscent() - fm.getDescent()) / 2.0));
        }

        private void drawNode(Graphics2D g, Point2D.Double p, Lesson l, boolean done, boolean unlocked, boolean hov) {
            double r = hov ? 36 : 33;
            if (done) {
                g.setColor(Theme.GREEN_DARK);
                g.fill(new Ellipse2D.Double(p.x - r, p.y - r + 5, r * 2, r * 2));
                g.setColor(Theme.GREEN);
                g.fill(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
                g.setColor(Theme.GOLD);
                g.setStroke(new BasicStroke(4f));
                g.draw(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
                Icons.paint(g, "check", p.x - 15, p.y - 15, 30, Theme.CREAM);
            } else {
                g.setColor(unlocked ? Theme.c(0xF2E3BF) : Theme.c(0xE2D0A8));
                g.fill(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
                g.setColor(Theme.c(0xB99A6A));
                g.setStroke(new BasicStroke(3.5f));
                g.draw(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
                Icons.paint(g, unlocked ? "search" : "lock", p.x - 13, p.y - 13, 26, Theme.c(0x8A6A48));
            }
            boolean right = p.x < getWidth() * 0.6;
            tag(g, right ? p.x + r + 14 : p.x - r - 14, p.y, l.title, false, !right);
        }

        private void drawCurrent(Graphics2D g, Point2D.Double p, Lesson l, boolean hov) {
            double r = hov ? 46 : 43;
            g.setColor(new Color(163, 58, 40, 40));
            g.fill(new Ellipse2D.Double(p.x - r - 16, p.y - r - 16, (r + 16) * 2, (r + 16) * 2));
            g.setColor(Theme.RED_DARK);
            g.fill(new Ellipse2D.Double(p.x - r, p.y - r + 6, r * 2, r * 2));
            g.setColor(Theme.RED);
            g.fill(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
            g.setColor(Theme.GOLD);
            g.setStroke(new BasicStroke(4.5f));
            g.draw(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
            Icons.paint(g, "compass", p.x - 18, p.y - 18, 36, Theme.CREAM);

            // 말풍선 카드
            int np = game.content.puzzlesOf(l).size();
            boolean right = p.x < getWidth() * 0.55;
            double cw = 176, ch = 92;
            double cx = right ? p.x + r + 14 : p.x - r - 14 - cw, cy = p.y - ch / 2;
            Shape card = new RoundRectangle2D.Double(cx, cy, cw, ch, 8, 8);
            g.setColor(Theme.c(0xFFFAF0));
            g.fill(card);
            g.setColor(Theme.RED);
            g.setStroke(new BasicStroke(2f));
            g.draw(card);
            g.setFont(Theme.tracked(Theme.serif(11.5f), 0.08f));
            g.drawString("지금 풀기 · 수수께끼", (float) cx + 14, (float) cy + 24);
            g.setFont(Theme.xbold(17));
            g.setColor(Theme.INK);
            g.drawString(l.title, (float) cx + 14, (float) cy + 51);
            g.setFont(Theme.serif(12.5f));
            g.setColor(Theme.INK_SOFT);
            g.drawString("퍼즐 " + np + "개 · 약 " + l.minutes + "분", (float) cx + 14, (float) cy + 76);
        }

        private void drawBoss(Graphics2D g, Point2D.Double p, Lesson l, boolean done, boolean unlocked, boolean hov) {
            double r = hov ? 50 : 46;
            Path2D d = new Path2D.Double();
            d.moveTo(p.x, p.y - r); d.lineTo(p.x + r, p.y); d.lineTo(p.x, p.y + r); d.lineTo(p.x - r, p.y); d.closePath();
            g.setColor(new Color(0xE9C877));
            g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(d);
            g.setColor(done ? Theme.GREEN : unlocked ? Theme.RED : new Color(0x4A3424));
            g.fill(d);
            g.setColor(new Color(0x3A2819));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(d);
            Icons.paint(g, done ? "star_fill" : "star", p.x - 18, p.y - 18, 36, Theme.GOLD);
            tag(g, p.x + r + 10, p.y, "최종 사건 · " + l.caseName, true, false);
        }
    }

    // ------------------------------------------------------------------ 사건 카드 팝업
    private void openLesson(Lesson l) {
        if (!game.isUnlocked(l)) {
            frame.toast("이전 사건을 먼저 해결해야 열 수 있습니다.");
            return;
        }
        Chapter ch = game.content.chapterOf(l);
        boolean done = game.isDone(l);
        Ui.Modal m = Ui.modal(frame, (l.boss ? "최종 사건 · " : "사건 " + l.order + " · ") + l.caseName, 520);
        m.add(Ui.label(ch.label + "  ·  " + l.title + "  ·  " + l.location, Theme.serif(14), Theme.INK_SOFT));
        m.add((JComponent) Ui.vgap(14));
        JPanel memo = Ui.vbox();
        for (String s : l.memo) {
            JLabel b = Ui.label("•  " + s, Theme.serif(15), Theme.INK);
            b.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            memo.add(b);
        }
        m.add(memo);
        m.add((JComponent) Ui.vgap(12));
        String info = "수수께끼 " + game.content.puzzlesOf(l).size() + "개 · 약 " + l.minutes + "분 · 보상 " + l.xp + " XP";
        if (done) {
            geomystery.data.SaveData.LessonRecord r = game.save.progress.get(l.id);
            info += "\n최고 정확도 " + r.bestAcc + "% · 최단 기록 " + Ui.time(r.bestTime) + " (다시 풀면 경험치 절반)";
        }
        m.text(info);
        m.button("닫기", Btn.Style.OUTLINE, null);
        if (done) m.button("바로 풀기", Btn.Style.OUTLINE, () -> frame.startLesson(l, true));
        m.button(done ? "다시 풀기" : "사건 시작", Btn.Style.PRIMARY, () -> frame.startLesson(l, false));
        m.show();
    }

    // ------------------------------------------------------------------ 장 넘기기 화살표
    /** 지도 옆에 붙는 큰 연갈색 화살표. 넘길 장이 없으면 흐려지고 눌리지 않는다. */
    private static final class SideArrow extends JComponent {
        static final int W = 48, H = 104;
        private final int dir;
        private boolean hover, press;

        SideArrow(int dir, Runnable action) {
            this.dir = dir;
            setOpaque(false);
            setToolTipText(dir < 0 ? "이전 장" : "다음 장");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; press = false; repaint(); }
                @Override public void mousePressed(MouseEvent e) { if (isEnabled()) { press = true; repaint(); } }
                @Override public void mouseReleased(MouseEvent e) {
                    boolean fire = press && isEnabled() && contains(e.getPoint());
                    press = false;
                    repaint();
                    if (fire) action.run();
                }
            };
            addMouseListener(ma);
        }

        @Override
        public void setEnabled(boolean b) {
            super.setEnabled(b);
            setCursor(Cursor.getPredefinedCursor(b ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            boolean on = isEnabled();
            if (!on) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            double dy = on && press ? 2 : 0;
            // 큰 연갈색 화살표 하나(테두리·바탕 없이). 클릭 범위는 이 칸 전체.
            double cx = w / 2.0, cy = h / 2.0 + dy, ax = 11, ay = 28;
            Path2D chev = new Path2D.Double();
            chev.moveTo(cx - dir * ax, cy - ay);
            chev.lineTo(cx + dir * ax, cy);
            chev.lineTo(cx - dir * ax, cy + ay);
            BasicStroke st = new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            g.setStroke(st);
            if (!(on && press)) {
                g.setColor(new Color(90, 60, 30, 55)); // 살짝 그림자
                g.translate(0, 2.5);
                g.draw(chev);
                g.translate(0, -2.5);
            }
            g.setColor(on && hover ? Theme.c(0xB08B5E) : Theme.c(0xC9A57A));
            g.draw(chev);
            g.dispose();
        }
    }
}
