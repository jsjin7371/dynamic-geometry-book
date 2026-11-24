package geomystery.ui;

import geomystery.data.Model.Lesson;
import geomystery.data.Model.Puzzle;
import geomystery.data.Profiles;
import geomystery.data.SaveData;
import geomystery.game.Game;
import geomystery.game.Result;
import geomystery.game.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/** 메인 창. 사이드바가 있는 탭 화면과 전체 화면(대화·풀이·결과)을 CardLayout으로 전환한다. */
public class MainFrame extends JFrame {
    public final Game game;
    public final Profiles profiles;
    /** 테마·프로필을 바꾸느라 새 창으로 갈아 끼우는 중이면 true(창을 닫아도 종료하지 않음). */
    private boolean replaced = false;

    private final CardLayout rootCards = new CardLayout();
    private final JPanel root = new JPanel(rootCards);
    private final CardLayout tabCards = new CardLayout();
    private final JPanel tabs = new JPanel(tabCards);

    private final Sidebar sidebar;
    private final MapScreen map;
    private final BoxScreen box;
    private final AssocScreen assoc;
    private final NotebookScreen notebook;
    private final AnalysisScreen analysis;
    private final FinaleScreen finale;
    private final EndingScreen ending;
    private final CaseBoardScreen board;
    private final OfficeScreen office;
    private final DialogueScreen dialogue;
    private final PuzzleScreen puzzle;
    private final ResultScreen result;

    public MainFrame(Game game, Profiles profiles) {
        super("기하미궁 (幾何迷宮)");
        this.game = game;
        this.profiles = profiles;
        Sfx.setLevel(game.save.sfxLevel);
        Music.setLevel(game.save.musicLevel); // 켜져 있으면 곡을 미리 합성해 둔다
        HazelLook.apply(game);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Image icon = Ui.img("app_icon");
        if (icon != null) {
            setIconImage(icon);
            try {
                if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    Taskbar.getTaskbar().setIconImage(icon);
                }
            } catch (Exception ignored) { }
        }

        sidebar = new Sidebar(this);
        map = new MapScreen(this);
        box = new BoxScreen(this);
        assoc = new AssocScreen(this);
        notebook = new NotebookScreen(this);
        analysis = new AnalysisScreen(this);
        finale = new FinaleScreen(this);
        ending = new EndingScreen(this);
        board = new CaseBoardScreen(this);
        office = new OfficeScreen(this);
        dialogue = new DialogueScreen(this);
        puzzle = new PuzzleScreen(this);
        result = new ResultScreen(this);

        tabs.add(map, "map");
        tabs.add(box, "box");
        tabs.add(assoc, "assoc");
        tabs.add(notebook, "notebook");
        tabs.add(analysis, "analysis");
        tabs.add(board, "board");
        tabs.add(office, "office");

        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(Theme.BG);
        shell.add(sidebar, BorderLayout.WEST);
        shell.add(tabs, BorderLayout.CENTER);

