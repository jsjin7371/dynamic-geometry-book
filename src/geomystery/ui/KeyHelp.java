package geomystery.ui;

import javax.swing.*;
import java.awt.*;

/** 키보드 단축키 안내(F1 또는 ? 키). */
final class KeyHelp {
    private KeyHelp() {}

    private static final String MOD = System.getProperty("os.name", "").toLowerCase().contains("mac") ? "⌘" : "Ctrl+";

    private static final String[][] ROWS = {
            {"어디서나", ""},
            {MOD + "1 ~ " + MOD + "8", "메뉴 이동(지도·상자·협회·도감·분석·사건 보드·사무소·수첩)"},
            {"F1 / ?", "이 단축키 안내"},
            {"탐험 지도", ""},
            {"Enter", "지금 사건 이어서 하기"},
            {"대화", ""},
            {"Space / Enter / →", "다음 대사"},
            {"←", "이전 대사"},
            {"S", "대화 건너뛰기"},
            {"Esc", "지도로 돌아가기"},
            {"수수께끼", ""},
            {"0 ~ 9", "숫자 입력 · 객관식/이유/추리 카드 고르기"},
            {"O / X", "참·거짓 판단"},
            {"x + - / * ( ) ^ .", "식 입력  (p → π,  r → √)"},
            {"Enter", "제출 · 정답 뒤에는 다음 문제"},
            {"Backspace", "한 글자 지우기"},
            {"H", "다음 힌트 열기(단서 토큰 1개)"},
            {"A", "보조선 긋기 / 지우기"},
            {"R", "정답 뒤: 풀이 다시 보기"},
            {"E", "정답 뒤: 정리 탐구"},
            {"Space", "정답 뒤: 다음 문제"},
            {"Esc", "그만두기"},
            {"최종 추리", ""},
            {"1 ~ 9 / ↑ ↓", "증언 고르기"},
            {"← →", "정리 카드 고르기"},
            {"Enter", "증거 제시"},
            {"자백 만화 · 결과", ""},
            {"Space / Enter", "다음 칸 · 계속하기"},
            {"R", "결과 화면: 오답 다시 보기"},
            {"Esc", "만화 닫기 · 엔딩 건너뛰기"},
    };

    static void show(JFrame frame) {
        Ui.Modal m = Ui.modal(frame, "키보드 단축키", 620);
        JPanel grid = Ui.vbox();
        for (String[] r : ROWS) {
            if (r[1].isEmpty()) {
                grid.add(Ui.vgap(8));
                JLabel h = Ui.label(r[0], Theme.xbold(15), Theme.RED);
                grid.add(h);
                grid.add(Ui.vgap(4));
                continue;
            }
            JPanel row = Ui.clear(new BorderLayout(14, 0));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel k = Ui.label(r[0], Theme.bold(14), Theme.INK);
            k.setPreferredSize(new Dimension(170, 24));
            row.add(k, BorderLayout.WEST);
            row.add(Ui.label(r[1], Theme.serif(14), Theme.INK_SOFT), BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            grid.add(row);
        }
        m.add(grid);
        m.button("닫기", Btn.Style.PRIMARY, null);
        m.show();
    }
}
