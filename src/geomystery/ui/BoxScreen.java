package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.data.SaveData;
import geomystery.game.Game;
import geomystery.game.Session;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 수수께끼 상자 — 오답 노트, 무작위 연습, 해결한 사건 다시 풀기. */
public class BoxScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private final Header header;
    private final JPanel content;

    public BoxScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        header = new Header(game);
        header.setTitle("PUZZLE BOX", "수수께끼 상자");
        add(header, BorderLayout.NORTH);

        content = new Ui.TrackWidth(null);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 36, 36, 36));
        add(Ui.scroll(content), BorderLayout.CENTER);
    }

    public void refresh() {
        header.refresh();
        content.removeAll();

        JPanel top = Ui.clear(new GridLayout(1, 2, 24, 0));
        top.setAlignmentX(LEFT_ALIGNMENT);
        List<Puzzle> mistakes = game.mistakePuzzles();
        top.add(actionCard("오답 노트", "틀렸던 수수께끼 " + mistakes.size() + "개가 남아 있어요.\n처음 시도에 맞히면 노트에서 지워집니다.",
                Theme.RED, "refresh", mistakes.isEmpty() ? null : "오답 다시 풀기", () -> {
                    List<Puzzle> ps = new ArrayList<>(mistakes);
                    Collections.shuffle(ps);
                    frame.startPractice(ps.subList(0, Math.min(6, ps.size())), Session.Mode.REVIEW);
                }));
        List<Puzzle> pool = game.practicePool();
        top.add(actionCard("무작위 연습", pool.isEmpty() ? "사건을 하나 이상 해결하면 연습할 수 있어요."
                        : "해결한 사건의 수수께끼 " + pool.size() + "개 중 5개를 골라 풉니다.\n경험치는 문제당 10 XP.",
                Theme.GREEN, "dice", pool.isEmpty() ? null : "5문제 풀기", () -> {
                    List<Puzzle> ps = new ArrayList<>(pool);
                    Collections.shuffle(ps);
                    frame.startPractice(ps.subList(0, Math.min(5, ps.size())), Session.Mode.PRACTICE);
                }));
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        content.add(top);
        content.add(Ui.vgap(34));

        for (Chapter ch : game.content.chapters) {
            content.add(Ui.label(ch.label + "  ·  " + ch.title, Theme.xbold(20), Theme.INK));
            content.add(Ui.vgap(4));
            content.add(Ui.label(ch.subtitle + "  —  " + game.doneCount(ch) + " / " + game.content.lessonsOf(ch).size() + " 해결",
                    Theme.serif(13.5f), Theme.INK_SOFT));
            content.add(Ui.vgap(12));
            for (Lesson l : game.content.lessonsOf(ch)) {
                content.add(lessonRow(l));
                content.add(Ui.vgap(10));
            }
            content.add(Ui.vgap(22));
        }
        content.revalidate();
        content.repaint();
    }

    private JComponent actionCard(String title, String text, Color accent, String icon, String btn, Runnable action) {
        RoundPanel p = new RoundPanel(new BorderLayout(18, 0), Theme.PAPER, Theme.BORDER, 14).borderWidth(2f);
        p.pad(22, 24, 22, 24);
        JComponent ic = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(64, 64); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                g.setColor(accent);
                g.fillOval(0, 0, 64, 64);
                Icons.paint(g, icon, 16, 16, 32, Theme.CREAM);
                g.dispose();
            }
        };
        JPanel icw = Ui.clear(new BorderLayout());
        icw.add(ic, BorderLayout.NORTH);
        p.add(icw, BorderLayout.WEST);
        JPanel t = Ui.vbox();
        t.add(Ui.label(title, Theme.xbold(20), Theme.INK));
        t.add(Ui.vgap(8));
        t.add(new Widgets.TextBlock(text, Theme.serif(14.5f), Theme.INK_SOFT).spacing(1.6f).prefWidth(260));
        p.add(t, BorderLayout.CENTER);
        if (btn != null) {
            Btn b = new Btn(btn, accent == Theme.GREEN ? Btn.Style.GREEN : Btn.Style.PRIMARY).font(15);
            b.addActionListener(e -> action.run());
            JPanel bw = Ui.clear(new GridBagLayout());
            bw.add(b);
            p.add(bw, BorderLayout.EAST);
        }
        return p;
    }

    private JComponent lessonRow(Lesson l) {
        boolean done = game.isDone(l), unlocked = game.isUnlocked(l);
        RoundPanel p = new RoundPanel(new BorderLayout(16, 0), done ? Theme.PAPER : Theme.c(0xEADCBC),
                done ? Theme.BORDER : Theme.c(0xD8C49C), 12).borderWidth(1.6f);
        p.pad(12, 18, 12, 14);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        JComponent mark = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(40, 40); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                int y = (getHeight() - 40) / 2;
                g.setColor(done ? Theme.GREEN : unlocked ? Theme.RED : Theme.c(0xC8B48C));
                g.fillOval(0, y, 40, 40);
                Icons.paint(g, done ? "check" : unlocked ? (l.boss ? "star" : "search") : "lock", 9, y + 9, 22, Theme.CREAM);
                g.dispose();
            }
        };
        p.add(mark, BorderLayout.WEST);
        JPanel t = Ui.vbox();
        t.add(Ui.label((l.boss ? "최종 사건 · " : "사건 " + l.order + " · ") + l.caseName, Theme.bold(16), unlocked ? Theme.INK : Theme.MUTED));
        t.add(Ui.vgap(4));
        String sub = l.title + "  ·  수수께끼 " + game.content.puzzlesOf(l).size() + "개";
        if (done) {
            SaveData.LessonRecord r = game.save.progress.get(l.id);
            sub += "  ·  최고 정확도 " + r.bestAcc + "%  ·  최단 " + Ui.time(r.bestTime) + "  ·  " + r.plays + "회 해결";
        }
        t.add(Ui.label(sub, Theme.serif(13.5f), Theme.INK_SOFT));
        JPanel tw = Ui.clear(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        tw.add(t, c);
        p.add(tw, BorderLayout.CENTER);
        JPanel btns = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (done) {
            Btn quick = new Btn("바로 풀기", Btn.Style.OUTLINE).font(14);
            quick.addActionListener(e -> frame.startLesson(l, true));
            Btn again = new Btn("대화부터", Btn.Style.OUTLINE).font(14);
            again.addActionListener(e -> frame.startLesson(l, false));
            btns.add(again);
            btns.add(quick);
        } else if (unlocked) {
            Btn go = new Btn("사건 시작", Btn.Style.PRIMARY).font(14);
            go.addActionListener(e -> frame.startLesson(l, false));
            btns.add(go);
        } else {
            btns.add(Ui.label("잠김", Theme.serif(14), Theme.MUTED));
        }
        JPanel bw = Ui.clear(new GridBagLayout());
        bw.add(btns);
        p.add(bw, BorderLayout.EAST);
        return p;
    }
}
