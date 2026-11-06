package geometrybook.app;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            JFrame frame = new JFrame("도형탐험대");
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            AppNavigation navigation = new AppNavigation(frame);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent event) {
                    navigation.close();
                }
            });
            frame.setMinimumSize(new Dimension(960, 680));
            frame.setSize(1440, 960);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
