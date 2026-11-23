package geomystery.ui;

import geomystery.data.Profiles;
import geomystery.data.SaveData;
import geomystery.game.Game;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;

/** 탐정 수첩 — 프로필, 통계, 최근 7일 경험치, 설정. */
public class NotebookScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private final Header header;
    private final JTextField nameField;
    private final JLabel rank;
    private final Widgets.Bar bar;
    private final JPanel stats;
    private final WeekChart chart;
    private CandleCalendar calendar;
    private Btn endingBtn;
    private final Btn[] speedBtns = new Btn[3];
    private final Btn[] sfxBtns = new Btn[4];
    private final Btn[] musicBtns = new Btn[4];
    private final Btn[] themeBtns = new Btn[2];
    private final Btn[] fontBtns = new Btn[3];
    private JLabel dataPath;
    private JPanel profileList;
    private JComponent lookPreview;
    private JPanel lookSwatches;

    /** 꾸미기 색 고르기 줄(해금된 색은 누르면 바로 적용, 잠긴 색은 조건 안내). */
    private void rebuildLook() {
        lookSwatches.removeAll();
        int[] c = HazelLook.chosen(game);
        String[] labels = {"모자", "코트", "넥타이"};
        HazelLook.Option[][] all = {HazelLook.HATS, HazelLook.COATS, HazelLook.TIES};
        for (int slot = 0; slot < 3; slot++) {
            JPanel row = Ui.clear(new FlowLayout(FlowLayout.LEFT, 5, 2));
            row.setAlignmentX(LEFT_ALIGNMENT);
            JLabel l = Ui.label(labels[slot], Theme.bold(13), Theme.INK);
            l.setPreferredSize(new Dimension(48, 30));
            row.add(l);
            for (int i = 0; i < all[slot].length; i++) {
                HazelLook.Option o = all[slot][i];
                final int fs = slot, fi = i;
                boolean open = o.isUnlocked(game), sel = c[slot] == i;
                JComponent sw = new JComponent() {
                    @Override public Dimension getPreferredSize() { return new Dimension(30, 30); }
                    @Override protected void paintComponent(Graphics g0) {
                        Graphics2D g = Theme.aa(g0.create());
                        g.setColor(open ? o.main : Theme.c(0xD8CBB0));
                        g.fillOval(3, 3, 24, 24);
                        g.setColor(sel ? Theme.RED : Theme.BORDER_DARK);
                        g.setStroke(new BasicStroke(sel ? 3f : 1.4f));
                        g.drawOval(3, 3, 24, 24);
                        if (!open) Icons.paint(g, "lock", 8, 8, 14, Theme.MUTED);
                        g.dispose();
                    }
                };
                sw.setToolTipText(o.name + (open ? (sel ? " (착용 중)" : " — 눌러서 입히기") : " — 잠김: " + o.condition));
                if (open) {
                    sw.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    sw.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                            HazelLook.choose(game, fs, fi);
                            Sfx.play("select");
                            frame.repaint();
                            rebuildLook();
                        }
                    });
                }
                row.add(sw);
            }
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            lookSwatches.add(row);
        }
        lookSwatches.revalidate();
        lookSwatches.repaint();
        lookPreview.repaint();
    }

    public NotebookScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        header = new Header(game);
        header.setTitle("DETECTIVE NOTEBOOK", "탐정 수첩");
        add(header, BorderLayout.NORTH);

        JPanel body = Ui.clear(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        GridBagConstraints c = new GridBagConstraints();

        // ---------- 왼쪽
        JPanel left = Ui.vbox();
        RoundPanel prof = new RoundPanel(new BorderLayout(20, 0), Theme.PAPER, Theme.BORDER, 14).borderWidth(2f);
        prof.pad(20, 22, 20, 22);
        prof.setAlignmentX(LEFT_ALIGNMENT);
        JComponent face = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(96, 96); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                g.setColor(Theme.c(0xE9D8B2));
                g.fillOval(0, 0, 96, 96);
                Ui.drawScaled(g, Ui.img("hazel_face"), 4, 4, 88, 88);
                g.dispose();
            }
        };
        prof.add(face, BorderLayout.WEST);
        JPanel pt = Ui.vbox();
        JPanel nameRow = Ui.hbox();
        nameRow.setAlignmentX(LEFT_ALIGNMENT);
        nameField = new JTextField(14);
        nameField.setFont(Theme.xbold(22));
        nameField.setForeground(Theme.INK);
        nameField.setBackground(Theme.c(0xFFFBF2));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        nameField.setMaximumSize(new Dimension(280, 46));
        nameField.addActionListener(e -> saveName());
        nameRow.add(nameField);
        nameRow.add(Ui.hgap(10));
        Btn save = new Btn("이름 저장", Btn.Style.OUTLINE).font(14);
        save.addActionListener(e -> saveName());
        nameRow.add(save);
        pt.add(nameRow);
        pt.add(Ui.vgap(10));
        rank = Ui.label("", Theme.serif(15), Theme.INK_SOFT);
        pt.add(rank);
        pt.add(Ui.vgap(10));
        bar = new Widgets.Bar(Theme.c(0xE6D6B4), Theme.GOLD_DARK, 10);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        pt.add(bar);
        prof.add(pt, BorderLayout.CENTER);
        prof.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        left.add(prof);
        left.add(Ui.vgap(20));

        stats = Ui.clear(new GridLayout(2, 3, 16, 16));
        stats.setAlignmentX(LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));
        left.add(stats);
        left.add(Ui.vgap(20));

        RoundPanel ch = new RoundPanel(new BorderLayout(0, 10), Theme.PAPER, Theme.BORDER, 14).borderWidth(2f);
        ch.pad(18, 22, 16, 22);
        ch.setAlignmentX(LEFT_ALIGNMENT);
        JPanel chHead = Ui.clear(new BorderLayout());
        chHead.add(Ui.label("최근 7일 경험치", Theme.xbold(18), Theme.INK), BorderLayout.WEST);
        chHead.add(Ui.label("최근 12주 촛불 달력", Theme.xbold(18), Theme.INK), BorderLayout.EAST);
        ch.add(chHead, BorderLayout.NORTH);
        JPanel chBody = Ui.clear(new BorderLayout(28, 0));
        chart = new WeekChart();
        chBody.add(chart, BorderLayout.CENTER);
        calendar = new CandleCalendar();
        chBody.add(calendar, BorderLayout.EAST);
        ch.add(chBody, BorderLayout.CENTER);
        left.add(ch);

        c.gridx = 0; c.gridy = 0; c.weightx = 1; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 26);
        body.add(left, c);

        // ---------- 오른쪽: 설정 · 프로필 · 백업
        JPanel right = Ui.vbox();
        RoundPanel set = section("설정");
        set.add(Ui.label("대사 출력 속도", Theme.bold(15), Theme.INK));
        set.add(Ui.vgap(8));
        set.add(segmented(new String[]{"느림", "보통", "빠름"}, speedBtns, i -> {
            game.save.typingSpeed = i + 1;
            game.save.save();
            refresh();
        }));
        set.add(Ui.vgap(16));
        set.add(Ui.label("효과음", Theme.bold(15), Theme.INK));
        set.add(Ui.vgap(8));
        set.add(segmented(new String[]{"끔", "작게", "보통", "크게"}, sfxBtns, i -> {
            game.save.sfxLevel = i;
            game.save.save();
            Sfx.setLevel(i);
            Sfx.play("correct");
            refresh();
        }));
        set.add(Ui.vgap(16));
        set.add(Ui.label("배경음악", Theme.bold(15), Theme.INK));
        set.add(Ui.vgap(8));
        set.add(segmented(new String[]{"끔", "작게", "보통", "크게"}, musicBtns, i -> {
            game.save.musicLevel = i;
            game.save.save();
            Music.setLevel(i);
            refresh();
        }));
        set.add(Ui.vgap(16));
        set.add(Ui.label("화면 테마", Theme.bold(15), Theme.INK));
        set.add(Ui.vgap(8));
        set.add(segmented(new String[]{"밝은 종이", "어두운 밤"}, themeBtns, i -> {
            if (game.save.darkMode == (i == 1)) return;
            game.save.darkMode = i == 1;
            MainFrame.rebuild(frame, game).showTab("notebook");
        }));
        set.add(Ui.vgap(16));
        set.add(Ui.label("글자 크기", Theme.bold(15), Theme.INK));
        set.add(Ui.vgap(8));
        set.add(segmented(new String[]{"작게", "보통", "크게"}, fontBtns, i -> {
            if (game.save.fontStep == i) return;
            game.save.fontStep = i;
            MainFrame.rebuild(frame, game).showTab("notebook");
        }));
        set.add(Ui.vgap(16));
        Btn keys = new Btn("키보드 단축키 보기  (F1)", Btn.Style.OUTLINE, "keyboard").font(14);
        keys.setAlignmentX(LEFT_ALIGNMENT);
        keys.addActionListener(e -> KeyHelp.show(frame));
        set.add(keys);
        right.add(set);
        right.add(Ui.vgap(18));

        RoundPanel look = section("헤이즐 꾸미기");
        JPanel lookRow = Ui.clear(new BorderLayout(14, 0));
        lookRow.setAlignmentX(LEFT_ALIGNMENT);
        lookPreview = new JComponent() {
            @Override public Dimension getPreferredSize() { return new Dimension(96, 128); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                g.setColor(Theme.c(0xE9D8B2));
                g.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, 96, 128, 14, 14));
                java.awt.image.BufferedImage im = Ui.img("hazel");
                if (im != null) Ui.drawScaled(g, im, 0, 0, 96, 128);
                g.dispose();
            }
        };
        lookRow.add(lookPreview, BorderLayout.WEST);
        lookSwatches = Ui.vbox();
        lookRow.add(lookSwatches, BorderLayout.CENTER);
        look.add(lookRow);
        look.add(Ui.vgap(8));
        look.add(new Widgets.TextBlock("기록을 쌓으면 새 색이 열려요. 잠긴 색에 마우스를 올리면 조건이 보여요.",
                Theme.serif(12), Theme.INK_SOFT).prefWidth(260));
        right.add(look);
        right.add(Ui.vgap(18));

        RoundPanel profBox = section("탐정 프로필");
        profBox.add(new Widgets.TextBlock("한 컴퓨터에서 여러 명이 각자의 수첩으로 풀 수 있어요.", Theme.serif(12.5f), Theme.INK_SOFT).prefWidth(260));
        profBox.add(Ui.vgap(10));
        profileList = Ui.vbox();
        profileList.setAlignmentX(LEFT_ALIGNMENT);
        profBox.add(profileList);
        profBox.add(Ui.vgap(8));
        Btn add = new Btn("새 프로필 만들기", Btn.Style.OUTLINE, "person").font(14);
        add.addActionListener(e -> newProfile());
        profBox.add(add);
        right.add(profBox);
        right.add(Ui.vgap(18));

        RoundPanel bk = section("백업");
        bk.add(new Widgets.TextBlock("수첩을 zip 파일 하나로 내보내 다른 컴퓨터로 옮기거나, 내보낸 파일을 새 프로필로 불러올 수 있어요.",
                Theme.serif(12.5f), Theme.INK_SOFT).prefWidth(260));
        bk.add(Ui.vgap(10));
        JPanel bkb = Ui.clear(new GridLayout(1, 2, 8, 0));
        bkb.setAlignmentX(LEFT_ALIGNMENT);
        Btn exp = new Btn("내보내기", Btn.Style.OUTLINE).font(14);
        exp.addActionListener(e -> exportProfile());
        Btn imp = new Btn("불러오기", Btn.Style.OUTLINE).font(14);
        imp.addActionListener(e -> importProfile());
        bkb.add(exp);
        bkb.add(imp);
        bkb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        bk.add(bkb);
        bk.add(Ui.vgap(16));
        bk.add(Ui.label("저장 위치", Theme.bold(14), Theme.INK));
        bk.add(Ui.vgap(4));
        dataPath = Ui.label("", Theme.serif(12), Theme.INK_SOFT);
        bk.add(dataPath);
        bk.add(Ui.vgap(14));
        Btn reset = new Btn("이 프로필 초기화", Btn.Style.OUTLINE, "refresh").font(14);
        reset.addActionListener(e -> confirmReset());
        bk.add(reset);
        right.add(bk);
        right.add(Ui.vgap(18));

        RoundPanel cred = new RoundPanel(new BorderLayout(0, 8), new Color(0x3A2819), Theme.GOLD_DARK, 14).borderWidth(2f);
        cred.pad(18, 20, 18, 20);
        cred.setAlignmentX(LEFT_ALIGNMENT);
        JPanel credTitle = Ui.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        credTitle.add(Ui.label("기하미궁", Theme.hand(26), Theme.GOLD));
        credTitle.add(Ui.label("  幾何迷宮", Theme.hanja(15), new Color(0xCDB68D)));
        cred.add(credTitle, BorderLayout.NORTH);
        cred.add(new Widgets.TextBlock("헤이즐 탐정과 함께 점·선·각·원·삼각형의 비밀을 풀어 가는 기하 학습 모험. "
                + "글꼴: 나눔명조 · 나눔손글씨 펜 (SIL OFL). 캐릭터와 배경은 Java2D로 직접 구웠고, 효과음과 배경음악은 코드로 합성합니다.",
                Theme.serif(12.5f), new Color(0xE9DCC0)).spacing(1.6f).prefWidth(260), BorderLayout.CENTER);
        endingBtn = new Btn("엔딩 다시 보기", Btn.Style.DARK_OUTLINE, "star").font(13);
        endingBtn.addActionListener(e -> frame.showEnding());
        JPanel eb = Ui.clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
        eb.add(endingBtn);
        cred.add(eb, BorderLayout.SOUTH);
        right.add(cred);
        Ui.TrackWidth rw = new Ui.TrackWidth(new BorderLayout());
        rw.add(right, BorderLayout.NORTH);
        JScrollPane rs = Ui.scroll(rw);
        rs.setPreferredSize(new Dimension(340, 600));
        rs.setMinimumSize(new Dimension(330, 300));
        c.gridx = 1; c.weightx = 0; c.insets = new Insets(0, 0, 0, 0);
        body.add(rs, c);
        add(body, BorderLayout.CENTER);
    }

    private interface IntAction { void run(int i); }

    private static RoundPanel section(String title) {
        RoundPanel p = new RoundPanel(null, Theme.PAPER, Theme.BORDER, 14).borderWidth(2f);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.pad(18, 20, 20, 20);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.add(Ui.label(title, Theme.xbold(20), Theme.INK));
        p.add(Ui.vgap(14));
        return p;
    }

    private static RoundPanel segmented(String[] names, Btn[] out, IntAction act) {
        RoundPanel seg = new RoundPanel(new GridLayout(1, names.length, 4, 0), Theme.c(0xE6D5AF), null, 12);
        seg.pad(4, 4, 4, 4);
        seg.setAlignmentX(LEFT_ALIGNMENT);
        for (int i = 0; i < names.length; i++) {
            final int k = i;
            out[i] = new Btn(names[i], Btn.Style.TAB).font(14);
            out[i].addActionListener(e -> act.run(k));
            seg.add(out[i]);
        }
        seg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        return seg;
    }

    // ------------------------------------------------------------------ 프로필
    private void switchTo(String id) {
        try {
            game.save.save();
            frame.profiles.setActive(id);
            SaveData s = SaveData.load(frame.profiles.dirOf(id));
            MainFrame f = MainFrame.rebuild(frame, new Game(game.content, s));
            f.showTab("map");
            f.toast("'" + s.name + "' 탐정의 수첩을 펼쳤습니다.");
        } catch (Exception ex) {
            frame.toast("프로필을 열지 못했습니다: " + ex.getMessage());
        }
    }

    private void newProfile() {
        Ui.Modal m = Ui.modal(frame, "새 탐정 프로필", 460);
        m.text("새 수첩에 적을 탐정 이름을 입력하세요. (최대 12자)");
        JTextField f = new JTextField(14);
        f.setFont(Theme.bold(18));
        f.setForeground(Theme.INK);
        f.setBackground(Theme.c(0xFFFBF2));
        f.setCaretColor(Theme.INK);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setMaximumSize(new Dimension(380, 44));
        m.add((JComponent) Ui.vgap(10));
        m.add(f);
        m.button("취소", Btn.Style.OUTLINE, null);
        Runnable create = () -> {
            String n = f.getText().trim();
            if (n.length() > 12) n = n.substring(0, 12);
            try {
                switchTo(frame.profiles.create(n));
            } catch (Exception ex) {
                frame.toast("프로필을 만들지 못했습니다: " + ex.getMessage());
            }
        };
        m.button("만들고 펼치기", Btn.Style.PRIMARY, create);
        f.addActionListener(e -> { m.close(); create.run(); });
        m.show();
    }

    private void deleteProfile(Profiles.Entry e) {
        Ui.Modal m = Ui.modal(frame, "'" + e.name + "' 프로필을 지울까요?", 480);
        m.text("이 프로필의 모든 기록(" + Ui.num(e.xp) + " XP)이 지워지고 되돌릴 수 없습니다. 필요하면 먼저 그 프로필로 바꿔 내보내기를 해 두세요.");
        m.button("취소", Btn.Style.OUTLINE, null);
        m.button("지우기", Btn.Style.PRIMARY, () -> {
            try {
                frame.profiles.delete(e.id);
                frame.toast("프로필을 지웠습니다.");
            } catch (Exception ex) {
                frame.toast("지우지 못했습니다: " + ex.getMessage());
            }
            refresh();
        });
        m.show();
    }

    private void exportProfile() {
        game.save.save();
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("탐정 수첩 내보내기");
        fc.setFileFilter(new FileNameExtensionFilter("탐정 수첩 백업 (*.zip)", "zip"));
        String safe = game.save.name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        fc.setSelectedFile(new File("geomystery-" + safe + "-" + game.today() + ".zip"));
        if (fc.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".zip")) out = new File(out.getParentFile(), out.getName() + ".zip");
        try {
            frame.profiles.exportZip(frame.profiles.activeId(), out.toPath());
            frame.toast("내보냈습니다: " + out.getName());
        } catch (Exception ex) {
            frame.toast("내보내지 못했습니다: " + ex.getMessage());
        }
    }

    private void importProfile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("탐정 수첩 불러오기");
        fc.setFileFilter(new FileNameExtensionFilter("탐정 수첩 백업 (*.zip)", "zip"));
        if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        try {
            String id = frame.profiles.importZip(fc.getSelectedFile().toPath());
            String name = id;
            for (Profiles.Entry e : frame.profiles.list()) if (e.id.equals(id)) name = e.name;
            refresh();
            Ui.Modal m = Ui.modal(frame, "불러오기 완료", 460);
            m.text("'" + name + "' 탐정의 수첩을 새 프로필로 불러왔습니다. 지금 펼쳐 볼까요?");
            m.button("나중에", Btn.Style.OUTLINE, null);
            m.button("지금 펼치기", Btn.Style.PRIMARY, () -> switchTo(id));
            m.show();
        } catch (Exception ex) {
            frame.toast("불러오지 못했습니다: " + ex.getMessage());
        }
    }

    private JComponent profileRow(Profiles.Entry e) {
        JPanel row = Ui.clear(new BorderLayout(8, 0));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JPanel t = Ui.vbox();
        t.add(Ui.label(e.name + (e.active ? "  (사용 중)" : ""), e.active ? Theme.bold(15) : Theme.serif(15), Theme.INK));
        t.add(Ui.label(Game.titleFor(1 + e.xp / Game.XP_PER_LEVEL) + " · " + Ui.num(e.xp) + " XP", Theme.serif(12.5f), Theme.INK_SOFT));
        row.add(t, BorderLayout.CENTER);
        if (!e.active) {
            JPanel b = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            Btn open = new Btn("펼치기", Btn.Style.OUTLINE).font(13);
            open.addActionListener(ev -> switchTo(e.id));
            Btn del = new Btn("", Btn.Style.GHOST, "close");
            del.setToolTipText("이 프로필 지우기");
            del.setPreferredSize(new Dimension(38, 38));
            del.addActionListener(ev -> deleteProfile(e));
            b.add(open);
            b.add(del);
            row.add(b, BorderLayout.EAST);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        return row;
    }

    private void saveName() {
        String n = nameField.getText().trim();
        if (n.isEmpty()) n = "견습 탐정";
        if (n.length() > 12) n = n.substring(0, 12);
        game.save.name = n;
        game.save.save();
        frame.toast("탐정 이름을 '" + n + "'(으)로 바꿨습니다.");
        refresh();
    }

    private void confirmReset() {
        Ui.Modal m = Ui.modal(frame, "이 프로필을 초기화할까요?", 480);
        m.text("'" + game.save.name + "' 수첩의 사건 기록, 경험치, 토큰, 오답 노트, 풀이 분석, 연속 학습 기록이 지워집니다. 되돌릴 수 없어요.");
        m.button("취소", Btn.Style.OUTLINE, null);
        m.button("초기화", Btn.Style.PRIMARY, () -> {
            game.save.reset();
            frame.refreshChrome();
            frame.toast("새 수첩으로 다시 시작합니다.");
        });
        m.show();
    }

    private RoundPanel statCard(String label, String value, Color color) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 6), Theme.PAPER, Theme.BORDER, 12).borderWidth(1.8f);
        p.pad(16, 18, 16, 18);
        p.add(Ui.label(label, Theme.serif(13.5f), Theme.INK_SOFT), BorderLayout.NORTH);
        p.add(Ui.label(value, Theme.xbold(28), color), BorderLayout.CENTER);
        return p;
    }

    public void refresh() {
        header.refresh();
        nameField.setText(game.save.name);
        rank.setText(game.title() + " Lv." + game.level() + "  ·  누적 " + Ui.num(game.save.xp) + " XP");
        bar.setValue(game.levelProgress());
        stats.removeAll();
        stats.add(statCard("해결한 사건", game.lessonsDone() + " / " + game.content.lessons.size(), Theme.GREEN));
        stats.add(statCard("푼 수수께끼", "" + game.save.solvedTotal, Theme.INK));
        stats.add(statCard("첫 시도 정답률", game.overallAccuracy() + "%", Theme.GREEN));
        stats.add(statCard("최장 연속 학습", Math.max(game.save.bestStreak, game.streakNow()) + "일", Theme.ORANGE));
        stats.add(statCard("정리 카드", game.cards().size() + " / " + game.content.theorems.size(), Theme.RED));
        stats.add(statCard("보조선 사용", game.save.auxTotal + "회", Theme.INK));
        stats.revalidate();
        for (int i = 0; i < 3; i++) speedBtns[i].setSelected(game.save.typingSpeed == i + 1);
        for (int i = 0; i < 4; i++) sfxBtns[i].setSelected(game.save.sfxLevel == i);
        for (int i = 0; i < 4; i++) musicBtns[i].setSelected(game.save.musicLevel == i);
        themeBtns[0].setSelected(!game.save.darkMode);
        themeBtns[1].setSelected(game.save.darkMode);
        for (int i = 0; i < 3; i++) fontBtns[i].setSelected(game.save.fontStep == i);
        rebuildLook();
        profileList.removeAll();
        for (Profiles.Entry e : frame.profiles.list()) {
            profileList.add(profileRow(e));
            profileList.add(Ui.vgap(8));
        }
        profileList.revalidate();
        dataPath.setText(game.save.dir().toAbsolutePath().toString());
        dataPath.setToolTipText(dataPath.getText());
        chart.repaint();
        calendar.repaint();
        endingBtn.setVisible(game.allCleared());
    }

    /** 최근 12주를 요일×주 칸으로 보여 주는 달력. 그날 얻은 경험치가 많을수록 촛불이 밝다. */
    private class CandleCalendar extends JComponent {
        private static final int CELL = 15, GAP = 3, WEEKS = 12;

        CandleCalendar() {
            setPreferredSize(new Dimension(26 + WEEKS * (CELL + GAP), 24 + 7 * (CELL + GAP) + 26));
            setToolTipText("");
        }

        private LocalDate firstMonday() {
            return game.today().with(java.time.DayOfWeek.MONDAY).minusWeeks(WEEKS - 1);
        }

        private LocalDate dateAt(int x, int y) {
            int col = (x - 26) / (CELL + GAP), row = (y - 24) / (CELL + GAP);
            if (x < 26 || y < 24 || col < 0 || col >= WEEKS || row < 0 || row > 6) return null;
            return firstMonday().plusDays(col * 7L + row);
        }

        @Override public String getToolTipText(java.awt.event.MouseEvent e) {
            LocalDate d = dateAt(e.getX(), e.getY());
            if (d == null || d.isAfter(game.today())) return null;
            int[] v = game.save.history.get(d.toString());
            return d.getMonthValue() + "월 " + d.getDayOfMonth() + "일 · " + (v == null ? "쉬어 간 날" : v[0] + " XP · 수수께끼 " + v[1] + "개");
        }

        private Color shade(int xp) {
            if (xp <= 0) return Theme.c(0xE6D6B4);
            if (xp < 60) return new Color(0xEFCB7A);
            if (xp < 150) return new Color(0xE0A445);
            if (xp < 300) return new Color(0xCF7A2E);
            return new Color(0xB2452A);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            LocalDate first = firstMonday(), today = game.today();
            String[] dn = {"월", "", "수", "", "금", "", "일"};
            g.setFont(Theme.serif(10.5f));
            g.setColor(Theme.INK_SOFT);
            for (int r = 0; r < 7; r++) if (!dn[r].isEmpty()) g.drawString(dn[r], 4, 24 + r * (CELL + GAP) + 12);
            int lastMonth = -1;
            for (int c = 0; c < WEEKS; c++) {
                LocalDate wk = first.plusWeeks(c);
                int x = 26 + c * (CELL + GAP);
                if (wk.getMonthValue() != lastMonth) {
                    lastMonth = wk.getMonthValue();
                    g.setColor(Theme.INK_SOFT);
                    g.drawString(lastMonth + "월", x, 14);
                }
                for (int r = 0; r < 7; r++) {
                    LocalDate d = wk.plusDays(r);
                    int y = 24 + r * (CELL + GAP);
                    if (d.isAfter(today)) continue;
                    int[] v = game.save.history.get(d.toString());
                    g.setColor(shade(v == null ? 0 : v[0]));
                    g.fillRoundRect(x, y, CELL, CELL, 4, 4);
                    if (d.equals(today)) {
                        g.setColor(Theme.RED);
                        g.setStroke(new BasicStroke(2f));
                        g.drawRoundRect(x - 1, y - 1, CELL + 1, CELL + 1, 5, 5);
                    }
                }
            }
            int ly = 24 + 7 * (CELL + GAP) + 14;
            g.setColor(Theme.INK_SOFT);
            g.drawString("적음", 26, ly + 2);
            int[] samples = {0, 30, 100, 200, 400};
            for (int i = 0; i < samples.length; i++) {
                g.setColor(shade(samples[i]));
                g.fillRoundRect(58 + i * 14, ly - 9, 11, 11, 3, 3);
            }
            g.setColor(Theme.INK_SOFT);
            g.drawString("많음", 58 + samples.length * 14 + 4, ly + 2);
            g.dispose();
        }
    }

    private class WeekChart extends JComponent {
        WeekChart() { setPreferredSize(new Dimension(500, 170)); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            LocalDate today = game.today();
            int[] v = new int[7];
            int max = 50;
            for (int i = 0; i < 7; i++) {
                int[] d = game.save.history.get(today.minusDays(6 - i).toString());
                v[i] = d == null ? 0 : d[0];
                max = Math.max(max, v[i]);
            }
            int base = h - 26, top = 22;
            g.setColor(Theme.c(0xE2D2AE));
            g.drawLine(0, base, w, base);
            String[] dn = {"월", "화", "수", "목", "금", "토", "일"};
            double cw = w / 7.0, bw = Math.min(46, cw * 0.5);
            for (int i = 0; i < 7; i++) {
                LocalDate d = today.minusDays(6 - i);
                double bh = (base - top) * v[i] / (double) max;
                double x = cw * i + (cw - bw) / 2;
                boolean isToday = i == 6;
                g.setColor(isToday ? Theme.RED : Theme.GOLD_DARK);
                if (v[i] > 0) g.fill(new RoundRectangle2D.Double(x, base - bh, bw, bh, 6, 6));
                g.setFont(Theme.serif(12));
                g.setColor(Theme.INK_SOFT);
                if (v[i] > 0) Theme.drawCentered(g, "" + v[i], x + bw / 2, base - bh - 10);
                g.setFont(isToday ? Theme.bold(12.5f) : Theme.serif(12.5f));
                g.setColor(isToday ? Theme.RED : Theme.INK_SOFT);
                Theme.drawCentered(g, isToday ? "오늘" : dn[d.getDayOfWeek().getValue() - 1], x + bw / 2, base + 14);
            }
            g.dispose();
        }
    }
}
