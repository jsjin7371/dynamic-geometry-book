package geometrybook.gui.lesson;

import geometrybook.domain.StudyProgress;
import geometrybook.domain.LessonTopic;
import geometrybook.gui.common.*;
import geometrybook.gui.lesson.animation.LessonAnimation;
import geometrybook.gui.lesson.animation.TriangleAnimation;
import geometrybook.gui.lesson.animation.SquareAnimation;
import geometrybook.gui.lesson.animation.ParallelogramAnimation;
import java.awt.*;
import javax.swing.*;

/** 도형 설명, 확인 문제, 결과 화면을 담당한다. 영구 저장은 수행하지 않는다. */
public final class LessonPanel extends JPanel {
    private final StudyProgress study;
    private StudyProgress.Session session;
    private final Runnable onHome, onLevel, onQuiz, onLogout;
    private LessonAnimation animation;
    private final LessonTopic topic;
    private boolean result;
    public LessonPanel(StudyProgress study, LessonTopic topic, Runnable onHome, Runnable onLevel, Runnable onQuiz, Runnable onLogout) {
        super(new BorderLayout());
        this.study = study;
        this.topic = topic;
        this.onHome = onHome;
        this.onLevel = onLevel;
        this.onQuiz = onQuiz;
        this.onLogout = onLogout;
    }
    /** 새 시도를 만들며 이전 답안을 버린다. 완료한 도형은 복습으로 시작한다. */
    public void begin() {
        stop();
        session = study.start(topic);
        result = false;
        showExplanation();
    }

