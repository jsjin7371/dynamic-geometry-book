package geomystery.ui;

import geomystery.data.Model.Badge;
import geomystery.game.Game;
import geomystery.tools.AssetBaker;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Predicate;

/**
 * 헤이즐 꾸미기. 기록으로 해금되는 모자·코트·나비넥타이 색.
 * 고를 때 헤이즐을 한 번만 다시 그려 이미지 캐시("hazel", "hazel_face")에 넣어 두므로,
 * 화면마다 따로 그리지 않고 모든 곳(지도·대화·수첩·결과·엔딩)에 그대로 반영된다.
 */
public final class HazelLook {
    private HazelLook() {}

    public static final class Option {
        public final String name, condition;
        public final Color main, second;
        final Predicate<Game> unlocked;

        Option(String name, Color main, Color second, String condition, Predicate<Game> unlocked) {
            this.name = name;
            this.main = main;
            this.second = second;
            this.condition = condition;
            this.unlocked = unlocked;
        }

        public boolean isUnlocked(Game g) { return unlocked.test(g); }
    }

    private static int badges(Game g) {
        int n = 0;
        for (Badge b : g.content.badges) if (g.hasBadge(b)) n++;
        return n;
    }

    public static final Option[] HATS = {
            new Option("갈색 사냥모자", new Color(0x8C5A2E), null, "처음부터", g -> true),
            new Option("남색 모자", new Color(0x3E4F7A), null, "레벨 5 달성", g -> g.level() >= 5),
            new Option("자주 모자", new Color(0x7A3550), null, "레벨 10 달성", g -> g.level() >= 10),
            new Option("회색 모자", new Color(0x6E6A66), null, "훈장 5개 모으기", g -> badges(g) >= 5),
    };
    public static final Option[] COATS = {
            new Option("초록 코트", new Color(0x2F5E4F), new Color(0x3A6D5D), "처음부터", g -> true),
            new Option("남색 코트", new Color(0x2E4466), new Color(0x3A5480), "레벨 3 달성", g -> g.level() >= 3),
            new Option("버건디 코트", new Color(0x6E2A2E), new Color(0x84363A), "최종 추리로 범인 1명 밝혀내기", g -> !g.save.solvedFinales.isEmpty()),
            new Option("갈색 코트", new Color(0x6A4A30), new Color(0x7E5A3C), "레벨 15 달성", g -> g.level() >= 15),
    };
    public static final Option[] TIES = {
            new Option("빨간 나비넥타이", new Color(0xB33A2A), null, "처음부터", g -> true),
            new Option("금색 나비넥타이", new Color(0xD9B456), null, "정리 카드 10장 모으기", g -> g.cards().size() >= 10),
            new Option("파란 나비넥타이", new Color(0x3E6E9C), null, "3일 연속 학습", g -> g.stat("streak_best") >= 3),
    };

    /** 저장된 선택(모자, 코트, 넥타이 번호). 해금되지 않았거나 잘못된 값이면 기본으로. */
    public static int[] chosen(Game g) {
        int[] out = {0, 0, 0};
        String[] p = g.save.outfit == null ? new String[0] : g.save.outfit.split(",");
        Option[][] all = {HATS, COATS, TIES};
        for (int i = 0; i < 3 && i < p.length; i++) {
            try {
                int v = Integer.parseInt(p[i].trim());
                if (v >= 0 && v < all[i].length && all[i][v].isUnlocked(g)) out[i] = v;
            } catch (NumberFormatException ignored) { }
        }
        return out;
    }

    private static String applied = "0,0,0";

    /** 지금 선택을 헤이즐 그림에 반영한다(같으면 아무것도 하지 않음). */
    public static void apply(Game g) {
        int[] c = chosen(g);
        String key = c[0] + "," + c[1] + "," + c[2];
        if (key.equals(applied)) return;
        applied = key;
        if (key.equals("0,0,0")) {
            Ui.resetImage("hazel");
            Ui.resetImage("hazel_face");
            return;
        }
        AssetBaker.Outfit o = new AssetBaker.Outfit(HATS[c[0]].main, COATS[c[1]].main, COATS[c[1]].second, TIES[c[2]].main);
        BufferedImage[] im = AssetBaker.renderHazel(o);
        Ui.putImage("hazel", im[0]);
        Ui.putImage("hazel_face", im[1]);
    }

    public static void choose(Game g, int slot, int index) {
        int[] c = chosen(g);
        c[slot] = index;
        g.save.outfit = c[0] + "," + c[1] + "," + c[2];
        g.save.save();
        apply(g);
    }
}
