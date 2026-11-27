package geomystery.ui;

import geomystery.data.Model.*;
import geomystery.game.Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 엔딩 — 마지막 사건을 해결하면 등장인물·모은 정리·탐정 기록이 차례로 올라가는 크레딧.
 * 클릭(또는 Space)하면 빨리 감고, Esc 나 끝의 단추로 지도로 돌아간다.
 */
public class EndingScreen extends JPanel {
    private final MainFrame frame;
    private final Game game;
    private final List<Item> items = new ArrayList<>();
    private final javax.swing.Timer timer;
    private double scroll, speed = 55;
    private long last;
    private int total;
    private final Btn back;

    private static final class Item {
        final String kind;      // title, head, text, small, faces, cards, gap
        final String text;
        final List<String[]> people = new ArrayList<>();
        final List<Theorem> cards = new ArrayList<>();
        final int height;

        Item(String kind, String text, int height) { this.kind = kind; this.text = text; this.height = height; }
    }

    public EndingScreen(MainFrame frame) {
        this.frame = frame;
        this.game = frame.game;
        setLayout(null);
        setBackground(new Color(0x140E09));
        back = new Btn("탐험 지도로 돌아가기", Btn.Style.CREAM).font(16);
        back.addActionListener(e -> finish());
        back.setVisible(false);
        add(back);
        timer = new javax.swing.Timer(16, e -> tick());
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { speed = 260; }
            @Override public void mouseReleased(MouseEvent e) { speed = 55; }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "skip");
        getActionMap().put("skip", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (isShowing()) finish(); }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed SPACE"), "fast");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("released SPACE"), "slow");
        getActionMap().put("fast", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { speed = 260; } });
        getActionMap().put("slow", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { speed = 55; } });
    }

    @Override
    public void doLayout() {
        Dimension p = back.getPreferredSize();
        back.setBounds((getWidth() - p.width - 40) / 2, getHeight() - p.height - 46, p.width + 40, p.height);
    }

    public void start() {
        items.clear();
        add(new Item("gap", null, 120));
        add(new Item("hanja", "幾 何 迷 宮", 40));
        add(new Item("title", "모든 수수께끼가 풀렸다", 90));
        add(new Item("text", "선의 정원에서 시작된 작은 사건은 시계탑과 수도원을 지나 천문대의 별빛 아래에서 막을 내렸다.", 40));
        add(new Item("text", "보이지 않는 선을 그을 줄 아는 탐정, " + game.save.name + "에게 감사를.", 40));
        add(new Item("gap", null, 120));
        add(new Item("head", "등장인물", 70));
        String[][] groups = {{"hazel", "minerva", "gregor", "toru", "luna"}, {"mole", "crow", "pigeon", "cat", "mouse"}, {"fox", "bat", "hedgehog", "rabbit", "otter", "sheep"}};
        for (String[] gp : groups) {
            Item row = new Item("faces", null, 190);
            for (String id : gp) {
                Npc n = game.content.npc(id);
                if (n != null) row.people.add(new String[]{n.image, n.name, n.role});
            }
            add(row);
        }
        add(new Item("gap", null, 80));
        add(new Item("head", "모은 정리 카드", 70));
        List<Theorem> cards = game.cards();
        for (int i = 0; i < cards.size(); i += 5) {
            Item row = new Item("cards", null, 170);
            row.cards.addAll(cards.subList(i, Math.min(cards.size(), i + 5)));
            add(row);
        }
        add(new Item("gap", null, 80));
        add(new Item("head", "탐정 기록", 70));
        add(new Item("text", "해결한 사건 " + game.lessonsDone() + "건  ·  푼 수수께끼 " + game.save.solvedTotal + "개  ·  첫 시도 정답률 " + game.overallAccuracy() + "%", 40));
        add(new Item("text", "밝혀낸 범인 " + game.save.solvedFinales.size() + "명  ·  보조선 " + game.save.auxTotal + "번  ·  최장 연속 학습 " + Math.max(game.save.bestStreak, game.streakNow()) + "일", 40));
        add(new Item("text", game.title() + " Lv." + game.level() + "  ·  누적 " + Ui.num(game.save.xp) + " XP  ·  " + game.tierName(), 40));
        add(new Item("gap", null, 100));
        add(new Item("head", "만든 것들", 70));
        add(new Item("text", "그림 — 모든 인물과 배경은 Java2D 벡터 드로잉으로 구웠습니다", 36));
        add(new Item("text", "소리 — 효과음과 배경음악은 코드로 직접 작곡·합성했습니다", 36));
        add(new Item("text", "글꼴 — 나눔명조 · 나눔손글씨 펜 (SIL Open Font License)", 36));
        add(new Item("gap", null, 160));
        add(new Item("title", "그리고, 수수께끼는 계속된다…", 90));
        add(new Item("small", "수수께끼 상자에서 언제든 다시 풀 수 있어요", 40));
        add(new Item("gap", null, 260));
        total = 0;
        for (Item it : items) total += it.height;
        scroll = 0;
        last = System.currentTimeMillis();
        back.setVisible(false);
        speed = 55;
        game.save.endingSeen = true;
        game.save.save();
        Music.play("observatory");
        Sfx.play("fanfare");
        timer.start();
    }

    private void add(Item it) { items.add(it); }

    private void tick() {
        long now = System.currentTimeMillis();
        double dt = Math.min(0.05, (now - last) / 1000.0);
        last = now;
        double max = total;
        scroll = Math.min(max, scroll + speed * dt);
        if (scroll >= max && !back.isVisible()) back.setVisible(true);
        repaint();
    }

    private void finish() {
        timer.stop();
        frame.showTab("map");
    }

    /** 배경(어두운 정원)과 위아래 그늘은 크기가 바뀔 때만 다시 그린다. 예전에는 매 프레임 배경 전체를 고품질로 축소해 한 프레임에 0.6초가 걸렸다. */
    private BufferedImage bgCache, fadeTop, fadeBottom;
    private int cw, ch;
    private double cs;
    private final Font smallFont = Theme.tracked(Theme.serif(15), 0.3f);

    private void buildCache(Graphics2D like, int w, int h) {
        bgCache = Ui.deviceImage(like, w, h);
        Graphics2D b = Ui.cacheGraphics(bgCache, like);
        b.setColor(new Color(0x140E09));
        b.fillRect(0, 0, w, h);
        BufferedImage bg = Ui.img("bg_garden");
        if (bg != null) {
            double s = Math.max(w / (double) bg.getWidth(), h / (double) bg.getHeight());
            b.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            Ui.drawScaled(b, bg, (w - bg.getWidth() * s) / 2, h - bg.getHeight() * s, bg.getWidth() * s, bg.getHeight() * s);
        }
        b.dispose();
        fadeTop = Ui.deviceImage(like, w, 110);
        Graphics2D t = Ui.cacheGraphics(fadeTop, like);
        t.setPaint(new GradientPaint(0, 0, new Color(20, 14, 9, 255), 0, 110, new Color(20, 14, 9, 0)));
        t.fillRect(0, 0, w, 110);
        t.dispose();
        fadeBottom = Ui.deviceImage(like, w, 140);
        Graphics2D d = Ui.cacheGraphics(fadeBottom, like);
        d.setPaint(new GradientPaint(0, 0, new Color(20, 14, 9, 0), 0, 140, new Color(20, 14, 9, 255)));
        d.fillRect(0, 0, w, 140);
        d.dispose();
        cw = w; ch = h; cs = Ui.deviceScale(like);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.aa(g0.create());
        int w = getWidth(), h = getHeight();
        if (bgCache == null || cw != w || ch != h || (cs != Ui.deviceScale(g) && !Ui.offscreen(g))) buildCache(g, w, h);
        Ui.blit(g, bgCache, 0, 0, w, h);
        // 별
        Random r = new Random(3);
        double t = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < 90; i++) {
            double x = r.nextDouble() * w, y = r.nextDouble() * h;
            float a = (float) (0.25 + 0.3 * Math.sin(t * 1.5 + i));
            g.setColor(new Color(1f, 0.95f, 0.8f, Math.max(0f, a)));
            double sz = 1 + r.nextDouble() * 2;
            g.fill(new Ellipse2D.Double(x, y, sz, sz));
        }
        double y = h - scroll;
        for (Item it : items) {
            if (y + it.height > -50 && y < h + 50) paintItem(g, it, w, y);
            y += it.height;
        }
        // 위아래 어둡게
        Ui.blit(g, fadeTop, 0, 0, w, 110);
        Ui.blit(g, fadeBottom, 0, h - 140, w, 140);
        g.setFont(Theme.serif(12.5f));
        g.setColor(new Color(0x8C7A5C));
        g.drawString("누르고 있으면 빨리 감기 · Esc 건너뛰기", 24, h - 20);
        g.dispose();
    }

    private void paintItem(Graphics2D g, Item it, int w, double y) {
        double cx = w / 2.0, cy = y + it.height / 2.0;
        switch (it.kind) {
            case "title":
                g.setFont(Theme.xbold(44));
                g.setColor(new Color(0xF7EEDC));
                Theme.drawCentered(g, it.text, cx, cy);
                break;
            case "small":
                g.setFont(smallFont);
                g.setColor(Theme.GOLD);
                Theme.drawCentered(g, it.text, cx, cy);
                break;
            case "hanja":
                g.setFont(Theme.hanja(20));
                g.setColor(Theme.GOLD);
                Theme.drawCentered(g, it.text, cx, cy);
                break;
            case "head":
                g.setFont(Theme.hand(36));
                g.setColor(Theme.GOLD);
                Theme.drawCentered(g, it.text, cx, cy);
                g.setStroke(new BasicStroke(1.2f));
                g.drawLine((int) cx - 180, (int) cy + 24, (int) cx + 180, (int) cy + 24);
                break;
            case "text":
                g.setFont(Theme.serif(18));
                g.setColor(new Color(0xE9DCC0));
                Theme.drawCentered(g, it.text, cx, cy);
                break;
            case "faces": {
                int n = it.people.size(), cell = Math.min(210, (w - 40) / Math.max(1, n));
                double x0 = cx - n * cell / 2.0;
                for (int i = 0; i < n; i++) {
                    String[] p = it.people.get(i);
                    double fx = x0 + i * cell + cell / 2.0;
                    double bob = Math.sin(System.currentTimeMillis() / 600.0 + i) * 3;
                    g.setColor(new Color(0xE9D8B2));
                    g.fill(new Ellipse2D.Double(fx - 55, y + 8 + bob, 110, 110));
                    BufferedImage im = Ui.img(p[0] + "_face");
                    if (im != null) Ui.drawScaled(g, im, fx - 53, y + 10 + bob, 106, 106);
                    g.setFont(Theme.bold(16));
                    g.setColor(new Color(0xF7EEDC));
                    Theme.drawCentered(g, p[1], fx, y + 140);
                    g.setFont(Theme.serif(13));
                    g.setColor(new Color(0xB59C78));
                    Theme.drawCentered(g, p[2], fx, y + 164);
                }
                break;
            }
            case "cards": {
                int n = it.cards.size(), cell = 150;
                double x0 = cx - n * cell / 2.0;
                for (int i = 0; i < n; i++) {
                    Theorem th = it.cards.get(i);
                    double fx = x0 + i * cell + cell / 2.0;
                    java.awt.geom.RoundRectangle2D r = new java.awt.geom.RoundRectangle2D.Double(fx - 56, y + 6, 112, 140, 10, 10);
                    g.setColor(new Color(0xF4E8CC));
                    g.fill(r);
                    g.setColor(Theme.GOLD);
                    g.setStroke(new BasicStroke(3f));
                    g.draw(r);
                    Icons.theorem(g, th.icon, new java.awt.geom.Rectangle2D.Double(fx - 34, y + 16, 68, 68), new Color(0x3A2819), new Color(0xA33A28));
                    g.setFont(Theme.bold(12.5f));
                    g.setColor(new Color(0x3A2819));
                    int ly = (int) y + 106;
                    for (String ln : Widgets.TextBlock.wrap(th.name, g.getFontMetrics(), 100)) {
                        Theme.drawCentered(g, ln, fx, ly);
                        ly += 16;
                    }
                }
                break;
            }
            default: break;
        }
    }
}
