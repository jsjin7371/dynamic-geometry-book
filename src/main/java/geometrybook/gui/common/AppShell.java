package geometrybook.gui.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

/** 로그인 후 화면에서 공유하는 헤더와 탐색 메뉴. 사용자 정보는 현재 임시 값이다. */
public final class AppShell extends JPanel {
    /** 사이드바에서 강조할 현재 화면. */
    public enum Page { HOME, LEVEL, LESSON }

    /**
     * 공통 헤더와 사이드바를 구성한다. 화면 전환과 로그아웃 처리는 호출자가 연결한다.
     * @param selected 사이드바에서 강조할 화면
     * @param onHome 나의 탐험 메뉴를 선택했을 때 실행할 동작
     * @param onLevel 레벨 지도 메뉴를 선택했을 때 실행할 동작
     * @param onLogout 로그아웃을 선택했을 때 실행할 동작
     */
    public AppShell(Page selected, Runnable onHome, Runnable onLevel, Runnable onLogout) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(header(), BorderLayout.NORTH);
        add(sidebar(selected, onHome, onLevel, onLogout), BorderLayout.WEST);
    }

    /**
     * 공통 메뉴 옆 중앙 영역에 화면 본문을 배치한다. 본문을 처음 구성할 때 호출한다.
     * @param content 표시할 화면 본문 컴포넌트
     */
    public void setContent(java.awt.Component content) {
        add(content, BorderLayout.CENTER);
    }

    /** 앱 이름과 임시 학생 요약 정보를 표시하는 헤더를 만든다. */
    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        header.add(UiComponents.label("△  도형탐험대", 23, Font.BOLD, Theme.GREEN), BorderLayout.WEST);
        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 0));
        info.setOpaque(false);
        info.add(UiComponents.label("학습 탐험", 15, Font.BOLD, Theme.GREEN));
        info.add(UiComponents.label("포인트 · 미정", 15, Font.BOLD, Theme.GREEN));
        info.add(UiComponents.label("초록별", 15, Font.BOLD, Theme.TEXT));
        header.add(info, BorderLayout.EAST);
        return header;
    }

    /** 현재 화면을 강조하고 연결된 탐색 및 로그아웃 동작을 제공하는 사이드바를 만든다. */
    private JPanel sidebar(Page selected, Runnable onHome, Runnable onLevel, Runnable onLogout) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Color.WHITE);
        side.setBorder(BorderFactory.createEmptyBorder(24, 20, 24, 20));
        side.setPreferredSize(new Dimension(205, 400));
        addMenu(side, "⌂  나의 탐험", selected == Page.HOME, onHome);
        side.add(Box.createVerticalStrut(3));
        addMenu(side, "◇  레벨 지도", selected == Page.LEVEL, onLevel);
        side.add(Box.createVerticalStrut(3));
        addMenu(side, "☆  랭킹", false, null);
        side.add(Box.createVerticalStrut(3));
        addMenu(side, "○  내 프로필", false, null);
        side.add(Box.createVerticalStrut(40));
        JButton logout = new JButton("로그아웃");
        logout.setFont(Theme.font(Font.PLAIN, 13));
        logout.setForeground(Theme.MUTED);
        logout.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        logout.setBorderPainted(false); logout.setContentAreaFilled(false); logout.setAlignmentX(LEFT_ALIGNMENT);
        logout.addActionListener(event -> onLogout.run());
        side.add(logout);
        return side;
    }

    /** 현재 화면과 미연결 메뉴는 레이블로, 이동 가능한 메뉴는 버튼으로 추가한다. */
    private void addMenu(JPanel side, String text, boolean selected, Runnable action) {
        JPanel row = selected ? new RoundedPanel(new BorderLayout(), Theme.MINT, 24) : new JPanel(new BorderLayout());
        if (!selected) row.setOpaque(false);
        if (action == null || selected) {
            javax.swing.JLabel label = UiComponents.label(text, 16, selected ? Font.BOLD : Font.PLAIN,
                    selected ? Theme.GREEN : Theme.MUTED);
            label.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
            row.add(label);
        } else {
            JButton button = new JButton(text);
            button.setHorizontalAlignment(JButton.LEFT);
            button.setFont(Theme.font(Font.PLAIN, 16)); button.setForeground(Theme.MUTED);
            button.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
            button.setBorderPainted(false); button.setContentAreaFilled(false); button.setFocusPainted(false);
            button.addActionListener(event -> action.run());
            row.add(button);
        }
        row.setPreferredSize(new Dimension(0, 44));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setAlignmentX(LEFT_ALIGNMENT);
        side.add(row);
    }
}
