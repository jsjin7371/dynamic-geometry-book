package geometrybook.gui.lesson;

import geometrybook.domain.TriangleStudy;
import geometrybook.gui.common.*;
import java.awt.*;
import javax.swing.*;

/** 삼각형 설명, 확인 문제, 결과 화면을 담당한다. 영구 저장은 수행하지 않는다. */
public final class LessonPanel extends JPanel {
    private final TriangleStudy study;
    private TriangleStudy.Session session;
    private final Runnable onHome, onLevel, onLogout;
    private TriangleAnimation animation;
    private boolean result;
    private final String[] prompts = {
        "삼각형의 변은 몇 개일까요?", "삼각형의 꼭짓점은 몇 개일까요?", "삼각형의 세 내각을 모두 더하면 몇 도일까요?"
    };
    private final String[][] choices = {
        {
            "2개", "3개", "4개"
        }, {
            "1개", "2개", "3개"
        }, {
            "180도", "90도", "360도"
        }
    };
    private final String[] explanations = {
        "삼각형은 곧은 선분 3개로 둘러싸인 도형이에요.", "변과 변이 만나는 꼭짓점은 3개예요.", "세 내각을 한 점에 모으면 곧은 각인 180도가 돼요."
    };

    public LessonPanel(TriangleStudy study, Runnable onHome, Runnable onLevel, Runnable onLogout) {
        super(new BorderLayout());
        this.study = study;
        this.onHome = onHome;
        this.onLevel = onLevel;
        this.onLogout = onLogout;
    }
    /** 새 시도를 만들며 이전 답안을 버린다. 완료한 삼각형은 복습으로 시작한다. */
    public void begin() {
        stop();
        session = study.start();
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
        AppShell shell = new AppShell(AppShell.Page.LESSON, () -> leave(onHome), () -> leave(onLevel), () -> leave(onLogout));
        JPanel page = column();
        page.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        page.setBackground(Theme.BACKGROUND);
        page.setOpaque(true);
        JButton back = UiComponents.roundedPrimaryButton("‹ 레벨 지도로    ·    LEVEL 01 삼각형");
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
        JProgressBar progress = new JProgressBar(0, TriangleAnimation.DURATION);
        animation = new TriangleAnimation(() -> {
            caption.setText(animation.caption());
            progress.setValue(animation.elapsed());
            pause.setText(animation.playing()?"일시정지":"재생");
        });
        playback.add(UiComponents.label("삼각형의 비밀을 알아볼까요?", 24, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
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
        JLabel summary = UiComponents.label("<html><b>오늘의 발견</b><br><br>변 3개 · 꼭짓점 3개<br><br>세 내각의 합은 180°<br><br>모양이 달라도<br>삼각형의 내각의 합은<br>항상 같아요.</html>", 17, Font.PLAIN, Theme.TEXT);
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
            card.add(UiComponents.label((i+1)+". "+prompts[i], 18, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
            JPanel options = column();
            ButtonGroup group = new ButtonGroup();
            JRadioButton[] radios = new JRadioButton[3];
            for (int j = 0; j<3; j++) {
                radios[j] = new JRadioButton(choices[i][j]);
                radios[j].setFont(Theme.font(Font.PLAIN, 16));
                radios[j].setOpaque(false);
                radios[j].setEnabled(!session.isCorrect(i));
                group.add(radios[j]);
                options.add(radios[j]);
            }
            card.add(options, BorderLayout.CENTER);
            JPanel footer = column();
            JLabel feedback = UiComponents.label(session.isCorrect(i)?"정답! "+explanations[i]:" ", 14, Font.PLAIN, Theme.GREEN);
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
                feedback.setText((correct?"정답! ":"다시 생각해 보세요. ")+explanations[index]);
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
            study.complete(session);
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
        card.add(UiComponents.label(session.review?"삼각형 복습 완료!":"삼각형 탐험 완료!", 30, Font.BOLD, Theme.GREEN), BorderLayout.NORTH);
        card.add(UiComponents.label("<html>세 문제를 모두 맞혔어요!<br><br>변과 꼭짓점은 3개, 세 내각의 합은 180도예요.<br><br>"+(session.review?"복습으로 추가 포인트는 지급되지 않아요.":"첫 탐험을 마쳤어요. 다음 단계는 정사각형이에요.")+"<br><br>이번 완료 기록은 앱 실행 중에만 유지됩니다.</html>", 18, Font.PLAIN, Theme.TEXT), BorderLayout.CENTER);
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
