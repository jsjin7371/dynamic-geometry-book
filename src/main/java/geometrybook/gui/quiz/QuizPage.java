package geometrybook.gui.quiz;

import geometrybook.application.quiz.QuizSource;
import geometrybook.domain.Level;
import geometrybook.domain.quiz.QuizAttempt;
import geometrybook.domain.quiz.QuizQuestion;
import geometrybook.gui.common.AppShell;
import geometrybook.gui.common.RoundedPanel;
import geometrybook.gui.common.RoundedProgressBar;
import geometrybook.gui.common.Theme;
import geometrybook.gui.common.UiComponents;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;

/** 단원 선택, 한 문제씩 채점, 결과와 오답 재도전을 제공한다. */
public final class QuizPage extends JPanel {
    private final QuizSource source;
    private final BiConsumer<String, Boolean> onGraded;
    private final Supplier<List<QuizQuestion>> wrongNote;
    private final JPanel content = new ViewportWidthPanel();
    private QuizAttempt attempt;
    private SwingWorker<List<QuizQuestion>, Void> loader;
    private boolean active = true;
    private boolean retry;
    private String levelTitle;
    private String selectedChoiceId;

    /**
     * onGraded: 한 문제를 채점할 때마다 (문제 ID, 정답 여부)를 받는다. 오답 기록 저장에 쓴다.
     * wrongNote: 현재 학생의 오답 문제를 틀린 순서대로 돌려준다. 오답노트에 쓴다.
     */
    public QuizPage(String name, QuizSource source, BiConsumer<String, Boolean> onGraded, Supplier<List<QuizQuestion>> wrongNote,
                     Runnable onHome, Runnable onLevel, Runnable onLogout) {
        super(new BorderLayout());
        this.source = source;
        this.onGraded = onGraded;
        this.wrongNote = wrongNote;
        AppShell shell = new AppShell(AppShell.Page.QUIZ, name, () -> leave(onHome),
                () -> leave(onLevel), () -> {}, () -> leave(onLogout));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(30, 32, 32, 32));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BACKGROUND);   // 내용이 짧을 때 아래 빈 곳도 같은 배경
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        shell.setContent(scroll);
        add(shell);
        showLevels();
    }

    /** 시도를 버리기 전에 확인하고 비동기 조회를 정리한다. */
    public void leave(Runnable action) {
        if (attempt != null && !attempt.isFinished()
                && JOptionPane.showConfirmDialog(this, "나가면 현재 퀴즈 답안이 사라져요. 나갈까요?",
                "퀴즈 종료", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        active = false;
        if (loader != null) loader.cancel(true);
        action.run();
    }

    private void heading(String title, String description) {
        content.removeAll();
        content.add(UiComponents.label(title, 28, Font.BOLD, Theme.TEXT));
        gap(14);
        content.add(UiComponents.label(description, 16, Font.PLAIN, Theme.MUTED));
        gap(24);
    }

    private void showLevels() {
        attempt = null;
        retry = false;
        heading("도형 퀴즈에 도전해요", "학습 완료 여부와 관계없이 원하는 단원을 선택해요.");
        RoundedPanel banner = new RoundedPanel(new BorderLayout(0, 8), Theme.MINT, 24);
        banner.setAlignmentX(LEFT_ALIGNMENT);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        banner.add(UiComponents.label("배운 개념을 한 번 더 생각해 봐요", 19, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        banner.add(UiComponents.label("한 문제씩 도전하고, 틀린 문제는 다시 풀어봐요.", 14, Font.PLAIN, Theme.MUTED), BorderLayout.CENTER);
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        content.add(banner);
        gap(16);
        JButton note = UiComponents.roundedPrimaryButton("오답노트 보기 →");
        note.addActionListener(event -> load(wrongNote, this::showWrongNote));
        content.add(note);
        gap(16);

        RoundedPanel list = new RoundedPanel(null, Color.WHITE, 28);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setAlignmentX(LEFT_ALIGNMENT);
        list.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        list.add(UiComponents.label("단원별 퀴즈 선택", 21, Font.BOLD, Theme.TEXT));
        list.add(Box.createVerticalStrut(20));
        for (Level level : Level.values()) {
            if (level.number() > 1) list.add(Box.createVerticalStrut(14));
            list.add(levelCard(level.number(), level.title(), () -> {
                levelTitle = level.title();
                load(() -> source.findByLevelId(level.name()), this::startLevel);
            }));
        }
        content.add(list);
        refresh();
    }

    /** 번호·단원명·선택 버튼을 같은 규격의 카드로 배치한다. */
    private JPanel levelCard(int number, String title, Runnable onSelect) {
        Color background = number == 1 ? Theme.MINT : Theme.BACKGROUND;
        RoundedPanel row = new RoundedPanel(new BorderLayout(18, 0), background, 24);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        row.setPreferredSize(new Dimension(0, 88));
        row.setMinimumSize(new Dimension(0, 88));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));

        JPanel identity = new JPanel(new BorderLayout(16, 0));
        identity.setOpaque(false);
        RoundedPanel badge = new RoundedPanel(new BorderLayout(), Color.WHITE, 48);
        badge.setPreferredSize(new Dimension(48, 48));
        JLabel numberLabel = UiComponents.label(String.format("%02d", number), 18, Font.BOLD, Theme.GREEN);
        numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        badge.add(numberLabel);
        identity.add(badge, BorderLayout.WEST);

        JPanel labels = new JPanel(new BorderLayout(0, 6));
        labels.setOpaque(false);
        labels.add(UiComponents.label(title, 19, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        labels.add(UiComponents.label(title + " 퀴즈에 도전해요", 13, Font.PLAIN, Theme.MUTED), BorderLayout.CENTER);
        identity.add(labels, BorderLayout.CENTER);
        row.add(identity, BorderLayout.CENTER);

        JButton button = UiComponents.roundedPrimaryButton("퀴즈 풀기 →");
        button.getAccessibleContext().setAccessibleName(title + " 퀴즈");
        button.addActionListener(event -> onSelect.run());
        row.add(button, BorderLayout.EAST);
        return row;
    }

    /** finder로 문제를 백그라운드에서 불러온 뒤 onLoaded로 화면을 그린다. */
    private void load(Supplier<List<QuizQuestion>> finder, Consumer<List<QuizQuestion>> onLoaded) {
        heading("문제를 불러오고 있어요", "잠시만 기다려 주세요.");
        refresh();
        loader = new SwingWorker<>() {
            @Override protected List<QuizQuestion> doInBackground() {
                return finder.get();
            }
            @Override protected void done() {
                if (!active || isCancelled()) return;
                try {
                    onLoaded.accept(get());
                } catch (CancellationException ignored) {
                    return;
                } catch (Exception exception) {
                    heading("문제를 불러오지 못했어요", "잠시 후 다시 시도해 주세요.");
                    JButton reload = UiComponents.roundedPrimaryButton("다시 시도");
                    reload.addActionListener(event -> load(finder, onLoaded));
                    content.add(reload);
                    backButton();
                }
                refresh();
            }
        };
        loader.execute();
    }

    /** 틀린 문제 목록을 보여 주고 다시 풀게 한다. 맞히면 onGraded가 오답 기록을 지운다. */
    private void showWrongNote(List<QuizQuestion> questions) {
        if (questions.isEmpty()) {
            heading("오답노트", "틀린 문제가 없어요. 퀴즈에 도전해 봐요!");
            backButton();
            return;
        }
        heading("오답노트", "틀린 문제 " + questions.size() + "개 · 다시 풀어서 맞히면 오답노트에서 사라져요.");
        int number = 1;
        for (QuizQuestion question : questions) {
            RoundedPanel row = new RoundedPanel(new BorderLayout(), Color.WHITE, 20);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            row.add(UiComponents.label(number++ + ". " + question.prompt, 17, Font.PLAIN, Theme.TEXT));
            content.add(row);
            gap(10);
        }
        gap(10);
        JButton again = UiComponents.roundedPrimaryButton("오답 다시 풀기");
        again.addActionListener(event -> {
            retry = true;
            attempt = new QuizAttempt(questions);
            showQuestion();
        });
        content.add(again);
        backButton();
    }

    private void startLevel(List<QuizQuestion> questions) {
        if (questions.isEmpty()) {
            heading("퀴즈 준비 중", "이 단원의 문제는 아직 준비 중이에요.");
            backButton();
        } else {
            attempt = new QuizAttempt(questions);
            showQuestion();
        }
    }

    private void showQuestion() {
        QuizQuestion question = attempt.current();
        heading(retry ? "오답에 다시 도전해요" : levelTitle + " 심화 퀴즈",
                attempt.number() + " / " + attempt.total() + " 문제 · " + (question.type == QuizQuestion.Type.OX ? "OX" : "객관식"));
        RoundedProgressBar progressBar = new RoundedProgressBar(0, attempt.total());
        progressBar.setValue(attempt.number());
        progressBar.setAlignmentX(LEFT_ALIGNMENT);
        content.add(progressBar);
        gap(20);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 22), Color.WHITE, 24);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createEmptyBorder(28, 30, 28, 30));
        card.add(wrappedText(question.prompt, 28, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);

        boolean ox = question.type == QuizQuestion.Type.OX;
        JPanel options = new JPanel(ox ? new GridLayout(1, 2, 16, 0) : new GridLayout(0, 1, 0, 12));
        options.setOpaque(false);
        options.setAlignmentX(LEFT_ALIGNMENT);
        List<ChoiceTile> tiles = new ArrayList<>();
        QuizBuddy buddy = new QuizBuddy();
        selectedChoiceId = null;
        for (QuizQuestion.Choice choice : question.choices) {
            ChoiceTile tile = new ChoiceTile(choice.choiceNo, choice.content, ox);
            tile.addActionListener(event -> {
                selectedChoiceId = choice.id;
                for (ChoiceTile other : tiles) {
                    other.setState(other == tile ? ChoiceTile.State.SELECTED : ChoiceTile.State.IDLE);
                }
                buddy.react(QuizBuddy.Mood.HOP);
            });
            tiles.add(tile);
            options.add(tile);
        }
        options.setMaximumSize(new Dimension(Integer.MAX_VALUE, options.getPreferredSize().height));

        JPanel answerColumn = UiComponents.column();
        answerColumn.add(options);
        TriangleFigure figure = TriangleFigure.of(question.figure);
        if (figure != null) {
            figure.onEvent(event -> {
                if (event.equals("done")) buddy.react(QuizBuddy.Mood.HAPPY);
                else if (event.equals("tap") || event.equals("snap")) buddy.react(QuizBuddy.Mood.HOP);
            });
        }
        if (!question.hint.isBlank()) {
            answerColumn.add(Box.createVerticalStrut(18));
            RoundedPanel hintBox = new RoundedPanel(new BorderLayout(), Theme.YELLOW, 20);
            hintBox.setAlignmentX(LEFT_ALIGNMENT);
            hintBox.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
            hintBox.add(wrappedText("힌트  " + question.hint, 19, Font.BOLD, Theme.TEXT));
            hintBox.setVisible(false);
            JButton hint = UiComponents.roundedPrimaryButton("힌트 보기");
            hint.setBackground(Theme.YELLOW);
            hint.setForeground(Theme.TEXT);
            hint.addActionListener(event -> {
                hintBox.setVisible(true);
                hint.setVisible(false);
                buddy.react(QuizBuddy.Mood.THINK);
                if (figure != null) figure.showHint();
                refresh();
            });
            answerColumn.add(hint);
            answerColumn.add(hintBox);
        }

        JPanel answerArea = new JPanel(new BorderLayout());
        answerArea.setOpaque(false);
        answerArea.add(answerColumn, BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(28, 0));
        body.setOpaque(false);
        if (figure != null) body.add(figure, BorderLayout.WEST);
        body.add(answerArea, BorderLayout.CENTER);
        card.add(body, BorderLayout.CENTER);

        RoundedPanel result = new RoundedPanel(new BorderLayout(18, 0), Theme.BACKGROUND, 20);
        result.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 20));
        result.add(buddy, BorderLayout.WEST);
        JPanel resultText = new JPanel(new BorderLayout(0, 8));
        resultText.setOpaque(false);
        resultText.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
        JLabel feedback = UiComponents.label("보기를 고르고 정답을 확인하세요.", 20, Font.BOLD, Theme.GREEN);
        resultText.add(feedback, BorderLayout.NORTH);
        result.add(resultText, BorderLayout.CENTER);
        card.add(result, BorderLayout.SOUTH);
        content.add(card);
        gap(20);

        JButton submit = UiComponents.roundedPrimaryButton("정답 확인");
        JButton next = UiComponents.roundedPrimaryButton(attempt.number() == attempt.total() ? "결과 보기" : "다음 문제 →");
        next.setEnabled(false);
        submit.addActionListener(event -> {
            if (selectedChoiceId == null) {
                feedback.setText("보기를 먼저 골라 주세요.");
                return;
            }
            boolean correct = attempt.submit(selectedChoiceId);
            try {
                onGraded.accept(question.id, correct);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "오답 기록을 저장하지 못했어요. 퀴즈는 계속할 수 있어요.",
                        "저장 실패", JOptionPane.WARNING_MESSAGE);
            }
            result.setBackground(correct ? new Color(0xD6F5E3) : new Color(0xFBE0DE));
            feedback.setFont(Theme.font(Font.BOLD, 24));
            feedback.setForeground(correct ? new Color(0x1F7A52) : new Color(0xB23B35));
            feedback.setText(correct ? "정답이에요! 멋져요!" : "아쉬워요. 정답은 " + question.correctChoice().content + "예요.");
            if (!question.explanation.isBlank()) {
                resultText.add(wrappedText(question.explanation, 19, Font.PLAIN, Theme.TEXT), BorderLayout.CENTER);
            }
            buddy.react(correct ? QuizBuddy.Mood.HAPPY : QuizBuddy.Mood.SAD);
            if (figure != null) figure.reveal(correct);
            submit.setEnabled(false);
            for (int i = 0; i < tiles.size(); i++) {
                QuizQuestion.Choice choice = question.choices.get(i);
                ChoiceTile tile = tiles.get(i);
                tile.setEnabled(false);
                if (choice.isCorrect) tile.setState(ChoiceTile.State.CORRECT);
                else if (choice.id.equals(selectedChoiceId)) tile.setState(ChoiceTile.State.WRONG);
                else tile.setState(ChoiceTile.State.DIMMED);
            }
            next.setEnabled(true);
            refresh();
        });
        next.addActionListener(event -> {
            attempt.next();
            if (attempt.isFinished()) showResult();
            else showQuestion();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(submit);
        buttons.add(Box.createHorizontalStrut(12));
        buttons.add(next);
        content.add(buttons);
        refresh();
    }

    /** 줄바꿈되는 읽기 전용 글. 배경은 부모를 따른다. */
    private static JTextArea wrappedText(String text, int size, int style, Color color) {
        JTextArea area = new JTextArea(text);
        area.setFont(Theme.font(style, size));
        area.setForeground(color);
        area.setOpaque(false);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setColumns(10);   // 선호 너비를 작게 둬야 부모 너비에 맞춰 줄바꿈한다
        area.setAlignmentX(LEFT_ALIGNMENT);
        return area;
    }

    private void showResult() {
        int score = (int) Math.round(100.0 * attempt.correctCount() / attempt.total());
        heading(retry ? "오답 재도전 결과" : "퀴즈 결과", "이번 시도: " + score + "점 · " + attempt.correctCount() + " / " + attempt.total() + " 정답");
        List<QuizQuestion> wrong = attempt.wrongQuestions();
        if (wrong.isEmpty()) {
            content.add(UiComponents.label("모두 맞혔어요! 멋진 도전이었어요.", 20, Font.BOLD, Theme.GREEN));
        } else {
            for (QuizQuestion question : wrong) {
                JTextArea text = new JTextArea(question.prompt + "\n정답: " + question.correctChoice().content);
                text.setEditable(false);
                text.setFont(Theme.font(Font.PLAIN, 17));
                text.setLineWrap(true);
                text.setWrapStyleWord(true);
                text.setRows(3);
                text.setAlignmentX(LEFT_ALIGNMENT);
                content.add(text);
                gap(12);
            }
            JButton again = UiComponents.roundedPrimaryButton("오답만 다시 풀기");
            again.addActionListener(event -> {
                retry = true;
                attempt = new QuizAttempt(wrong);
                showQuestion();
            });
            content.add(again);
        }
        backButton();
        refresh();
    }

    private void backButton() {
        gap(20);
        JButton back = UiComponents.roundedPrimaryButton("단원 선택으로");
        back.addActionListener(event -> showLevels());
        content.add(back);
    }

    private void gap(int height) { content.add(Box.createVerticalStrut(height)); }

    /** 가로는 스크롤 영역 너비를 따라가서, 세로 스크롤이 생겨도 글이 줄바꿈되고 가로 스크롤이 생기지 않는 패널. */
    private static final class ViewportWidthPanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return visible.height; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private void refresh() { content.revalidate(); content.repaint(); }
}