    public void stop() {
        if (animation != null) animation.pause();
    }
    /** 미완료 시도에서 이탈을 확인하고 타이머를 정리한다. */
    public void leave(Runnable action) {
        boolean wasPlaying = animation != null && animation.playing();
        stop();
        if (!result && JOptionPane.showConfirmDialog(this, "학습을 나가면 현재 답안은 사라져요. 나갈까요?", "학습 종료", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            if (wasPlaying) animation.play();
            return;
        }
        action.run();
    }

    private JPanel page(int step) {
        stop();
        removeAll();
        AppShell shell = new AppShell(AppShell.Page.LESSON, () -> leave(onHome), () -> leave(onLevel), () -> leave(onQuiz), () -> leave(onLogout));
        JPanel page = column();
        page.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        page.setBackground(Theme.BACKGROUND);
        page.setOpaque(true);
        JButton back = UiComponents.roundedPrimaryButton(String.format("‹ 레벨 지도로    ·    LEVEL %02d %s", topic.number(), topic.title()));
        back.addActionListener(e -> leave(onLevel));
        page.add(back);
        gap(page, 16);
        page.add(UiComponents.label((step == 1?"●":"○")+" 설명 보기     "+(step == 2?"●":"○")+" 확인 문제     "+(step == 3?"●":"○")+" 탐험 완료"+(session.review?"     · 복습 모드":""), 18, Font.BOLD, Theme.GREEN));
        gap(page, 20);
        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        shell.setContent(scroll);
        add(shell);
        revalidate();
        repaint();
        return page;
    }

    private void showExplanation() {
        JPanel page = page(1);
        RoundedPanel card = new RoundedPanel(new BorderLayout(20, 0), Theme.MINT, 28);
        card.setAlignmentX(0f);
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JPanel playback = new JPanel(new BorderLayout(0, 12));
        playback.setOpaque(false);
        JLabel caption = UiComponents.label("", 15, Font.BOLD, Theme.GREEN);
        JButton pause = UiComponents.roundedPrimaryButton("일시정지");
        RoundedProgressBar progress = new RoundedProgressBar(0, LessonAnimation.DURATION);
        Runnable updatePlayback = () -> {
            caption.setText(animation.caption());
            progress.setValue(animation.elapsed());
            pause.setText(animation.playing()?"일시정지":"재생");
        };
        switch (topic) {
            case TRIANGLE:
                animation = new TriangleAnimation(updatePlayback);
                break;
            case SQUARE:
                animation = new SquareAnimation(updatePlayback);
                break;
            case PARALLELOGRAM:
                animation = new ParallelogramAnimation(updatePlayback);
                break;
            default:
                JOptionPane.showMessageDialog(this, "이 레슨의 애니메이션은 준비 중이에요.");
                onLevel.run();
                return;
        }
        playback.add(UiComponents.label(topic.title() + "의 비밀을 알아볼까요?", 24, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
        playback.add(animation, BorderLayout.CENTER);
        JPanel controls = column();
        controls.add(caption);
        gap(controls, 12);
        controls.add(progress);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        pause.addActionListener(e -> {
            if (animation.playing()) animation.pause();
            else animation.play();
        });
        JButton replay = UiComponents.roundedPrimaryButton("다시 보기");
        replay.addActionListener(e -> animation.restart());
        buttons.add(pause);
        buttons.add(replay);
        controls.add(buttons);
        playback.add(controls, BorderLayout.SOUTH);
        card.add(playback, BorderLayout.CENTER);
        JLabel summary = UiComponents.label("<html><b>오늘의 발견</b><br><br>" + topic.summary() + "</html>", 17, Font.PLAIN, Theme.TEXT);
        summary.setPreferredSize(new Dimension(190, 280));
        card.add(summary, BorderLayout.EAST);
        page.add(card);
        gap(page, 20);
        JButton quiz = UiComponents.roundedPrimaryButton(session.review?"설명 생략하고 문제 풀기 →":"설명 학습 완료 · 문제 풀기 →");
        quiz.addActionListener(e -> {
            session.confirmExplanation();
            showQuiz();
        });
        page.add(quiz);
        animation.play();
        revalidate();
        repaint();
    }

    private void showQuiz() {
        JPanel page = page(2);
        page.add(UiComponents.label("세 문제를 모두 맞혀 보세요. 맞힌 문제는 그대로 유지돼요.", 16, Font.PLAIN, Theme.TEXT));
        gap(page, 16);
        JButton finish = UiComponents.roundedPrimaryButton("탐험 완료 →");
        finish.setEnabled(session.allCorrect());
        for (int i = 0; i<3; i++) {
            final int index = i;
            RoundedPanel card = new RoundedPanel(new BorderLayout(8, 12), Color.WHITE, 24);
            card.setAlignmentX(0f);
            card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
            card.add(UiComponents.label((i+1)+". "+topic.prompt(i), 18, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
            JPanel options = column();
            ButtonGroup group = new ButtonGroup();
            JRadioButton[] radios = new JRadioButton[3];
            for (int j = 0; j<3; j++) {
                radios[j] = new JRadioButton(topic.choice(i, j));
                radios[j].setFont(Theme.font(Font.PLAIN, 16));
                radios[j].setOpaque(false);
                radios[j].setEnabled(!session.isCorrect(i));
                group.add(radios[j]);
                options.add(radios[j]);
            }
            card.add(options, BorderLayout.CENTER);
            JPanel footer = column();
            JLabel feedback = UiComponents.label(session.isCorrect(i)?"정답! "+topic.explanation(i):" ", 14, Font.PLAIN, Theme.GREEN);
            JButton submit = UiComponents.roundedPrimaryButton(session.isCorrect(i)?"정답 확인 완료":"정답 확인");
            submit.setEnabled(!session.isCorrect(i));
            submit.addActionListener(e -> {
                int choice = -1;
                for (int j = 0; j<3; j++)if (radios[j].isSelected())choice = j;
                if (choice<0) {
                    feedback.setText("보기를 먼저 선택해 주세요.");
                    return;
                }
                boolean correct = session.submit(index, choice);
                feedback.setText((correct?"정답! ":"다시 생각해 보세요. ")+topic.explanation(index));
                if (correct) {
                    submit.setEnabled(false);
                    submit.setText("정답 확인 완료");
                    for (JRadioButton radio:radios)radio.setEnabled(false);
                }
                finish.setEnabled(session.allCorrect());
            });
            footer.add(feedback);
            gap(footer, 8);
            footer.add(submit);
            card.add(footer, BorderLayout.SOUTH);
            page.add(card);
            gap(page, 14);
        }
        finish.addActionListener(e -> {
            try {
                study.complete(session);
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "완료 기록을 저장하지 못했어요. 잠시 후 다시 눌러 주세요.",
                        "저장 실패", JOptionPane.ERROR_MESSAGE);
                return;
            }
            result = true;
            showResult();
        });
        page.add(finish);
        revalidate();
        repaint();
    }

    private void showResult() {
        JPanel page = page(3);
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 24), Theme.MINT, 28);
        card.setAlignmentX(0f);
        card.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
        card.add(UiComponents.label(topic.title() + (session.review ? " 복습 완료!" : " 탐험 완료!"), 30, Font.BOLD, Theme.GREEN), BorderLayout.NORTH);
        card.add(UiComponents.label("<html>세 문제를 모두 맞혔어요!<br><br>" + topic.summary() + "<br><br>"+(session.review?"복습으로 추가 포인트는 지급되지 않아요.":(topic.nextTitle().isEmpty() ? "마지막 단계까지 탐험을 마쳤어요!" : "첫 탐험을 마쳤어요. 다음 단계는 " + topic.nextTitle() + "이에요."))+"</html>", 18, Font.PLAIN, Theme.TEXT), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false);
        JButton review = UiComponents.roundedPrimaryButton("복습하기");
        review.addActionListener(e -> begin());
        actions.add(review);
        JButton levels = UiComponents.roundedPrimaryButton("레벨 지도로");
        levels.addActionListener(e -> onLevel.run());
        actions.add(levels);
        card.add(actions, BorderLayout.SOUTH);
        page.add(card);
        revalidate();
        repaint();
    }

    private static JPanel column() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(0f);
        return p;
    }

    private static void gap(JPanel p, int height) {
        p.add(Box.createVerticalStrut(height));
    }
}