        root.add(shell, "shell");
        root.add(dialogue, "dialogue");
        root.add(puzzle, "puzzle");
        root.add(result, "result");
        root.add(finale, "finale");
        root.add(ending, "ending");
        setContentPane(root);

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1440, screen.width), Math.min(940, screen.height));
        setMinimumSize(new Dimension(1120, 720));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { game.save.save(); }
            @Override public void windowClosed(WindowEvent e) { if (!replaced) System.exit(0); }
            @Override public void windowOpened(WindowEvent e) {
                Theme.warmUp();
                javax.swing.Timer warm = new javax.swing.Timer(400, ev -> {
                    board.prewarm(); dialogue.prewarm(); office.prewarm();
                    double sc = 1;
                    try { sc = Math.max(1, getGraphicsConfiguration().getDefaultTransform().getScaleX()); } catch (Exception ignored) { }
                    RewardFly.prewarm(sc);
                });
                warm.setRepeats(false);
                warm.start();
                String n = game.takeLeagueNotice();
                if (n != null) SwingUtilities.invokeLater(() -> leagueNotice(n));
            }
        });
        showTab("map");
        bindGlobalKeys();
    }

    /** 어디서나 쓰는 단축키: ⌘/Ctrl + 숫자로 메뉴 이동, F1·? 로 단축키 안내. */
    private void bindGlobalKeys() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();
        int mod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        String[] keys = {"map", "box", "assoc", "dex", "analysis", "board", "office", "notebook"};
        for (int i = 0; i < keys.length; i++) {
            final String k = keys[i];
            im.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_1 + i, mod), "tab-" + k);
            am.put("tab-" + k, new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (!"shell".equals(rootCard)) return; // 대화·풀이 도중에는 무시
                    if ("dex".equals(k)) showAssocTab("dex");
                    else if ("assoc".equals(k)) showAssocTab("league");
                    else showTab(k);
                }
            });
        }
        im.put(KeyStroke.getKeyStroke("F1"), "keyHelp");
        im.put(KeyStroke.getKeyStroke("typed ?"), "keyHelp");
        am.put("keyHelp", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { KeyHelp.show(MainFrame.this); }
        });
    }

    /** 글자 크기 단계 → 배율. */
    public static float fontScale(int step) { return step <= 0 ? 0.9f : step >= 2 ? 1.12f : 1f; }

    /**
     * 테마나 프로필이 바뀌었을 때 창을 새로 만든다(색·글꼴은 컴포넌트를 만들 때 정해지므로).
     * 창 위치와 크기는 그대로 유지한다.
     */
    public static MainFrame rebuild(MainFrame old, Game game) {
        game.save.save();
        Theme.apply(game.save.darkMode, fontScale(game.save.fontStep));
        MainFrame f = new MainFrame(game, old.profiles);
        f.setBounds(old.getBounds());
        f.setExtendedState(old.getExtendedState());
        old.replaced = true;
        f.setVisible(true);
        old.dispose();
        return f;
    }

    private void leagueNotice(String msg) {
        if (msg.contains("승급")) Ui.celebrate(this, "리그 승급!", game.tierName() + "에 올라섰습니다");
        else Sfx.play("hint");
        Ui.Modal m = Ui.modal(this, "주간 리그 정산", 500);
        m.text(msg + (msg.contains("승급") ? "\n승급 보상으로 단서 토큰 2개를 받았습니다." : ""));
        m.text("이번 주 리그: " + game.tierName() + "  ·  상위 " + Game.PROMOTE_RANK + "명 승급, 하위 " + Game.DEMOTE_FROM_BOTTOM + "명 강등");
        m.button("확인", Btn.Style.PRIMARY, () -> refreshChrome());
        m.show();
    }

    // ---------------------------------------------------------------- 내비게이션
    /** 지금 진행 중인 장의 곡. */
    private String chapterTrack() { return Music.forScene(game.currentChapter().scene); }

    /** 지금 보이는 큰 화면(shell = 사이드바가 있는 탭 화면). */
    private String rootCard = "shell";
    /** 결과 화면에서 받은 토큰(지도로 돌아가면 상단 알약으로 날아간다). */
    private int pendingTokens;

    public void queueTokenFly(int n) { pendingTokens = Math.max(0, n); }

    /** 큰 화면을 바꾼다. kind 가 있으면 전환 연출과 함께. */
    private void go(String card, Transition.Kind kind, Runnable prepare, Runnable after) {
        Runnable sw = () -> {
            if (prepare != null) prepare.run();
            rootCards.show(root, card);
            rootCard = card;
        };
        if (kind == null) {
            sw.run();
            if (after != null) after.run();
        } else {
            Transition.play(this, kind, sw, after);
        }
    }

    private static Header findHeader(Container c) {
        for (Component k : c.getComponents()) {
            if (!k.isVisible()) continue;
            if (k instanceof Header) return (Header) k;
            if (k instanceof Container) { Header h = findHeader((Container) k); if (h != null) return h; }
        }
        return null;
    }

    private void flyTokens() {
        if (pendingTokens <= 0) return;
        int n = pendingTokens;
        pendingTokens = 0;
        Header h = findHeader(tabs);
        if (h != null) RewardFly.play(this, h, n, game.save.tokens - n);
    }

    public void showTab(String key) {
        Music.play(chapterTrack());
        switch (key) {
            case "map": map.refresh(); break;
            case "box": box.refresh(); break;
            case "assoc": assoc.refresh(); break;
            case "notebook": notebook.refresh(); break;
            case "analysis": analysis.refresh(); break;
            case "board": board.refresh(); break;
            case "office": office.refresh(); break;
            default: break;
        }
        Runnable prep = () -> {
            tabCards.show(tabs, key);
            sidebar.select(key);
        };
        boolean fromFull = !"shell".equals(rootCard);
        go("shell", fromFull && isShowing() ? Transition.Kind.FADE : null, prep, this::flyTokens);
    }

    public void showAssocTab(String sub) {
        assoc.setTab(sub);
        showTab("assoc");
        sidebar.selectDex("dex".equals(sub));
    }

    /** 사건 시작: 등불이 없으면 안내하고, 아니면 도입 대화부터 보여 준다. */
    public void startLesson(Lesson l, boolean skipDialogue) {
        if (!ensureLanterns()) return;
        if (skipDialogue || game.content.dialogueOf(l).isEmpty()) {
            startPuzzles(new Session(l, game.content.puzzlesOf(l), Session.Mode.LESSON));
        } else {
            Music.play(Music.forScene(l.scene));
            if (l.order == 1 && isShowing()) {
                // 장의 첫 사건: 장 표지가 지금 화면 위로 떠올라 다 덮으면 그 뒤에서 대화로 바꾸고,
                // 표지가 사라지면 대사가 시작된다(대화 화면이 표지보다 먼저 보이지 않게)
                ChapterCard.show(this, game.content.chapterOf(l),
                        () -> go("dialogue", null, () -> dialogue.start(l, true), null),
                        dialogue::beginTyping);
            } else {
                go("dialogue", Transition.Kind.BOOK, () -> dialogue.start(l, false), null);
            }
        }
    }

    /** 후일담(호감도를 가득 채운 인물의 대화). */
    public void startEpilogue(geomystery.data.Model.Npc n) {
        geomystery.data.Model.Chapter ch = game.content.chapterOfNpc(n.id);
        if (ch == null) return;
        Music.play(Music.forScene(ch.scene));
        go("dialogue", Transition.Kind.BOOK, () -> dialogue.startEpilogue(n), null);
    }

    public void startPuzzles(Session s) {
        if (s.puzzles.isEmpty()) {
            Ui.toast(this, "풀 수 있는 수수께끼가 없습니다.");
            return;
        }
        Music.play("puzzle");
        go("puzzle", Transition.Kind.FADE, () -> puzzle.start(s), null);
    }

    public void startPractice(List<Puzzle> puzzles, Session.Mode mode) {
        if (!ensureLanterns()) return;
        startPuzzles(new Session(null, puzzles, mode));
    }

    public void showResult(Result r) {
        Music.play(r.session.lesson != null ? Music.forScene(r.session.lesson.scene) : chapterTrack());
        go("result", Transition.Kind.FADE, () -> result.show(r), null);
    }

    /** 마지막 사건을 처음 해결하면 엔딩 크레딧이 올라간다. */
    public void showEnding() {
        go("ending", Transition.Kind.FADE, ending::start, null);
    }

    /** 최종 사건의 수수께끼를 모두 풀면 최종 추리로 넘어간다. */
    public void showFinale(Session s) {
        Music.play("finale");
        go("finale", Transition.Kind.FADE, () -> finale.start(s), null);
    }

    public void toast(String msg) { Ui.toast(this, msg); }

    public void toasts(List<String> msgs) { for (String m : msgs) toast(m); }

    /** 등불이 하나도 없으면 충전 안내 모달을 띄우고 false. */
    public boolean ensureLanterns() {
        if (game.lanterns() > 0) return true;
        long ms = game.msToNextLantern();
        Ui.Modal m = Ui.modal(this, "등불이 모두 꺼졌습니다", 480);
        m.text("어둠 속에서는 수수께끼를 풀 수 없어요. 등불은 30분마다 하나씩 다시 켜집니다.\n"
                + "다음 등불까지 약 " + Math.max(1, ms / 60000) + "분 남았습니다.");
        m.button("닫기", Btn.Style.OUTLINE, null);
        if (game.save.tokens >= Game.REFILL_COST) {
            m.button("단서 토큰 " + Game.REFILL_COST + "개로 모두 켜기", Btn.Style.PRIMARY, () -> {
                game.refillLanterns();
                refreshChrome();
                toast("등불 " + SaveData.MAX_LANTERNS + "개가 모두 켜졌습니다.");
            });
        }
        m.show();
        return false;
    }

    public void refreshChrome() {
        map.refresh();
        box.refresh();
        assoc.refresh();
        notebook.refresh();
        analysis.refresh();
        sidebar.refresh();
        office.refresh();
    }
}
