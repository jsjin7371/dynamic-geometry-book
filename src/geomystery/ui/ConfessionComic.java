package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.diagram.DiagramView;
import geomystery.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 범인 자백 만화: 최종 추리에서 범인을 밝히면 만화 세 칸이 차례로 나온다.
 * <ol>
 *   <li>동기 — 범인이 탐내던 것(생각 풍선)</li>
 *   <li>수법 — 범인이 실제로 범행을 저지르는 장면(삽질하는 까마귀, 톱니를 억지로 끼우는 고양이 …)</li>
 *   <li>체포 — 달아나던 범인에게 헤이즐이 던진 수갑이 '철컥' 채워지고, 의뢰인이 앞을 막아선다</li>
 * </ol>
 * <b>버벅거림 없이</b>: 칸마다 움직이지 않는 바탕(배경·소품·가만히 있는 인물)은 열 때 백그라운드에서
 * 한 장으로 그려 두고, 매 프레임에는 그 그림을 1:1로 복사한 뒤 움직이는 인물(미리 줄여 둔 그림을 옮기기만)과
 * 작은 도형 몇 개만 그린다. 다시 그리는 곳도 지금 보이는 칸뿐이고 초당 30번이다.
 * 뒤 화면은 처음에 한 장 찍어 두고(또는 넘겨받고) 어둡게 깔아서 실제 화면을 다시 그리지 않는다.
 */
final class ConfessionComic {
    private ConfessionComic() {}

    private static final long FADE = 260, POP = 300;
    /** 칸마다 저절로 넘어가기까지(동기, 수법). 체포 칸은 클릭할 때까지 남는다. */
    private static final long[] AUTO = {2400, 3600};

    static void play(JFrame frame, Game game, Lesson lesson, Runnable done) { play(frame, game, lesson, null, done); }

