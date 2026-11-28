package geomystery.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** 색상 팔레트와 글꼴. design/ 시안의 고서·탐정 수첩 분위기를 따른다. */
public final class Theme {
    private Theme() {}

    // 색상 토큰. apply()가 밝은/어두운 팔레트로 다시 채운다(화면을 새로 만들어야 반영됨).
    public static Color DARK, SIDEBAR, BG, PAPER, PAPER2, BORDER, BORDER_DARK, GOLD, GOLD_DARK,
            RED, RED_DARK, GREEN, GREEN_DARK, INK, INK_SOFT, MUTED, ORANGE, CREAM;

    private static boolean dark = false;
    private static float scale = 1f;

    static { apply(false, 1f); }

    public static boolean isDark() { return dark; }
    public static float scale() { return scale; }

    /** 팔레트와 글자 배율을 정한다. 이미 만든 컴포넌트에는 반영되지 않으므로 호출 뒤 화면을 다시 만든다. */
    public static void apply(boolean darkMode, float fontScale) {
        dark = darkMode;
        scale = Math.max(0.8f, Math.min(1.3f, fontScale));
        if (FONT_CACHE != null) FONT_CACHE.clear();
        GOLD = new Color(0xD9B456);
        GREEN = new Color(0x2F5E50);
        GREEN_DARK = new Color(0x22473D);
        CREAM = new Color(0xF7EEDC);
        if (!dark) {
            DARK = new Color(0x3A2819); SIDEBAR = new Color(0x3B2A1B);
            BG = new Color(0xEFE3C7); PAPER = new Color(0xF7EEDC); PAPER2 = new Color(0xF2E6CB);
            BORDER = new Color(0xCBAE7C); BORDER_DARK = new Color(0x8C6A48);
            GOLD_DARK = new Color(0xB8903A);
            RED = new Color(0xA33A28); RED_DARK = new Color(0x74271A);
            INK = new Color(0x3A2819); INK_SOFT = new Color(0x6E5641); MUTED = new Color(0x9A8266);
            ORANGE = new Color(0xC0661E);
        } else {
            DARK = new Color(0x17110C); SIDEBAR = new Color(0x15100B);
            BG = new Color(0x1F1812); PAPER = new Color(0x2B221A); PAPER2 = new Color(0x32281F);
            BORDER = new Color(0x5C4934); BORDER_DARK = new Color(0x806549);
            GOLD_DARK = new Color(0xC9A04A);
            RED = new Color(0xB9493A); RED_DARK = new Color(0x6E2418);
            INK = new Color(0xEDE0C4); INK_SOFT = new Color(0xC2AE8C); MUTED = new Color(0x8E7B62);
            ORANGE = new Color(0xE08A40);
        }
        UIManager.put("ToolTip.background", PAPER);
        UIManager.put("ToolTip.foreground", INK);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(BORDER));
    }

    /**
     * 화면 곳곳의 고정 색(종이·테두리 계열)을 현재 팔레트에 맞춘다.
     * 밝은 모드에서는 그대로, 어두운 모드에서는 밝기를 뒤집어 어두운 종이색으로 바꾼다(어두운 색은 유지).
     */
    public static Color c(int rgb) {
        Color base = new Color(rgb);
        if (!dark) return base;
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        float luma = (0.299f * base.getRed() + 0.587f * base.getGreen() + 0.114f * base.getBlue()) / 255f;
        if (luma < 0.45f) return base;
        float b = 0.13f + (1f - luma) * 0.62f;
        return Color.getHSBColor(hsb[0], Math.min(hsb[1], 0.45f), Math.min(0.62f, b));
    }

    private static Font REG = new Font("Serif", Font.PLAIN, 14);
    private static Font BOLD = new Font("Serif", Font.BOLD, 14);
    private static Font XBOLD = BOLD;
    private static Font HAND = REG;

    public static void loadFonts(Path dir) {
        REG = load(dir.resolve("NanumMyeongjo-Regular.ttf"), REG);
        BOLD = load(dir.resolve("NanumMyeongjo-Bold.ttf"), BOLD);
        XBOLD = load(dir.resolve("NanumMyeongjo-ExtraBold.ttf"), BOLD);
        HAND = load(dir.resolve("NanumPenScript-Regular.ttf"), REG);
        FONT_CACHE.clear();
        Font base = serif(14);
        for (String k : new String[]{"Label.font", "Button.font", "TextField.font", "ToolTip.font",
                "OptionPane.messageFont", "OptionPane.buttonFont", "TextArea.font"}) {
            UIManager.put(k, base);
        }
    }

    private static Font load(Path p, Font fallback) {
        try (InputStream in = Files.newInputStream(p)) {
            Font f = Font.createFont(Font.TRUETYPE_FONT, in);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
            return f;
        } catch (Exception e) {
            System.err.println("[theme] 글꼴을 불러오지 못했습니다: " + p + " (" + e.getMessage() + ")");
            return fallback;
        }
    }

    /** 크기별 글꼴 캐시(그릴 때마다 deriveFont 하면 칠판처럼 글자가 많은 화면이 느려진다). */
    private static final Map<Long, Font> FONT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static Font sized(int family, Font base, float size) {
        float px = size * scale;
        long key = ((long) family << 32) | Float.floatToIntBits(px);
        return FONT_CACHE.computeIfAbsent(key, k -> base.deriveFont(Font.PLAIN, px));
    }

    public static Font serif(float size) { return sized(0, REG, size); }
    public static Font bold(float size) { return sized(1, BOLD, size); }
    public static Font xbold(float size) { return sized(2, XBOLD, size); }
    public static Font hand(float size) { return sized(3, HAND, size); }

    /**
     * 한자용 글꼴. 함께 넣은 나눔 글꼴에는 한자가 없으므로 시스템의 명조 계열 한자 글꼴을 찾아 쓴다
     * (없으면 자바 기본 Serif — 운영체제가 알아서 한자 글꼴로 대신 그린다).
     */
    public static Font hanja(float size) {
        if (HANJA == null) {
            HANJA = new Font(Font.SERIF, Font.PLAIN, 14);
            for (String n : new String[]{"AppleMyungjo", "Batang", "바탕", "Noto Serif CJK KR", "Source Han Serif K", "Songti SC", "SimSun", "MS Mincho", "Hiragino Mincho ProN"}) {
                Font f = new Font(n, Font.PLAIN, 14);
                if (f.getFamily().equals(n) && f.canDisplayUpTo("幾何迷宮") == -1) { HANJA = f; break; }
            }
        }
        return HANJA.deriveFont(size);
    }
    private static Font HANJA;

    /** 자간을 넓힌 글꼴 (CHAPTER II 같은 작은 영문 라벨용). */
    public static Font tracked(Font f, float tracking) {
        Map<TextAttribute, Object> m = new HashMap<>();
        m.put(TextAttribute.TRACKING, tracking);
        return f.deriveFont(m);
    }

    public static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g2;
    }

    public static Color alpha(Color c, float a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, Math.round(a * 255))));
    }

    public static void drawCentered(Graphics2D g, String s, double cx, double cy) {
        FontMetrics fm = g.getFontMetrics();
        drawString(g, s, (float) (cx - width(fm, s) / 2.0), (float) (cy + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    // ------------------------------------------------------------------ 없는 글자 대신 그리기
    /** 함께 넣은 나눔 글꼴에는 π, −(빼기) 같은 기호가, 손글씨 글꼴에는 √ ² ∠ 등이 없다. 그 글자만 이 시스템 글꼴로 그린다. */
    private static Font SYMBOL;
    private static final Map<Long, Font> SYMBOL_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final String PROBE = "π−√²∠⊥∥½→×°";

    private static Font symbolBase() {
        if (SYMBOL == null) {
            Font pick = new Font(Font.SERIF, Font.PLAIN, 14);
            // 서양 세리프 글꼴을 먼저: 한중일 글꼴은 π 같은 그리스 문자를 전각(넓게)으로 그려 간격이 벌어진다
            for (String n : new String[]{"Palatino", "Georgia", "Times New Roman", "Baskerville", "Charter", "Cambria", "STIX Two Text",
                    "DejaVu Serif", "AppleMyungjo", "Batang", "바탕", "Malgun Gothic", "Noto Serif CJK KR", "Arial Unicode MS"}) {
                Font f = new Font(n, Font.PLAIN, 14);
                if (f.getFamily().equals(n) && f.canDisplayUpTo(PROBE) == -1) { pick = f; break; }
            }
            SYMBOL = pick;
        }
        return SYMBOL;
    }

    /** 시작할 때 백그라운드에서 기호 글꼴과 글자 그림을 미리 준비한다(처음 π를 그릴 때 멈추지 않도록). */
    public static void warmUp() {
        Ui.background("geomystery-fonts", Thread.MIN_PRIORITY, () -> {
            java.awt.image.BufferedImage im = new java.awt.image.BufferedImage(64, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = aa(im.createGraphics());
            for (Font f : new Font[]{serif(15), bold(15), xbold(22), hand(26), hand(96), xbold(42)}) {
                g.setFont(f);
                drawString(g, "6π − √2 ∠A ½", 0, 20);
                outline("6π cm²", f, g.getFontRenderContext());
            }
            g.dispose();
        });
    }

    /** 이 글꼴 대신 쓸 기호 글꼴(같은 크기·굵기). */
    public static Font fallback(Font f) {
        String n = f.getFontName();
        boolean bold = n.contains("Bold") || n.contains("ExtraBold") || f.isBold();
        long key = ((long) (bold ? 1 : 0) << 32) | Float.floatToIntBits(f.getSize2D());
        return SYMBOL_CACHE.computeIfAbsent(key, k -> symbolBase().deriveFont(bold ? Font.BOLD : Font.PLAIN, f.getSize2D()));
    }

    /** 글꼴이 이 글자를 모두 그릴 수 있으면 true(대부분은 이 빠른 길로 끝난다). */
    public static boolean canDraw(Font f, String s) { return s == null || f.canDisplayUpTo(s) == -1; }

    /** 글꼴별·글자별 표시 가능 여부(0 모름, 1 가능, 2 불가). canDisplay 를 글자마다 부르면 느리다. */
    private static final Map<Font, byte[]> GLYPHS = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean has(Font f, char ch) {
        byte[] m = GLYPHS.computeIfAbsent(f, k -> new byte[65536]);
        byte v = m[ch];
        if (v == 0) { v = (byte) (f.canDisplay(ch) ? 1 : 2); m[ch] = v; }
        return v == 1;
    }

    /** 없는 글자는 기호 글꼴로 바꿔 가며 그린다. */
    public static void drawString(Graphics2D g, String s, float x, float y) {
        if (s == null || s.isEmpty()) return;
        Font f = g.getFont();
        if (canDraw(f, s)) { g.drawString(s, x, y); return; }
        Font fb = fallback(f);
        int i = 0, n = s.length();
        float cx = x;
        while (i < n) {
            boolean ok = has(f, s.charAt(i));
            int j = i + 1;
            while (j < n && has(f, s.charAt(j)) == ok) j++;
            String run = s.substring(i, j);
            Font use = ok ? f : fb;
            g.setFont(use);
            g.drawString(run, cx, y);
            cx += (float) use.getStringBounds(run, g.getFontRenderContext()).getWidth();
            i = j;
        }
        g.setFont(f);
    }

    /** drawString 과 맞는 글자 폭. */
    public static int width(FontMetrics fm, String s) {
        if (s == null || s.isEmpty()) return 0;
        Font f = fm.getFont();
        if (canDraw(f, s)) return fm.stringWidth(s);
        Font fb = fallback(f);
        java.awt.font.FontRenderContext frc = fm.getFontRenderContext();
        double w = 0;
        int i = 0, n = s.length();
        while (i < n) {
            boolean ok = has(f, s.charAt(i));
            int j = i + 1;
            while (j < n && has(f, s.charAt(j)) == ok) j++;
            String run = s.substring(i, j);
            w += ok ? fm.stringWidth(run) : fb.getStringBounds(run, frc).getWidth();
            i = j;
        }
        return (int) Math.round(w);
    }

    /** 글자 윤곽(없는 글자는 기호 글꼴로). 기준선 왼쪽이 (0,0). */
    public static Shape outline(String s, Font f, java.awt.font.FontRenderContext frc) {
        java.awt.geom.Path2D out = new java.awt.geom.Path2D.Double();
        Font fb = canDraw(f, s) ? f : fallback(f);
        double x = 0;
        int i = 0, n = s.length();
        while (i < n) {
            boolean ok = has(f, s.charAt(i));
            int j = i + 1;
            while (j < n && has(f, s.charAt(j)) == ok) j++;
            java.awt.font.TextLayout tl = new java.awt.font.TextLayout(s.substring(i, j), ok ? f : fb, frc);
            out.append(tl.getOutline(java.awt.geom.AffineTransform.getTranslateInstance(x, 0)), false);
            x += tl.getAdvance();
            i = j;
        }
        return out;
    }
}
