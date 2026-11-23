package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;
import geomystery.game.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 01.5 도입 대화 — 비주얼 노벨처럼 NPC와 대화하며 사건을 소개한다. */
public class DialogueScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;

    private Lesson lesson;
    private List<Line> lines = new ArrayList<>();
    private int index;
    private float shown;
    private final javax.swing.Timer typer;
    private final javax.swing.Timer anim;
    private long animStart = System.currentTimeMillis();

    private final JLabel topSmall, topTitle;
    private final Dots dots;
    private final Scene scene;
    private final DialogBox box;
    private final Memo memo;
    private final Btn prevBtn, goBtn;

    public DialogueScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.DARK);

        // ---------- 상단 바
        JPanel top = new DarkBar(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(14, 30, 14, 30));
        JPanel tl = Ui.hbox();
        Btn back = new Btn("", Btn.Style.CIRCLE_DARK, "back");
        back.setToolTipText("탐험 지도로 돌아가기");
        back.addActionListener(e -> frame.showTab(epilogueOf != null ? "office" : "map"));
        tl.add(back);
        tl.add(Ui.hgap(18));
        JPanel tt = Ui.vbox();
        topSmall = Ui.label("", Theme.tracked(Theme.serif(12.5f), 0.16f), new Color(0xD8C39A));
        topTitle = Ui.label("", Theme.xbold(22), Theme.CREAM);
        tt.add(topSmall);
        tt.add(Ui.vgap(2));
        tt.add(topTitle);
        tl.add(tt);
        top.add(tl, BorderLayout.WEST);
        JPanel tr = Ui.hbox();
        dots = new Dots();
        tr.add(dots);
        tr.add(Ui.hgap(18));
        Btn skip = new Btn("대화 건너뛰기", Btn.Style.DARK_OUTLINE, "skip").font(14);
        skip.addActionListener(e -> toPuzzles());
        tr.add(skip);
        JPanel trWrap = Ui.clear(new GridBagLayout());
        trWrap.add(tr);
        top.add(trWrap, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // ---------- 장면
        scene = new Scene();
        add(scene, BorderLayout.CENTER);

        // ---------- 하단: 대화창 + 수첩
        JPanel bottom = Ui.clear(new GridBagLayout());
        bottom.setOpaque(true);
        bottom.setBackground(Theme.DARK);
        bottom.setBorder(BorderFactory.createEmptyBorder(20, 32, 26, 32));
        bottom.setPreferredSize(new Dimension(100, 340));
        GridBagConstraints c = new GridBagConstraints();

        JPanel leftCol = Ui.clear(new BorderLayout(0, 14));
        box = new DialogBox();
        leftCol.add(box, BorderLayout.CENTER);
        JPanel under = Ui.clear(new BorderLayout());
        prevBtn = new Btn("이전 대사", Btn.Style.DARK_OUTLINE).font(14);
        prevBtn.addActionListener(e -> prev());
        under.add(prevBtn, BorderLayout.WEST);
        JPanel ur = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        JLabel hint = Ui.label("대화창을 클릭하면 다음으로 넘어갑니다", Theme.serif(13.5f), new Color(0xD8C39A));
        ur.add(hint);
        goBtn = new Btn("수수께끼 풀러 가기", Btn.Style.PRIMARY, "next").font(15);
        goBtn.addActionListener(e -> toPuzzles());
        ur.add(goBtn);
        under.add(ur, BorderLayout.EAST);
        leftCol.add(under, BorderLayout.SOUTH);

        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 24);
        bottom.add(leftCol, c);
        memo = new Memo();
        c.gridx = 1; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0);
        bottom.add(memo, c);
        add(bottom, BorderLayout.SOUTH);

        typer = new javax.swing.Timer(16, e -> tick());
        anim = new javax.swing.Timer(33, e -> { if (isShowing()) { scene.repaint(); box.repaint(); } });

        bindKey("SPACE", this::next);
        bindKey("ENTER", this::next);
        bindKey("RIGHT", this::next);
        bindKey("LEFT", this::prev);
        bindKey("ESCAPE", () -> frame.showTab(epilogueOf != null ? "office" : "map"));
        bindKey("pressed S", () -> { if (!lines.isEmpty()) toPuzzles(); });
    }

    private void bindKey(String key, Runnable r) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (isShowing()) r.run(); }
        });
    }

    public void start(Lesson l) { start(l, false); }

    /**
     * 대화를 준비한다. waitForCard 이면 장 표지가 사라질 때까지 대사를 쓰기 시작하지 않는다
     * (표지가 끝나면 beginTyping()).
     */
    public void start(Lesson l, boolean waitForCard) {
        epilogueOf = null;
        begin(l, game.content.dialogueOf(l), waitForCard);
    }

    /** 후일담을 보고 있으면 그 인물(끝나면 탐정 사무소로 돌아간다). */
    private Npc epilogueOf;

    /** 인물의 후일담(호감도를 가득 채우면 열린다)을 대화 화면으로 보여 준다. */
    public void startEpilogue(Npc n) {
        Chapter ch = game.content.chapterOfNpc(n.id);
        List<Line> ls = game.content.epilogues.get(n.id);
        if (ch == null || ls == null || ls.isEmpty()) return;
        Lesson fake = new Lesson();
        fake.id = "E:" + n.id;
        fake.chapterId = ch.id;
        fake.npc = n.image;
        fake.scene = ch.scene;
        fake.location = n.name + "의 후일담";
        fake.caseName = "후일담";
        fake.memo = new ArrayList<>(Arrays.asList("인물: " + n.name, "소개: " + n.role, "만난 곳: " + ch.title, "호감도: 가득 — 마음을 열었다"));
        epilogueOf = n;
        begin(fake, ls, false);
        topSmall.setText(ch.label + "  ·  후일담");
    }

    private void begin(Lesson l, List<Line> ls, boolean waitForCard) {
        lesson = l;
        lines = ls;
        Chapter ch = game.content.chapterOf(l);
        topSmall.setText(ch.label + "  ·  " + (l.boss ? "최종 사건" : "사건 " + l.order));
        topTitle.setText(l.location);
        goBtn.setText(epilogueOf != null ? "사무소로 돌아가기" : "수수께끼 풀러 가기");
        Npc client = game.content.npc(l.npc);
        if (epilogueOf == null && client != null) memo.set(l, client.name, game.hearts(client.id));
        else memo.set(l, null, -1);
        scene.setLesson(l);
        index = 0;
        shown = 0;
        animStart = System.currentTimeMillis();
        anim.start();
        updateControls();
        dots.revalidate();
        if (!waitForCard) typer.start();
    }

    /** 장 표지가 끝난 뒤 대사를 쓰기 시작한다. */
    public void beginTyping() {
        animStart = System.currentTimeMillis();
        lineStart = System.currentTimeMillis();
        typer.start();
    }

    /**
     * 시작 직후 백그라운드에서 장면 배경과 인물 그림을 지금 창 크기에 맞게 미리 줄여 둔다.
     * 그래서 사건에 처음 들어갈 때 화면이 멈추지 않는다.
     */
    public void prewarm() {
        int w = scene.getWidth(), h = scene.getHeight();
        if (w <= 0 || h <= 0) return;
        double sc = 1;
        try { sc = Math.max(1, getGraphicsConfiguration().getDefaultTransform().getScaleX()); } catch (Exception ignored) { }
        final double ds = sc;
        // 지금 진행 중인 장의 장면과 그 장에 나오는 인물부터 준비한다(곧 들어갈 가능성이 가장 높다)
        Chapter now = game.currentChapter();
        java.util.List<String> scenes = new ArrayList<>();
        scenes.add(now.scene);
        for (Chapter ch : game.content.chapters) if (!scenes.contains(ch.scene)) scenes.add(ch.scene);
        java.util.List<String> people = new ArrayList<>();
        people.add("hazel");
        for (Lesson l : game.content.lessonsOf(now)) {
            for (Line ln : game.content.dialogueOf(l)) {
                Npc n = game.content.npc(ln.speaker);
                if (n != null && !people.contains(n.image)) people.add(n.image);
            }
        }
        for (Npc n : game.content.npcs.values()) if (!people.contains(n.image)) people.add(n.image);
        // 실제 화면 배율과, 화면 전환 사진(1배)용 두 가지를 만든다
        double[] scales = ds == 1 ? new double[]{1} : new double[]{ds, 1};
        Thread t = new Thread(() -> {
            for (double sc2 : scales) {
                for (String sname : scenes) {
                    BufferedImage bg = Ui.img("bg_" + sname);
                    if (bg == null) continue;
                    double s = Math.max(w / (double) bg.getWidth(), h / (double) bg.getHeight());
                    Ui.prescale(bg, (int) (bg.getWidth() * s), (int) (bg.getHeight() * s), sc2, false);
                }
                for (String id : people) {
                    BufferedImage img = Ui.img(id);
                    if (img == null) continue;
                    double ch = h * 1.02, cw = ch * img.getWidth() / img.getHeight();
                    // 말하는 크기(1.03)와 듣는 크기(0.98, 흐린 그림): 헤이즐은 왼쪽(그대로), 나머지는 오른쪽(좌우 반전)
                    boolean mirror = !"hazel".equals(id);
                    Ui.prescale(img, Math.round(cw * 1.03), Math.round(ch * 1.03), sc2, mirror);
                    BufferedImage dimmedImg = scene.dim(id);
                    Ui.prescale(dimmedImg, Math.round(cw * 0.98), Math.round(ch * 0.98), sc2, mirror);
                }
            }
        }, "geomystery-prewarm");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    private void tick() {
        if (lines.isEmpty()) return;
        String t = lines.get(index).text;
        if (shown < t.length()) {
            int sp = game.save.typingSpeed;
            int before = (int) shown;
            shown += sp == 1 ? 0.45f : sp == 3 ? 2.2f : 1f;
            // 타자기 소리: 두세 글자마다 한 번(공백 제외)
            int now = (int) Math.min(shown, t.length());
            if (now / 3 != before / 3 && now > 0 && !Character.isWhitespace(t.charAt(now - 1))) Sfx.play("type");
            box.repaint();
        }
    }

    private boolean typing() {
        return !lines.isEmpty() && shown < lines.get(index).text.length();
    }

    private void next() {
        if (lines.isEmpty()) { toPuzzles(); return; }
        if (typing()) {
            shown = lines.get(index).text.length();
            box.repaint();
            return;
        }
        if (index < lines.size() - 1) {
            index++;
            shown = 0;
            updateControls();
        } else {
            toPuzzles();
        }
    }

    private void prev() {
        if (index > 0) {
            index--;
            shown = lines.get(index).text.length();
            updateControls();
        }
    }

    /** 지금 대사가 시작된 시각(표정 연출용). */
    private long lineStart = System.currentTimeMillis();

    private void updateControls() {
        lineStart = System.currentTimeMillis();
        prevBtn.setEnabled(index > 0);
        goBtn.setVisible(index >= lines.size() - 1);
        dots.repaint();
        scene.repaint();
        box.repaint();
    }

    private void toPuzzles() {
        typer.stop();
        anim.stop();
        if (epilogueOf != null) {
            game.save.epiloguesSeen.add(epilogueOf.id);
            game.save.save();
            epilogueOf = null;
            frame.showTab("office");
            return;
        }
        if (lesson == null) return;
        frame.startPuzzles(new Session(lesson, game.content.puzzlesOf(lesson), Session.Mode.LESSON));
    }

    private Line current() { return lines.isEmpty() ? null : lines.get(index); }

    // ------------------------------------------------------------------ 구성 요소
    static class DarkBar extends JPanel {
        DarkBar(LayoutManager lm) { super(lm); setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            g.setColor(Theme.DARK);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.GOLD_DARK);
            g.fillRect(0, getHeight() - 5, getWidth(), 2);
            g.setColor(new Color(0x6E5236));
            g.fillRect(0, getHeight() - 2, getWidth(), 2);
            g.dispose();
        }
    }

    private class Dots extends JComponent {
        Dots() { setPreferredSize(new Dimension(160, 20)); }

        @Override public Dimension getPreferredSize() { return new Dimension(Math.max(20, lines.size() * 16), 20); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            for (int i = 0; i < lines.size(); i++) {
                g.setColor(i == index ? Theme.CREAM : i < index ? new Color(0xB59C78) : new Color(0x6E5638));
                g.fill(new Ellipse2D.Double(i * 16 + 2, 5, 10, 10));
            }
            g.dispose();
        }
    }

    private class Scene extends JComponent {
        private BufferedImage bg;
        private String leftId = "hazel", rightId = "gregor";
        /** 말하지 않는 인물의 흐린 그림(미리 만들기 스레드와 함께 쓰므로 동시 접근 가능한 맵). */
        private final Map<String, BufferedImage> dimmed = new java.util.concurrent.ConcurrentHashMap<>();

        Scene() {
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { next(); }
            });
        }

        private String scene;
        private boolean clockStopped;

        void setLesson(Lesson l) {
            bg = Ui.img("bg_" + l.scene);
            scene = l.scene;
            rightId = l.npc;
            // 시계탑 최종 사건을 풀기 전에는 대시계가 10시에 멈춰 있다(대사 내용과 맞춘다)
            clockStopped = l.boss && "clock".equals(l.scene) && !game.isDone(l);
        }

        /** 오른쪽에 설 인물: 지금까지 마지막으로 말한 헤이즐 아닌 인물(그림이 있을 때), 없으면 사건 의뢰인. */
        private String rightSpeaker() {
            String r = lesson == null ? rightId : lesson.npc;
            for (int i = 0; i <= index && i < lines.size(); i++) {
                String sp = lines.get(i).speaker;
                if (sp == null || "hazel".equals(sp) || "narration".equals(sp)) continue;
                Npc n = game.content.npc(sp);
                if (n != null && Ui.img(n.image) != null) r = n.image;
            }
            return r;
        }

        private BufferedImage dim(String id) {
            BufferedImage orig = Ui.img(id);
            if (orig == null) return null;
            return dimmed.computeIfAbsent(id + "@" + System.identityHashCode(orig), k -> {
                BufferedImage src = orig;
                BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = copy.createGraphics();
                g.drawImage(src, 0, 0, null);
                g.dispose();
                return new RescaleOp(new float[]{0.62f, 0.58f, 0.55f, 1f}, new float[4], null).filter(copy, null);
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            g.setColor(new Color(0x5A3A22));
            g.fillRect(0, 0, w, h);
            if (bg != null) {
                double s = Math.max(w / (double) bg.getWidth(), h / (double) bg.getHeight());
                int bw = (int) (bg.getWidth() * s), bh = (int) (bg.getHeight() * s);
                Ui.drawScaled(g, bg, (w - bw) / 2.0, h - bh, bw, bh);
            }
            double s0 = bg == null ? 1 : Math.max(w / (double) bg.getWidth(), h / (double) bg.getHeight());
            double bx0 = bg == null ? 0 : (w - bg.getWidth() * s0) / 2, by0 = bg == null ? 0 : h - bg.getHeight() * s0;
            double bs = bg == null ? 1 : s0 * bg.getWidth() / 1600.0;
            double tt = System.currentTimeMillis() / 1000.0;
            if (bg != null) SceneFx.behind(g, scene, bx0, by0, bs, tt);
            if (bg != null && "clock".equals(scene)) drawClockHands(g, bx0, by0, bs);
            timeOfDay(g, w, h);
            Line cur = current();
            String speaker = cur == null ? "" : cur.speaker;
            rightId = rightSpeaker();
            Npc rn = game.content.npc(speaker);
            if (rn != null && rn.image.equals(rightId)) speaker = rightId;
            double t = (System.currentTimeMillis() - animStart) / 1000.0;
            String emo = cur == null ? "" : cur.emotion;
            drawChar(g, leftId, w * 0.2, h, speaker.equals(leftId), t, false, speaker.equals(leftId) ? emo : "");
            drawChar(g, rightId, w * 0.78, h, speaker.equals(rightId), t + 1.3, true, speaker.equals(rightId) ? emo : "");
            if (bg != null) SceneFx.front(g, scene, bx0, by0, bs, w, h, tt);
            // 아래쪽을 하단 패널과 자연스럽게 잇는 그라데이션
            g.setPaint(new GradientPaint(0, h - 40, new Color(58, 40, 25, 0), 0, h, new Color(58, 40, 25, 230)));
            g.fillRect(0, h - 40, w, 40);
            g.dispose();
        }

        /** 배경 속 대시계(굽기 좌표 800,250)에 실제 시각의 바늘을 그린다. */
        private void drawClockHands(Graphics2D g, double x0, double y0, double s) {
            double cx = x0 + 800 * s, cy = y0 + 250 * s;
            java.time.LocalTime now = java.time.LocalTime.now();
            double sec = clockStopped ? 0 : now.getSecond() + now.getNano() / 1e9;
            double min = clockStopped ? 0 : now.getMinute() + sec / 60.0;
            double hr = clockStopped ? 10 : (now.getHour() % 12) + min / 60.0;
            Color ink = new Color(0x3A2819);
            hand(g, cx, cy, hr * 30, 100 * s, 11 * s, ink);
            hand(g, cx, cy, min * 6, 145 * s, 8 * s, ink);
            if (!clockStopped) hand(g, cx, cy, Math.floor(sec) * 6, 160 * s, 2.5 * s, new Color(0xA8382A));
            g.setColor(new Color(0xA8382A));
            g.fill(new Ellipse2D.Double(cx - 18 * s, cy - 18 * s, 36 * s, 36 * s));
            g.setColor(ink);
            g.setStroke(new BasicStroke((float) (4 * s)));
            g.draw(new Ellipse2D.Double(cx - 18 * s, cy - 18 * s, 36 * s, 36 * s));
        }

        private void hand(Graphics2D g, double cx, double cy, double deg, double len, double width, Color c) {
            double a = Math.toRadians(deg - 90);
            g.setColor(c);
            g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx, cy, cx + Math.cos(a) * len, cy + Math.sin(a) * len));
        }

        /** 실제 시각에 따라 새벽·노을·밤 빛을 덧씌운다. */
        /** 시간대 빛(새벽·노을·밤 그늘과 등불빛)은 시간대·크기가 바뀔 때만 다시 그린다. 반짝이는 별만 매 프레임. */
        private BufferedImage tintCache;
        private String tintKey;

        private void timeOfDay(Graphics2D g, int w, int h) {
            int hr = java.time.LocalTime.now().getHour();
            boolean indoor = "clock".equals(scene) || "observatory".equals(scene);
            int period = hr >= 5 && hr < 8 ? 1 : hr >= 17 && hr < 20 ? 2 : (hr >= 20 || hr < 5) ? 3 : 0;
            if (period == 0) return;
            String base = period + ":" + indoor + ":" + w + "x" + h + "@";
            String key = base + Ui.deviceScale(g);
            boolean reuse = Ui.offscreen(g) && tintCache != null && tintKey != null && tintKey.startsWith(base);
            if (!key.equals(tintKey) && !reuse) {
                tintCache = Ui.deviceImage(g, w, h);
                Graphics2D t = Ui.cacheGraphics(tintCache, g);
                if (period == 1) {
                    t.setPaint(new GradientPaint(0, 0, new Color(255, 170, 150, 60), 0, h, new Color(255, 220, 160, 10)));
                    t.fillRect(0, 0, w, h);
                } else if (period == 2) {
                    t.setPaint(new GradientPaint(0, 0, new Color(255, 120, 40, indoor ? 40 : 75), 0, h, new Color(140, 60, 90, indoor ? 20 : 45)));
                    t.fillRect(0, 0, w, h);
                } else {
                    t.setColor(new Color(20, 30, 70, indoor ? 70 : 115));
                    t.fillRect(0, 0, w, h);
                    // 등불의 따뜻한 빛
                    t.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.5f, h * 0.9f), w * 0.5f,
                            new float[]{0f, 1f}, new Color[]{new Color(255, 200, 120, 55), new Color(255, 200, 120, 0)}));
                    t.fillRect(0, 0, w, h);
                }
                t.dispose();
                tintKey = key;
            }
            Ui.blit(g, tintCache, 0, 0, w, h);
            if (period == 3 && !indoor) {
                Random r = new Random(7);
                double tt = System.currentTimeMillis() / 1000.0;
                for (int i = 0; i < 40; i++) {
                    double x = r.nextDouble() * w, y = r.nextDouble() * h * 0.38;
                    float a = (float) (0.35 + 0.35 * Math.sin(tt * 2 + i));
                    g.setColor(new Color(1f, 0.97f, 0.85f, Math.max(0f, a)));
                    double sz = 1.5 + r.nextDouble() * 2;
                    g.fill(new Ellipse2D.Double(x, y, sz, sz));
                }
            }
        }

        private void drawChar(Graphics2D g, String id, double cx, int h, boolean speaking, double t, boolean mirror, String emotion) {
            BufferedImage img = speaking ? Ui.img(id) : dim(id);
            if (img == null) return;
            double ch = h * 1.02;
            double cw = ch * img.getWidth() / img.getHeight();
            double bob = speaking ? Math.sin(t * 3.2) * 3 : Math.sin(t * 1.4) * 1.2;
            double scale = speaking ? 1.03 : 0.98;
            double dw = cw * scale, dh = ch * scale;
            double x = cx - dw / 2, y = h - dh + dh * 0.04 + bob;
            g.setColor(new Color(0, 0, 0, 60));
            g.fill(new Ellipse2D.Double(cx - dw * 0.28, h - dh * 0.035, dw * 0.56, dh * 0.05));
            double since = (System.currentTimeMillis() - lineStart) / 1000.0;
            y += SceneFx.jump(emotion, since);
            Ui.drawScaled(g, img, Math.round(x), y, Math.round(dw), Math.round(dh), mirror);
            double wall = System.currentTimeMillis() / 1000.0;
            if (SceneFx.blinking(id, wall)) SceneFx.blink(g, id, Math.round(x), y, Math.round(dw), Math.round(dh), mirror, !speaking);
            if (!emotion.isEmpty()) {
                double[] hd = SceneFx.head(id);
                double k = Math.round(dw) / 360.0;
                double hx = Math.round(x) + (mirror ? 360 - hd[0] : hd[0]) * k, hy = y + hd[1] * (Math.round(dh) / 480.0);
                SceneFx.emotion(g, emotion, hx, hy, hd[2] * k, since, mirror);
            }
        }
    }

    private class DialogBox extends JComponent {
        DialogBox() {
            setPreferredSize(new Dimension(420, 200));
            setMinimumSize(new Dimension(300, 150));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { next(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            int top = 22;
            Shape outer = new RoundRectangle2D.Float(2, top, w - 4, h - top - 2, 18, 18);
            g.setColor(Theme.c(0xF6EAD0));
            g.fill(outer);
            g.setColor(Theme.GOLD);
            g.setStroke(new BasicStroke(4f));
            g.draw(outer);
            g.setColor(Theme.INK);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new RoundRectangle2D.Float(9, top + 7, w - 18, h - top - 16, 12, 12));

            Line cur = current();
            if (cur == null) { g.dispose(); return; }
            boolean narration = "narration".equals(cur.speaker);
            if (!narration) {
                Npc npc = game.content.npc(cur.speaker);
                String name = npc != null ? npc.name : cur.speaker;
                g.setFont(Theme.xbold(18));
                FontMetrics fm = g.getFontMetrics();
                int tw = fm.stringWidth(name) + 38;
                Shape tab = new RoundRectangle2D.Float(24, 0, tw, 40, 8, 8);
                g.setColor(new Color(0x7B5532));
                g.fill(tab);
                g.setColor(Theme.GOLD);
                g.setStroke(new BasicStroke(2.5f));
                g.draw(tab);
                g.setColor(Theme.CREAM);
                g.drawString(name, 24 + 19, 27);
            }
            String text = cur.text.substring(0, Math.min(cur.text.length(), (int) shown));
            g.setFont(narration ? Theme.serif(19).deriveFont(Font.ITALIC) : Theme.serif(21));
            g.setColor(narration ? Theme.INK_SOFT : Theme.INK);
            FontMetrics fm = g.getFontMetrics();
            List<String> wrapped = Widgets.TextBlock.wrap(cur.text, fm, w - 110);
            int lh = (int) (fm.getHeight() * 1.6);
            int y = top + 30 + Math.max(0, (h - top - 70 - wrapped.size() * lh) / 2) + fm.getAscent();
            int remaining = text.length();
            for (String line : wrapped) {
                if (remaining <= 0) break;
                String part = line.substring(0, Math.min(line.length(), remaining));
                g.drawString(part, 40, y);
                remaining -= line.length() + (cur.text.length() > 0 ? 1 : 0);
                y += lh;
            }
            if (!typing()) {
                double t = (System.currentTimeMillis() - animStart) / 1000.0;
                boolean last = index >= lines.size() - 1;
                if (last) {
                    g.setFont(Theme.bold(15));
                    g.setColor(Theme.RED);
                    String s = "클릭해서 수수께끼 풀기  ▶";
                    g.drawString(s, w - 36 - g.getFontMetrics().stringWidth(s), h - 26);
                } else if (Math.sin(t * 5) > -0.3) {
                    double bx = w - 46, by = h - 40 + Math.sin(t * 5) * 2;
                    Path2D tri = new Path2D.Double();
                    tri.moveTo(bx, by); tri.lineTo(bx + 16, by); tri.lineTo(bx + 8, by + 13); tri.closePath();
                    g.setColor(Theme.RED);
                    g.fill(tri);
                }
            }
            g.dispose();
        }
    }

    private static class Memo extends JComponent {
        private List<String> items = new ArrayList<>();

        Memo() {
            setPreferredSize(new Dimension(372, 300));
            setMinimumSize(new Dimension(320, 200));
        }

        private String client;
        private int hearts = -1;

        void set(Lesson l, String clientName, int h) { items = l.memo; client = clientName; hearts = h; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            g.rotate(Math.toRadians(0.8), w / 2.0, h / 2.0);
            Shape paper = new RoundRectangle2D.Float(8, 4, w - 18, h - 14, 6, 6);
            g.setColor(new Color(0, 0, 0, 50));
            g.translate(3, 4);
            g.fill(paper);
            g.translate(-3, -4);
            g.setColor(Theme.c(0xF3E7CA));
            g.fill(paper);
            g.setColor(Theme.GOLD);
            g.setStroke(new BasicStroke(2.5f));
            g.draw(paper);
            g.setFont(Theme.hand(26));
            g.setColor(Theme.RED);
            g.drawString("헤이즐의 수첩", 30, 46);
            g.setFont(Theme.xbold(19));
            g.setColor(Theme.INK);
            g.drawString("사건 메모", 30, 82);
            g.setFont(Theme.serif(15));
            FontMetrics fm = g.getFontMetrics();
            int y = 120;
            for (String it : items) {
                List<String> ws = Widgets.TextBlock.wrap(it, fm, w - 90);
                g.fill(new Ellipse2D.Double(32, y - 9, 6, 6));
                for (String s : ws) {
                    g.drawString(s, 48, y);
                    y += 26;
                }
                y += 4;
            }
            if (client != null && hearts >= 0) {
                // 의뢰인과의 우정(호감도 하트)
                int hy = Math.max(y + 8, h - 52);
                g.setFont(Theme.serif(13));
                g.setColor(Theme.INK_SOFT);
                String label = client + "와의 우정";
                g.drawString(label, 30, hy);
                double hx = 30 + g.getFontMetrics().stringWidth(label) + 10;
                for (int i = 0; i < Game.HEARTS; i++) {
                    java.awt.geom.Path2D hp = Icons.heart(hx + i * 20 + 8, hy - 5, 7.5);
                    if (i < hearts) { g.setColor(new Color(0xD9534A)); g.fill(hp); }
                    else { g.setColor(new Color(0xC9B48A)); g.setStroke(new BasicStroke(1.6f)); g.draw(hp); }
                }
            }
            g.dispose();
        }
    }
}
