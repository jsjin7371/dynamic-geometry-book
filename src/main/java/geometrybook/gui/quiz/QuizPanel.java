package geometrybook.gui.quiz;

import geometrybook.application.quiz.QuizSource;
import geometrybook.domain.quiz.QuizAttempt;
import geometrybook.domain.quiz.QuizQuestion;
import geometrybook.gui.common.AppShell;
import geometrybook.gui.common.RoundedPanel;
import geometrybook.gui.common.Theme;
import geometrybook.gui.common.UiComponents;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import javax.swing.*;

/** 단원 선택, 한 문제씩 채점, 결과와 오답 재도전을 제공한다. */
public final class QuizPanel extends JPanel {
    private final QuizSource source;
    private final BiConsumer<String, Boolean> onGraded;
    private final JPanel content = new JPanel();
    private QuizAttempt attempt;
    private SwingWorker<List<QuizQuestion>, Void> loader;
    private boolean active = true;
    private boolean retry;
    private String levelTitle;

        public QuizPanel(QuizSource source, Runnable onHome, Runnable onLevel, Runnable onLogout) {
        this(source, (questionId, correct) -> { }, onHome, onLevel, onLogout);
    }
    /** onGraded: 한 문제를 채점할 때마다 (문제 ID, 정답 여부)를 받는다. 오답 기록 저장에 쓴다. */
    public QuizPanel(QuizSource source, BiConsumer<String, Boolean> onGraded,
                     Runnable onHome, Runnable onLevel, Runnable onLogout) {
        super(new BorderLayout());
        this.source = source;
        this.onGraded = onGraded;
        AppShell shell = new AppShell(AppShell.Page.QUIZ, () -> leave(onHome),
                () -> leave(onLevel), () -> {}, () -> leave(onLogout));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(30, 32, 32, 32));
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
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

        RoundedPanel list = new RoundedPanel(null, Color.WHITE, 28);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setAlignmentX(LEFT_ALIGNMENT);
        list.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        list.add(UiComponents.label("단원별 퀴즈 선택", 21, Font.BOLD, Theme.TEXT));
        list.add(Box.createVerticalStrut(20));
        String[] titles = {"삼각형", "정사각형", "직사각형", "마름모", "사다리꼴", "평행사변형", "원"};
        String[] ids = {"TRIANGLE", "SQUARE", "RECTANGLE", "RHOMBUS", "TRAPEZOID", "PARALLELOGRAM", "CIRCLE"};
        for (int i = 0; i < titles.length; i++) {
            final String id = ids[i];
            final String title = titles[i];
            list.add(levelCard(i + 1, title, () -> {
                levelTitle = title;
                load(id);
            }));
            if (i < titles.length - 1) list.add(Box.createVerticalStrut(14));
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

    private void load(String levelId) {
        heading("문제를 불러오고 있어요", "잠시만 기다려 주세요.");
        refresh();
        loader = new SwingWorker<>() {
            @Override protected List<QuizQuestion> doInBackground() {
                return source.findByLevelId(levelId);
            }
            @Override protected void done() {
                if (!active || isCancelled()) return;
                try {
                    List<QuizQuestion> questions = get();
                    if (questions.isEmpty()) {
                        heading("퀴즈 준비 중", "이 단원의 문제는 아직 준비 중이에요.");
                        backButton();
                    } else {
                        attempt = new QuizAttempt(questions);
                        showQuestion();
                    }
                } catch (CancellationException ignored) {
                    return;
                } catch (Exception exception) {
                    heading("문제를 불러오지 못했어요", "잠시 후 다시 시도해 주세요.");
                    JButton reload = UiComponents.roundedPrimaryButton("다시 시도");
                    reload.addActionListener(event -> load(levelId));
                    content.add(reload);
                    backButton();
                }
                refresh();
            }
        };
        loader.execute();
    }

    private void showQuestion() {
        QuizQuestion question = attempt.current();
        heading(retry ? "오답에 다시 도전해요" : levelTitle + " 심화 퀴즈",
                attempt.number() + " / " + attempt.total() + " 문제 · " + (question.type == QuizQuestion.Type.OX ? "OX" : "객관식"));
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 24), Color.WHITE, 24);
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        JTextArea prompt = new JTextArea(question.prompt);
        prompt.setFont(Theme.font(Font.BOLD, 22));
        prompt.setForeground(Theme.TEXT);
        prompt.setLineWrap(true);
        prompt.setWrapStyleWord(true);
        prompt.setEditable(false);
        prompt.setRows(3);
        card.add(prompt, BorderLayout.NORTH);
        JPanel options = new JPanel();
        options.setOpaque(false);
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        ButtonGroup group = new ButtonGroup();
        for (QuizQuestion.Choice choice : question.choices) {
            JRadioButton radio = new JRadioButton(choice.content);
            radio.setActionCommand(choice.id);
            radio.setOpaque(false);
            radio.setFont(Theme.font(Font.PLAIN, 19));
            radio.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
            group.add(radio);
            options.add(radio);
        }
        card.add(options, BorderLayout.CENTER);
        JLabel feedback = UiComponents.label("보기를 선택하고 정답을 확인하세요.", 16, Font.PLAIN, Theme.GREEN);
        card.add(feedback, BorderLayout.SOUTH);
        content.add(card);
        gap(20);
        JButton submit = UiComponents.roundedPrimaryButton("정답 확인");
        JButton next = UiComponents.roundedPrimaryButton(attempt.number() == attempt.total() ? "결과 보기" : "다음 문제 →");
        next.setEnabled(false);
        submit.addActionListener(event -> {
            if (group.getSelection() == null) {
                feedback.setText("보기를 먼저 선택해 주세요.");
                return;
            }
            boolean correct = attempt.submit(group.getSelection().getActionCommand());
            try {
                onGraded.accept(question.id, correct);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "오답 기록을 저장하지 못했어요. 퀴즈는 계속할 수 있어요.",
                        "저장 실패", JOptionPane.WARNING_MESSAGE);
            }
            feedback.setText(correct ? "정답이에요!" : "아쉬워요. 정답은 " + question.correctChoice().content + "입니다.");
            submit.setEnabled(false);
            java.util.Collections.list(group.getElements()).forEach(button -> button.setEnabled(false));
            next.setEnabled(true);
        });
        next.addActionListener(event -> {
            attempt.next();
            if (attempt.isFinished()) showResult();
            else showQuestion();
        });
        content.add(submit);
        gap(12);
        content.add(next);
        refresh();
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
    private void refresh() { content.revalidate(); content.repaint(); }
}
