package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.diagram.DiagramView;
import geomystery.game.Expr;
import geomystery.game.Game;
import geomystery.game.Result;
import geomystery.game.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 02 수수께끼 풀이 — 쪽지, 헤이즐의 말풍선, 힌트 수첩, 도형 칠판, 답안.
 * 답안 영역은 문제 유형(숫자·객관식·식·참거짓·추리 순서)에 따라 바뀐다.
 */
public class PuzzleScreen extends JPanel {
    private enum State { INPUT, SOLVED }

    private final MainFrame frame;
    private final Game game;
    private Session session;
    private Puzzle puzzle;
    private State state = State.INPUT;
    /** 숫자·식 입력 내용. */
    private String input = "";
    /** 객관식 선택 / 참거짓의 이유 선택. */
    private String choice = null;
    /** 참거짓의 판단(참/거짓). */
    private String verdict = null;
    /** 추리 순서: 아직 놓지 않은 단계 / 놓은 순서. */
    private final List<String> pool = new ArrayList<>();
    private final List<String> placed = new ArrayList<>();

    private final JLabel topSmall, topTitle;
    private final SegBar seg;
    private final Widgets.Pill lanternPill, tokenPill, rewardPill;

    private final JLabel noteTitle;
    private final Widgets.TextBlock noteText;
    private final Widgets.TextBlock bubble;
    private final JPanel hintList;
    private final DiagramView diagram;
    private final Btn auxBtn, exploreBtn, replayBtn;
    private final Widgets.FaceView hazel;
    private final Btn[] penBtns = new Btn[5];

    private final RoundPanel answerPanel;
    private final CardLayout answerCards = new CardLayout();
    private final JPanel answerArea = Ui.clear(answerCards);
    private final JLabel answerLabel, choiceLabel, exprLabel, tfLabel, orderLabel;
    private final Display display, exprDisplay;
    private final Btn submitKey, exprSubmit;
    private final JPanel choiceGrid;
    private final List<Btn> choiceBtns = new ArrayList<>();
    private final Btn choiceSubmit;
    // 참/거짓
    private final Btn tfTrue, tfFalse, tfSubmit;
    private final JPanel tfReasons;
    private final List<Btn> reasonBtns = new ArrayList<>();
    // 추리 순서
    private final JPanel orderPool, orderPlaced;
    private final Btn orderSubmit, orderReset;

    private static final String[] WRONG_LINES = {
            "흠... 다시 생각해 보자. 등불이 하나 꺼졌어.",
            "아깝다! 단서를 다시 한 번 살펴봐.",
            "그 답은 아닌 것 같아. 도형을 다시 보자고.",
            "이런, 자물쇠가 꿈쩍도 안 하네. 한 번 더!"
    };
    private static final String[] RIGHT_LINES = {"정답이야!", "역시 탐정다워!", "딸깍, 자물쇠가 열렸어!", "훌륭한 추리야!"};
    private final Random rnd = new Random();

