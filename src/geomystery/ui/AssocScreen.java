package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/** 04 탐정 협회 — 리그 순위 · 도형 도감. */
public class AssocScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private final Header header;
    private final CardLayout cards = new CardLayout();
    private final JPanel left = Ui.clear(cards);
    private final Btn tabLeague, tabDex;
    private final JPanel leagueRows;
    private final JLabel leagueSub, leagueTitle, leagueFoot;
    private final JPanel dexGrid;
    private final JLabel dexCount;
    private final JLabel levelTitle, levelNext;
    private final Widgets.Bar levelBar;
    private final JPanel badgeGrid;
    private String tab = "league";
    private long lastScroll;
    private final java.util.List<DexCard> dexCards = new java.util.ArrayList<>();

    public AssocScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        header = new Header(game);
        header.setTitle("ASSOCIATION OF DETECTIVES", "탐정 협회");
        RoundPanel seg = new RoundPanel(new FlowLayout(FlowLayout.CENTER, 4, 0), Theme.c(0xE6D5AF), null, 14);
        seg.pad(5, 5, 5, 5);
        tabLeague = new Btn("리그 순위", Btn.Style.TAB).font(15);
        tabDex = new Btn("도형 도감", Btn.Style.TAB).font(15);
        tabLeague.setPreferredSize(new Dimension(104, 46));
        tabDex.setPreferredSize(new Dimension(104, 46));
        tabLeague.addActionListener(e -> frame.showAssocTab("league"));
        tabDex.addActionListener(e -> frame.showAssocTab("dex"));
        seg.add(tabLeague);
        seg.add(tabDex);
        header.replaceRight(seg);
        add(header, BorderLayout.NORTH);

        JPanel body = Ui.clear(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        GridBagConstraints c = new GridBagConstraints();

        // ---------- 리그 표
        RoundPanel table = new RoundPanel(new BorderLayout(), Theme.PAPER, Theme.BORDER_DARK, 14).borderWidth(2f);
        JPanel th = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                g.setColor(Theme.DARK);
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() + 20, 12, 12));
                g.dispose();
            }
        };
        th.setOpaque(false);
        th.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel sIcon = new JLabel(Icons.icon("search", 40, Theme.ORANGE));
        th.add(sIcon, BorderLayout.WEST);
        JPanel tht = Ui.vbox();
        leagueTitle = Ui.label("", Theme.xbold(21), Theme.CREAM);
        tht.add(leagueTitle);
        tht.add(Ui.vgap(4));
        leagueSub = Ui.label("", Theme.serif(13.5f), new Color(0xD8C39A));
        tht.add(leagueSub);
        th.add(tht, BorderLayout.CENTER);
        table.add(th, BorderLayout.NORTH);
        leagueRows = Ui.vtrack();
        table.add(Ui.scroll(leagueRows), BorderLayout.CENTER);
        leagueFoot = Ui.label("", Theme.serif(13), Theme.INK_SOFT);
        leagueFoot.setBorder(BorderFactory.createEmptyBorder(14, 24, 16, 24));
        table.add(leagueFoot, BorderLayout.SOUTH);
        left.add(table, "league");

        // ---------- 도감
        JPanel dex = Ui.clear(new BorderLayout(0, 16));
        dexCount = Ui.label("", Theme.serif(15), Theme.INK_SOFT);
        dex.add(dexCount, BorderLayout.NORTH);
        dexGrid = new Ui.TrackWidth(new GridLayout(0, 3, 16, 16));
        JScrollPane dexScroll = Ui.scroll(dexGrid);
        // 스크롤하는 동안에는 금박 빛줄기를 멈춰 스크롤이 매끄럽게 한다
        dexScroll.getViewport().addChangeListener(e -> lastScroll = System.currentTimeMillis());
        dex.add(dexScroll, BorderLayout.CENTER);
        left.add(dex, "dex");

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 26);
        body.add(left, c);

        // ---------- 오른쪽: 등급 + 훈장
        JPanel right = Ui.vtrack();
        right.setPreferredSize(new Dimension(290, 600));
        right.setMinimumSize(new Dimension(290, 300));
        RoundPanel lv = new RoundPanel(null, Theme.GREEN, Theme.GOLD_DARK, 14).borderWidth(2.5f);
        lv.setLayout(new BoxLayout(lv, BoxLayout.Y_AXIS));
        lv.pad(20, 22, 22, 22);
        lv.setAlignmentX(LEFT_ALIGNMENT);
        lv.add(Ui.label("나의 탐정 등급", Theme.tracked(Theme.serif(13), 0.1f), new Color(0xCFE0D6)));
        lv.add(Ui.vgap(8));
        levelTitle = Ui.label("", Theme.xbold(27), Theme.CREAM);
        lv.add(levelTitle);
        lv.add(Ui.vgap(14));
        levelBar = new Widgets.Bar(new Color(0x1E4037), Theme.GOLD, 11);
        levelBar.setAlignmentX(LEFT_ALIGNMENT);
        lv.add(levelBar);
        lv.add(Ui.vgap(12));
        levelNext = Ui.label("", Theme.serif(13.5f), Theme.CREAM);
        lv.add(levelNext);
        lv.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        right.add(lv);
        right.add(Ui.vgap(20));

        // 훈장 수만큼 늘어난다(최대 높이로 묶으면 칸이 눌려 이름이 잘린다)
        RoundPanel bd = new RoundPanel(new BorderLayout(0, 14), Theme.PAPER, Theme.BORDER, 14) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        }.borderWidth(2f);
        bd.pad(18, 18, 18, 18);
        bd.setAlignmentX(LEFT_ALIGNMENT);
        bd.add(Ui.label("훈장", Theme.xbold(20), Theme.INK), BorderLayout.NORTH);
        badgeGrid = Ui.clear(new GridLayout(0, 3, 6, 14));
        bd.add(badgeGrid, BorderLayout.CENTER);
        right.add(bd);
        right.add(Box.createVerticalGlue());
        // 창이 낮으면 오른쪽 열(등급·훈장)을 스크롤한다
        right.setPreferredSize(null);
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 10));
        JScrollPane rightScroll = Ui.scroll(right);
        rightScroll.setPreferredSize(new Dimension(310, 600));
        rightScroll.setMinimumSize(new Dimension(310, 300));
        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(rightScroll, c);
        add(body, BorderLayout.CENTER);
        new javax.swing.Timer(50, e -> {
            if (!Ui.inFront(this) || !"dex".equals(tab)) return;
            if (System.currentTimeMillis() - lastScroll < 350) return;
            for (DexCard d : dexCards) if (d.animating()) d.repaint();
        }).start();
    }

    public void setTab(String t) {
        tab = t;
        cards.show(left, t);
        tabLeague.setSelected("league".equals(t));
        tabDex.setSelected("dex".equals(t));
    }

    public String tab() {
        return tab;
    }

    public void refresh() {
        header.refresh();
        setTab(tab);
        leagueTitle.setText(game.tierName() + "  ·  " + (game.tier() + 1) + " / " + Game.TIERS.length + "단계");
        String next = game.nextTierName();
        leagueSub.setText((next != null ? "상위 " + Game.PROMOTE_RANK + "명은 " + next + " 리그로 승급" : "최고 리그 — 순위를 지켜라")
                + (game.tier() > 0 ? " · 하위 " + Game.DEMOTE_FROM_BOTTOM + "명 강등" : "") + " · " + game.leagueTimeLeft() + " 남음");
        leagueFoot.setText(next != null ? "— 승급선 — " + Game.PROMOTE_RANK + "위 이내에 들면 다음 주에 '" + next + "' 리그로 올라가고 단서 토큰 2개를 받습니다."
                : "명탐정 리그에서는 경쟁자들이 가장 부지런합니다. 강등선 위를 지키세요.");
        leagueRows.removeAll();
        List<Game.LeagueRow> rows = game.league();
        int myRank = game.myRank();
        String week = game.today().with(java.time.DayOfWeek.MONDAY).toString();
        int oldRank = -1;
        String[] seen = game.save.leagueSeen == null ? new String[0] : game.save.leagueSeen.split(":");
        if (seen.length == 2 && seen[0].equals(week)) {
            try {
                oldRank = Integer.parseInt(seen[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        LeagueTable lt = new LeagueTable(rows, next != null, game.tier() > 0);
        leagueRows.add(lt);
        if ("league".equals(tab)) {
            if (oldRank > 0 && oldRank != myRank) lt.animateFrom(oldRank - 1);
            game.save.leagueSeen = week + ":" + myRank;
            game.save.save();
        }
        leagueRows.revalidate();

        List<Theorem> cards = game.cards();
        dexCount.setText("모은 정리 카드 " + cards.size() + " / " + game.content.theorems.size() + "  —  사건을 해결하면 그 사건의 정리 카드를 얻습니다.");
        dexGrid.removeAll();
        dexCards.clear();
        long now = System.currentTimeMillis();
        int stagger = 0;
        boolean dexVisible = "dex".equals(tab);
        for (Theorem t : game.content.theorems.values()) {
            boolean owned = cards.contains(t);
            Lesson l = game.lessonForTheorem(t.id);
            Chapter ch = l == null ? null : game.content.chapterOf(l);
            boolean foil = owned && ch != null && game.doneCount(ch) == game.content.lessonsOf(ch).size();
            DexCard dc = new DexCard(t, owned, foil);
            if (owned && dexVisible && !game.save.seenCards.contains(t.id)) {
                dc.flipStart = now + 300 + stagger++ * 220L;
                game.save.seenCards.add(t.id);
            }
            dexCards.add(dc);
            dexGrid.add(dc);
        }
        if (stagger > 0) {
            game.save.save();
            Sfx.play("hint");
        }
        dexGrid.revalidate();

        levelTitle.setText(game.title() + " Lv." + game.level());
        levelBar.setValue(game.levelProgress());
        String nt = game.nextTitle();
        levelNext.setText(nt == null ? "최고 등급에 도달했습니다" : nt + "까지 " + Ui.num(game.xpToNextTitle()) + " XP");

        badgeGrid.removeAll();
        for (Badge b : game.content.badges) badgeGrid.add(new BadgeView(b, game.hasBadge(b), game.stat(b.stat)));
        badgeGrid.revalidate();
        repaint();
    }

    // ------------------------------------------------------------------
    /**
     * 리그 순위표 한 덩어리. 내 순위가 바뀌었으면 내 줄이 예전 자리에서 새 자리로 미끄러지고
     * 다른 줄들이 비켜선다. 움직이는 동안(0.9초)만 이 표 자신을 다시 그린다.
     */
    private class LeagueTable extends JComponent {
        private static final int RH = 54, LINE = 6;
        private final List<Game.LeagueRow> rows;
        private final boolean promo, demote;
        private int[] oldIndex;
        private long animStart;
        private int moved;   // 바뀐 순위(+는 오름)

        LeagueTable(List<Game.LeagueRow> rows, boolean promo, boolean demote) {
            this.rows = rows;
            this.promo = promo;
            this.demote = demote;
            setAlignmentX(LEFT_ALIGNMENT);

            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int index = -1;

                    for (int i = 0; i < rows.size(); i++) {
                        double y = yOf(i);

                        if (e.getY() >= y && e.getY() < y + RH) {
                            index = i;
                            break;
                        }
                    }  /*승급선·강등선 때문에 행 사이 간격이 달라져도 클릭한 탐정을 정확하게 찾기위해 수정함  248~257 */

                    if (index >= 0 && index < rows.size()) {
                        Game.LeagueRow r = rows.get(index);

                        /*수정된내용: 탐정 프로필에 총 학습 시간, 정리 카드 수, 챕터별 학습 진행률 표시 추가 250~300*/

                        // 총 학습 시간을 시간과 분으로 변환
                        int hours = r.studySeconds / 3600;
                        int minutes = (r.studySeconds % 3600) / 60;

                        String studyTime;
                        if (hours > 0) {
                            studyTime = hours + "시간 " + minutes + "분";
                        } else {
                            studyTime = minutes + "분";
                        }

                        StringBuilder info = new StringBuilder();

                        info.append("이름: ").append(r.name)
                                .append("\n순위: ").append(index + 1).append("위")
                                .append("\n주간 XP: ").append(r.xp)
                                .append("\n해결한 사건: ").append(r.solvedCases)
                                .append("\n첫 시도 정답률: ").append(r.accuracy).append("%")
                                .append("\n최장 연속 학습: ").append(r.bestStreak).append("일")
                                .append("\n총 학습 시간: ").append(studyTime)
                                .append("\n보조선 사용: ").append(r.auxTotal).append("회")
                                .append("\n모은 정리 카드: ").append(r.cardsCount).append("개");

                        // 챕터별 학습 진행률 표시
                        info.append("\n\n[챕터별 학습 진행률]");

                        for (int i = 0; i < game.content.chapters.size(); i++) {
                            Chapter ch = game.content.chapters.get(i);

                            String chapterName = ch.title != null ? ch.title : ch.label;

                            int done = 0;
                            int total = 0;

                            if (r.chapterDone != null && i < r.chapterDone.size()) {
                                done = r.chapterDone.get(i);
                            }

                            if (r.chapterTotal != null && i < r.chapterTotal.size()) {
                                total = r.chapterTotal.get(i);
                            }

                            int percent = total == 0 ? 0 : done * 100 / total;

                            info.append("\n")
                                    .append(chapterName)
                                    .append(": ")
                                    .append(done).append(" / ").append(total)
                                    .append("개 완료 (")
                                    .append(percent).append("%)");
                        }

                        /*수정된내용 끝*/

                        JOptionPane.showMessageDialog(
                                AssocScreen.this,
                                info.toString(),
                                "탐정 프로필",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                }
            });
        } /*리그 순위표의 탐정 이름을 클릭하면 해당 탐정의 통계가 팝업으로 표시되도록 수정함 237~272 */

        void animateFrom(int oldMine) {
            int me = -1;
            for (int i = 0; i < rows.size(); i++) if (rows.get(i).me) me = i;
            if (me < 0) return;
            oldMine = Math.max(0, Math.min(rows.size() - 1, oldMine));
            java.util.List<Integer> order = new java.util.ArrayList<>();
            for (int i = 0; i < rows.size(); i++) if (i != me) order.add(i);
            order.add(oldMine, me);
            oldIndex = new int[rows.size()];
            for (int pos = 0; pos < order.size(); pos++) oldIndex[order.get(pos)] = pos;
            moved = oldMine - me;
            animStart = System.currentTimeMillis() + 350;
            javax.swing.Timer t = new javax.swing.Timer(16, null);
            t.addActionListener(e -> {
                repaint();
                if (System.currentTimeMillis() - animStart > 950) t.stop();
            });
            t.start();
            Sfx.play(moved > 0 ? "correct" : "hint");
        }

        private double yOf(int index) {
            int extra = 0;
            if (demote && index + 1 >= game.demoteRank()) extra += LINE;
            if (promo && index + 1 > Game.PROMOTE_RANK) extra += LINE;
            return index * RH + extra;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(600, rows.size() * RH + LINE * 2);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth();
            double k = oldIndex == null ? 1 : Math.max(0, Math.min(1, (System.currentTimeMillis() - animStart) / 900.0));
            double e = 1 - Math.pow(1 - k, 3);
            // 승급선·강등선은 제자리에
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{4, 3}, 0));
            if (promo) {
                g.setColor(Theme.GREEN);
                double ly = yOf(Game.PROMOTE_RANK - 1) + RH + LINE / 2.0;
                g.draw(new java.awt.geom.Line2D.Double(0, ly, w, ly));
            }
            if (demote) {
                g.setColor(Theme.RED);
                double ly = yOf(game.demoteRank() - 1) - LINE / 2.0;
                g.draw(new java.awt.geom.Line2D.Double(0, ly, w, ly));
            }
            int meIdx = -1;
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).me) {
                    meIdx = i;
                    continue;
                }
                paintRow(g, i, e, w);
            }
            if (meIdx >= 0) paintRow(g, meIdx, e, w); // 내 줄은 맨 위에
            g.dispose();
        }

        private void paintRow(Graphics2D g0, int i, double e, int w) {
            double y = oldIndex == null ? yOf(i) : yOf(oldIndex[i]) + (yOf(i) - yOf(oldIndex[i])) * e;
            Graphics2D g = (Graphics2D) g0.create();
            g.translate(0, y);
            Game.LeagueRow r = rows.get(i);
            int h = RH;
            if (r.me) {
                if (oldIndex != null && e < 1) {
                    g.setColor(new Color(0, 0, 0, 40));
                    g.fillRect(0, 4, w, h);
                }
                g.setColor(Theme.c(0xF2DC9E));
                g.fillRect(0, 0, w, h);
            } else {
                g.setColor(Theme.PAPER);
                g.fillRect(0, 0, w, h);
            }
            g.setColor(Theme.c(0xE6D6B6));
            g.fillRect(0, h - 1, w, 1);
            g.setFont(Theme.serif(20));
            g.setColor(Theme.RED);
            FontMetrics fm = g.getFontMetrics();
            int by = (h + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString("" + (i + 1), 24, by);
            g.setColor(new Color(r.color));
            g.fill(new Ellipse2D.Double(70, h / 2.0 - 19, 38, 38));
            g.setColor(Theme.INK);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Double(70, h / 2.0 - 19, 38, 38));
            g.setFont(Theme.bold(16));
            g.setColor(Theme.CREAM);
            Theme.drawCentered(g, r.me ? "나" : r.name.substring(0, 1), 89, h / 2.0);
            g.setFont(r.me ? Theme.bold(16.5f) : Theme.serif(16.5f));
            g.setColor(Theme.INK);
            g.drawString(r.name, 128, by);
            if (r.me && oldIndex != null && moved != 0) {
                String b = (moved > 0 ? "▲ " : "▼ ") + Math.abs(moved);
                g.setFont(Theme.bold(13));
                int bw = g.getFontMetrics().stringWidth(b) + 16;
                int bx = 128 + g.getFontMetrics(Theme.bold(16.5f)).stringWidth(r.name) + 14;
                g.setColor(moved > 0 ? Theme.GREEN : Theme.RED);
                g.fill(new RoundRectangle2D.Float(bx, h / 2f - 12, bw, 24, 24, 24));
                g.setColor(Theme.CREAM);
                g.drawString(b, bx + 8, h / 2 + 5);
            }
            String xp = Ui.num(r.xp) + " XP";
            g.setFont(Theme.serif(16));
            g.setColor(Theme.INK);
            g.drawString(xp, w - 26 - g.getFontMetrics().stringWidth(xp), by);
            g.dispose();
        }
    }

    private static class Row extends JComponent {
        final int rank;
        final Game.LeagueRow r;

        Row(int rank, Game.LeagueRow r) {
            this.rank = rank;
            this.r = r;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(600, 54));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            if (r.me) {
                g.setColor(Theme.c(0xF2DC9E));
                g.fillRect(0, 0, w, h);
            }
            g.setColor(Theme.c(0xE6D6B6));
            g.fillRect(0, h - 1, w, 1);
            g.setFont(Theme.serif(20));
            g.setColor(Theme.RED);
            FontMetrics fm = g.getFontMetrics();
            int by = (h + fm.getAscent() - fm.getDescent()) / 2;
            g.drawString("" + rank, 24, by);
            Color av = new Color(r.color);
            g.setColor(av);
            g.fill(new Ellipse2D.Double(70, h / 2.0 - 19, 38, 38));
            g.setColor(Theme.INK);
            g.setStroke(new BasicStroke(1.6f));
            g.draw(new Ellipse2D.Double(70, h / 2.0 - 19, 38, 38));
            g.setFont(Theme.bold(16));
            g.setColor(Theme.CREAM);
            String initial = r.me ? "나" : r.name.substring(0, 1);
            Theme.drawCentered(g, initial, 89, h / 2.0);
            g.setFont(r.me ? Theme.bold(16.5f) : Theme.serif(16.5f));
            g.setColor(Theme.INK);
            g.drawString(r.name, 128, by);
            String xp = Ui.num(r.xp) + " XP";
            g.setFont(Theme.serif(16));
            g.drawString(xp, w - 26 - g.getFontMetrics().stringWidth(xp), by);
            g.dispose();
        }
    }

    private static class PromoLine extends JComponent {
        private final Color color;

        PromoLine(Color color) {
            this.color = color;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(100, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            g.setColor(color);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{4, 3}, 0));
            g.drawLine(0, 3, getWidth(), 3);
            g.dispose();
        }
    }

    private class DexCard extends JComponent {
        final Theorem t;
        final boolean owned, foil;
        long flipStart = 0;
        boolean hover;

        DexCard(Theorem t, boolean owned, boolean foil) {
            this.t = t;
            this.owned = owned;
            this.foil = foil;
            setPreferredSize(new Dimension(220, 250));
            if (!owned) {
                Lesson l = game.lessonForTheorem(t.id);
                setToolTipText(l == null ? "아직 발견하지 못했습니다" : "'" + l.caseName + "' 사건을 해결하면 획득");
            } else {
                setToolTipText("클릭 — 카드를 크게 보기" + (foil ? " (장을 모두 해결한 금박 카드)" : ""));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        CardZoom.open(frame, t);
                    }

                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }
        }

        boolean animating() {
            long el = System.currentTimeMillis() - flipStart;
            return foil || (flipStart > 0 && el < 1500);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            long el = System.currentTimeMillis() - flipStart;
            boolean flipping = flipStart > 0 && el < 760;
            boolean showBack = flipStart > 0 && el < 380;
            if (flipping) {
                double p = Math.max(0, el) / 760.0;
                double sx = Math.max(0.02, Math.abs(Math.cos(Math.PI * p)));
                g.translate(w / 2.0, 0);
                g.scale(sx, 1 + 0.05 * Math.sin(Math.PI * p));
                g.translate(-w / 2.0, 0);
            }
            if (hover) g.translate(0, -3);
            RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 12, 12);
            if (showBack) {
                g.setColor(Theme.GREEN);
                g.fill(shape);
                g.setColor(Theme.GOLD);
                g.setStroke(new BasicStroke(3f));
                g.draw(shape);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new RoundRectangle2D.Float(10, 10, w - 20, h - 20, 8, 8));
                Icons.paint(g, "compass", w / 2.0 - 30, h / 2.0 - 40, 60, Theme.GOLD);
                g.setFont(Theme.tracked(Theme.hanja(13), 0.3f));
                Theme.drawCentered(g, "幾何迷宮", w / 2.0, h / 2.0 + 44);
                g.dispose();
                return;
            }
            if (flipping) {
                paintFace(g, w, h, shape);
            } else {
                // 앞면은 한 번 그려 캐시해 두고 복사한다(금박 빛줄기 때문에 계속 다시 그려지므로)
                double sc = Ui.deviceScale(g);
                if (faceCache == null || faceW != w || faceH != h || (faceScale != sc && !Ui.offscreen(g))) {
                    faceCache = Ui.deviceImage(g, w, h);
                    Graphics2D cg = Ui.cacheGraphics(faceCache, g);
                    paintFace(cg, w, h, shape);
                    cg.dispose();
                    faceW = w;
                    faceH = h;
                    faceScale = sc;
                }
                Ui.blit(g, faceCache, 0, 0, w, h);
            }
            if (owned && foil) {
                // 금박: 비스듬한 빛줄기가 천천히 지나간다
                double tt = System.currentTimeMillis() / 1000.0;
                double cx = ((tt * 0.45 + t.id.hashCode() % 7 * 0.13) % 1.8 - 0.4) * w;
                Graphics2D f = (Graphics2D) g.create();
                f.clip(shape);
                f.setPaint(new LinearGradientPaint(new java.awt.geom.Point2D.Double(cx - 60, 0), new java.awt.geom.Point2D.Double(cx + 60, h),
                        new float[]{0f, 0.5f, 1f}, new Color[]{new Color(255, 236, 170, 0), new Color(255, 240, 190, 120), new Color(255, 236, 170, 0)}));
                f.fill(shape);
                f.dispose();
            }
            if (owned && flipStart > 0 && el >= 380 && el < 1500) {
                double k = (el - 380) / 1120.0;
                for (int i = 0; i < 10; i++) {
                    double a = i * Math.PI / 5;
                    double rr = 40 + k * 90;
                    double px = w / 2.0 + Math.cos(a) * rr, py = 80 + Math.sin(a) * rr * 0.8;
                    g.setColor(new Color(242, 192, 74, (int) (220 * (1 - k))));
                    g.fill(new java.awt.geom.Ellipse2D.Double(px - 4, py - 4, 8, 8));
                }
                g.setFont(Theme.bold(13));
                g.setColor(new Color(194, 59, 42, (int) (255 * Math.min(1, (1 - k) * 2))));
                Theme.drawCentered(g, "NEW!", w - 40, 24);
            }
            g.dispose();
        }

        private java.awt.image.BufferedImage faceCache;
        private int faceW, faceH;
        private double faceScale;

        /** 카드 앞면(움직이지 않는 부분). */
        private void paintFace(Graphics2D g, int w, int h, RoundRectangle2D shape) {
            g.setColor(owned ? Theme.PAPER : Theme.c(0xE3D3B0));
            g.fill(shape);
            g.setColor(owned ? Theme.GOLD : Theme.c(0xC9B48C));
            g.setStroke(new BasicStroke(owned ? (foil ? 3.5f : 2.5f) : 1.6f));
            g.draw(shape);
            if (foil) {
                g.setStroke(new BasicStroke(1.2f));
                g.setColor(Theme.GOLD_DARK);
                g.draw(new RoundRectangle2D.Float(7, 7, w - 14, h - 14, 8, 8));
            }
            g.setFont(Theme.tracked(Theme.serif(11.5f), 0.15f));
            g.setColor(Theme.INK_SOFT);
            g.drawString("No. " + t.id.substring(1), 16, 26);
            if (owned) {
                Icons.theorem(g, t.icon, new Rectangle2D.Double(w / 2.0 - 45, 36, 90, 90), Theme.INK, Theme.RED);
                g.setFont(Theme.xbold(17));
                g.setColor(Theme.INK);
                Theme.drawCentered(g, t.name, w / 2.0, 148);
                g.setFont(Theme.serif(12.5f));
                g.setColor(Theme.INK_SOFT);
                int y = 176;
                for (String line : Widgets.TextBlock.wrap(t.description, g.getFontMetrics(), w - 30)) {
                    if (y > h - 8) break;
                    Theme.drawCentered(g, line, w / 2.0, y);
                    y += 19;
                }
                if (foil) Icons.paint(g, "star_fill", w - 34, 12, 18, Theme.GOLD);
            } else {
                g.setFont(Theme.xbold(54));
                g.setColor(Theme.c(0xBFA87E));
                Theme.drawCentered(g, "?", w / 2.0, 92);
                g.setFont(Theme.bold(15));
                g.setColor(Theme.MUTED);
                Theme.drawCentered(g, "미발견 정리", w / 2.0, 150);
                Lesson l = game.lessonForTheorem(t.id);
                if (l != null) {
                    g.setFont(Theme.serif(12.5f));
                    Theme.drawCentered(g, "'" + l.caseName + "' 해결 시 획득", w / 2.0, 178);
                }
            }
        }
    }

    private static class BadgeView extends JComponent {
        final Badge b;
        final boolean owned;

        BadgeView(Badge b, boolean owned, int value) {
            this.b = b;
            this.owned = owned;
            setPreferredSize(new Dimension(80, 104));
            setToolTipText(b.description + " (" + Math.min(value, b.target) + " / " + b.target + ")");
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth();
            double d = 58, x = (w - d) / 2;
            if (owned) {
                g.setColor(new Color(0xDDB55A));
                g.fill(new Ellipse2D.Double(x, 4, d, d));
                g.setColor(Theme.BORDER_DARK);
                g.setStroke(new BasicStroke(2.5f));
                g.draw(new Ellipse2D.Double(x, 4, d, d));
                Icons.paint(g, b.icon, x + 15, 19, 28, Theme.INK);
            } else {
                g.setColor(Theme.c(0xE6D6B4));
                g.fill(new Ellipse2D.Double(x, 4, d, d));
                g.setColor(Theme.c(0xBFA77D));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{4, 3}, 0));
                g.draw(new Ellipse2D.Double(x, 4, d, d));
                Icons.paint(g, "lock", x + 17, 21, 24, Theme.c(0xA08660));
            }
            g.setFont(Theme.serif(12));
            g.setColor(owned ? Theme.INK : Theme.MUTED);
            int y = 80;
            for (String line : Widgets.TextBlock.wrap(b.name, g.getFontMetrics(), w - 4)) {
                Theme.drawCentered(g, line, w / 2.0, y);
                y += 16;
            }
            g.dispose();
        }
    }
}
