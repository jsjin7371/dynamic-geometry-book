package geometrybook.gui.level;

import geometrybook.gui.common.AppShell;
import geometrybook.domain.StudyProgress;
import geometrybook.domain.LessonTopic;
import java.util.function.Consumer;
import geometrybook.gui.common.RoundedPanel;
import geometrybook.gui.common.Theme;
import geometrybook.gui.common.TriangleCharacter;
import geometrybook.gui.common.UiComponents;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

public final class LevelMapPanel extends JPanel {
    private static final String[] TITLES = {
        "삼각형", "정사각형", "직사각형", "마름모", "사다리꼴", "평행사변형", "원"
    };
    private final StudyProgress study;
    private final Consumer<LessonTopic> onLesson;
    private int COMPLETED;
    private final JPanel levelList = new JPanel(new GridLayout(0, 1, 0, 8));
    private final JPanel details = new JPanel(new BorderLayout());
    private int selected = 0;

    public LevelMapPanel(StudyProgress study, Consumer<LessonTopic> onLesson, Runnable onHome, Runnable onLogout) {
        super(new BorderLayout());
        this.study = study;
        this.onLesson = onLesson;
        COMPLETED = study.completedCount();
        AppShell shell = new AppShell(AppShell.Page.LEVEL, onHome, () -> {
        }, onLogout);
        JScrollPane scroll = new JScrollPane(content());
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        shell.setContent(scroll);
        add(shell);
    }

    private JPanel content() {
        JPanel content = new JPanel();
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(30, 32, 32, 32));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(UiComponents.label("EXPLORE THE SHAPES", 12, Font.BOLD, Theme.GREEN));
        content.add(Box.createVerticalStrut(10));
        content.add(UiComponents.label("한 걸음씩, 도형 세계를 탐험해요", 28, Font.BOLD, Theme.TEXT));
        content.add(Box.createVerticalStrut(10));
        content.add(UiComponents.label("완료한 단계는 언제든 다시 볼 수 있어요.", 16, Font.PLAIN, Theme.MUTED));
        content.add(Box.createVerticalStrut(20));
        content.add(progress());
        content.add(Box.createVerticalStrut(16));
        JPanel main = new JPanel(new BorderLayout(20, 0));
        main.setOpaque(false);
        main.setAlignmentX(LEFT_ALIGNMENT);
        RoundedPanel listCard = new RoundedPanel(new BorderLayout(), Color.WHITE, 28);
        listCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        levelList.setOpaque(false);
        listCard.add(levelList);
        main.add(listCard, BorderLayout.CENTER);
        details.setOpaque(false);
        details.setPreferredSize(new Dimension(300, 490));
        main.add(details, BorderLayout.EAST);
        content.add(main);
        refreshSelection();
        return content;
    }

    private JPanel progress() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(20, 0), Theme.MINT, 24);
        card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.add(UiComponents.label("전체 7단계 중 " + COMPLETED + "단계 완료", 16, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        JProgressBar bar = new JProgressBar(0, TITLES.length);
        bar.setValue(COMPLETED);
        bar.setForeground(Theme.GREEN);
        bar.setBackground(new Color(0xDCE6DF));
        bar.setBorderPainted(false);
        bar.setPreferredSize(new Dimension(160, 10));
        card.add(bar, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        return card;
    }

    private void refreshSelection() {
        levelList.removeAll();
        for (int i = 0; i < TITLES.length; i++) {
            final int index = i;
            String status = i < COMPLETED ? "완료 · 복습 가능" : i == COMPLETED ? "학습 가능" : "잠김";
            JButton row = UiComponents.roundedPrimaryButton(String.format("%02d    %s    ·    %s", i + 1, TITLES[i], status));
            row.setHorizontalAlignment(JButton.LEFT);
            row.setBackground(i == selected ? new Color(0xFFF1C9) : i < COMPLETED ? Theme.MINT : Theme.BACKGROUND);
            row.setForeground(i > COMPLETED ? Theme.MUTED : Theme.TEXT);
            row.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            row.addActionListener(event -> {
                selected = index;
                refreshSelection();
            });
            levelList.add(row);
        }
        details.removeAll();
        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.MINT, 24);
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(UiComponents.label(String.format("선택한 탐험 · LEVEL %02d", selected + 1), 13, Font.BOLD, Theme.GREEN));
        body.add(Box.createVerticalStrut(16));
        TriangleCharacter character = new TriangleCharacter();
        character.setPreferredSize(new Dimension(140, 130));
        character.setMaximumSize(new Dimension(160, 130));
        character.setAlignmentX(LEFT_ALIGNMENT);
        body.add(character);
        body.add(Box.createVerticalStrut(16));
        body.add(UiComponents.label(TITLES[selected], 24, Font.BOLD, Theme.TEXT));
        body.add(Box.createVerticalStrut(14));
        body.add(UiComponents.label("난이도 · 미정", 15, Font.PLAIN, Theme.TEXT));
        body.add(Box.createVerticalStrut(8));
        body.add(UiComponents.label("최초 보상 · 미정", 15, Font.PLAIN, Theme.TEXT));
        body.add(Box.createVerticalStrut(8));
        body.add(UiComponents.label(selected < COMPLETED ? "완료 · 복습 가능" : selected == COMPLETED ? "학습 가능" : "이전 단계를 완료하면 열려요", 14, Font.PLAIN, Theme.MUTED));
        body.add(Box.createVerticalStrut(20));
        JButton start = UiComponents.roundedPrimaryButton(selected < COMPLETED ? "복습하러 가기  →" : "탐험 시작하기  →");
        start.setEnabled(selected < LessonTopic.values().length && selected <= COMPLETED);
        start.addActionListener(e -> onLesson.accept(LessonTopic.values()[selected]));
        start.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        body.add(start);
        card.add(body, BorderLayout.NORTH);
        details.add(card);
        levelList.revalidate();
        levelList.repaint();
        details.revalidate();
        details.repaint();
    }
}
