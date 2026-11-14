package geometrybook.gui.home;

import geometrybook.gui.common.RoundedPanel;
import geometrybook.domain.Level;
import geometrybook.domain.StudyProgress;
import geometrybook.gui.common.AppShell;
import geometrybook.gui.common.Theme;
import geometrybook.gui.common.TriangleCharacter;
import geometrybook.gui.common.UiComponents;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/** 임시 데이터로 구성한 나의 탐험 화면. */
public final class HomePanel extends JPanel {
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private final Set<LocalDate> studyDates;
    private final StudyProgress study;
    private final Runnable onLevel;
    private YearMonth month = YearMonth.from(today);
    private final JLabel monthLabel = UiComponents.label("", 17, Font.BOLD, Theme.TEXT);
    private final JPanel calendarGrid = new JPanel(new GridLayout(0, 7, 4, 4));

    public HomePanel(StudyProgress study, Runnable onLevel, Runnable onQuiz, Runnable onLogout) {
        super(new BorderLayout());
        this.study = study;
        this.studyDates = study.studyDates();
        this.onLevel = onLevel;
        setBackground(Theme.BACKGROUND);
        AppShell shell = new AppShell(AppShell.Page.HOME, () -> {
        }, onLevel, onQuiz, onLogout);
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
        content.add(UiComponents.label("MY ADVENTURE", 12, Font.BOLD, Theme.GREEN));
        content.add(Box.createVerticalStrut(10));
        content.add(UiComponents.label("초록별, 오늘은 어떤 도형을 만날까?", 28, Font.BOLD, Theme.TEXT));
        content.add(Box.createVerticalStrut(10));
        content.add(UiComponents.label("도형 친구들과 함께 탐험해요.", 16, Font.PLAIN, Theme.MUTED));
        content.add(Box.createVerticalStrut(20));
        content.add(lessonCard());
        content.add(Box.createVerticalStrut(12));
        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(0f);
        stats.add(statCard("전체 진도", study.completedCount() + " / " + Level.values().length + " 단계", Theme.LAVENDER));
        stats.add(statCard("모은 포인트", "미정", new Color(0xFFF1C9)));
        stats.add(statCard("연속 학습", studyDates.isEmpty()?"아직 기록 없음":"1일째 탐험 중", new Color(0xFCE6DC)));
        stats.setPreferredSize(new Dimension(0, 115));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        content.add(stats);
        content.add(Box.createVerticalStrut(12));
        content.add(calendar());
        return content;
    }

    /** 레벨은 순서대로만 완료되므로 완료 수가 곧 다음 레벨의 위치다. */
    private String nextLabel() {
        int done = study.completedCount();
        if (done == 0) return "첫 탐험 · LEVEL 01";
        if (done >= Level.values().length) return "배운 도형을 복습해요";
        return "다음 탐험 · " + Level.values()[done].title();
    }

    private JPanel lessonCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.MINT, 28);
        card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        card.setAlignmentX(0f);
        JPanel text = UiComponents.column();
        text.add(UiComponents.label(nextLabel(), 13, Font.BOLD, Theme.GREEN));
        text.add(Box.createVerticalStrut(10));
        text.add(UiComponents.label("새로운 도형을 만나봐요!", 23, Font.BOLD, Theme.TEXT));
        text.add(Box.createVerticalStrut(8));
        text.add(UiComponents.label("한 걸음씩 도형의 비밀을 알아가요.", 15, Font.PLAIN, Theme.TEXT));
        text.add(Box.createVerticalStrut(16));
        JButton start = UiComponents.roundedPrimaryButton("탐험 시작하기  →");
        start.setMaximumSize(new Dimension(248, 52));
        start.setPreferredSize(new Dimension(248, 52));
        start.addActionListener(e -> onLevel.run());
        text.add(start);
        card.add(text, BorderLayout.CENTER);
        TriangleCharacter character = new TriangleCharacter();
        character.setPreferredSize(new Dimension(215, 145));
        character.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 35));
        card.add(character, BorderLayout.EAST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 215));
        return card;
    }

    private JPanel statCard(String title, String value, Color color) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), color, 24);
        card.setBorder(BorderFactory.createEmptyBorder(10, 20, 18, 20));
        JLabel titleLabel = UiComponents.label(title, 13, Font.PLAIN, Theme.MUTED);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(UiComponents.label(value, 22, Font.BOLD, Theme.TEXT), BorderLayout.CENTER);
        return card;
    }

    private JPanel calendar() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 16), Color.WHITE, 28);
        card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        card.setAlignmentX(0f);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UiComponents.label("나의 학습 달력", 20, Font.BOLD, Theme.TEXT), BorderLayout.WEST);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        controls.setOpaque(false);
        JButton previous = monthButton("‹", -1);
        JButton next = monthButton("›", 1);
        controls.add(previous);
        controls.add(monthLabel);
        controls.add(next);
        top.add(controls, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);
        calendarGrid.setOpaque(false);
        card.add(calendarGrid, BorderLayout.CENTER);
        card.add(UiComponents.label("✓ 학습 완료  ·  하루에 여러 번 완료해도 한 번 표시돼요", 12, Font.PLAIN, Theme.MUTED), BorderLayout.SOUTH);
        refreshCalendar();
        return card;
    }

    private JButton monthButton(String text, int offset) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.BOLD, 20));
        button.setForeground(Theme.GREEN);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.addActionListener(event -> {
            month = month.plusMonths(offset);
            refreshCalendar();
        });
        return button;
    }

    private void refreshCalendar() {
        monthLabel.setText(month.getYear() + "년 " + month.getMonthValue() + "월");
        calendarGrid.removeAll();
        for (String day : new String[] {
            "일", "월", "화", "수", "목", "금", "토"
        }) {
            calendarGrid.add(calendarCell(day, false));
        }
        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
        for (int i = 0; i < offset; i++) calendarGrid.add(calendarCell("", false));
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            boolean studied = studyDates.contains(month.atDay(day));
            calendarGrid.add(calendarCell((studied ? "✓ " : "") + day, studied));
        }
        calendarGrid.revalidate();
        calendarGrid.repaint();
    }

    private JComponent calendarCell(String text, boolean studied) {
        JLabel cell = new JLabel(text, SwingConstants.CENTER);
        cell.setFont(Theme.font(studied ? Font.BOLD : Font.PLAIN, 13));
        cell.setForeground(studied ? Theme.GREEN : Theme.TEXT);
        cell.setPreferredSize(new Dimension(38, 28));
        if (studied) {
            RoundedPanel background = new RoundedPanel(new BorderLayout(), Theme.MINT, 20);
            background.add(cell, BorderLayout.CENTER);
            return background;
        }
        return cell;
    }
}
