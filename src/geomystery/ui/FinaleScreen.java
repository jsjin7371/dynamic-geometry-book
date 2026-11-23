package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;
import geomystery.game.Result;
import geomystery.game.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * 최종 사건의 마지막 단계 — 용의자들의 증언 중 정리와 모순되는 것을 골라,
 * 그 모순을 증명하는 정리 카드를 제시한다. 틀리면 등불이 하나 꺼진다.
 */
public class FinaleScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private Session session;
    private Lesson lesson;
    private final List<FinaleLine> testimonies = new ArrayList<>();
    private final Set<FinaleLine> exposed = new HashSet<>();
    private FinaleLine pickedLine;
    private Theorem pickedCard;
    private boolean solved, animating;
    private final Widgets.FaceView hazel;
    /** 화자별 얼굴(다시 그려도 표정이 유지되도록 재사용). */
    private final Map<String, Widgets.FaceView> faces = new HashMap<>();
    private final Map<FinaleLine, JComponent> testimonyViews = new HashMap<>();
    private final Map<Theorem, JComponent> cardViews = new HashMap<>();
    private FinaleLine culprit;
    private double shakeX, shakeY;

    private final JLabel topSmall, topTitle, mistakesLabel;
    private final Widgets.Pill lanternPill;
    private final Widgets.TextBlock intro;
    private final JPanel testimonyList;
    private final JPanel cardGrid;
    private final Widgets.TextBlock bubble;
    private final Btn present;
    private final JLabel leftTitle;

    public FinaleScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.DARK);

        JPanel top = new DialogueScreen.DarkBar(new BorderLayout(24, 0));
        top.setBorder(BorderFactory.createEmptyBorder(16, 30, 16, 30));
        JPanel tl = Ui.hbox();
        Btn close = new Btn("", Btn.Style.CIRCLE_DARK, "close");
        close.setToolTipText("그만두기");
        close.addActionListener(e -> confirmQuit());
        tl.add(close);
        tl.add(Ui.hgap(18));
        JPanel tt = Ui.vbox();
        topSmall = Ui.label("", Theme.tracked(Theme.serif(12.5f), 0.16f), new Color(0xD8C39A));
        topTitle = Ui.label("", Theme.xbold(22), Theme.CREAM);
        tt.add(topSmall);
        tt.add(Ui.vgap(2));
        tt.add(topTitle);
        tl.add(tt);
        top.add(tl, BorderLayout.WEST);
        JPanel tr = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        mistakesLabel = Ui.label("", Theme.serif(14), new Color(0xD8C39A));
        tr.add(mistakesLabel);
        lanternPill = new Widgets.Pill("lantern", "", Widgets.Pill.Kind.DARK);
        tr.add(lanternPill);
        JPanel trWrap = Ui.clear(new GridBagLayout());
        trWrap.add(tr);
        top.add(trWrap, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel body = Ui.clear(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints c = new GridBagConstraints();

        // ---------- 왼쪽: 사건 정리 + 증언
        Ui.TrackWidth left = new Ui.TrackWidth(null);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        RoundPanel introCard = new RoundPanel(new BorderLayout(0, 10), new Color(0x4A3524), Theme.GOLD_DARK, 14).borderWidth(2f);
        introCard.pad(18, 22, 18, 22);
        introCard.setAlignmentX(LEFT_ALIGNMENT);
        introCard.add(Ui.label("사건 정리", Theme.hand(26), Theme.GOLD), BorderLayout.NORTH);
        intro = new Widgets.TextBlock("", Theme.serif(15.5f), new Color(0xEFE3C8)).spacing(1.65f).prefWidth(560);
        introCard.add(intro, BorderLayout.CENTER);
        left.add(introCard);
        left.add(Ui.vgap(18));
        leftTitle = Ui.label("", Theme.xbold(19), Theme.CREAM);
        left.add(leftTitle);
        left.add(Ui.vgap(10));
        testimonyList = Ui.vbox();
        testimonyList.setAlignmentX(LEFT_ALIGNMENT);
        left.add(testimonyList);
        JScrollPane ls = Ui.scroll(left);
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 24);
        body.add(ls, c);

        // ---------- 오른쪽: 정리 카드 + 헤이즐 + 제시 버튼
        JPanel right = Ui.clear(new BorderLayout(0, 16));
        right.setPreferredSize(new Dimension(430, 600));
        right.setMinimumSize(new Dimension(380, 300));
        right.add(Ui.label("정리 카드 — 증거로 제시할 카드를 고르세요", Theme.xbold(17), Theme.CREAM), BorderLayout.NORTH);
        cardGrid = new Ui.TrackWidth(new GridLayout(0, 3, 10, 10));
        right.add(Ui.scroll(cardGrid), BorderLayout.CENTER);
        JPanel south = Ui.clear(new BorderLayout(12, 12));
        JPanel hz = Ui.clear(new BorderLayout(12, 0));
        hazel = new Widgets.FaceView("hazel_face", 70);
        hz.add(hazel, BorderLayout.WEST);
        RoundPanel bub = new RoundPanel(new BorderLayout(), Theme.c(0xFFFBF2), Theme.GOLD, 14).borderWidth(2f);
        bub.pad(12, 16, 12, 16);
        bubble = new Widgets.TextBlock("", Theme.serif(14.5f), Theme.INK).spacing(1.55f).prefWidth(300);
        bub.add(bubble, BorderLayout.CENTER);
        hz.add(bub, BorderLayout.CENTER);
        south.add(hz, BorderLayout.CENTER);
        present = new Btn("증거 제시!", Btn.Style.PRIMARY, "search").font(18);
        present.setPreferredSize(new Dimension(200, 60));
        present.addActionListener(e -> onPresent());
        JPanel pw = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pw.add(present);
        south.add(pw, BorderLayout.SOUTH);
        right.add(south, BorderLayout.SOUTH);
        c.gridx = 1; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0);
        body.add(right, c);
        add(body, BorderLayout.CENTER);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "present");
        getActionMap().put("present", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (isShowing()) onPresent(); }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        // 증언: 숫자 키·위아래 화살표, 정리 카드: 좌우 화살표
        for (int d = 1; d <= 9; d++) {
            final int idx = d - 1;
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("typed " + d), "testimony" + d);
            getActionMap().put("testimony" + d, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { if (isShowing()) pickTestimony(idx); }
            });
        }
        String[][] arrows = {{"UP", "-1", "t"}, {"DOWN", "1", "t"}, {"LEFT", "-1", "c"}, {"RIGHT", "1", "c"}};
        for (String[] a : arrows) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(a[0]), "finale" + a[0]);
            getActionMap().put("finale" + a[0], new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    if (!isShowing() || solved || animating) return;
                    int step = Integer.parseInt(a[1]);
                    if ("t".equals(a[2])) pickTestimony(pickedLine == null ? (step > 0 ? 0 : testimonies.size() - 1) : Math.floorMod(testimonies.indexOf(pickedLine) + step, testimonies.size()));
                    else {
                        List<Theorem> hand = game.finaleHand(lesson);
                        if (hand.isEmpty()) return;
                        int i = pickedCard == null ? (step > 0 ? 0 : hand.size() - 1) : Math.floorMod(hand.indexOf(pickedCard) + step, hand.size());
                        pickedCard = hand.get(i);
                        Sfx.play("select");
                        rebuild();
                        JComponent v = cardViews.get(pickedCard);
                        if (v != null) v.scrollRectToVisible(new Rectangle(0, 0, v.getWidth(), v.getHeight()));
                    }
                }
            });
        }
        getActionMap().put("quit", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (isShowing()) confirmQuit(); }
        });
    }

    private void pickTestimony(int i) {
        if (solved || animating || i < 0 || i >= testimonies.size()) return;
        FinaleLine f = testimonies.get(i);
        pickedLine = f;
        Sfx.play("select");
        bubble.setText(speakerName(f.speaker) + "의 증언을 골랐어. 이 말과 모순되는 정리 카드는 무엇일까?");
        rebuild();
    }

    public void start(Session s) {
        session = s;
        lesson = s.lesson;
        solved = false;
        exposed.clear();
        pickedLine = null;
        pickedCard = null;
        testimonies.clear();
        faces.clear();
        culprit = null;
        animating = false;
        StringBuilder sb = new StringBuilder();
        for (FinaleLine f : game.content.finaleOf(lesson)) {
            if (f.isTestimony()) testimonies.add(f);
            else if (FinaleLine.INTRO.equals(f.kind)) sb.append(sb.length() == 0 ? "" : "\n").append(speakerPrefix(f)).append(f.text);
        }
        Chapter ch = game.content.chapterOf(lesson);
        topSmall.setText(ch.label + "  ·  최종 추리");
        topTitle.setText(lesson.caseName + " — 범인은 이 안에 있다");
        intro.setText(sb.toString());
        leftTitle.setText("용의자들의 증언 — 정리와 어긋나는 증언을 하나 고르세요");
        bubble.setText("증언 가운데 우리가 모은 정리와 맞지 않는 말이 숨어 있어. 수상한 증언을 고르고, 그 모순을 증명할 정리 카드를 내밀어 보자!");
        present.setText("증거 제시!");
        present.setStyle(Btn.Style.PRIMARY);
        rebuild();
    }

    /** 화자의 얼굴(용의자는 npcs.csv 의 얼굴 이미지, 없으면 이름 첫 글자). */
    private Widgets.FaceView face(String speaker, int size) {
        return faces.computeIfAbsent(speaker + "@" + size, k -> {
            Npc n = game.content.npc(speaker);
            String name = speakerName(speaker);
            String last = name.contains(" ") ? name.substring(name.lastIndexOf(' ') + 1) : name;
            float hue = (Math.abs(name.hashCode()) % 360) / 360f;
            return new Widgets.FaceView(n != null ? n.image + "_face" : null, size).round(true)
                    .fallback(last.substring(0, 1), Color.getHSBColor(hue, 0.35f, 0.42f));
        });
    }

    private String speakerPrefix(FinaleLine f) {
        if (f.speaker == null || f.speaker.isEmpty() || "narration".equals(f.speaker)) return "";
        return speakerName(f.speaker) + ": ";
    }

    private String speakerName(String id) {
        Npc n = game.content.npc(id);
        return n != null ? n.name : id;
    }

    private void rebuild() {
        lanternPill.set("lantern", "등불 " + game.lanterns());
        mistakesLabel.setText(session.finaleMistakes == 0 ? "보너스 +" + Game.finaleBonus(0) + " XP"
                : "잘못 제시 " + session.finaleMistakes + "회 · 보너스 +" + Game.finaleBonus(session.finaleMistakes) + " XP");
        testimonyList.removeAll();
        testimonyViews.clear();
        cardViews.clear();
        if (!solved) {
            for (int i = 0; i < testimonies.size(); i++) {
                TestimonyCard tc = new TestimonyCard(i + 1, testimonies.get(i));
                testimonyViews.put(testimonies.get(i), tc);
                testimonyList.add(tc);
                testimonyList.add(Ui.vgap(10));
            }
        } else {
            for (FinaleLine f : game.content.finaleOf(lesson)) {
                if (!FinaleLine.REVEAL.equals(f.kind)) continue;
                RoundPanel p = new RoundPanel(new BorderLayout(14, 0), Theme.c(0xF6EAD0), Theme.GOLD, 12).borderWidth(2f);
                p.pad(14, 16, 14, 16);
                p.setAlignmentX(LEFT_ALIGNMENT);
                if (!"narration".equals(f.speaker)) {
                    Widgets.FaceView fv = face(f.speaker, 64);
                    if (culprit != null && f.speaker.equals(culprit.speaker) && fv.mood() != Widgets.FaceView.Mood.CONFESS) {
                        fv.mood(Widgets.FaceView.Mood.CONFESS, 0);
                    }
                    p.add(fv, BorderLayout.WEST);
                }
                JPanel t = Ui.vbox();
                if (!"narration".equals(f.speaker)) {
                    t.add(Ui.label(speakerName(f.speaker), Theme.bold(15), Theme.RED));
                    t.add(Ui.vgap(4));
                }
                t.add(new Widgets.TextBlock(f.text, Theme.serif(15), Theme.INK).spacing(1.6f).prefWidth(460));
                p.add(t, BorderLayout.CENTER);
                p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 40));
                testimonyList.add(p);
                testimonyList.add(Ui.vgap(10));
            }
        }
        testimonyList.revalidate();
        cardGrid.removeAll();
        for (Theorem t : game.finaleHand(lesson)) {
            CardView cv = new CardView(t);
            cardViews.put(t, cv);
            cardGrid.add(cv);
        }
        cardGrid.revalidate();
        repaint();
    }

    private void onPresent() {
        if (session == null || animating) return;
        if (solved) { close(); return; }
        if (pickedLine == null) { bubble.setText("먼저 수상한 증언을 하나 골라 줘."); return; }
        if (pickedCard == null) { bubble.setText("이제 그 증언을 반박할 정리 카드를 골라 줘."); return; }
        boolean lie = FinaleLine.LIE.equals(pickedLine.kind);
        if (lie && pickedCard.id.equals(pickedLine.theoremId)) {
            exposed.add(pickedLine);
            int remaining = 0;
            for (FinaleLine f : testimonies) if (FinaleLine.LIE.equals(f.kind) && !exposed.contains(f)) remaining++;
            if (remaining > 0) {
                Sfx.play("correct");
                bubble.setText("맞아! '" + pickedCard.name + "'에 어긋나는 증언이야. 그런데 아직 수상한 증언이 " + remaining + "개 더 남았어.");
                pickedLine = null;
                pickedCard = null;
                rebuild();
                return;
            }
            culprit = pickedLine;
            session.finaleSolved = true;
            final Theorem card = pickedCard;
            playContradiction(card, pickedLine, () -> ConfessionComic.play(frame, game, lesson, lastBackdrop, () -> {
                solved = true;
                Sfx.play("fanfare");
                hazel.mood(Widgets.FaceView.Mood.HAPPY, 2600);
                leftTitle.setText("진실이 밝혀졌다!");
                bubble.setText("'" + card.name + "' — 이 정리 앞에서는 거짓말이 통하지 않아! 사건 종결이야.");
                present.setText("사건 종결");
                present.setStyle(Btn.Style.GREEN);
                rebuild();
            }));
            return;
        }
        Sfx.play("wrong");
        hazel.mood(Widgets.FaceView.Mood.PUZZLED, 2000);
        lanternPill.flicker();
        game.onFinaleWrong(session);
        if (!lie) {
            bubble.setText(speakerName(pickedLine.speaker) + "의 증언은 우리가 아는 정리와 딱 맞아떨어져. 거짓말이 아니야. 다른 증언을 살펴보자.");
        } else {
            bubble.setText("그 증언이 수상한 건 맞아! 하지만 '" + pickedCard.name + "' 카드로는 모순을 증명할 수 없어. 증언 속 숫자와 관계를 다시 읽어 봐.");
        }
        pickedCard = null;
        rebuild();
        if (game.lanterns() <= 0) SwingUtilities.invokeLater(this::outOfLanterns);
    }

    // ------------------------------------------------------------------ 모순 발견 연출
    @Override
    public void paint(Graphics g) {
        if (shakeX != 0 || shakeY != 0) g.translate((int) shakeX, (int) shakeY);
        super.paint(g);
    }

    /**
     * 증거 카드가 증언으로 날아가 꽂히고, 번쩍임·흔들림과 함께 "모순 발견!" 도장이 찍힌다.
     * 범인의 얼굴은 당황한 표정으로 바뀐다. 클릭하면 건너뛴다.
     */
    /** 모순 발견 연출에서 찍어 둔 뒤 화면(화면 해상도). 이어지는 자백 만화가 다시 찍지 않도록 넘긴다. */
    private java.awt.image.BufferedImage lastBackdrop;

    private void playContradiction(Theorem card, FinaleLine line, Runnable done) {
        JRootPane rp = SwingUtilities.getRootPane(this);
        JComponent from = cardViews.get(card), to = testimonyViews.get(line);
        if (rp == null || from == null || to == null || !isShowing()) { done.run(); return; }
        JLayeredPane lp = rp.getLayeredPane();
        Point a = SwingUtilities.convertPoint(from, from.getWidth() / 2, from.getHeight() / 2, lp);
        Point b = SwingUtilities.convertPoint(to, to.getWidth() / 2, to.getHeight() / 2, lp);
        Widgets.FaceView culpritFace = faces.get(line.speaker + "@72");
        animating = true;
        final long t0 = System.currentTimeMillis();
        final boolean[] hit = {false}, finished = {false};
        // 뒤 화면은 처음에 1배로 한 장 찍어 두고, 첫 프레임에 화면 해상도로 한 번 키워 둔다(이후 복사만)
        int lw = lp.getWidth(), lh = lp.getHeight();
        java.awt.image.BufferedImage back = new java.awt.image.BufferedImage(Math.max(1, lw), Math.max(1, lh), java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D bg0 = back.createGraphics();
        rp.getContentPane().paint(bg0);
        bg0.dispose();
        java.awt.image.BufferedImage[] backDev = {null};
        lastBackdrop = null;
        final double[] shake = {0, 0};
        JComponent overlay = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                long el = System.currentTimeMillis() - t0;
                int w = getWidth(), h = getHeight();
                if (backDev[0] == null) {
                    backDev[0] = Ui.deviceImage(g, w, h);
                    Graphics2D b = Ui.cacheGraphics(backDev[0], g);
                    b.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    b.drawImage(back, 0, 0, w, h, null);
                    b.dispose();
                }
                g.setColor(Theme.DARK);
                g.fillRect(0, 0, w, h);
                Ui.blit(g, backDev[0], Math.round(shake[0]), Math.round(shake[1]), w, h);
                if (el < 560) {
                    // 1) 카드가 포물선을 그리며 날아간다
                    double t = el / 560.0, e = t * t * (3 - 2 * t);
                    double x = a.x + (b.x - a.x) * e, y = a.y + (b.y - a.y) * e - Math.sin(Math.PI * e) * 140;
                    g.setColor(new Color(0, 0, 0, 90));
                    g.fillRect(0, 0, w, h);
                    g.translate(x, y);
                    g.rotate(Math.toRadians(-12 + 372 * e));
                    double sc = 1 + 0.35 * Math.sin(Math.PI * e);
                    g.scale(sc, sc);
                    drawCard(g, card);
                } else {
                    long k = el - 560;
                    // 2) 번쩍임
                    if (k < 260) {
                        g.setColor(new Color(255, 248, 220, (int) (200 * (1 - k / 260.0))));
                        g.fillRect(0, 0, w, h);
                    }
                    float fade = el > 2150 ? Math.max(0f, 1f - (el - 2150) / 350f) : 1f;
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
                    g.setColor(new Color(0, 0, 0, 110));
                    g.fillRect(0, 0, w, h);
                    // 증언 위에 꽂힌 카드
                    Graphics2D c2 = (Graphics2D) g.create();
                    c2.translate(b.x + Math.min(240, to.getWidth() / 2.0 - 70), b.y);
                    c2.rotate(Math.toRadians(8));
                    drawCard(c2, card);
                    c2.dispose();
                    // 집중선
                    double cx = w / 2.0, cy = h / 2.0;
                    g.setColor(new Color(255, 214, 102, 70));
                    for (int i = 0; i < 28; i++) {
                        double ang = i * Math.PI * 2 / 28 + k / 900.0;
                        java.awt.geom.Path2D ray = new java.awt.geom.Path2D.Double();
                        ray.moveTo(cx + Math.cos(ang) * 150, cy + Math.sin(ang) * 90);
                        ray.lineTo(cx + Math.cos(ang - 0.05) * 1400, cy + Math.sin(ang - 0.05) * 1400);
                        ray.lineTo(cx + Math.cos(ang + 0.05) * 1400, cy + Math.sin(ang + 0.05) * 1400);
                        ray.closePath();
                        g.fill(ray);
                    }
                    // 3) 도장
                    double p = Math.min(1, k / 300.0), ease = 1 - Math.pow(1 - p, 3);
                    double s = 2.3 - 1.3 * ease;
                    g.translate(cx, cy);
                    g.rotate(Math.toRadians(-8));
                    g.scale(s, s);
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, p * 1.5) * fade));
                    Color red = new Color(0xC23B2A);
                    java.awt.geom.RoundRectangle2D box = new java.awt.geom.RoundRectangle2D.Double(-230, -66, 460, 132, 22, 22);
                    g.setColor(new Color(255, 246, 232, 235));
                    g.fill(box);
                    g.setColor(red);
                    g.setStroke(new BasicStroke(7f));
                    g.draw(box);
                    g.setStroke(new BasicStroke(2.5f));
                    g.draw(new java.awt.geom.RoundRectangle2D.Double(-218, -54, 436, 108, 16, 16));
                    g.setFont(Theme.xbold(70 / Theme.scale()));
                    Theme.drawCentered(g, "모순 발견!", 0, -4);
                    g.setFont(Theme.tracked(Theme.bold(15 / Theme.scale()), 0.3f));
                    Theme.drawCentered(g, "CONTRADICTION", 0, 42);
                }
                g.dispose();
            }
        };
        overlay.setOpaque(true);
        overlay.setBounds(0, 0, lp.getWidth(), lp.getHeight());
        overlay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { /* 아래 타이머가 끝낸다 */ }
        });
        lp.add(overlay, JLayeredPane.DRAG_LAYER);
        javax.swing.Timer tm = new javax.swing.Timer(16, null);
        Runnable finish = () -> {
            if (finished[0]) return;
            finished[0] = true;
            tm.stop();
            shakeX = shakeY = 0;
            lastBackdrop = backDev[0]; // 이어지는 자백 만화가 같은 배경을 쓴다
            lp.remove(overlay);
            lp.repaint();
            animating = false;
            done.run();
        };
        overlay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                if (System.currentTimeMillis() - t0 > 700) finish.run();
            }
        });
        tm.addActionListener(e -> {
            long el = System.currentTimeMillis() - t0;
            if (el >= 560 && !hit[0]) {
                hit[0] = true;
                Sfx.play("stamp");
                if (culpritFace != null) culpritFace.mood(Widgets.FaceView.Mood.SHOCK, 0);
                hazel.mood(Widgets.FaceView.Mood.HAPPY, 2400);
            }
            if (el >= 560 && el < 1060) {
                double k = (el - 560) / 500.0;
                shake[0] = Math.sin(el / 18.0) * 14 * (1 - k);
                shake[1] = Math.cos(el / 23.0) * 8 * (1 - k);
            } else {
                shake[0] = shake[1] = 0;
            }
            overlay.repaint();
            if (el > 2500) finish.run();
        });
        tm.start();
    }

    /** 날아가는 정리 카드(중심 기준). */
    private static void drawCard(Graphics2D g, Theorem t) {
        java.awt.geom.RoundRectangle2D r = new java.awt.geom.RoundRectangle2D.Double(-58, -74, 116, 148, 10, 10);
        g.setColor(new Color(0, 0, 0, 70));
        g.translate(4, 6);
        g.fill(r);
        g.translate(-4, -6);
        g.setColor(new Color(0xF4E8CC));
        g.fill(r);
        g.setColor(new Color(0xD9B456));
        g.setStroke(new BasicStroke(4f));
        g.draw(r);
        Icons.theorem(g, t.icon, new Rectangle2D.Double(-34, -60, 68, 68), new Color(0x3A2819), new Color(0xA33A28));
        g.setFont(Theme.bold(12.5f / Theme.scale()));
        g.setColor(new Color(0x3A2819));
        int y = 32;
        for (String ln : Widgets.TextBlock.wrap(t.name, g.getFontMetrics(), 100)) {
            Theme.drawCentered(g, ln, 0, y);
            y += 16;
        }
    }

    private void close() {
        Result r = game.finish(session);
        frame.showResult(r);
        frame.toasts(r.messages);
    }

    private void outOfLanterns() {
        Ui.Modal m = Ui.modal(frame, "등불이 모두 꺼졌다!", 500);
        m.text("어둠 속에서는 추리를 이어 갈 수 없어요. 단서 토큰 " + Game.REFILL_COST + "개로 등불을 다시 켜거나, 사건을 접어 두었다가 다시 도전하세요.");
        m.button("사건 접어 두기", Btn.Style.OUTLINE, () -> frame.showTab("map"));
        if (game.save.tokens >= Game.REFILL_COST) {
            m.button("토큰 " + Game.REFILL_COST + "개로 등불 켜기", Btn.Style.PRIMARY, () -> {
                game.refillLanterns();
                rebuild();
            });
        }
        m.show();
        if (game.lanterns() <= 0 && isShowing()) frame.showTab("map");
    }

    private void confirmQuit() {
        if (solved) { close(); return; }
        Ui.Modal m = Ui.modal(frame, "추리를 그만둘까요?", 460);
        m.text("지금 그만두면 이번 사건의 풀이 기록이 저장되지 않습니다. 수수께끼는 모두 풀었으니 조금만 더 힘내요!");
        m.button("계속 추리하기", Btn.Style.OUTLINE, null);
        m.button("그만두기", Btn.Style.PRIMARY, () -> frame.showTab("map"));
        m.show();
    }

    // ------------------------------------------------------------------ 구성 요소
    private class TestimonyCard extends RoundPanel {
        TestimonyCard(int no, FinaleLine f) {
            super(new BorderLayout(14, 0), f == pickedLine ? Theme.c(0xF8E3C8) : Theme.PAPER,
                    f == pickedLine ? Theme.RED : Theme.BORDER, 12);
            borderWidth(f == pickedLine ? 2.6f : 1.6f);
            pad(14, 16, 14, 16);
            setAlignmentX(LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            add(face(f.speaker, 72), BorderLayout.WEST);
            JPanel t = Ui.vbox();
            t.add(Ui.label("증언 " + no + "  ·  " + speakerName(f.speaker), Theme.bold(15), f == pickedLine ? Theme.RED : Theme.INK_SOFT));
            t.add(Ui.vgap(6));
            t.add(new Widgets.TextBlock("“" + f.text + "”", Theme.serif(15.5f), Theme.INK).spacing(1.6f).prefWidth(460));
            add(t, BorderLayout.CENTER);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (solved) return;
                    pickedLine = f;
                    Sfx.play("select");
                    bubble.setText(speakerName(f.speaker) + "의 증언을 골랐어. 이 말과 모순되는 정리 카드는 무엇일까?");
                    rebuild();
                }
            });
        }

        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }

    private class CardView extends JComponent {
        final Theorem t;
        boolean hover;

        CardView(Theorem t) {
            this.t = t;
            setPreferredSize(new Dimension(126, 150));
            setToolTipText("<html><b>" + t.name + "</b><br>" + t.description + "</html>");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (solved) return;
                    pickedCard = t;
                    Sfx.play("select");
                    rebuild();
                }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            boolean sel = t == pickedCard;
            g.setColor(sel ? Theme.c(0xF8E3C8) : hover ? Theme.c(0xFFF8EA) : Theme.c(0xF4E8CC));
            g.fill(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));
            g.setColor(sel ? Theme.RED : Theme.GOLD);
            g.setStroke(new BasicStroke(sel ? 3.5f : 2.2f));
            g.draw(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 10, 10));
            double s = Math.min(62, w - 40);
            Icons.theorem(g, t.icon, new Rectangle2D.Double(w / 2.0 - s / 2, 14, s, s), Theme.INK, Theme.RED);
            g.setFont(Theme.bold(13));
            g.setColor(Theme.INK);
            int y = (int) (s + 34);
            for (String line : Widgets.TextBlock.wrap(t.name, g.getFontMetrics(), w - 16)) {
                Theme.drawCentered(g, line, w / 2.0, y);
                y += 18;
            }
            g.dispose();
        }
    }
}
