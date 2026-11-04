package geometrybook.gui.lesson;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import geometrybook.gui.common.*;
import java.awt.*;
import javax.swing.*;

/** 4단계 마름모 설명, 확인 문제, 완료 화면. */
public final class RhombusLessonPanel extends JPanel {
    private static final String[] PROMPTS = {
        "어떤 사각형을 마름모라고 할까요?",
        "마름모의 한 변이 5cm예요. 다른 변의 길이는 얼마일까요?",
        "마름모의 두 대각선은 어떤 각으로 만날까요?"
    };
    private static final String[][] CHOICES = {
        {"네 각이 모두 직각인 사각형", "두 변만 길이가 같은 사각형", "네 변의 길이가 모두 같은 사각형"},
        {"5cm", "10cm", "20cm"},
        {"45도", "90도", "180도"}
    };
    private static final String[] EXPLANATIONS = {
        "네 변에 같은 길이 표시가 있었지요. 네 변의 길이가 모두 같으면 마름모예요.",
        "네 변의 길이가 같으니 다른 변도 각각 5cm예요.",
        "두 대각선이 만나는 곳의 작은 네모는 직각, 즉 90도를 뜻해요."
    };

    private static final int[] ANSWERS = {2, 0, 1};
    private final Runnable onHome, onLevel, onLogout;
    private RhombusAnimation animation;
    private final boolean[] solved = new boolean[3];
    private boolean completed, reviewMode, result;

    public RhombusLessonPanel(Runnable onHome, Runnable onLevel, Runnable onLogout) {
        super(new BorderLayout());
        this.onHome = onHome;
        this.onLevel = onLevel;
        this.onLogout = onLogout;
    }
    /** 새 시도를 만들며 이전 답안을 버린다. 완료한 단원은 복습으로 시작한다. */
    public void begin() {
        stop();
        java.util.Arrays.fill(solved, false);
        reviewMode = completed;
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
        JButton back = UiComponents.roundedPrimaryButton("‹ 레벨 지도로    ·    " + "LEVEL 04 마름모");
        back.addActionListener(e -> leave(onLevel));
        page.add(back);
        gap(page, 16);
        page.add(UiComponents.label((step == 1?"●":"○")+" 설명 보기     "+(step == 2?"●":"○")+" 확인 문제     "+(step == 3?"●":"○")+" 탐험 완료"+(reviewMode?"     · 복습 모드":""), 18, Font.BOLD, Theme.GREEN));
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

    private boolean allCorrect() {
        return solved[0] && solved[1] && solved[2];
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
        JProgressBar progress = new JProgressBar(0, RhombusAnimation.DURATION);
        animation = new RhombusAnimation(() -> {
            caption.setText(animation.caption());
            progress.setValue(animation.elapsed());
            pause.setText(animation.playing()?"일시정지":"재생");
        });
        playback.add(UiComponents.label("마름모의 비밀을 알아볼까요?", 24, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
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
        JLabel summary = UiComponents.label("<html><b>오늘의 발견</b><br><br>네 변의 길이가 같아요.<br><br>마주 보는 꼭짓점을 이은<br>선분은 대각선이에요.<br><br>두 대각선은 직각으로<br>만나요.</html>", 17, Font.PLAIN, Theme.TEXT);
        summary.setPreferredSize(new Dimension(190, 280));
        card.add(summary, BorderLayout.EAST);
        page.add(card);
        gap(page, 20);
        JButton quiz = UiComponents.roundedPrimaryButton(reviewMode?"설명 생략하고 문제 풀기 →":"설명 학습 완료 · 문제 풀기 →");
        quiz.addActionListener(e -> {
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
        finish.setEnabled(allCorrect());
        for (int i = 0; i<3; i++) {
            final int index = i;
            RoundedPanel card = new RoundedPanel(new BorderLayout(8, 12), Color.WHITE, 24);
            card.setAlignmentX(0f);
            card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
            card.add(UiComponents.label((i+1)+". "+PROMPTS[i], 18, Font.BOLD, Theme.TEXT), BorderLayout.NORTH);
            JPanel options = column();
            ButtonGroup group = new ButtonGroup();
            JRadioButton[] radios = new JRadioButton[3];
            for (int j = 0; j<3; j++) {
                radios[j] = new JRadioButton(CHOICES[i][j]);
                radios[j].setFont(Theme.font(Font.PLAIN, 16));
                radios[j].setOpaque(false);
                radios[j].setEnabled(!solved[i]);
                group.add(radios[j]);
                options.add(radios[j]);
            }
            card.add(options, BorderLayout.CENTER);
            JPanel footer = column();
            JLabel feedback = UiComponents.label(solved[i]?"정답! "+EXPLANATIONS[i]:" ", 14, Font.PLAIN, Theme.GREEN);
            JButton submit = UiComponents.roundedPrimaryButton(solved[i]?"정답 확인 완료":"정답 확인");
            submit.setEnabled(!solved[i]);
            submit.addActionListener(e -> {
                int choice = -1;
                for (int j = 0; j<3; j++)if (radios[j].isSelected())choice = j;
                if (choice<0) {
                    feedback.setText("보기를 먼저 선택해 주세요.");
                    return;
                }
                boolean correct = choice == ANSWERS[index];
                feedback.setText((correct?"정답! ":"다시 생각해 보세요. ")+EXPLANATIONS[index]);
                if (correct) {
                    solved[index] = true;
                    submit.setEnabled(false);
                    submit.setText("정답 확인 완료");
                    for (JRadioButton radio:radios)radio.setEnabled(false);
                }
                finish.setEnabled(allCorrect());
            });
            footer.add(feedback);
            gap(footer, 8);
            footer.add(submit);
            card.add(footer, BorderLayout.SOUTH);
            page.add(card);
            gap(page, 14);
        }
        finish.addActionListener(e -> {
            completed = true;
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
        card.add(UiComponents.label("마름모" + (reviewMode ? " 복습 완료!" : " 탐험 완료!"), 30, Font.BOLD, Theme.GREEN), BorderLayout.NORTH);
        card.add(UiComponents.label("<html>세 문제를 모두 맞혔어요!<br><br>네 변의 길이가 같고, 두 대각선은 직각으로 만나요.<br><br>"+(reviewMode?"복습으로 추가 포인트는 지급되지 않아요.":"첫 탐험을 마쳤어요. 다음 단계는 사다리꼴이에요.")+"<br><br>이번 완료 기록은 앱 실행 중에만 유지됩니다.</html>", 18, Font.PLAIN, Theme.TEXT), BorderLayout.CENTER);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("도형탐험대 · 마름모 미리보기");
            RhombusLessonPanel panel = new RhombusLessonPanel(
                    frame::dispose, frame::dispose, frame::dispose);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    panel.leave(frame::dispose);
                }
            });
            frame.setContentPane(panel);
            frame.setSize(1200, 800);
            frame.setLocationRelativeTo(null);
            panel.begin();
            frame.setVisible(true);
        });
    }
}
