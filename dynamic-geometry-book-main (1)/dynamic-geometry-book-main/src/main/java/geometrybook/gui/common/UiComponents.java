package geometrybook.gui.common;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;

/** 화면에서 반복되는 기본 Swing 컴포넌트의 스타일을 생성·적용한다. */
public final class UiComponents {
    /** 유틸리티 클래스이므로 인스턴스를 만들지 않는다. */
    private UiComponents() { }

    /**
     * 공통 글꼴과 색상을 적용한 왼쪽 정렬 레이블을 만든다.
     * @param text 표시할 텍스트
     * @param size 글꼴 크기
     * @param style {@link Font}의 스타일 상수
     * @param color 글자 색상
     * @return 설정된 레이블
     */
    public static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(style, size));
        label.setForeground(color);
        label.setAlignmentX(0f);
        return label;
    }

    /**
     * 기존 입력 필드에 공통 글꼴, 색상, 테두리와 크기를 적용한다.
     * {@code JPasswordField}에도 사용할 수 있으며 입력값이나 검증 동작은 변경하지 않는다.
     * @param field 스타일을 적용할 필드
     */
    public static void styleInput(JTextField field) {
        field.setFont(Theme.font(Font.PLAIN, 17));
        field.setForeground(Theme.TEXT);
        field.setBackground(Color.WHITE);
        field.setBorder(new CompoundBorder(BorderFactory.createLineBorder(new Color(0xDCE5DF)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        field.setPreferredSize(new Dimension(380, 60));
        field.setAlignmentX(0f);
    }

    /**
     * 공통 스타일의 기본 버튼을 만든다. 동작은 호출자가 리스너를 등록해 연결한다.
     * @param text 버튼에 표시할 텍스트
     * @return 기본 버튼
     */
    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.BOLD, 17));
        button.setBackground(Theme.GREEN);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        button.setFocusPainted(false);
        button.setAlignmentX(0f);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        return button;
    }

    /**
     * 둥근 배경의 기본 버튼을 만든다. 크기와 클릭 동작은 호출 화면에서 지정한다.
     * @param text 버튼에 표시할 텍스트
     * @return 둥근 기본 버튼
     */
    public static JButton roundedPrimaryButton(String text) {
        JButton button = new RoundedButton(text);
        button.setFont(Theme.font(Font.BOLD, 16));
        button.setBackground(Theme.GREEN);
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setFocusPainted(false);
        button.setAlignmentX(0f);
        return button;
    }
}
