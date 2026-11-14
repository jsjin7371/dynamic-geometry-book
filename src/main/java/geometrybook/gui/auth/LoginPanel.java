package geometrybook.gui.auth;

import geometrybook.gui.common.RoundedPanel;
import geometrybook.gui.common.Theme;
import geometrybook.gui.common.UiComponents;
import geometrybook.storage.AuthService;
import geometrybook.storage.Student;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public final class LoginPanel extends JPanel {
    private final Runnable onLogin;
    private final AuthService auth;
    private final JTextField idField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = UiComponents.primaryButton("로그인하고 탐험하기  →");
    private boolean authenticating;

    public LoginPanel(AuthService auth, Runnable onLogin) {
        super(new GridBagLayout());
        this.auth = auth;
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
        JPanel content = UiComponents.column();
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
        JPanel form = UiComponents.column();
        form.setPreferredSize(new Dimension(430, 440));
        form.add(UiComponents.label("반가워, 탐험가!", 32, Font.BOLD, Theme.TEXT));
        form.add(Box.createVerticalStrut(14));
        form.add(UiComponents.label("로그인하고 오늘의 탐험을 시작해요.", 17, Font.PLAIN, Theme.MUTED));
        form.add(Box.createVerticalStrut(30));
        addField(form, "아이디", idField);
        form.add(Box.createVerticalStrut(20));
        addField(form, "비밀번호", passwordField);
        form.add(Box.createVerticalStrut(20));
        JCheckBox remember = new JCheckBox("아이디 기억하기");
        remember.setFont(Theme.font(Font.PLAIN, 15));
        remember.setOpaque(false);
        remember.setEnabled(false);
        remember.setAlignmentX(0f);
        form.add(remember);
        form.add(Box.createVerticalStrut(22));
        loginButton.addActionListener(event -> authenticate());
        passwordField.addActionListener(event -> authenticate());
        form.add(loginButton);
        wrapper.add(form);
        return wrapper;
    }

    private void authenticate() {
        if (authenticating) return;
        String loginId = idField.getText();
        char[] password = passwordField.getPassword();
        passwordField.setText("");
        setAuthenticating(true);
        new SwingWorker<Optional<Student>, Void>() {
            @Override
            protected Optional<Student> doInBackground() {
                try {
                    return auth.login(loginId, new String(password));
                } finally {
                    Arrays.fill(password, '\0');
                }
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    auth.logout();
                    return;
                }
                try {
                    Optional<Student> student = get();
                    if (student.isPresent()) {
                        JOptionPane.showMessageDialog(LoginPanel.this,
                                student.get().getLoginId() + "님, 환영합니다.", "로그인 성공",
                                JOptionPane.INFORMATION_MESSAGE);
                        onLogin.run();
                    } else {
                        JOptionPane.showMessageDialog(LoginPanel.this,
                                "아이디 또는 비밀번호가 틀렸습니다", "로그인 실패",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showAuthenticationError();
                } catch (ExecutionException exception) {
                    showAuthenticationError();
                } finally {
                    setAuthenticating(false);
                }
            }
        }.execute();
    }

    private void setAuthenticating(boolean busy) {
        authenticating = busy;
        loginButton.setEnabled(!busy);
        idField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
        loginButton.setText(busy ? "로그인 중…" : "로그인하고 탐험하기  →");
    }

    private void showAuthenticationError() {
        auth.logout();
        JOptionPane.showMessageDialog(this,
                "계정 정보를 읽지 못했습니다. data 폴더를 확인한 후 다시 시도해 주세요.",
                "로그인 오류", JOptionPane.ERROR_MESSAGE);
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
