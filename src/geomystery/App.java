package geomystery;

import geomystery.data.Content;
import geomystery.data.Profiles;
import geomystery.data.SaveData;
import geomystery.game.Game;
import geomystery.tools.AssetBaker;
import geomystery.ui.MainFrame;
import geomystery.ui.Theme;
import geomystery.ui.Ui;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 기하미궁 (幾何迷宮) — 진입점.
 * 작업 폴더(또는 -Dgeomystery.home)에서 data/, assets/ 를 찾는다.
 */
public class App {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("apple.awt.application.name", "기하미궁");
        Path home = home();
        SwingUtilities.invokeLater(() -> {
            try {
                Theme.loadFonts(home.resolve("assets/fonts"));
                Path img = home.resolve("assets/img");
                if (!AssetBaker.isBaked(img)) AssetBaker.bakeAll(img);
                Ui.initImages(img);
                Content content = Content.load(home.resolve("data"));
                Profiles profiles = Profiles.open(home.resolve("data/save"));
                SaveData save = SaveData.load(profiles.dirOf(profiles.activeId()));
                Theme.apply(save.darkMode, MainFrame.fontScale(save.fontStep));
                new MainFrame(new Game(content, save), profiles).setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "실행 중 오류가 발생했습니다:\n" + e,
                        "기하미궁 (幾何迷宮)", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    static Path home() {
        String p = System.getProperty("geomystery.home");
        if (p != null) return Paths.get(p).toAbsolutePath();
        Path d = Paths.get("").toAbsolutePath();
        for (Path c = d; c != null; c = c.getParent()) {
            if (Files.exists(c.resolve("data/chapters.csv"))) return c;
        }
        return d;
    }
}
