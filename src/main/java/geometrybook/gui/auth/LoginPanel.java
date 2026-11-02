package geometrybook.gui.auth;

import geometrybook.gui.common.RoundedPanel;
import geometrybook.gui.common.Theme;
import geometrybook.gui.common.UiComponents;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public final class LoginPanel extends JPanel {
    private final Runnable onLogin;

    public LoginPanel(Runnable onLogin) {
        super(new GridBagLayout());
        this.onLogin = onLogin;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.fill = GridBagConstraints.BOTH; c.weighty = 1;
        c.gridx = 0; c.weightx = 1; c.insets = new Insets(0, 0, 0, 24);
        add(createWelcome(), c);
        c.gridx = 1; c.weightx = 1; c.insets = new Insets(0, 24, 0, 0);
        add(createForm(), c);
    }

    private JPanel createWelcome() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.MINT, 32);
        card.setBorder(BorderFactory.createEmptyBorder(42, 42, 42, 42));
        JPanel content = column();
        content.add(UiComponents.label("△  도형탐험대", 25, Font.BOLD, Theme.GREEN));
        content.add(Box.createVerticalStrut(30));
        content.add(UiComponents.label("초등학생을 위한 도형 탐험", 14, Font.BOLD, Theme.GREEN));
        content.add(Box.createVerticalStrut(28));
        content.add(UiComponents.label("도형과 친구가 되는", 34, Font.BOLD, Theme.TEXT));
        content.add(Box.createVerticalStrut(6));
        content.add(UiComponents.label("가장 즐거운 방법", 34, Font.BOLD, Theme.TEXT));
        content.add(Box.createVerticalStrut(26));
        content.add(UiComponents.label("하루 한 걸음씩 새로운 도형을 발견해요.", 17, Font.PLAIN, Theme.MUTED));
        content.add(Box.createVerticalStrut(28));
        CharacterArt art = new CharacterArt();
        art.setAlignmentX(0f);
        art.setMaximumSize(new Dimension(500, 180));
        content.add(art);
        content.add(Box.createVerticalStrut(28));
        content.add(UiComponents.label("배우고 · 기록하고 · 함께 성장해요", 17, Font.BOLD, Theme.GREEN));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createForm() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        JPanel form = column();
        form.setPreferredSize(new Dimension(430, 440));
        form.add(UiComponents.label("반가워, 탐험가!", 32, Font.BOLD, Theme.TEXT));
        form.add(Box.createVerticalStrut(14));
        form.add(UiComponents.label("로그인하고 오늘의 탐험을 시작해요.", 17, Font.PLAIN, Theme.MUTED));
        form.add(Box.createVerticalStrut(30));
        addField(form, "아이디", new JTextField());
        form.add(Box.createVerticalStrut(20));
        addField(form, "비밀번호", new JPasswordField());
        form.add(Box.createVerticalStrut(20));
        JCheckBox remember = new JCheckBox("아이디 기억하기");
        remember.setFont(Theme.font(Font.PLAIN, 15));
        remember.setOpaque(false);
        remember.setEnabled(false);
        remember.setAlignmentX(0f);
        form.add(remember);
        form.add(Box.createVerticalStrut(22));
        JButton login = UiComponents.primaryButton("로그인하고 탐험하기  →");
        login.addActionListener(event -> onLogin.run());
        form.add(login);
        wrapper.add(form);
        return wrapper;
    }

    private static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static void addField(JPanel form, String title, JTextField field) {
        JLabel label = UiComponents.label(title, 16, Font.BOLD, Theme.TEXT);
        label.setLabelFor(field);
        form.add(label);
        form.add(Box.createVerticalStrut(8));
        UiComponents.styleInput(field);
        form.add(field);
    }
}
