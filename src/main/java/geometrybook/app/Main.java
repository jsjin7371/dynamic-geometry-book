package geometrybook.app;

import geometrybook.gui.auth.LoginPanel;
import geometrybook.gui.home.HomePanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            JFrame frame = new JFrame("도형탐험대");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            CardLayout layout = new CardLayout();
            JPanel screens = new JPanel(layout);
            screens.add(new LoginPanel(() -> layout.show(screens, "home")), "login");
            screens.add(new HomePanel(() -> layout.show(screens, "login")), "home");
            frame.setContentPane(screens);
            frame.setMinimumSize(new Dimension(960, 680));
            frame.setSize(1440, 960);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