    /** backdrop: 이미 찍어 둔 뒤 화면(있으면 다시 찍지 않는다). */
    static void play(JFrame frame, Game game, Lesson lesson, BufferedImage backdrop, Runnable done) {
        List<Confession> cfs = game.content.confessions.get(lesson == null ? "" : lesson.id);
        String culprit = lesson == null ? null : game.content.culpritOf(lesson);
        JRootPane root = frame.getRootPane();
        if (cfs == null || cfs.isEmpty() || culprit == null || root == null || !frame.isShowing()) {
            if (done != null) done.run();
            return;
        }
        JLayeredPane lp = root.getLayeredPane();
        int w = lp.getWidth(), h = lp.getHeight();
        BufferedImage back = backdrop;
        if (back == null) {
            back = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = back.createGraphics();
            root.getContentPane().paint(bg);
            bg.dispose();
        }
        final BufferedImage backImg = back;

        int pw = Math.min(1180, w - 80), ph = Math.min(600, h - 110);
        int px = (w - pw) / 2, py = (h - ph) / 2 - 10;
        List<Shape> shapes = layout(pw, ph);
        double sc = 1;
        try { sc = Math.max(1, frame.getGraphicsConfiguration().getDefaultTransform().getScaleX()); } catch (Exception ignored) { }
        final double scale = sc;
        Npc cn = game.content.npc(culprit);
        Chapter ch = game.content.chapterOf(lesson);
        Npc client = ch == null ? null : game.content.npc(ch.npc);
        Cast cast = new Cast(cn == null ? culprit : cn.image, cn == null ? culprit : cn.name,
                client == null ? null : client.image, ch == null ? "garden" : ch.scene);

        int n = Math.min(cfs.size(), shapes.size());
        Panel[] panels = new Panel[n];
        for (int i = 0; i < n; i++) {
            Shape s = shapes.get(i);
            Rectangle2D b = s.getBounds2D();
            Shape local = AffineTransform.getTranslateInstance(-b.getX(), -b.getY()).createTransformedShape(s);
            panels[i] = Panel.of(game, cfs.get(i), cast, local, new Rectangle2D.Double(px + b.getX(), py + b.getY(), b.getWidth(), b.getHeight()));
        }
        // 칸 바탕은 백그라운드에서(첫 칸부터 차례로) 그린다
        Thread painter = new Thread(() -> {
            for (Panel p : panels) p.build(scale);
        }, "geomystery-comic");
        painter.setDaemon(true);
        painter.start();

        long t0 = System.currentTimeMillis();
        int[] shown = {0};
        boolean[] closing = {false}, finished = {false};
        long[] closeAt = {0};
        panels[0].shownAt = t0 + FADE;

        BufferedImage[] backDev = new BufferedImage[2];
        JComponent overlay = new JComponent() {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = Theme.aa(g0.create());
                long now = System.currentTimeMillis();
                float in = (float) Math.min(1, (now - t0) / (double) FADE);
                if (closing[0]) in *= (float) Math.max(0, 1 - (now - closeAt[0]) / (double) FADE);
                if (backDev[0] == null && Math.abs(backImg.getWidth() - Math.ceil(getWidth() * Ui.deviceScale(g))) <= 1) backDev[0] = backImg;
                if (backDev[0] == null) {
                    backDev[0] = Ui.deviceImage(g, getWidth(), getHeight());
                    Graphics2D b = Ui.cacheGraphics(backDev[0], g);
                    // 어둡게 깔리는 배경이라 단순 확대(가장 빠른 방식)면 충분하다
                    b.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    b.drawImage(backImg, 0, 0, getWidth(), getHeight(), null);
                    b.dispose();
                }
                if (backDev[1] == null) {
                    backDev[1] = Ui.deviceImage(g, getWidth(), getHeight());
                    Graphics2D d = Ui.cacheGraphics(backDev[1], g);
                    d.drawImage(backDev[0], 0, 0, getWidth(), getHeight(), null);
                    d.setColor(new Color(20, 12, 6, 215));
                    d.fillRect(0, 0, getWidth(), getHeight());
                    d.dispose();
                }
                if (in >= 1f) {
                    Ui.blit(g, backDev[1], 0, 0, getWidth(), getHeight());
                } else {
                    Ui.blit(g, backDev[0], 0, 0, getWidth(), getHeight());
                    g.setColor(new Color(20, 12, 6, Math.round(215 * in)));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                Composite keep = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, in));
                // 만화 원고지
                g.setColor(new Color(0xF7EEDC));
                g.fill(new RoundRectangle2D.Double(px - 16, py - 16, pw + 32, ph + 32, 10, 10));
                g.setColor(new Color(0x3A2819));
                g.setStroke(new BasicStroke(3f));
                g.draw(new RoundRectangle2D.Double(px - 16, py - 16, pw + 32, ph + 32, 10, 10));
                for (int i = 0; i < panels.length; i++) {
                    Panel p = panels[i];
                    Shape s = AffineTransform.getTranslateInstance(p.at.getX(), p.at.getY()).createTransformedShape(p.shape);
                    if (i > shown[0] || p.shownAt == 0 || now < p.shownAt || p.base == null) {
                        // 아직 나오지 않은 칸: 빈 칸 테두리만
                        g.setColor(new Color(0xE6DCC8));
                        g.fill(s);
                        g.setColor(new Color(0xBDAE90));
                        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{8, 6}, 0));
                        g.draw(s);
                        continue;
                    }
                    double k = Math.min(1, (now - p.shownAt) / (double) POP);
                    double e = 1 - Math.pow(1 - k, 3);
                    // 크기를 바꾸지 않고 살짝 미끄러져 들어온다(1:1 복사라 빠르다)
                    double dy = Math.round((1 - e) * 26 - (k < 1 ? Math.sin(Math.PI * k) * 4 : 0));
                    // 지금 칸만 움직이고, 지나간 칸은 넘어간 순간의 모습으로 멈춘다
                    double t = (i == shown[0] && !closing[0] ? now - p.shownAt : p.frozenAt) / 1000.0;
                    Graphics2D c = (Graphics2D) g.create();
                    c.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, in * (float) Math.min(1, k * 1.8)));
                    c.translate(p.at.getX(), p.at.getY() + dy);
                    Ui.blit(c, p.base, 0, 0, p.at.getWidth(), p.at.getHeight());
                    Graphics2D live = (Graphics2D) c.create();
                    live.clip(p.liveClip);
                    p.drawLive(live, t);
                    live.dispose();
                    if (p.caption != null) Ui.blit(c, p.caption, p.capBox.getX(), p.capBox.getY(), p.capBox.getWidth(), p.capBox.getHeight());
                    c.setColor(new Color(0x2A1D12));
                    c.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    c.draw(p.shape);
                    c.dispose();
                }
                g.setFont(Theme.serif(14));
                g.setColor(new Color(0xE9DCC0));
                String hint = shown[0] >= panels.length - 1 ? "클릭하면 계속합니다" : "클릭하면 다음 칸";
                Theme.drawCentered(g, cast.culpritName + "의 자백  ·  " + hint, getWidth() / 2.0, py + ph + 40);
                g.setComposite(keep);
                g.dispose();
            }
        };
        overlay.setOpaque(true);
        overlay.setBounds(0, 0, w, h);
        lp.add(overlay, Integer.valueOf(JLayeredPane.DRAG_LAYER + 10));

        javax.swing.Timer tm = new javax.swing.Timer(33, null);
        Runnable finish = () -> {
            if (finished[0]) return;
            finished[0] = true;
            tm.stop();
            lp.remove(overlay);
            lp.repaint();
            if (done != null) done.run();
        };
        Runnable advance = () -> {
            if (closing[0]) return;
            long now = System.currentTimeMillis();
            Panel cur = panels[shown[0]];
            if (shown[0] < panels.length - 1) {
                // 지금 칸이 다 나오기 전에 누르면 그 칸부터 마무리
                if (now < cur.shownAt + POP) { cur.shownAt = now - POP; return; }
                cur.frozenAt = now - cur.shownAt;
                shown[0]++;
                panels[shown[0]].shownAt = now;
                panels[shown[0]].sounded = false;
                Sfx.play("page");
                overlay.repaint(px - 24, py - 24, pw + 48, ph + 80);
            } else {
                cur.frozenAt = now - cur.shownAt;
                closing[0] = true;
                closeAt[0] = now;
            }
        };
        overlay.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { advance.run(); }
        });
        // 키보드: 스페이스·엔터·오른쪽 화살표로 넘긴다
        InputMap im = overlay.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (String k : new String[]{"SPACE", "ENTER", "RIGHT"}) im.put(KeyStroke.getKeyStroke(k), "comicNext");
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "comicClose");
        overlay.getActionMap().put("comicNext", new AbstractAction() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { advance.run(); } });
        overlay.getActionMap().put("comicClose", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (closing[0]) return;
                Panel cur = panels[shown[0]];
                cur.frozenAt = System.currentTimeMillis() - cur.shownAt;
                closing[0] = true;
                closeAt[0] = System.currentTimeMillis();
            }
        });
        Sfx.play("page");
        tm.addActionListener(e -> {
            long now = System.currentTimeMillis();
            if (closing[0] && now - closeAt[0] > FADE) { finish.run(); return; }
            Panel cur = panels[shown[0]];
            if (!closing[0] && cur.base != null && shown[0] < AUTO.length && shown[0] < panels.length - 1
                    && now - cur.shownAt > AUTO[shown[0]] + POP) advance.run();
            cur = panels[shown[0]];
            // 장면 속 소리(철컥 등)
            if (!closing[0] && cur.base != null && now >= cur.shownAt) cur.sound((now - cur.shownAt) / 1000.0);
            boolean fading = now - t0 < FADE + 50 || closing[0];
            if (fading) overlay.repaint();
            else {
                // 지금 칸만(움직이는 동안) 다시 그린다
                Rectangle2D a = cur.at;
                overlay.repaint((int) a.getX() - 4, (int) a.getY() - 6, (int) a.getWidth() + 8, (int) a.getHeight() + 40);
            }
        });
        tm.start();
        overlay.repaint();
    }

    /** 칸 배치: 위 두 칸(비스듬한 칸막이) + 아래 한 칸. 원고지 좌표. */
    private static List<Shape> layout(int pw, int ph) {
        List<Shape> out = new ArrayList<>();
        double gap = 14, topH = ph * 0.55, split = pw * 0.42, slant = 26;
        out.add(poly(0, 0, split + slant, 0, split - slant, topH, 0, topH));
        out.add(poly(split + slant + gap, 0, pw, 0, pw, topH - 18, split - slant + gap, topH));
        out.add(poly(0, topH + gap, pw, topH + gap - 18, pw, ph, 0, ph));
        return out;
    }

    private static Path2D poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }

    // ================================================================== 등장인물
    private static final class Cast {
        final String culprit, culpritName, client, scene;

        Cast(String culprit, String culpritName, String client, String scene) {
            this.culprit = culprit;
            this.culpritName = culpritName;
            this.client = client;
            this.scene = scene;
        }
    }

    /** 굽기 좌표(360×480)에서 두 손의 위치. */
    private static final double HAND_LX = 107, HAND_RX = 253, HAND_Y = 398;

    // ================================================================== 칸
    /**
     * 한 칸. base(움직이지 않는 부분)는 build()에서 한 번 그리고, drawLive()는 매 프레임 움직이는 것만 그린다.
     * 좌표는 칸의 왼쪽 위가 (0,0)인 논리 좌표.
     */
    private abstract static class Panel {
        final Game game;
        final Confession cf;
        final Cast cast;
        final Shape shape;
        final Rectangle2D at;
        final double w, h;
        volatile BufferedImage base, caption;
        Rectangle2D capBox;
        /** 움직이는 것을 그릴 때 쓰는 사각형 잘라내기(비스듬한 칸막이 안쪽). 다각형으로 잘라내면 느리다. */
        final Shape liveClip;
        long shownAt, frozenAt;
        boolean sounded;

        Panel(Game game, Confession cf, Cast cast, Shape shape, Rectangle2D at) {
            this.game = game;
            this.cf = cf;
            this.cast = cast;
            this.shape = shape;
            this.at = at;
            this.w = at.getWidth();
            this.h = at.getHeight();
            Rectangle2D sb = shape.getBounds2D();
            // 비스듬한 변을 피해 안쪽 사각형(인물은 이 안에서 움직인다)
            this.liveClip = new Rectangle2D.Double(sb.getX() + 54, sb.getY() + 20, sb.getWidth() - 56, sb.getHeight() - 22);
        }

        static Panel of(Game game, Confession cf, Cast cast, Shape shape, Rectangle2D at) {
            if (Confession.METHOD.equals(cf.kind)) {
                switch (cf.prop == null ? "" : cf.prop) {
                    case "channel": return new Channel(game, cf, cast, shape, at);
                    case "gear": return new GearJam(game, cf, cast, shape, at);
                    case "rope": return new BellRope(game, cf, cast, shape, at);
                    case "disc": return new DiscThief(game, cf, cast, shape, at);
                    default: return new DiagramMethod(game, cf, cast, shape, at);
                }
            }
            if (Confession.ARREST.equals(cf.kind)) return new Arrest(game, cf, cast, shape, at);
            return new Motive(game, cf, cast, shape, at);
        }

        /** 바탕과 해설 상자를 그린다(백그라운드 스레드). */
        void build(double sc) {
            prescale(sc);
            BufferedImage im = new BufferedImage((int) Math.ceil(w * sc), (int) Math.ceil(h * sc), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = Theme.aa(im.createGraphics());
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.scale(sc, sc);
            g.clip(shape);
            paintBase(g);
            g.dispose();
            buildCaption(sc);
            base = im;
        }

        /** 움직이는 인물 그림을 미리 화면 크기로 줄여 둔다(첫 프레임에 멈추지 않도록). */
        void prescale(double sc) {}

        abstract void paintBase(Graphics2D g);

        void drawLive(Graphics2D g, double t) {}

        /** 장면 속 효과음(한 번만). */
        void sound(double t) {}

        String tag() { return "동기"; }

        double captionX() { return 16; }

        private void buildCaption(double sc) {
            if (cf.caption == null || cf.caption.isEmpty()) return;
            BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D pg = probe.createGraphics();
            Font f = Theme.bold(15f);
            FontMetrics fm = pg.getFontMetrics(f);
            pg.dispose();
            int maxW = (int) Math.min(w - captionX() - 120, w * 0.85);
            List<String> lines = Widgets.TextBlock.wrap(cf.caption, fm, maxW - 24);
            int bw = 0;
            for (String l : lines) bw = Math.max(bw, Theme.width(fm, l));
            bw += 24;
            int bh = lines.size() * (fm.getHeight() + 2) + 14;
            capBox = new Rectangle2D.Double(Math.round(captionX()) - 2, 12, bw + 8, bh + 8);
            BufferedImage im = new BufferedImage((int) Math.ceil(capBox.getWidth() * sc), (int) Math.ceil(capBox.getHeight() * sc), BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = Theme.aa(im.createGraphics());
            g.scale(sc, sc);
            g.translate(2, 2);
            g.setColor(new Color(0, 0, 0, 60));
            g.fill(new Rectangle2D.Double(3, 4, bw, bh));
            g.setColor(new Color(0xFFF6D8));
            g.fill(new Rectangle2D.Double(0, 0, bw, bh));
            g.setColor(new Color(0x2A1D12));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new Rectangle2D.Double(0, 0, bw, bh));
            g.setFont(f);
            int ty = 7 + fm.getAscent();
            for (String l : lines) {
                Theme.drawString(g, l, 12, ty);
                ty += fm.getHeight() + 2;
            }
            g.dispose();
            caption = im;
        }

        /** 칸 오른쪽 위 종류 딱지(바탕에 그린다). */
        void tagLabel(Graphics2D g) {
            String tag = tag();
            g.setFont(Theme.xbold(13f));
            FontMetrics tm = g.getFontMetrics();
            double tw = tm.stringWidth(tag) + 18;
            double tx = w - tw - 40, ty = 14;
            g.setColor(new Color(0xC23B2A));
            g.fill(new RoundRectangle2D.Double(tx, ty, tw, 24, 12, 12));
            g.setColor(Color.WHITE);
            g.drawString(tag, (float) (tx + 9), (float) (ty + 17));
        }

        // ---- 공통 그리기
        /**
         * 장면 배경(bg_장면, 1600×640)을 칸에 꽉 차게 깔고 색을 덮는다.
         * 돌려주는 값 {x, y, 배율}: 배경 좌표 (bx, by) → 칸 좌표 (x + bx·배율, y + by·배율).
         */
        double[] sceneBg(Graphics2D g, String scene, Color tint, double focusX) {
            BufferedImage bg = Ui.img("bg_" + scene);
            g.setColor(new Color(0x3A2819));
            g.fillRect(0, 0, (int) w + 1, (int) h + 1);
            double s = Math.max(w / 1600.0, h / 640.0) * 1.15;
            double iw = 1600 * s, ih = 640 * s;
            double x = Math.max(w - iw, Math.min(0, w / 2 - focusX * s));
            if (bg != null) Ui.drawScaled(g, bg, x, h - ih, iw, ih);
            if (tint != null) {
                g.setColor(tint);
                g.fillRect(0, 0, (int) w + 1, (int) h + 1);
            }
            return new double[]{x, h - ih, s};
        }

        /** 인물 전신(아래 끝이 bottom, 키 height). 옮기기만 하므로 미리 줄여 둔 그림을 그대로 복사한다. */
        static void figure(Graphics2D g, String id, double cx, double bottom, double height, boolean mirror) {
            BufferedImage img = Ui.img(id);
            if (img == null) return;
            double ww = Math.round(height * img.getWidth() / img.getHeight()), hh = Math.round(height);
            Ui.drawScaled(g, img, Math.round(cx - ww / 2), Math.round(bottom - hh), ww, hh, mirror);
        }

        static void prescaleFigure(String id, double height, double sc, boolean mirror) {
            BufferedImage img = Ui.img(id);
            if (img == null) return;
            Ui.prescale(img, Math.round(height * img.getWidth() / img.getHeight()), Math.round(height), sc, mirror);
        }

        /** 인물 그림 속 한 점(굽기 좌표)의 화면 위치. */
        static Point2D.Double at(String id, double cx, double bottom, double height, boolean mirror, double bx, double by) {
            BufferedImage img = Ui.img(id);
            double ww = img == null ? height * 0.75 : Math.round(height * img.getWidth() / img.getHeight());
            double hh = Math.round(height);
            double left = Math.round(cx - ww / 2), top = Math.round(bottom - hh);
            double x = mirror ? 360 - bx : bx;
            return new Point2D.Double(left + x / 360.0 * ww, top + by / 480.0 * hh);
        }

        /** 손글씨 효과음 글자(테두리 있는). */
        static void sfx(Graphics2D g, String s, double x, double y, float size, Color fill, double rot) {
            Graphics2D t = (Graphics2D) g.create();
            t.translate(x, y);
            t.rotate(Math.toRadians(rot));
            t.setFont(Theme.hand(size));
            t.setColor(new Color(0x2A1D12));
            for (int dx = -2; dx <= 2; dx += 2) for (int dy = -2; dy <= 2; dy += 2) if (dx != 0 || dy != 0) Theme.drawString(t, s, dx, dy);
            t.setColor(fill);
            Theme.drawString(t, s, 0, 0);
            t.dispose();
        }

        /** 칠판 분필 같은 작은 숫자 표시. */
        static void chalkTag(Graphics2D g, String s, double x, double y, Color c) {
            g.setFont(Theme.bold(16f));
            FontMetrics fm = g.getFontMetrics();
            double tw = Theme.width(fm, s) + 14;
            g.setColor(new Color(0x24, 0x4A, 0x3F, 220));
            g.fill(new RoundRectangle2D.Double(x - tw / 2, y - 13, tw, 26, 8, 8));
            g.setColor(c);
            Theme.drawCentered(g, s, x, y);
        }

        static Path2D gearShape(double r, int teeth, double tooth) {
            Path2D p = new Path2D.Double();
            int n = teeth * 4;
            for (int i = 0; i < n; i++) {
                double a = 2 * Math.PI * i / n;
                double rr = (i % 4 == 0 || i % 4 == 1) ? r + tooth : r;
                if (i == 0) p.moveTo(rr * Math.cos(a), rr * Math.sin(a)); else p.lineTo(rr * Math.cos(a), rr * Math.sin(a));
            }
            p.closePath();
            return p;
        }
    }

    // ------------------------------------------------------------------ 1. 동기
    private static final class Motive extends Panel {
        Motive(Game g, Confession c, Cast k, Shape s, Rectangle2D a) { super(g, c, k, s, a); }

        @Override void paintBase(Graphics2D g) {
            g.setPaint(new RadialGradientPaint(new Point2D.Double(w * 0.7, h * 0.4), (float) Math.max(w, h),
                    new float[]{0f, 1f}, new Color[]{new Color(0xFBE2A0), new Color(0xE9A85A)}));
            g.fillRect(0, 0, (int) w + 1, (int) h + 1);
            halftone(g, w, h, new Color(196, 120, 50, 70), w * 0.7, h * 0.4);
            figure(g, cast.culprit, w * 0.28, h + h * 0.35, h * 1.25, false);
            double bx = w * 0.68, by = h * 0.46, br = Math.min(w * 0.26, h * 0.32);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(bx - br * 1.2, by - br, br * 2.4, br * 2));
            g.fill(new Ellipse2D.Double(w * 0.42, h * 0.62, 22, 18));
            g.fill(new Ellipse2D.Double(w * 0.38, h * 0.7, 12, 10));
            g.setColor(new Color(0x3A2819));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Ellipse2D.Double(bx - br * 1.2, by - br, br * 2.4, br * 2));
            g.draw(new Ellipse2D.Double(w * 0.42, h * 0.62, 22, 18));
            g.draw(new Ellipse2D.Double(w * 0.38, h * 0.7, 12, 10));
            prop(g, cf.prop, bx, by + 4, br * 0.75);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + 0.6;
                g.setColor(new Color(0xE6BE55));
                g.fill(Icons.star(bx + Math.cos(a) * br * 1.05, by + Math.sin(a) * br * 0.8, 9, 3.5));
            }
            tagLabel(g);
        }
    }

    private static void halftone(Graphics2D g, double w, double h, Color c, double cx, double cy) {
        g.setColor(c);
        double maxD = Math.hypot(w, h);
        for (int y = 6; y < h; y += 12) {
            for (int x = (y / 12) % 2 == 0 ? 6 : 12; x < w; x += 12) {
                double d = Math.hypot(x - cx, y - cy) / maxD;
                double r = 1.2 + 3.2 * d;
                g.fill(new Ellipse2D.Double(x - r / 2, y - r / 2, r, r));
            }
        }
    }

    // ------------------------------------------------------------------ 2. 수법(장면별)
    private abstract static class Method extends Panel {
        Method(Game g, Confession c, Cast k, Shape s, Rectangle2D a) { super(g, c, k, s, a); }

        @Override String tag() { return "수법"; }

        /** 왼쪽 칸막이가 비스듬해서 해설 상자는 조금 안쪽에서 시작한다. */
        @Override double captionX() { return 66; }

        /** 범인의 키(칸 높이에 맞춘 전신). */
        double bodyH() { return Math.round(h * 0.74); }

        @Override void prescale(double sc) {
            prescaleFigure(cast.culprit, bodyH(), sc, false);
            prescaleFigure(cast.culprit, bodyH(), sc, true);
        }
    }

    /** 까마귀: 분수대에서 담장까지 수선 대신 비스듬한 물길을 판다. 물이 그 길로 흘러 등불이 꺼져 간다. */
    private static final class Channel extends Method {
        Channel(Game g, Confession c, Cast k, Shape s, Rectangle2D a) { super(g, c, k, s, a); }

        /** 배경 그림 속 분수대(굽기 좌표 800, 285)의 칸 위치. 바탕을 그릴 때 정해진다. */
        private volatile double fxv = -1, fyv = -1;

        double fx() { return fxv < 0 ? w * 0.3 : fxv; }
        double fy() { return fyv < 0 ? h * 0.4 : fyv; }
        double ex() { return w * 0.66; }
        double ey() { return h * 0.86; }

        @Override void paintBase(Graphics2D g) {
            double[] m = sceneBg(g, "garden", new Color(18, 26, 60, 150), 1300);
            fxv = m[0] + 800 * m[2];
            fyv = m[1] + 285 * m[2];
            // 담장(직선 l)
            g.setColor(new Color(0x6B4A2E));
            g.fill(new Rectangle2D.Double(0, h * 0.86, w, h * 0.14));
            g.setColor(new Color(0x4A3020));
            g.setStroke(new BasicStroke(3f));
            for (double x = 10; x < w; x += 34) g.draw(new Line2D.Double(x, h * 0.86, x, h));
            // 원래 내야 했던 가장 짧은 길(수선): 흐린 점선과 직각 표시
            g.setColor(new Color(255, 255, 255, 110));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{8, 8}, 0));
            g.draw(new Line2D.Double(fx(), fy() + 14, fx(), h * 0.86));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Rectangle2D.Double(fx(), h * 0.86 - 14, 14, 14));
            chalkTag(g, "수선", fx() - 34, (fy() + h * 0.86) / 2, new Color(0xF4ECD8));
            // 비스듬히 판 물길(흙)
            g.setColor(new Color(0x4A3020));
            g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(fx() + 40, fy() + 8, ex(), ey()));
            // 등불길(꺼져 가는 등불 기둥)
            for (int i = 0; i < 3; i++) {
                double x = w * (0.5 + i * 0.14);
                g.setColor(new Color(0x3A2819));
                g.fill(new Rectangle2D.Double(x - 3, h * 0.22, 6, h * 0.3));
            }
            tagLabel(g);
        }

        @Override void drawLive(Graphics2D g, double t) {
            // 등불: 물이 흐를수록 하나씩 꺼진다
            for (int i = 0; i < 3; i++) {
                double x = w * (0.5 + i * 0.14);
                double life = Math.max(0, 1 - Math.max(0, t - 0.6 - i * 0.6) / 0.8);
                float flick = (float) (0.6 + 0.4 * Math.sin(t * 13 + i * 2));
                g.setColor(new Color(1f, 0.82f, 0.45f, (float) (0.35 * life * flick)));
                g.fill(new Ellipse2D.Double(x - 22, h * 0.2 - 22, 44, 44));
                g.setColor(life > 0.05 ? new Color(0xFBE39A) : new Color(0x5A5040));
                g.fill(new RoundRectangle2D.Double(x - 9, h * 0.2 - 12, 18, 24, 6, 6));
                g.setColor(new Color(0x2A1D12));
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Double(x - 9, h * 0.2 - 12, 18, 24, 6, 6));
            }
            // 비스듬한 물길로 흐르는 물(줄무늬가 흘러간다)
            double flow = Math.min(1, t / 0.9);
            double sx = fx() + 40, sy = fy() + 8;
            double tx = sx + (ex() - sx) * flow, ty = sy + (ey() - sy) * flow;
            g.setColor(new Color(0x6FA3C9));
            g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(sx, sy, tx, ty));
            g.setColor(new Color(220, 240, 255, 200));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{10, 16}, (float) (26 - (t * 60) % 26)));
            g.draw(new Line2D.Double(sx, sy, tx, ty));
            // 삽질하는 까마귀(물길 끝)
            double bob = Math.abs(Math.sin(t * 6)) * 10;
            double cx = ex() + w * 0.13, bottom = h * 0.98;
            figure(g, cast.culprit, cx, bottom - bob, bodyH(), true);
            // 삽: 손에서 땅으로(파는 동작)
            Point2D.Double hand = at(cast.culprit, cx, bottom - bob, bodyH(), true, HAND_RX, HAND_Y);
            double dig = Math.sin(t * 6) * 0.35;
            double tipX = hand.x - 70 * Math.cos(0.9 + dig), tipY = hand.y + 50 * Math.sin(0.9 + dig);
            g.setColor(new Color(0x8C6A3A));
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(hand.x, hand.y, tipX, tipY));
            g.setColor(new Color(0x9A9CA4));
            g.fill(new Ellipse2D.Double(tipX - 12, tipY - 8, 24, 18));
            // 튀는 흙
            for (int i = 0; i < 4; i++) {
                double ph = (t * 1.8 + i * 0.25) % 1.0;
                double dx = -30 - i * 12, dy = -60 * Math.sin(Math.PI * ph);
                g.setColor(new Color(0x6B4A2E));
                g.fill(new Ellipse2D.Double(tipX + dx * ph - 4, tipY + dy - 4, 8, 7));
            }
            sfx(g, "철벅 철벅", (fx() + ex()) / 2 - 40, h * 0.62, 30, new Color(0xA9D0EA), -8);
        }
    }

    /** 고양이: 대각이 100°, 100°인 비뚤어진 톱니를 원에 억지로 밀어 넣는다. 불꽃이 튀고 톱니가 덜컹거린다. */
    private static final class GearJam extends Method {
        GearJam(Game g, Confession c, Cast k, Shape s, Rectangle2D a) { super(g, c, k, s, a); }

        double gx() { return w * 0.66; }
        double gy() { return h * 0.58; }
        double gr() { return h * 0.28; }

        @Override void paintBase(Graphics2D g) {
            sceneBg(g, "clock", new Color(30, 18, 8, 120), 800);
            // 시계 기관의 원(톱니가 걸려야 할 자리)
            g.setColor(new Color(0x5A3A22));
            g.fill(new Ellipse2D.Double(gx() - gr() - 14, gy() - gr() - 14, (gr() + 14) * 2, (gr() + 14) * 2));
            g.setColor(new Color(0xF4ECD8));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{8, 6}, 0));
            g.draw(new Ellipse2D.Double(gx() - gr(), gy() - gr(), gr() * 2, gr() * 2));
            chalkTag(g, "100° + 100° ?", gx() + gr() * 0.2, gy() + gr() + 26, new Color(0xE0664C));
            tagLabel(g);
        }

        @Override void drawLive(Graphics2D g, double t) {
            // 옆에서 도는 작은 톱니 두 개(정상)
            double[][] gs = {{gx() - gr() - 34, gy() + gr() * 0.6, 30, 1}, {gx() + gr() + 26, gy() - gr() * 0.3, 24, -1.3}};
            for (double[] q : gs) {
                Graphics2D o = (Graphics2D) g.create();
                o.translate(q[0], q[1]);
                o.rotate(t * q[3]);
                o.setColor(new Color(0xB8903A));
                o.fill(gearShape(q[2], 9, 8));
                o.setColor(new Color(0x5A3A22));
                o.fill(new Ellipse2D.Double(-q[2] * 0.3, -q[2] * 0.3, q[2] * 0.6, q[2] * 0.6));
                o.dispose();
            }
            // 억지로 끼우는 비뚤어진 사각형 톱니: 덜컹덜컹
            double push = Math.max(0, Math.sin(t * 5));
            double jx = (Math.sin(t * 37) * 3) * push, jy = (Math.cos(t * 41) * 3) * push;
            Graphics2D q = (Graphics2D) g.create();
            q.translate(gx() + jx - (1 - push) * 14, gy() + jy);
            q.rotate(Math.toRadians(8 + Math.sin(t * 5) * 4));
            double r = gr();
            Path2D quad = poly(-r * 0.95, -r * 0.2, -r * 0.15, -r * 0.98, r * 0.92, -r * 0.05, r * 0.05, r * 1.05);
            q.setColor(new Color(0xC9A456));
            q.fill(quad);
            q.setColor(new Color(0x3A2819));
            q.setStroke(new BasicStroke(3f));
            q.draw(quad);
            q.setColor(new Color(0xE0664C));
            q.setStroke(new BasicStroke(2.5f));
            q.draw(new Arc2D.Double(-r * 0.95 - 22, -r * 0.2 - 22, 44, 44, -60, 80, Arc2D.OPEN));
            q.draw(new Arc2D.Double(r * 0.92 - 22, -r * 0.05 - 22, 44, 44, 120, 80, Arc2D.OPEN));
            q.dispose();
            // 미는 고양이
            double cx = w * 0.39 + push * 14, bottom = h * 1.0;
            figure(g, cast.culprit, cx, bottom, bodyH(), false);
            // 불꽃(닿는 곳)
            double sx = gx() - gr() * 0.95 + jx - 6, sy = gy() - gr() * 0.1;
            if (push > 0.4) {
                java.util.Random rnd = new java.util.Random((long) (t * 15));
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 7; i++) {
                    double a = rnd.nextDouble() * Math.PI * 2, len = 8 + rnd.nextDouble() * 18;
                    g.setColor(i % 2 == 0 ? new Color(0xFFE08A) : new Color(0xF2A23A));
                    g.draw(new Line2D.Double(sx + Math.cos(a) * 6, sy + Math.sin(a) * 6, sx + Math.cos(a) * len, sy + Math.sin(a) * len));
                }
            }
            sfx(g, "끼익!", gx() - gr() * 0.3 + Math.sin(t * 30) * 2 * push, h * 0.36, 34, new Color(0xFFE08A), -10);
        }
    }

    /** 박쥐: 날갯짓하며 남은 밧줄로 종을 칭칭 감는다. */
    private static final class BellRope extends Method {
        BellRope(Game g, Confession c, Cast k, Shape s, Rectangle2D a) { super(g, c, k, s, a); }

        double bx() { return w * 0.68; }

        @Override double bodyH() { return Math.round(h * 0.66); }

        @Override void paintBase(Graphics2D g) {
            sceneBg(g, "monastery", new Color(24, 18, 36, 140), 800);
            // 종탑 들보
            g.setColor(new Color(0x5A3A22));
            g.fill(new Rectangle2D.Double(w * 0.3, h * 0.08, w * 0.7, 22));
            g.setColor(new Color(0x3A2416));
            g.fill(new Rectangle2D.Double(bx() - 6, h * 0.08 + 22, 12, h * 0.12));
            chalkTag(g, "빗변 17m ?", w * 0.86, h * 0.86, new Color(0xE0664C));
            tagLabel(g);
        }

        @Override void drawLive(Graphics2D g, double t) {
            // 살짝 흔들리는 종
            double sw = Math.sin(t * 2.2) * 0.05;
            Graphics2D b = (Graphics2D) g.create();
            b.translate(bx(), h * 0.2 + 22);
            b.rotate(sw);
            double r = h * 0.2;
            Path2D bell = new Path2D.Double();
            bell.moveTo(-r * 0.45, r * 0.25);
            bell.curveTo(-r * 0.5, r * 1.2, -r * 0.9, r * 1.25, -r * 1.0, r * 1.45);
            bell.lineTo(r * 1.0, r * 1.45);
            bell.curveTo(r * 0.9, r * 1.25, r * 0.5, r * 1.2, r * 0.45, r * 0.25);
            bell.curveTo(r * 0.3, -r * 0.05, -r * 0.3, -r * 0.05, -r * 0.45, r * 0.25);
            b.setColor(new Color(0xD9B456));
            b.fill(bell);
            b.setColor(new Color(0x3A2819));
            b.setStroke(new BasicStroke(3f));
            b.draw(bell);
            // 감기는 밧줄: 시간이 갈수록 감긴 줄이 늘어난다
            int wraps = (int) Math.min(5, t * 2.2);
            b.setColor(new Color(0xC9A26C));
            b.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < wraps; i++) {
                double y = r * (0.5 + i * 0.18);
                double half = r * (0.42 + i * 0.1);
                b.draw(new Line2D.Double(-half, y, half, y + r * 0.08));
            }
            b.dispose();
            // 날갯짓하며 감는 박쥐(종 둘레를 오간다)
            double orbit = Math.sin(t * 2.2);
            double cx = bx() - w * 0.25 + orbit * w * 0.04, lift = Math.abs(Math.sin(t * 9)) * 8;
            double bottom = h * 0.96 - lift;
            figure(g, cast.culprit, cx, bottom, bodyH(), false);
            // 손에서 종으로 이어진 밧줄
            Point2D.Double hand = at(cast.culprit, cx, bottom, bodyH(), false, HAND_RX, HAND_Y);
            double wy = h * 0.2 + 22 + r * (0.5 + Math.max(0, wraps - 1) * 0.18);
            g.setColor(new Color(0xC9A26C));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new QuadCurve2D.Double(hand.x, hand.y, (hand.x + bx()) / 2, wy + 40, bx() - r * 0.5, wy));
            sfx(g, "칭칭…", bx() + r * 0.7, h * 0.36, 30, new Color(0xF2DFAE), 8);
            sfx(g, "쉿", cx - 80, h * 0.46, 26, new Color(0xF2DFAE), -12);
        }
    }

    /** 수달: 별자리 원판에서 뜯어낸 부채꼴 조각을 이고 살금살금 빠져나간다. */
    private static final class DiscThief extends Method {
        DiscThief(Game g, Confession c, Cast k, Shape s, Rectangle2D a) { super(g, c, k, s, a); }

        double dx() { return w * 0.3; }
        double dy() { return h * 0.56; }
        double dr() { return h * 0.3; }

        @Override void paintBase(Graphics2D g) {
            sceneBg(g, "observatory", new Color(10, 12, 40, 110), 800);
            // 별자리 원판(부채꼴 한 조각이 비어 있다)
            Arc2D whole = new Arc2D.Double(dx() - dr(), dy() - dr(), dr() * 2, dr() * 2, 110, 310, Arc2D.PIE);
            g.setColor(new Color(0x2E3A66));
            g.fill(whole);
            g.setColor(new Color(0xC9A456));
            g.setStroke(new BasicStroke(3f));
            g.draw(whole);
            java.util.Random r = new java.util.Random(9);
            for (int i = 0; i < 16; i++) {
                double a = Math.toRadians(110 + r.nextDouble() * 300), rr = dr() * (0.2 + r.nextDouble() * 0.75);
                g.setColor(new Color(0xF2DFAE));
                g.fill(Icons.star(dx() + Math.cos(a) * rr, dy() - Math.sin(a) * rr, 5, 2));
            }
            g.setColor(new Color(255, 255, 255, 50));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{6, 6}, 0));
            g.draw(new Arc2D.Double(dx() - dr(), dy() - dr(), dr() * 2, dr() * 2, 50, 60, Arc2D.PIE));
            // 받침대에 걸린 두 쇠줄(속인 길이)
            double pX = w * 0.58, pY = dy();
            g.setColor(new Color(0x9A9CA4));
            g.setStroke(new BasicStroke(3f));
            g.draw(new Line2D.Double(pX, pY, dx() + dr() * 0.42, dy() - dr() * 0.9));
            g.draw(new Line2D.Double(pX, pY, dx() + dr() * 0.42, dy() + dr() * 0.9));
            chalkTag(g, "7m", (pX + dx() + dr() * 0.42) / 2 + 18, dy() - dr() * 0.62, new Color(0xF4ECD8));
            chalkTag(g, "9m ?", (pX + dx() + dr() * 0.42) / 2 + 18, dy() + dr() * 0.62, new Color(0xE0664C));
            tagLabel(g);
        }

        @Override void drawLive(Graphics2D g, double t) {
            // 살금살금 오른쪽으로(칸 끝에 닿으면 다시)
            double span = w * 0.26;
            double prog = (t * 40) % span;
            double cx = w * 0.66 + prog, lift = Math.abs(Math.sin(t * 7)) * 6;
            double bottom = h * 0.99 - lift;
            // 발자국
            g.setColor(new Color(255, 255, 255, 60));
            for (int i = 1; i <= 4; i++) {
                double fx = cx - i * 22;
                if (fx < w * 0.62) break;
                g.fill(new Ellipse2D.Double(fx - 5, h * 0.95 + (i % 2) * 6, 10, 6));
            }
            figure(g, cast.culprit, cx, bottom, bodyH(), false);
            // 머리 위로 인 부채꼴 조각(금박)
            Point2D.Double head = at(cast.culprit, cx, bottom, bodyH(), false, 180, 110);
            Graphics2D p = (Graphics2D) g.create();
            p.translate(head.x, head.y);
            p.rotate(Math.toRadians(-6 + Math.sin(t * 7) * 4));
            double r = h * 0.22;
            Arc2D piece = new Arc2D.Double(-r, -r * 0.9, r * 2, r * 2, 55, 70, Arc2D.PIE);
            p.setColor(new Color(0xE6BE55));
            p.fill(piece);
            p.setColor(new Color(0x3A2819));
            p.setStroke(new BasicStroke(2.5f));
            p.draw(piece);
            p.dispose();
            sfx(g, "살금살금", cx - 70, h * 0.24, 28, new Color(0xF2DFAE), -6);
        }
    }

    /** 장면이 정해지지 않은 수법 칸: 도형(칠판)과 범인. */
    private static final class DiagramMethod extends Method {
        DiagramMethod(Game g, Confession c, Cast k, Shape s, Rectangle2D a) { super(g, c, k, s, a); }

        @Override void paintBase(Graphics2D g) {
            g.setPaint(new GradientPaint(0, 0, new Color(0x356A5B), 0, (float) h, new Color(0x244A40)));
            g.fillRect(0, 0, (int) w + 1, (int) h + 1);
            List<Cmd> script = game.content.diagrams.get(cf.prop);
            if (script != null) {
                double s = Math.min(w * 0.70 / DiagramView.W, (h - 60) / DiagramView.H);
                Graphics2D d = (Graphics2D) g.create();
                d.translate(w * 0.28 + (w * 0.70 - DiagramView.W * s) / 2, (h - DiagramView.H * s) / 2 + 14);
                d.scale(s, s);
                try { DiagramView.render(d, script, 1f); } catch (RuntimeException ignored) { }
                d.dispose();
            }
            figure(g, cast.culprit, w * 0.18, h * 1.0, bodyH(), false);
            tagLabel(g);
        }
    }

    // ------------------------------------------------------------------ 3. 체포
    /**
     * 체포: 달아나던 범인이 멈칫하고, 헤이즐이 던진 수갑이 날아가 두 손목에 '철컥' 채워진다.
     * 의뢰인이 앞을 막아서고, 범인은 움찔하며 땀을 흘린다. 마지막에 '체포 완료' 도장.
     */
    private static final class Arrest extends Panel {
        Arrest(Game g, Confession c, Cast k, Shape s, Rectangle2D a) { super(g, c, k, s, a); }

        static final double RUN = 0.45, FLY0 = 0.35, FLY1 = 0.8, STAMP = 1.25;

        @Override String tag() { return "체포"; }

        double bodyH() { return Math.round(h * 0.86); }
        double hazelX() { return w * 0.16; }
        double clientX() { return w * 0.86; }
        double culpritStop() { return w * 0.64; }

        @Override double captionX() { return w * 0.3; }

        @Override void prescale(double sc) {
            prescaleFigure(cast.culprit, bodyH(), sc, true);
        }

        @Override void paintBase(Graphics2D g) {
            sceneBg(g, cast.scene, new Color(255, 236, 200, 60), 800);
            // 집중선
            g.setColor(new Color(255, 255, 255, 70));
            double cx = culpritStop(), cy = h * 0.5;
            for (int i = 0; i < 36; i++) {
                double a = i * Math.PI * 2 / 36;
                Path2D r = new Path2D.Double();
                r.moveTo(cx + Math.cos(a) * 150, cy + Math.sin(a) * 90);
                r.lineTo(cx + Math.cos(a - 0.03) * 1600, cy + Math.sin(a - 0.03) * 1600);
                r.lineTo(cx + Math.cos(a + 0.03) * 1600, cy + Math.sin(a + 0.03) * 1600);
                r.closePath();
                g.fill(r);
            }
            // 헤이즐(왼쪽)과 앞을 막아선 의뢰인(오른쪽)
            figure(g, "hazel", hazelX(), h * 1.0, Math.round(h * 0.92), false);
            if (cast.client != null) {
                figure(g, cast.client, clientX(), h * 1.0, Math.round(h * 0.9), true);
                sfx(g, "멈춰라!", clientX() - w * 0.14, h * 0.36, 26, new Color(0xFFF6D8), 6);
            }
            tagLabel(g);
        }

        @Override void sound(double t) {
            if (!sounded && t >= FLY1) {
                sounded = true;
                Sfx.play("stamp");
            }
        }

        @Override void drawLive(Graphics2D g, double t) {
            // 1) 달아나다 멈칫
            double run = Math.min(1, t / RUN), e = 1 - Math.pow(1 - run, 3);
            double cx = w * 0.44 + (culpritStop() - w * 0.44) * e;
            double bottom = h * 1.0;
            double shake = 0, slump = 0;
            if (t >= FLY1 && t < FLY1 + 0.45) shake = Math.sin(t * 70) * 6 * (1 - (t - FLY1) / 0.45);
            if (t >= FLY1) slump = Math.min(1, (t - FLY1) / 0.3) * 8;
            double bob = t < RUN ? Math.abs(Math.sin(t * 22)) * 8 : 0;
            cx += shake;
            bottom += slump - bob;
            if (t < RUN) {
                // 달리는 먼지
                g.setColor(new Color(255, 255, 255, 140));
                for (int i = 0; i < 3; i++) g.fill(new Ellipse2D.Double(cx - 70 - i * 26, h * 0.92 - i * 4, 20 - i * 4, 11 - i * 2));
            }
            figure(g, cast.culprit, cx, bottom, bodyH(), true);
            Point2D.Double l = at(cast.culprit, cx, bottom, bodyH(), true, HAND_LX, HAND_Y);
            Point2D.Double r = at(cast.culprit, cx, bottom, bodyH(), true, HAND_RX, HAND_Y);
            double cuffR = Math.max(9, bodyH() * 0.04);
            if (t >= FLY0 && t < FLY1) {
                // 2) 헤이즐의 손에서 날아가는 수갑(빙글빙글)
                double k = (t - FLY0) / (FLY1 - FLY0);
                double sx = hazelX() + w * 0.07, sy = h * 0.62;
                double mx = (l.x + r.x) / 2, my = (l.y + r.y) / 2;
                double x = sx + (mx - sx) * k, y = sy + (my - sy) * k - Math.sin(Math.PI * k) * h * 0.35;
                g.setColor(new Color(255, 255, 255, 120));
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{4, 8}, 0));
                g.draw(new QuadCurve2D.Double(sx, sy, (sx + x) / 2, Math.min(sy, y) - 40, x, y));
                Graphics2D c = (Graphics2D) g.create();
                c.translate(x, y);
                c.rotate(k * Math.PI * 4);
                cuffs(c, new Point2D.Double(-cuffR * 1.6, 0), new Point2D.Double(cuffR * 1.6, 0), cuffR, true);
                c.dispose();
            }
            if (t >= FLY1) {
                // 3) 두 손목에 '철컥'
                cuffs(g, l, r, cuffR, false);
                double k = Math.min(1, (t - FLY1) / 0.2);
                double z = 1.6 - 0.6 * k;
                Graphics2D s = (Graphics2D) g.create();
                s.translate((l.x + r.x) / 2, Math.min(l.y, r.y) - cuffR * 3);
                s.scale(z, z);
                sfx(s, "철컥!", -40, 0, 34, new Color(0xFFE08A), -8);
                s.dispose();
                // 땀방울(맺혔다 떨어진다)
                Point2D.Double head = at(cast.culprit, cx, bottom, bodyH(), true, 230, 150);
                for (int i = 0; i < 3; i++) {
                    double ph = ((t - FLY1) * 0.9 + i * 0.33) % 1.0;
                    double x = head.x + i * 16, y = head.y + ph * 40 - i * 8;
                    Path2D d = new Path2D.Double();
                    d.moveTo(x, y - 12);
                    d.quadTo(x + 7, y, x, y + 6);
                    d.quadTo(x - 7, y, x, y - 12);
                    g.setColor(new Color(141, 195, 232, (int) (220 * (1 - ph))));
                    g.fill(d);
                }
                // 헤이즐 쪽: "잡았다!"
                sfx(g, "잡았다!", hazelX() + w * 0.07, h * 0.55, 30, new Color(0xFFF6D8), -6);
            }
            if (t >= STAMP) {
                // 4) 체포 완료 도장
                double k = Math.min(1, (t - STAMP) / 0.18);
                double z = 2.0 - k;
                Graphics2D st = (Graphics2D) g.create();
                st.translate(w * 0.42, h * 0.74);
                st.rotate(Math.toRadians(-10));
                st.scale(z, z);
                st.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) Math.min(1, k * 1.5) * 0.92f));
                st.setColor(new Color(255, 246, 232, 200));
                st.fill(new RoundRectangle2D.Double(-78, -26, 156, 52, 10, 10));
                st.setColor(new Color(0xC23B2A));
                st.setStroke(new BasicStroke(4f));
                st.draw(new RoundRectangle2D.Double(-78, -26, 156, 52, 10, 10));
                st.setStroke(new BasicStroke(1.6f));
                st.draw(new RoundRectangle2D.Double(-71, -19, 142, 38, 7, 7));
                st.setFont(Theme.xbold(24f / Theme.scale()));
                Theme.drawCentered(st, "체포 완료", 0, 0);
                st.dispose();
            }
        }

        /** 수갑: 두 고리와 사슬. */
        static void cuffs(Graphics2D g, Point2D.Double a, Point2D.Double b, double r, boolean open) {
            Color steel = new Color(0xC4C8D0), dark = new Color(0x4A4E58);
            g.setColor(dark);
            g.setStroke(new BasicStroke(2.2f));
            int links = 4;
            for (int i = 0; i < links; i++) {
                double k0 = (i + 0.5) / links;
                double x = a.x + (b.x - a.x) * k0, y = a.y + (b.y - a.y) * k0 + Math.sin(Math.PI * k0) * r * 0.8;
                g.draw(new Ellipse2D.Double(x - r * 0.32, y - r * 0.2, r * 0.64, r * 0.4));
            }
            for (Point2D.Double p : new Point2D.Double[]{a, b}) {
                g.setColor(steel);
                g.setStroke(new BasicStroke((float) (r * 0.45), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (open) g.draw(new Arc2D.Double(p.x - r, p.y - r, r * 2, r * 2, 40, 300, Arc2D.OPEN));
                else g.draw(new Ellipse2D.Double(p.x - r, p.y - r, r * 2, r * 2));
                g.setColor(dark);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(new Ellipse2D.Double(p.x - r * 1.22, p.y - r * 1.22, r * 2.44, r * 2.44));
                g.setColor(new Color(255, 255, 255, 170));
                g.fill(new Ellipse2D.Double(p.x - r * 0.75, p.y - r * 1.05, r * 0.5, r * 0.28));
            }
        }
    }

    // ================================================================== 소품(동기 칸의 생각 풍선 속 · 사무소 진열대)
    static void prop(Graphics2D g, String name, double cx, double cy, double r) {
        Color ol = new Color(0x3A2819);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name == null ? "" : name) {
            case "lamp": {
                g.setColor(new Color(255, 220, 120, 120));
                g.fill(new Ellipse2D.Double(cx - r, cy - r * 0.9, r * 2, r * 2));
                Path2D body = new Path2D.Double();
                body.moveTo(cx - r * 0.4, cy - r * 0.45); body.lineTo(cx + r * 0.4, cy - r * 0.45);
                body.lineTo(cx + r * 0.5, cy + r * 0.5); body.lineTo(cx - r * 0.5, cy + r * 0.5); body.closePath();
                g.setColor(new Color(0xFBE39A)); g.fill(body);
                g.setColor(ol); g.draw(body);
                g.setColor(new Color(0xB8903A));
                g.fill(new Rectangle2D.Double(cx - r * 0.55, cy - r * 0.62, r * 1.1, r * 0.18));
                g.fill(new Rectangle2D.Double(cx - r * 0.6, cy + r * 0.5, r * 1.2, r * 0.16));
                g.setColor(ol);
                g.draw(new Arc2D.Double(cx - r * 0.22, cy - r * 0.95, r * 0.44, r * 0.5, 0, 180, Arc2D.OPEN));
                g.setColor(new Color(0xF2A23A));
                g.fill(new Ellipse2D.Double(cx - r * 0.1, cy - r * 0.15, r * 0.2, r * 0.34));
                break;
            }
            case "gear": {
                Path2D p = new Path2D.Double();
                int n = 12 * 4;
                for (int i = 0; i < n; i++) {
                    double a = 2 * Math.PI * i / n;
                    double rr = (i % 4 == 0 || i % 4 == 1) ? r * 0.85 : r * 0.68;
                    double x = cx + rr * Math.cos(a), y = cy + rr * Math.sin(a);
                    if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
                }
                p.closePath();
                g.setColor(new Color(0xC9A456)); g.fill(p);
                g.setColor(ol); g.draw(p);
                g.setColor(new Color(0x8C6A3A));
                g.fill(new Ellipse2D.Double(cx - r * 0.25, cy - r * 0.25, r * 0.5, r * 0.5));
                g.setColor(ol);
                g.draw(new Ellipse2D.Double(cx - r * 0.25, cy - r * 0.25, r * 0.5, r * 0.5));
                break;
            }
            case "rope": {
                g.setColor(new Color(0xC9A26C));
                g.setStroke(new BasicStroke((float) (r * 0.16), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 3; i++) g.draw(new Ellipse2D.Double(cx - r * (0.75 - i * 0.12), cy - r * (0.45 - i * 0.06), r * (1.5 - i * 0.24), r * (0.9 - i * 0.12)));
                g.draw(new QuadCurve2D.Double(cx + r * 0.6, cy, cx + r * 0.9, cy + r * 0.5, cx + r * 0.5, cy + r * 0.8));
                g.setColor(ol);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Ellipse2D.Double(cx - r * 0.75 - 4, cy - r * 0.45 - 4, r * 1.5 + 8, r * 0.9 + 8));
                break;
            }
            case "lens": {
                g.setColor(new Color(0xC9A456));
                g.fill(new Ellipse2D.Double(cx - r * 0.8, cy - r * 0.8, r * 1.6, r * 1.6));
                g.setColor(new Color(0xA9D0EA));
                g.fill(new Ellipse2D.Double(cx - r * 0.64, cy - r * 0.64, r * 1.28, r * 1.28));
                g.setColor(new Color(255, 255, 255, 190));
                g.fill(new Ellipse2D.Double(cx - r * 0.42, cy - r * 0.45, r * 0.36, r * 0.24));
                g.setColor(ol);
                g.draw(new Ellipse2D.Double(cx - r * 0.8, cy - r * 0.8, r * 1.6, r * 1.6));
                g.draw(new Ellipse2D.Double(cx - r * 0.64, cy - r * 0.64, r * 1.28, r * 1.28));
                break;
            }
            default:
                g.setColor(new Color(0xE6BE55));
                g.fill(Icons.star(cx, cy, r * 0.8, r * 0.35));
        }
    }
}
