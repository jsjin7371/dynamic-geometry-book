package geomystery.ui;

import geomystery.data.Content;
import geomystery.data.Model.*;
import geomystery.diagram.DiagramView;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사건의 증거 사진: 그 사건 마지막 수수께끼의 칠판(보조선과 정답까지 쓴 상태)을 찍은 폴라로이드.
 * 결과 화면과 사건 보드가 함께 쓴다. 크기별로 한 번만 그려 두고(캐시) 다시 그리지 않는다.
 * 백그라운드 스레드(사건 보드 그리기)에서도 부를 수 있다.
 */
final class EvidencePhoto {
    private EvidencePhoto() {}

    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();

    /** 칠판 사진(픽셀 크기 dw×dh). 도형이 없으면 null. */
    static BufferedImage of(Content c, Lesson l, int dw, int dh) {
        if (l == null || dw <= 0 || dh <= 0) return null;
        List<Puzzle> ps = c.puzzlesOf(l);
        if (ps.isEmpty()) return null;
        Puzzle p = ps.get(ps.size() - 1);
        List<Cmd> script = c.diagrams.get(p.diagram);
        if (script == null) return null;
        String key = l.id + ":" + dw + "x" + dh;
        BufferedImage hit = CACHE.get(key);
        if (hit != null) return hit;
        BufferedImage im = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = Theme.aa(im.createGraphics());
        g.setPaint(new GradientPaint(0, 0, new Color(0x356A5B), 0, dh, new Color(0x274F44)));
        g.fillRect(0, 0, dw, dh);
        // 칠판 전체가 보이도록 맞추고(남는 곳은 칠판색), 가운데에 둔다
        double s = DiagramView.fit(dw, dh) * 1.08;
        Graphics2D d = (Graphics2D) g.create();
        d.translate((dw - DiagramView.W * s) / 2, (dh - DiagramView.H * s) / 2);
        d.scale(s, s);
        DiagramView.Opts o = new DiagramView.Opts();
        o.aux = 1f;
        o.drawIn = false;
        if (p.isNumber() || p.isExpr()) {
            String u = p.unit == null ? "" : p.unit.trim();
            o.reveal = 1f;
            o.answer = p.answer + (u.equals("°") || u.equals("m") || u.equals("cm") ? u : "");
        }
        try {
            DiagramView.render(d, script, o);
        } catch (RuntimeException ignored) {
            // 그림 오류는 칠판만 보여 준다
        }
        d.dispose();
        // 사진 느낌: 따뜻한 색조와 가장자리 그늘
        g.setColor(new Color(255, 214, 160, 28));
        g.fillRect(0, 0, dw, dh);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(dw / 2f, dh / 2f), Math.max(dw, dh) * 0.75f,
                new float[]{0.55f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(10, 20, 15, 120)}));
        g.fillRect(0, 0, dw, dh);
        g.dispose();
        CACHE.put(key, im);
        return im;
    }

    /**
     * 폴라로이드 한 장을 그린다(x, y, w, h 는 논리 좌표, 회전 없이). photoAlpha 는 사진의 현상 정도(0이면 캄캄).
     * caption 은 아래 여백의 손글씨.
     */
    static void paint(Graphics2D g, Content c, Lesson l, double x, double y, double w, double h, float photo, String caption) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Rectangle2D.Double(x + 4, y + 6, w, h));
        g.setColor(new Color(0xFBF7EE));
        g.fill(new Rectangle2D.Double(x, y, w, h));
        g.setColor(new Color(0xE6DCC8));
        g.setStroke(new BasicStroke(1f));
        g.draw(new Rectangle2D.Double(x, y, w, h));
        double pad = Math.max(6, w * 0.06);
        double iw = w - pad * 2, ih = h * 0.72 - pad;
        double sc = Ui.deviceScale(g);
        BufferedImage im = of(c, l, (int) Math.round(iw * sc), (int) Math.round(ih * sc));
        if (im != null) Ui.blit(g, im, x + pad, y + pad, iw, ih);
        if (photo < 1f) {
            // 현상 중: 짙은 갈색에서 서서히 드러난다
            g.setColor(new Color(58 / 255f, 40 / 255f, 25 / 255f, 1f - Math.max(0f, photo)));
            g.fill(new Rectangle2D.Double(x + pad, y + pad, iw, ih));
        }
        if (caption != null && !caption.isEmpty()) {
            g.setFont(Theme.hand((float) Math.min(24, h * 0.1)));
            g.setColor(new Color(0x3A2819));
            Theme.drawCentered(g, caption, x + w / 2, y + pad + ih + (h - pad - ih) / 2);
        }
    }
}
