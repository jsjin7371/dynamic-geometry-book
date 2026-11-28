package geomystery.ui;

import geomystery.data.Model.Quote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/** 왼쪽 어두운 사이드바: 로고, 메뉴, 오늘의 한마디. */
public class Sidebar extends JPanel {
    private final MainFrame frame;
    private final Map<String, NavItem> items = new LinkedHashMap<>();
    private final Widgets.TextBlock quote;
    private final JLabel quoteAuthor;

    public Sidebar(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(232, 100));
        setBorder(BorderFactory.createEmptyBorder(26, 18, 26, 22));

        JPanel top = Ui.vbox();
        top.add(new Logo());
        top.add(Ui.vgap(22));
        add(item("map", "탐험 지도", "map"), top);
        add(item("box", "수수께끼 상자", "box"), top);
        add(item("assoc", "탐정 협회", "trophy"), top);
        add(item("dex", "도형 도감", "book"), top);
        add(item("analysis", "추리 분석", "bulb"), top);
        add(item("board", "사건 보드", "search"), top);
        add(item("office", "탐정 사무소", "home"), top);
        add(item("notebook", "탐정 수첩", "person"), top);
        add(top, BorderLayout.NORTH);

        RoundPanel card = new RoundPanel(new BorderLayout(0, 10), new Color(0x4A3524), new Color(0x6E5236), 12);
        card.pad(16, 16, 16, 14);
        card.add(Ui.label("오늘의 한마디", Theme.hand(22), Theme.GOLD), BorderLayout.NORTH);
        quote = new Widgets.TextBlock("", Theme.serif(13.5f), new Color(0xE9DCC0)).spacing(1.6f).prefWidth(170);
        card.add(quote, BorderLayout.CENTER);
        quoteAuthor = Ui.label("", Theme.serif(12), new Color(0xB59C78));
        quoteAuthor.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(quoteAuthor, BorderLayout.SOUTH);
        add(card, BorderLayout.SOUTH);
        // 창이 낮으면 메뉴와 겹치지 않도록 '오늘의 한마디'를 숨긴다
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                boolean fits = getHeight() >= top.getPreferredSize().height + card.getPreferredSize().height + 70;
                if (card.isVisible() != fits) { card.setVisible(fits); revalidate(); }
            }
        });
        refresh();
    }

    private void add(NavItem item, JPanel to) {
        to.add(item);
        to.add(Ui.vgap(5));
    }

    private NavItem item(String key, String text, String icon) {
        NavItem it = new NavItem(text, icon, () -> frame.showNav(key));
        items.put(key, it);
        return it;
    }

    public void select(String key) {
        for (Map.Entry<String, NavItem> e : items.entrySet()) e.getValue().setSelected(e.getKey().equals(key));
    }

    public void selectDex(boolean dex) {
        items.get("assoc").setSelected(!dex);
        items.get("dex").setSelected(dex);
    }

    public void refresh() {
        NavItem office = items.get("office");
        if (office != null) office.setBadge(frame.game.newEpilogues());
        Quote q = frame.game.quoteOfDay();
        if (q != null) {
            quote.setText("“" + q.text + "”");
            quoteAuthor.setText("— " + q.author);
        }
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = Theme.aa(g0.create());
        g.setColor(Theme.SIDEBAR);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Theme.GOLD_DARK);
        g.fillRect(getWidth() - 5, 0, 2, getHeight());
        g.setColor(new Color(0x2A1D12));
        g.fillRect(getWidth() - 3, 0, 3, getHeight());
        g.dispose();
    }

    // ------------------------------------------------------------------
    private static class Logo extends JComponent {
        Logo() {
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(190, 66));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            Icons.paint(g, "compass", 4, 14, 34, Theme.GOLD);
            g.setFont(Theme.xbold(22));
            g.setColor(Theme.CREAM);
            g.drawString("기하미궁", 48, 30);
            g.setFont(Theme.tracked(Theme.hanja(15), 0.35f));
            g.setColor(new Color(0xCDB68D));
            g.drawString("幾何迷宮", 49, 54);
            g.dispose();
        }
    }

    private static class NavItem extends JComponent {
        private final String text, icon;
        private boolean selected, hover;
        private int badge;

        void setBadge(int n) { if (n != badge) { badge = n; repaint(); } }

        NavItem(String text, String icon, Runnable action) {
            this.text = text;
            this.icon = icon;
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(190, 46));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { action.run(); }
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setSelected(boolean s) { selected = s; repaint(); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = Theme.aa(g0.create());
            int w = getWidth(), h = getHeight();
            Color fg = Theme.CREAM;
            if (selected) {
                g.setColor(Theme.c(0xF4E6C2));
                g.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));
                g.setColor(Theme.GOLD);
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));
                fg = Theme.INK;
            } else if (hover) {
                g.setColor(new Color(255, 255, 255, 18));
                g.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));
            }
            Icons.paint(g, icon, 16, h / 2.0 - 11, 22, fg);
            g.setFont(selected ? Theme.bold(16) : Theme.serif(16));
            g.setColor(fg);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(text, 52, (h + fm.getAscent() - fm.getDescent()) / 2);
            if (badge > 0) {
                // 새 후일담 수
                String b = String.valueOf(badge);
                g.setFont(Theme.bold(12));
                double bw = Math.max(20, g.getFontMetrics().stringWidth(b) + 12);
                g.setColor(new Color(0xC23B2A));
                g.fill(new RoundRectangle2D.Double(w - bw - 10, h / 2.0 - 10, bw, 20, 10, 10));
                g.setColor(Color.WHITE);
                Theme.drawCentered(g, b, w - bw / 2 - 10, h / 2.0);
            }
            g.dispose();
        }
    }
}
