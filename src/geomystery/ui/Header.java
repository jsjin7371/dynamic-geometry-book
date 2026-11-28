package geomystery.ui;

import geomystery.game.Game;

import javax.swing.*;
import java.awt.*;

/** 탭 화면 상단: 작은 라벨 + 큰 제목, 오른쪽에 상태 알약(연속·토큰·등불·XP). */
public class Header extends JPanel {
    private final JLabel small, title;
    private final JPanel right;
    private final Widgets.Pill streak, tokens, lanterns, xp;
    private final Game game;

    public Header(Game game) {
        this.game = game;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.c(0xD8C49C)),
                BorderFactory.createEmptyBorder(22, 36, 20, 36)));
        JPanel left = Ui.vbox();
        small = Ui.label("", Theme.tracked(Theme.serif(13), 0.2f), Theme.INK_SOFT);
        title = Ui.label("", Theme.xbold(32), Theme.INK);
        left.add(small);
        left.add(Ui.vgap(2));
        left.add(title);
        add(left, BorderLayout.WEST);

        right = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        streak = new Widgets.Pill("flame", "", Widgets.Pill.Kind.LIGHT);
        tokens = new Widgets.Pill("token", "", Widgets.Pill.Kind.LIGHT);
        lanterns = new Widgets.Pill("lantern", "", Widgets.Pill.Kind.LIGHT);
        xp = new Widgets.Pill(null, "", Widgets.Pill.Kind.RED);
        streak.setToolTipText("연속 학습 일수");
        tokens.setToolTipText("단서 토큰 — 힌트를 열거나 등불을 채울 때 사용");
        xp.setToolTipText("누적 경험치");
        right.add(streak);
        right.add(tokens);
        right.add(lanterns);
        right.add(xp);
        add(right, BorderLayout.EAST);
    }

    public void setTitle(String smallText, String titleText) {
        small.setText(smallText);
        title.setText(titleText);
    }

    /** 오른쪽 알약 대신 다른 컴포넌트를 둔다. */
    public void replaceRight(JComponent c) {
        right.removeAll();
        right.add(c);
    }

    /** 토큰이 날아오는 동안 보여 줄 값(음수면 실제 값). */
    private int tokensShown = -1;

    public void showTokens(int v) {
        tokensShown = v;
        setTokenLabel();
    }

    private void setTokenLabel() {
        tokens.set("token", "단서 토큰 " + (tokensShown >= 0 ? tokensShown : game.save.tokens));
    }

    public void pulseTokens() { tokens.pulse(); }

    /** 토큰 알약의 중심(주어진 컴포넌트 좌표). */
    public Point tokenCenter(Component in) {
        return SwingUtilities.convertPoint(tokens, tokens.getWidth() / 2, tokens.getHeight() / 2, in);
    }

    public void refresh() {
        streak.set("flame", game.streakNow() + "일 연속");
        setTokenLabel();
        int l = game.lanterns();
        lanterns.set("lantern", "등불 " + l);
        long ms = game.msToNextLantern();
        lanterns.setToolTipText(ms > 0 ? "다음 등불까지 약 " + Math.max(1, ms / 60000) + "분" : "등불이 가득 찼습니다");
        xp.set(null, Ui.num(game.save.xp) + " XP");
    }
}
