package geomystery.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** 레이아웃·모달·토스트·이미지 도우미. */
public final class Ui {
    private Ui() {}

    // ----------------------------------------------------------- 이미지
    private static Path imgDir;
    private static final Map<String, BufferedImage> cache = new java.util.concurrent.ConcurrentHashMap<>();

    public static void initImages(Path dir) { imgDir = dir; }

    /** 메모리에서 만든 그림으로 바꿔 끼운다(헤이즐 꾸미기). */
    public static void putImage(String name, BufferedImage im) { if (im != null) cache.put(name, im); }

    /** 바꿔 끼운 그림을 버리고 다음에 파일에서 다시 읽게 한다. */
    public static void resetImage(String name) { cache.remove(name); }

    /** 없는 이미지를 표시하는 자리(ConcurrentHashMap 은 null 을 담지 못한다). */
    private static final BufferedImage MISSING = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    /**
     * 이미지를 읽어 캐시한다. 여러 스레드(사건 보드 그리기)에서 불러도 되고, 파일을 읽는 동안
     * 다른 스레드를 막지 않는다(같은 그림을 동시에 두 번 읽는 일은 드물고 해롭지 않다).
     */
    public static BufferedImage img(String name) {
        BufferedImage hit = cache.get(name);
        if (hit != null) return hit == MISSING ? null : hit;
        BufferedImage im = null;
        try {
            Path p = imgDir.resolve(name + ".png");
            if (Files.exists(p)) im = ImageIO.read(p.toFile());
        } catch (Exception e) {
            System.err.println("[ui] 이미지를 읽지 못했습니다: " + name);
        }
        BufferedImage prev = cache.putIfAbsent(name, im == null ? MISSING : im);
        BufferedImage out = prev != null ? prev : (im == null ? MISSING : im);
        return out == MISSING ? null : out;
    }