    public PuzzleScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        // ---------------- 상단 바
        JPanel top = new DialogueScreen.DarkBar(new BorderLayout(24, 0));
        top.setBorder(BorderFactory.createEmptyBorder(16, 30, 16, 30));
        JPanel tl = Ui.hbox();
        Btn close = new Btn("", Btn.Style.CIRCLE_DARK, "close");
        close.setToolTipText("그만두기");
        close.addActionListener(e -> confirmQuit());
        tl.add(close);
        tl.add(Ui.hgap(18));
        JPanel tt = Ui.vbox();
        topSmall = Ui.label("", Theme.bold(13), new Color(0xD8C39A));
        topTitle = Ui.label("", Theme.xbold(22), Theme.CREAM);
        tt.add(topSmall);
        tt.add(Ui.vgap(2));
        tt.add(topTitle);
        tl.add(tt);
        top.add(tl, BorderLayout.WEST);
        seg = new SegBar();
        JPanel segWrap = Ui.clear(new GridBagLayout());
        GridBagConstraints sc = new GridBagConstraints();
        sc.weightx = 1; sc.fill = GridBagConstraints.HORIZONTAL;
        segWrap.add(seg, sc);
        top.add(segWrap, BorderLayout.CENTER);
        JPanel tr = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        lanternPill = new Widgets.Pill("lantern", "", Widgets.Pill.Kind.DARK);
        tokenPill = new Widgets.Pill("token", "", Widgets.Pill.Kind.DARK);
        rewardPill = new Widgets.Pill(null, "", Widgets.Pill.Kind.RED);
        tr.add(lanternPill);
        tr.add(tokenPill);
        tr.add(rewardPill);
        top.add(Ui.center(tr), BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // ---------------- 본문
        JPanel body = Ui.clear(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(22, 32, 24, 32));
        GridBagConstraints c = new GridBagConstraints();

        // 왼쪽 열
        Ui.TrackWidth leftInner = new Ui.TrackWidth(null);
        leftInner.setLayout(new BoxLayout(leftInner, BoxLayout.Y_AXIS));
        leftInner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));

        RoundPanel note = new RoundPanel(new BorderLayout(0, 14), Theme.PAPER, Theme.BORDER_DARK, 6)
                .shadow(7, Theme.c(0xD2BC90)).borderWidth(1.8f);
        note.pad(18, 22, 20, 20);
        note.setAlignmentX(LEFT_ALIGNMENT);
        noteTitle = Ui.label("", Theme.hand(25), Theme.RED);
        note.add(noteTitle, BorderLayout.NORTH);
        noteText = new Widgets.TextBlock("", Theme.serif(16f), Theme.INK).spacing(1.7f).prefWidth(320);
        note.add(noteText, BorderLayout.CENTER);
        leftInner.add(note);
        leftInner.add(Ui.vgap(16));

        JPanel hz = Ui.clear(new BorderLayout(12, 0));
        hz.setAlignmentX(LEFT_ALIGNMENT);
        hazel = new Widgets.FaceView("hazel_face", 78);
        hz.add(hazel, BorderLayout.WEST);
        RoundPanel bub = new RoundPanel(new BorderLayout(), Theme.c(0xFFFBF2), Theme.INK, 14).borderWidth(2f);
        bub.pad(12, 16, 12, 16);
        bubble = new Widgets.TextBlock("", Theme.serif(14.5f), Theme.INK).spacing(1.6f).prefWidth(230);
        bub.add(bubble, BorderLayout.CENTER);
        hz.add(bub, BorderLayout.CENTER);
        leftInner.add(hz);
        leftInner.add(Ui.vgap(20));

        leftInner.add(Ui.label("힌트 수첩", Theme.xbold(18), Theme.INK));
        leftInner.add(Ui.vgap(10));
        hintList = Ui.vbox();
        hintList.setAlignmentX(LEFT_ALIGNMENT);
        leftInner.add(hintList);

        JScrollPane leftScroll = Ui.scroll(leftInner);
        leftScroll.setPreferredSize(new Dimension(392, 500));
        leftScroll.setMinimumSize(new Dimension(360, 300));
        c.gridx = 0; c.gridy = 0; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 24);
        body.add(leftScroll, c);

        // 오른쪽: 칠판 + 답안
        JPanel right = Ui.clear(new BorderLayout(0, 20));
        RoundPanel board = new RoundPanel(new BorderLayout(), Theme.GREEN, Theme.GOLD, 16).borderWidth(3f)
                .doubleBorder(Theme.alpha(Theme.GOLD, 0.35f));
        board.pad(16, 18, 16, 18);
        JPanel boardTop = Ui.clear(new BorderLayout());
        JPanel btl = Ui.clear(new FlowLayout(FlowLayout.LEFT, 8, 0));
        auxBtn = new Btn("보조선 긋기", Btn.Style.DARK_OUTLINE, "aux").font(14);
        auxBtn.arc(40);
        auxBtn.addActionListener(e -> toggleAux());
        btl.add(auxBtn);
        exploreBtn = new Btn("정리 탐구 — 직접 움직여 보기", Btn.Style.CREAM, "compass").font(14);
        exploreBtn.arc(40);
        exploreBtn.setToolTipText("이 사건의 정리를 도형을 움직이며 확인해 보세요");
        exploreBtn.addActionListener(e -> openExplore());
        exploreBtn.setVisible(false);
        replayBtn = new Btn("풀이 다시 보기", Btn.Style.DARK_OUTLINE, "refresh").font(14);
        replayBtn.arc(40);
        replayBtn.setToolTipText("칠판에서 풀이 과정을 한 단계씩 다시 보여 줍니다");
        replayBtn.addActionListener(e -> toggleReplay());
        replayBtn.setVisible(false);
        btl.add(replayBtn);
        btl.add(exploreBtn);
        boardTop.add(btl, BorderLayout.WEST);
        // 낙서 펜 도구
        JPanel pens = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        String[] tips = {"분필(흰색)", "분필(노랑)", "분필(빨강)", "지우개 — 낙서를 문질러 지우기", "낙서 모두 지우기"};
        Color[] dots = {new Color(0xF7F1E3), new Color(0xF2CE63), new Color(0xF08A70), null, null};
        for (int i = 0; i < 5; i++) {
            final int k = i;
            Btn b = new PenButton(dots[i], i == 3 ? "eraser" : i == 4 ? "trash" : null);
            b.setToolTipText(tips[i] + (i < 3 ? " — 칠판 위에 직접 써 보세요. 한 번 더 누르면 펜을 내려놓아요" : ""));
            b.addActionListener(e -> {
                togglePen(PEN_TOOLS[k]);
            });
            penBtns[i] = b;
            pens.add(b);
        }
        JLabel penLabel = Ui.label("낙서 펜", Theme.serif(13), new Color(0xCFE0D6));
        pens.add(penLabel, 0);
        boardTop.add(pens, BorderLayout.EAST);
        board.add(boardTop, BorderLayout.NORTH);
        diagram = new DiagramView();
        board.add(diagram, BorderLayout.CENTER);
        right.add(board, BorderLayout.CENTER);

        answerPanel = new RoundPanel(new BorderLayout(), Theme.PAPER, Theme.BORDER_DARK, 16).borderWidth(1.8f);
        answerPanel.pad(18, 20, 18, 20);
        answerPanel.add(answerArea, BorderLayout.CENTER);

        // ---- 숫자 입력 카드
        JPanel numCard = Ui.clear(new BorderLayout(24, 0));
        JPanel disp = Ui.clear(new BorderLayout(0, 10));
        answerLabel = Ui.label("", Theme.serif(14), Theme.INK_SOFT);
        disp.add(answerLabel, BorderLayout.NORTH);
        display = new Display();
        disp.add(display, BorderLayout.CENTER);
        numCard.add(fillWidth(disp), BorderLayout.CENTER);
        JPanel keys = Ui.clear(new GridLayout(3, 4, 10, 8));
        Btn del = new Btn("지우기", Btn.Style.KEY_ALT).font(15);
        del.addActionListener(e -> backspace());
        submitKey = new Btn("제출", Btn.Style.PRIMARY).font(16);
        submitKey.addActionListener(e -> submit());
        // 계산기 배열: 7 8 9 0 / 4 5 6 지우기 / 1 2 3 제출
        for (String k : new String[]{"7", "8", "9", "0", "4", "5", "6", "DEL", "1", "2", "3", "SUBMIT"}) {
            if (k.equals("DEL")) { keys.add(del); continue; }
            if (k.equals("SUBMIT")) { keys.add(submitKey); continue; }
            Btn b = new Btn(k, Btn.Style.KEY);
            b.addActionListener(e -> type(k));
            keys.add(b);
        }
        keys.setPreferredSize(new Dimension(500, 166));
        numCard.add(keys, BorderLayout.EAST);
        answerArea.add(numCard, "num");

        // ---- 식 입력 카드
        JPanel exprCard = Ui.clear(new BorderLayout(20, 0));
        JPanel ed = Ui.clear(new BorderLayout(0, 10));
        exprLabel = Ui.label("", Theme.serif(14), Theme.INK_SOFT);
        ed.add(exprLabel, BorderLayout.NORTH);
        exprDisplay = new Display();
        ed.add(exprDisplay, BorderLayout.CENTER);
        ed.add(Ui.label("곱셈 기호는 생략해도 돼요 (2x, 2√3, 6π)  ·  키보드: p → π, r → √", Theme.serif(12.5f), Theme.MUTED), BorderLayout.SOUTH);
        exprCard.add(fillWidth(ed), BorderLayout.CENTER);
        JPanel ekeys = Ui.clear(new GridLayout(4, 6, 8, 7));
        String[] ek = {"7", "8", "9", "x", "(", ")", "4", "5", "6", "+", "-", "/", "1", "2", "3", "×", ".", "²", "0", "√", "π"};
        for (String k : ek) {
            Btn b = new Btn(k, "0123456789".contains(k) ? Btn.Style.KEY : Btn.Style.KEY_ALT).font(19);
            b.addActionListener(e -> type(k));
            ekeys.add(b);
        }
        Btn edel = new Btn("지우기", Btn.Style.KEY_ALT).font(14);
        edel.addActionListener(e -> backspace());
        ekeys.add(edel);
        Btn eclr = new Btn("비우기", Btn.Style.KEY_ALT).font(14);
        eclr.addActionListener(e -> { if (state == State.INPUT) { input = ""; exprDisplay.repaint(); } });
        ekeys.add(eclr);
        exprSubmit = new Btn("제출", Btn.Style.PRIMARY).font(15);
        exprSubmit.addActionListener(e -> submit());
        ekeys.add(exprSubmit);
        ekeys.setPreferredSize(new Dimension(470, 200));
        exprCard.add(ekeys, BorderLayout.EAST);
        answerArea.add(exprCard, "expr");

        // ---- 객관식 카드
        JPanel chCard = Ui.clear(new BorderLayout(20, 10));
        choiceLabel = Ui.label("", Theme.serif(14), Theme.INK_SOFT);
        chCard.add(choiceLabel, BorderLayout.NORTH);
        choiceGrid = Ui.clear(new GridLayout(2, 2, 12, 10));
        chCard.add(choiceGrid, BorderLayout.CENTER);
        choiceSubmit = new Btn("제출", Btn.Style.PRIMARY).font(16);
        choiceSubmit.setPreferredSize(new Dimension(120, 60));
        choiceSubmit.addActionListener(e -> submit());
        chCard.add(Ui.center(choiceSubmit), BorderLayout.EAST);
        answerArea.add(chCard, "choice");

        // ---- 참/거짓 + 이유 카드
        JPanel tfCard = Ui.clear(new BorderLayout(20, 10));
        JPanel tfTop = Ui.clear(new BorderLayout(14, 0));
        tfLabel = Ui.label("", Theme.serif(14), Theme.INK_SOFT);
        tfTop.add(tfLabel, BorderLayout.CENTER);
        JPanel tfv = Ui.clear(new GridLayout(1, 2, 10, 0));
        tfTrue = new Btn("참 (O)", Btn.Style.CHOICE).font(16);
        tfFalse = new Btn("거짓 (X)", Btn.Style.CHOICE).font(16);
        tfTrue.addActionListener(e -> selectVerdict("참"));
        tfFalse.addActionListener(e -> selectVerdict("거짓"));
        tfv.add(tfTrue);
        tfv.add(tfFalse);
        tfv.setPreferredSize(new Dimension(260, 52));
        tfTop.add(tfv, BorderLayout.EAST);
        tfCard.add(tfTop, BorderLayout.NORTH);
        tfReasons = Ui.clear(new GridLayout(2, 2, 12, 10));
        tfCard.add(tfReasons, BorderLayout.CENTER);
        tfSubmit = new Btn("제출", Btn.Style.PRIMARY).font(16);
        tfSubmit.setPreferredSize(new Dimension(120, 60));
        tfSubmit.addActionListener(e -> submit());
        tfCard.add(Ui.center(tfSubmit), BorderLayout.EAST);
        answerArea.add(tfCard, "tf");

        // ---- 추리 순서 카드
        JPanel orCard = Ui.clear(new BorderLayout(16, 10));
        JPanel orTop = Ui.clear(new BorderLayout());
        orderLabel = Ui.label("", Theme.serif(14), Theme.INK_SOFT);
        orTop.add(orderLabel, BorderLayout.WEST);
        orCard.add(orTop, BorderLayout.NORTH);
        JPanel cols = Ui.clear(new GridLayout(1, 2, 16, 0));
        JPanel poolCol = Ui.clear(new BorderLayout(0, 6));
        poolCol.add(Ui.label("단서 카드 — 누르면 순서에 놓여요", Theme.bold(13), Theme.INK_SOFT), BorderLayout.NORTH);
        orderPool = Ui.clear(new GridLayout(0, 1, 0, 6));
        poolCol.add(orderPool, BorderLayout.CENTER);
        JPanel placedCol = Ui.clear(new BorderLayout(0, 6));
        placedCol.add(Ui.label("추리 순서 — 누르면 되돌려요", Theme.bold(13), Theme.INK_SOFT), BorderLayout.NORTH);
        orderPlaced = Ui.clear(new GridLayout(0, 1, 0, 6));
        placedCol.add(orderPlaced, BorderLayout.CENTER);
        cols.add(poolCol);
        cols.add(placedCol);
        orCard.add(cols, BorderLayout.CENTER);
        JPanel orBtns = Ui.clear(new GridLayout(2, 1, 0, 10));
        orderReset = new Btn("다시 놓기", Btn.Style.OUTLINE).font(14);
        orderReset.addActionListener(e -> resetOrder());
        orderSubmit = new Btn("제출", Btn.Style.PRIMARY).font(16);
        orderSubmit.addActionListener(e -> submit());
        orBtns.add(orderReset);
        orBtns.add(orderSubmit);
        orBtns.setPreferredSize(new Dimension(120, 120));
        orCard.add(Ui.center(orBtns), BorderLayout.EAST);
        answerArea.add(orCard, "order");

        answerPanel.setPreferredSize(new Dimension(700, 214));
        right.add(answerPanel, BorderLayout.SOUTH);

        c.gridx = 1; c.weightx = 1; c.insets = new Insets(0, 0, 0, 0);
        body.add(right, c);
        add(body, BorderLayout.CENTER);

        // 키보드 입력
        for (int d = 0; d <= 9; d++) {
            final String k = "" + d;
            bind("typed " + d, () -> digitKey(k));
        }
        for (String ch : new String[]{"x", "+", "-", "/", "*", "(", ")", ".", "^", "p", "r"}) {
            bind("typed " + ch, () -> charKey(ch));
        }
        bind("typed o", () -> { if (puzzle != null && puzzle.isTf()) selectVerdict("참"); });
        bind("BACK_SPACE", this::backspace);
        bind("ENTER", this::submit);
        // 글자 대신 키 위치로 묶어서 한글 입력 상태에서도 동작한다
        bind("pressed H", () -> {
            if (puzzle == null || state != State.INPUT) return;
            int opened = session.hintsOpened(puzzle);
            if (opened < puzzle.hints.size()) openHint(opened);
        });
        bind("pressed A", () -> { if (puzzle != null) toggleAux(); });
        bind("pressed R", () -> { if (puzzle != null && state == State.SOLVED) toggleReplay(); });
        bind("pressed E", () -> { if (puzzle != null && state == State.SOLVED && exploreSpec() != null) openExplore(); });
        bind("SPACE", () -> { if (puzzle != null && state == State.SOLVED) next(); });
        bind("ESCAPE", this::confirmQuit);
    }

    private static JPanel fillWidth(JComponent c) {
        JPanel wrap = Ui.clear(new GridBagLayout());
        GridBagConstraints dc = new GridBagConstraints();
        dc.weightx = 1; dc.fill = GridBagConstraints.HORIZONTAL;
        wrap.add(c, dc);
        return wrap;
    }

    /** 낙서 펜 단추 순서(마지막 null 은 '모두 지우기'). */
    private static final DiagramView.Tool[] PEN_TOOLS = {DiagramView.Tool.CHALK, DiagramView.Tool.GOLD, DiagramView.Tool.RED, DiagramView.Tool.ERASER, null};

    /** null 이면 낙서를 모두 지우고, 같은 펜을 다시 누르면 내려놓는다. */
    private void togglePen(DiagramView.Tool t) {
        if (t == null) { diagram.clearScribbles(); return; }
        setTool(diagram.tool() == t ? DiagramView.Tool.NONE : t);
    }

    private void setTool(DiagramView.Tool t) {
        diagram.setTool(t);
        for (int i = 0; i < 4; i++) penBtns[i].setSelected(diagram.tool() == PEN_TOOLS[i]);
    }

    /** 칠판 위 작은 펜 단추(색 점 또는 아이콘). */
    private static class PenButton extends Btn {
        private final Color dot;
        private final String kind;

        PenButton(Color dot, String kind) {
            super("", Style.CIRCLE_DARK);
            this.dot = dot;
            this.kind = kind;
            setPreferredSize(new Dimension(34, 34));
        }

        @Override public Dimension getPreferredSize() { return new Dimension(34, 34); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            boolean hover = getModel().isRollover();
            g.setColor(isSelected() ? new Color(255, 255, 255, 60) : hover ? new Color(255, 255, 255, 25) : new Color(0, 0, 0, 0));
            g.fillOval(1, 1, w - 2, h - 2);
            g.setColor(isSelected() ? Theme.GOLD : Theme.alpha(Theme.GOLD, 0.6f));
            g.setStroke(new BasicStroke(isSelected() ? 2.4f : 1.4f));
            g.drawOval(1, 1, w - 3, h - 3);
            if (dot != null) {
                g.setColor(dot);
                g.fillOval(w / 2 - 7, h / 2 - 7, 14, 14);
            } else if ("eraser".equals(kind)) {
                g.setColor(new Color(0xF2B8A8));
                g.rotate(Math.toRadians(-35), w / 2.0, h / 2.0);
                g.fillRoundRect(w / 2 - 9, h / 2 - 5, 18, 10, 4, 4);
                g.setColor(new Color(0xF7F1E3));
                g.fillRoundRect(w / 2 - 9, h / 2 - 5, 7, 10, 4, 4);
            } else {
                g.setColor(Theme.CREAM);
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(w / 2 - 6, h / 2 - 5, w / 2 + 6, h / 2 - 5);
                g.drawRect(w / 2 - 4, h / 2 - 3, 8, 10);
                g.drawLine(w / 2 - 2, h / 2 - 8, w / 2 + 2, h / 2 - 8);
            }
            g.dispose();
        }
    }

    private Explore exploreSpec() {
        Lesson l = puzzle == null ? null : game.content.lessonById.get(puzzle.lessonId);
        return l == null ? null : game.content.explores.get(l.theoremId);
    }

    private void toggleReplay() {
        if (diagram.replaying()) { diagram.stopSteps(); return; }
        setTool(DiagramView.Tool.NONE);
        replayBtn.setText("풀이 멈추기");
        Sfx.play("hint");
        diagram.playSteps(Game.solutionSteps(puzzle, diagram.hasAux()), boardAnswer(), () -> {
            replayBtn.setText("풀이 다시 보기");
            auxBtn.setText(diagram.isAuxShown() ? "보조선 지우기" : "보조선 긋기");
        });
    }

    private void openExplore() {
        Lesson l = puzzle == null ? null : game.content.lessonById.get(puzzle.lessonId);
        if (l != null) ExploreBoard.open(frame, game.content.theorems.get(l.theoremId));
    }

    private void bind(String key, Runnable r) {
        KeyStroke ks = KeyStroke.getKeyStroke(key);
        if (ks == null) return;
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ks, key);
        getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (isShowing()) r.run(); }
        });
    }

    private void digitKey(String k) {
        if (puzzle == null) return;
        int idx = Integer.parseInt(k) - 1;
        if (puzzle.isChoice()) selectChoice(idx);
        else if (puzzle.isTf()) selectReason(idx);
        else if (puzzle.isOrder()) { if (idx >= 0 && idx < pool.size()) place(pool.get(idx)); }
        else type(k);
    }

    private void charKey(String ch) {
        if (puzzle == null) return;
        if (puzzle.isTf() && ch.equals("x")) { selectVerdict("거짓"); return; }
        if (!puzzle.isExpr()) return;
        type(ch.equals("*") ? "×" : ch.equals("p") ? "π" : ch.equals("r") ? "√" : ch);
    }

    // ================================================================== 흐름
    public void start(Session s) {
        session = s;
        String reward;
        if (s.lesson != null) {
            reward = "보상 " + (game.isDone(s.lesson) ? s.lesson.xp / 2 : s.lesson.xp) + " XP";
        } else {
            reward = s.mode == Session.Mode.REVIEW ? "오답 노트" : "연습 모드";
        }
        rewardPill.set(null, reward);
        showPuzzle();
    }

    private void showPuzzle() {
        puzzle = session.current();
        session.shownMs = System.currentTimeMillis();
        state = State.INPUT;
        input = "";
        choice = null;
        verdict = null;
        topSmall.setText("수수께끼 No. " + String.format("%03d", puzzle.number) + "  ·  " + puzzle.typeName()
                + (session.lesson != null ? "" : session.mode == Session.Mode.REVIEW ? "  ·  오답 노트" : "  ·  연습"));
        topTitle.setText(puzzle.title);
        noteTitle.setText(puzzle.noteTitle);
        noteText.setText(puzzle.note);
        bubble.setText(puzzle.hazel);
        diagram.setScript(game.content.diagrams.get(puzzle.diagram));
        setTool(DiagramView.Tool.NONE);
        exploreBtn.setVisible(false);
        replayBtn.setVisible(false);
        replayBtn.setText("풀이 다시 보기");
        auxBtn.setEnabled(diagram.hasAux());
        auxBtn.setText("보조선 긋기");
        auxBtn.setToolTipText(diagram.hasAux() ? "보조선을 그어 단서를 찾아보세요" : "이 수수께끼에는 보조선이 없습니다");

        int height = 214;
        if (puzzle.isChoice()) {
            choiceLabel.setText(puzzle.answerLabel + " — 하나를 고르세요 (키보드 1~4)");
            choiceGrid.removeAll();
            choiceBtns.clear();
            for (int i = 0; i < puzzle.choices.size(); i++) {
                final int idx = i;
                Btn b = new Btn((i + 1) + ".  " + puzzle.choices.get(i), Btn.Style.CHOICE).font(15);
                b.addActionListener(e -> selectChoice(idx));
                choiceBtns.add(b);
                choiceGrid.add(b);
            }
            resetSubmit(choiceSubmit);
            answerCards.show(answerArea, "choice");
        } else if (puzzle.isTf()) {
            tfLabel.setText(label(puzzle.answerLabel, "이 주장은 참일까 거짓일까? 판단과 그 이유를 고르세요"));
            tfTrue.setSelected(false);
            tfFalse.setSelected(false);
            tfTrue.fill(null);
            tfFalse.fill(null);
            tfReasons.removeAll();
            reasonBtns.clear();
            for (int i = 0; i < puzzle.choices.size(); i++) {
                final int idx = i;
                Btn b = new Btn((i + 1) + ".  " + puzzle.choices.get(i), Btn.Style.CHOICE).font(14);
                b.addActionListener(e -> selectReason(idx));
                b.setToolTipText(puzzle.choices.get(i));
                reasonBtns.add(b);
                tfReasons.add(b);
            }
            resetSubmit(tfSubmit);
            answerCards.show(answerArea, "tf");
            height = 250;
        } else if (puzzle.isOrder()) {
            orderLabel.setText(label(puzzle.answerLabel, "추리의 순서를 바로잡으세요") + "  (키보드 1~" + puzzle.choices.size() + ")");
            resetOrder();
            resetSubmit(orderSubmit);
            answerCards.show(answerArea, "order");
            height = Math.max(240, 86 + puzzle.choices.size() * 48);
        } else if (puzzle.isExpr()) {
            exprLabel.setText(label(puzzle.answerLabel, "식으로 답하세요"));
            resetSubmit(exprSubmit);
            answerCards.show(answerArea, "expr");
            height = 250;
        } else {
            answerLabel.setText(puzzle.answerLabel);
            resetSubmit(submitKey);
            answerCards.show(answerArea, "num");
        }
        answerPanel.setPreferredSize(new Dimension(700, height));
        display.reset();
        exprDisplay.reset();
        rebuildHints();
        seg.repaint();
        refreshPills();
        revalidate();
        repaint();
    }

    private static String label(String s, String def) { return s == null || s.trim().isEmpty() ? def : s; }

    private static void resetSubmit(Btn b) {
        b.setText("제출");
        b.setStyle(Btn.Style.PRIMARY);
    }

    private void refreshPills() {
        lanternPill.set("lantern", "등불 " + game.lanterns());
        tokenPill.set("token", "토큰 " + game.save.tokens);
    }

    private void type(String d) {
        if (state != State.INPUT || puzzle == null) return;
        if (puzzle.isExpr()) {
            if (input.length() >= 18) return;
            input += d;
            exprDisplay.repaint();
            return;
        }
        if (!puzzle.isNumber() || !Character.isDigit(d.charAt(0))) return;
        if (input.length() >= 4) return;
        if (input.equals("0")) input = "";
        input += d;
        display.repaint();
    }

    private void backspace() {
        if (state != State.INPUT || puzzle == null) return;
        if (puzzle.isOrder()) {
            if (!placed.isEmpty()) unplace(placed.get(placed.size() - 1));
            return;
        }
        if (input.isEmpty()) return;
        input = input.substring(0, input.length() - 1);
        display.repaint();
        exprDisplay.repaint();
    }

    private void selectChoice(int i) {
        if (state != State.INPUT || i < 0 || i >= choiceBtns.size() || !choiceBtns.get(i).isEnabled()) return;
        choice = puzzle.choices.get(i);
        Sfx.play("select");
        for (int k = 0; k < choiceBtns.size(); k++) choiceBtns.get(k).setSelected(k == i);
    }

    private void selectVerdict(String v) {
        if (state != State.INPUT || puzzle == null || !puzzle.isTf()) return;
        verdict = v;
        Sfx.play("select");
        tfTrue.setSelected("참".equals(v));
        tfFalse.setSelected("거짓".equals(v));
    }

    private void selectReason(int i) {
        if (state != State.INPUT || i < 0 || i >= reasonBtns.size()) return;
        choice = puzzle.choices.get(i);
        Sfx.play("select");
        for (int k = 0; k < reasonBtns.size(); k++) reasonBtns.get(k).setSelected(k == i);
    }

    // ---- 추리 순서
    private void resetOrder() {
        if (state != State.INPUT && puzzle != null && puzzle.isOrder()) return;
        pool.clear();
        placed.clear();
        if (puzzle == null || !puzzle.isOrder()) return;
        pool.addAll(puzzle.choices);
        // 정답 순서와 같지 않게 섞는다
        Random r = new Random(puzzle.id.hashCode() * 31L + System.nanoTime());
        for (int tries = 0; tries < 8; tries++) {
            Collections.shuffle(pool, r);
            if (!pool.equals(puzzle.choices) || pool.size() < 2) break;
        }
        rebuildOrder();
    }

    private void place(String step) {
        if (state != State.INPUT || !pool.remove(step)) return;
        placed.add(step);
        Sfx.play("select");
        rebuildOrder();
    }

    private void unplace(String step) {
        if (state != State.INPUT || !placed.remove(step)) return;
        pool.add(step);
        Sfx.play("click");
        rebuildOrder();
    }

    private void rebuildOrder() {
        orderPool.removeAll();
        orderPlaced.removeAll();
        int n = puzzle == null ? 0 : puzzle.choices.size();
        for (int i = 0; i < pool.size(); i++) {
            String s = pool.get(i);
            Btn b = stepButton((i + 1) + ".  " + s, Btn.Style.CHOICE);
            b.addActionListener(e -> place(s));
            orderPool.add(b);
        }
        for (int i = pool.size(); i < n; i++) orderPool.add(Box.createGlue());
        for (int i = 0; i < n; i++) {
            if (i < placed.size()) {
                String s = placed.get(i);
                Btn b = stepButton("①②③④⑤⑥⑦⑧".charAt(Math.min(i, 7)) + "  " + s, Btn.Style.CREAM);
                if (state == State.SOLVED) b.fill(Theme.c(0xD9EBDD));
                b.addActionListener(e -> unplace(s));
                orderPlaced.add(b);
            } else {
                orderPlaced.add(new Slot(i + 1));
            }
        }
        orderPool.revalidate();
        orderPlaced.revalidate();
        repaint();
    }

    private static Btn stepButton(String text, Btn.Style st) {
        Btn b = new Btn(text, st).font(13.5f).align(SwingConstants.LEFT);
        b.setToolTipText(text);
        return b;
    }

    /** 아직 비어 있는 순서 칸. */
    private static class Slot extends JComponent {
        final int n;
        Slot(int n) { this.n = n; }
        @Override public Dimension getPreferredSize() { return new Dimension(100, 40); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            g.setColor(Theme.BORDER);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{5, 4}, 0));
            g.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 3, getHeight() - 6, 12, 12));
            g.setFont(Theme.serif(13));
            g.setColor(Theme.MUTED);
            g.drawString(n + "번째 단계", 16, (getHeight() - 4) / 2 + 5);
            g.dispose();
        }
    }

    /** 현재 입력을 채점용 문자열로. 아직 덜 입력했으면 안내 문구와 함께 null. */
    private String collectAnswer() {
        if (puzzle.isChoice()) {
            if (choice == null) { bubble.setText("먼저 답을 하나 골라 줘."); return null; }
            return choice;
        }
        if (puzzle.isTf()) {
            if (verdict == null) { bubble.setText("먼저 참인지 거짓인지 판단해 줘. (O / X 키)"); return null; }
            if (choice == null) { bubble.setText("판단의 근거가 될 이유도 하나 골라 줘."); return null; }
            return verdict + "|" + choice;
        }
        if (puzzle.isOrder()) {
            if (!pool.isEmpty()) { bubble.setText("단서 카드를 모두 순서대로 놓아 줘. 아직 " + pool.size() + "장 남았어."); return null; }
            return String.join("|", placed);
        }
        if (input.isEmpty()) {
            bubble.setText(puzzle.isExpr() ? "식을 먼저 입력해 줘." : "비밀번호를 먼저 입력해 줘.");
            return null;
        }
        if (puzzle.isExpr() && !Expr.isValid(input)) {
            bubble.setText("식이 덜 끝난 것 같아. 괄호나 기호를 확인해 줘. (등불은 꺼지지 않았어)");
            return null;
        }
        return input;
    }

    private Btn currentSubmit() {
        if (puzzle.isChoice()) return choiceSubmit;
        if (puzzle.isTf()) return tfSubmit;
        if (puzzle.isOrder()) return orderSubmit;
        if (puzzle.isExpr()) return exprSubmit;
        return submitKey;
    }

    private Display currentDisplay() { return puzzle.isExpr() ? exprDisplay : display; }

    private void submit() {
        if (puzzle == null) return;
        if (state == State.SOLVED) { next(); return; }
        String ans = collectAnswer();
        if (ans == null) {
            currentDisplay().shake();
            return;
        }
        if (Game.checkAnswer(puzzle, ans)) onRight();
        else onWrongAnswer(ans);
    }

    private void onRight() {
        state = State.SOLVED;
        Sfx.play("correct");
        hazel.mood(Widgets.FaceView.Mood.HAPPY, 1900);
        diagram.revealAnswer(boardAnswer());
        diagram.chalkAnswer(bigAnswer(), theoremName());
        exploreBtn.setVisible(exploreSpec() != null);
        replayBtn.setVisible(true);
        List<String> msgs = game.onCorrect(session, puzzle);
        bubble.setText(RIGHT_LINES[rnd.nextInt(RIGHT_LINES.length)] + " " + puzzle.explanation);
        currentDisplay().correct();
        Btn sb = currentSubmit();
        sb.setText(session.isLast() ? (hasFinaleNext() ? "최종 추리로" : "사건 해결!") : "다음");
        sb.setStyle(Btn.Style.GREEN);
        Color ok = Theme.c(0xD9EBDD);
        if (puzzle.isChoice()) {
            for (int k = 0; k < choiceBtns.size(); k++) {
                if (puzzle.choices.get(k).equals(puzzle.answer)) choiceBtns.get(k).fill(ok);
                choiceBtns.get(k).repaint();
            }
        } else if (puzzle.isTf()) {
            ("참".equals(puzzle.tfVerdict()) ? tfTrue : tfFalse).fill(ok);
            for (int k = 0; k < reasonBtns.size(); k++) {
                if (puzzle.choices.get(k).equals(puzzle.tfReason())) reasonBtns.get(k).fill(ok);
            }
            repaint();
        } else if (puzzle.isOrder()) {
            rebuildOrder();
        }
        seg.repaint();
        frame.toasts(msgs);
    }

    /** 정답 순간 칠판에 크게 쓸 글자: 숫자·식은 답(단위 포함), 나머지는 '정답!'. */
    private String bigAnswer() {
        if (puzzle.isNumber() || puzzle.isExpr()) {
            return withUnit(puzzle.answer);
        }
        return "정답!";
    }

    /** 값 뒤에 단위를 붙인다(단위가 길면 붙이지 않는다). */
    private String withUnit(String v) {
        String u = puzzle.unit == null ? "" : puzzle.unit.trim();
        return v + (u.length() <= 3 ? u : "");
    }

    /** 이 문제의 정리 이름(도장). */
    private String theoremName() {
        Lesson l = game.content.lessonById.get(puzzle.lessonId);
        Theorem t = l == null ? null : game.content.theorems.get(l.theoremId);
        return t == null ? null : t.name;
    }

    /** 오답 때 칠판에 적었다가 지울 글자. */
    private String wrongText(String ans) {
        if (puzzle.isNumber() || puzzle.isExpr()) {
            return withUnit(ans);
        }
        if (puzzle.isTf()) {
            int i = ans.indexOf('|');
            return i < 0 ? ans : ans.substring(0, i);
        }
        if (puzzle.isOrder()) return "순서가 달라!";
        return ans.length() > 9 ? ans.substring(0, 8) + "…" : ans;
    }

    /** 칠판의 '?' 자리에 쓸 답(숫자·식 문제만). */
    private String boardAnswer() {
        if (!(puzzle.isNumber() || puzzle.isExpr())) return null;
        String u = puzzle.unit == null ? "" : puzzle.unit.trim();
        String v = puzzle.answer;
        return v + (u.equals("°") || u.equals("m") || u.equals("cm") ? u : "");
    }

    private void onWrongAnswer(String ans) {
        Sfx.play("wrong");
        hazel.mood(Widgets.FaceView.Mood.PUZZLED, 1900);
        lanternPill.flicker();
        Game.Diagnosis d = game.onWrong(session, puzzle, ans);
        diagram.chalkWrong(wrongText(ans));
        if (d != null && d.text != null && !d.text.isEmpty()) {
            Misconception m = d.tag == null ? null : game.content.misconceptions.get(d.tag);
            bubble.setText((m != null ? "〔" + m.name + "〕 " : "") + d.text);
        } else {
            bubble.setText(WRONG_LINES[rnd.nextInt(WRONG_LINES.length)]);
        }
        currentDisplay().wrong();
        refreshPills();
        if (puzzle.isChoice()) {
            for (int k = 0; k < choiceBtns.size(); k++) {
                if (puzzle.choices.get(k).equals(choice)) {
                    choiceBtns.get(k).setEnabled(false);
                    choiceBtns.get(k).setSelected(false);
                }
            }
            choice = null;
        } else if (puzzle.isTf()) {
            choice = null;
            for (Btn b : reasonBtns) b.setSelected(false);
        }
        if (puzzle.isNumber() || puzzle.isExpr()) {
            javax.swing.Timer t = new javax.swing.Timer(650, e -> {
                if (state == State.INPUT) { input = ""; display.repaint(); exprDisplay.repaint(); }
            });
            t.setRepeats(false);
            t.start();
        }
        if (game.lanterns() <= 0) SwingUtilities.invokeLater(this::outOfLanterns);
    }

    private boolean hasFinaleNext() {
        return session.mode == Session.Mode.LESSON && session.lesson != null && game.content.hasFinale(session.lesson);
    }

    private void next() {
        if (session.isLast()) {
            if (hasFinaleNext() && !session.finaleSolved) {
                frame.showFinale(session);
                return;
            }
            Result r = game.finish(session);
            frame.showResult(r);
            frame.toasts(r.messages);
        } else {
            session.index++;
            showPuzzle();
        }
    }

    private void outOfLanterns() {
        Ui.Modal m = Ui.modal(frame, "등불이 모두 꺼졌다!", 500);
        m.text("어둠 속에서는 더 이상 추리를 이어 갈 수 없어요.\n단서 토큰 " + Game.REFILL_COST
                + "개로 등불을 다시 켜거나, 사건을 잠시 접어 두었다가 등불이 다시 켜지면(30분마다 1개) 도전할 수 있습니다.");
        m.button("사건 접어 두기", Btn.Style.OUTLINE, () -> frame.showTab("map"));
        if (game.save.tokens >= Game.REFILL_COST) {
            m.button("토큰 " + Game.REFILL_COST + "개로 등불 켜기", Btn.Style.PRIMARY, () -> {
                game.refillLanterns();
                refreshPills();
                bubble.setText("휴, 다시 밝아졌어! 이번엔 천천히 생각해 보자.");
            });
        }
        m.show();
        if (game.lanterns() <= 0 && isShowing()) frame.showTab("map");
    }

    private void confirmQuit() {
        if (!isShowing()) return;
        Ui.Modal m = Ui.modal(frame, "사건을 그만둘까요?", 460);
        m.text("지금 그만두면 이번 풀이의 경험치와 기록은 저장되지 않습니다. (꺼진 등불은 돌아오지 않아요)");
        m.button("계속 풀기", Btn.Style.OUTLINE, null);
        m.button("그만두기", Btn.Style.PRIMARY, () -> frame.showTab("map"));
        m.show();
    }

    private void toggleAux() {
        if (!diagram.hasAux()) return;
        boolean on = !diagram.isAuxShown();
        diagram.setAux(on);
        auxBtn.setText(on ? "보조선 지우기" : "보조선 긋기");
        if (on) {
            Sfx.play("aux");
            frame.toasts(game.useAux(session, puzzle));
        }
    }

    // ================================================================== 힌트
    private void rebuildHints() {
        hintList.removeAll();
        int opened = session.hintsOpened(puzzle);
        for (int i = 0; i < puzzle.hints.size(); i++) {
            hintList.add(new HintRow(i, i < opened, i == opened));
            hintList.add(Ui.vgap(10));
        }
        hintList.revalidate();
        hintList.repaint();
    }

    private void openHint(int i) {
        int opened = session.hintsOpened(puzzle);
        if (i != opened) return;
        if (!game.useHint(session, puzzle)) {
            bubble.setText("단서 토큰이 부족해... 오늘의 의뢰를 완료하면 토큰을 받을 수 있어.");
            return;
        }
        Sfx.play("hint");
        refreshPills();
        rebuildHints();
    }

    private class HintRow extends RoundPanel {
        HintRow(int i, boolean open, boolean next) {
            super(new BorderLayout(0, 8), open ? Theme.c(0xFBF3E1) : Theme.PAPER, open ? Theme.GOLD_DARK : Theme.BORDER, 10);
            borderWidth(1.8f);
            pad(12, 16, 12, 14);
            setAlignmentX(LEFT_ALIGNMENT);
            JPanel head = Ui.clear(new BorderLayout());
            JLabel title = Ui.label("  힌트 " + (i + 1) + (open ? "" : "  열기"), Theme.bold(15), next || open ? Theme.INK : Theme.MUTED);
            title.setIcon(Icons.icon(open ? "bulb" : "lock", 18, next || open ? Theme.INK : Theme.MUTED));
            head.add(title, BorderLayout.WEST);
            if (!open) head.add(Ui.label("단서 토큰 " + Game.HINT_COST + "개", Theme.serif(12.5f), Theme.INK_SOFT), BorderLayout.EAST);
            add(head, BorderLayout.NORTH);
            if (open) {
                add(new Widgets.TextBlock(puzzle.hints.get(i), Theme.serif(14.5f), Theme.INK).spacing(1.55f).prefWidth(300),
                        BorderLayout.CENTER);
            } else {
                setCursor(Cursor.getPredefinedCursor(next ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                if (!next) setToolTipText("앞의 힌트를 먼저 열어야 합니다");
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { if (next) openHint(i); }
                    @Override public void mouseEntered(MouseEvent e) { if (next) setFill(Theme.c(0xFBF3E1)); }
                    @Override public void mouseExited(MouseEvent e) { setFill(Theme.PAPER); }
                });
            }
        }

        @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    }

    // ================================================================== 구성 요소
    private class SegBar extends JComponent {
        SegBar() { setPreferredSize(new Dimension(400, 14)); }

        @Override
        protected void paintComponent(Graphics g0) {
            if (session == null) return;
            Graphics2D g = Theme.aa(g0.create());
            int n = session.puzzles.size(), gap = 7;
            double w = (getWidth() - gap * (n - 1)) / (double) n;
            for (int i = 0; i < n; i++) {
                boolean done = i < session.index || (i == session.index && state == State.SOLVED);
                Color c = done ? Theme.GOLD : i == session.index ? new Color(0xF6EAD0) : new Color(0x5A4230);
                g.setColor(c);
                g.fill(new RoundRectangle2D.Double(i * (w + gap), 1, w, 12, 12, 12));
            }
            g.dispose();
        }
    }

    /** 비밀번호·식을 보여 주는 어두운 표시창. */
    private class Display extends JComponent {
        private Color tint = null;
        private String status = null;
        private long shakeStart = 0;
        private final javax.swing.Timer t = new javax.swing.Timer(16, e -> {
            repaint();
            if (System.currentTimeMillis() - shakeStart > 450) ((javax.swing.Timer) e.getSource()).stop();
        });

        Display() { setPreferredSize(new Dimension(320, 76)); }

        void reset() { tint = null; status = null; repaint(); }
        void correct() { tint = new Color(0x2F6B55); status = "정답!"; repaint(); }
        void wrong() {
            tint = new Color(0x8E2E20);
            status = "틀렸습니다";
            shake();
            javax.swing.Timer back = new javax.swing.Timer(900, e -> { if (state == State.INPUT) { tint = null; status = null; repaint(); } });
            back.setRepeats(false);
            back.start();
        }
        void shake() { shakeStart = System.currentTimeMillis(); t.start(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            long el = System.currentTimeMillis() - shakeStart;
            double dx = el < 450 ? Math.sin(el / 22.0) * 8 * (1 - el / 450.0) : 0;
            g.translate(dx, 0);
            int w = getWidth(), h = getHeight();
            g.setColor(tint != null ? tint : new Color(0x3A2819));
            g.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 14, 14));
            String unit = puzzle == null || puzzle.unit == null ? "" : puzzle.unit.trim();
            boolean empty = input.isEmpty();
            boolean expr = puzzle != null && puzzle.isExpr();
            String main = state == State.SOLVED && puzzle != null && (puzzle.isNumber() || expr) ? (expr && !input.isEmpty() ? input : puzzle.answer)
                    : empty ? "?" : input;
            float size = 42;
            g.setFont(Theme.xbold(size));
            int statusW = status == null ? 0 : getFontMetrics(Theme.bold(17)).stringWidth(status) + 30;
            while (size > 18 && Theme.width(g.getFontMetrics(), main + unit) > w - 60 - statusW) {
                size -= 2;
                g.setFont(Theme.xbold(size));
            }
            g.setColor(empty && state != State.SOLVED ? Theme.GOLD : Theme.CREAM);
            FontMetrics fm = g.getFontMetrics();
            int by = (h + fm.getAscent() - fm.getDescent()) / 2 + 2;
            Theme.drawString(g, main, 24, by);
            int x = 24 + Theme.width(fm, main) + 4;
            boolean deg = unit.equals("°");
            g.setFont(Theme.bold(deg ? size * 0.7f : Math.min(20, size * 0.5f)));
            Theme.drawString(g, unit, x, deg ? by - Math.round(size / 3) : by);
            if (status != null) {
                g.setFont(Theme.bold(17));
                g.setColor(Theme.CREAM);
                g.drawString(status, w - 24 - g.getFontMetrics().stringWidth(status), by - 4);
            }
            g.dispose();
        }
    }
}
