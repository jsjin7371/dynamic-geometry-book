package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;
import geomystery.game.Result;
import geomystery.game.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 03 사건 해결 — 레슨 완료 화면. */
public class ResultScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private Result result;

    private final Stamp stamp;
    private final JLabel small, title;
    private final Widgets.TextBlock desc;
    private final Stat xpStat, accStat, timeStat, streakStat;
    private final Candles candles;
    private final CardPanel card;
    private final JLabel quote;
    private final Btn reviewBtn, endingBtn;
    private final Photo photo;

    public ResultScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new GridBagLayout());
        setBackground(Theme.DARK);

        RoundPanel sheet = new RoundPanel(null, Theme.c(0xEFE3C6), Theme.GOLD, 10).borderWidth(3f)
                .doubleBorder(Theme.alpha(Theme.GOLD_DARK, 0.8f));
        sheet.setLayout(new BoxLayout(sheet, BoxLayout.Y_AXIS));
        sheet.setBorder(BorderFactory.createEmptyBorder(40, 50, 36, 50));

        // 머리: 도장 + 제목
        JPanel head = Ui.clear(new BorderLayout(34, 0));
        head.setAlignmentX(LEFT_ALIGNMENT);
        stamp = new Stamp();
        head.add(stamp, BorderLayout.WEST);
        JPanel ht = Ui.vbox();
        ht.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        small = Ui.label("", Theme.tracked(Theme.serif(13), 0.18f), Theme.INK_SOFT);
        title = Ui.label("", Theme.xbold(36), Theme.INK);
        desc = new Widgets.TextBlock("", Theme.serif(16), Theme.INK_SOFT).spacing(1.7f).prefWidth(470);
        ht.add(small);
        ht.add(Ui.vgap(10));
        ht.add(title);
        ht.add(Ui.vgap(14));
        ht.add(desc);
        head.add(ht, BorderLayout.CENTER);
        photo = new Photo();
        head.add(photo, BorderLayout.EAST);
        sheet.add(head);
        sheet.add(Ui.vgap(28));

        // 통계 카드 4개
        JPanel stats = Ui.clear(new GridLayout(1, 4, 18, 0));
        stats.setAlignmentX(LEFT_ALIGNMENT);
        xpStat = new Stat("획득 경험치", Theme.RED);
        accStat = new Stat("정확도", Theme.GREEN);
        timeStat = new Stat("풀이 시간", Theme.INK);
        streakStat = new Stat("연속 학습", Theme.ORANGE);
        stats.add(xpStat);
        stats.add(accStat);
        stats.add(timeStat);
        stats.add(streakStat);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        sheet.add(stats);
        sheet.add(Ui.vgap(28));

        // 촛불 + 정리 카드
        JPanel mid = Ui.clear(new GridLayout(1, 2, 22, 0));
        mid.setAlignmentX(LEFT_ALIGNMENT);
        RoundPanel cw = new RoundPanel(new BorderLayout(0, 14), Theme.PAPER, Theme.BORDER, 14).borderWidth(2f);
        cw.pad(20, 22, 16, 22);
        cw.add(Ui.label("이번 주 촛불", Theme.xbold(19), Theme.INK), BorderLayout.NORTH);
        candles = new Candles();
        cw.add(candles, BorderLayout.CENTER);
        mid.add(cw);
        card = new CardPanel();
        mid.add(card);
        mid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 172));
        mid.setPreferredSize(new Dimension(900, 172));
        sheet.add(mid);
        sheet.add(Ui.vgap(30));

        JComponent dash = new JComponent() {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                g.setColor(Theme.BORDER_DARK);
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{5, 4}, 0));
                g.drawLine(0, 1, getWidth(), 1);
                g.dispose();
            }
        };
        dash.setAlignmentX(LEFT_ALIGNMENT);
        dash.setPreferredSize(new Dimension(100, 3));
        dash.setMaximumSize(new Dimension(Integer.MAX_VALUE, 3));
        sheet.add(dash);
        sheet.add(Ui.vgap(16));

        JPanel foot = Ui.clear(new BorderLayout(16, 0));
        foot.setAlignmentX(LEFT_ALIGNMENT);
        JPanel fl = Ui.hbox();
        JComponent face = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(70, 70); }
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
            @Override protected void paintComponent(Graphics g0) {
                BufferedImage im = Ui.img("hazel_face");
                if (im != null) Ui.drawScaled(Theme.aa(g0), im, 0, 0, 70, 70);
            }
        };
        fl.add(face);
        fl.add(Ui.hgap(14));
        quote = Ui.label("", Theme.hand(26), Theme.INK);
        fl.add(quote);
        foot.add(fl, BorderLayout.CENTER);
        JPanel fb = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        reviewBtn = new Btn("오답 다시 보기", Btn.Style.OUTLINE).font(16);
        reviewBtn.addActionListener(e -> showMistakes());
        Btn cont = new Btn("계속하기", Btn.Style.PRIMARY).font(16);
        for (String k : new String[]{"ENTER", "SPACE"}) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(k), "resultContinue");
        }
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed R"), "resultReview");
        getActionMap().put("resultContinue", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (isShowing()) cont.doClick(0); }
        });
        getActionMap().put("resultReview", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (isShowing()) showMistakes(); }
        });
        cont.setPreferredSize(new Dimension(132, 58));
        reviewBtn.setPreferredSize(new Dimension(160, 54));
        cont.addActionListener(e -> {
            if (result != null && result.gameCleared) frame.showEnding();
            else frame.showTab("map");
        });
        // 모든 사건을 해결한 뒤 마지막 사건을 다시 풀면, 원할 때 엔딩 크레딧을 다시 볼 수 있다
        endingBtn = new Btn("엔딩 크레딧 보기", Btn.Style.OUTLINE, "star").font(16);
        endingBtn.setPreferredSize(new Dimension(190, 54));
        endingBtn.setToolTipText("모든 사건을 해결했어요. 엔딩 크레딧을 다시 볼 수 있습니다");
        endingBtn.addActionListener(e -> frame.showEnding());
        endingBtn.setVisible(false);
        fb.add(endingBtn);
        fb.add(reviewBtn);
        fb.add(cont);
        JPanel fbWrap = Ui.clear(new GridBagLayout());
        fbWrap.add(fb);
        foot.add(fbWrap, BorderLayout.EAST);
        foot.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        sheet.add(foot);

        sheet.setPreferredSize(new Dimension(1080, 800));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(24, 24, 24, 24);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1; c.weighty = 1;
        JPanel limiter = Ui.clear(new BorderLayout());
        limiter.add(sheet);
        add(new MaxSize(limiter, 1080, 800), c);
    }

    /** 자식의 최대 크기를 제한하고 가운데 정렬하는 래퍼. */
    private static class MaxSize extends JPanel {
        MaxSize(JComponent child, int mw, int mh) {
            super(null);
            setOpaque(false);
            add(child);
            this.mw = mw;
            this.mh = mh;
        }
        final int mw, mh;

        @Override public void doLayout() {
            Component c = getComponent(0);
            int w = Math.min(getWidth(), mw), h = Math.min(getHeight(), mh);
            c.setBounds((getWidth() - w) / 2, (getHeight() - h) / 2, w, h);
        }
    }

    public void show(Result r) {
        result = r;
        Session s = r.session;
        stamp.play();
        Sfx.play("stamp");
        if (s.lesson != null) {
            Lesson l = s.lesson;
            Chapter ch = game.content.chapterOf(l);
            small.setText(ch.label + "  ·  " + (l.boss ? "최종 사건" : "사건 " + l.order));
            title.setText(Ui.josa(l.caseName, "을", "를") + " 풀었다!");
            desc.setText(r.finaleXp > 0 ? l.resolution + "  최종 추리로 진실을 밝혀냈다! (보너스 +" + r.finaleXp + " XP)" : l.resolution);
            quote.setText("“" + l.nextLine + "”");
        } else {
            small.setText(s.mode == Session.Mode.REVIEW ? "오답 노트" : "수수께끼 상자 · 연습");
            title.setText(s.mode == Session.Mode.REVIEW ? "오답을 다시 풀었다!" : "연습 수수께끼를 마쳤다!");
            desc.setText(s.mode == Session.Mode.REVIEW
                    ? "처음 시도에 맞힌 문제는 오답 노트에서 지워졌다. 남은 오답: " + game.save.mistakes.size() + "개"
                    : "해결한 사건의 수수께끼를 다시 풀며 단서를 되짚었다.");
            quote.setText("“틀렸던 문제를 다시 보는 것도 훌륭한 수사라네.”");
        }
        String xpSub = s.hintsUsed > 0 ? "힌트 " + s.hintsUsed + "개 사용" : "힌트 없이 해결";
        if (r.finaleXp > 0) xpSub = "최종 추리 +" + r.finaleXp + " 포함";
        xpStat.set("+" + r.xp, xpSub);
        accStat.set(r.accuracy + "%", r.total + "문제 중 " + r.firstTry + "문제");
        String timeSub;
        if (r.previousSeconds > 0) {
            int d = r.previousSeconds - r.seconds;
            timeSub = d > 0 ? "지난번보다 " + d + "초 빠름" : d < 0 ? "지난번보다 " + (-d) + "초 느림" : "지난번과 같은 기록";
        } else timeSub = "첫 기록";
        timeStat.set(Ui.time(r.seconds), timeSub);
        streakStat.set(r.streak + "일", r.streakGrew ? "촛불이 하나 더 커졌다" : "오늘도 촛불이 타오른다");
        candles.repaint();
        card.repaint();
        Lesson last = game.content.lessons.get(game.content.lessons.size() - 1);
        // 처음 해결했을 때는 '계속하기'가 곧바로 엔딩으로 이어지므로, 다시 풀었을 때만 선택 단추를 보인다
        endingBtn.setVisible(s.lesson == last && game.allCleared() && !r.gameCleared);
        reviewBtn.setEnabled(!r.wrong.isEmpty());
        reviewBtn.setToolTipText(r.wrong.isEmpty() ? "틀린 문제가 없습니다" : null);
        revalidate();
        repaint();
        if (r.tokensGained > 0) frame.queueTokenFly(r.tokensGained);
        photo.play(s.lesson);
        // 호감도: 의뢰인 하트가 늘었으면 알리고, 새 후일담이 열렸으면 사무소로 안내한다
        if (r.heartNpc != null && r.heartsAfter > r.heartsBefore) {
            Npc n = game.content.npc(r.heartNpc);
            if (n != null) {
                StringBuilder hs = new StringBuilder();
                for (int i = 0; i < Game.HEARTS; i++) hs.append(i < r.heartsAfter ? "♥" : "♡");
                String msg = n.name + "와(과) 한층 가까워졌다  " + hs;
                javax.swing.Timer ht = new javax.swing.Timer(1500, e -> { Sfx.play("heart"); frame.toast(msg); });
                ht.setRepeats(false);
                ht.start();
            }
        }
        if (!r.epiloguesOpened.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (String id : r.epiloguesOpened) { Npc n = game.content.npc(id); if (n != null) names.add(n.name); }
            String msg = "후일담이 열렸다: " + String.join(", ", names) + " — 탐정 사무소에서 볼 수 있어요";
            javax.swing.Timer et = new javax.swing.Timer(2300, e -> frame.toast(msg));
            et.setRepeats(false);
            et.start();
        }
        if (r.levelAfter > r.levelBefore) {
            String before = Game.titleFor(r.levelBefore), after = Game.titleFor(r.levelAfter);
            String title = after.equals(before) ? "레벨 업!" : "새 등급: " + after + "!";
            javax.swing.Timer t = new javax.swing.Timer(700, e -> Ui.celebrate(frame, title, "Lv." + r.levelBefore + " → Lv." + r.levelAfter + "  ·  " + after));
            t.setRepeats(false);
            t.start();
        }
    }

    private void showMistakes() {
        if (result == null || result.wrong.isEmpty()) return;
        Ui.Modal m = Ui.modal(frame, "오답 다시 보기", 620);
        JPanel list = Ui.vbox();
        for (Puzzle p : result.wrong) {
            RoundPanel row = new RoundPanel(new BorderLayout(0, 8), Theme.PAPER2, Theme.BORDER, 10);
            row.pad(14, 16, 14, 16);
            row.setAlignmentX(LEFT_ALIGNMENT);
            JPanel head = Ui.vbox();
            head.add(Ui.label("No. " + String.format("%03d", p.number) + "  " + p.title + "  ·  " + p.typeName(), Theme.bold(15.5f), Theme.INK));
            head.add(Ui.vgap(4));
            head.add(new Widgets.TextBlock("정답: " + p.answerText(), Theme.bold(14), Theme.RED).prefWidth(540));
            row.add(head, BorderLayout.NORTH);
            row.add(new Widgets.TextBlock(p.explanation, Theme.serif(14.5f), Theme.INK_SOFT).prefWidth(540), BorderLayout.CENTER);
            list.add(row);
            list.add(Ui.vgap(10));
        }
        m.add(list);
        m.text("틀린 문제는 '수수께끼 상자 → 오답 노트'에서 다시 풀 수 있어요.");
        m.button("닫기", Btn.Style.PRIMARY, null);
        m.show();
    }

    // ------------------------------------------------------------------
    private static class Stat extends RoundPanel {
        private final Widgets.CountLabel value;
        private final JLabel sub;

        Stat(String label, Color color) {
            super(new BorderLayout(0, 6), Theme.PAPER, Theme.BORDER, 14);
            borderWidth(2f);
            pad(18, 20, 18, 16);
            add(Ui.label(label, Theme.serif(13.5f), Theme.INK_SOFT), BorderLayout.NORTH);
            value = new Widgets.CountLabel(Theme.xbold(34), color);
            add(value, BorderLayout.CENTER);
            sub = Ui.label("", Theme.serif(13), Theme.INK_SOFT);
            add(sub, BorderLayout.SOUTH);
        }

        void set(String v, String s) { value.countTo(v, 250); sub.setText(s); }
    }

    private class Candles extends JComponent {
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            LocalDate today = game.today();
            LocalDate sunday = today.minusDays(today.getDayOfWeek().getValue() % 7);
            String[] names = {"일", "월", "화", "수", "목", "금", "토"};
            double cw = getWidth() / 7.0;
            for (int i = 0; i < 7; i++) {
                LocalDate d = sunday.plusDays(i);
                double cx = cw * i + cw / 2;
                boolean lit = game.save.history.containsKey(d.toString());
                boolean isToday = d.equals(today);
                boolean future = d.isAfter(today);
                if (future) {
                    g.setColor(Theme.c(0xD7C49C));
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{2, 3}, 0));
                    g.draw(new Ellipse2D.Double(cx - 7, 20, 14, 14));
                } else if (lit) {
                    Icons.paint(g, "candle", cx - 14, 6, 28, isToday ? Theme.RED : Theme.INK);
                } else {
                    g.setColor(Theme.c(0xBBA680));
                    g.setStroke(new BasicStroke(1.8f));
                    g.draw(new RoundRectangle2D.Double(cx - 4.5, 21, 9, 12, 2, 2));
                }
                g.setFont(isToday ? Theme.bold(13.5f) : Theme.serif(13.5f));
                g.setColor(isToday ? Theme.RED : Theme.INK_SOFT);
                Theme.drawCentered(g, isToday ? "오늘" : names[i], cx, 54);
                if (isToday) {
                    g.setStroke(new BasicStroke(1.4f));
                    g.drawLine((int) cx - 14, 64, (int) cx + 14, 64);
                }
            }
            g.dispose();
        }
    }

    private class CardPanel extends JComponent {
        /** 다 그린 카드(결과·크기·배율별 한 장). 위로 꽃가루가 지나가며 매 프레임 다시 그릴 때 복사만 한다. */
        private java.awt.image.BufferedImage cache;
        private Object cacheResult;
        private String cacheKey;

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            int w = getWidth(), h = getHeight();
            if (Ui.offscreen(g)) { paintCard(Theme.aa((Graphics2D) g.create()), w, h); return; }
            String key = w + "x" + h + "@" + Ui.deviceScale(g) + "|" + Theme.isDark() + "|" + game.save.mistakes.size();
            if (cache == null || cacheResult != result || !key.equals(cacheKey)) {
                cache = Ui.deviceImage(g, w, h);
                Graphics2D cg = Ui.cacheGraphics(cache, g);
                paintCard(cg, w, h);
                cacheResult = result;
                cacheKey = key;
            }
            Ui.blit(g, cache, 0, 0, w, h);
        }

        /** g 는 여기서 정리한다. */
        private void paintCard(Graphics2D g, int w, int h) {
            g.setColor(Theme.GREEN);
            g.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));
            g.setColor(Theme.GOLD_DARK);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 14, 14));
            if (result == null) { g.dispose(); return; }
            Theorem t = result.card;
            // 기울어진 카드
            Graphics2D cg = (Graphics2D) g.create();
            cg.rotate(Math.toRadians(-4), 70, h / 2.0);
            Shape cs = new RoundRectangle2D.Float(22, h / 2f - 62, 96, 124, 8, 8);
            cg.setColor(Theme.c(0xF4E8CC));
            cg.fill(cs);
            cg.setColor(Theme.GOLD);
            cg.setStroke(new BasicStroke(3.5f));
            cg.draw(cs);
            if (t != null) Icons.theorem(cg, t.icon, new Rectangle2D.Double(30, h / 2.0 - 40, 80, 80), Theme.INK, Theme.RED);
            else Icons.paint(cg, "refresh", 46, h / 2.0 - 24, 48, Theme.INK);
            cg.dispose();

            int x = 150;
            g.setFont(Theme.tracked(Theme.serif(12.5f), 0.14f));
            g.setColor(new Color(0xCFE0D6));
            String headline = t == null ? "복습 기록" : result.cardIsNew ? "새 정리 카드 획득" : "정리 카드 복습";
            g.drawString(headline, x, 44);
            g.setFont(Theme.xbold(22));
            g.setColor(Theme.CREAM);
            g.drawString(t == null ? "남은 오답 " + game.save.mistakes.size() + "개" : t.name, x, 76);
            g.setFont(Theme.serif(14));
            String body = t == null ? "오답 노트의 문제를 처음 시도에 맞히면 노트에서 지워진다."
                    : t.description + " 도형 도감 " + result.cardCount + " / " + result.cardTotal;
            int y = 104;
            for (String line : Widgets.TextBlock.wrap(body, g.getFontMetrics(), w - x - 24)) {
                if (y > h - 10) break;
                g.drawString(line, x, y);
                y += 23;
            }
            if (result.tokensGained > 0) {
                g.setFont(Theme.bold(13));
                g.setColor(Theme.GOLD);
                String tk = "단서 토큰 +" + result.tokensGained;
                g.drawString(tk, w - 22 - g.getFontMetrics().stringWidth(tk), 44);
            }
            g.dispose();
        }
    }

    /**
     * 증거 사진: 도장이 찍힌 뒤 '찰칵' 플래시와 함께 칠판 사진이 나오고, 짙은 갈색에서 서서히 현상된다.
     * 이 작은 영역만 다시 그린다.
     */
    private class Photo extends JComponent {
        private static final long AT = 950, FLASH = 260, DROP = 320, DEVELOP = 1700;
        private Lesson lesson;
        private long start;
        private boolean shot;
        private final javax.swing.Timer t = new javax.swing.Timer(16, e -> {
            long el = System.currentTimeMillis() - start;
            if (!shot && el >= AT) { shot = true; Sfx.play("shutter"); }
            repaint();
            if (el > AT + DROP + DEVELOP + 100) ((javax.swing.Timer) e.getSource()).stop();
        });

        Photo() { setOpaque(false); }

        @Override public Dimension getPreferredSize() { return lesson == null ? new Dimension(0, 0) : new Dimension(250, 220); }

        void play(Lesson l) {
            lesson = l;
            full = null;
            dark = null;
            t.stop();
            shot = false;
            setVisible(l != null);
            if (l == null) return;
            // 사진(현상 전·후)은 백그라운드에서 미리 그려 둔다. 움직이는 동안에는 복사만 한다
            double sc = Ui.screenScale(frame);
            final double fsc = sc;
            Ui.background("geomystery-photo", Thread.currentThread().getPriority(), () -> {
                BufferedImage f = render(fsc, true), d = render(fsc, false);
                SwingUtilities.invokeLater(() -> { if (lesson == l) { full = f; dark = d; builtScale = fsc; } });
            });
            start = System.currentTimeMillis();
            t.start();
            revalidate();
        }

        /** 미리 회전해 그려 둔 사진: 다 현상된 것과 현상 전(어두운) 것. 움직이는 동안에는 이 둘을 겹쳐 복사만 한다. */
        private BufferedImage full, dark;
        private double builtScale;
        private static final double PW = 176, PH = 196, BOX = 250, ROT = 5;

        private BufferedImage render(double sc, boolean developed) {
            BufferedImage im = new BufferedImage((int) Math.ceil(BOX * sc), (int) Math.ceil(BOX * sc), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = Theme.aa(im.createGraphics());
            g.scale(sc, sc);
            g.translate(BOX / 2, BOX / 2);
            g.rotate(Math.toRadians(ROT));
            if (developed) {
                EvidencePhoto.paint(g, game.content, lesson, -PW / 2, -PH / 2, PW, PH, 1f, lesson.caseName);
                g.setColor(new Color(240, 226, 180, 190));
                g.fill(new Rectangle2D.Double(-26, -PH / 2 - 9, 52, 18));
            } else {
                // 현상 전: 사진 칸만 짙은 갈색
                double pad = Math.max(6, PW * 0.06), iw = PW - pad * 2, ih = PH * 0.72 - pad;
                g.setColor(new Color(0x3A2819));
                g.fill(new Rectangle2D.Double(-PW / 2 + pad, -PH / 2 + pad, iw, ih));
            }
            g.dispose();
            return im;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (lesson == null) return;
            long el = System.currentTimeMillis() - start - AT;
            if (el < 0) return;
            Graphics2D g = Theme.aa(g0.create());
            double sc = Ui.deviceScale(g);
            if (full == null) {
                // 아직 준비 중이면 섬광만(준비되면 다음 프레임부터 나타난다)
                g.dispose();
                return;
            }
            if (Math.abs(sc - builtScale) > 0.01) {
                full = render(sc, true);
                dark = render(sc, false);
                builtScale = sc;
            }
            double k = Math.min(1, el / (double) DROP), e = 1 - Math.pow(1 - k, 3);
            double cx = getWidth() / 2.0, cy = getHeight() / 2.0 + 4 - (1 - e) * 26;
            float in = (float) Math.min(1, k * 1.6);
            float dev = (float) Math.max(0, Math.min(1, (el - DROP * 0.6) / (double) DEVELOP));
            Composite keep = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, in));
            Ui.blit(g, full, Math.round(cx - BOX / 2), Math.round(cy - BOX / 2), BOX, BOX);
            float d = in * (1 - dev * dev);
            if (d > 0.003f) {
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, d));
                Ui.blit(g, dark, Math.round(cx - BOX / 2), Math.round(cy - BOX / 2), BOX, BOX);
            }
            g.setComposite(keep);
            // 찰칵: 하얀 섬광
            if (el < FLASH) {
                float a = (float) (1 - el / (double) FLASH);
                g.setPaint(new RadialGradientPaint(new Point2D.Float((float) cx, (float) cy), 150f, new float[]{0f, 1f},
                        new Color[]{new Color(1f, 1f, 0.97f, a), new Color(1f, 1f, 0.97f, 0f)}));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.dispose();
        }
    }

    /** "해결 · CASE CLOSED" 도장. 찍히는 애니메이션이 있다. */
    private static class Stamp extends JComponent {
        private long start;
        private final javax.swing.Timer t = new javax.swing.Timer(16, e -> {
            repaint();
            if (System.currentTimeMillis() - start > 500) ((javax.swing.Timer) e.getSource()).stop();
        });

        Stamp() { setPreferredSize(new Dimension(200, 200)); }

        void play() { start = System.currentTimeMillis(); t.start(); }

        /** 다 찍힌 도장(배율별 한 장). 찍히는 0.4초가 지나면 이것만 복사한다. */
        private java.awt.image.BufferedImage done;
        private double doneScale;

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g0d = (Graphics2D) g0;
            boolean finished = System.currentTimeMillis() - start >= 420;
            if (finished && !Ui.offscreen(g0d)) {
                double ds = Ui.deviceScale(g0d);
                if (done == null || doneScale != ds) {
                    done = Ui.deviceImage(g0d, getWidth(), getHeight());
                    Graphics2D cg = Ui.cacheGraphics(done, g0d);
                    paintStamp(cg, 1);
                    doneScale = ds;
                }
                Ui.blit(g0d, done, 0, 0, getWidth(), getHeight());
                return;
            }
            paintStamp(Theme.aa((Graphics2D) g0.create()), Math.min(1, (System.currentTimeMillis() - start) / 420.0));
        }

        /** g 는 여기서 정리한다. */
        private void paintStamp(Graphics2D g, double p) {
            double ease = 1 - Math.pow(1 - p, 3);
            double s = 1.7 - 0.7 * ease;
            float alpha = (float) Math.min(1, p * 1.6);
            double cx = 100, cy = 100;
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.translate(cx, cy);
            g.scale(s, s);
            g.rotate(Math.toRadians(-12));
            Color red = new Color(0xC23B2A);
            g.setColor(red);
            g.setStroke(new BasicStroke(3.5f));
            g.draw(new Ellipse2D.Double(-94, -94, 188, 188));
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Double(-84, -84, 168, 168));
            g.setFont(Theme.xbold(46));
            Theme.drawCentered(g, "해결", 0, -12);
            g.setFont(Theme.tracked(Theme.serif(13), 0.25f));
            String cc = "CASE CLOSED";
            g.setFont(Theme.serif(14));
            FontMetrics fm = g.getFontMetrics();
            double gap = 3.5, total = 0;
            for (char ch : cc.toCharArray()) total += fm.stringWidth(String.valueOf(ch)) + gap;
            // 아래쪽 호를 따라 왼쪽→오른쪽으로 글자를 배치 (화면 좌표: y 아래, φ 시계 방향)
            double r = 70, phi0 = Math.PI / 2 + total / r / 2, acc = 0;
            for (char ch : cc.toCharArray()) {
                double cwid = fm.stringWidth(String.valueOf(ch)) + gap;
                double phi = phi0 - (acc + cwid / 2) / r;
                AffineTransform keep = g.getTransform();
                g.translate(Math.cos(phi) * r, Math.sin(phi) * r);
                g.rotate(phi - Math.PI / 2);
                g.drawString(String.valueOf(ch), (float) (-(cwid - gap) / 2.0), 0f);
                g.setTransform(keep);
                acc += cwid;
            }
            g.dispose();
        }
    }
}
