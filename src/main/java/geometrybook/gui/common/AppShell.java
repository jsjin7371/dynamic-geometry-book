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

/** 로그인 후 화면에서 공유하는 헤더와 탐색 메뉴. */
public final class AppShell extends JPanel {
    public enum Page { HOME, LEVEL }

    public AppShell(Page selected, Runnable onHome, Runnable onLevel, Runnable onLogout) {
        super(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(header(), BorderLayout.NORTH);
        add(sidebar(selected, onHome, onLevel, onLogout), BorderLayout.WEST);
    }

    /** 공통 메뉴 옆에 각 화면의 본문을 배치한다. */
    public void setContent(java.awt.Component content) {
        add(content, BorderLayout.CENTER);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        header.add(UiComponents.label("△  도형탐험대", 23, Font.BOLD, Theme.GREEN), BorderLayout.WEST);
        JPanel info = new JPanel(new FlowLayout(FlowLayout.RIGHT, 24, 0));
        info.setOpaque(false);
        info.add(UiComponents.label("연속 3일", 15, Font.BOLD, Theme.GREEN));
        info.add(UiComponents.label("150 P", 15, Font.BOLD, Theme.GREEN));
        info.add(UiComponents.label("초록별", 15, Font.BOLD, Theme.TEXT));
        header.add(info, BorderLayout.EAST);
        return header;
    }

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
