package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;
import geomystery.game.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Map;

/** 추리 분석 — 정리별 실력, 자주 하는 실수, 문제 유형별 정답률, 약점 집중 연습. */
public class AnalysisScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private final Header header;
    private final JPanel skillRows;
    private final JPanel focus, slips, types;

    public AnalysisScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        header = new Header(game);
        header.setTitle("CASE ANALYSIS", "추리 분석");
        add(header, BorderLayout.NORTH);

        JPanel body = Ui.clear(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        GridBagConstraints c = new GridBagConstraints();

        // ---------- 왼쪽: 정리별 실력표
        RoundPanel table = new RoundPanel(new BorderLayout(0, 0), Theme.PAPER, Theme.BORDER_DARK, 14).borderWidth(2f);
        table.pad(0, 0, 8, 0);
        JPanel th = new JPanel(new BorderLayout(16, 0)) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                g.setColor(Theme.DARK);
                g.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() + 20, 12, 12));
                g.dispose();
            }
        };
        th.setOpaque(false);
        th.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JPanel tht = Ui.vbox();
        tht.add(Ui.label("정리별 실력", Theme.xbold(20), Theme.CREAM));
        tht.add(Ui.vgap(4));
        tht.add(Ui.label("첫 시도 정답률 · 평균 풀이 시간 · 힌트 사용으로 약한 정리부터 보여 줍니다", Theme.serif(13), new Color(0xD8C39A)));
        th.add(tht, BorderLayout.CENTER);
        table.add(th, BorderLayout.NORTH);
        skillRows = Ui.vbox();
        Ui.TrackWidth tw = new Ui.TrackWidth(new BorderLayout());
        tw.add(skillRows, BorderLayout.NORTH);
        table.add(Ui.scroll(tw), BorderLayout.CENTER);
        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 26);
        body.add(table, c);

        // ---------- 오른쪽
        JPanel right = Ui.vtrack();
        focus = card(Theme.GREEN, Theme.GOLD_DARK);
        slips = card(Theme.PAPER, Theme.BORDER);
        types = card(Theme.PAPER, Theme.BORDER);
        right.add(focus);
        right.add(Ui.vgap(16));
        right.add(slips);
        right.add(Ui.vgap(16));
        right.add(types);
        right.add(Box.createVerticalGlue());
        // 카드 오른쪽 테두리가 스크롤바에 가려지지 않도록 여백을 둔다
        right.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 10));
        JScrollPane rs = Ui.scroll(right);
        rs.setPreferredSize(new Dimension(340, 600));
        rs.setMinimumSize(new Dimension(320, 300));
        c.gridx = 1; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0);
        body.add(rs, c);
        add(body, BorderLayout.CENTER);
    }

    private static RoundPanel card(Color fill, Color border) {
        RoundPanel p = new RoundPanel(null, fill, border, 14).borderWidth(2f);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.pad(18, 20, 18, 20);
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    public void refresh() {
        header.refresh();
        List<Game.Skill> skills = game.skills();

        // 정리별 표
        skillRows.removeAll();
        if (skills.isEmpty()) {
            JPanel e = Ui.vbox();
            e.setBorder(BorderFactory.createEmptyBorder(40, 30, 40, 30));
            e.add(Ui.label("아직 분석할 기록이 없어요", Theme.xbold(19), Theme.INK));
            e.add(Ui.vgap(8));
            e.add(new Widgets.TextBlock("사건을 풀면 문제마다 첫 시도 정답 여부, 풀이 시간, 힌트 사용이 기록되고 "
                    + "여기에서 정리별로 정리해 보여 줍니다.", Theme.serif(14.5f), Theme.INK_SOFT).prefWidth(480));
            skillRows.add(e);
        } else {
            for (Game.Skill k : skills) skillRows.add(new SkillRow(k));
        }
        skillRows.revalidate();

        // 집중 연습 추천
        focus.removeAll();
        focus.add(Ui.label("오늘의 집중 수사", Theme.tracked(Theme.serif(13), 0.1f), new Color(0xCFE0D6)));
        focus.add(Ui.vgap(8));
        Game.Skill weak = null;
        for (Game.Skill k : skills) if (k.score() < 85) { weak = k; break; }
        if (weak == null) {
            focus.add(Ui.label(skills.isEmpty() ? "기록을 모으는 중" : "약점이 보이지 않아요!", Theme.xbold(22), Theme.CREAM));
            focus.add(Ui.vgap(8));
            focus.add(new Widgets.TextBlock(skills.isEmpty() ? "사건을 하나 해결하면 추천이 나타납니다."
                    : "모든 정리를 탄탄하게 익혔어요. 오답 노트나 무작위 연습으로 감을 유지해 보세요.",
                    Theme.serif(14), new Color(0xE3EDE6)).prefWidth(280));
        } else {
            final Game.Skill w = weak;
            focus.add(Ui.label(w.theorem.name, Theme.xbold(23), Theme.CREAM));
            focus.add(Ui.vgap(8));
            focus.add(new Widgets.TextBlock("첫 시도 정답률 " + w.accuracy() + "% · 평균 " + w.avgSeconds() + "초 · 힌트 "
                    + String.format("%.1f", w.hintsPerPuzzle()) + "개/문제\n'" + w.lesson.caseName + "'의 수수께끼 중 자주 틀린 것부터 다시 풀어요.",
                    Theme.serif(14), new Color(0xE3EDE6)).spacing(1.6f).prefWidth(280));
            focus.add(Ui.vgap(14));
            Btn go = new Btn("집중 연습 시작", Btn.Style.CREAM, "search").font(15);
            go.addActionListener(e -> drill(w));
            focus.add(go);
        }
        focus.setMaximumSize(new Dimension(Integer.MAX_VALUE, focus.getPreferredSize().height));

        // 자주 하는 실수
        slips.removeAll();
        slips.add(Ui.label("자주 하는 실수", Theme.xbold(19), Theme.INK));
        slips.add(Ui.vgap(10));
        List<Map.Entry<String, Integer>> top = game.topSlips();
        if (top.isEmpty()) {
            slips.add(new Widgets.TextBlock("틀린 답을 살펴보고 실수 유형(보각 혼동, 절반 계산 등)을 찾아 여기에 모아 둡니다. 아직 발견된 실수가 없어요.",
                    Theme.serif(13.5f), Theme.INK_SOFT).prefWidth(280));
        } else {
            int shown = 0;
            for (Map.Entry<String, Integer> e : top) {
                if (shown++ >= 5) break;
                Misconception m = game.content.misconceptions.get(e.getKey());
                String name = m != null ? m.name : e.getKey();
                JPanel row = Ui.clear(new BorderLayout(8, 4));
                row.setAlignmentX(LEFT_ALIGNMENT);
                row.add(Ui.label(name, Theme.bold(15), Theme.INK), BorderLayout.WEST);
                row.add(Ui.label(e.getValue() + "회", Theme.bold(15), Theme.RED), BorderLayout.EAST);
                if (m != null && m.tip != null && !m.tip.isEmpty()) {
                    row.add(new Widgets.TextBlock(m.tip, Theme.serif(13), Theme.INK_SOFT).spacing(1.5f).prefWidth(280), BorderLayout.SOUTH);
                }
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 30));
                slips.add(row);
                slips.add(Ui.vgap(12));
            }
        }
        slips.setMaximumSize(new Dimension(Integer.MAX_VALUE, slips.getPreferredSize().height + 60));

        // 유형별 정답률
        types.removeAll();
        types.add(Ui.label("문제 유형별 첫 시도 정답률", Theme.xbold(19), Theme.INK));
        types.add(Ui.vgap(12));
        for (Map.Entry<String, int[]> e : game.typeStats().entrySet()) {
            int solved = e.getValue()[0], first = e.getValue()[1];
            JPanel row = Ui.clear(new BorderLayout(0, 6));
            row.setAlignmentX(LEFT_ALIGNMENT);
            JPanel tr = Ui.clear(new BorderLayout());
            tr.add(Ui.label(Puzzle.typeName(e.getKey()), Theme.serif(14.5f), Theme.INK), BorderLayout.WEST);
            tr.add(Ui.label(solved == 0 ? "기록 없음" : Math.round(first * 100f / solved) + "%  (" + solved + "문제)",
                    Theme.bold(14), solved == 0 ? Theme.MUTED : Theme.INK), BorderLayout.EAST);
            row.add(tr, BorderLayout.NORTH);
            Widgets.Bar b = new Widgets.Bar(Theme.c(0xE6D6B4), Theme.GREEN, 8);
            b.setValue(solved == 0 ? 0 : first / (float) solved);
            row.add(b, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            types.add(row);
            types.add(Ui.vgap(10));
        }
        types.setMaximumSize(new Dimension(Integer.MAX_VALUE, types.getPreferredSize().height + 40));
        revalidate();
        repaint();
    }

    private void drill(Game.Skill k) {
        List<Puzzle> ps = game.drillFor(k);
        if (ps.isEmpty()) return;
        frame.startPractice(ps, Session.Mode.PRACTICE);
    }

    // ------------------------------------------------------------------
    private class SkillRow extends JComponent {
        final Game.Skill k;
        boolean hover;

        SkillRow(Game.Skill k) {
            this.k = k;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(600, 78));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("클릭하면 이 정리의 집중 연습을 시작합니다 (" + k.seen + " / " + k.puzzles + "문제 기록, 시도 " + k.attempts + "회)");
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) { drill(k); }
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            if (hover) {
                g.setColor(Theme.c(0xF2E6CB));
                g.fillRect(0, 0, w, h);
            }
            g.setColor(Theme.c(0xE6D6B6));
            g.fillRect(0, h - 1, w, 1);
            Icons.theorem(g, k.theorem.icon, new Rectangle2D.Double(22, 13, 52, 52), Theme.INK, Theme.RED);
            String v = k.verdict();
            Color vc = "약점".equals(v) ? Theme.RED : "보통".equals(v) ? Theme.ORANGE : Theme.GREEN;
            g.setFont(Theme.bold(16.5f));
            g.setColor(Theme.INK);
            g.drawString(k.theorem.name, 92, 32);
            g.setFont(Theme.serif(13));
            g.setColor(Theme.INK_SOFT);
            g.drawString(k.lesson.caseName + " · " + k.seen + "/" + k.puzzles + "문제 · 평균 " + k.avgSeconds() + "초 · 힌트 "
                    + k.hints + "개", 92, 56);
            // 정답률 막대
            int bx = w - 330, bw = 170;
            g.setColor(Theme.c(0xE6D6B4));
            g.fill(new RoundRectangle2D.Float(bx, h / 2f - 5, bw, 10, 10, 10));
            int acc = Math.max(0, k.accuracy());
            g.setColor(vc);
            if (acc > 0) g.fill(new RoundRectangle2D.Float(bx, h / 2f - 5, Math.max(10, bw * acc / 100f), 10, 10, 10));
            g.setFont(Theme.bold(15));
            g.setColor(Theme.INK);
            String pct = acc + "%";
            g.drawString(pct, bx + bw + 12, h / 2 + 6);
            // 판정 꼬리표
            g.setFont(Theme.bold(13));
            FontMetrics fm = g.getFontMetrics();
            int tw = fm.stringWidth(v) + 18;
            g.setColor(vc);
            g.fill(new RoundRectangle2D.Float(w - 24 - tw, h / 2f - 13, tw, 26, 26, 26));
            g.setColor(Theme.CREAM);
            g.drawString(v, w - 24 - tw + 9, h / 2 + 5);
            g.dispose();
        }
    }
}
