package geomystery.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 * 게임에 쓰이는 캐릭터·배경 이미지를 Java2D 벡터 드로잉으로 "구워" PNG로 저장한다.
 * 앱을 처음 실행할 때 assets/img 가 비어 있으면 자동으로 실행되며,
 * 직접 실행할 수도 있다:  java -cp out geomystery.tools.AssetBaker [출력폴더]
 */
public final class AssetBaker {
    private static final Color OL = new Color(0x3A2819);
    private static final Color GOLD = new Color(0xD9B456);
    /** 스테인드글라스 4색(파랑·빨강·노랑·초록). */
    private static final Color[] GLASS = {new Color(0x3E6E9C), new Color(0xB5402E), new Color(0xE0B850), new Color(0x2F6E5E)};
    private static final int VERSION = 7;

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "assets/img");
        bakeAll(out);
        System.out.println("에셋 굽기 완료 → " + out.toAbsolutePath());
    }

    public static boolean isBaked(Path dir) {
        try {
            Path v = dir.resolve("VERSION");
            return Files.exists(v) && new String(Files.readAllBytes(v)).trim().equals("" + VERSION);
        } catch (IOException e) {
            return false;
        }
    }

    public static void bakeAll(Path dir) throws IOException {
        Files.createDirectories(dir);
        character(dir, "hazel", g -> hazel(g, Outfit.DEFAULT));
        character(dir, "gregor", AssetBaker::gregor);
        character(dir, "minerva", AssetBaker::minerva);
        character(dir, "toru", AssetBaker::toru);
        // 최종 추리 용의자들(얼굴만)
        suspect(dir, "mole", new Color(0x6B7F3A), new Color(0x5B4A42), AssetBaker::mole);
        suspect(dir, "crow", new Color(0x2B2C36), new Color(0x55565E), AssetBaker::crow);
        suspect(dir, "pigeon", new Color(0x7E8594), new Color(0xE07A6A), AssetBaker::pigeon);
        suspect(dir, "cat", new Color(0x2B2B31), new Color(0xF4F1EA), AssetBaker::cat);
        suspect(dir, "mouse", new Color(0x7A5A3A), new Color(0x9C9CA8), AssetBaker::mouse);
        suspect(dir, "fox", new Color(0x6E5A44), OL, AssetBaker::fox);
        suspect(dir, "bat", new Color(0x4A3D55), new Color(0x6A5A78), AssetBaker::bat);
        suspect(dir, "hedgehog", new Color(0x8A6B4A), new Color(0xEBD3A6), AssetBaker::hedgehog);
        background(dir, "bg_garden", AssetBaker::garden);
        background(dir, "bg_clock", AssetBaker::clockTower);
        background(dir, "bg_monastery", AssetBaker::monastery);
        // 4장: 원의 천문대
        suspect(dir, "luna", new Color(0x2E3A66), new Color(0x3A3A44), AssetBaker::luna);
        suspect(dir, "rabbit", new Color(0x6F9CC9), new Color(0xF4EFE6), AssetBaker::rabbit);
        suspect(dir, "otter", new Color(0x7A5232), new Color(0x5A3C26), AssetBaker::otter);
        suspect(dir, "sheep", new Color(0x8A3A4A), new Color(0x4A3A36), AssetBaker::sheep);
        background(dir, "bg_observatory", AssetBaker::observatory);
        appIcon(dir);
        Files.write(dir.resolve("VERSION"), ("" + VERSION).getBytes());
    }

    public interface Painter { void paint(Graphics2D g); }

    // ------------------------------------------------------------------ 공통
    private static Graphics2D hq(BufferedImage im) {
        Graphics2D g = im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        return g;
    }

    /** 360×480 캐릭터를 2배 해상도로 굽고, 얼굴 부분을 잘라 _face 이미지도 만든다. */
    private static void character(Path dir, String name, Painter p) throws IOException {
        BufferedImage[] r = render(p);
        ImageIO.write(r[0], "png", dir.resolve(name + ".png").toFile());
        ImageIO.write(r[1], "png", dir.resolve(name + "_face.png").toFile());
    }

    /** 캐릭터를 메모리에 그린다: {전신(720×960), 얼굴(240×240)}. */
    public static BufferedImage[] render(Painter p) {
        int S = 2;
        BufferedImage im = new BufferedImage(360 * S, 480 * S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = hq(im);
        g.scale(S, S);
        p.paint(g);
        g.dispose();
        BufferedImage face = new BufferedImage(240, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D f = hq(face);
        f.drawImage(im.getSubimage(70 * S, 60 * S, 220 * S, 220 * S), 0, 0, 240, 240, null);
        f.dispose();
        return new BufferedImage[]{im, face};
    }

    /** 헤이즐 꾸미기: 모자·코트(망토)·나비넥타이 색. */
    public static final class Outfit {
        public static final Outfit DEFAULT = new Outfit(new Color(0x8C5A2E), new Color(0x2F5E4F), new Color(0x3A6D5D), new Color(0xB33A2A));
        public final Color hat, coat, cape, tie;

        public Outfit(Color hat, Color coat, Color cape, Color tie) {
            this.hat = hat;
            this.coat = coat;
            this.cape = cape;
            this.tie = tie;
        }
    }

    /** 헤이즐을 주어진 옷차림으로 그린다(전신, 얼굴). */
    public static BufferedImage[] renderHazel(Outfit o) { return render(g -> hazel(g, o)); }

    private static Color shade(Color c, double k) {
        return new Color((int) Math.min(255, c.getRed() * k), (int) Math.min(255, c.getGreen() * k), (int) Math.min(255, c.getBlue() * k));
    }

    /** 용의자 전신: 공통 몸통(옷 색·발 색) 위에 상반신 그림을 얹는다. 얼굴 이미지도 함께 만든다. */
    private static void suspect(Path dir, String name, Color body, Color feet, Painter bust) throws IOException {
        character(dir, name, g -> {
            shape(g, ell(146, 458, 58, 22), feet, 3f);
            shape(g, ell(214, 458, 58, 22), feet, 3f);
            Path2D b = new Path2D.Double();
            b.moveTo(112, 270); b.lineTo(248, 270); b.lineTo(268, 448); b.quadTo(180, 464, 92, 448); b.closePath();
            shape(g, b, body, 3f);
            Color dark = body.darker();
            line(g, dark, 2.5f, 180, 300, 180, 452);
            Path2D arm = new Path2D.Double();
            arm.moveTo(110, 282); arm.quadTo(84, 340, 96, 392); arm.lineTo(118, 392); arm.quadTo(112, 340, 128, 304); arm.closePath();
            shape(g, arm, body, 3f);
            Path2D arm2 = new Path2D.Double();
            arm2.moveTo(250, 282); arm2.quadTo(276, 340, 264, 392); arm2.lineTo(242, 392); arm2.quadTo(248, 340, 232, 304); arm2.closePath();
            shape(g, arm2, body, 3f);
            shape(g, ell(107, 398, 26, 24), feet, 2.5f);
            shape(g, ell(253, 398, 26, 24), feet, 2.5f);
            bust.paint(g);
        });
    }

    /** 어깨(상반신 아랫부분). */
    private static void shoulders(Graphics2D g, Color c) {
        Path2D b = new Path2D.Double();
        b.moveTo(86, 300); b.curveTo(96, 250, 140, 236, 180, 236); b.curveTo(220, 236, 264, 250, 274, 300); b.closePath();
        shape(g, b, c, 3f);
    }

    private static void whiskers(Graphics2D g, Color c, double cx, double cy, double spread) {
        g.setColor(c);
        g.setStroke(round(1.8f));
        for (int k = -1; k <= 1; k++) {
            g.draw(new Line2D.Double(cx - 14, cy + k * 5, cx - 14 - spread, cy + k * 9 - 2));
            g.draw(new Line2D.Double(cx + 14, cy + k * 5, cx + 14 + spread, cy + k * 9 - 2));
        }
    }

    private static void blush(Graphics2D g, double lx, double rx, double y) {
        g.setColor(new Color(229, 140, 110, 85));
        g.fill(ell(lx, y, 18, 10));
        g.fill(ell(rx, y, 18, 10));
    }

    // ------------------------------------------------------------------ 용의자: 두더지 모울 (정원사)
    static void mole(Graphics2D g) {
        Color fur = new Color(0x5B4A42), furL = new Color(0x84705F), pink = new Color(0xE7A0A0);
        shoulders(g, new Color(0x6B7F3A));
        line(g, new Color(0x4E5E28), 6f, 140, 244, 146, 300);
        line(g, new Color(0x4E5E28), 6f, 220, 244, 214, 300);
        shape(g, ell(146, 262, 10, 10), GOLD, 1.5f);
        shape(g, ell(214, 262, 10, 10), GOLD, 1.5f);
        shape(g, ell(180, 188, 152, 136), fur, 3f);
        shape(g, ell(180, 214, 76, 48), furL, 2f);
        eye(g, 150, 180, 4.2);
        eye(g, 210, 180, 4.2);
        shape(g, ell(180, 202, 36, 25), pink, 2.5f);
        g.setColor(new Color(255, 255, 255, 170));
        g.fill(ell(172, 196, 10, 6));
        smile(g, 168, 214, 24, 14);
        whiskers(g, new Color(0x3A2C25), 180, 214, 30);
        blush(g, 134, 226, 206);
        // 밀짚모자
        shape(g, ell(180, 130, 176, 40), new Color(0xE3C072), 3f);
        Path2D crown = new Path2D.Double();
        crown.moveTo(136, 132); crown.curveTo(136, 84, 224, 84, 224, 132); crown.closePath();
        shape(g, crown, new Color(0xE9CB80), 3f);
        shape(g, new Rectangle2D.Double(137, 116, 86, 12), new Color(0xB33A2A), 2f);
        g.setColor(new Color(0xC9A257));
        g.setStroke(new BasicStroke(1.5f));
        for (double x = 104; x <= 256; x += 12) g.draw(new Line2D.Double(x, 124, x + 4, 138));
        // 작은 꽃
        shape(g, ell(214, 114, 14, 14), new Color(0xF2E6C8), 1.5f);
        shape(g, ell(214, 114, 6, 6), new Color(0xE3A23B), 0);
    }

    // ------------------------------------------------------------------ 용의자: 까마귀 코르부스
    static void crow(Graphics2D g) {
        Color black = new Color(0x2B2C36), sheen = new Color(0x3E4558), beakC = new Color(0x55565E);
        shoulders(g, black);
        Path2D scarf = new Path2D.Double();
        scarf.moveTo(126, 246); scarf.quadTo(180, 270, 234, 246); scarf.lineTo(236, 262); scarf.quadTo(180, 288, 124, 262); scarf.closePath();
        shape(g, scarf, new Color(0xA33A28), 2.5f);
        shape(g, geomystery.ui.Ui.poly(200, 266, 218, 268, 214, 300, 198, 298), new Color(0xA33A28), 2.5f);
        // 훔친 반짝이 장식
        shape(g, ell(152, 280, 16, 16), new Color(0xE6BE55), 2f);
        g.setColor(Color.WHITE);
        g.fill(ell(149, 277, 5, 5));
        // 머리 깃
        shape(g, geomystery.ui.Ui.poly(166, 128, 170, 92, 184, 124), black, 3f);
        shape(g, geomystery.ui.Ui.poly(182, 124, 196, 96, 200, 130), black, 3f);
        shape(g, ell(176, 186, 140, 132), black, 3f);
        g.setColor(geomystery.ui.Theme.alpha(sheen, 0.9f));
        g.fill(rot(ell(150, 146, 52, 20), -28, 150, 146));
        g.setColor(new Color(255, 255, 255, 40));
        g.fill(rot(ell(146, 144, 26, 8), -28, 146, 144));
        // 눈: 반쯤 감은 의뭉스러운 눈
        shape(g, ell(152, 178, 34, 34), new Color(0xF1E8D2), 2.5f);
        shape(g, ell(200, 178, 30, 30), new Color(0xF1E8D2), 2.5f);
        eye(g, 155, 182, 7);
        eye(g, 202, 182, 6.5);
        g.setColor(black);
        g.fill(new Arc2D.Double(134, 160, 36, 36, 0, 180, Arc2D.CHORD));
        g.fill(new Arc2D.Double(184, 162, 32, 32, 0, 180, Arc2D.CHORD));
        line(g, OL, 3f, 134, 178, 170, 176);
        line(g, OL, 3f, 184, 176, 216, 179);
        // 부리
        Path2D beak = new Path2D.Double();
        beak.moveTo(168, 200); beak.quadTo(214, 196, 262, 218); beak.quadTo(214, 232, 170, 226); beak.closePath();
        shape(g, beak, beakC, 3f);
        line(g, OL, 2f, 172, 214, 236, 218);
        g.setColor(new Color(255, 255, 255, 90));
        g.fill(ell(196, 206, 26, 5));
    }

    // ------------------------------------------------------------------ 용의자: 비둘기 청소부 피오
    static void pigeon(Graphics2D g) {
        Color grey = new Color(0xA9AFBB), dark = new Color(0x7E8594);
        shoulders(g, dark);
        // 무지갯빛 목
        Path2D neck = new Path2D.Double();
        neck.moveTo(120, 236); neck.quadTo(180, 270, 240, 236); neck.lineTo(236, 256); neck.quadTo(180, 290, 124, 256); neck.closePath();
        g.setPaint(new GradientPaint(120, 240, new Color(0x5E8C74), 240, 260, new Color(0x7D5A93)));
        g.fill(neck);
        shape(g, neck, null, 2.5f);
        shape(g, ell(180, 182, 128, 128), grey, 3f);
        shape(g, ell(156, 178, 24, 24), new Color(0xE8913A), 2f);
        shape(g, ell(204, 178, 24, 24), new Color(0xE8913A), 2f);
        eye(g, 156, 178, 6);
        eye(g, 204, 178, 6);
        shape(g, ell(180, 200, 18, 9), new Color(0xF2EEE6), 1.5f);
        shape(g, geomystery.ui.Ui.poly(171, 204, 189, 204, 180, 226), new Color(0x3A3A3A), 2.5f);
        blush(g, 138, 222, 208);
        // 청소부 모자
        Path2D cap = new Path2D.Double();
        cap.moveTo(122, 154); cap.curveTo(124, 100, 236, 100, 238, 154); cap.quadTo(180, 142, 122, 154); cap.closePath();
        shape(g, cap, new Color(0x3E5F8A), 3f);
        shape(g, rot(ell(232, 152, 64, 16), -8, 232, 152), new Color(0x2F4A6E), 2.5f);
        shape(g, ell(180, 112, 12, 10), new Color(0xE6BE55), 1.5f);
        // 빗자루 끝
        line(g, new Color(0x8A5A34), 6f, 252, 300, 268, 218);
        shape(g, geomystery.ui.Ui.poly(260, 222, 278, 214, 286, 196, 266, 192), GOLD, 2f);
    }

    // ------------------------------------------------------------------ 용의자: 고양이 집사 토크 (턱시도 고양이)
    static void cat(Graphics2D g) {
        Color black = new Color(0x2B2B31), white = new Color(0xF4F1EA), pink = new Color(0xE7A0A0);
        shoulders(g, black);
        shape(g, geomystery.ui.Ui.poly(150, 240, 210, 240, 180, 300), white, 2.5f);
        shape(g, geomystery.ui.Ui.poly(180, 256, 156, 246, 156, 268), new Color(0xA33A28), 2f);
        shape(g, geomystery.ui.Ui.poly(180, 256, 204, 246, 204, 268), new Color(0xA33A28), 2f);
        shape(g, ell(180, 257, 9, 9), new Color(0x74271A), 1.5f);
        // 귀
        shape(g, geomystery.ui.Ui.poly(114, 158, 124, 90, 166, 128), black, 3f);
        shape(g, geomystery.ui.Ui.poly(246, 158, 236, 90, 194, 128), black, 3f);
        g.setColor(pink);
        g.fill(geomystery.ui.Ui.poly(126, 146, 130, 106, 154, 130));
        g.fill(geomystery.ui.Ui.poly(234, 146, 230, 106, 206, 130));
        shape(g, ell(180, 188, 150, 130), black, 3f);
        // 흰 얼굴 무늬
        Path2D blaze = new Path2D.Double();
        blaze.moveTo(180, 150); blaze.curveTo(170, 170, 140, 196, 140, 216); blaze.curveTo(150, 246, 210, 246, 220, 216);
        blaze.curveTo(220, 196, 190, 170, 180, 150); blaze.closePath();
        shape(g, blaze, white, 0);
        // 눈
        shape(g, ell(152, 184, 28, 24), new Color(0xB6CF5E), 2.5f);
        shape(g, ell(208, 184, 28, 24), new Color(0xB6CF5E), 2.5f);
        g.setColor(OL);
        g.fill(ell(152, 184, 6, 18));
        g.fill(ell(208, 184, 6, 18));
        g.setColor(Color.WHITE);
        g.fill(ell(156, 179, 5, 5));
        g.fill(ell(212, 179, 5, 5));
        // 외눈 안경
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(4f));
        g.draw(ell(208, 184, 40, 40));
        g.setStroke(new BasicStroke(1.8f));
        Path2D chain = new Path2D.Double();
        chain.moveTo(226, 198); chain.quadTo(246, 232, 222, 252);
        g.draw(chain);
        shape(g, geomystery.ui.Ui.poly(173, 204, 187, 204, 180, 213), pink, 2f);
        g.setColor(OL);
        g.setStroke(round(2.2f));
        Path2D m = new Path2D.Double();
        m.moveTo(180, 213); m.lineTo(180, 218);
        m.moveTo(170, 220); m.quadTo(175, 225, 180, 218); m.quadTo(185, 225, 190, 220);
        g.draw(m);
        whiskers(g, new Color(0x6E6A62), 180, 214, 34);
    }

    // ------------------------------------------------------------------ 용의자: 생쥐 기술자 틱
    static void mouse(Graphics2D g) {
        Color fur = new Color(0x9C9CA8), pink = new Color(0xE9A3A8);
        shoulders(g, new Color(0x7A5A3A));
        shape(g, new RoundRectangle2D.Double(150, 252, 60, 40, 8, 8), new Color(0x8C6A46), 2.5f);
        line(g, new Color(0xB8BCC4), 5f, 166, 262, 188, 282);
        shape(g, ell(192, 284, 12, 12), new Color(0xB8BCC4), 2f);
        shape(g, ell(116, 138, 78, 76), fur, 3f);
        shape(g, ell(244, 138, 78, 76), fur, 3f);
        g.setColor(pink);
        g.fill(ell(118, 140, 48, 46));
        g.fill(ell(242, 140, 48, 46));
        shape(g, ell(180, 194, 128, 118), fur, 3f);
        shape(g, ell(180, 222, 62, 40), new Color(0xC9C9D2), 0);
        eye(g, 158, 196, 6.5);
        eye(g, 202, 196, 6.5);
        shape(g, ell(180, 218, 16, 12), pink, 2f);
        shape(g, new Rectangle2D.Double(176, 228, 8, 8), Color.WHITE, 1.5f);
        whiskers(g, new Color(0x55555E), 180, 220, 32);
        blush(g, 140, 220, 214);
        // 이마의 고글
        shape(g, new RoundRectangle2D.Double(118, 146, 124, 12, 10, 10), new Color(0x5A3A1E), 2f);
        for (double x : new double[]{158, 202}) {
            shape(g, ell(x, 150, 38, 32), new Color(0xC99B45), 2.5f);
            shape(g, ell(x, 150, 26, 20), new Color(0x9FC6D6), 2f);
            g.setColor(new Color(255, 255, 255, 160));
            g.fill(ell(x - 5, 146, 8, 5));
        }
    }

    // ------------------------------------------------------------------ 용의자: 여우 순례자 레나
    static void fox(Graphics2D g) {
        Color fur = new Color(0xD9772E), cream = new Color(0xF4E3C2), cloak = new Color(0x6E5A44);
        shoulders(g, cloak);
        // 두건 뒷부분
        Path2D hood = new Path2D.Double();
        hood.moveTo(98, 262); hood.curveTo(78, 120, 282, 120, 262, 262); hood.quadTo(180, 246, 98, 262); hood.closePath();
        shape(g, hood, new Color(0x5E4C38), 3f);
        // 귀(두건 밖으로)
        shape(g, geomystery.ui.Ui.poly(124, 160, 118, 84, 164, 130), fur, 3f);
        shape(g, geomystery.ui.Ui.poly(236, 160, 242, 84, 196, 130), fur, 3f);
        g.setColor(OL);
        g.fill(geomystery.ui.Ui.poly(119, 96, 118, 84, 130, 96));
        g.fill(geomystery.ui.Ui.poly(241, 96, 242, 84, 230, 96));
        g.setColor(new Color(0xF0B27A));
        g.fill(geomystery.ui.Ui.poly(130, 148, 128, 104, 154, 132));
        g.fill(geomystery.ui.Ui.poly(230, 148, 232, 104, 206, 132));
        shape(g, ell(180, 186, 136, 120), fur, 3f);
        // 흰 볼과 주둥이
        Path2D cheeks = new Path2D.Double();
        cheeks.moveTo(114, 196); cheeks.quadTo(150, 186, 180, 210); cheeks.quadTo(210, 186, 246, 196);
        cheeks.quadTo(232, 244, 180, 248); cheeks.quadTo(128, 244, 114, 196); cheeks.closePath();
        shape(g, cheeks, cream, 0);
        eye(g, 156, 182, 6.5);
        eye(g, 204, 182, 6.5);
        g.setColor(OL);
        g.setStroke(round(2.4f));
        g.draw(new Arc2D.Double(144, 168, 22, 12, 30, 120, Arc2D.OPEN));
        g.draw(new Arc2D.Double(194, 168, 22, 12, 30, 120, Arc2D.OPEN));
        shape(g, ell(180, 214, 16, 11), OL, 0);
        g.draw(new Arc2D.Double(170, 216, 20, 12, 200, 140, Arc2D.OPEN));
        blush(g, 136, 224, 210);
        // 순례자의 가리비 배지
        Path2D shell = new Path2D.Double();
        shell.moveTo(214, 282); shell.lineTo(200, 264); shell.quadTo(214, 252, 228, 264); shell.closePath();
        shape(g, shell, new Color(0xE6BE55), 2f);
        line(g, new Color(0xB8903A), 1.5f, 214, 280, 214, 260);
        line(g, new Color(0xB8903A), 1.5f, 214, 280, 206, 262);
        line(g, new Color(0xB8903A), 1.5f, 214, 280, 222, 262);
    }

    // ------------------------------------------------------------------ 용의자: 박쥐 종지기 벨
    static void bat(Graphics2D g) {
        Color fur = new Color(0x6A5A78), face = new Color(0x8E7E9C), wing = new Color(0x4A3D55);
        // 날개 어깨
        Path2D w = new Path2D.Double();
        w.moveTo(76, 300); w.lineTo(96, 246); w.quadTo(110, 262, 124, 250); w.quadTo(140, 262, 152, 244);
        w.lineTo(208, 244); w.quadTo(220, 262, 236, 250); w.quadTo(250, 262, 264, 246); w.lineTo(284, 300); w.closePath();
        shape(g, w, wing, 3f);
        // 목의 작은 종
        Path2D bell = new Path2D.Double();
        bell.moveTo(168, 284); bell.quadTo(168, 258, 180, 258); bell.quadTo(192, 258, 192, 284); bell.closePath();
        shape(g, bell, new Color(0xE6BE55), 2f);
        shape(g, ell(180, 286, 8, 8), new Color(0xB8903A), 1.5f);
        // 큰 귀
        shape(g, geomystery.ui.Ui.poly(126, 166, 92, 70, 166, 132), fur, 3f);
        shape(g, geomystery.ui.Ui.poly(234, 166, 268, 70, 194, 132), fur, 3f);
        g.setColor(new Color(0xC48FA8));
        g.fill(geomystery.ui.Ui.poly(132, 152, 108, 92, 156, 134));
        g.fill(geomystery.ui.Ui.poly(228, 152, 252, 92, 204, 134));
        shape(g, ell(180, 192, 130, 118), fur, 3f);
        shape(g, ell(180, 208, 84, 62), face, 0);
        shape(g, ell(158, 186, 22, 22), new Color(0xF2D46A), 2f);
        shape(g, ell(202, 186, 22, 22), new Color(0xF2D46A), 2f);
        eye(g, 158, 187, 5.5);
        eye(g, 202, 187, 5.5);
        shape(g, ell(180, 208, 20, 13), new Color(0x4A3D55), 2f);
        g.setColor(OL);
        g.fill(ell(176, 208, 4, 4));
        g.fill(ell(184, 208, 4, 4));
        smile(g, 164, 214, 32, 16);
        shape(g, geomystery.ui.Ui.poly(169, 225, 175, 225, 172, 234), Color.WHITE, 1.4f);
        shape(g, geomystery.ui.Ui.poly(185, 225, 191, 225, 188, 234), Color.WHITE, 1.4f);
    }

    // ------------------------------------------------------------------ 용의자: 고슴도치 견습 수사 마르코
    static void hedgehog(Graphics2D g) {
        Color spine = new Color(0x7A5A3E), spineD = new Color(0x5E4430), face = new Color(0xEBD3A6);
        shoulders(g, new Color(0x8A6B4A));
        g.setColor(GOLD);
        g.setStroke(round(5));
        Path2D rope = new Path2D.Double();
        rope.moveTo(132, 246); rope.quadTo(180, 270, 228, 246);
        g.draw(rope);
        // 가시
        Path2D spikes = new Path2D.Double();
        int n = 22;
        for (int i = 0; i <= n; i++) {
            double a = Math.toRadians(160 + 220.0 * i / n);
            double r = i % 2 == 0 ? 96 : 72;
            double x = 180 + r * Math.cos(a), y = 196 + r * 0.92 * Math.sin(a);
            if (i == 0) spikes.moveTo(x, y); else spikes.lineTo(x, y);
        }
        spikes.lineTo(180, 230);
        spikes.closePath();
        shape(g, spikes, spine, 3f);
        g.setColor(spineD);
        g.setStroke(round(2f));
        for (int i = 1; i < n; i += 2) {
            double a = Math.toRadians(160 + 220.0 * i / n);
            g.draw(new Line2D.Double(180 + 52 * Math.cos(a), 196 + 48 * Math.sin(a), 180 + 66 * Math.cos(a), 196 + 61 * Math.sin(a)));
        }
        shape(g, ell(138, 160, 22, 20), face, 2.5f);
        shape(g, ell(222, 160, 22, 20), face, 2.5f);
        shape(g, ell(180, 200, 116, 102), face, 3f);
        eye(g, 160, 194, 6);
        eye(g, 200, 194, 6);
        shape(g, ell(180, 214, 18, 13), OL, 0);
        g.setColor(Color.WHITE);
        g.fill(ell(176, 210, 5, 4));
        smile(g, 170, 218, 20, 12);
        blush(g, 140, 220, 214);
        // 머리 위 작은 묵주알
        shape(g, ell(180, 118, 10, 10), new Color(0xB33A2A), 1.5f);
    }

    private static void background(Path dir, String name, Painter p) throws IOException {
        BufferedImage im = new BufferedImage(1600, 640, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = hq(im);
        p.paint(g);
        g.dispose();
        ImageIO.write(im, "png", dir.resolve(name + ".png").toFile());
    }

    private static void shape(Graphics2D g, Shape s, Color fill, float stroke) {
        if (fill != null) { g.setColor(fill); g.fill(s); }
        if (stroke > 0) {
            g.setColor(OL);
            g.setStroke(round(stroke));
            g.draw(s);
        }
    }

    private static Ellipse2D ell(double cx, double cy, double w, double h) {
        return new Ellipse2D.Double(cx - w / 2, cy - h / 2, w, h);
    }

    private static Shape rot(Shape s, double deg, double cx, double cy) {
        return AffineTransform.getRotateInstance(Math.toRadians(deg), cx, cy).createTransformedShape(s);
    }

    private static BasicStroke round(float w) { return new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); }

    /** 윤곽선 색의 둥근 미소 호(x, y, w, h 상자 안). */
    private static void smile(Graphics2D g, double x, double y, double w, double h) {
        g.setColor(OL);
        g.setStroke(round(2.2f));
        g.draw(new Arc2D.Double(x, y, w, h, 200, 140, Arc2D.OPEN));
    }

    private static void line(Graphics2D g, Color c, float w, double x1, double y1, double x2, double y2) {
        g.setColor(c);
        g.setStroke(round(w));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void eye(Graphics2D g, double x, double y, double r) {
        g.setColor(OL);
        g.fill(ell(x, y, r * 2, r * 2.2));
        g.setColor(Color.WHITE);
        g.fill(ell(x + r * 0.35, y - r * 0.45, r * 0.75, r * 0.75));
    }

    private static Shape gear(double cx, double cy, double r, int teeth, double toothH) {
        Path2D p = new Path2D.Double();
        int n = teeth * 4;
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            double rr = (i % 4 == 0 || i % 4 == 1) ? r + toothH : r;
            double x = cx + rr * Math.cos(a), y = cy + rr * Math.sin(a);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    // ------------------------------------------------------------------ 헤이즐 (다람쥐 탐정)
    static void hazel(Graphics2D g, Outfit o) {
        Color fur = new Color(0xC47A3A), furDark = new Color(0xA35F2A), cream = new Color(0xF2DDB6);
        Color coat = o.coat, coat2 = o.cape;
        Color hat = o.hat, hatLine = shade(o.hat, 1.2), hatBand = shade(o.hat, 0.78), hatBrim = shade(o.hat, 0.88);
        // 꼬리
        Path2D tail = new Path2D.Double();
        tail.moveTo(222, 410);
        tail.curveTo(338, 418, 352, 262, 304, 212);
        tail.curveTo(272, 178, 226, 192, 236, 232);
        tail.curveTo(244, 264, 292, 262, 290, 304);
        tail.curveTo(288, 348, 252, 372, 218, 374);
        tail.closePath();
        shape(g, tail, new Color(0xB5652B), 3f);
        g.setColor(new Color(0xD58F4E));
        g.setStroke(round(9));
        Path2D hl = new Path2D.Double();
        hl.moveTo(306, 240); hl.curveTo(326, 290, 306, 356, 250, 390);
        g.draw(hl);
        // 발
        shape(g, ell(150, 455, 58, 22), new Color(0x7A4A26), 3f);
        shape(g, ell(212, 455, 58, 22), new Color(0x7A4A26), 3f);
        // 코트
        Path2D body = new Path2D.Double();
        body.moveTo(128, 268); body.lineTo(232, 268); body.lineTo(254, 446);
        body.quadTo(180, 460, 106, 446); body.closePath();
        shape(g, body, coat, 3f);
        line(g, shade(coat, 0.77), 2.5f, 180, 330, 180, 448);
        for (int y : new int[]{350, 388, 424}) {
            shape(g, ell(196, y, 10, 10), GOLD, 1.5f);
        }
        // 망토
        Path2D cape = new Path2D.Double();
        cape.moveTo(122, 262); cape.quadTo(180, 248, 238, 262); cape.lineTo(266, 334);
        cape.quadTo(180, 352, 94, 334); cape.closePath();
        shape(g, cape, coat2, 3f);
        // 오른팔 + 돋보기
        Path2D arm = new Path2D.Double();
        arm.moveTo(236, 300); arm.quadTo(262, 330, 262, 360); arm.lineTo(246, 364); arm.quadTo(244, 336, 226, 316); arm.closePath();
        shape(g, arm, coat, 2.5f);
        line(g, new Color(0x5A3A1E), 8f, 262, 360, 276, 336);
        shape(g, ell(256, 366, 22, 22), fur, 2.5f);
        g.setColor(new Color(255, 248, 220, 110));
        g.fill(ell(290, 318, 46, 46));
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(7));
        g.draw(ell(290, 318, 46, 46));
        g.setColor(OL);
        g.setStroke(new BasicStroke(2f));
        g.draw(ell(290, 318, 54, 54));
        g.setColor(new Color(255, 255, 255, 170));
        g.setStroke(round(3));
        g.draw(new Arc2D.Double(276, 304, 26, 26, 100, 70, Arc2D.OPEN));
        // 나비넥타이
        shape(g, geomystery.ui.Ui.poly(180, 270, 160, 260, 160, 282), o.tie, 2f);
        shape(g, geomystery.ui.Ui.poly(180, 270, 200, 260, 200, 282), o.tie, 2f);
        shape(g, ell(180, 271, 9, 9), shade(o.tie, 0.78), 1.5f);
        // 귀 (머리 묶음은 몸통과 겹치도록 아래로 이동)
        AffineTransform keep = g.getTransform();
        g.translate(0, 24);
        shape(g, geomystery.ui.Ui.poly(126, 150, 132, 92, 166, 130), fur, 3f);
        shape(g, geomystery.ui.Ui.poly(234, 150, 228, 92, 194, 130), fur, 3f);
        g.setColor(new Color(0xE8A86A));
        g.fill(geomystery.ui.Ui.poly(136, 140, 138, 108, 156, 132));
        g.fill(geomystery.ui.Ui.poly(224, 140, 222, 108, 204, 132));
        // 얼굴
        shape(g, ell(180, 180, 130, 114), fur, 3f);
        shape(g, ell(180, 208, 88, 56), cream, 2f);
        g.setColor(new Color(229, 140, 110, 90));
        g.fill(ell(134, 206, 20, 11));
        g.fill(ell(226, 206, 20, 11));
        eye(g, 156, 184, 7.5);
        eye(g, 204, 184, 7.5);
        shape(g, ell(180, 201, 16, 11), OL, 0);
        g.setColor(OL);
        g.setStroke(round(2.2f));
        Path2D m = new Path2D.Double();
        m.moveTo(180, 206); m.lineTo(180, 212);
        m.moveTo(170, 212); m.quadTo(180, 220, 190, 212);
        g.draw(m);
        shape(g, new Rectangle2D.Double(175.5, 214, 9, 8), Color.WHITE, 1.6f);
        // 사냥 모자 (디어스토커)
        Path2D hatShape = new Path2D.Double();
        hatShape.moveTo(114, 156); hatShape.curveTo(116, 96, 244, 96, 246, 156); hatShape.quadTo(180, 142, 114, 156); hatShape.closePath();
        shape(g, hatShape, hat, 3f);
        g.setColor(hatLine);
        g.setStroke(new BasicStroke(2f));
        for (double x : new double[]{148, 180, 212}) g.draw(new Line2D.Double(x, 108, x + (x - 180) * 0.12, 148));
        Path2D plaid = new Path2D.Double();
        plaid.moveTo(122, 132); plaid.quadTo(180, 116, 238, 132);
        g.draw(plaid);
        Path2D band = new Path2D.Double();
        band.moveTo(112, 150); band.quadTo(180, 134, 248, 150); band.lineTo(250, 162);
        band.quadTo(180, 148, 110, 162); band.closePath();
        shape(g, band, hatBand, 2.5f);
        Path2D brim = new Path2D.Double();
        brim.moveTo(140, 160); brim.quadTo(180, 150, 220, 160); brim.quadTo(180, 176, 140, 160); brim.closePath();
        shape(g, brim, hatBrim, 2.5f);
        shape(g, ell(180, 101, 14, 10), hatBand, 2f);
        // 귀 끝이 모자 위로
        shape(g, geomystery.ui.Ui.poly(128, 128, 134, 90, 152, 116), fur, 3f);
        shape(g, geomystery.ui.Ui.poly(232, 128, 226, 90, 208, 116), fur, 3f);
        g.setTransform(keep);
    }

    // ------------------------------------------------------------------ 그레고르 영감 (오소리 시계탑지기)
    static void gregor(Graphics2D g) {
        Color grey = new Color(0x8E959E), coat = new Color(0x5F6670), dark = new Color(0x2B2E33), white = new Color(0xF4F1EA);
        shape(g, ell(148, 458, 62, 22), new Color(0x4A4F57), 3f);
        shape(g, ell(214, 458, 62, 22), new Color(0x4A4F57), 3f);
        Path2D body = new Path2D.Double();
        body.moveTo(106, 268); body.lineTo(254, 268); body.lineTo(272, 450); body.quadTo(180, 464, 88, 450); body.closePath();
        shape(g, body, coat, 3f);
        Path2D collar = new Path2D.Double();
        collar.moveTo(156, 268); collar.lineTo(180, 306); collar.lineTo(204, 268); collar.closePath();
        shape(g, collar, white, 2.5f);
        // 앞치마
        Path2D apron = new Path2D.Double();
        apron.moveTo(138, 300); apron.lineTo(222, 300); apron.lineTo(236, 450); apron.quadTo(180, 458, 124, 450); apron.closePath();
        shape(g, apron, new Color(0xA06B3A), 3f);
        line(g, OL, 2.5f, 142, 300, 150, 266);
        line(g, OL, 2.5f, 218, 300, 210, 266);
        shape(g, new RoundRectangle2D.Double(158, 344, 44, 32, 6, 6), new Color(0x8A5A2E), 2.5f);
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(3));
        g.draw(ell(172, 334, 10, 10));
        g.draw(new Line2D.Double(177, 334, 192, 334));
        g.draw(new Line2D.Double(188, 334, 188, 339));
        // 팔
        Path2D arm = new Path2D.Double();
        arm.moveTo(104, 280); arm.quadTo(80, 340, 92, 396); arm.lineTo(114, 396); arm.quadTo(108, 340, 124, 302); arm.closePath();
        shape(g, arm, coat, 3f);
        shape(g, ell(103, 400, 24, 24), new Color(0x4A4F57), 2.5f);
        Path2D arm2 = new Path2D.Double();
        arm2.moveTo(256, 280); arm2.quadTo(280, 340, 268, 396); arm2.lineTo(246, 396); arm2.quadTo(252, 340, 236, 302); arm2.closePath();
        shape(g, arm2, coat, 3f);
        shape(g, ell(257, 400, 24, 24), new Color(0x4A4F57), 2.5f);
        // 머리
        AffineTransform keep = g.getTransform();
        g.translate(0, 28);
        shape(g, ell(126, 130, 34, 32), new Color(0x3E4249), 3f);
        shape(g, ell(234, 130, 34, 32), new Color(0x3E4249), 3f);
        shape(g, ell(180, 180, 142, 122), grey, 3f);
        Path2D stripe = new Path2D.Double();
        stripe.moveTo(180, 120); stripe.curveTo(162, 122, 158, 160, 166, 200);
        stripe.lineTo(194, 200); stripe.curveTo(202, 160, 198, 122, 180, 120); stripe.closePath();
        g.setColor(white);
        g.fill(stripe);
        g.setColor(dark);
        g.fill(rot(ell(148, 180, 44, 30), 18, 148, 180));
        g.fill(rot(ell(212, 180, 44, 30), -18, 212, 180));
        g.setColor(Color.WHITE);
        g.fill(ell(150, 180, 13, 13));
        g.fill(ell(210, 180, 13, 13));
        g.setColor(OL);
        g.fill(ell(151, 181, 7, 7));
        g.fill(ell(209, 181, 7, 7));
        shape(g, ell(148, 214, 60, 40), white, 0);
        shape(g, ell(212, 214, 60, 40), white, 0);
        // 안경
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(4));
        g.draw(ell(150, 180, 38, 38));
        g.draw(ell(210, 180, 38, 38));
        g.draw(new Line2D.Double(169, 177, 191, 177));
        // 코와 콧수염
        shape(g, ell(180, 204, 24, 15), dark, 0);
        Path2D mus = new Path2D.Double();
        mus.moveTo(180, 212); mus.curveTo(170, 226, 150, 228, 140, 216); mus.curveTo(152, 222, 166, 216, 180, 212);
        mus.moveTo(180, 212); mus.curveTo(190, 226, 210, 228, 220, 216); mus.curveTo(208, 222, 194, 216, 180, 212);
        shape(g, mus, white, 2f);
        g.setColor(OL);
        g.setStroke(new BasicStroke(3));
        g.draw(ell(180, 180, 142, 122));
        g.setTransform(keep);
    }

    // ------------------------------------------------------------------ 미네르바 교수 (부엉이)
    static void minerva(Graphics2D g) {
        Color feather = new Color(0x8B6B45), dark = new Color(0x6A4E30), belly = new Color(0xEAD8B2);
        // 발
        for (double fx : new double[]{152, 208}) {
            for (int k = -1; k <= 1; k++) shape(g, ell(fx + k * 11, 456, 12, 18), new Color(0xE3A23B), 2f);
        }
        // 몸통과 날개
        shape(g, ell(180, 352, 196, 214), feather, 3f);
        shape(g, ell(180, 372, 126, 156), belly, 2f);
        g.setColor(new Color(0xC9B088));
        g.setStroke(round(2.5f));
        for (int r = 0; r < 4; r++) {
            for (int c = -1; c <= 1; c++) {
                double x = 180 + c * 26 + (r % 2) * 13 - 6, y = 320 + r * 30;
                g.draw(new Arc2D.Double(x - 8, y - 6, 16, 12, 200, 140, Arc2D.OPEN));
            }
        }
        shape(g, rot(ell(96, 352, 62, 158), 12, 96, 352), dark, 3f);
        shape(g, rot(ell(264, 352, 62, 158), -12, 264, 352), dark, 3f);
        // 컴퍼스(제도 도구)
        line(g, new Color(0xB8903A), 6f, 286, 300, 262, 410);
        line(g, new Color(0xB8903A), 6f, 286, 300, 306, 410);
        shape(g, ell(286, 296, 16, 16), GOLD, 2f);
        line(g, OL, 2f, 262, 410, 262, 420);
        // 스카프
        shape(g, new RoundRectangle2D.Double(110, 246, 140, 30, 18, 18), new Color(0x2F5E50), 3f);
        shape(g, geomystery.ui.Ui.poly(196, 262, 222, 262, 232, 338, 210, 334), new Color(0x2F5E50), 3f);
        line(g, GOLD, 3f, 214, 322, 228, 324);
        // 머리
        shape(g, geomystery.ui.Ui.poly(104, 132, 108, 66, 146, 112), feather, 3f);
        shape(g, geomystery.ui.Ui.poly(256, 132, 252, 66, 214, 112), feather, 3f);
        shape(g, ell(180, 172, 196, 164), feather, 3f);
        shape(g, ell(144, 176, 84, 84), new Color(0xF1E2BF), 2.5f);
        shape(g, ell(216, 176, 84, 84), new Color(0xF1E2BF), 2.5f);
        shape(g, ell(144, 176, 46, 46), new Color(0xF2C04A), 2.5f);
        shape(g, ell(216, 176, 46, 46), new Color(0xF2C04A), 2.5f);
        eye(g, 144, 177, 11);
        eye(g, 216, 177, 11);
        // 반달 안경
        g.setColor(new Color(0x7A5530));
        g.setStroke(new BasicStroke(3.5f));
        g.draw(new Arc2D.Double(118, 164, 52, 46, 180, 180, Arc2D.OPEN));
        g.draw(new Arc2D.Double(190, 164, 52, 46, 180, 180, Arc2D.OPEN));
        g.draw(new Line2D.Double(170, 187, 190, 187));
        shape(g, geomystery.ui.Ui.poly(170, 200, 190, 200, 180, 226), new Color(0xE3A23B), 2.5f);
        // 학자 모자
        shape(g, geomystery.ui.Ui.poly(110, 88, 180, 62, 250, 88, 180, 112), new Color(0x2F2A26), 3f);
        shape(g, new Rectangle2D.Double(150, 92, 60, 22), new Color(0x2F2A26), 3f);
        line(g, GOLD, 3f, 238, 90, 246, 128);
        shape(g, ell(246, 132, 10, 12), GOLD, 1.5f);
    }

    // ------------------------------------------------------------------ 토르투 수도사 (거북이)
    static void toru(Graphics2D g) {
        Color skin = new Color(0x8FA460), shell = new Color(0x6E7F45), robe = new Color(0x7A5232), hood = new Color(0x664329);
        // 등껍질
        shape(g, ell(180, 318, 244, 226), shell, 3f);
        g.setColor(new Color(0x56652F));
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60 + 30);
            g.draw(new Line2D.Double(180 + 40 * Math.cos(a), 300 + 40 * Math.sin(a), 180 + 110 * Math.cos(a), 300 + 100 * Math.sin(a)));
        }
        Path2D hex = new Path2D.Double();
        for (int i = 0; i < 6; i++) {
            double a = Math.toRadians(i * 60 + 30);
            double x = 180 + 44 * Math.cos(a), y = 300 + 44 * Math.sin(a);
            if (i == 0) hex.moveTo(x, y); else hex.lineTo(x, y);
        }
        hex.closePath();
        g.draw(hex);
        // 발
        shape(g, ell(148, 458, 56, 22), skin, 3f);
        shape(g, ell(212, 458, 56, 22), skin, 3f);
        // 수도복
        Path2D body = new Path2D.Double();
        body.moveTo(122, 252); body.lineTo(238, 252); body.lineTo(262, 450); body.quadTo(180, 462, 98, 450); body.closePath();
        shape(g, body, robe, 3f);
        line(g, new Color(0x5F3E24), 2.5f, 150, 300, 140, 446);
        line(g, new Color(0x5F3E24), 2.5f, 210, 300, 220, 446);
        // 허리끈
        g.setColor(GOLD);
        g.setStroke(round(6));
        Path2D rope = new Path2D.Double();
        rope.moveTo(112, 362); rope.quadTo(180, 376, 250, 362);
        g.draw(rope);
        g.draw(new Line2D.Double(200, 370, 206, 420));
        g.fill(ell(206, 424, 12, 12));
        // 삼각자를 든 손
        shape(g, geomystery.ui.Ui.poly(130, 296, 236, 296, 130, 364), GOLD, 3f);
        shape(g, geomystery.ui.Ui.poly(146, 308, 196, 308, 146, 342), robe, 2f);
        shape(g, ell(132, 330, 26, 26), skin, 2.5f);
        shape(g, ell(226, 302, 26, 26), skin, 2.5f);
        // 두건
        Path2D h = new Path2D.Double();
        h.moveTo(102, 262); h.curveTo(86, 120, 274, 120, 258, 262); h.quadTo(180, 238, 102, 262); h.closePath();
        shape(g, h, hood, 3f);
        // 얼굴
        shape(g, ell(180, 186, 118, 116), skin, 3f);
        g.setColor(new Color(0x7A8F4E));
        g.fill(ell(150, 150, 16, 12));
        g.fill(ell(208, 146, 12, 9));
        g.fill(ell(200, 228, 10, 8));
        eye(g, 158, 186, 7);
        eye(g, 202, 186, 7);
        g.setColor(skin.darker());
        g.setStroke(round(3));
        g.draw(new Arc2D.Double(146, 170, 24, 14, 20, 140, Arc2D.OPEN));
        g.draw(new Arc2D.Double(190, 170, 24, 14, 20, 140, Arc2D.OPEN));
        g.setColor(OL);
        g.setStroke(round(2.4f));
        g.draw(new Arc2D.Double(166, 204, 28, 16, 200, 140, Arc2D.OPEN));
        g.setColor(new Color(229, 140, 110, 80));
        g.fill(ell(140, 210, 18, 10));
        g.fill(ell(220, 210, 18, 10));
        // 두건 앞자락
        Path2D hf = new Path2D.Double();
        hf.moveTo(112, 248); hf.quadTo(180, 278, 248, 248); hf.lineTo(240, 268); hf.quadTo(180, 292, 120, 268); hf.closePath();
        shape(g, hf, hood, 3f);
    }

    // ------------------------------------------------------------------ 배경: 선의 정원
    static void garden(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0xF2DFAE), 0, 420, new Color(0xE6C98C)));
        g.fillRect(0, 0, 1600, 640);
        g.setColor(new Color(255, 240, 200, 140));
        g.fill(ell(1240, 120, 180, 180));
        g.setColor(new Color(255, 244, 214));
        g.fill(ell(1240, 120, 110, 110));
        // 먼 언덕
        g.setColor(new Color(0xA7B07A));
        Path2D h1 = new Path2D.Double();
        h1.moveTo(0, 380); h1.curveTo(260, 290, 520, 330, 760, 360); h1.curveTo(1000, 300, 1300, 280, 1600, 350);
        h1.lineTo(1600, 640); h1.lineTo(0, 640); h1.closePath();
        g.fill(h1);
        g.setColor(new Color(0x8A9A62));
        Path2D h2 = new Path2D.Double();
        h2.moveTo(0, 430); h2.curveTo(300, 380, 600, 410, 900, 420); h2.curveTo(1200, 390, 1400, 400, 1600, 420);
        h2.lineTo(1600, 640); h2.lineTo(0, 640); h2.closePath();
        g.fill(h2);
        // 기하학 정원수
        Color tree = new Color(0x4F6B3E), treeHi = new Color(0x63814C), trunk = new Color(0x6B4A2C);
        for (int[] t : new int[][]{{170, 0}, {420, 1}, {1180, 2}, {1440, 0}, {980, 1}}) {
            int x = t[0];
            g.setColor(trunk);
            g.fillRect(x - 9, 380, 18, 90);
            g.setColor(tree);
            switch (t[1]) {
                case 0: g.fill(ell(x, 330, 150, 150)); g.setColor(treeHi); g.fill(ell(x - 25, 305, 60, 60)); break;
                case 1: g.fill(geomystery.ui.Ui.poly(x, 190, x + 80, 400, x - 80, 400)); g.setColor(treeHi); g.fill(geomystery.ui.Ui.poly(x, 220, x + 30, 300, x - 30, 300)); break;
                default: g.fill(new Rectangle2D.Double(x - 70, 260, 140, 140)); g.setColor(treeHi); g.fill(new Rectangle2D.Double(x - 50, 280, 40, 40));
            }
        }
        // 땅
        g.setColor(new Color(0x7E9A5A));
        g.fillRect(0, 470, 1600, 170);
        g.setColor(new Color(0x6E8A4C));
        g.fillRect(0, 470, 1600, 8);
        // 돌길
        g.setColor(new Color(0xD8C39A));
        g.fill(geomystery.ui.Ui.poly(720, 470, 880, 470, 1060, 640, 540, 640));
        g.setColor(new Color(0xBFA77A));
        g.setStroke(new BasicStroke(3));
        for (int y = 500; y < 640; y += 36) g.draw(new Line2D.Double(800 - (y - 470) * 1.6, y, 800 + (y - 470) * 1.6, y));
        // 끊어진 울타리 말뚝
        Color post = new Color(0x8A6038);
        Random r = new Random(4);
        for (int i = 0; i < 12; i++) {
            int x = 60 + i * 135 + (i > 5 ? 120 : 0);
            if (x > 640 && x < 960) continue;
            int y = 450 + r.nextInt(8);
            g.setColor(post);
            g.fill(new RoundRectangle2D.Double(x - 8, y - 60, 16, 70, 6, 6));
            g.setColor(new Color(0x5E3F22));
            g.setStroke(new BasicStroke(2.5f));
            g.draw(new RoundRectangle2D.Double(x - 8, y - 60, 16, 70, 6, 6));
            if (i % 3 != 1) {
                g.setColor(new Color(0xE9D7A9));
                g.setStroke(round(3));
                g.draw(new QuadCurve2D.Double(x + 8, y - 40, x + 30, y - 20, x + 40, y - 6));
            }
        }
        // 분수대
        g.setColor(new Color(0xBFB29A));
        g.fill(ell(800, 470, 260, 60));
        g.setColor(new Color(0x9C8F78));
        g.setStroke(new BasicStroke(3));
        g.draw(ell(800, 470, 260, 60));
        g.setColor(new Color(0x9CC3C9));
        g.fill(ell(800, 466, 220, 40));
        g.setColor(new Color(0xBFB29A));
        g.fill(new Rectangle2D.Double(788, 380, 24, 86));
        g.fill(ell(800, 380, 80, 22));
        // 꽃
        for (int i = 0; i < 60; i++) {
            int x = r.nextInt(1600), y = 500 + r.nextInt(130);
            if (x > 520 && x < 1080 && y < 640 - (x > 800 ? x - 800 : 800 - x) * 0.4) continue;
            g.setColor(i % 3 == 0 ? new Color(0xE7C25A) : i % 3 == 1 ? new Color(0xD9826A) : new Color(0xF3E7CA));
            g.fill(ell(x, y, 9, 9));
        }
        vignette(g);
    }

    // ------------------------------------------------------------------ 배경: 시계탑 태엽실
    static void clockTower(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x7E5532), 0, 520, new Color(0x6B4527)));
        g.fillRect(0, 0, 1600, 640);
        g.setColor(new Color(0x5A3A22));
        g.fillRect(170, 0, 44, 520);
        g.fillRect(1386, 0, 44, 520);
        // 빛 기둥
        g.setColor(new Color(255, 220, 150, 30));
        g.fill(geomystery.ui.Ui.poly(690, 300, 910, 300, 1060, 520, 540, 520));
        // 톱니바퀴
        g.setColor(new Color(0x9A6B3C));
        g.fill(gear(360, 230, 92, 14, 22));
        g.setColor(new Color(0x7E5532));
        g.fill(ell(360, 230, 130, 130));
        g.setColor(new Color(0x9A6B3C));
        g.setStroke(new BasicStroke(10));
        g.draw(ell(360, 230, 130, 130));
        g.setColor(new Color(0xE8C26A));
        g.fill(ell(360, 230, 34, 34));
        g.setColor(new Color(0x8E6236));
        g.fill(gear(500, 380, 58, 11, 16));
        g.setColor(new Color(0x5A3A22));
        g.fill(ell(500, 380, 66, 66));
        g.setColor(new Color(0xE8C26A));
        g.fill(ell(500, 380, 24, 24));
        // 램프
        for (int x : new int[]{580, 1020}) {
            g.setColor(new Color(255, 220, 140, 40));
            g.fill(ell(x, 130, 110, 110));
            line(g, OL, 3, x, 0, x, 92);
            shape(g, geomystery.ui.Ui.poly(x - 22, 96, x + 22, 96, x + 15, 140, x - 15, 140), new Color(0xF2C75A), 4f);
        }
        // 대형 시계
        g.setColor(OL);
        g.fill(ell(800, 250, 470, 470));
        g.setColor(new Color(0xF3D88A));
        g.fill(ell(800, 250, 420, 420));
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(4));
        g.draw(ell(800, 250, 376, 376));
        g.setColor(new Color(0x9A6B3C));
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            g.fill(ell(800 + 162 * Math.cos(a), 250 + 162 * Math.sin(a), 18, 18));
        }
        // 바늘과 중심축은 대화 장면에서 실제 시각에 맞춰 그린다 (DialogueScreen: CLOCK_X/Y)
        // 책장
        g.setColor(new Color(0x4A2F1B));
        g.fill(new Rectangle2D.Double(1140, 100, 270, 420));
        g.setColor(new Color(0x5E3D24));
        for (int s = 0; s < 4; s++) g.fill(new Rectangle2D.Double(1152, 112 + s * 100, 246, 88));
        Color[] bc = {new Color(0xA8382A), new Color(0x2F6E5E), new Color(0xC9952E), new Color(0xD8C49A), new Color(0x6A4A8C)};
        Random r = new Random(7);
        for (int s = 0; s < 4; s++) {
            int x = 1160;
            while (x < 1380) {
                int w = 16 + r.nextInt(14), h = 50 + r.nextInt(32);
                if (r.nextInt(6) == 0) { x += 40; continue; }
                g.setColor(bc[r.nextInt(bc.length)]);
                g.fill(new Rectangle2D.Double(x, 112 + s * 100 + 88 - h, w, h));
                x += w + 3;
            }
        }
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(6));
        g.draw(ell(1350, 260, 46, 46));
        // 바닥
        g.setColor(new Color(0x553620));
        g.fillRect(0, 520, 1600, 120);
        g.setColor(new Color(0x3F2715));
        g.fillRect(0, 516, 1600, 8);
        g.setStroke(new BasicStroke(2));
        for (int y = 560; y < 640; y += 44) g.draw(new Line2D.Double(0, y, 1600, y));
        vignette(g);
    }

    // ------------------------------------------------------------------ 배경: 삼각형의 수도원
    static void monastery(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x8A8273), 0, 520, new Color(0x6C655A)));
        g.fillRect(0, 0, 1600, 640);
        g.setColor(new Color(0x5E584E));
        g.setStroke(new BasicStroke(2));
        for (int y = 0; y < 520; y += 48) {
            g.draw(new Line2D.Double(0, y, 1600, y));
            for (int x = (y / 48) % 2 == 0 ? 0 : 60; x < 1600; x += 120) g.draw(new Line2D.Double(x, y, x, y + 48));
        }
        // 측면 뾰족 아치 창
        for (int x : new int[]{230, 1370}) {
            g.setColor(new Color(0x4A443B));
            g.fill(geomystery.ui.Ui.poly(x - 90, 470, x - 90, 210, x, 90, x + 90, 210, x + 90, 470));
            int k = 0;
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 2; col++) {
                    g.setColor(GLASS[(k++) % GLASS.length]);
                    g.fill(new Rectangle2D.Double(x - 72 + col * 74, 220 + row * 60, 70, 56));
                }
            }
            g.setColor(GLASS[2]);
            g.fill(geomystery.ui.Ui.poly(x - 72, 212, x, 110, x + 72, 212));
            g.setColor(new Color(0x2E2A24));
            g.setStroke(new BasicStroke(5));
            g.draw(geomystery.ui.Ui.poly(x - 90, 470, x - 90, 210, x, 90, x + 90, 210, x + 90, 470));
        }
        // 중앙 시에르핀스키 삼각형 스테인드글라스
        g.setColor(new Color(255, 240, 200, 40));
        g.fill(geomystery.ui.Ui.poly(800, 70, 1050, 520, 550, 520));
        sierpinski(g, 800, 70, 1040, 470, 560, 470, 4);
        g.setColor(new Color(0x2E2A24));
        g.setStroke(round(8));
        g.draw(geomystery.ui.Ui.poly(800, 70, 1040, 470, 560, 470));
        // 촛대
        for (int x : new int[]{470, 1130}) {
            g.setColor(new Color(0xB8903A));
            g.fill(new Rectangle2D.Double(x - 5, 380, 10, 140));
            g.fill(ell(x, 520, 60, 14));
            g.fill(new RoundRectangle2D.Double(x - 40, 372, 80, 10, 6, 6));
            for (int dx : new int[]{-30, 0, 30}) {
                g.setColor(new Color(0xF3E6C4));
                g.fill(new Rectangle2D.Double(x + dx - 6, 336, 12, 38));
                g.setColor(new Color(255, 200, 90, 70));
                g.fill(ell(x + dx, 322, 40, 40));
                g.setColor(new Color(0xF2A23A));
                g.fill(ell(x + dx, 324, 10, 18));
            }
        }
        // 바닥 타일
        g.setColor(new Color(0x4C463D));
        g.fillRect(0, 520, 1600, 120);
        for (int x = 0, i = 0; x < 1600; x += 80, i++) {
            for (int y = 520, j = 0; y < 640; y += 40, j++) {
                if ((i + j) % 2 == 0) {
                    g.setColor(new Color(0x5C554A));
                    g.fill(new Rectangle2D.Double(x, y, 80, 40));
                }
            }
        }
        g.setColor(new Color(0x2E2A24));
        g.fillRect(0, 516, 1600, 6);
        vignette(g);
    }

    // ------------------------------------------------------------------ 4장 의뢰인: 너구리 천문학자 루나 박사
    static void luna(Graphics2D g) {
        Color fur = new Color(0x9A938C), dark = new Color(0x3A3A44), white = new Color(0xF2EEE6), robe = new Color(0x2E3A66);
        shoulders(g, robe);
        // 어깨의 별 무늬와 초승달 브로치
        g.setColor(new Color(0xE6C65A));
        for (double[] st : new double[][]{{120, 280}, {150, 262}, {226, 270}, {250, 288}, {196, 290}}) g.fill(Icons_star(st[0], st[1], 6, 2.6));
        Path2D moon = new Path2D.Double();
        moon.append(new Arc2D.Double(166, 252, 28, 28, 60, 240, Arc2D.OPEN), false);
        moon.append(new Arc2D.Double(174, 254, 22, 24, 290, -220, Arc2D.OPEN), true);
        moon.closePath();
        shape(g, moon, new Color(0xE6C65A), 2f);
        // 귀
        shape(g, ell(122, 130, 46, 46), fur, 3f);
        shape(g, ell(238, 130, 46, 46), fur, 3f);
        shape(g, ell(122, 132, 24, 24), dark, 0);
        shape(g, ell(238, 132, 24, 24), dark, 0);
        shape(g, ell(180, 190, 150, 132), fur, 3f);
        // 너구리 가면
        Path2D mask = new Path2D.Double();
        mask.moveTo(112, 176); mask.quadTo(140, 154, 172, 178); mask.quadTo(180, 186, 188, 178); mask.quadTo(220, 154, 248, 176);
        mask.quadTo(238, 206, 206, 202); mask.quadTo(180, 196, 154, 202); mask.quadTo(122, 206, 112, 176); mask.closePath();
        shape(g, mask, dark, 0);
        shape(g, ell(156, 150, 40, 12), white, 0);
        shape(g, ell(204, 150, 40, 12), white, 0);
        Path2D muzzle = new Path2D.Double();
        muzzle.moveTo(146, 212); muzzle.quadTo(180, 196, 214, 212); muzzle.quadTo(210, 246, 180, 248); muzzle.quadTo(150, 246, 146, 212); muzzle.closePath();
        shape(g, muzzle, white, 2f);
        shape(g, ell(156, 184, 24, 24), white, 0);
        shape(g, ell(204, 184, 24, 24), white, 0);
        eye(g, 156, 184, 6.5);
        eye(g, 204, 184, 6.5);
        shape(g, ell(180, 214, 18, 12), OL, 0);
        g.setColor(OL);
        g.setStroke(round(2.2f));
        g.draw(new Arc2D.Double(170, 222, 20, 12, 200, 140, Arc2D.OPEN));
        whiskers(g, new Color(0x5A5650), 180, 222, 26);
        blush(g, 134, 226, 214);
        // 이마 위 놋쇠 고글
        shape(g, new RoundRectangle2D.Double(116, 122, 128, 12, 8, 8), new Color(0x6B4A2E), 2f);
        for (double x : new double[]{150, 210}) {
            shape(g, ell(x, 126, 40, 34), new Color(0xC9A456), 3f);
            shape(g, ell(x, 126, 26, 22), new Color(0x8DB7DA), 2f);
            g.setColor(new Color(255, 255, 255, 150));
            g.fill(ell(x - 5, 121, 8, 6));
        }
    }

    // ------------------------------------------------------------------ 4장 용의자: 토끼 조수 코코
    static void rabbit(Graphics2D g) {
        Color fur = new Color(0xF4EFE6), pink = new Color(0xEFA9AE), coat = new Color(0x6F9CC9);
        shoulders(g, coat);
        shape(g, new RoundRectangle2D.Double(150, 250, 60, 50, 10, 10), new Color(0xF2E6C8), 2.5f);
        line(g, new Color(0xC9B48A), 2f, 160, 268, 200, 268);
        // 귀
        shape(g, rot(ell(150, 92, 40, 128), -10, 150, 92), fur, 3f);
        shape(g, rot(ell(212, 88, 40, 128), 14, 212, 88), fur, 3f);
        g.setColor(pink);
        g.fill(rot(ell(150, 96, 18, 96), -10, 150, 96));
        g.fill(rot(ell(212, 92, 18, 96), 14, 212, 92));
        shape(g, ell(180, 196, 140, 124), fur, 3f);
        eye(g, 158, 190, 7.5);
        eye(g, 202, 190, 7.5);
        shape(g, geomystery.ui.Ui.poly(172, 210, 188, 210, 180, 220), pink, 2f);
        g.setColor(OL);
        g.setStroke(round(2.2f));
        g.draw(new Line2D.Double(180, 220, 180, 228));
        g.draw(new Arc2D.Double(168, 222, 12, 12, 200, 140, Arc2D.OPEN));
        g.draw(new Arc2D.Double(180, 222, 12, 12, 200, 140, Arc2D.OPEN));
        shape(g, new RoundRectangle2D.Double(174, 230, 12, 10, 3, 3), Color.WHITE, 1.5f);
        whiskers(g, new Color(0xB8AFA2), 180, 218, 24);
        blush(g, 136, 224, 214);
        // 머리 위 별 모양 머리핀
        g.setColor(new Color(0xE6C65A));
        g.fill(Icons_star(214, 146, 11, 5));
        g.setColor(OL);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(Icons_star(214, 146, 11, 5));
    }

    // ------------------------------------------------------------------ 4장 용의자: 수달 렌즈공 오토
    static void otter(Graphics2D g) {
        Color fur = new Color(0x8B5E3C), light = new Color(0xE7D3B0), apron = new Color(0x7A5232);
        shoulders(g, new Color(0x5E6B4A));
        Path2D ap = new Path2D.Double();
        ap.moveTo(136, 252); ap.lineTo(224, 252); ap.lineTo(236, 300); ap.lineTo(124, 300); ap.closePath();
        shape(g, ap, apron, 2.5f);
        line(g, new Color(0x5A3C26), 3f, 140, 252, 150, 236);
        line(g, new Color(0x5A3C26), 3f, 220, 252, 210, 236);
        shape(g, ell(196, 276, 16, 16), new Color(0x8DB7DA), 2f);
        shape(g, ell(122, 150, 28, 24), fur, 3f);
        shape(g, ell(238, 150, 28, 24), fur, 3f);
        shape(g, ell(180, 192, 150, 128), fur, 3f);
        Path2D face = new Path2D.Double();
        face.moveTo(128, 206); face.quadTo(180, 176, 232, 206); face.quadTo(226, 250, 180, 252); face.quadTo(134, 250, 128, 206); face.closePath();
        shape(g, face, light, 0);
        eye(g, 156, 186, 6);
        eye(g, 204, 186, 6);
        shape(g, ell(180, 210, 26, 16), OL, 0);
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(ell(174, 206, 8, 4));
        g.setColor(OL);
        g.setStroke(round(2.2f));
        g.draw(new Arc2D.Double(168, 218, 24, 14, 200, 140, Arc2D.OPEN));
        whiskers(g, new Color(0x5A3C26), 180, 220, 30);
        blush(g, 136, 224, 214);
        // 한쪽 눈의 확대경(루페)
        shape(g, ell(204, 186, 34, 34), new Color(0xC9A456), 3f);
        g.setColor(new Color(141, 183, 218, 90));
        g.fill(ell(204, 186, 24, 24));
        g.setColor(new Color(255, 255, 255, 160));
        g.fill(ell(198, 180, 7, 5));
        line(g, new Color(0x6B4A2E), 2f, 220, 190, 246, 168);
    }

    // ------------------------------------------------------------------ 4장 용의자: 양 기록관 메리
    static void sheep(Graphics2D g) {
        Color wool = new Color(0xF4EEDF), face = new Color(0xE8D2B0), card = new Color(0x8A3A4A);
        shoulders(g, card);
        for (double x = 150; x <= 210; x += 30) shape(g, ell(x, 252, 12, 12), new Color(0xE6C65A), 1.5f);
        // 귀(옆으로)
        shape(g, rot(ell(112, 196, 50, 24), 20, 112, 196), face, 3f);
        shape(g, rot(ell(248, 196, 50, 24), -20, 248, 196), face, 3f);
        // 털 구름
        for (double[] w : new double[][]{{124, 160, 52}, {150, 128, 56}, {180, 116, 58}, {210, 128, 56}, {236, 160, 52}, {120, 196, 40}, {240, 196, 40}}) {
            shape(g, ell(w[0], w[1], w[2], w[2]), wool, 3f);
        }
        shape(g, ell(180, 204, 112, 112), face, 3f);
        for (double[] w : new double[][]{{150, 140, 46}, {180, 134, 50}, {210, 140, 46}}) shape(g, ell(w[0], w[1], w[2], w[2] * 0.9), wool, 2.5f);
        eye(g, 158, 192, 6);
        eye(g, 202, 192, 6);
        // 동그란 안경
        g.setColor(OL);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(ell(158, 192, 30, 28));
        g.draw(ell(202, 192, 30, 28));
        g.draw(new Line2D.Double(173, 190, 187, 190));
        shape(g, ell(180, 222, 22, 12), new Color(0x8A6A58), 0);
        g.setColor(OL);
        g.setStroke(round(2.2f));
        g.draw(new Arc2D.Double(170, 226, 20, 12, 200, 140, Arc2D.OPEN));
        blush(g, 140, 220, 220);
        // 귀 뒤에 꽂은 깃펜
        Path2D quill = new Path2D.Double();
        quill.moveTo(252, 214); quill.quadTo(286, 150, 300, 110); quill.quadTo(276, 150, 258, 218); quill.closePath();
        shape(g, quill, new Color(0xF2E6C8), 2f);
        line(g, new Color(0x6B4A2E), 1.5f, 256, 214, 296, 116);
    }

    /** 별 모양(아이콘 클래스를 쓰지 않고 굽기 전용으로). */
    private static Shape Icons_star(double cx, double cy, double r1, double r2) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? r1 : r2;
            double a = Math.toRadians(-90 + i * 36);
            double x = cx + r * Math.cos(a), y = cy + r * Math.sin(a);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    // ------------------------------------------------------------------ 4장 배경: 원의 천문대
    static void observatory(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x1E2340), 0, 520, new Color(0x34304E)));
        g.fillRect(0, 0, 1600, 640);
        // 돔 갈빗대(꼭대기로 모이는 곡선)
        g.setStroke(round(6));
        for (int i = -6; i <= 6; i++) {
            if (Math.abs(i) <= 1) continue;
            double x = 800 + i * 140;
            g.setColor(new Color(0x2A2846));
            g.draw(new QuadCurve2D.Double(x, 520, 800 + i * 60, 120, 800, -160));
            g.setColor(new Color(0x47436A));
            g.setStroke(new BasicStroke(2));
            g.draw(new QuadCurve2D.Double(x + 6, 520, 800 + i * 60 + 4, 120, 800, -160));
            g.setStroke(round(6));
        }
        g.setColor(new Color(0x26243F));
        g.fillRect(0, 470, 1600, 50);
        // 열린 틈새: 밤하늘
        Shape slit = new RoundRectangle2D.Double(690, -40, 220, 360, 60, 60);
        g.setPaint(new GradientPaint(0, 0, new Color(0x0B1030), 0, 320, new Color(0x223163)));
        g.fill(slit);
        Graphics2D sk = (Graphics2D) g.create();
        sk.clip(slit);
        sk.setColor(new Color(150, 170, 230, 40));
        sk.fill(rot(ell(800, 140, 120, 420), 28, 800, 140));
        java.util.Random r = new java.util.Random(4);
        for (int i = 0; i < 90; i++) {
            double x = 690 + r.nextDouble() * 220, y = r.nextDouble() * 320, sz = 1 + r.nextDouble() * 2.4;
            sk.setColor(new Color(255, 250, 230, 120 + r.nextInt(130)));
            sk.fill(ell(x, y, sz, sz));
        }
        sk.dispose();
        g.setColor(new Color(0x8C6A3A));
        g.setStroke(new BasicStroke(10));
        g.draw(slit);
        // 왼쪽 벽: 별자리 지도 두루마리
        shape(g, new Rectangle2D.Double(250, 110, 260, 230), new Color(0xE9D8B2), 4f);
        shape(g, new RoundRectangle2D.Double(238, 100, 284, 18, 10, 10), new Color(0x8C6A3A), 3f);
        shape(g, new RoundRectangle2D.Double(238, 332, 284, 18, 10, 10), new Color(0x8C6A3A), 3f);
        g.setColor(new Color(0x3E4F7A));
        g.setStroke(new BasicStroke(2f));
        double[][] cons = {{290, 160}, {340, 200}, {390, 180}, {440, 230}, {470, 290}, {410, 300}, {340, 270}};
        for (int i = 0; i + 1 < cons.length; i++) g.draw(new Line2D.Double(cons[i][0], cons[i][1], cons[i + 1][0], cons[i + 1][1]));
        g.draw(ell(380, 225, 190, 150));
        for (double[] c : cons) { g.setColor(new Color(0xC9A456)); g.fill(Icons_star(c[0], c[1], 8, 3.4)); }
        // 왼쪽 선반과 지구본
        shape(g, new Rectangle2D.Double(60, 400, 200, 14), new Color(0x6B4A2E), 3f);
        shape(g, ell(130, 360, 70, 70), new Color(0x4E8DA8), 3f);
        g.setColor(new Color(0x7FAE6A));
        g.fill(ell(120, 350, 28, 22));
        g.fill(ell(146, 372, 20, 16));
        g.setColor(new Color(0xC9A456));
        g.setStroke(new BasicStroke(4));
        g.draw(new Arc2D.Double(88, 318, 84, 84, 60, 240, Arc2D.OPEN));
        shape(g, new Rectangle2D.Double(124, 394, 12, 8), new Color(0xC9A456), 2f);
        // 큰 망원경(가운데 바닥에서 틈새를 향해)
        Graphics2D t = (Graphics2D) g.create();
        t.translate(820, 430);
        t.rotate(Math.toRadians(-58));
        shape(t, new RoundRectangle2D.Double(-40, -34, 400, 68, 20, 20), new Color(0xB8903A), 4f);
        shape(t, new RoundRectangle2D.Double(340, -44, 70, 88, 16, 16), new Color(0xC9A456), 4f);
        shape(t, new RoundRectangle2D.Double(-90, -22, 60, 44, 10, 10), new Color(0x8C6A3A), 4f);
        for (int x = 40; x < 340; x += 90) shape(t, new Rectangle2D.Double(x, -36, 14, 72), new Color(0x8C6A3A), 3f);
        t.setColor(new Color(255, 255, 255, 60));
        t.fill(new RoundRectangle2D.Double(-30, -26, 370, 12, 10, 10));
        t.dispose();
        // 받침대(삼각대)
        g.setColor(new Color(0x5A3A22));
        g.setStroke(round(12));
        g.draw(new Line2D.Double(820, 430, 740, 560));
        g.draw(new Line2D.Double(820, 430, 900, 560));
        g.draw(new Line2D.Double(820, 430, 822, 570));
        shape(g, ell(820, 430, 44, 44), new Color(0xC9A456), 4f);
        // 오른쪽: 태양계 모형(고리는 그림, 행성은 장면 연출에서 돈다)
        shape(g, new Rectangle2D.Double(1310, 330, 20, 150), new Color(0x8C6A3A), 3f);
        shape(g, ell(1320, 490, 120, 26), new Color(0x6B4A2E), 3f);
        g.setColor(new Color(0xC9A456));
        g.setStroke(new BasicStroke(3));
        for (int rr : new int[]{50, 85, 120}) g.draw(ell(1320, 300, rr * 2, rr * 0.7));
        shape(g, ell(1320, 300, 40, 40), new Color(0xF2C04A), 3f);
        // 바닥 나무 널빤지와 별 무늬 깔개
        g.setColor(new Color(0x4A3020));
        g.fillRect(0, 520, 1600, 120);
        g.setColor(new Color(0x3A2416));
        g.setStroke(new BasicStroke(2));
        for (int y = 540; y < 640; y += 26) g.draw(new Line2D.Double(0, y, 1600, y));
        for (int x = 0; x < 1600; x += 160) g.draw(new Line2D.Double(x + (x / 160 % 2) * 60, 520, x + (x / 160 % 2) * 60, 640));
        g.setColor(new Color(0x2E3A66));
        g.fill(ell(820, 590, 520, 90));
        g.setColor(new Color(0xC9A456));
        g.setStroke(new BasicStroke(3));
        g.draw(ell(820, 590, 480, 76));
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            g.fill(Icons_star(820 + Math.cos(a) * 190, 590 + Math.sin(a) * 28, 9, 4));
        }
        g.setColor(new Color(0x1A1430));
        g.fillRect(0, 516, 1600, 6);
        vignette(g);
    }

    private static void sierpinski(Graphics2D g, double ax, double ay, double bx, double by, double cx, double cy, int depth) {
        if (depth == 0) {
            int k = (int) Math.abs((ax * 7 + ay * 13) / 10) % GLASS.length;
            g.setColor(GLASS[k]);
            g.fill(geomystery.ui.Ui.poly(ax, ay, bx, by, cx, cy));
            g.setColor(new Color(0x2E2A24));
            g.setStroke(new BasicStroke(3));
            g.draw(geomystery.ui.Ui.poly(ax, ay, bx, by, cx, cy));
            return;
        }
        double abx = (ax + bx) / 2, aby = (ay + by) / 2, bcx = (bx + cx) / 2, bcy = (by + cy) / 2, cax = (cx + ax) / 2, cay = (cy + ay) / 2;
        sierpinski(g, ax, ay, abx, aby, cax, cay, depth - 1);
        sierpinski(g, abx, aby, bx, by, bcx, bcy, depth - 1);
        sierpinski(g, cax, cay, bcx, bcy, cx, cy, depth - 1);
        g.setColor(new Color(0x3B352D));
        g.fill(geomystery.ui.Ui.poly(abx, aby, bcx, bcy, cax, cay));
    }

    private static void vignette(Graphics2D g) {
        RadialGradientPaint p = new RadialGradientPaint(new Point2D.Double(800, 320), 900,
                new float[]{0.55f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(30, 18, 8, 120)});
        g.setPaint(p);
        g.fillRect(0, 0, 1600, 640);
    }

    // ------------------------------------------------------------------ 앱 아이콘
    private static void appIcon(Path dir) throws IOException {
        BufferedImage im = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = hq(im);
        g.setColor(OL);
        g.fill(new RoundRectangle2D.Double(8, 8, 240, 240, 56, 56));
        g.setColor(GOLD);
        g.setStroke(new BasicStroke(8));
        g.draw(ell(128, 128, 170, 170));
        Path2D s = new Path2D.Double();
        double[] r = {78, 18};
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(-90 + i * 45);
            double rr = r[i % 2];
            double x = 128 + rr * Math.cos(a), y = 128 + rr * Math.sin(a);
            if (i == 0) s.moveTo(x, y); else s.lineTo(x, y);
        }
        s.closePath();
        g.fill(s);
        g.setColor(new Color(0xA33A28));
        g.fill(ell(128, 128, 22, 22));
        g.dispose();
        ImageIO.write(im, "png", dir.resolve("app_icon.png").toFile());
    }
}