    // ----------------------------------------------------------- 축소 이미지 캐시
    /** (원본, 화면 픽셀 크기, 좌우반전) → 미리 고품질로 줄여 둔 이미지. 최근 것 160개까지. */
    private static final Map<String, BufferedImage> SCALED = new java.util.LinkedHashMap<String, BufferedImage>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> e) { return size() > 160; }
    };

    /**
     * 이미지를 (x, y, w, h)에 그린다. 매번 고품질로 다시 축소하면 아주 느리므로(레티나에서 배경 한 장에 수백 ms),
     * 실제 화면 픽셀 크기로 한 번 줄여 둔 사본을 캐시해 1:1로 복사한다. 회전된 좌표계에서는 그냥 그린다.
     */
    public static void drawScaled(Graphics2D g, java.awt.Image src, double x, double y, double w, double h, boolean mirror) {
        if (src == null || w <= 0 || h <= 0) return;
        java.awt.geom.AffineTransform t = g.getTransform();
        if (!(src instanceof BufferedImage)) {
            if (mirror) g.drawImage(src, (int) Math.round(x + w), (int) Math.round(y), (int) -Math.round(w), (int) Math.round(h), null);
            else g.drawImage(src, (int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h), null);
            return;
        }
        if (t.getShearX() != 0 || t.getShearY() != 0) {
            // 회전된 좌표계: 화면 크기로 미리 줄여 둔 사본을 회전만 시켜 그린다(큰 원본을 매번 샘플링하지 않도록)
            double ks = Math.hypot(t.getScaleX(), t.getShearY());
            int rw = Math.max(1, (int) Math.round(w * ks)), rh = Math.max(1, (int) Math.round(h * ks));
            BufferedImage b0 = (BufferedImage) src;
            String rk = System.identityHashCode(b0) + ":" + rw + "x" + rh + (mirror ? "m" : "");
            BufferedImage sc0;
            synchronized (SCALED) { sc0 = SCALED.get(rk); }
            if (sc0 == null) {
                sc0 = resize(b0, rw, rh, mirror);
                synchronized (SCALED) { SCALED.put(rk, sc0); }
            }
            g.drawImage(sc0, (int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h), null);
            return;
        }
        double sx = Math.abs(t.getScaleX()), sy = Math.abs(t.getScaleY());
        int pw = Math.max(1, (int) Math.round(w * sx)), ph = Math.max(1, (int) Math.round(h * sy));
        BufferedImage b = (BufferedImage) src;
        String key = System.identityHashCode(b) + ":" + pw + "x" + ph + (mirror ? "m" : "");
        BufferedImage scaled;
        synchronized (SCALED) { scaled = SCALED.get(key); } // 사건 보드는 백그라운드에서도 그린다
        if (scaled == null) {
            scaled = resize(b, pw, ph, mirror);
            synchronized (SCALED) { SCALED.put(key, scaled); }
        }
        java.awt.geom.Point2D dev = t.transform(new java.awt.geom.Point2D.Double(x, y), null);
        Graphics2D d = (Graphics2D) g.create();
        d.setTransform(new java.awt.geom.AffineTransform());
        d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        d.drawImage(scaled, (int) Math.round(dev.getX()), (int) Math.round(dev.getY()), null);
        d.dispose();
    }

    /** 화면 배율(레티나는 2)을 반영한 크기의 빈 캐시 이미지. */
    public static BufferedImage deviceImage(Graphics2D g, int w, int h) {
        double s = Math.max(1, Math.abs(g.getTransform().getScaleX()));
        // 미리 곱한 투명도 형식: 화면(OpenGL)으로 복사할 때 변환 없이 바로 올라간다(일반 ARGB는 매번 임시 변환)
        return new BufferedImage(Math.max(1, (int) Math.ceil(w * s)), Math.max(1, (int) Math.ceil(h * s)), BufferedImage.TYPE_INT_ARGB_PRE);
    }

    /**
     * deviceImage 로 만든 캐시(논리 크기 lw×lh)를 (x, y)에 그린다. 지금 화면 배율로 만든 캐시면 픽셀 1:1로
     * 복사하고(빠름), 다른 배율로 만든 것이면(창을 다른 모니터로 옮긴 직후 등) 크기를 맞춰 그린다.
     */
    public static void blit(Graphics2D g, BufferedImage img, double x, double y, double lw, double lh) {
        java.awt.geom.AffineTransform t = g.getTransform();
        int expect = (int) Math.ceil(lw * Math.abs(t.getScaleX()));
        if (t.getShearX() != 0 || t.getShearY() != 0 || Math.abs(img.getWidth() - expect) > 1) {
            g.drawImage(img, (int) Math.round(x), (int) Math.round(y), (int) Math.round(lw), (int) Math.round(lh), null);
            return;
        }
        java.awt.geom.Point2D dev = t.transform(new java.awt.geom.Point2D.Double(x, y), null);
        Graphics2D d = (Graphics2D) g.create();
        d.setTransform(new java.awt.geom.AffineTransform());
        d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        d.drawImage(img, (int) Math.round(dev.getX()), (int) Math.round(dev.getY()), null);
        d.dispose();
    }

    /** 캐시를 그릴 때 쓸 Graphics: 캐시 이미지 위에 논리 좌표로 그리도록 배율을 맞춘다. */
    public static Graphics2D cacheGraphics(BufferedImage img, Graphics2D like) {
        double s = Math.max(1, Math.abs(like.getTransform().getScaleX()));
        Graphics2D g = Theme.aa(img.createGraphics());
        g.scale(s, s);
        return g;
    }

    /**
     * 화면이 아니라 이미지에 그리는 중인지(화면 전환 연출이 1배 사진을 찍을 때 등).
     * 이때는 화면 배율에 맞춘 캐시를 새로 만들지 말고 있는 것을 쓴다(1배 캐시로 바꿔치기하면 다시 2배로 그려야 한다).
     */
    public static boolean offscreen(Graphics2D g) {
        try {
            return g.getDeviceConfiguration().getDevice().getType() == GraphicsDevice.TYPE_IMAGE_BUFFER;
        } catch (Exception e) {
            return false;
        }
    }

    public static double deviceScale(Graphics2D g) { return Math.max(1, Math.abs(g.getTransform().getScaleX())); }

    /** drawScaled 가 쓸 축소본을 미리 만들어 둔다(백그라운드 스레드에서 불러도 된다). */
    public static void prescale(BufferedImage b, double w, double h, double deviceScale, boolean mirror) {
        if (b == null || w <= 0 || h <= 0) return;
        int pw = Math.max(1, (int) Math.round(w * deviceScale)), ph = Math.max(1, (int) Math.round(h * deviceScale));
        String key = System.identityHashCode(b) + ":" + pw + "x" + ph + (mirror ? "m" : "");
        synchronized (SCALED) { if (SCALED.containsKey(key)) return; }
        BufferedImage scaled = resize(b, pw, ph, mirror);
        synchronized (SCALED) { SCALED.put(key, scaled); }
    }

    public static void drawScaled(Graphics2D g, java.awt.Image src, double x, double y, double w, double h) {
        drawScaled(g, src, x, y, w, h, false);
    }

    /** 반씩 여러 번 줄여 계단 없이 고품질로 축소한다(한 번만 하므로 느려도 괜찮다). */
    public static BufferedImage resize(BufferedImage src, int w, int h, boolean mirror) {
        BufferedImage cur = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) {
            cw /= 2; ch /= 2;
            BufferedImage half = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_ARGB);
            Graphics2D hg = half.createGraphics();
            hg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            hg.drawImage(cur, 0, 0, cw, ch, null);
            hg.dispose();
            cur = half;
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D og = out.createGraphics();
        // 마지막 단계는 bilinear(bicubic 보다 약 3배 빠르고, 반씩 줄인 뒤라 화질 차이가 거의 없다)
        og.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        og.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (mirror) og.drawImage(cur, w, 0, -w, h, null);
        else og.drawImage(cur, 0, 0, w, h, null);
        og.dispose();
        return out;
    }

    // ----------------------------------------------------------- 레이아웃
    /** 커스텀 페인팅과 같은 렌더링 힌트로 글자를 그리는 라벨. */
    public static class AALabel extends JLabel {
        public AALabel(String text) { super(text); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.aa(g2);
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            String t = getText();
            if (getIcon() == null && t != null && !t.startsWith("<html") && !Theme.canDraw(getFont(), t)) {
                // 글꼴에 없는 기호(π 등)가 있으면 직접 그린다
                if (isOpaque()) { g2.setColor(getBackground()); g2.fillRect(0, 0, getWidth(), getHeight()); }
                g2.setFont(getFont());
                g2.setColor(isEnabled() ? getForeground() : Theme.alpha(getForeground(), 0.4f));
                FontMetrics fm = g2.getFontMetrics();
                Insets in = getInsets();
                int tw = Theme.width(fm, t), aw = getWidth() - in.left - in.right;
                int ha = getHorizontalAlignment();
                int x = ha == SwingConstants.CENTER ? in.left + (aw - tw) / 2 : (ha == SwingConstants.RIGHT || ha == SwingConstants.TRAILING) ? getWidth() - in.right - tw : in.left;
                int y = in.top + (getHeight() - in.top - in.bottom + fm.getAscent() - fm.getDescent()) / 2;
                Theme.drawString(g2, t, x, y);
            } else {
                super.paintComponent(g2);
            }
            g2.dispose();
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            String t = getText();
            if (!isPreferredSizeSet() && getIcon() == null && t != null && !t.startsWith("<html") && !Theme.canDraw(getFont(), t)) {
                FontMetrics fm = getFontMetrics(getFont());
                d.width = Math.max(d.width, Theme.width(fm, t) + getInsets().left + getInsets().right);
            }
            return d;
        }
    }

    public static JLabel label(String text, Font f, Color c) {
        JLabel l = new AALabel(text);
        if (f.getAttributes().get(java.awt.font.TextAttribute.TRACKING) != null) {
            java.util.Map<java.awt.font.TextAttribute, Object> m = new java.util.HashMap<>();
            m.put(java.awt.font.TextAttribute.TRACKING, 0f);
            f = f.deriveFont(m);
        }
        l.setFont(f);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        l.setForeground(c);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    public static JPanel clear(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    public static JPanel vbox() {
        JPanel p = clear(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    public static JPanel hbox() {
        JPanel p = clear(null);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        return p;
    }

    public static Component vgap(int h) { return Box.createRigidArea(new Dimension(0, h)); }
    public static Component hgap(int w) { return Box.createRigidArea(new Dimension(w, 0)); }

    public static <T extends JComponent> T left(T c) { c.setAlignmentX(Component.LEFT_ALIGNMENT); return c; }

    /**
     * 스크롤 영역. 내용을 불투명한 받침(OpaqueView)에 올려, Swing 이 스크롤할 때 이미 그린 화면을
     * 밀어서 복사(blit)하고 새로 드러난 띠만 그리게 한다. 투명한 채로 두면 스크롤 한 칸마다
     * 보이는 영역 전체(와 뒤 배경)를 처음부터 다시 그려 느려진다.
     */
    public static JScrollPane scroll(JComponent c) {
        OpaqueView view = new OpaqueView(c);
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(true);
        sp.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    /** 스크롤 내용의 불투명 받침. 배경색은 화면에 붙을 때 둘러싼 패널의 색을 따른다. */
    static final class OpaqueView extends JPanel implements Scrollable {
        private final JComponent child;

        OpaqueView(JComponent child) {
            super(new BorderLayout());
            this.child = child;
            setOpaque(true);
            add(child, BorderLayout.CENTER);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            Color bg = surroundingColor(getParent() == null ? null : getParent().getParent());
            setBackground(bg);
            if (getParent() instanceof JViewport) getParent().setBackground(bg);
        }

        /** 둘러싼 패널에서 실제로 보이는 바탕색(둥근 패널은 채움색, 불투명 패널은 배경색). */
        private static Color surroundingColor(Container c) {
            for (Container p = c; p != null; p = p.getParent()) {
                if (p instanceof RoundPanel && ((RoundPanel) p).fill() != null) return ((RoundPanel) p).fill();
                if (p instanceof JComponent && p.isOpaque()) return p.getBackground();
                if (p instanceof Window) return p.getBackground();
            }
            return Theme.BG;
        }

        private Scrollable sc() { return child instanceof Scrollable ? (Scrollable) child : null; }

        @Override public Dimension getPreferredScrollableViewportSize() { return sc() != null ? sc().getPreferredScrollableViewportSize() : getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return sc() != null ? sc().getScrollableUnitIncrement(r, o, d) : 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return sc() != null ? sc().getScrollableBlockIncrement(r, o, d) : r.height - 40; }
        /** 스크롤 기능이 없는 내용은 예전처럼(JViewport 기본 동작) 보이는 영역보다 작으면 늘려서 채운다. */
        @Override public boolean getScrollableTracksViewportWidth() {
            if (sc() != null) return sc().getScrollableTracksViewportWidth();
            return getParent() instanceof JViewport && getParent().getWidth() > getPreferredSize().width;
        }

        @Override public boolean getScrollableTracksViewportHeight() {
            if (sc() != null) return sc().getScrollableTracksViewportHeight();
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    /** 테마에 맞춘 얇은 스크롤바. */
    static class ThinScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        private JButton zero() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = Theme.aa(g.create());
            g2.setColor(isThumbRollover() ? Theme.BORDER_DARK : Theme.c(0xC9AF82));
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    /** 스크롤 영역에서 가로 폭을 뷰포트에 맞추는 패널. */
    /** 스크롤 영역의 폭에 맞춰 줄어드는 세로 상자(안의 카드·표가 보이는 폭보다 넓어져 잘리지 않도록). */
    public static TrackWidth vtrack() {
        TrackWidth p = new TrackWidth(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    public static class TrackWidth extends JPanel implements Scrollable {
        public TrackWidth(LayoutManager lm) { super(lm); setOpaque(false); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - 40; }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ----------------------------------------------------------- 한국어 조사
    /** 마지막 글자의 받침 유무에 따라 조사를 고른다. 예) josa("비밀", "을", "를") → "비밀을" */
    public static String josa(String word, String withBatchim, String without) {
        if (word == null || word.isEmpty()) return word;
        char c = word.charAt(word.length() - 1);
        boolean batchim;
        if (c >= 0xAC00 && c <= 0xD7A3) batchim = (c - 0xAC00) % 28 != 0;
        else batchim = Character.isDigit(c) && "013678".indexOf(c) >= 0;
        return word + (batchim ? withBatchim : without);
    }

    public static String time(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    public static String num(int n) { return String.format("%,d", n); }

    // ----------------------------------------------------------- 모달
    public static final class Modal {
        public final JDialog dialog;
        public final JPanel body;
        public final JPanel buttons;

        /** 불투명 창인지. 반투명 창은 둥근 모서리·그림자가 예쁘지만, 내용이 자주 다시 그려지면 깜빡이고 느리다. */
        private final boolean solid;

        private Modal(Component parent, String title, int width, boolean solid) {
            this.solid = solid;
            Window w = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
            dialog = new JDialog(w, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setUndecorated(true);
            if (solid) dialog.setBackground(Theme.PAPER);
            else dialog.setBackground(new Color(0, 0, 0, 0));

            RoundPanel card = new RoundPanel(new BorderLayout(0, 18), Theme.PAPER, Theme.GOLD_DARK, 18)
                    .borderWidth(2f).doubleBorder(Theme.alpha(Theme.GOLD, 0.7f));
            if (!solid) card.shadow(6, new Color(0, 0, 0, 70));
            card.setOpaque(solid);
            card.setBackground(Theme.PAPER);
            card.pad(30, 34, 26, 34);
            JLabel t = label(title, Theme.xbold(23), Theme.INK);
            card.add(t, BorderLayout.NORTH);
            body = vbox();
            card.add(body, BorderLayout.CENTER);
            buttons = clear(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            card.add(buttons, BorderLayout.SOUTH);
            dialog.setContentPane(card);
            card.setPreferredSize(null);
            dialog.getRootPane().registerKeyboardAction(e -> close(),
                    KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
            this.width = width;
        }

        private final int width;

        public Modal text(String s) {
            Widgets.TextBlock tb = new Widgets.TextBlock(s, Theme.serif(16), Theme.INK_SOFT).prefWidth(width - 70);
            body.add(tb);
            return this;
        }

        public Modal add(JComponent c) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(c);
            return this;
        }

        public Modal button(String text, Btn.Style style, Runnable r) {
            Btn b = new Btn(text, style);
            b.addActionListener(e -> {
                close();
                if (r != null) r.run();
            });
            buttons.add(b);
            return this;
        }

        public void close() { dialog.dispose(); }

        public void show() {
            dialog.pack();
            dialog.setSize(width, dialog.getPreferredSize().height);
            dialog.validate();
            dialog.setSize(width, dialog.getPreferredSize().height);
            if (solid) {
                // 불투명 창은 창 모양 자체를 둥글게 잘라 모서리를 맞춘다(지원하지 않는 환경이면 네모난 창)
                try {
                    dialog.setShape(new java.awt.geom.RoundRectangle2D.Double(0, 0, dialog.getWidth(), dialog.getHeight(), 18, 18));
                } catch (UnsupportedOperationException | IllegalComponentStateException ignored) { }
            }
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setVisible(true);
        }
    }

    public static Modal modal(Component parent, String title, int width) {
        return new Modal(parent, title, width, false);
    }

    /** 애니메이션·드래그처럼 내용이 계속 바뀌는 창. 불투명이라 깜빡이지 않고 빠르다. */
    public static Modal solidModal(Component parent, String title, int width) {
        return new Modal(parent, title, width, true);
    }

    /**
     * 이 컴포넌트의 창이 지금 앞에 있는지. 모달 창(정리 탐구 등)이 떠 있으면 false —
     * 뒤쪽 화면의 장식 애니메이션은 이때 멈춰서 앞 창의 드래그를 방해하지 않게 한다.
     */
    public static boolean inFront(Component c) {
        if (c == null || !c.isShowing()) return false;
        Window w = SwingUtilities.getWindowAncestor(c);
        if (w == null) return false;
        // 포커스와 상관없이: 이 창 위에 떠 있는 모달 창이 있으면 '뒤에 있음'
        for (Window owned : w.getOwnedWindows()) {
            if (owned.isShowing() && owned instanceof Dialog && ((Dialog) owned).isModal()) return false;
        }
        return true;
    }

    // ----------------------------------------------------------- 축하
    /**
     * 종이 꽃가루가 쏟아지고 화면 위쪽에 축하 띠가 내려온다(약 3.6초). 클릭을 막지 않는다.
     */
    public static void celebrate(JFrame frame, String title, String sub) {
        if (frame == null || !frame.isShowing()) return;
        JLayeredPane lp = frame.getLayeredPane();
        long t0 = System.currentTimeMillis();
        java.util.Random r = new java.util.Random();
        int n = 150;
        double[][] bits = new double[n][7]; // x, y, vx, vy, rot, vrot, color
        Color[] colors = {Theme.GOLD, new Color(0xC23B2A), new Color(0x2F6B55), new Color(0x3E5F8A), new Color(0xF3E6C4), new Color(0xE08A40)};
        for (double[] b : bits) {
            b[0] = r.nextDouble();
            b[1] = -r.nextDouble() * 0.6;
            b[2] = (r.nextDouble() - 0.5) * 0.08;
            b[3] = 0.25 + r.nextDouble() * 0.35;
            b[4] = r.nextDouble() * Math.PI * 2;
            b[5] = (r.nextDouble() - 0.5) * 12;
            b[6] = r.nextInt(colors.length);
        }
        JComponent c = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                double t = (System.currentTimeMillis() - t0) / 1000.0;
                int w = getWidth(), h = getHeight();
                float fade = t > 3.0 ? (float) Math.max(0, 1 - (t - 3.0) / 0.6) : 1f;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
                for (double[] b : bits) {
                    double x = (b[0] + b[2] * t + Math.sin(t * 3 + b[4]) * 0.01) * w;
                    double y = (b[1] + b[3] * t * 0.55) * h;
                    if (y < -20 || y > h * 0.55) continue;
                    Graphics2D p = (Graphics2D) g.create();
                    p.translate(x, y);
                    p.rotate(b[4] + b[5] * t);
                    p.scale(1, Math.abs(Math.cos(t * 6 + b[4])) * 0.8 + 0.2);
                    p.setColor(colors[(int) b[6]]);
                    p.fillRect(-6, -3, 12, 7);
                    p.dispose();
                }
                // 축하 띠
                double drop = Math.min(1, t / 0.45), ease = 1 - Math.pow(1 - drop, 3);
                double by = -120 + (150 + 120) * ease;
                g.setFont(Theme.xbold(34));
                FontMetrics fm = g.getFontMetrics();
                g.setFont(Theme.serif(16));
                FontMetrics fs = g.getFontMetrics();
                int bw = Math.max(fm.stringWidth(title), fs.stringWidth(sub == null ? "" : sub)) + 120;
                java.awt.geom.RoundRectangle2D band = new java.awt.geom.RoundRectangle2D.Double(w / 2.0 - bw / 2.0, by - 50, bw, 100, 20, 20);
                g.setColor(new Color(0, 0, 0, 60));
                g.translate(0, 5);
                g.fill(band);
                g.translate(0, -5);
                g.setColor(new Color(0x3A2819));
                g.fill(band);
                g.setColor(Theme.GOLD);
                g.setStroke(new BasicStroke(3f));
                g.draw(band);
                Icons.paint(g, "star_fill", w / 2.0 - bw / 2.0 + 22, by - 18, 36, Theme.GOLD);
                Icons.paint(g, "star_fill", w / 2.0 + bw / 2.0 - 58, by - 18, 36, Theme.GOLD);
                g.setFont(Theme.xbold(34));
                g.setColor(new Color(0xF7EEDC));
                Theme.drawCentered(g, title, w / 2.0, by - (sub == null ? 0 : 12));
                if (sub != null) {
                    g.setFont(Theme.serif(16));
                    g.setColor(new Color(0xE9D8B2));
                    Theme.drawCentered(g, sub, w / 2.0, by + 26);
                }
                g.dispose();
            }
        };
        c.setBounds(0, 0, lp.getWidth(), lp.getHeight());
        lp.add(c, JLayeredPane.POPUP_LAYER);
        javax.swing.Timer tm = new javax.swing.Timer(33, null);
        tm.addActionListener(e -> {
            // 꽃가루는 화면 위쪽 절반에서만 보이므로 그 띠만 다시 그린다(아래 화면 전체를 다시 그리지 않도록)
            c.repaint(0, 0, c.getWidth(), (int) (c.getHeight() * 0.55) + 40);
            if (System.currentTimeMillis() - t0 > 3700) {
                tm.stop();
                lp.remove(c);
                lp.repaint();
            }
        });
        tm.start();
        Sfx.play("fanfare");
    }

    // ----------------------------------------------------------- 토스트
    public static void toast(JFrame frame, String message) {
        if (frame == null || message == null) return;
        JLayeredPane lp = frame.getLayeredPane();
        JComponent t = new JComponent() {
            float alpha = 0f;
            {
                setOpaque(false);
                javax.swing.Timer tm = new javax.swing.Timer(16, null);
                final long start = System.currentTimeMillis();
                tm.addActionListener(e -> {
                    long el = System.currentTimeMillis() - start;
                    alpha = el < 250 ? el / 250f : el > 2800 ? Math.max(0, 1 - (el - 2800) / 400f) : 1f;
                    repaint();
                    if (el > 3200) {
                        tm.stop();
                        lp.remove(this);
                        lp.repaint();
                    }
                });
                tm.start();
            }

            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setColor(Theme.DARK);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g.setColor(Theme.GOLD);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
                Icons.paint(g, "token", 16, getHeight() / 2.0 - 11, 22, Theme.CREAM);
                g.setFont(Theme.bold(15));
                g.setColor(Theme.CREAM);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(message, 48, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g.dispose();
            }
        };
        FontMetrics fm = t.getFontMetrics(Theme.bold(15));
        int w = fm.stringWidth(message) + 72, h = 48;
        int existing = 0;
        for (Component c : lp.getComponentsInLayer(JLayeredPane.POPUP_LAYER)) existing++;
        t.setBounds((frame.getRootPane().getWidth() - w) / 2, 96 + existing * 56, w, h);
        lp.add(t, JLayeredPane.POPUP_LAYER);
        lp.repaint();
    }
}
