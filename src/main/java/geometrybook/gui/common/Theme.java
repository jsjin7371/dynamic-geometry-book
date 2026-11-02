package geometrybook.gui.common;

import java.awt.Color;
import java.awt.Font;
import javax.swing.UIManager;

/** 화면 간 색상과 글꼴을 통일하기 위한 디자인 토큰 모음. Swing 화면에서 공유한다. */
public final class Theme {
    public static final Color BACKGROUND = new Color(0xF5F7F3);
    public static final Color GREEN = new Color(0x28755F);
    public static final Color TEXT = new Color(0x263C3A);
    public static final Color MUTED = new Color(0x718783);
    public static final Color MINT = new Color(0xE3F2E8);
    public static final Color YELLOW = new Color(0xFFEAAE);
    public static final Color LAVENDER = new Color(0xDED5F3);

    /** 인스턴스 없이 상수와 글꼴 생성 메서드만 사용한다. */
    private Theme() { }

    /**
     * Noto Sans KR이 설치되어 있으면 사용하고, 없으면 Swing 기본 글꼴 계열로 대체한다.
     * @param style {@link Font#PLAIN}, {@link Font#BOLD} 등의 글꼴 스타일
     * @param size 글꼴 크기
     * @return 지정한 스타일과 크기의 글꼴
     */
    public static Font font(int style, int size) {
        Font system = UIManager.getFont("Label.font");
        String family = system == null ? Font.SANS_SERIF : system.getFamily();
        for (String available : java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (available.equals("Noto Sans KR")) {
                family = available;
                break;
            }
        }
        return new Font(family, style, size);
    }
}
